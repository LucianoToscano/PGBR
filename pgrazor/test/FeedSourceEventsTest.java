package dev.pgrazor;

import org.json.JSONArray;
import org.json.JSONObject;

/** Category-aware parsing for PokeList and page-aware parsing for iFlowGo (fixtures are synthetic). */
public final class FeedSourceEventsTest {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static final long NOW = 1_800_000_000_000L;

    public static void main(String[] args) throws Exception {
        // --- PokeList: five categories, each with its own kind --------------------------
        JSONObject pokemon = new JSONObject().put("type", "pokemon").put("pokemons", new JSONArray()
                .put(new JSONObject().put("id", "p1").put("number", 25).put("name", "Pikachu")
                        .put("latitude", -23.55).put("longitude", -46.63).put("cp", 500)
                        .put("level", 25).put("iv", 100).put("attack", 15).put("defense", 15)
                        .put("hp", 15).put("endTime", (NOW + 120_000) / 1000L))
                .put(new JSONObject().put("id", "p2").put("number", 25)
                        .put("latitude", 0).put("longitude", 0))
                .put(new JSONObject().put("id", "p1").put("number", 26)
                        .put("latitude", -23.56).put("longitude", -46.64)));
        PokeListFeedData.EventPage pokemonPage =
                PokeListFeedData.parseEvents(pokemon, NOW, PokeListFeedData.Category.POKEMON);
        check(pokemonPage.events.size() == 1, "Um spawn válido, foi " + pokemonPage.events.size());
        check(pokemonPage.received == 3, "Recebidos conta a resposta bruta");
        check(pokemonPage.rejected == 2, "0,0 e duplicata rejeitados");
        SourceEvent spawn = pokemonPage.events.get(0);
        check(spawn.kind == SourceEvent.Kind.POKEMON, "Tipo Pokémon");
        check(spawn.ivPercent == 100.0 && spawn.ivAtk == 15, "IV e A/D/S lidos");
        check(spawn.shinyObserved == null, "shiny ausente não vira confirmação");
        check("pokelist".equals(spawn.sourceId), "Proveniência PokeList");

        JSONObject raid = new JSONObject().put("type", "raid").put("pokemons", new JSONArray()
                .put(new JSONObject().put("id", "r1").put("number", 384).put("name", "Ginásio Central")
                        .put("latitude", -23.55).put("longitude", -46.63).put("level", 5)
                        .put("endTime", (NOW + 1800_000) / 1000L)));
        PokeListFeedData.EventPage raidPage =
                PokeListFeedData.parseEvents(raid, NOW, PokeListFeedData.Category.RAID);
        check(raidPage.events.get(0).kind == SourceEvent.Kind.RAID, "Reide vira tipo raid");
        check(raidPage.events.get(0).pokemonId == 384 && raidPage.events.get(0).level == 5,
                "Chefe e nível da reide lidos");

        JSONObject quest = new JSONObject().put("type", "quest").put("pokemons", new JSONArray()
                .put(new JSONObject().put("id", "q1").put("name", "Praça")
                        .put("latitude", -23.55).put("longitude", -46.63)
                        .put("reward", "50 Pokébolas")));
        PokeListFeedData.EventPage questPage =
                PokeListFeedData.parseEvents(quest, NOW, PokeListFeedData.Category.QUEST);
        check(questPage.events.get(0).kind == SourceEvent.Kind.QUEST, "Pesquisa vira tipo quest");
        check(questPage.events.get(0).note.contains("50 Pokébolas"), "Recompensa preservada");

        JSONObject rocket = new JSONObject().put("type", "rocket").put("pokemons", new JSONArray()
                .put(new JSONObject().put("id", "k1").put("latitude", -23.55).put("longitude", -46.63)
                        .put("rocket_type", "male")
                        .put("endTime", (NOW + 900_000) / 1000L)));
        PokeListFeedData.EventPage rocketPage =
                PokeListFeedData.parseEvents(rocket, NOW, PokeListFeedData.Category.ROCKET);
        check(rocketPage.events.get(0).kind == SourceEvent.Kind.ROCKET, "Invasão vira tipo rocket");

        JSONObject leader = new JSONObject().put("type", "rocket_leader").put("pokemons", new JSONArray()
                .put(new JSONObject().put("id", "l1").put("name", "Cliff")
                        .put("latitude", -23.55).put("longitude", -46.63).put("rocket_type", "cliff")
                        .put("endTime", (NOW + 900_000) / 1000L)));
        PokeListFeedData.EventPage leaderPage =
                PokeListFeedData.parseEvents(leader, NOW, PokeListFeedData.Category.ROCKET_LEADER);
        check(leaderPage.events.get(0).kind == SourceEvent.Kind.ROCKET, "Líder continua tipo rocket");
        check(leaderPage.events.get(0).note.contains("cliff"), "Líder identificado");

        // A provider that answers with another type is a contract mismatch, not a silent success.
        try {
            PokeListFeedData.parseEvents(raid, NOW, PokeListFeedData.Category.POKEMON);
            throw new AssertionError("Tipo divergente deve ser rejeitado");
        } catch (org.json.JSONException expected) {
            check(expected.getMessage().contains("tipo"), "Motivo do tipo divergente: " + expected.getMessage());
        }

        // The legacy parser keeps its old contract (LiveFeedData rows).
        check(PokeListFeedData.parse(pokemon, NOW).entries.size() == 1,
                "Parser legado do LiveFeedData preservado");

        // --- iFlowGo: pagination is copied from the response, never assumed --------------
        JSONObject globalFirst = new JSONObject().put("items", new JSONArray()
                .put(new JSONObject().put("id", "g1").put("pokemon_id", 25)
                        .put("lat", -23.55).put("lon", -46.63).put("percent_iv", 100)
                        .put("cp", 500).put("level", 20).put("attack_iv", 15)
                        .put("defense_iv", 15).put("stamina_iv", 15).put("gender", 1)
                        .put("expires_at", "2027-01-15T12:00:00Z")))
                .put("total", 75).put("total_pages", 3).put("page_size", 25).put("page", 1);
        IFlowGoFeedData.EventPage first = IFlowGoFeedData.parseEvents(globalFirst, NOW, false, true);
        check(first.hasMore, "total_pages=3 com page=1 informa mais páginas");
        check(first.totalKnown == 75 && first.totalPages == 3, "Totais declarados preservados");
        check(first.events.size() == 1 && first.events.get(0).ivPercent == 100.0,
                "Spawn global normalizado");
        check(first.note.contains("página 1 de 3"), "Nota mostra a página real: " + first.note);

        JSONObject globalLast = new JSONObject().put("items", new JSONArray())
                .put("total", 75).put("total_pages", 3).put("page_size", 25).put("page", 3);
        IFlowGoFeedData.EventPage last = IFlowGoFeedData.parseEvents(globalLast, NOW, false, true);
        check(!last.hasMore, "Última página não promete mais resultados");

        JSONObject single = new JSONObject().put("items", new JSONArray()).put("total_pages", 1).put("page", 1);
        IFlowGoFeedData.EventPage singlePage = IFlowGoFeedData.parseEvents(single, NOW, false, true);
        check(!singlePage.hasMore, "25 itens em uma página não viram varredura global");

        JSONObject noPaging = new JSONObject().put("items", new JSONArray());
        IFlowGoFeedData.EventPage unknownPaging = IFlowGoFeedData.parseEvents(noPaging, NOW, false, true);
        check(!unknownPaging.hasMore && unknownPaging.totalKnown == null,
                "Sem total informado, nada é presumido");
        check(unknownPaging.note.contains("não informado"), "Nota declara a ausência de total");

        // --- iFlowGo: nearby layers stay separate ---------------------------------------
        JSONObject nearby = new JSONObject().put("spawns", new JSONArray()
                .put(new JSONObject().put("id", "s1").put("pokemon_id", 25)
                        .put("lat", -23.55).put("lon", -46.63).put("percent_iv", 87.5)
                        .put("cp", 431).put("level", 21).put("gender", 2)
                        .put("expires_at", "2027-01-15T12:00:00Z")));
        IFlowGoFeedData.EventPage spawns = IFlowGoFeedData.parseEvents(nearby, NOW, false, false);
        check(spawns.events.size() == 1 && spawns.events.get(0).kind == SourceEvent.Kind.POKEMON,
                "Camada de spawns");

        JSONObject raidLayer = new JSONObject().put("raids", new JSONArray()
                .put(new JSONObject().put("id", "rr1").put("gym_name", "Ginásio")
                        .put("lat", -23.55).put("lon", -46.63).put("raid_pokemon_id", 384)
                        .put("raid_level", 5).put("raid_end_at", "2027-01-15T12:00:00Z")
                        .put("raid_battle_at", "2027-01-15T11:00:00Z")));
        IFlowGoFeedData.EventPage raids = IFlowGoFeedData.parseEvents(raidLayer, NOW, true, false);
        check(raids.events.size() == 1 && raids.events.get(0).kind == SourceEvent.Kind.RAID,
                "Camada de reides");

        try {
            IFlowGoFeedData.parseEvents(new JSONObject(), NOW, false, false);
            throw new AssertionError("Campo ausente deve ser erro");
        } catch (org.json.JSONException expected) { /* ok */ }

        System.out.println("FeedSourceEventsTest: OK");
    }
}
