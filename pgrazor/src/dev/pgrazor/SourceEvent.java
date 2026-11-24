package dev.pgrazor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Canonical, UI-agnostic record shared by every PGBR source: the five integrations (PokeList,
 * iFlowGo, PoGoCoordinates, PGSharp, iPogo), the local import store and the PGBR 100% IV feed.
 *
 * <p><b>Rules that this class enforces.</b>
 * <ul>
 *   <li>Absent is {@code null}, never {@code 0}. An IV of {@code 0} is a real observation and stays
 *       {@code 0}; an unknown IV stays {@code null}.</li>
 *   <li>{@code isDemo} marks synthetic rows so no screen can present them as live data.</li>
 *   <li>Coordinates are validated once, here: {@code NaN}, infinity, out-of-range values and the
 *       {@code (0,0)} legend placeholder are all rejected and become {@code null} with
 *       {@link CoordinatePrecision#UNAVAILABLE}.</li>
 *   <li>{@link #isActionable(long)} is the single gate used by copy/map/teleport actions: it needs
 *       exact coordinates, a future expiry (or no expiry at all) and a non-demo origin.</li>
 * </ul>
 */
public final class SourceEvent {
    public enum Kind { POKEMON, RAID, QUEST, ROCKET, POWERSPOT, NEST, ROUTE }

    public enum CoordinatePrecision {
        EXACT("exact"), APPROXIMATE("approximate"), UNAVAILABLE("unavailable");
        public final String wire;
        CoordinatePrecision(String wire) { this.wire = wire; }
        public static CoordinatePrecision fromWire(String value) {
            if (value == null) return UNAVAILABLE;
            for (CoordinatePrecision p : values()) if (p.wire.equals(value)) return p;
            return UNAVAILABLE;
        }
    }

    public static final String QUALITY_PROVIDER_CLAIMED = "provider_claimed";
    public static final String QUALITY_IMPORTED = "imported";
    public static final String QUALITY_VERIFIED = "verified";
    public static final String QUALITY_UNKNOWN = "unknown";

    // --- identity --------------------------------------------------------------------------
    public final String sourceId;
    public final String eventId;
    public final Kind kind;
    public final OriginKind originKind;

    // --- subject ---------------------------------------------------------------------------
    public final Integer pokemonId;
    public final Integer formId;
    public final String name;

    // --- location --------------------------------------------------------------------------
    public final Double lat;
    public final Double lon;
    public final CoordinatePrecision coordinatePrecision;

    // --- attributes (null = absent) --------------------------------------------------------
    public final Double ivPercent;
    public final Integer ivAtk, ivDef, ivSta;
    public final Integer cp, level;
    public final String gender;
    public final String size;
    public final Boolean weatherBoost;
    public final Integer pvpGreatRank, pvpUltraRank;
    /** True only when the origin proves an observed shiny; eligibility never sets this. */
    public final Boolean shinyObserved;

    // --- time ------------------------------------------------------------------------------
    public final long observedAt;
    public final long retrievedAt;
    /** 0 = the origin did not report an expiry. */
    public final long expiresAt;

    // --- provenance ------------------------------------------------------------------------
    public final boolean isDemo;
    public final String dataQuality;
    public final String coverage;
    public final String sourceUrl;
    public final String note;

    private SourceEvent(Builder b) {
        sourceId = b.sourceId == null ? "" : b.sourceId;
        eventId = b.eventId == null ? "" : b.eventId;
        kind = b.kind == null ? Kind.POKEMON : b.kind;
        originKind = b.originKind == null ? OriginKind.PERSONAL_IMPORT : b.originKind;
        pokemonId = b.pokemonId;
        formId = b.formId;
        name = b.name == null ? "" : b.name;
        coordinatePrecision = b.lat != null && b.lon != null ? b.precision : CoordinatePrecision.UNAVAILABLE;
        lat = b.lat;
        lon = b.lon;
        ivPercent = b.ivPercent;
        ivAtk = b.ivAtk;
        ivDef = b.ivDef;
        ivSta = b.ivSta;
        cp = b.cp;
        level = b.level;
        gender = b.gender;
        size = b.size;
        weatherBoost = b.weatherBoost;
        pvpGreatRank = b.pvpGreatRank;
        pvpUltraRank = b.pvpUltraRank;
        shinyObserved = b.shinyObserved;
        observedAt = Math.max(0L, b.observedAt);
        retrievedAt = Math.max(0L, b.retrievedAt);
        expiresAt = Math.max(0L, b.expiresAt);
        isDemo = b.isDemo;
        dataQuality = b.dataQuality == null ? QUALITY_UNKNOWN : b.dataQuality;
        coverage = b.coverage;
        sourceUrl = b.sourceUrl;
        note = b.note == null ? "" : b.note;
    }

    public static Builder builder(String sourceId, String eventId, Kind kind) {
        Builder b = new Builder();
        b.sourceId = sourceId;
        b.eventId = eventId;
        b.kind = kind;
        return b;
    }

    /** Copy with every attribute preserved; useful for adapters that only add provenance. */
    public Builder toBuilder() {
        Builder b = new Builder();
        b.sourceId = sourceId; b.eventId = eventId; b.kind = kind; b.originKind = originKind;
        b.pokemonId = pokemonId; b.formId = formId; b.name = name;
        b.lat = lat; b.lon = lon; b.precision = coordinatePrecision;
        b.ivPercent = ivPercent; b.ivAtk = ivAtk; b.ivDef = ivDef; b.ivSta = ivSta;
        b.cp = cp; b.level = level; b.gender = gender; b.size = size; b.weatherBoost = weatherBoost;
        b.pvpGreatRank = pvpGreatRank; b.pvpUltraRank = pvpUltraRank; b.shinyObserved = shinyObserved;
        b.observedAt = observedAt; b.retrievedAt = retrievedAt; b.expiresAt = expiresAt;
        b.isDemo = isDemo; b.dataQuality = dataQuality; b.coverage = coverage;
        b.sourceUrl = sourceUrl; b.note = note;
        return b;
    }

    // --- validation ------------------------------------------------------------------------

    /** True when the origin supplied usable coordinates (range-checked, not the 0,0 legend). */
    public boolean hasCoordinates() { return lat != null && lon != null; }

    /** True when a real expiry was reported and has passed. */
    public boolean expired(long now) { return expiresAt > 0 && expiresAt <= now; }

    /** Remaining milliseconds, or {@link Long#MAX_VALUE} when the origin reported no expiry. */
    public long remainingMs(long now) { return expiresAt <= 0 ? Long.MAX_VALUE : expiresAt - now; }

    /**
     * Gate for copy/map/teleport. Never true for expired rows, approximate coordinates, demo rows
     * or rows whose origin only holds a reveal code.
     */
    public boolean isActionable(long now) {
        return !isDemo && coordinatePrecision == CoordinatePrecision.EXACT && hasCoordinates()
                && !expired(now);
    }

    /** Great-circle distance in kilometres from a centre, or {@link Double#NaN} when unknown. */
    public double distanceKm(double centerLat, double centerLon) {
        if (!hasCoordinates()) return Double.NaN;
        return distanceKmStatic(centerLat, centerLon, lat, lon);
    }

    /** Haversine distance; the caller validates the inputs. */
    public static double distanceKmStatic(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 6371.0088 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    /** Range + null-island validation shared by every parser and importer. */
    public static boolean validCoordinates(Double lat, Double lon) {
        if (lat == null || lon == null) return false;
        double la = lat, lo = lon;
        if (Double.isNaN(la) || Double.isNaN(lo) || Double.isInfinite(la) || Double.isInfinite(lo)) return false;
        if (la < -90 || la > 90 || lo < -180 || lo > 180) return false;
        return !(Math.abs(la) < 1e-9 && Math.abs(lo) < 1e-9);
    }

    /** Parses a numeric coordinate and returns {@code null} instead of guessing. */
    public static Double coordinate(Object raw) {
        if (raw == null) return null;
        double value;
        try {
            value = raw instanceof Number ? ((Number) raw).doubleValue()
                    : Double.parseDouble(String.valueOf(raw).trim());
        } catch (RuntimeException invalid) {
            return null;
        }
        return Double.isFinite(value) ? value : null;
    }

    /** Parses an integer attribute, returning {@code null} for anything absent or malformed. */
    public static Integer integer(Object raw) {
        if (raw == null) return null;
        try {
            if (raw instanceof Number) return (int) Math.rint(((Number) raw).doubleValue());
            return Integer.parseInt(String.valueOf(raw).trim());
        } catch (RuntimeException invalid) {
            return null;
        }
    }

    /** Parses a decimal attribute, returning {@code null} for anything absent or malformed. */
    public static Double decimal(Object raw) {
        if (raw == null) return null;
        try {
            double value = raw instanceof Number ? ((Number) raw).doubleValue()
                    : Double.parseDouble(String.valueOf(raw).trim());
            return Double.isFinite(value) ? value : null;
        } catch (RuntimeException invalid) {
            return null;
        }
    }

    /** Normalises the gender spellings seen across sources, or returns {@code null} when absent. */
    public static String normalizeGender(Object raw) {
        if (raw == null) return null;
        String value = String.valueOf(raw).trim();
        if (value.isEmpty()) return null;
        String low = value.toLowerCase(java.util.Locale.ROOT);
        if (low.equals("1") || low.startsWith("m")) return "MALE";
        if (low.equals("2") || low.startsWith("f")) return "FEMALE";
        if (low.equals("3") || low.equals("n") || low.equals("genderless") || low.equals("none")) return "GENDERLESS";
        return null;
    }

    /** Normalises a size label to XXS/XS/S/M/L/XL/XXL, or returns {@code null} when unknown. */
    public static String normalizeSize(Object raw) {
        if (raw == null) return null;
        String value = String.valueOf(raw).trim().toUpperCase(java.util.Locale.ROOT);
        if (value.isEmpty()) return null;
        if (value.equals("1") || value.equals("XXS")) return "XXS";
        if (value.equals("2") || value.equals("XS")) return "XS";
        if (value.equals("3") || value.equals("S") || value.equals("MEDIUM") || value.equals("NORMAL")) return "S";
        if (value.equals("4") || value.equals("M")) return "M";
        if (value.equals("5") || value.equals("XL")) return "XL";
        if (value.equals("6") || value.equals("XXL")) return "XXL";
        return null;
    }

    /** Human label that always keeps the origin visible. */
    public String sourceLabel() {
        String label = SourceRegistry.label(sourceId);
        return label == null ? sourceId : label;
    }

    // --- JSON ------------------------------------------------------------------------------

    public JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("source_id", sourceId);
        o.put("event_id", eventId);
        o.put("kind", kind.name().toLowerCase(java.util.Locale.ROOT));
        o.put("origin_kind", originKind.wire);
        if (pokemonId != null) o.put("pokemon_id", pokemonId.intValue());
        if (formId != null) o.put("form_id", formId.intValue());
        if (!name.isEmpty()) o.put("name", name);
        if (lat != null) o.put("latitude", lat.doubleValue());
        if (lon != null) o.put("longitude", lon.doubleValue());
        o.put("coordinate_precision", coordinatePrecision.wire);
        if (ivPercent != null) o.put("iv_percent", ivPercent.doubleValue());
        if (ivAtk != null) o.put("iv_atk", ivAtk.intValue());
        if (ivDef != null) o.put("iv_def", ivDef.intValue());
        if (ivSta != null) o.put("iv_sta", ivSta.intValue());
        if (cp != null) o.put("cp", cp.intValue());
        if (level != null) o.put("level", level.intValue());
        if (gender != null) o.put("gender", gender);
        if (size != null) o.put("size", size);
        if (weatherBoost != null) o.put("weather_boost", weatherBoost.booleanValue());
        if (pvpGreatRank != null) o.put("pvp_great_rank", pvpGreatRank.intValue());
        if (pvpUltraRank != null) o.put("pvp_ultra_rank", pvpUltraRank.intValue());
        if (shinyObserved != null) o.put("shiny_observed", shinyObserved.booleanValue());
        o.put("observed_at", observedAt);
        o.put("retrieved_at", retrievedAt);
        o.put("expires_at", expiresAt);
        o.put("is_demo", isDemo);
        o.put("data_quality", dataQuality);
        if (coverage != null) o.put("coverage", coverage);
        if (sourceUrl != null) o.put("source_url", sourceUrl);
        if (note != null && !note.isEmpty()) o.put("note", note);
        return o;
    }

    /** Reads one canonical event; unknown keys are ignored, malformed numbers become {@code null}. */
    public static SourceEvent fromJson(JSONObject o) throws JSONException {
        if (o == null) throw new JSONException("Evento ausente");
        String sourceId = o.optString("source_id", "");
        String eventId = o.optString("event_id", "");
        if (sourceId.isEmpty() || eventId.isEmpty())
            throw new JSONException("Evento sem source_id/event_id");
        Kind kind = Kind.POKEMON;
        String rawKind = o.optString("kind", "pokemon");
        for (Kind candidate : Kind.values())
            if (candidate.name().toLowerCase(java.util.Locale.ROOT).equals(rawKind)) kind = candidate;
        Builder b = builder(sourceId, eventId, kind);
        b.originKind(OriginKind.fromWire(o.optString("origin_kind", null)));
        b.pokemonId = integer(o.opt("pokemon_id"));
        b.formId = integer(o.opt("form_id"));
        b.name = o.optString("name", "");
        Double lat = coordinate(o.opt("latitude"));
        Double lon = coordinate(o.opt("longitude"));
        if (validCoordinates(lat, lon)) {
            b.lat = lat;
            b.lon = lon;
            b.precision = CoordinatePrecision.fromWire(o.optString("coordinate_precision", "exact"));
            if (b.precision == CoordinatePrecision.UNAVAILABLE) b.precision = CoordinatePrecision.EXACT;
        } else {
            b.lat = null;
            b.lon = null;
            b.precision = CoordinatePrecision.UNAVAILABLE;
        }
        b.ivPercent = decimal(o.opt("iv_percent"));
        b.ivAtk = integer(o.opt("iv_atk"));
        b.ivDef = integer(o.opt("iv_def"));
        b.ivSta = integer(o.opt("iv_sta"));
        b.cp = integer(o.opt("cp"));
        b.level = integer(o.opt("level"));
        b.gender = normalizeGender(o.opt("gender"));
        b.size = normalizeSize(o.opt("size"));
        b.weatherBoost = o.isNull("weather_boost") ? null : Boolean.valueOf(o.optBoolean("weather_boost", false));
        b.pvpGreatRank = integer(o.opt("pvp_great_rank"));
        b.pvpUltraRank = integer(o.opt("pvp_ultra_rank"));
        b.shinyObserved = o.isNull("shiny_observed") ? null : Boolean.valueOf(o.optBoolean("shiny_observed", false));
        b.observedAt = o.optLong("observed_at", 0L);
        b.retrievedAt = o.optLong("retrieved_at", 0L);
        b.expiresAt = o.optLong("expires_at", 0L);
        b.isDemo = o.optBoolean("is_demo", false);
        b.dataQuality = o.optString("data_quality", QUALITY_UNKNOWN);
        b.coverage = o.optString("coverage", null);
        b.sourceUrl = o.optString("source_url", null);
        b.note = o.optString("note", "");
        return b.build();
    }

    public static JSONArray toJsonArray(List<SourceEvent> events) throws JSONException {
        JSONArray array = new JSONArray();
        for (SourceEvent event : events) array.put(event.toJson());
        return array;
    }

    public static List<SourceEvent> listFromJson(JSONArray array) throws JSONException {
        if (array == null) return Collections.emptyList();
        List<SourceEvent> out = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            JSONObject o = array.optJSONObject(i);
            if (o != null) out.add(fromJson(o));
        }
        return out;
    }

    public static final class Builder {
        private String sourceId, eventId;
        private Kind kind;
        private OriginKind originKind;
        private Integer pokemonId, formId;
        private String name;
        private Double lat, lon;
        private CoordinatePrecision precision = CoordinatePrecision.EXACT;
        private Double ivPercent;
        private Integer ivAtk, ivDef, ivSta, cp, level, pvpGreatRank, pvpUltraRank;
        private String gender, size;
        private Boolean weatherBoost, shinyObserved;
        private long observedAt, retrievedAt, expiresAt;
        private boolean isDemo;
        private String dataQuality, coverage, sourceUrl, note;

        private Builder() {}

        public Builder originKind(OriginKind value) { this.originKind = value; return this; }
        public Builder pokemonId(Integer value) { this.pokemonId = value; return this; }
        public Builder formId(Integer value) { this.formId = value; return this; }
        public Builder name(String value) { this.name = value; return this; }
        public Builder coordinates(Double lat, Double lon) {
            this.lat = lat;
            this.lon = lon;
            return this;
        }
        public Builder precision(CoordinatePrecision value) {
            this.precision = value == null ? CoordinatePrecision.EXACT : value;
            return this;
        }
        public Builder ivPercent(Double value) { this.ivPercent = value; return this; }
        public Builder ivAtk(Integer value) { this.ivAtk = value; return this; }
        public Builder ivDef(Integer value) { this.ivDef = value; return this; }
        public Builder ivSta(Integer value) { this.ivSta = value; return this; }
        public Builder cp(Integer value) { this.cp = value; return this; }
        public Builder level(Integer value) { this.level = value; return this; }
        public Builder gender(String value) { this.gender = value; return this; }
        public Builder size(String value) { this.size = value; return this; }
        public Builder weatherBoost(Boolean value) { this.weatherBoost = value; return this; }
        public Builder pvpGreatRank(Integer value) { this.pvpGreatRank = value; return this; }
        public Builder pvpUltraRank(Integer value) { this.pvpUltraRank = value; return this; }
        public Builder shinyObserved(Boolean value) { this.shinyObserved = value; return this; }
        public Builder observedAt(long value) { this.observedAt = value; return this; }
        public Builder retrievedAt(long value) { this.retrievedAt = value; return this; }
        public Builder expiresAt(long value) { this.expiresAt = value; return this; }
        public Builder isDemo(boolean value) { this.isDemo = value; return this; }
        public Builder dataQuality(String value) { this.dataQuality = value; return this; }
        public Builder coverage(String value) { this.coverage = value; return this; }
        public Builder sourceUrl(String value) { this.sourceUrl = value; return this; }
        public Builder note(String value) { this.note = value; return this; }

        public SourceEvent build() { return new SourceEvent(this); }
    }
}
