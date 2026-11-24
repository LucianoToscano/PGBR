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
 * Named filter profiles for ONE sniper slot, plus that slot's saved preferences.
 *
 * <p>Each slot owns its own {@code pgrazor-snipe-slot-1} / {@code pgrazor-snipe-slot-2} key, so the two
 * snipers never share filters. Inside one slot the JSON stores:
 * <ul>
 *   <li>{@code active} — the filter set currently applied;</li>
 *   <li>{@code profiles} — named sets the user saved (name -> filter JSON);</li>
 *   <li>{@code source}, {@code order}, {@code global}, {@code radiusKm}, {@code autoRefreshSeconds}
 *       — the panel preferences, validated against their own allowed ranges on read.</li>
 * </ul>
 *
 * <p>A corrupt profile never discards the others: each entry is parsed independently and a bad one is
 * skipped, while a corrupt document as a whole falls back to the slot defaults. Pure data — no I/O, no
 * Android types.
 */
public final class SpawnProfiles {
    public static final String SLOT_ONE = "slot1";
    public static final String SLOT_TWO = "slot2";
    public static final String PREF_FILE = "pgrazor-snipe-v2";

    public static final int MAX_PROFILES = 20;
    public static final int MAX_NAME = 40;
    /** Auto-refresh is opt-in and bounded; 0 means "manual only". */
    public static final int AUTO_OFF = 0, AUTO_MIN_SECONDS = 30, AUTO_MAX_SECONDS = 900;
    public static final int[] AUTO_CHOICES = {AUTO_OFF, 30, 60, 120, 300, 600};
    public static final double DEFAULT_RADIUS_KM = 25;

    private final String slot;
    private final SpawnFilters active;
    private final Map<String, SpawnFilters> profiles;
    private final String sourceId;
    private final SpawnContract.Order order;
    private final boolean global;
    private final double radiusKm;
    private final int autoRefreshSeconds;

    private SpawnProfiles(String slot, SpawnFilters active, Map<String, SpawnFilters> profiles,
                          String sourceId, SpawnContract.Order order, boolean global, double radiusKm,
                          int autoRefreshSeconds) {
        this.slot = slot;
        this.active = active;
        this.profiles = Collections.unmodifiableMap(new LinkedHashMap<>(profiles));
        this.sourceId = sourceId;
        this.order = order;
        this.global = global;
        this.radiusKm = radiusKm;
        this.autoRefreshSeconds = autoRefreshSeconds;
    }

    /** Sniper 1 starts on 100% IV; Sniper 2 starts open so the user can build their own preset. */
    public static SpawnProfiles defaults(String slot) {
        SpawnFilters filters = SLOT_ONE.equals(slot)
                ? new SpawnFilters.Builder().perfectOnly(true).build()
                : SpawnFilters.defaults();
        return new SpawnProfiles(slot, filters, new LinkedHashMap<String, SpawnFilters>(),
                SpawnSourceRegistry.defaultSourceId(), SpawnContract.Order.DISTANCE, false,
                DEFAULT_RADIUS_KM, AUTO_OFF);
    }

    // ------------------------------------------------------------------ copy-on-write updates

    public SpawnProfiles withFilters(SpawnFilters value) {
        return new SpawnProfiles(slot, value == null ? SpawnFilters.defaults() : value, profiles,
                sourceId, order, global, radiusKm, autoRefreshSeconds);
    }

    public SpawnProfiles withSource(String value) {
        SpawnSourceRegistry.SourceInfo info = SpawnSourceRegistry.find(value);
        String fixed = info != null && info.selectable() ? value : SpawnSourceRegistry.defaultSourceId();
        return new SpawnProfiles(slot, active, profiles, fixed, order, global, radiusKm,
                autoRefreshSeconds);
    }

    public SpawnProfiles withOrder(SpawnContract.Order value) {
        return new SpawnProfiles(slot, active, profiles, sourceId,
                value == null ? SpawnContract.Order.DISTANCE : value, global, radiusKm,
                autoRefreshSeconds);
    }

    public SpawnProfiles withGlobal(boolean value) {
        return new SpawnProfiles(slot, active, profiles, sourceId, order, value, radiusKm,
                autoRefreshSeconds);
    }

    public SpawnProfiles withRadius(double value) {
        double fixed = Double.isFinite(value) ? Math.max(1, Math.min(SpawnFilters.MAX_RADIUS_KM, value))
                : DEFAULT_RADIUS_KM;
        return new SpawnProfiles(slot, active, profiles, sourceId, order, global, fixed,
                autoRefreshSeconds);
    }

    public SpawnProfiles withAutoRefresh(int seconds) {
        return new SpawnProfiles(slot, active, profiles, sourceId, order, global, radiusKm,
                autoRefresh(seconds));
    }

    /** Saves (or replaces) a named profile. Names are trimmed, bounded and case-sensitive. */
    public SpawnProfiles saveProfile(String name, SpawnFilters filters) {
        String clean = cleanName(name);
        if (clean == null || filters == null) return this;
        Map<String, SpawnFilters> copy = new LinkedHashMap<>(profiles);
        if (!copy.containsKey(clean) && copy.size() >= MAX_PROFILES) return this;
        copy.put(clean, filters);
        return new SpawnProfiles(slot, active, copy, sourceId, order, global, radiusKm,
                autoRefreshSeconds);
    }

    public SpawnProfiles deleteProfile(String name) {
        String clean = cleanName(name);
        if (clean == null || !profiles.containsKey(clean)) return this;
        Map<String, SpawnFilters> copy = new LinkedHashMap<>(profiles);
        copy.remove(clean);
        return new SpawnProfiles(slot, active, copy, sourceId, order, global, radiusKm,
                autoRefreshSeconds);
    }

    /** Applies a saved profile as the active filter set, keeping the profile itself. */
    public SpawnProfiles applyProfile(String name) {
        String clean = cleanName(name);
        SpawnFilters saved = clean == null ? null : profiles.get(clean);
        return saved == null ? this : withFilters(saved);
    }

    /** "Limpar filtros": back to broad defaults, profiles preserved. */
    public SpawnProfiles clearFilters() {
        return withFilters(SpawnFilters.defaults());
    }

    // ------------------------------------------------------------------ parsing / writing

    public JSONObject toJson() {
        try {
            JSONObject root = new JSONObject();
            root.put("slot", slot);
            root.put("active", active.toJson());
            JSONObject saved = new JSONObject();
            for (Map.Entry<String, SpawnFilters> entry : profiles.entrySet())
                saved.put(entry.getKey(), entry.getValue().toJson());
            root.put("profiles", saved);
            root.put("source", sourceId);
            root.put("order", order.name());
            root.put("global", global);
            root.put("radius_km", radiusKm);
            root.put("auto_refresh_s", autoRefreshSeconds);
            return root;
        } catch (JSONException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    /**
     * Reads one slot's state. A malformed document yields {@link #defaults(String)}; individually bad
     * profiles are skipped so one corrupt entry never wipes the rest.
     */
    public static SpawnProfiles fromJson(String slot, JSONObject root) {
        SpawnProfiles base = defaults(slot);
        if (root == null) return base;
        SpawnFilters active = base.active;
        JSONObject rawActive = root.optJSONObject("active");
        if (rawActive != null) {
            try {
                active = SpawnFilters.fromJson(rawActive);
            } catch (JSONException invalid) {
                active = base.active;      // keep the slot's default rather than dropping everything
            }
        }
        Map<String, SpawnFilters> profiles = new LinkedHashMap<>();
        JSONObject rawProfiles = root.optJSONObject("profiles");
        if (rawProfiles != null) {
            List<String> names = new ArrayList<>();
            for (java.util.Iterator<String> it = rawProfiles.keys(); it.hasNext(); ) names.add(it.next());
            Collections.sort(names);
            for (String name : names) {
                if (profiles.size() >= MAX_PROFILES) break;
                String clean = cleanName(name);
                JSONObject raw = rawProfiles.optJSONObject(name);
                if (clean == null || raw == null) continue;
                try {
                    profiles.put(clean, SpawnFilters.fromJson(raw));
                } catch (JSONException skipped) {
                    // A single bad profile is skipped; the others are still restored.
                }
            }
        }
        SpawnProfiles parsed = new SpawnProfiles(slot, active, profiles, base.sourceId, base.order,
                base.global, base.radiusKm, base.autoRefreshSeconds);
        parsed = parsed.withSource(root.optString("source", base.sourceId));
        parsed = parsed.withOrder(orderOf(root.optString("order", null), base.order));
        parsed = parsed.withGlobal(root.optBoolean("global", base.global));
        parsed = parsed.withRadius(root.optDouble("radius_km", base.radiusKm));
        return parsed.withAutoRefresh(root.optInt("auto_refresh_s", base.autoRefreshSeconds));
    }

    /** Only the documented choices are accepted; anything else falls back to no auto-refresh. */
    static int autoRefresh(int seconds) {
        for (int choice : AUTO_CHOICES) if (choice == seconds) return seconds;
        return AUTO_OFF;
    }

    private static SpawnContract.Order orderOf(String name, SpawnContract.Order fallback) {
        if (name == null) return fallback;
        for (SpawnContract.Order order : SpawnContract.Order.values())
            if (order.name().equals(name)) return order;
        return fallback;
    }

    static String cleanName(String name) {
        if (name == null) return null;
        String clean = name.trim();
        if (clean.isEmpty() || clean.length() > MAX_NAME) return null;
        // Control characters would break the JSON key round-trip and the list widget.
        for (int i = 0; i < clean.length(); i++) if (clean.charAt(i) < 0x20) return null;
        return clean;
    }

    // ------------------------------------------------------------------ accessors

    public String slot() { return slot; }
    public SpawnFilters filters() { return active; }
    public List<String> profileNames() {
        return Collections.unmodifiableList(new ArrayList<>(profiles.keySet()));
    }
    public SpawnFilters profile(String name) { return profiles.get(name); }
    public String sourceId() { return sourceId; }
    public SpawnContract.Order order() { return order; }
    public boolean global() { return global; }
    public double radiusKm() { return radiusKm; }
    public int autoRefreshSeconds() { return autoRefreshSeconds; }
}
