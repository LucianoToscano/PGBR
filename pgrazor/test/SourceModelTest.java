package dev.pgrazor;

import org.json.JSONArray;
import org.json.JSONObject;

/** Canonical model: null vs zero, coordinate validation, provenance and capability honesty. */
public final class SourceModelTest {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    public static void main(String[] args) throws Exception {
        long now = 1_800_000_000_000L;

        // --- absent is null; IV zero is a real observation -------------------------------
        SourceEvent unknownIv = SourceEvent.builder("iflowgo", "a", SourceEvent.Kind.POKEMON)
                .coordinates(-23.5, -46.6).pokemonId(25).build();
        check(unknownIv.ivPercent == null, "IV ausente deve ser null, não zero");
        check(unknownIv.cp == null && unknownIv.level == null, "CP/nível ausentes devem ser null");

        SourceEvent zeroIv = SourceEvent.builder("iflowgo", "b", SourceEvent.Kind.POKEMON)
                .coordinates(-23.5, -46.6).pokemonId(25).ivPercent(0.0)
                .ivAtk(0).ivDef(0).ivSta(0).build();
        check(zeroIv.ivPercent != null && zeroIv.ivPercent == 0.0, "IV 0 deve ser preservado");
        check(zeroIv.ivAtk == 0 && zeroIv.ivDef == 0 && zeroIv.ivSta == 0, "A/D/S zero é observação real");

        // --- coordinates: 0,0 is a legend, not a destination -----------------------------
        check(!SourceEvent.validCoordinates(0.0, 0.0), "0,0 é legenda e não pode virar destino");
        check(!SourceEvent.validCoordinates(Double.NaN, 10.0), "NaN é inválido");
        check(!SourceEvent.validCoordinates(91.0, 10.0), "Latitude fora do intervalo é inválida");
        check(!SourceEvent.validCoordinates(null, 10.0), "Coordenada nula é inválida");
        check(SourceEvent.validCoordinates(-23.5505, -46.6333), "Coordenada válida é aceita");

        SourceEvent legend = SourceEvent.builder("import", "legend", SourceEvent.Kind.POKEMON)
                .coordinates(0.0, 0.0).build();
        check(legend.lat == null && legend.lon == null, "0,0 é descartada, não armazenada");
        check(legend.coordinatePrecision == SourceEvent.CoordinatePrecision.UNAVAILABLE,
                "0,0 fica com precisão indisponível");
        check(!legend.isActionable(now), "0,0 nunca pode ser destino de teleporte");

        // --- JSON round trip keeps null vs zero and provenance ---------------------------
        SourceEvent original = SourceEvent.builder("pokelist", "spawn-1", SourceEvent.Kind.POKEMON)
                .originKind(OriginKind.LIVE_EXTERNAL)
                .coordinates(-23.5505, -46.6333).pokemonId(25).formId(12)
                .name("Pikachu").ivPercent(87.5).ivAtk(14).ivDef(13).ivSta(12)
                .cp(431).level(21).gender("MALE").size("XXL").weatherBoost(true)
                .pvpGreatRank(3).shinyObserved(false)
                .observedAt(now - 60_000).retrievedAt(now).expiresAt(now + 300_000)
                .dataQuality(SourceEvent.QUALITY_PROVIDER_CLAIMED)
                .coverage("PokeList · consulta por área")
                .sourceUrl("https://moonani.com/PokeList/")
                .note("teste").build();
        JSONObject json = original.toJson();
        SourceEvent restored = SourceEvent.fromJson(json);
        check(restored.ivPercent == 87.5 && restored.ivAtk == 14, "IV e A/D/S preservados no JSON");
        check(restored.shinyObserved != null && !restored.shinyObserved, "shiny=false é distinto de ausente");
        check("pokelist".equals(restored.sourceId) && restored.originKind == OriginKind.LIVE_EXTERNAL,
                "Proveniência preservada");
        check(restored.expiresAt == original.expiresAt, "TTL preservado");
        check(restored.isActionable(now), "Registro válido e não expirado é acionável");
        check(!restored.isActionable(now + 300_001), "Registro expirado não é acionável");

        SourceEvent nullIv = SourceEvent.builder("iflowgo", "c", SourceEvent.Kind.POKEMON)
                .coordinates(-23.5, -46.6).build();
        SourceEvent restoredNullIv = SourceEvent.fromJson(nullIv.toJson());
        check(restoredNullIv.ivPercent == null, "Ausente continua null após o round trip");
        check(!nullIv.toJson().has("iv_percent"), "Campo ausente não é serializado como zero");

        // --- demo rows never become actions ----------------------------------------------
        SourceEvent demo = SourceEvent.builder("import", "demo-1", SourceEvent.Kind.POKEMON)
                .coordinates(-23.5, -46.6).isDemo(true).build();
        check(!demo.isActionable(now), "Demo não pode acionar teleporte");

        // --- approximate coordinates are not destinations --------------------------------
        SourceEvent approx = SourceEvent.builder("import", "approx", SourceEvent.Kind.RAID)
                .coordinates(-23.5, -46.6).precision(SourceEvent.CoordinatePrecision.APPROXIMATE)
                .expiresAt(now + 60_000).build();
        check(!approx.isActionable(now), "Coordenada aproximada não vira destino");

        SourceEvent unavailable = SourceEvent.fromJson(original.toJson().put("coordinate_precision", "unavailable"));
        check(!unavailable.isActionable(now), "Precisão indisponível permanece indisponível");
        check(!SourceEvent.fromJson(original.toJson().put("coordinate_precision", "invalid")).isActionable(now),
                "Precisão desconhecida não vira exata");
        check(SourceEvent.fromJson(original.toJson().put("origin_kind", "demo_fixture").put("is_demo", false)).isDemo,
                "Origem demonstrativa do gateway permanece demo");
        check(SourceEvent.integer(Double.NaN) == null && SourceEvent.integer(1e20) == null,
                "Número inválido não vira IV zero nem inteiro saturado");

        // --- distance --------------------------------------------------------------------
        SourceEvent near = SourceEvent.builder("import", "near", SourceEvent.Kind.POKEMON)
                .coordinates(-23.55, -46.63).build();
        double km = near.distanceKm(-23.55, -46.63);
        check(km < 0.05, "Distância de um ponto até ele mesmo é ~0, foi " + km);
        check(Double.isNaN(legend.distanceKm(-23.55, -46.63)), "Sem coordenada, distância é desconhecida");

        // --- query validation ------------------------------------------------------------
        try {
            SourceQuery.builder().iv(90.0, 10.0).build().validate();
            throw new AssertionError("IV invertido deve ser rejeitado");
        } catch (org.json.JSONException expected) {
            check(expected.getMessage().contains("mínimo"), "Mensagem de IV invertido: " + expected.getMessage());
        }
        try {
            SourceQuery.builder().attack(14, 12).build().validate();
            throw new AssertionError("A/D/S invertido deve ser rejeitado");
        } catch (org.json.JSONException expected) { /* ok */ }
        try {
            SourceQuery.builder().gender("OTHER").build().validate();
            throw new AssertionError("Sexo inválido deve ser rejeitado");
        } catch (org.json.JSONException expected) { /* ok */ }
        SourceQuery roundTrip = SourceQuery.fromJson(SourceQuery.builder()
                .pokemon(25, 26).iv(90.0, 100.0).pvp("GL", 10).size("XXL")
                .center(-23.5, -46.6, 5.0).minTtlSeconds(60L).build().toJson());
        check(roundTrip.pokemonIds.size() == 2 && roundTrip.ivMin == 90.0,
                "Query sobrevive ao round trip");
        java.util.List<String> criteria = roundTrip.activeCriteria();
        check(criteria.size() == 6, "Critérios ativos contados corretamente: " + criteria);
        for (String expected : new String[]{"espécies (2)", "IV mín 90%", "tamanho (1)",
                "PvP GL até #10", "raio 5 km", "TTL mín 60s"})
            check(criteria.contains(expected), "Critério ausente: " + expected + " em " + criteria);
        check(!SourceQuery.builder().build().isActive(), "Query padrão não tem critério ativo");

        // --- registry: exactly five integrations, none of them "connected" --------------
        java.util.Map<String, SourceCapabilities> all = SourceRegistry.all();
        check(all.size() == 7, "Cinco integrações + dois feeds do PGBR, foi " + all.size());
        for (String id : new String[]{"pokelist", "iflowgo", "pogocoordinates", "pgsharp", "ipogo"})
            check(SourceRegistry.isKnown(id), "Integração ausente: " + id);
        check(!SourceRegistry.isKnown("pokemond") && !SourceRegistry.isKnown("pgtools"),
                "Pokémod e PGTools não são integrações");
        for (SourceCapabilities capabilities : all.values())
            check(capabilities.status != SourceCapabilities.Status.CONNECTED,
                    "Nenhuma fonte pode nascer 'conectada': " + capabilities.sourceId);
        SourceCapabilities pokeList = SourceRegistry.of("pokelist");
        check(pokeList.isRemote(SourceCapabilities.SPECIES), "Espécie é filtro remoto na PokeList");
        check(!pokeList.isSupported(SourceCapabilities.IV_ADS)
                || pokeList.support(SourceCapabilities.IV_ADS) == SourceCapabilities.Support.LOCAL,
                "A/D/S não pode ser declarado como remoto na PokeList");
        check(!SourceRegistry.of("pgsharp").supportsRemoteSearch, "PGSharp é local-only");
        check(!SourceRegistry.of("ipogo").supportsRemoteSearch, "iPogo é local-only");
        check(SourceRegistry.of("pogocoordinates").defaultCoordinatePrecision
                == SourceEvent.CoordinatePrecision.UNAVAILABLE,
                "PoGoCoordinates pode devolver apenas reveal_code");
        check(SourceCapabilities.withStatus(pokeList, SourceCapabilities.Status.BLOCKED).status
                == SourceCapabilities.Status.BLOCKED, "Recusa rebaixa o status para bloqueado");
        check(!pokeList.unsupportedReason(SourceCapabilities.WEATHER).isEmpty(),
                "Filtro sem suporte precisa de explicação");

        // --- adapter support: refusal policy --------------------------------------------
        check(SourceAdapterSupport.isRefusal(401) && SourceAdapterSupport.isRefusal(403)
                && SourceAdapterSupport.isRefusal(429), "401/403/429 são recusas");
        check(!SourceAdapterSupport.isRefusal(500) && !SourceAdapterSupport.isRefusal(0),
                "5xx/rede não é recusa de acesso");
        SourceUnavailableException refused = SourceAdapterSupport.refusal("PokeList", 403, 0L);
        check(refused.isRefusal() && refused.httpStatus == 403, "Recusa carrega o status HTTP");
        check(SourceAdapterSupport.categoryFor(SourceQuery.builder().kind(SourceEvent.Kind.QUEST).build())
                == PokeListFeedData.Category.QUEST, "Categoria de quest mapeada");
        check(SourceAdapterSupport.categoryFor(SourceQuery.builder().kind(SourceEvent.Kind.RAID).build())
                == PokeListFeedData.Category.RAID, "Categoria de reide mapeada");
        check(SourceAdapterSupport.categoryFor(SourceQuery.builder().build())
                == PokeListFeedData.Category.POKEMON, "Categoria padrão é Pokémon");

        // Store envelope validation must happen before the first mutation.
        LocalEventStore storage = new LocalEventStore();
        check(storage.add(near), "Existing local record");
        String snapshot = storage.toJson();
        org.json.JSONArray excessive = new org.json.JSONArray();
        for (int i = 0; i <= LocalEventStore.MAX_EVENTS; i++)
            excessive.put(SourceEvent.builder("import", "extra-" + i, SourceEvent.Kind.ROUTE).build().toJson());
        for (JSONObject malformedStore : new JSONObject[]{
                new JSONObject(snapshot).put("version", 1.5),
                new JSONObject(snapshot).put("version", "1"),
                new JSONObject(snapshot).put("events", new JSONObject()),
                new JSONObject(snapshot).put("events", excessive)}) {
            try {
                storage.mergeJson(malformedStore.toString(), now);
                throw new AssertionError("Invalid store envelope accepted");
            } catch (org.json.JSONException expected) { /* ok */ }
            check(snapshot.equals(storage.toJson()), "Invalid envelope must preserve previous records");
        }
        JSONObject missingEvents = new JSONObject(snapshot);
        missingEvents.remove("events");
        try {
            storage.mergeJson(missingEvents.toString(), now);
            throw new AssertionError("Missing event list accepted");
        } catch (org.json.JSONException expected) { /* ok */ }
        check(snapshot.equals(storage.toJson()), "Missing list cannot mutate the store");

        System.out.println("SourceModelTest: OK");
    }
}
