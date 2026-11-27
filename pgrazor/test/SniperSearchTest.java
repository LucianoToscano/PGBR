package dev.pgrazor;

import java.util.ArrayList;
import java.util.List;

/** Sniper 1/2: real gating, honest multi-source contributions, ordering and cooldown. */
public final class SniperSearchTest {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static final long NOW = 1_800_000_000_000L;

    private static SourceEvent row(String id, double lat, double lon, long expiresAt, boolean demo) {
        return SourceEvent.builder("import", id, SourceEvent.Kind.POKEMON)
                .coordinates(lat, lon).pokemonId(25).ivPercent(100.0).cp(500)
                .observedAt(NOW).retrievedAt(NOW).expiresAt(expiresAt).isDemo(demo).build();
    }

    /** Deterministic adapter used instead of a network source. */
    private static final class FakeAdapter implements SourceAdapter {
        final String id;
        final List<SourceEvent> rows;
        final SourceUnavailableException failure;

        FakeAdapter(String id, List<SourceEvent> rows, SourceUnavailableException failure) {
            this.id = id;
            this.rows = rows;
            this.failure = failure;
        }

        @Override public String sourceId() { return id; }
        @Override public SourceCapabilities capabilities() { return SourceRegistry.of(id); }
        @Override public SourceCapabilities status() { return SourceRegistry.of(id); }
        @Override public String attribution() { return capabilities().attribution; }
        @Override public void cancel() { }

        @Override public SourceSearchResult search(SourceQuery query, String cursor, long now) throws Exception {
            if (failure != null) throw failure;
            EventFilterEngine.Outcome outcome = EventFilterEngine.apply(query, rows, now);
            return new SourceSearchResult(id, outcome.kept, false, null, Integer.valueOf(rows.size()),
                    now, capabilities().coverage, new ArrayList<String>(), outcome.appliedLocally,
                    outcome.unsupported, "fonte de teste");
        }
    }

    public static void main(String[] args) throws Exception {
        LocalEventStore store = new LocalEventStore();
        store.add(row("near", -23.5505, -46.6333, NOW + 600_000, false));
        store.add(row("far", -23.6000, -46.7000, NOW + 600_000, false));
        store.add(row("expired", -23.5505, -46.6333, NOW - 1000, false));
        store.add(row("demo", -23.5505, -46.6333, NOW + 600_000, true));
        store.add(SourceEvent.builder("import", "locked", SourceEvent.Kind.POKEMON)
                .pokemonId(25).ivPercent(100.0).precision(SourceEvent.CoordinatePrecision.UNAVAILABLE)
                .expiresAt(NOW + 600_000).build());

        // --- Sniper 1 keeps only usable targets, nearest first --------------------------
        SniperSearch.Outcome first = SniperSearch.sniper1(SourceQuery.builder().pokemon(25).build(),
                new ArrayList<SourceAdapter>(), store, NOW, -23.55, -46.63, 0L);
        check(first.results.size() == 2, "Sniper 1 deve listar 2 alvos, listou " + first.results.size());
        check("near".equals(first.results.get(0).event.eventId), "Mais próximo primeiro");
        check(first.results.get(0).actionable(), "Alvo válido permite teleporte");
        check(first.results.get(1).distanceKm > first.results.get(0).distanceKm, "Ordenado por distância");

        // --- default TTL removes targets that despawn in under a minute ------------------
        store.add(row("short", -23.5505, -46.6333, NOW + 30_000, false));
        SniperSearch.Outcome ttl = SniperSearch.sniper1(SourceQuery.builder().build(),
                new ArrayList<SourceAdapter>(), store, NOW, -23.55, -46.63, 0L);
        for (SniperSearch.Result result : ttl.results)
            check(!"short".equals(result.event.eventId), "TTL padrão remove alvo de 30 s");

        // --- cooldown blocks teleport but keeps copy/map ---------------------------------
        SniperSearch.Outcome cooling = SniperSearch.sniper1(SourceQuery.builder().pokemon(25).build(),
                new ArrayList<SourceAdapter>(), store, NOW, -23.55, -46.63, NOW + 60_000);
        check(!cooling.results.get(0).action.canTeleport, "Cooldown bloqueia o teleporte");
        check(cooling.results.get(0).action.canCopy && cooling.results.get(0).action.canMap,
                "Cooldown não bloqueia copiar/abrir mapa");
        check(cooling.results.get(0).action.blockReason.contains("cooldown"),
                "Motivo do bloqueio: " + cooling.results.get(0).action.blockReason);

        // --- reveal-only rows never become targets ---------------------------------------
        SniperSearch.ActionTarget lockedAction = SniperSearch.actionFor(
                SourceEvent.builder("pogocoordinates", "locked", SourceEvent.Kind.POKEMON)
                        .precision(SourceEvent.CoordinatePrecision.UNAVAILABLE)
                        .expiresAt(NOW + 600_000).build(), NOW, 0L);
        check(!lockedAction.canTeleport && !lockedAction.canCopy,
                "reveal_code não vira coordenada");
        check(lockedAction.blockReason.toLowerCase().contains("reveal"), "Motivo cita reveal_code");

        // --- a refused source is reported, never replaced --------------------------------
        SourceUnavailableException refusal = new SourceUnavailableException(
                "iFlowGo recusou a consulta (HTTP 403).", 403, 0L, true);
        List<SourceAdapter> adapters = new ArrayList<>();
        adapters.add(new FakeAdapter("iflowgo", null, refusal));
        adapters.add(new FakeAdapter("pgbr_hundo", new ArrayList<SourceEvent>(), null));
        SniperSearch.Outcome mixed = SniperSearch.sniper1(SourceQuery.builder().pokemon(25).build(),
                adapters, store, NOW, -23.55, -46.63, 0L);
        check(mixed.anyUnavailable, "Fonte indisponível é sinalizada");
        boolean foundBlocked = false;
        for (SniperSearch.Contribution contribution : mixed.contributions) {
            if ("iflowgo".equals(contribution.sourceId)) {
                foundBlocked = true;
                check("blocked".equals(contribution.status), "Recusa é status blocked: " + contribution.status);
                check(contribution.detail.contains("403"), "Detalhe carrega o HTTP: " + contribution.detail);
            }
        }
        check(foundBlocked, "Contribuição da fonte recusada aparece no relatório");
        check(mixed.summary().contains("iFlowGo"), "Resumo nomeia a fonte que falhou: " + mixed.summary());
        check(!mixed.results.isEmpty(), "As fontes que responderam continuam utilizáveis");

        // --- Sniper 2 returns several results with AND/OR and PvP ------------------------
        List<SourceEvent> advancedRows = new ArrayList<>();
        advancedRows.add(SourceEvent.builder("iflowgo", "pvp", SourceEvent.Kind.POKEMON)
                .coordinates(-23.55, -46.63).pokemonId(25).ivPercent(98.0).cp(1400)
                .pvpGreatRank(2).expiresAt(NOW + 600_000).build());
        advancedRows.add(SourceEvent.builder("iflowgo", "plain", SourceEvent.Kind.POKEMON)
                .coordinates(-23.55, -46.63).pokemonId(26).ivPercent(50.0).cp(100)
                .expiresAt(NOW + 600_000).build());
        List<SourceAdapter> advanced = new ArrayList<>();
        advanced.add(new FakeAdapter("iflowgo", advancedRows, null));
        SniperSearch.Outcome second = SniperSearch.sniper2(
                SourceQuery.builder().pvp(SourceQuery.LEAGUE_GREAT, 5).build(),
                advanced, store, NOW, -23.55, -46.63, 0L);
        check(second.results.size() == 1 && "pvp".equals(second.results.get(0).event.eventId),
                "Sniper 2 aplica rank PvP");

        SniperSearch.Outcome empty = SniperSearch.sniper1(
                SourceQuery.builder().pokemon(999).build(), new ArrayList<SourceAdapter>(),
                new LocalEventStore(), NOW, -23.55, -46.63, 0L);
        check(empty.isEmpty() && empty.shown == 0, "Sem dados, resultado vazio e honesto");
        check(empty.summary().contains("exibidos 0"), "Resumo não inventa resultado: " + empty.summary());

        // Dedicated PGSharp screen: exercise the real local adapter with a mixed store,
        // and no unfiltered fallback. The source label is still an imported claim.
        LocalEventStore scoped = new LocalEventStore();
        scoped.add(SourceEvent.builder(SourceRegistry.PGSHARP, "pg-only", SourceEvent.Kind.POKEMON)
                .coordinates(-23.55, -46.63).pokemonId(25).ivPercent(100.0)
                .expiresAt(NOW + 600_000).build());
        scoped.add(SourceEvent.builder(SourceRegistry.PGSHARP, "pg-expired", SourceEvent.Kind.POKEMON)
                .coordinates(-23.55, -46.63).pokemonId(25).ivPercent(100.0)
                .expiresAt(NOW - 1000).build());
        scoped.add(row("other-import", -23.55, -46.63, NOW + 600_000, false));
        scoped.add(SourceEvent.builder(SourceRegistry.POKELIST, "other-source", SourceEvent.Kind.POKEMON)
                .coordinates(-23.55, -46.63).pokemonId(25).ivPercent(100.0)
                .expiresAt(NOW + 600_000).build());
        List<SourceAdapter> onlyPg = java.util.Collections.singletonList(new LocalSourceAdapters.PGSharp(scoped));
        SourceQuery pgQuery = SourceQuery.builder().pokemon(25).build();
        for (SniperSearch.Outcome outcome : new SniperSearch.Outcome[]{
                SniperSearch.sniper1(pgQuery, onlyPg, null, NOW, -23.55, -46.63, 0L),
                SniperSearch.sniper2(pgQuery, onlyPg, null, NOW, -23.55, -46.63, 0L)}) {
            check(outcome.results.size() == 1 && "pg-only".equals(outcome.results.get(0).event.eventId),
                    "PGSharp-only mode excludes other sources and expired data");
            check(outcome.contributions.size() == 1 && SourceRegistry.PGSHARP.equals(outcome.contributions.get(0).sourceId),
                    "PGSharp-only mode does not merge the general local store");
        }

        System.out.println("SniperSearchTest: OK");
    }
}
