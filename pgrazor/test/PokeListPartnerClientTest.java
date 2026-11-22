package dev.pgrazor;

import java.time.Instant;
import org.json.JSONArray;
import org.json.JSONObject;

/** Tests the partner fixture contract without contacting a third-party server. */
public final class PokeListPartnerClientTest {
    private static int checks;
    private static void yes(boolean condition, String why) {
        checks++;
        if (!condition) throw new AssertionError(why);
    }

    private static JSONObject entry(long now) throws Exception {
        return new JSONObject().put("event_id", "spawn-25")
                .put("kind", "pokemon").put("pokemon_id", 25).put("name", "Pikachu")
                .put("latitude", -5.79).put("longitude", -35.21)
                .put("coordinate_precision", "exact")
                .put("iv_percent", 100).put("attack_iv", 15)
                .put("defense_iv", 15).put("stamina_iv", 15)
                .put("cp", 700).put("level", 27)
                .put("shiny_status", "eligible").put("quality", "observed")
                .put("source", "authorized_scanner")
                .put("observed_at", Instant.ofEpochMilli(now - 1000L).toString())
                .put("expires_at", Instant.ofEpochMilli(now + 300000L).toString());
    }

    private static PokeListPartnerClient.Page page(JSONObject row, long now) throws Exception {
        return PokeListPartnerClient.parsePage(new JSONObject()
                .put("kind", "pokemon")
                .put("has_more", false)
                .put("items", new JSONArray().put(row)), now);
    }

    public static void main(String[] args) throws Exception {
        long now = System.currentTimeMillis();
        JSONObject valid = entry(now);
        PokeListPartnerClient.Page first = page(valid, now);
        yes(first.rows.size() == 1, "valid spawn included");
        SpawnContract.Spawn p = first.rows.get(0);
        yes(p.hasLocation(), "exact verified coordinates included");
        yes(p.perfect(), "15/15/15 observed");
        yes(p.providerSaysPerfect(), "source 100 percent");
        yes(p.shinyPossible && !p.shinyConfirmed, "shiny eligible does not mean caught shiny");
        yes(p.observedAt < p.despawnAt, "timestamps retained");
        yes("pokelist-partner".equals(p.sourceId), "isolated source");
        yes(!first.hasMore, "no remaining page");

        JSONObject estimated = entry(now).put("quality", "estimated");
        p = page(estimated, now).rows.get(0);
        yes(!p.hasLocation(), "estimated observation cannot drive teleport");
        yes(!p.hasIvComponents() && p.ivPercent == SpawnContract.UNKNOWN,
                "estimated IV is not certified");
        JSONObject rounded = entry(now).put("coordinate_precision", "rounded");
        yes(!page(rounded, now).rows.get(0).hasLocation(),
                "rounded coordinates cannot drive teleport");
        JSONObject demo = entry(now).put("source", "smoke_fixture");
        yes(page(demo, now).rows.isEmpty(), "fixture never displayed as live");
        yes(page(entry(now).put("demo", true), now).rows.isEmpty(), "demo flagged row skipped");
        yes(page(entry(now).put("longitude", 0).put("latitude", 0), now)
                .rows.get(0).coordinatesStatus == SpawnContract.Coordinates.INVALID,
                "Null Island blocked");
        yes(page(entry(now).put("expires_at", Instant.ofEpochMilli(now - 1000L).toString()),
                now).rows.isEmpty(), "expired excluded");
        yes(page(entry(now).put("observed_at",
                Instant.ofEpochMilli(now + 7200000L).toString()), now).rows.isEmpty(),
                "observation from distant future excluded");

        boolean failed = false;
        try { PokeListPartnerClient.safeEndpoint("http://insecure.example"); }
        catch (LiveFeedClient.FeedException ok) { failed = true; }
        yes(failed, "http rejected");
        failed = false;
        try { PokeListPartnerClient.safeEndpoint("https://u:p@example.org"); }
        catch (LiveFeedClient.FeedException ok) { failed = true; }
        yes(failed, "basic auth in URL rejected");
        failed = false;
        try { PokeListPartnerClient.safeEndpoint("https://example.org/v1/search/pokemon"); }
        catch (LiveFeedClient.FeedException ok) { failed = true; }
        yes(failed, "endpoint path rejected");
        yes("https://example.org".equals(PokeListPartnerClient.safeEndpoint(
                "https://example.org/")), "good origin canonical");

        JSONObject licensed = new JSONObject().put("licensing",
                new JSONObject().put("status", "approved")).put("data_layers",
                new JSONArray().put(new JSONObject().put("layer", "live_provider_feed")
                        .put("status", "configured")));
        PokeListPartnerClient.ensureLiveAndLicensed(licensed);
        yes(true, "approved + configured accepted");

        failed = false;
        try { PokeListPartnerClient.ensureLiveAndLicensed(
                new JSONObject().put("licensing",
                        new JSONObject().put("status", "pending_owner_approval"))); }
        catch (LiveFeedClient.FeedException ok) { failed = true; }
        yes(failed, "pending licence rejected");
        failed = false;
        try { PokeListPartnerClient.ensureLiveAndLicensed(
                licensed.put("data_layers", new JSONArray())); }
        catch (LiveFeedClient.FeedException ok) { failed = true; }
        yes(failed, "no live feed rejected");

        System.out.println("PASS: PokeListPartnerClientTest " + checks + " verificações");
    }
}
