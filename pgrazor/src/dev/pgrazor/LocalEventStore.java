package dev.pgrazor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Bounded, in-process store for imported events. It deduplicates by {@code source|event} id, refuses
 * rows without a stable identity, prunes expired rows on demand and round-trips through JSON so the
 * caller can keep it in app-private preferences. Nothing here performs network I/O.
 *
 * <p>Imported data keeps {@link OriginKind#PERSONAL_IMPORT}: storing it does not turn it into a
 * live feed, and the UI labels it accordingly.
 */
public final class LocalEventStore {
    public static final int MAX_EVENTS = 2000;
    public static final String STORE_FORMAT = "PGBR local events";
    public static final int STORE_VERSION = 1;

    private final Map<String, SourceEvent> events = new LinkedHashMap<>();

    /** Adds one event; returns {@code false} when the same identity is already stored. */
    public synchronized boolean add(SourceEvent event) {
        if (event == null || blankIdentity(event.sourceId) || blankIdentity(event.eventId)) return false;
        String key = key(event);
        if (events.containsKey(key)) return false;
        if (events.size() >= MAX_EVENTS) {
            // Drop the oldest entry instead of refusing the whole import.
            String oldest = events.keySet().iterator().next();
            events.remove(oldest);
        }
        events.put(key, event);
        return true;
    }

    /** Adds every event, returning how many were new (duplicates are skipped, not errors). */
    public synchronized int addAll(List<SourceEvent> list) {
        if (list == null) return 0;
        int added = 0;
        for (SourceEvent event : list) if (add(event)) added++;
        return added;
    }

    public synchronized List<SourceEvent> all() {
        return Collections.unmodifiableList(new ArrayList<>(events.values()));
    }

    /** Events that are not expired at {@code now}; unknown expiry counts as not expired. */
    public synchronized List<SourceEvent> live(long now) {
        List<SourceEvent> out = new ArrayList<>();
        for (SourceEvent event : events.values()) if (!event.expired(now)) out.add(event);
        return Collections.unmodifiableList(out);
    }

    /** Drops expired rows and returns how many were removed. */
    public synchronized int prune(long now) {
        int removed = 0;
        List<String> gone = new ArrayList<>();
        for (Map.Entry<String, SourceEvent> entry : events.entrySet())
            if (entry.getValue().expired(now)) gone.add(entry.getKey());
        for (String key : gone) { events.remove(key); removed++; }
        return removed;
    }

    public synchronized int size() { return events.size(); }

    public synchronized void clear() { events.clear(); }

    /** Only rows of one source (used by the PGSharp/iPogo local adapters). */
    public synchronized List<SourceEvent> bySource(String sourceId) {
        List<SourceEvent> out = new ArrayList<>();
        for (SourceEvent event : events.values()) if (event.sourceId.equals(sourceId)) out.add(event);
        return Collections.unmodifiableList(out);
    }

    private static boolean blankIdentity(String value) {
        return value == null || value.codePoints().allMatch(cp -> Character.isWhitespace(cp) || Character.isSpaceChar(cp));
    }

    private static String key(SourceEvent event) {
        return event.sourceId.length() + ":" + event.sourceId + event.eventId;
    }

    public synchronized String toJson() {
        try {
            JSONObject root = new JSONObject();
            root.put("format", STORE_FORMAT);
            root.put("version", STORE_VERSION);
            root.put("events", SourceEvent.toJsonArray(new ArrayList<>(events.values())));
            return root.toString();
        } catch (JSONException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    /** Reads a stored document, ignoring unknown keys and rows that no longer validate. */
    public synchronized int mergeJson(String json, long now) throws JSONException {
        if (json == null || json.trim().isEmpty()) return 0;
        JSONObject root = new JSONObject(json);
        if (!STORE_FORMAT.equals(root.optString("format")))
            throw new JSONException("Formato de armazenamento desconhecido");
        Object version = root.opt("version");
        if (!(version instanceof Number) || ((Number) version).doubleValue() != STORE_VERSION)
            throw new JSONException("Versão de armazenamento desconhecida");
        JSONArray rows = root.optJSONArray("events");
        if (rows == null) throw new JSONException("Lista de eventos ausente ou inválida");
        if (rows.length() > MAX_EVENTS)
            throw new JSONException("Lista de eventos excede o limite de " + MAX_EVENTS);
        int added = 0;
        int limit = rows.length();
        for (int i = 0; i < limit; i++) {
            JSONObject row = rows.optJSONObject(i);
            if (row == null) continue;
            try {
                SourceEvent event = SourceEvent.fromJson(row);
                if (event.expired(now)) continue; // stale imports never resurrect
                if (add(event)) added++;
            } catch (JSONException invalidRow) {
                // Keep recovering valid rows; this read does not modify the original document.
            }
        }
        return added;
    }
}
