package dev.pgrazor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Decoder for the existing PGBR hundo JSON schema, independent of Android, UI and transport.
 *
 * "100% IV" is the PROVIDER'S claim. These rows are not re-checked against a game
 * encounter and do not establish that a spawn is currently present on the map.
 * No missing coordinates, IV, CP, level, timestamp or despawn is fabricated.
 */
public final class HundoFeedData {
    private HundoFeedData() {}

    public static final long MAX_SNAPSHOT_AGE_MS = 5 * 60_000L;
    private static final long MAX_FUTURE_SKEW_MS = 60_000L;
    private static final long MAX_REMAINING_MS = 24 * 60 * 60_000L;
    public static final int MAX_RESULTS = 10_000;
    private static final int MAX_ROWS_INSPECTED = 50_000;
    /**
     * IV asserted by this feed schema for every row it returns. It is the provider's claim inherited
     * from the {"v":1,"hundos":[...]} format, NOT a value re-read from a game encounter.
     */
    public static final int FEED_CLAIMED_IV = 100;

    public static final class Entry {
        public final String encounterId;
        public final int dex, form, cp;
        /** Provider-asserted IV; {@link #FEED_CLAIMED_IV} for this schema, -1 when not stated. */
        public final int iv;
        public final double level, lat, lon;
        public final long despawnMs;

        Entry(String encounterId, int dex, int form, double lat, double lon,
              long despawnMs, int cp, double level, int iv) {
            this.encounterId = encounterId;
            this.dex = dex;
            this.form = form;
            this.lat = lat;
            this.lon = lon;
            this.despawnMs = despawnMs;
            this.cp = cp;
            this.level = level;
            this.iv = iv;
        }

        public boolean expired(long now) { return despawnMs <= now; }
    }

    public static final class Snapshot {
        public final List<Entry> entries;
        public final long fetchedMs;
        public final int intervalSeconds;
        Snapshot(List<Entry> entries, long fetchedMs, int intervalSeconds) {
            this.entries = Collections.unmodifiableList(new ArrayList<>(entries));
            this.fetchedMs = fetchedMs;
            this.intervalSeconds = intervalSeconds;
        }
    }

    /**
     * Input: {"v":1,"fetched_ms":...,"interval_s":30,
     * "hundos":[[encounterId,dex,form,lat,lon,despawnMs,cp,level],...]}.
     *
     * Missing or stale fetched_ms fails closed: an old cached feed must not be
     * presented as live. Unknown IV is NEVER promoted to 100% by this parser;
     * the named source itself asserts its hundo-only selection.
     */
    public static Snapshot parse(JSONObject root, long now) throws JSONException {
        if (root == null || root.optInt("v", -1) != 1)
            throw new JSONException("Formato de feed 100% IV desconhecido.");
        long fetched = root.optLong("fetched_ms", 0L);
        if (fetched <= 0 || fetched > now + MAX_FUTURE_SKEW_MS
                || now - fetched > MAX_SNAPSHOT_AGE_MS)
            throw new JSONException("Feed 100% IV sem atualização recente verificável.");
        JSONArray rows = root.optJSONArray("hundos");
        if (rows == null)
            throw new JSONException("Lista de Pokémon 100% IV ausente.");
        int interval = root.optInt("interval_s", 30);
        if (interval < 15 || interval > 3600) interval = 30;
        Set<String> seen = new HashSet<>();
        List<Entry> result = new ArrayList<>();
        int count = Math.min(rows.length(), MAX_ROWS_INSPECTED);
        for (int i = 0; i < count && result.size() < MAX_RESULTS; i++) {
            JSONArray row = rows.optJSONArray(i);
            if (row == null || row.length() < 6) continue;
            String id = row.optString(0, "").trim();
            if (!id.matches("[0-9]{1,22}") || seen.contains(id)) continue;
            int dex = row.optInt(1, -1);
            if (dex < 1 || dex > 1025) continue;
            double lat = row.optDouble(3, Double.NaN);
            double lon = row.optDouble(4, Double.NaN);
            if (!Double.isFinite(lat) || !Double.isFinite(lon)
                    || Math.abs(lat) > 90 || Math.abs(lon) > 180) continue;
            long despawn = row.optLong(5, 0L);
            if (despawn <= now || despawn - now > MAX_REMAINING_MS) continue;
            int form = row.optInt(2, -1);
            if (form < 0) form = -1;
            int cp = row.length() > 6 ? row.optInt(6, -1) : -1;
            if (cp < 1 || cp > 10000) cp = -1;
            double level = row.length() > 7 ? row.optDouble(7, Double.NaN) : Double.NaN;
            if (!Double.isFinite(level) || level < 1 || level > 50) level = Double.NaN;
            seen.add(id);
            result.add(new Entry(id, dex, form, lat, lon, despawn, cp, level, FEED_CLAIMED_IV));
        }
        return new Snapshot(result, fetched, interval);
    }
}
