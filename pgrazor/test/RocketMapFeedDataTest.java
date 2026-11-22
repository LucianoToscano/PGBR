package dev.pgrazor;

import org.json.JSONArray;
import org.json.JSONObject;

/** Offline fixtures for public RocketMap /raw_data fields; no third-party server contacted. */
public final class RocketMapFeedDataTest {
    static void ok(boolean truth, String message) {
        if (!truth) throw new AssertionError(message);
    }
    public static void main(String[] args) throws Exception {
        long now = 1_800_000_000_000L;
        JSONObject pokemon = new JSONObject().put("encounter_id", "18446744073709551615")
                .put("pokemon_id", 25).put("pokemon_name", "Pikachu")
                .put("latitude", 35.66).put("longitude", 139.70)
                .put("individual_attack", 15).put("individual_defense", 15)
                .put("individual_stamina", 15).put("cp", 500)
                .put("level", 24)
                .put("disappear_time", now + 120000);
        JSONObject unknown = new JSONObject().put("encounter_id", "unknown")
                .put("pokemon_id", 26).put("latitude", 35.67).put("longitude", 139.71)
                .put("disappear_time", now + 120000);
        JSONObject expired = new JSONObject().put("pokemon_id", 25)
                .put("latitude", 35.6).put("longitude", 139.8)
                .put("disappear_time", now - 120000);
        JSONObject badCoordinate = new JSONObject().put("pokemon_id", 25)
                .put("latitude", 140).put("longitude", 139.8)
                .put("disappear_time", now + 120000);
        LiveFeedData.Snapshot p = RocketMapFeedData.parse(new JSONObject()
                .put("pokemons", new JSONArray().put(pokemon).put(pokemon)
                        .put(unknown).put(expired).put(badCoordinate)),
                now, false);
        ok(p.entries.size() == 2, "dedupe, coordinate and expiry validation");
        LiveFeedData.Entry e = p.entries.get(0);
        ok(e.kind == LiveFeedData.Kind.POKEMON && e.dexId == 25, "dex matches");
        ok(e.iv == 100 && e.cp == 500 && e.expiresAt == now + 120000, "known stats and expiry");
        ok(e.level == 24, "authorized bridge level is preserved");
        ok(e.details.contains("Nv 24"), "confirmed level appears in details");
        ok(e.id.contains("18446744073709551615"), "64-bit encounter identity preserved as string");
        ok(p.entries.get(1).iv < 0 && !p.entries.get(1).hasStats, "unknown IV is not zero");
        ok(p.entries.get(1).attrs.atkIv < 0, "unknown component stays unknown");
        ok(p.entries.get(1).level < 0, "missing level stays unknown and never matches strict filter");
        ok(p.note.contains("RocketMap"), "provider attribution");

        JSONObject hatch = new JSONObject().put("end", now + 240000)
                .put("start", now - 60000).put("pokemon_id", 150).put("level", 5)
                .put("cp", 30000);
        JSONObject gym = new JSONObject().put("gym_id", "gym-a").put("name", "Ginásio Teste")
                .put("latitude", 35.7).put("longitude", 139.6).put("team_id", 1)
                .put("raid", hatch);
        JSONObject egg = new JSONObject().put("end", now + 360000)
                .put("start", now + 120000).put("pokemon_id", 249).put("level", 5);
        JSONObject gymEgg = new JSONObject().put("gym_id", "gym-b")
                .put("latitude", 35.71).put("longitude", 139.61).put("raid", egg);
        JSONObject staleGym = new JSONObject().put("gym_id", "gym-c")
                .put("latitude", 35.72).put("longitude", 139.62)
                .put("raid", new JSONObject().put("end", now - 100));
        LiveFeedData.Snapshot raids = RocketMapFeedData.parse(new JSONObject()
                .put("gyms", new JSONObject().put("gym-a", gym)
                        .put("gym-b", gymEgg).put("gym-c", staleGym)),
                now, true);
        ok(raids.entries.size() == 2, "active raids only");
        LiveFeedData.Entry active = raids.entries.get(0);
        ok(active.raidBossLive && active.dexId == 150 && active.team == 1,
                "boss confirmed after known hatch");
        LiveFeedData.Entry notHatched = raids.entries.get(1);
        ok(!notHatched.raidBossLive && notHatched.dexId == -1,
                "egg must not expose future boss as confirmed");
        ok(notHatched.details.contains("Ovo"), "egg label");
        ok(RocketMapFeedData.unixMillis(1800000000L) == 0,
                "seconds are not assumed to be rocketmap milliseconds");
        try {
            RocketMapFeedData.parse(new JSONObject().put("gyms", new JSONObject()), now, false);
            throw new AssertionError("missing pokemon field must not silently succeed");
        } catch (org.json.JSONException expected) {}
        try {
            RocketMapFeedData.parse(new JSONObject().put("pokemons", new JSONArray()), now, true);
            throw new AssertionError("missing gym field must not silently succeed");
        } catch (org.json.JSONException expected) {}
        System.out.println("PASS: RocketMap live Pokémon and Reides defensive contracts");
    }
}
