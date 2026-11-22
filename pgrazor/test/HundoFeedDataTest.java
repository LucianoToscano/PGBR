package dev.pgrazor;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Offline, synthetic-only verification of the PGBR manual 100% IV schema. */
public final class HundoFeedDataTest {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static JSONArray row(String id, int dex, double lat, double lon, long despawn) {
        return new JSONArray().put(id).put(dex).put(0).put(lat).put(lon)
                .put(despawn).put(900).put(33.5);
    }

    private static JSONObject feed(long now, JSONArray rows) throws JSONException {
        return new JSONObject().put("v", 1).put("fetched_ms", now - 1000)
                .put("interval_s", 30).put("hundos", rows);
    }

    private static void reject(JSONObject payload, long now, String what) throws Exception {
        boolean failed = false;
        try { HundoFeedData.parse(payload, now); }
        catch (JSONException error) { failed = true; }
        check(failed, "Must reject " + what);
    }

    public static void main(String[] args) throws Exception {
        long now = 1_800_000_000_000L;
        JSONArray rows = new JSONArray()
                .put(row("111", 25, -6.2, -35.2, now + 120_000))
                .put(row("111", 25, -6.2, -35.2, now + 120_000))  // duplicate
                .put(row("112", 1, 91, 10, now + 120_000))       // bad latitude
                .put(row("113", 1, 0, -181, now + 120_000))      // bad longitude
                .put(row("114", 1, 0, 0, now - 10))               // expired
                .put(row("115", 1026, 0, 0, now + 120_000))       // unknown dex
                .put(row("116", 1, 0, 0, now + 5L*86400000L))     // improbable lifetime
                .put(row("117", 4, 40, 30, now + 120_000))       // valid
                .put(new JSONArray().put("118").put(5).put(0)
                        .put(-10).put(-10).put(now + 90_000));    // optional stats missing
        HundoFeedData.Snapshot snap = HundoFeedData.parse(feed(now, rows), now);
        check(snap.entries.size() == 3, "valid, unique, unexpired results only");
        check(snap.entries.get(0).dex == 25 && snap.entries.get(0).cp == 900,
                "dex/cp preserved");
        check(!snap.entries.get(0).expired(now), "valid entry is not expired");
        check(snap.entries.get(2).cp == -1
                    && Double.isNaN(snap.entries.get(2).level),
                "missing CP/level must remain unknown");
        check(snap.fetchedMs == now - 1000 && snap.intervalSeconds == 30,
                "metadata preserved");
        boolean immutable = false;
        try { snap.entries.clear(); }
        catch (UnsupportedOperationException expected) { immutable = true; }
        check(immutable, "snapshot must be immutable");
        check(HundoFeedData.parse(feed(now, new JSONArray()), now).entries.isEmpty(),
                "empty response is valid, not fabricated");
        reject(feed(now - 400_000, rows), now, "stale feed");
        reject(feed(now + 120_000, rows), now, "future feed");
        reject(new JSONObject().put("v", 2).put("hundos", rows), now, "schema version");
        reject(new JSONObject().put("v", 1).put("fetched_ms", now), now, "missing rows");
        JSONObject fallback = feed(now, new JSONArray()).put("interval_s", 1);
        check(HundoFeedData.parse(fallback, now).intervalSeconds == 30,
                "invalid interval defaults safely");
        System.out.println("HundoFeedDataTest passed");
    }
}
