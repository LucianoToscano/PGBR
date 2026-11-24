package dev.pgrazor;

import java.util.ArrayList;
import java.util.List;
import org.json.JSONObject;

/** Presets: export/import, v1 migration, validation and local alerts with real criteria. */
public final class PresetCodecTest {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static final long NOW = 1_800_000_000_000L;

    private static SourceEvent event(String id, int dex, Double iv, Integer rank, long expiresAt) {
        return SourceEvent.builder("import", id, SourceEvent.Kind.POKEMON)
                .coordinates(-23.55, -46.63).pokemonId(dex).ivPercent(iv).cp(500)
                .pvpGreatRank(rank).expiresAt(expiresAt).observedAt(NOW).retrievedAt(NOW).build();
    }

    public static void main(String[] args) throws Exception {
        // --- export / import round trip --------------------------------------------------
        PresetCodec.Preset hundred = PresetCodec.create("IV 100", SourceQuery.builder()
                .iv(100.0, 100.0).limit(25).build(), NOW);
        PresetCodec.Preset pvp = PresetCodec.create("PvP GL", SourceQuery.builder()
                .pvp(SourceQuery.LEAGUE_GREAT, 10).build(), NOW + 1000);
        pvp = pvp.withFavorite(true, NOW + 2000);
        List<PresetCodec.Preset> presets = new ArrayList<>();
        presets.add(hundred);
        presets.add(pvp);
        String exported = PresetCodec.export(presets);
        check(!exported.contains("token") && !exported.contains("password"),
                "Preset exportado não contém segredo");
        List<PresetCodec.Preset> imported = PresetCodec.importPresets(exported);
        check(imported.size() == 2, "Dois presets reimportados, foi " + imported.size());
        PresetCodec.Preset restoredPvp = null;
        for (PresetCodec.Preset preset : imported) if ("PvP GL".equals(preset.name)) restoredPvp = preset;
        check(restoredPvp != null && restoredPvp.favorite, "Favorito preservado no round trip");
        check(restoredPvp.query.pvpLeague.equals(SourceQuery.LEAGUE_GREAT)
                && restoredPvp.query.pvpMaxRank == 10, "Critérios PvP preservados");
        check(PresetCodec.favoriteIds(imported).size() == 1, "Um favorito");

        // --- v1 migration (historical saved-filter shape) ---------------------------------
        JSONObject v1 = new JSONObject().put("name", "Hundo Rápido")
                .put("ivMin", 95).put("ivMax", 100)
                .put("species", new org.json.JSONArray().put(25).put(149))
                .put("pvpLeague", "UL").put("pvpRank", 3)
                .put("gender", "male").put("sizes", new org.json.JSONArray().put("XXL"))
                .put("lat", -23.55).put("lon", -46.63).put("radiusKm", 3);
        PresetCodec.Preset migrated = PresetCodec.presetFromJson(v1);
        check("1".equals(migrated.migratedFrom), "Migração registra a origem v1");
        check(migrated.query.pokemonIds.size() == 2, "Espécies migradas");
        check(migrated.query.ivMin == 95.0 && migrated.query.ivMax == 100.0, "Faixa de IV migrada");
        check(migrated.query.pvpLeague.equals(SourceQuery.LEAGUE_ULTRA), "Liga migrada");
        check(migrated.query.gender.equals("MALE"), "Sexo migrado");
        check(migrated.query.sizes.contains("XXL"), "Tamanho migrado");
        check(migrated.query.radiusKm == 3.0, "Raio migrado");
        check(migrated.rawSource.contains("ivMin"), "Objeto original preservado em raw_source");

        // --- a v1 file with an unsupported field keeps it in raw_source -------------------
        JSONObject v1Unknown = new JSONObject().put("name", "Antigo").put("ivMin", 90)
                .put("customField", "valor");
        PresetCodec.Preset migratedUnknown = PresetCodec.presetFromJson(v1Unknown);
        check(migratedUnknown.rawSource.contains("customField"), "Campo desconhecido preservado");
        check(migratedUnknown.query.ivMin == 90.0, "Campo conhecido aplicado");

        // --- invalid input ------------------------------------------------------------------
        try {
            PresetCodec.importPresets("{\"format\":\"outro\",\"version\":2,\"presets\":[]}");
            throw new AssertionError("Formato errado deve ser rejeitado");
        } catch (org.json.JSONException expected) { /* ok */ }
        try {
            PresetCodec.importPresets("{\"format\":\"PGBR preset\",\"version\":99,\"presets\":[]}");
            throw new AssertionError("Versão futura deve ser rejeitada");
        } catch (org.json.JSONException expected) {
            check(expected.getMessage().contains("Versão"), "Mensagem de versão: " + expected.getMessage());
        }
        try {
            PresetCodec.importPresets("   ");
            throw new AssertionError("Arquivo vazio deve ser rejeitado");
        } catch (org.json.JSONException expected) { /* ok */ }

        // --- dedupe by id -------------------------------------------------------------------
        List<PresetCodec.Preset> duplicated = new ArrayList<>();
        duplicated.add(hundred);
        duplicated.add(hundred);
        check(PresetCodec.dedupe(duplicated).size() == 1, "Presets duplicados por id são fundidos");

        // --- rename keeps the id -------------------------------------------------------------
        PresetCodec.Preset renamed = hundred.rename("Novo nome", NOW);
        check(renamed.id.equals(hundred.id) && renamed.name.equals("Novo nome"),
                "Renomear mantém a identidade do preset");
        check(PresetCodec.slug("IV 100% Rápido!").equals("iv-100-rapido"),
                "Slug estável: " + PresetCodec.slug("IV 100% Rápido!"));

        // --- alerts: preset without criteria never fires ------------------------------------
        List<SourceEvent> rows = new ArrayList<>();
        rows.add(event("a", 25, 100.0, 1, NOW + 600_000));
        rows.add(event("b", 26, 10.0, null, NOW + 600_000));
        rows.add(event("expired", 25, 100.0, 1, NOW - 1000));
        PresetCodec.Preset emptyPreset = PresetCodec.create("Vazio", SourceQuery.builder().build(), NOW);
        List<PresetCodec.Preset> forAlerts = new ArrayList<>();
        forAlerts.add(emptyPreset);
        check(AlertEngine.evaluate(forAlerts, rows, NOW).isEmpty(),
                "Preset sem critério não dispara alerta");

        PresetCodec.Preset alertPreset = PresetCodec.create("IV 100", SourceQuery.builder()
                .iv(100.0, 100.0).build(), NOW);
        forAlerts.add(alertPreset);
        List<AlertEngine.Alert> alerts = AlertEngine.evaluate(forAlerts, rows, NOW);
        check(alerts.size() == 1, "Um alerta para o registro que casa, foi " + alerts.size());
        check(alerts.get(0).reason.contains("IV 100%"), "Motivo cita o critério: " + alerts.get(0).reason);
        check(alerts.get(0).reason.contains("import"), "Motivo cita a origem");
        check(!"expired".equals(alerts.get(0).event.eventId), "Registro expirado não gera alerta");

        // Repeated observations and expired rows must not consume the preset limit.
        List<SourceEvent> repeated = new ArrayList<>();
        repeated.add(event("same", 25, 100.0, 1, NOW + 600_000));
        repeated.add(SourceEvent.builder("import", "same", SourceEvent.Kind.POKEMON)
                .pokemonId(25).ivPercent(100.0).observedAt(NOW + 1)
                .retrievedAt(NOW + 1).expiresAt(NOW - 1).build());
        repeated.add(event("valid", 25, 100.0, 1, NOW + 600_000));
        List<PresetCodec.Preset> limited = new ArrayList<>();
        limited.add(PresetCodec.create("Limitado", SourceQuery.builder()
                .iv(100.0, 100.0).includeExpired(true).limit(1).build(), NOW));
        List<AlertEngine.Alert> limitedAlerts = AlertEngine.evaluate(limited, repeated, NOW);
        check(limitedAlerts.size() == 1 && "valid".equals(limitedAlerts.get(0).event.eventId),
                "Observação expirada mais recente não ressuscita a antiga nem consome o limite");

        // --- shiny is only reported when observed -------------------------------------------
        SourceEvent shinyObserved = SourceEvent.builder("import", "shiny", SourceEvent.Kind.POKEMON)
                .coordinates(-23.55, -46.63).pokemonId(25).name("Pikachu")
                .shinyObserved(true).expiresAt(NOW + 600_000).build();
        List<SourceEvent> shinyRows = new ArrayList<>();
        shinyRows.add(shinyObserved);
        shinyRows.add(SourceEvent.builder("import", "shiny-catalog", SourceEvent.Kind.POKEMON)
                .coordinates(-23.55, -46.63).pokemonId(25).name("Shiny Pikachu")
                .expiresAt(NOW + 600_000).build());
        PresetCodec.Preset speciesPreset = PresetCodec.create("Espécie 25",
                SourceQuery.builder().pokemon(25).build(), NOW);
        List<PresetCodec.Preset> shinyPresets = new ArrayList<>();
        shinyPresets.add(speciesPreset);
        List<AlertEngine.Alert> shinyAlerts = AlertEngine.evaluate(shinyPresets, shinyRows, NOW);
        int observed = 0, unconfirmed = 0;
        for (AlertEngine.Alert alert : shinyAlerts) {
            if (alert.reason.contains("Shiny observado pela fonte")) observed++;
            if (alert.reason.contains("sem observação confirmada")) unconfirmed++;
        }
        check(observed == 1, "Somente shiny observado é afirmado, foi " + observed);
        check(unconfirmed == 1, "Nome com 'shiny' sem observação é marcado como não confirmado");

        System.out.println("PresetCodecTest: OK");
    }
}
