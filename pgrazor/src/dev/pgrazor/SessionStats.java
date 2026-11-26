package dev.pgrazor;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONObject;

/**
 * Session stats: what the signed-in account gained in the last hour and the last 24 hours. The native side
 * reports the game's lifetime counters about once a minute (see session_stats.cpp); this keeps a rolling
 * history per account in its own preferences file, outside the settings backup. The counters are the
 * account's own, so they include play on other devices between readings.
 */
public final class SessionStats {
    private SessionStats() {}
    private static final Object LOCK = new Object();
    private static final long SAVE_MS = 5 * 60_000L;
    private static final String PREFS = "pgrazor-session-stats";
    private static final SessionStatsHistory history = new SessionStatsHistory();
    private static SharedPreferences store;
    private static String account = "";
    private static long savedAt;
    private static boolean dirty, writableHistory;

    private static String key() {
        String id = AccountProfiles.currentId();
        return id == null ? "" : id;
    }
    static void prepare(Context context) {
        if (context == null) return;
        synchronized (LOCK) {
            if (store != null) return;
            try { store = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE); }
            catch (RuntimeException unavailable) { return; }
            account = "";
            follow(System.currentTimeMillis());
        }
    }
    // Under LOCK: switches to the signed-in account's history.
    private static void follow(long now) {
        String current = key();
        if (current.equals(account)) return;
        persist(now);
        account = current;
        String stored = "";
        writableHistory = store != null && !current.isEmpty() && !"unknown".equals(current);
        if (writableHistory) {
            try { stored = store.getString(current, ""); }
            catch (ClassCastException incompatible) {
                // Preserve an incompatible stored value; use an in-memory history this session.
                writableHistory = false;
            }
        }
        history.load(stored, now);
        dirty = false; savedAt = now;
    }
    /** A native "session-stats" event: the counters read just now. */
    static void publish(String detail) {
        if (detail == null || detail.length() > 4096) return;
        double[] values = new double[SessionStatsHistory.READ_COUNT];
        try {
            JSONObject o = new JSONObject(detail);
            if (!o.has("xp")) return;
            values[SessionStatsHistory.XP] = o.getDouble("xp");
            values[SessionStatsHistory.CAUGHT] = o.getDouble("caught");
            values[SessionStatsHistory.SEEN] = o.getDouble("seen");
            values[SessionStatsHistory.STOPS] = o.getDouble("stops");
            values[SessionStatsHistory.KM] = o.getDouble("km");
            values[SessionStatsHistory.HATCHED] = o.getDouble("hatched");
            values[SessionStatsHistory.READ_BALANCE] = o.optDouble("dust", -1);
        } catch (Exception malformed) { return; }
        long now = System.currentTimeMillis();
        synchronized (LOCK) {
            follow(now);
            if (account.isEmpty() || "unknown".equals(account) || !AccountProfiles.allowed()) return;
            if (!history.record(now, values)) return;
            dirty = true;
            if (now < savedAt || now - savedAt >= SAVE_MS) persist(now);
        }
    }
    /** The change over the last {@code windowMs} for the signed-in account, or null before the first reading. */
    static SessionStatsHistory.Change change(long windowMs) {
        synchronized (LOCK) {
            follow(System.currentTimeMillis());
            if (account.isEmpty() || "unknown".equals(account) || !AccountProfiles.allowed()) return null;
            return history.change(windowMs);
        }
    }
    // Under LOCK: preserve incompatible existing values and retry dirty data if saving fails.
    private static void persist(long now) {
        if (store == null || !writableHistory || !dirty || account.isEmpty()) return;
        try {
            store.edit().putString(account, history.save()).apply();
            savedAt = now;
            dirty = false;
        } catch (RuntimeException unavailable) {
            // Keep the pending history in memory; do not log account identifiers.
        }
    }
    static void flush() {
        synchronized (LOCK) { persist(System.currentTimeMillis()); }
    }
    /** Explicit deletion of one inactive profile's statistics, not the shared preferences file. */
    static void deleteInactiveProfile(Context context, String id) throws java.io.IOException {
        synchronized (LOCK) {
            if (id == null || id.isEmpty() || "unknown".equals(id) || id.equals(AccountProfiles.currentId()) || id.equals(account))
                throw new java.io.IOException("Invalid inactive statistics profile");
            try {
                SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
                if (!prefs.edit().remove(id).commit()) throw new java.io.IOException("Statistics deletion not confirmed");
            } catch (RuntimeException unavailable) {
                throw new java.io.IOException("Statistics storage unavailable", unavailable);
            }
        }
    }
}
