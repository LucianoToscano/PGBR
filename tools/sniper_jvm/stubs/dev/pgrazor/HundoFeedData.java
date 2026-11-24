package dev.pgrazor;
import org.json.JSONObject;
public final class HundoFeedData {
    public static final int FEED_CLAIMED_IV = 100;
    public static final long MAX_SNAPSHOT_AGE_MS = 300000L;
    public static final class Entry {
        public final String encounterId; public final int dex, form, cp; public final double lat, lon, level;
        public final long despawnMs;
        Entry(String id, int dex, int form, double lat, double lon, long despawn, int cp, double level, int iv) {
            this.encounterId = id; this.dex = dex; this.form = form; this.lat = lat; this.lon = lon;
            this.despawnMs = despawn; this.cp = cp; this.level = level;
        }
    }
    public static final class Snapshot {
        public final java.util.List<Entry> entries; public final long fetchedMs;
        Snapshot(java.util.List<Entry> entries, long fetchedMs) { this.entries = entries; this.fetchedMs = fetchedMs; }
    }
    public static Snapshot parse(JSONObject root, long now) throws org.json.JSONException { return null; }
}
