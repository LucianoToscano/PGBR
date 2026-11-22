package dev.pgrazor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Immutable filter set for ONE independent Snipe slot (Sniper 1 or Sniper 2).
 *
 * <p>The PGSharp client exposes two independent snipers. This class models a single slot so PGBR can
 * keep two disjoint filter sets over the feed it already reads ({@link HundoFeedData}). Slots never
 * share state: each instance belongs to one slot id and only ever reads its own JSON.
 *
 * <p>Every field is a PGBR-side filter applied to provider claims. Nothing here is a provider
 * guarantee and nothing is inferred: a value the provider did not send is never promoted to satisfy a
 * bound, and an unset bound hides nothing. All defaults are broad/off, so a freshly constructed
 * instance filters nothing it was not told to filter.
 *
 * <p>{@link #fromJson(JSONObject)} is the only mutable-input entry point for stored data and
 * {@link #fromEditor} the only one for UI text. Both validate types and ranges and reject malformed or
 * inverted input with {@link JSONException}; unknown keys are ignored.
 */
public final class SnipeFilters {
    public static final String SLOT_ONE = "sniper1";
    public static final String SLOT_TWO = "sniper2";

    public static final int IV_MIN = 0, IV_MAX = 100;
    public static final int LEVEL_MIN = 1, LEVEL_MAX = 50;
    public static final int MAX_SPECIES_ID = 1025;
    /** Upper bound of the accepted species list; menus show a summary, not one row per species. */
    public static final int MAX_SPECIES = 64;
    /** "Any form" marker. */
    public static final int FORM_ANY = -1;
    /** Marker for "IV not stated by the provider"; never treated as a low or a high IV. */
    public static final int IV_UNKNOWN = -1;
    /**
     * The 100% IV feed asserts every row it returns is 100% IV. That claim is inherited from the feed
     * schema ({"v":1,"hundos":[...]}, see {@link HundoFeedData#FEED_CLAIMED_IV}), it is NOT an IV
     * re-read from a game encounter.
     */
    public static final int HUNDO_CLAIMED_IV = HundoFeedData.FEED_CLAIMED_IV;

    /** Minimal view of a provider row; keeps the filter independent of one feed schema. */
    public interface Target {
        int dex();
        /** Provider form id, or -1 when it did not state one. */
        int form();
        /** Provider IV, or {@link #IV_UNKNOWN} when it did not state one. */
        int claimedIv();
        /** Provider level, or NaN when it did not state one. */
        double level();
        long despawnMs();
    }

    /** Adapter for the existing 100% IV feed: IV is the feed's own claim. */
    public static Target hundoTarget(final HundoFeedData.Entry e) {
        return new Target() {
            public int dex() { return e.dex; }
            public int form() { return e.form; }
            public int claimedIv() { return e.iv; }
            public double level() { return e.level; }
            public long despawnMs() { return e.despawnMs; }
        };
    }

    private final boolean enabled;
    /** Species allowlist; empty joins every species. Not a dex-range claim: unknown ids never match. */
    private final List<Integer> species;
    private final int minIv, maxIv;
    /** True only when the user set a level bound; an unstated level then never satisfies it. */
    private final boolean levelBounded;
    private final int minLevel, maxLevel;
    private final int form;
    /** Minimum seconds the target must still have before despawning; 0 hides nothing. */
    private final int minRemainingSeconds;

    private SnipeFilters(boolean enabled, List<Integer> species, int minIv, int maxIv,
                         boolean levelBounded, int minLevel, int maxLevel, int form,
                         int minRemainingSeconds) {
        this.enabled = enabled;
        this.species = Collections.unmodifiableList(new ArrayList<>(species));
        this.minIv = minIv;
        this.maxIv = maxIv;
        this.levelBounded = levelBounded;
        this.minLevel = minLevel;
        this.maxLevel = maxLevel;
        this.form = form;
        this.minRemainingSeconds = minRemainingSeconds;
    }

    /** Every slot starts disabled: nothing is listed until the user turns that slot on. */
    public static SnipeFilters defaults() {
        return new SnipeFilters(false, Collections.<Integer>emptyList(), IV_MIN, IV_MAX,
                false, LEVEL_MIN, LEVEL_MAX, FORM_ANY, 0);
    }

    /** True when the payload carries at least one recognised key. */
    public static boolean containsAnyKey(JSONObject o) {
        if (o == null) return false;
        for (String key : new String[]{"enabled", "species", "min_iv", "max_iv",
                "min_level", "max_level", "form", "min_remaining_s"}) {
            if (o.has(key)) return true;
        }
        return false;
    }

    /**
     * Builds a validated, immutable filter set. Missing keys keep their broad default, unknown keys
     * are ignored and {@code null} yields {@link #defaults()}.
     *
     * @throws JSONException on a wrong type, an out-of-range number, an inverted min/max, or a species
     *     list that is not an array of whole ids in {@code 1..MAX_SPECIES_ID}.
     */
    public static SnipeFilters fromJson(JSONObject o) throws JSONException {
        if (o == null) return defaults();

        boolean enabled = boolField(o, "enabled", false);
        int minIv = intField(o, "min_iv", IV_MIN, IV_MIN, IV_MAX);
        int maxIv = intField(o, "max_iv", IV_MAX, IV_MIN, IV_MAX);
        if (minIv > maxIv) throw new JSONException("min_iv cannot exceed max_iv.");

        boolean levelBounded = o.has("min_level") || o.has("max_level");
        int minLevel = intField(o, "min_level", LEVEL_MIN, LEVEL_MIN, LEVEL_MAX);
        int maxLevel = intField(o, "max_level", LEVEL_MAX, LEVEL_MIN, LEVEL_MAX);
        if (minLevel > maxLevel) throw new JSONException("min_level cannot exceed max_level.");

        int form = intField(o, "form", FORM_ANY, FORM_ANY, 9999);
        int remaining = intField(o, "min_remaining_s", 0, 0, 24 * 60 * 60);

        return new SnipeFilters(enabled, speciesField(o.get("species"), o.has("species")),
                minIv, maxIv, levelBounded, minLevel, maxLevel, form, remaining);
    }

    /**
     * Builds a filter set from raw editor text. Empty text keeps the broad default for that field; a
     * non-numeric, out-of-range or inverted value throws {@link JSONException} so the caller can show
     * the message and leave the dialog open.
     */
    public static SnipeFilters fromEditor(boolean enabled, String speciesCsv, String minIv,
                                          String maxIv, String minLevel, String maxLevel,
                                          String form, String remainingSeconds) throws JSONException {
        JSONObject o = new JSONObject();
        o.put("enabled", enabled);
        if (speciesCsv != null && speciesCsv.trim().length() > 0)
            o.put("species", speciesCsvToJson(speciesCsv));
        putNumber(o, "min_iv", minIv, IV_MIN, IV_MAX);
        putNumber(o, "max_iv", maxIv, IV_MIN, IV_MAX);
        putNumber(o, "min_level", minLevel, LEVEL_MIN, LEVEL_MAX);
        putNumber(o, "max_level", maxLevel, LEVEL_MIN, LEVEL_MAX);
        putNumber(o, "form", form, FORM_ANY, 9999);
        putNumber(o, "min_remaining_s", remainingSeconds, 0, 24 * 60 * 60);
        return fromJson(o);
    }

    /** Parses "25, 6 #150" (also "#025", spaces or semicolons) into a validated, de-duplicated list. */
    public static JSONArray speciesCsvToJson(String csv) throws JSONException {
        JSONArray ids = new JSONArray();
        if (csv == null) return ids;
        for (String raw : csv.split("[,;#\\s]+")) {
            String token = raw.trim();
            if (token.isEmpty()) continue;
            int id;
            try {
                id = Integer.parseInt(token);
            } catch (NumberFormatException notANumber) {
                throw new JSONException("Espécie inválida: \"" + token + "\". Use números da Pokédex.");
            }
            if (id < 1 || id > MAX_SPECIES_ID)
                throw new JSONException("Espécie fora de 1.." + MAX_SPECIES_ID + ": " + id + ".");
            boolean seen = false;
            for (int i = 0; i < ids.length(); i++) if (ids.optInt(i, -1) == id) { seen = true; break; }
            if (!seen) ids.put(id);
            if (ids.length() > MAX_SPECIES)
                throw new JSONException("No máximo " + MAX_SPECIES + " espécies por slot.");
        }
        return ids;
    }

    /** Exact JSON shape persisted per slot; {@link #fromJson} round-trips it. */
    public JSONObject toJson() {
        try {
            JSONObject o = new JSONObject();
            o.put("enabled", enabled);
            JSONArray ids = new JSONArray();
            for (Integer id : species) ids.put(id.intValue());
            o.put("species", ids);
            o.put("min_iv", minIv);
            o.put("max_iv", maxIv);
            if (levelBounded) {
                o.put("min_level", minLevel);
                o.put("max_level", maxLevel);
            }
            o.put("form", form);
            o.put("min_remaining_s", minRemainingSeconds);
            return o;
        } catch (JSONException impossible) {
            throw new IllegalStateException(impossible); // toJson only writes validated values.
        }
    }

    /** Copy of this slot with only the on/off state changed. */
    public SnipeFilters withEnabled(boolean value) {
        return new SnipeFilters(value, species, minIv, maxIv, levelBounded, minLevel, maxLevel,
                form, minRemainingSeconds);
    }

    /**
     * Applies this slot's filters to a provider row.
     *
     * @param now current clock; the row must still be alive and keep {@link #minRemainingSeconds()}.
     * @return false when the slot is disabled, the row is expired, or any bound is not satisfied.
     */
    public boolean matches(Target row, long now) {
        if (!enabled || row == null) return false;
        if (row.despawnMs() <= now) return false;
        if (((row.despawnMs() - now) / 1000L) < minRemainingSeconds) return false;
        if (!species.isEmpty() && !species.contains(row.dex())) return false;
        int rowForm = row.form();
        if (form != FORM_ANY) {
            if (rowForm < 0) return false;          // exact form required, provider did not say.
            if (rowForm != form) return false;
        }
        int iv = row.claimedIv();
        if (ivBounded()) {
            if (iv == IV_UNKNOWN) return false;     // unknown IV never satisfies a bound.
            if (iv < minIv || iv > maxIv) return false;
        }
        if (levelBounded) {
            double level = row.level();
            if (!Double.isFinite(level)) return false; // unknown level never satisfies a bound.
            if (level < minLevel || level > maxLevel) return false;
        }
        return true;
    }

    /** Convenience overload for the existing 100% IV feed rows. */
    public boolean matches(HundoFeedData.Entry entry, long now) {
        return entry != null && matches(hundoTarget(entry), now);
    }

    /** Filters a whole list, preserving input order. Never invents rows. */
    public List<HundoFeedData.Entry> select(Collection<HundoFeedData.Entry> entries, long now) {
        if (entries == null || !enabled) return Collections.emptyList();
        List<HundoFeedData.Entry> out = new ArrayList<>();
        for (HundoFeedData.Entry e : entries) if (matches(e, now)) out.add(e);
        return Collections.unmodifiableList(out);
    }

    public boolean enabled() { return enabled; }
    public List<Integer> species() { return species; }
    public int minIv() { return minIv; }
    public int maxIv() { return maxIv; }
    public boolean levelBounded() { return levelBounded; }
    public int minLevel() { return levelBounded ? minLevel : LEVEL_MIN; }
    public int maxLevel() { return levelBounded ? maxLevel : LEVEL_MAX; }
    public int form() { return form; }
    public int minRemainingSeconds() { return minRemainingSeconds; }
    /** True when an IV bound is narrower than the full 0..100 range. */
    public boolean ivBounded() { return minIv > IV_MIN || maxIv < IV_MAX; }

    /** One short line for menus and status rows; never claims results the provider did not send. */
    public String summary() {
        if (!enabled) return "Desligado";
        StringBuilder s = new StringBuilder();
        s.append(ivBounded() ? "IV " + minIv + "–" + maxIv : "IV qualquer");
        if (levelBounded) s.append(" · Nv ").append(minLevel).append('–').append(maxLevel);
        if (form != FORM_ANY) s.append(" · forma ").append(form);
        s.append(" · ");
        if (species.isEmpty()) {
            s.append("todas as espécies");
        } else {
            s.append(species.size()).append(' ').append(species.size() == 1 ? "espécie" : "espécies");
        }
        if (minRemainingSeconds > 0) s.append(" · ≥ ").append(minRemainingSeconds).append('s');
        return s.toString();
    }

    // ------------------------------------------------------------------ parsing helpers

    private static void putNumber(JSONObject o, String key, String text, int min, int max)
            throws JSONException {
        if (text == null || text.trim().isEmpty()) return;
        int value;
        try {
            value = Integer.parseInt(text.trim());
        } catch (NumberFormatException notANumber) {
            throw new JSONException(key + " precisa ser um número inteiro.");
        }
        if (value < min || value > max)
            throw new JSONException(key + " precisa ficar entre " + min + " e " + max + ".");
        o.put(key, value);
    }

    private static boolean boolField(JSONObject o, String key, boolean fallback) throws JSONException {
        if (!o.has(key)) return fallback;
        Object value = o.get(key);
        if (!(value instanceof Boolean)) throw new JSONException(key + " must be true or false.");
        return ((Boolean) value).booleanValue();
    }

    private static int intField(JSONObject o, String key, int fallback, int min, int max)
            throws JSONException {
        if (!o.has(key)) return fallback;
        Object value = o.get(key);
        if (!(value instanceof Number)) throw new JSONException(key + " must be a number.");
        double raw = ((Number) value).doubleValue();
        if (!Double.isFinite(raw) || raw != Math.rint(raw))
            throw new JSONException(key + " must be a whole number.");
        if (raw < min || raw > max)
            throw new JSONException(key + " must be between " + min + " and " + max + ".");
        return (int) raw;
    }

    private static List<Integer> speciesField(Object value, boolean present) throws JSONException {
        if (!present) return Collections.emptyList();
        if (!(value instanceof JSONArray)) throw new JSONException("species must be a list.");
        JSONArray array = (JSONArray) value;
        if (array.length() > MAX_SPECIES)
            throw new JSONException("species accepts at most " + MAX_SPECIES + " ids.");
        List<Integer> ids = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            Object item = array.get(i);
            if (!(item instanceof Number)) throw new JSONException("species ids must be numbers.");
            double raw = ((Number) item).doubleValue();
            if (!Double.isFinite(raw) || raw != Math.rint(raw))
                throw new JSONException("species ids must be whole numbers.");
            if (raw < 1 || raw > MAX_SPECIES_ID)
                throw new JSONException("species ids must be between 1 and " + MAX_SPECIES_ID + ".");
            int id = (int) raw;
            if (!ids.contains(id)) ids.add(id);      // deduplicated, first occurrence wins.
        }
        return ids;
    }
}
