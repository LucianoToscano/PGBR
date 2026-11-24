package dev.pgrazor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * One immutable filter set used by the radar, both Snipers, presets and the gateway contract.
 * Ranges are inclusive; {@code null} always means "no criterion" (never zero). A query knows which
 * criteria are actually active so the UI can show a real badge count and the engine can tell a
 * filter the origin ignored from one the origin applied.
 */
public final class SourceQuery {
    public enum Sort { DISTANCE, IV_DESC, CP_DESC, LEVEL_DESC, EXPIRES_ASC, NEWEST }
    public static final String LEAGUE_GREAT = "GL";
    public static final String LEAGUE_ULTRA = "UL";

    public final Set<SourceEvent.Kind> kinds;
    public final Set<OriginKind> originKinds;
    public final Set<Integer> pokemonIds;
    public final Set<Integer> formIds;
    public final Double ivMin, ivMax;
    public final Integer atkMin, atkMax, defMin, defMax, staMin, staMax;
    public final Integer cpMin, cpMax;
    public final Integer levelMin, levelMax;
    public final String gender;
    public final Set<String> sizes;
    public final Boolean weatherBoost;
    public final String pvpLeague;
    public final Integer pvpMaxRank;
    public final Double centerLat, centerLon, radiusKm;
    public final Long minTtlSeconds;
    public final Long maxRemainingSeconds;
    public final boolean includeExpired;
    public final boolean includeDemo;
    public final boolean andLogic;
    public final String text;
    public final int limit;
    public final Sort sort;

    private SourceQuery(Builder b) {
        kinds = immutable(b.kinds);
        originKinds = immutable(b.originKinds);
        pokemonIds = immutable(b.pokemonIds);
        formIds = immutable(b.formIds);
        ivMin = b.ivMin; ivMax = b.ivMax;
        atkMin = b.atkMin; atkMax = b.atkMax;
        defMin = b.defMin; defMax = b.defMax;
        staMin = b.staMin; staMax = b.staMax;
        cpMin = b.cpMin; cpMax = b.cpMax;
        levelMin = b.levelMin; levelMax = b.levelMax;
        gender = b.gender;
        sizes = immutable(b.sizes);
        weatherBoost = b.weatherBoost;
        pvpLeague = b.pvpLeague;
        pvpMaxRank = b.pvpMaxRank;
        centerLat = b.centerLat;
        centerLon = b.centerLon;
        radiusKm = b.radiusKm;
        minTtlSeconds = b.minTtlSeconds;
        maxRemainingSeconds = b.maxRemainingSeconds;
        includeExpired = b.includeExpired;
        includeDemo = b.includeDemo;
        andLogic = b.andLogic;
        text = b.text == null ? "" : b.text.trim();
        limit = b.limit <= 0 ? 25 : Math.min(b.limit, 500);
        sort = b.sort == null ? Sort.DISTANCE : b.sort;
    }

    private static <T> Set<T> immutable(Set<T> source) {
        if (source == null || source.isEmpty()) return Collections.emptySet();
        return Collections.unmodifiableSet(new LinkedHashSet<>(source));
    }

    public static Builder builder() { return new Builder(); }

    public Builder toBuilder() {
        Builder b = new Builder();
        b.kinds.addAll(kinds);
        b.originKinds.addAll(originKinds);
        b.pokemonIds.addAll(pokemonIds);
        b.formIds.addAll(formIds);
        b.ivMin = ivMin; b.ivMax = ivMax;
        b.atkMin = atkMin; b.atkMax = atkMax;
        b.defMin = defMin; b.defMax = defMax;
        b.staMin = staMin; b.staMax = staMax;
        b.cpMin = cpMin; b.cpMax = cpMax;
        b.levelMin = levelMin; b.levelMax = levelMax;
        b.gender = gender;
        b.sizes.addAll(sizes);
        b.weatherBoost = weatherBoost;
        b.pvpLeague = pvpLeague;
        b.pvpMaxRank = pvpMaxRank;
        b.centerLat = centerLat; b.centerLon = centerLon; b.radiusKm = radiusKm;
        b.minTtlSeconds = minTtlSeconds;
        b.maxRemainingSeconds = maxRemainingSeconds;
        b.includeExpired = includeExpired;
        b.includeDemo = includeDemo;
        b.andLogic = andLogic;
        b.text = text;
        b.limit = limit;
        b.sort = sort;
        return b;
    }

    /** True when at least one criterion (beyond defaults) is active. */
    public boolean isActive() { return !activeCriteria().isEmpty(); }

    /** Human-readable list of the criteria that are really active; defaults are not counted. */
    public List<String> activeCriteria() {
        List<String> out = new ArrayList<>();
        if (!kinds.isEmpty()) out.add("tipo de evento");
        if (!originKinds.isEmpty()) out.add("origem dos dados");
        if (!pokemonIds.isEmpty()) out.add("espécies (" + pokemonIds.size() + ")");
        if (!formIds.isEmpty()) out.add("formas (" + formIds.size() + ")");
        if (ivMin != null && ivMin > 0) out.add("IV mín " + trim(ivMin) + "%");
        if (ivMax != null && ivMax < 100) out.add("IV máx " + trim(ivMax) + "%");
        if (ivMin != null && ivMax != null && ivMin == 0 && ivMax == 0) out.add("IV 0%");
        if (ivMin != null && ivMax != null && ivMin == 100 && ivMax == 100) out.add("IV 100%");
        if (atkMin != null || atkMax != null) out.add("Ataque " + range(atkMin, atkMax, 15));
        if (defMin != null || defMax != null) out.add("Defesa " + range(defMin, defMax, 15));
        if (staMin != null || staMax != null) out.add("Stamina " + range(staMin, staMax, 15));
        if (cpMin != null || cpMax != null) out.add("PC " + range(cpMin, cpMax, 9999));
        if (levelMin != null || levelMax != null) out.add("Nível " + range(levelMin, levelMax, 50));
        if (gender != null) out.add("sexo");
        if (!sizes.isEmpty()) out.add("tamanho (" + sizes.size() + ")");
        if (weatherBoost != null) out.add("clima");
        if (pvpLeague != null) out.add("PvP " + pvpLeague + " até #" + (pvpMaxRank == null ? 1 : pvpMaxRank));
        if (radiusKm != null && centerLat != null && centerLon != null) out.add("raio " + trim(radiusKm) + " km");
        if (minTtlSeconds != null) out.add("TTL mín " + minTtlSeconds + "s");
        if (maxRemainingSeconds != null) out.add("expira em até " + maxRemainingSeconds + "s");
        if (!text.isEmpty()) out.add("texto \"" + text + "\"");
        return out;
    }

    private static String trim(double value) {
        return value == Math.rint(value) ? Long.toString((long) value) : Double.toString(value);
    }

    private static String range(Integer min, Integer max, int ceiling) {
        if (min == null && max == null) return "livre";
        if (min == null) return "até " + max;
        if (max == null) return min + " ou mais";
        return min + "–" + max;
    }

    /** True when the query's centre and radius are usable for distance sorting/filtering. */
    public boolean hasCenter() {
        return SourceEvent.validCoordinates(centerLat, centerLon);
    }

    // --- validation -----------------------------------------------------------------------------

    /** Throws {@link JSONException} for inverted or out-of-range values. */
    public void validate() throws JSONException {
        check(ivMin == null || (ivMin >= 0 && ivMin <= 100), "IV mínimo fora de 0..100");
        check(ivMax == null || (ivMax >= 0 && ivMax <= 100), "IV máximo fora de 0..100");
        check(ivMin == null || ivMax == null || ivMin <= ivMax, "IV mínimo maior que o máximo");
        checkRange("Ataque", atkMin, atkMax, 15);
        checkRange("Defesa", defMin, defMax, 15);
        checkRange("Stamina", staMin, staMax, 15);
        checkRange("PC", cpMin, cpMax, 9999);
        checkRange("Nível", levelMin, levelMax, 50);
        check(radiusKm == null || (radiusKm >= 0 && radiusKm <= 20000), "Raio inválido");
        check(centerLat == null || (centerLat >= -90 && centerLat <= 90), "Latitude inválida");
        check(centerLon == null || (centerLon >= -180 && centerLon <= 180), "Longitude inválida");
        check(gender == null || gender.equals("MALE") || gender.equals("FEMALE")
                || gender.equals("GENDERLESS"), "Sexo inválido");
        check(pvpLeague == null || pvpLeague.equals(LEAGUE_GREAT) || pvpLeague.equals(LEAGUE_ULTRA),
                "Liga PvP inválida");
        check(pvpLeague == null || pvpMaxRank == null || (pvpMaxRank >= 1 && pvpMaxRank <= 5000),
                "Rank PvP inválido");
        check(pvpLeague != null || pvpMaxRank == null, "Rank PvP sem liga");
        check(minTtlSeconds == null || (minTtlSeconds >= 0 && minTtlSeconds <= 86400), "TTL inválido");
        check(maxRemainingSeconds == null || (maxRemainingSeconds >= 0 && maxRemainingSeconds <= 86400),
                "Expiração máxima inválida");
    }

    private static void checkRange(String label, Integer min, Integer max, int ceiling) throws JSONException {
        if (min != null) check(min >= 0 && min <= ceiling, label + " mínimo inválido");
        if (max != null) check(max >= 0 && max <= ceiling, label + " máximo inválido");
        if (min != null && max != null) check(min <= max, label + " mínimo maior que o máximo");
    }

    private static void check(boolean condition, String message) throws JSONException {
        if (!condition) throw new JSONException(message);
    }

    // --- JSON -----------------------------------------------------------------------------------

    public JSONObject toJson() throws JSONException {
        validate();
        JSONObject o = new JSONObject();
        if (!kinds.isEmpty()) {
            JSONArray a = new JSONArray();
            for (SourceEvent.Kind kind : kinds) a.put(kind.name().toLowerCase(java.util.Locale.ROOT));
            o.put("kinds", a);
        }
        if (!originKinds.isEmpty()) {
            JSONArray a = new JSONArray();
            for (OriginKind kind : originKinds) a.put(kind.wire);
            o.put("origin_kinds", a);
        }
        if (!pokemonIds.isEmpty()) o.put("pokemon_ids", new JSONArray(new ArrayList<Object>(new TreeSet<>(pokemonIds))));
        if (!formIds.isEmpty()) o.put("form_ids", new JSONArray(new ArrayList<Object>(new TreeSet<>(formIds))));
        if (ivMin != null) o.put("iv_min", ivMin.doubleValue());
        if (ivMax != null) o.put("iv_max", ivMax.doubleValue());
        if (atkMin != null) o.put("atk_min", atkMin.intValue());
        if (atkMax != null) o.put("atk_max", atkMax.intValue());
        if (defMin != null) o.put("def_min", defMin.intValue());
        if (defMax != null) o.put("def_max", defMax.intValue());
        if (staMin != null) o.put("sta_min", staMin.intValue());
        if (staMax != null) o.put("sta_max", staMax.intValue());
        if (cpMin != null) o.put("cp_min", cpMin.intValue());
        if (cpMax != null) o.put("cp_max", cpMax.intValue());
        if (levelMin != null) o.put("level_min", levelMin.intValue());
        if (levelMax != null) o.put("level_max", levelMax.intValue());
        if (gender != null) o.put("gender", gender);
        if (!sizes.isEmpty()) o.put("sizes", new JSONArray(new ArrayList<Object>(new TreeSet<>(sizes))));
        if (weatherBoost != null) o.put("weather_boost", weatherBoost.booleanValue());
        if (pvpLeague != null) o.put("pvp_league", pvpLeague);
        if (pvpMaxRank != null) o.put("pvp_max_rank", pvpMaxRank.intValue());
        if (centerLat != null) o.put("center_lat", centerLat.doubleValue());
        if (centerLon != null) o.put("center_lon", centerLon.doubleValue());
        if (radiusKm != null) o.put("radius_km", radiusKm.doubleValue());
        if (minTtlSeconds != null) o.put("min_ttl_seconds", minTtlSeconds.longValue());
        if (maxRemainingSeconds != null) o.put("max_remaining_seconds", maxRemainingSeconds.longValue());
        o.put("include_expired", includeExpired);
        o.put("include_demo", includeDemo);
        o.put("and_logic", andLogic);
        if (!text.isEmpty()) o.put("text", text);
        o.put("limit", limit);
        o.put("sort", sort.name().toLowerCase(java.util.Locale.ROOT));
        return o;
    }

    /** Reads a query, ignoring unknown keys and rejecting malformed values. */
    public static SourceQuery fromJson(JSONObject o) throws JSONException {
        Builder b = builder();
        if (o == null) return b.build();
        JSONArray kinds = o.optJSONArray("kinds");
        if (kinds != null) for (int i = 0; i < kinds.length(); i++) {
            String raw = kinds.optString(i, "");
            for (SourceEvent.Kind kind : SourceEvent.Kind.values())
                if (kind.name().toLowerCase(java.util.Locale.ROOT).equals(raw)) b.kinds.add(kind);
        }
        JSONArray origins = o.optJSONArray("origin_kinds");
        if (origins != null) for (int i = 0; i < origins.length(); i++) {
            OriginKind kind = OriginKind.fromWire(origins.optString(i, null));
            if (kind != null) b.originKinds.add(kind);
        }
        addIds(b.pokemonIds, o.optJSONArray("pokemon_ids"));
        addIds(b.formIds, o.optJSONArray("form_ids"));
        b.ivMin = decimal(o, "iv_min"); b.ivMax = decimal(o, "iv_max");
        b.atkMin = integer(o, "atk_min"); b.atkMax = integer(o, "atk_max");
        b.defMin = integer(o, "def_min"); b.defMax = integer(o, "def_max");
        b.staMin = integer(o, "sta_min"); b.staMax = integer(o, "sta_max");
        b.cpMin = integer(o, "cp_min"); b.cpMax = integer(o, "cp_max");
        b.levelMin = integer(o, "level_min"); b.levelMax = integer(o, "level_max");
        if (!o.isNull("gender")) b.gender = SourceEvent.normalizeGender(o.opt("gender"));
        JSONArray sizes = o.optJSONArray("sizes");
        if (sizes != null) for (int i = 0; i < sizes.length(); i++) {
            String size = SourceEvent.normalizeSize(sizes.optString(i, null));
            if (size != null) b.sizes.add(size);
        }
        if (!o.isNull("weather_boost")) b.weatherBoost = Boolean.valueOf(o.optBoolean("weather_boost", false));
        b.pvpLeague = o.isNull("pvp_league") ? null : o.optString("pvp_league", null);
        b.pvpMaxRank = integer(o, "pvp_max_rank");
        b.centerLat = decimal(o, "center_lat");
        b.centerLon = decimal(o, "center_lon");
        b.radiusKm = decimal(o, "radius_km");
        b.minTtlSeconds = o.isNull("min_ttl_seconds") ? null : Long.valueOf(o.optLong("min_ttl_seconds", 0L));
        b.maxRemainingSeconds = o.isNull("max_remaining_seconds") ? null
                : Long.valueOf(o.optLong("max_remaining_seconds", 0L));
        b.includeExpired = o.optBoolean("include_expired", false);
        b.includeDemo = o.optBoolean("include_demo", false);
        b.andLogic = o.optBoolean("and_logic", true);
        b.text = o.optString("text", "");
        b.limit = o.optInt("limit", 25);
        String sort = o.optString("sort", "distance");
        for (Sort candidate : Sort.values())
            if (candidate.name().toLowerCase(java.util.Locale.ROOT).equals(sort)) b.sort = candidate;
        SourceQuery query = b.build();
        query.validate();
        return query;
    }

    private static void addIds(Set<Integer> target, JSONArray array) {
        if (array == null) return;
        for (int i = 0; i < array.length(); i++) {
            Integer value = SourceEvent.integer(array.opt(i));
            if (value != null && value >= 0) target.add(value);
        }
    }

    private static Integer integer(JSONObject o, String key) {
        return o.isNull(key) ? null : SourceEvent.integer(o.opt(key));
    }

    private static Double decimal(JSONObject o, String key) {
        return o.isNull(key) ? null : SourceEvent.decimal(o.opt(key));
    }

    public static final class Builder {
        private final Set<SourceEvent.Kind> kinds = EnumSet.noneOf(SourceEvent.Kind.class);
        private final Set<OriginKind> originKinds = EnumSet.noneOf(OriginKind.class);
        private final Set<Integer> pokemonIds = new TreeSet<>();
        private final Set<Integer> formIds = new TreeSet<>();
        private final Set<String> sizes = new TreeSet<>();
        private Double ivMin, ivMax;
        private Integer atkMin, atkMax, defMin, defMax, staMin, staMax, cpMin, cpMax, levelMin, levelMax;
        private String gender;
        private Boolean weatherBoost;
        private String pvpLeague;
        private Integer pvpMaxRank;
        private Double centerLat, centerLon, radiusKm;
        private Long minTtlSeconds, maxRemainingSeconds;
        private boolean includeExpired, includeDemo, andLogic = true;
        private String text;
        private int limit = 25;
        private Sort sort = Sort.DISTANCE;

        private Builder() {}

        public Builder kind(SourceEvent.Kind kind) { if (kind != null) kinds.add(kind); return this; }
        public Builder origin(OriginKind kind) { if (kind != null) originKinds.add(kind); return this; }
        public Builder pokemon(int... ids) {
            if (ids != null) for (int id : ids) if (id >= 1 && id <= 1025) pokemonIds.add(id);
            return this;
        }
        public Builder form(int... ids) {
            if (ids != null) for (int id : ids) if (id >= 0) formIds.add(id);
            return this;
        }
        public Builder iv(Double min, Double max) { this.ivMin = min; this.ivMax = max; return this; }
        public Builder attack(Integer min, Integer max) { this.atkMin = min; this.atkMax = max; return this; }
        public Builder defense(Integer min, Integer max) { this.defMin = min; this.defMax = max; return this; }
        public Builder stamina(Integer min, Integer max) { this.staMin = min; this.staMax = max; return this; }
        public Builder cp(Integer min, Integer max) { this.cpMin = min; this.cpMax = max; return this; }
        public Builder level(Integer min, Integer max) { this.levelMin = min; this.levelMax = max; return this; }
        public Builder gender(String value) { this.gender = value; return this; }
        public Builder size(String value) {
            String normalized = SourceEvent.normalizeSize(value);
            if (normalized != null) sizes.add(normalized);
            return this;
        }
        public Builder weatherBoost(Boolean value) { this.weatherBoost = value; return this; }
        public Builder pvp(String league, Integer maxRank) { this.pvpLeague = league; this.pvpMaxRank = maxRank; return this; }
        public Builder center(Double lat, Double lon, Double radiusKm) {
            this.centerLat = lat; this.centerLon = lon; this.radiusKm = radiusKm; return this;
        }
        public Builder minTtlSeconds(Long value) { this.minTtlSeconds = value; return this; }
        public Builder maxRemainingSeconds(Long value) { this.maxRemainingSeconds = value; return this; }
        public Builder includeExpired(boolean value) { this.includeExpired = value; return this; }
        public Builder includeDemo(boolean value) { this.includeDemo = value; return this; }
        public Builder andLogic(boolean value) { this.andLogic = value; return this; }
        public Builder text(String value) { this.text = value; return this; }
        public Builder limit(int value) { this.limit = value; return this; }
        public Builder sort(Sort value) { this.sort = value; return this; }

        public SourceQuery build() { return new SourceQuery(this); }
    }
}
