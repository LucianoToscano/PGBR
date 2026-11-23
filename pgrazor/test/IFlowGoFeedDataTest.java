package dev.pgrazor;

import org.json.JSONArray;
import org.json.JSONObject;

/** Contract fixtures use field names observed from live iFlowGo responses on 2026-11-21. */
public final class IFlowGoFeedDataTest {
    static void check(boolean ok, String text) {
        if (!ok) throw new AssertionError(text);
    }

    public static void main(String[] args) throws Exception {
        long now = 1_800_000_000_000L;
        JSONObject pokemon = new JSONObject().put("id", "spawn-live")
            .put("pokemon_id", 25).put("lat", 35.66).put("lon", 139.70)
            .put("cp", 512).put("level", 21).put("percent_iv", 100.0)
            .put("attack_iv", 15).put("defense_iv", 15).put("stamina_iv", 15)
            .put("expires_at", "2027-01-16T08:02:00Z");
        JSONObject invalid = new JSONObject().put("pokemon_id", 25)
            .put("lat", 200).put("lon", 139).put("expires_at", "2027-01-16T08:02:00Z");
        JSONObject expired = new JSONObject().put("pokemon_id", 25)
            .put("lat", 35.67).put("lon", 139.71).put("expires_at", "2026-01-01T00:00:00Z");
        JSONObject noStats = new JSONObject().put("id", "no-stats")
            .put("pokemon_id", 26).put("lat", 35.6).put("lon", 139.7)
            .put("expires_at", "2027-01-16T08:02:00+00:00");
        JSONArray spawns = new JSONArray().put(pokemon).put(pokemon).put(invalid).put(expired).put(noStats);
        LiveFeedData.Snapshot snapshot = IFlowGoFeedData.parse(
            new JSONObject().put("spawns", spawns), now, false);
        check(snapshot.entries.size() == 2, "valid deduped spawns without faked values");
        LiveFeedData.Entry p = snapshot.entries.get(0);
        check(p.kind == LiveFeedData.Kind.POKEMON && p.dexId == 25, "actual ID");
        check(p.iv == 100 && p.cp == 512 && p.level == 21, "actual IV/CP/level");
        check(p.attrs.atkIv == 15 && p.attrs.staIv == 15, "actual IV components");
        check(p.expiresAt > now && p.expiresAt < now + 70L * 24L * 60L * 60L * 1000L,
            "ISO expiry is milliseconds");
        check(snapshot.entries.get(1).iv < 0, "unreported IV must remain unknown");
        check(snapshot.entries.get(1).id.startsWith("iflowgo:spawn:"), "source prefixes ids");
        check(snapshot.note.contains("iFlowGo"), "source attribution");
        LiveFeedData.Snapshot global = IFlowGoFeedData.parseGlobal(
            new JSONObject().put("items", new JSONArray().put(pokemon)), now);
        check(global.entries.size() == 1 && global.entries.get(0).dexId == 25,
            "world page reuses real spawn fields");
        check(global.note.contains("busca global"), "world search labelled");
        check(global.note.contains("total desconhecido"),
            "no server total means the app must not claim more pages");
        // Pagination contract: two pages requested, three available -> the note must say so and
        // point to the way of narrowing the search instead of pretending this is worldwide.
        LiveFeedData.Snapshot paged = IFlowGoFeedData.parseGlobal(
            new JSONObject().put("items", new JSONArray().put(pokemon)), now, 2, 2, 3);
        check(paged.note.contains("páginas 2 de 3"), "pages shown/available stated");
        check(paged.note.contains("mais resultados"), "hidden pages are disclosed");
        check(paged.note.contains("estreite a busca"), "the user is told how to reach them");
        LiveFeedData.Snapshot complete = IFlowGoFeedData.parseGlobal(
            new JSONObject().put("items", new JSONArray().put(pokemon)), now, 1, 1, 1);
        check(!complete.note.contains("mais resultados"),
            "a complete search must not warn about missing pages");
        // hasNextPage uses the server's own total, never a guess.
        check(IFlowGoFeedData.hasNextPage(new JSONObject().put("total", 450), 2, 200),
            "450 items, 2 pages of 200 -> page 3 exists");
        check(!IFlowGoFeedData.hasNextPage(new JSONObject().put("total", 400), 2, 200),
            "400 items -> exactly two pages");
        check(!IFlowGoFeedData.hasNextPage(new JSONObject().put("items", new JSONArray()), 1, 200),
            "no total is not evidence of a next page");
        JSONObject raid = new JSONObject().put("id", "raid-live").put("gym_name", "Ginásio Teste")
            .put("lat", 35.65).put("lon", 139.70).put("raid_pokemon_id", 150)
            .put("raid_level", 5)
            .put("raid_battle_at", "2026-12-01T12:00:00Z")
            .put("raid_end_at", "2027-01-16T08:02:00Z")
            .put("ex_raid_eligible", true);
        LiveFeedData.Snapshot raids = IFlowGoFeedData.parse(
            new JSONObject().put("raids", new JSONArray().put(raid)), now, true);
        check(raids.entries.size() == 1, "live raid recorded");
        LiveFeedData.Entry r = raids.entries.get(0);
        check(r.kind == LiveFeedData.Kind.RAID && r.expiresAt > now, "raid kind/end");
        check(r.raidBossLive && r.dexId == 150 && r.level == 5, "confirmed boss and tier");
        check(r.details.contains("Ginásio EX elegível"), "true source flag only");
        JSONObject egg = new JSONObject().put("id", "egg").put("lat", 35.65)
            .put("lon", 139.71).put("raid_level", 5).put("raid_pokemon_id", 150)
            .put("raid_battle_at", "2027-01-16T06:00:00Z")
            .put("raid_end_at", "2027-01-16T08:00:00Z");
        LiveFeedData.Entry e = IFlowGoFeedData.parse(
            new JSONObject().put("raids", new JSONArray().put(egg)), now, true).entries.get(0);
        check(!e.raidBossLive && e.dexId == -1, "egg does not reveal a speculative boss");
        JSONObject unknownHatch = new JSONObject().put("id", "raid-hatch-unknown")
            .put("lat", 35.65).put("lon", 139.72).put("raid_pokemon_id", 150)
            .put("raid_end_at", "2027-01-16T08:00:00Z");
        LiveFeedData.Entry unknown = IFlowGoFeedData.parse(
            new JSONObject().put("raids", new JSONArray().put(unknownHatch)), now, true)
            .entries.get(0);
        check(!unknown.raidBossLive && unknown.dexId == -1,
            "unknown hatch time must not confirm raid boss");
        check(e.details.contains("Ovo"), "egg text");
        check(IFlowGoFeedData.isoMs("2026-11-21T06:00:00.123456Z")
            == IFlowGoFeedData.isoMs("2026-11-21T06:00:00.123Z"), "microseconds converted");
        try {
            IFlowGoFeedData.parse(new JSONObject().put("spawns", new JSONArray()), now, true);
            throw new AssertionError("wrong group must not be treated as empty results");
        } catch (org.json.JSONException expected) {}
        System.out.println("PASS: iFlowGo live Pokémon and raid feed contracts");
    }
}
