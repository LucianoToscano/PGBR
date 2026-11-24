package dev.pgrazor;

import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

/** Adapters: PoGoCoordinates reveal handling, the PGBR hundo feed, and local-only isolation. */
public final class SourceAdaptersTest {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static final long NOW = 1_800_000_000_000L;

    public static void main(String[] args) throws Exception {
        // --- PoGoCoordinates: reveal rows keep precision unavailable ---------------------
        JSONArray payload = new JSONArray()
                .put(new JSONObject().put("encounter_id", "111").put("pokemon_id", 25)
                        .put("lat", -23.55).put("lon", -46.63).put("iv_percent", 100)
                        .put("despawn_ms", NOW + 300_000).put("cp", 500).put("level", 20))
                .put(new JSONObject().put("encounter_id", "222").put("pokemon_id", 149)
                        .put("reveal_code", "abc123").put("despawn_ms", NOW + 300_000));
        PoGoCoordinatesSourceAdapter lockedAdapter = new PoGoCoordinatesSourceAdapter(
                (query, cursor) -> payload);
        SourceSearchResult lockedResult = lockedAdapter.search(SourceQuery.builder().build(), null, NOW);
        check(lockedResult.items.size() == 2, "Duas linhas normalizadas: " + lockedResult.items.size());
        check(PoGoCoordinatesSourceAdapter.lockedRows(lockedResult.items) == 1,
                "Uma linha com localização não liberada");
        SourceEvent locked = null, open = null;
        for (SourceEvent event : lockedResult.items) {
            if ("abc123".equals(event.eventId) || event.eventId.endsWith("222")) locked = event;
            else open = event;
        }
        check(locked != null && locked.lat == null && locked.lon == null,
                "reveal_code não vira coordenada");
        check(locked.coordinatePrecision == SourceEvent.CoordinatePrecision.UNAVAILABLE,
                "Precisão indisponível preservada");
        check(!locked.isActionable(NOW), "Linha revelada não pode ser destino");
        check(lockedResult.note.contains("reveal_code"), "Nota explica a limitação: " + lockedResult.note);
        check(open != null && open.isActionable(NOW), "Linha com coordenada continua acionável");

        // without an injected client the adapter refuses instead of inventing a response
        try {
            new PoGoCoordinatesSourceAdapter().search(SourceQuery.builder().build(), null, NOW);
            throw new AssertionError("Sem cliente, o adaptador deve recusar");
        } catch (SourceUnavailableException expected) {
            check(expected.getMessage().contains("reveal_code"), "Motivo cita reveal_code: "
                    + expected.getMessage());
        }

        // --- PGBR hundo feed: normalization, TTL and refusal -----------------------------
        JSONObject hundo = new JSONObject().put("v", 1).put("fetched_ms", NOW).put("interval_s", 30)
                .put("hundos", new JSONArray()
                        .put(new JSONArray().put("900").put(25).put(0).put(-23.55).put(-46.63)
                                .put(NOW + 600_000).put(800).put(30))
                        .put(new JSONArray().put("901").put(26).put(0).put(-23.56).put(-46.64)
                                .put(NOW - 1000).put(100).put(5))); // already despawned
        PGBRHundoSourceAdapter hundoAdapter = new PGBRHundoSourceAdapter(() -> hundo);
        SourceSearchResult hundoResult = hundoAdapter.search(SourceQuery.builder().build(), null, NOW);
        check(hundoResult.items.size() == 1, "Somente linha não expirada: " + hundoResult.items.size());
        check(hundoResult.items.get(0).ivPercent == 100.0, "Feed declara 100% IV");
        check(hundoResult.items.get(0).expiresAt == NOW + 600_000, "TTL do feed preservado");
        check(hundoResult.filtersAppliedRemotely.contains("IV % (somente 100%)"),
                "Filtro remoto declarado pelo feed");
        check(hundoAdapter.status().lastVerifiedAt == NOW, "Última verificação após resposta real");

        // stale feed fails closed
        JSONObject stale = new JSONObject().put("v", 1).put("fetched_ms", NOW - 10 * 60_000L)
                .put("hundos", new JSONArray());
        try {
            new PGBRHundoSourceAdapter(() -> stale).search(SourceQuery.builder().build(), null, NOW);
            throw new AssertionError("Feed antigo deve falhar fechado");
        } catch (SourceUnavailableException expected) {
            check(expected.getMessage().contains("atualização recente"), "Motivo: " + expected.getMessage());
        }

        // --- local-only isolation between PGSharp and iPogo data -------------------------
        LocalEventStore store = new LocalEventStore();
        store.add(SourceEvent.builder("pgsharp", "f1", SourceEvent.Kind.POKEMON)
                .coordinates(-23.55, -46.63).pokemonId(25).ivPercent(100.0)
                .expiresAt(NOW + 600_000).observedAt(NOW).retrievedAt(NOW).build());
        store.add(SourceEvent.builder("ipogo", "f2", SourceEvent.Kind.POKEMON)
                .coordinates(-23.56, -46.64).pokemonId(149).ivPercent(99.0)
                .expiresAt(NOW + 600_000).observedAt(NOW).retrievedAt(NOW).build());
        LocalSourceAdapters.PGSharp pgsharp = new LocalSourceAdapters.PGSharp(store);
        LocalSourceAdapters.IPogo ipogo = new LocalSourceAdapters.IPogo(store);
        SourceSearchResult pgsharpResult = pgsharp.search(SourceQuery.builder().build(), null, NOW);
        SourceSearchResult ipogoResult = ipogo.search(SourceQuery.builder().build(), null, NOW);
        check(pgsharpResult.items.size() == 1 && "pgsharp".equals(pgsharpResult.items.get(0).sourceId),
                "Adaptador PGSharp devolve apenas dados PGSharp");
        check(ipogoResult.items.size() == 1 && "ipogo".equals(ipogoResult.items.get(0).sourceId),
                "Adaptador iPogo devolve apenas dados iPogo");
        check(!pgsharp.capabilities().supportsRemoteSearch && !ipogo.capabilities().supportsRemoteSearch,
                "Nenhum dos dois declara busca remota");

        // pagination is refused instead of silently ignored
        try {
            pgsharp.search(SourceQuery.builder().build(), "2", NOW);
            throw new AssertionError("Cursor em dados locais deve ser recusado");
        } catch (SourceUnavailableException expected) {
            check(expected.getMessage().contains("paginação"), "Motivo: " + expected.getMessage());
        }

        // --- the generic local adapter applies filters locally and reports them -----------
        LocalStoreAdapter local = new LocalStoreAdapter(store);
        SourceSearchResult filtered = local.search(SourceQuery.builder().iv(100.0, 100.0).build(), null, NOW);
        check(filtered.items.size() == 1 && "pgsharp".equals(filtered.items.get(0).sourceId),
                "Filtro aplicado localmente");
        check(filtered.filtersAppliedLocally.contains("IV %"), "Filtro marcado como local");
        check(!filtered.hasMore && filtered.nextCursor == null, "Dados locais não paginam");

        System.out.println("SourceAdaptersTest: OK");
    }
}
