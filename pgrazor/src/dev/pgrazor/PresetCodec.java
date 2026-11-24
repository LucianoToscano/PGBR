package dev.pgrazor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Versioned preset storage: export, import, migration from the historical saved-filter shape and
 * safe validation. Presets contain filters only — never credentials, tokens or account data.
 *
 * <p>Migration is additive: a v1 file becomes a v2 preset with {@code migrated_from=1}, and unknown
 * keys are preserved in {@code raw_source} instead of being applied by guesswork.
 */
public final class PresetCodec {
    private PresetCodec() {}

    public static final String FORMAT = "PGBR preset";
    public static final int VERSION = 2;
    public static final int MAX_PRESETS = 200;

    public static final class Preset {
        public final String id;
        public final String name;
        public final SourceQuery query;
        public final int version;
        public final long createdAt, updatedAt;
        public final boolean favorite;
        public final String migratedFrom;
        public final String rawSource;

        Preset(String id, String name, SourceQuery query, int version, long createdAt, long updatedAt,
               boolean favorite, String migratedFrom, String rawSource) {
            this.id = id == null || id.trim().isEmpty() ? newId() : id.trim();
            this.name = name == null || name.trim().isEmpty() ? "Preset sem nome" : name.trim();
            this.query = query == null ? SourceQuery.builder().build() : query;
            this.version = version;
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;
            this.favorite = favorite;
            this.migratedFrom = migratedFrom == null ? "" : migratedFrom;
            this.rawSource = rawSource == null ? "" : rawSource;
        }

        public JSONObject toJson() throws JSONException {
            JSONObject o = new JSONObject();
            o.put("id", id);
            o.put("name", name);
            o.put("version", version);
            o.put("favorite", favorite);
            o.put("created_at", createdAt);
            o.put("updated_at", updatedAt);
            o.put("query", query.toJson());
            if (!migratedFrom.isEmpty()) o.put("migrated_from", migratedFrom);
            if (!rawSource.isEmpty()) o.put("raw_source", rawSource);
            return o;
        }

        /** Copy with a new name; used by rename/duplicate in the UI. */
        public Preset rename(String newName, long now) {
            return new Preset(id, newName, query, version, createdAt, now, favorite, migratedFrom, rawSource);
        }

        public Preset withFavorite(boolean value, long now) {
            return new Preset(id, name, query, version, createdAt, now, value, migratedFrom, rawSource);
        }
    }

    public static String newId() {
        return "preset-" + Long.toHexString(System.nanoTime()) + "-"
                + Integer.toHexString((int) (Math.random() * Integer.MAX_VALUE));
    }

    /** Builds a preset from a validated query. */
    public static Preset create(String name, SourceQuery query, long now) throws JSONException {
        if (query != null) query.validate();
        return new Preset(newId(), name, query, VERSION, now, now, false, "", "");
    }

    // --- export / import ------------------------------------------------------------------------

    public static String export(List<Preset> presets) throws JSONException {
        JSONObject root = new JSONObject();
        root.put("format", FORMAT);
        root.put("version", VERSION);
        JSONArray array = new JSONArray();
        if (presets != null) for (Preset preset : presets) if (preset != null) array.put(preset.toJson());
        root.put("presets", array);
        return root.toString();
    }

    /**
     * Reads an export. Rejects a wrong format, a newer version, inverted ranges and more than
     * {@link #MAX_PRESETS} entries; duplicate ids keep the first occurrence and are reported.
     */
    public static List<Preset> importPresets(String json) throws JSONException {
        if (json == null || json.trim().isEmpty()) throw new JSONException("Arquivo de presets vazio.");
        JSONObject root = new JSONObject(json);
        int version = root.optInt("version", 1);
        if (!FORMAT.equals(root.optString("format")) && !root.has("presets") && version == 1)
            return importV1(root);
        if (!FORMAT.equals(root.optString("format")))
            throw new JSONException("Arquivo não é um preset do PGBR.");
        if (version > VERSION)
            throw new JSONException("Versão de preset (" + version + ") não suportada por esta versão do PGBR.");
        JSONArray array = root.optJSONArray("presets");
        if (array == null) throw new JSONException("Arquivo de presets sem lista.");
        if (array.length() > MAX_PRESETS)
            throw new JSONException("Muitos presets em um arquivo (limite " + MAX_PRESETS + ").");
        Map<String, Preset> unique = new LinkedHashMap<>();
        for (int i = 0; i < array.length(); i++) {
            Preset preset = presetFromJson(array.optJSONObject(i));
            if (preset != null && !unique.containsKey(preset.id)) unique.put(preset.id, preset);
        }
        return Collections.unmodifiableList(new ArrayList<>(unique.values()));
    }

    /** Reads a single preset object (v1 or v2). */
    public static Preset presetFromJson(JSONObject o) throws JSONException {
        if (o == null) return null;
        int version = o.optInt("version", 1);
        String raw = o.toString();
        if (version <= 1) return migrateV1(o, raw);
        JSONObject queryJson = o.optJSONObject("query");
        SourceQuery query = queryJson == null
                ? SourceQuery.builder().build() : SourceQuery.fromJson(queryJson);
        long now = System.currentTimeMillis();
        return new Preset(o.optString("id", ""), o.optString("name", ""), query, version,
                o.optLong("created_at", now), o.optLong("updated_at", now),
                o.optBoolean("favorite", false), o.optString("migrated_from", ""), raw);
    }

    // --- migration -------------------------------------------------------------------------------

    /**
     * v1 is the historical saved-filter shape ({@code name, ivMin, ivMax, species, pvpLeague, ...}).
     * Every recognised field is mapped; the whole object is kept in {@code raw_source} so nothing is
     * lost when the old file carried a field this version does not understand.
     */
    static Preset migrateV1(JSONObject o, String raw) throws JSONException {
        SourceQuery.Builder b = SourceQuery.builder();
        Double ivMin = SourceEvent.decimal(o.opt("ivMin"));
        Double ivMax = SourceEvent.decimal(o.opt("ivMax"));
        if (ivMin == null && o.has("iv")) ivMin = SourceEvent.decimal(o.opt("iv"));
        if (ivMax == null && ivMin != null) ivMax = Double.valueOf(100.0);
        b.iv(ivMin, ivMax);
        Object species = o.opt("species");
        if (species instanceof JSONArray) {
            JSONArray ids = (JSONArray) species;
            for (int i = 0; i < ids.length(); i++) {
                Integer id = SourceEvent.integer(ids.opt(i));
                if (id != null && id >= 1 && id <= 1025) b.pokemon(id.intValue());
            }
        } else if (species instanceof String) {
            for (String part : ((String) species).split(",")) {
                Integer id = SourceEvent.integer(part.trim());
                if (id != null && id >= 1 && id <= 1025) b.pokemon(id.intValue());
            }
        }
        b.level(SourceEvent.integer(o.opt("levelMin")), SourceEvent.integer(o.opt("levelMax")));
        b.cp(SourceEvent.integer(o.opt("cpMin")), SourceEvent.integer(o.opt("cpMax")));
        b.attack(SourceEvent.integer(o.opt("attackMin")), SourceEvent.integer(o.opt("attackMax")));
        b.defense(SourceEvent.integer(o.opt("defenseMin")), SourceEvent.integer(o.opt("defenseMax")));
        b.stamina(SourceEvent.integer(o.opt("hpMin")), SourceEvent.integer(o.opt("hpMax")));
        String gender = SourceEvent.normalizeGender(o.opt("gender"));
        if (gender != null) b.gender(gender);
        Object sizes = o.opt("sizes");
        if (sizes instanceof JSONArray) {
            JSONArray array = (JSONArray) sizes;
            for (int i = 0; i < array.length(); i++) b.size(array.optString(i, null));
        } else if (sizes instanceof String) {
            for (String part : ((String) sizes).split(",")) b.size(part.trim());
        }
        String league = o.optString("pvpLeague", null);
        if (league != null && (league.equals(SourceQuery.LEAGUE_GREAT)
                || league.equals(SourceQuery.LEAGUE_ULTRA)))
            b.pvp(league, SourceEvent.integer(o.opt("pvpRank")));
        Double lat = SourceEvent.decimal(o.opt("lat"));
        Double lon = SourceEvent.decimal(o.opt("lon"));
        Double radius = SourceEvent.decimal(o.opt("radiusKm"));
        if (SourceEvent.validCoordinates(lat, lon) && radius != null) b.center(lat, lon, radius);
        SourceQuery query = b.build();
        query.validate();
        long now = System.currentTimeMillis();
        return new Preset(newId(), o.optString("name", "Preset importado"), query, VERSION, now, now,
                o.optBoolean("favorite", false), "1", raw);
    }

    /** v1 file that is a bare array or a single object. */
    private static List<Preset> importV1(JSONObject root) throws JSONException {
        List<Preset> out = new ArrayList<>();
        if (root.has("name") || root.has("ivMin") || root.has("species")) {
            out.add(migrateV1(root, root.toString()));
            return Collections.unmodifiableList(out);
        }
        throw new JSONException("Formato de preset não reconhecido.");
    }

    /** Removes duplicates by id and keeps at most {@link #MAX_PRESETS}. */
    public static List<Preset> dedupe(List<Preset> presets) {
        Map<String, Preset> unique = new LinkedHashMap<>();
        if (presets != null) for (Preset preset : presets)
            if (preset != null && !unique.containsKey(preset.id) && unique.size() < MAX_PRESETS)
                unique.put(preset.id, preset);
        return Collections.unmodifiableList(new ArrayList<>(unique.values()));
    }

    /** Ids of the presets marked as favourites, in a stable order. */
    public static Set<String> favoriteIds(List<Preset> presets) {
        Set<String> ids = new HashSet<>();
        if (presets != null) for (Preset preset : presets)
            if (preset != null && preset.favorite) ids.add(preset.id);
        return ids;
    }

    /**
     * Stable, filesystem-safe slug used for preset file names and ids: ASCII letters and digits
     * only, accents removed (NFD + combining marks dropped), separators collapsed and no trailing
     * dash, so "IV 100% Rápido!" and "IV 100% Rapido" produce the same id.
     */
    public static String slug(String value) {
        if (value == null) return "preset";
        String decomposed = java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        String low = decomposed.trim().toLowerCase(Locale.ROOT);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < low.length() && sb.length() < 48; i++) {
            char c = low.charAt(i);
            if (c < 128 && Character.isLetterOrDigit(c)) sb.append(c);
            else if (sb.length() > 0 && sb.charAt(sb.length() - 1) != '-') sb.append('-');
        }
        while (sb.length() > 0 && sb.charAt(sb.length() - 1) == '-') sb.setLength(sb.length() - 1);
        if (sb.length() == 0) sb.append("preset");
        return sb.toString();
    }
}
