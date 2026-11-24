package dev.pgrazor;

import java.util.ArrayList;
import java.util.List;

/** Filtering: real counts, fail-closed unsupported filters, AND/OR, TTL and ordering. */
public final class EventFilterEngineTest {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static final long NOW = 1_800_000_000_000L;

    private static SourceEvent spawn(String id, int dex, Double iv, int cp, long expiresAt) {
        return SourceEvent.builder("iflowgo", id, SourceEvent.Kind.POKEMON)
                .coordinates(-23.55, -46.63).pokemonId(dex).ivPercent(iv)
                .cp(cp).level(20).expiresAt(expiresAt).retrievedAt(NOW).observedAt(NOW).build();
    }

    public static void main(String[] args) throws Exception {
        List<SourceEvent> rows = new ArrayList<>();
        rows.add(spawn("a", 25, 100.0, 500, NOW + 600_000));
        rows.add(spawn("b", 25, 87.5, 431, NOW + 300_000));
        rows.add(spawn("c", 26, 0.0, 120, NOW + 120_000));
        rows.add(spawn("d", 149, null, 300, NOW + 240_000));
        rows.add(spawn("e", 150, 50.0, 200, NOW - 1000)); // expired
        rows.add(SourceEvent.builder("import", "f", SourceEvent.Kind.RAID)
                .coordinates(-23.55, -46.63).expiresAt(NOW + 900_000).build());
        rows.add(SourceEvent.builder("import", "g", SourceEvent.Kind.POKEMON)
                .coordinates(-23.55, -46.63).isDemo(true).build());

        // --- IV 100 filter returns only the real 100% row, never a fabricated list -----
        EventFilterEngine.Outcome hundred = EventFilterEngine.apply(
                SourceQuery.builder().iv(100.0, 100.0).build(), rows, NOW);
        check(hundred.kept.size() == 1, "IV 100 deve retornar 1 resultado, retornou " + hundred.kept.size());
        check("a".equals(hundred.kept.get(0).eventId), "IV 100 retornou o registro errado");
        check(hundred.filteredOut == 4, "Registros não correspondentes contados como filtrados: "
                + hundred.filteredOut);
        check(hundred.expired == 1, "Expirado contado separadamente");
        check(hundred.demoDropped == 1, "Demo contado separadamente");
        check(hundred.summary().contains("Recebidos 7"), "Resumo mostra recebidos: " + hundred.summary());

        // --- IV 0 filter must not match unknown IV -------------------------------------
        EventFilterEngine.Outcome zero = EventFilterEngine.apply(
                SourceQuery.builder().iv(0.0, 0.0).build(), rows, NOW);
        check(zero.kept.size() == 1 && "c".equals(zero.kept.get(0).eventId),
                "IV 0 é observação real e não casa com IV desconhecido");
        check(zero.appliedLocally.contains("IV %"), "Filtro de IV aplicado localmente");
        check(!zero.unsupported.contains("IV %"), "IV não pode ser marcado como não suportado");

        // --- a filter whose field is absent everywhere is reported, not silently inert --
        List<SourceEvent> noPvp = new ArrayList<>();
        noPvp.add(spawn("x", 25, 100.0, 500, NOW + 600_000));
        EventFilterEngine.Outcome unsupported = EventFilterEngine.apply(
                SourceQuery.builder().pvp(SourceQuery.LEAGUE_GREAT, 5).build(), noPvp, NOW);
        check(unsupported.kept.isEmpty(), "Filtro sem campo correspondente retorna vazio (fail-closed)");
        check(unsupported.unsupported.contains("rank PvP"),
                "Filtro não suportado precisa ser nomeado: " + unsupported.unsupported);
        check(unsupported.appliedLocally.isEmpty(), "Nada foi aplicado de fato");

        // --- species + CP + level combinations -----------------------------------------
        EventFilterEngine.Outcome combo = EventFilterEngine.apply(SourceQuery.builder()
                .pokemon(25).cp(400, 999).level(20, 20).build(), rows, NOW);
        check(combo.kept.size() == 2, "AND lógico entre espécie/PC/nível, retornou " + combo.kept.size());

        // --- OR logic -------------------------------------------------------------------
        EventFilterEngine.Outcome or = EventFilterEngine.apply(SourceQuery.builder()
                .pokemon(25).cp(100, 150).andLogic(false).build(), rows, NOW);
        check(or.kept.size() == 3, "OR lógico une critérios, retornou " + or.kept.size());

        // --- radius is fail-closed ------------------------------------------------------
        EventFilterEngine.Outcome radius = EventFilterEngine.apply(SourceQuery.builder()
                .center(-23.55, -46.63, 1.0).build(), rows, NOW);
        check(radius.kept.size() == 5, "Raio de 1 km mantém os registros próximos: " + radius.kept.size());

        List<SourceEvent> far = new ArrayList<>();
        far.add(SourceEvent.builder("import", "far", SourceEvent.Kind.POKEMON)
                .coordinates(51.5, -0.12).expiresAt(NOW + 600_000).build());
        EventFilterEngine.Outcome outside = EventFilterEngine.apply(SourceQuery.builder()
                .center(-23.55, -46.63, 1.0).build(), far, NOW);
        check(outside.kept.isEmpty(), "Registro fora do raio é filtrado");

        // --- TTL and expiry windows -----------------------------------------------------
        EventFilterEngine.Outcome ttl = EventFilterEngine.apply(SourceQuery.builder()
                .minTtlSeconds(200L).build(), rows, NOW);
        check(ttl.kept.size() == 4, "TTL mínimo remove os que expiram logo: " + ttl.kept.size());
        EventFilterEngine.Outcome expiring = EventFilterEngine.apply(SourceQuery.builder()
                .maxRemainingSeconds(300L).build(), rows, NOW);
        check(expiring.kept.size() == 3, "Janela de expiração seleciona os que terminam logo: "
                + expiring.kept.size());

        // --- ordering -------------------------------------------------------------------
        EventFilterEngine.Outcome byIv = EventFilterEngine.apply(SourceQuery.builder()
                .sort(SourceQuery.Sort.IV_DESC).build(), rows, NOW);
        check(byIv.kept.get(0).ivPercent == 100.0, "Ordenação por IV começa no maior");
        check(byIv.kept.get(byIv.kept.size() - 1).ivPercent == null, "IV desconhecido vai por último");

        // --- limit is a page, not a claim of coverage ------------------------------------
        EventFilterEngine.Outcome limited = EventFilterEngine.apply(SourceQuery.builder()
                .limit(2).sort(SourceQuery.Sort.IV_DESC).build(), rows, NOW);
        check(limited.kept.size() == 2 && limited.received == 7,
                "limit corta a página mas mantém a contagem recebida");

        // --- per-source counts -----------------------------------------------------------
        EventFilterEngine.Outcome perSource = EventFilterEngine.apply(SourceQuery.builder().build(), rows, NOW);
        check(perSource.perSource.get("iflowgo") == 4 && perSource.perSource.get("import") == 1,
                "Contagem por origem correta: " + perSource.perSource);
        check(!perSource.kept.isEmpty(), "Lista e contagem usam os mesmos registros");

        // --- demo is opt-in --------------------------------------------------------------
        EventFilterEngine.Outcome demo = EventFilterEngine.apply(
                SourceQuery.builder().includeDemo(true).build(), rows, NOW);
        check(demo.demoDropped == 0 && demo.kept.size() == 6, "Demo só entra quando pedido: " + demo.kept.size());

        // --- text search -----------------------------------------------------------------
        SourceEvent named = SourceEvent.builder("import", "named", SourceEvent.Kind.POKEMON)
                .coordinates(-23.55, -46.63).pokemonId(25).name("Pikachu")
                .expiresAt(NOW + 600_000).build();
        List<SourceEvent> namedRows = new ArrayList<>();
        namedRows.add(named);
        namedRows.add(spawn("z", 26, 10.0, 50, NOW + 600_000));
        EventFilterEngine.Outcome byName = EventFilterEngine.apply(
                SourceQuery.builder().text("pika").build(), namedRows, NOW);
        check(byName.kept.size() == 1 && "named".equals(byName.kept.get(0).eventId),
                "Busca por texto ignora maiúsculas/minúsculas");
        EventFilterEngine.Outcome byDex = EventFilterEngine.apply(
                SourceQuery.builder().text("#25").build(), namedRows, NOW);
        check(byDex.kept.size() == 1, "Busca por #Dex funciona");

        // Synthetic regressions: unavailable fields must not appear supported by discarded rows.
        List<SourceEvent> mixed = new ArrayList<>();
        mixed.add(null);
        mixed.add(spawn("unknown", 25, null, 300, NOW + 600_000));
        mixed.add(spawn("expired-known", 25, 100.0, 300, NOW - 1));
        mixed.add(SourceEvent.builder("import", "demo-known", SourceEvent.Kind.POKEMON)
                .coordinates(-23.55, -46.63).ivPercent(100.0).isDemo(true).build());
        EventFilterEngine.Outcome unavailable = EventFilterEngine.apply(
                SourceQuery.builder().iv(90.0, 100.0).build(), mixed, NOW);
        check(unavailable.kept.isEmpty(), "Unknown IV must not match a bound");
        check(unavailable.unsupported.contains("IV %"), "Discarded rows must not imply available IV");
        check(!unavailable.appliedLocally.contains("IV %"), "No eligible row carries IV");
        check(unavailable.expired == 1 && unavailable.demoDropped == 1, "Discard reasons preserved");

        List<SourceEvent> ranks = new ArrayList<>();
        ranks.add(SourceEvent.builder("import", "ultra-only", SourceEvent.Kind.POKEMON)
                .pvpUltraRank(1).build());
        EventFilterEngine.Outcome great = EventFilterEngine.apply(
                SourceQuery.builder().pvp(SourceQuery.LEAGUE_GREAT, 5).build(), ranks, NOW);
        check(great.kept.isEmpty() && great.unsupported.contains("rank PvP"),
                "Ultra-only data cannot satisfy Great League");
        EventFilterEngine.Outcome ultra = EventFilterEngine.apply(
                SourceQuery.builder().pvp(SourceQuery.LEAGUE_ULTRA, 5).build(), ranks, NOW);
        check(ultra.kept.size() == 1 && ultra.appliedLocally.contains("rank PvP"),
                "Selected league rank is correctly reported");
        EventFilterEngine.Outcome emptyOr = EventFilterEngine.apply(
                SourceQuery.builder().andLogic(false).build(), rows, NOW);
        check(emptyOr.kept.size() == perSource.kept.size(), "Empty OR must not discard all rows");

        System.out.println("EventFilterEngineTest: OK");
    }
}
