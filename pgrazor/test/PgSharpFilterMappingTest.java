package dev.pgrazor;

import org.json.JSONArray;
import org.json.JSONObject;

/** PGSharp archive → canonical presets: explicit mapping, preserved raw, reported gaps. */
public final class PgSharpFilterMappingTest {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    public static void main(String[] args) throws Exception {
        JSONObject archive = new JSONObject()
                .put("format", "PGBR PGSharp migration").put("version", 1)
                .put("favorites", new JSONArray())
                .put("routes", new JSONArray())
                .put("pokemonFilters", new JSONArray()
                        .put(new JSONObject().put("name", "Hundo rápido")
                                .put("ivMin", 90).put("ivMax", 100)
                                .put("species", new JSONArray().put(25).put(149))
                                .put("levelMin", 20).put("cpMin", 1500)
                                .put("gender", "female")
                                .put("pvpLeague", "GL").put("pvpRank", 5)
                                // a field this version does not understand must be reported, not applied
                                .put("unknownSetting", true).put("weather", true))
                        .put(new JSONObject().put("name", "Reide")
                                .put("tiers", new JSONArray().put(1).put(3))))
                .put("questFilters", new JSONArray()
                        .put(new JSONObject().put("name", "Pesquisa")
                                .put("reward", "Stardust").put("rewardType", "Stardust")))
                .put("raidFilters", new JSONArray()
                        .put(new JSONObject().put("name", "Reide 5")
                                .put("tiers", new JSONArray().put(5))));

        PgSharpFilterMapping.MappingReport report = PgSharpFilterMapping.map(archive);
        check(report.presets.size() == 4, "Quatro filtros convertidos, foi " + report.presets.size());
        check(report.converted == 4, "Contagem de convertidos: " + report.converted);
        check(report.rawSource.contains("unknownSetting"), "Arquivo original preservado");
        check(report.unsupportedFields.contains("pokemon.unknownSetting"),
                "Campo desconhecido reportado: " + report.unsupportedFields);
        for (String unsupported : new String[]{"pokemon.weather", "quest.reward", "quest.rewardType", "raid.tiers"})
            check(report.unsupportedFields.contains(unsupported), "Predicado não convertido precisa aparecer: " + unsupported);
        check(report.summary().contains("Revise os critérios não convertidos"), "Aviso de revisão");
        check(report.summary().contains("Nenhum feed ou teleporte foi ativado"),
                "Resumo não promete feed: " + report.summary());

        PresetCodec.Preset hundo = null;
        for (PresetCodec.Preset preset : report.presets)
            if ("Hundo rápido".equals(preset.name)) hundo = preset;
        check(hundo != null, "Preset convertido pelo nome");
        check(hundo.query.ivMin == 90.0 && hundo.query.ivMax == 100.0, "Faixa de IV mapeada");
        check(hundo.query.pokemonIds.size() == 2, "Espécies mapeadas");
        check(hundo.query.levelMin == 20 && hundo.query.cpMin == 1500, "Nível/PC mapeados");
        check("FEMALE".equals(hundo.query.gender), "Sexo mapeado");
        check(SourceQuery.LEAGUE_GREAT.equals(hundo.query.pvpLeague) && hundo.query.pvpMaxRank == 5,
                "PvP mapeado");
        check("pgsharp-data".equals(hundo.migratedFrom), "Proveniência PGSharp registrada");
        check(hundo.rawSource.contains("unknownSetting"), "Filtro original preservado no preset");

        // Quest/raid families map to their own kinds, so a quest never becomes a Pokémon filter.
        boolean questKind = false, raidKind = false;
        for (PresetCodec.Preset preset : report.presets) {
            if ("Pesquisa".equals(preset.name)) questKind = preset.query.kinds.contains(SourceEvent.Kind.QUEST);
            if ("Reide 5".equals(preset.name)) raidKind = preset.query.kinds.contains(SourceEvent.Kind.RAID);
        }
        check(questKind, "Filtro de pesquisa vira preset do tipo quest");
        check(raidKind, "Filtro de reide vira preset do tipo raid");

        // An archive with nothing to map reports zero conversions, not a fake preset.
        JSONObject empty = new JSONObject().put("format", "PGBR PGSharp migration").put("version", 1)
                .put("favorites", new JSONArray()).put("routes", new JSONArray())
                .put("pokemonFilters", new JSONArray()).put("questFilters", new JSONArray())
                .put("raidFilters", new JSONArray());
        PgSharpFilterMapping.MappingReport emptyReport = PgSharpFilterMapping.map(empty);
        check(emptyReport.presets.isEmpty() && emptyReport.converted == 0,
                "Arquivo sem filtros não gera preset");
        check(PgSharpFilterMapping.map(null).presets.isEmpty(), "Arquivo nulo não gera preset");

        System.out.println("PgSharpFilterMappingTest: OK");
    }
}
