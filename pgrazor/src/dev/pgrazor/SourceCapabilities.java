package dev.pgrazor;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Honest, per-source capability matrix. Every filter the app offers is classified as
 * {@link Support#REMOTE} (the origin applied it), {@link Support#LOCAL} (PGBR applies it to the
 * returned rows) or {@link Support#UNSUPPORTED} (the origin has no such field, so the app must say
 * so instead of pretending the filter worked).
 *
 * <p>{@link Status} never upgrades itself: {@link Status#CONNECTED} is only produced by an adapter
 * that actually completed a request against the origin.
 */
public final class SourceCapabilities {
    public enum Status {
        CONNECTED("conectado"), UNVERIFIED("não verificado"), UNAVAILABLE("indisponível"),
        BLOCKED("bloqueado pela fonte"), LOCAL_ONLY("somente local");
        public final String labelPtBr;
        Status(String labelPtBr) { this.labelPtBr = labelPtBr; }
    }

    public enum Support {
        REMOTE("filtro remoto"), LOCAL("filtro local"), UNSUPPORTED("não suportado pela fonte");
        public final String labelPtBr;
        Support(String labelPtBr) { this.labelPtBr = labelPtBr; }
    }

    /** Filter keys shared with {@link SourceQuery}; a source lists the ones it can honour. */
    public static final String SPECIES = "species";
    public static final String FORM = "form";
    public static final String IV_PERCENT = "iv_percent";
    public static final String IV_ADS = "iv_ads";
    public static final String CP = "cp";
    public static final String LEVEL = "level";
    public static final String GENDER = "gender";
    public static final String SIZE = "size";
    public static final String WEATHER = "weather";
    public static final String PVP = "pvp";
    public static final String RADIUS = "radius";
    public static final String KIND = "kind";
    public static final String TTL = "ttl";
    public static final String PAGINATION = "pagination";

    public final String sourceId;
    public final Status status;
    public final OriginKind originKind;
    public final boolean supportsRemoteSearch;
    public final boolean supportsPagination;
    public final boolean liveFeedAvailable;
    public final boolean supportsWebhook;
    public final CoordinatePrecision defaultCoordinatePrecision;
    public final String coverage;
    public final String attribution;
    public final long lastVerifiedAt;
    public final String note;
    private final Map<String, Support> filters;

    private SourceCapabilities(String sourceId, Status status, OriginKind originKind,
                               boolean supportsRemoteSearch, boolean supportsPagination,
                               boolean liveFeedAvailable, boolean supportsWebhook,
                               CoordinatePrecision defaultCoordinatePrecision, String coverage,
                               String attribution, long lastVerifiedAt, String note,
                               Map<String, Support> filters) {
        this.sourceId = sourceId;
        this.status = status;
        this.originKind = originKind;
        this.supportsRemoteSearch = supportsRemoteSearch;
        this.supportsPagination = supportsPagination;
        this.liveFeedAvailable = liveFeedAvailable;
        this.supportsWebhook = supportsWebhook;
        this.defaultCoordinatePrecision = defaultCoordinatePrecision;
        this.coverage = coverage;
        this.attribution = attribution;
        this.lastVerifiedAt = lastVerifiedAt;
        this.note = note;
        this.filters = Collections.unmodifiableMap(new LinkedHashMap<>(filters));
    }

    public static Builder builder(String sourceId) { return new Builder(sourceId); }

    /**
     * Copy with a new runtime status. Used by adapters after a real request: a refusal downgrades the
     * status to {@link Status#BLOCKED}; nothing upgrades it except a completed request.
     */
    public static SourceCapabilities withStatus(SourceCapabilities base, Status status) {
        if (base == null) return null;
        return new SourceCapabilities(base.sourceId, status == null ? base.status : status,
                base.originKind, base.supportsRemoteSearch, base.supportsPagination,
                base.liveFeedAvailable, base.supportsWebhook, base.defaultCoordinatePrecision,
                base.coverage, base.attribution, base.lastVerifiedAt, base.note, base.filters);
    }

    /** Copy with a fresh verification timestamp (only after a real, authorised response). */
    public static SourceCapabilities withLastVerified(SourceCapabilities base, long at) {
        if (base == null) return null;
        return new SourceCapabilities(base.sourceId, base.status, base.originKind,
                base.supportsRemoteSearch, base.supportsPagination, base.liveFeedAvailable,
                base.supportsWebhook, base.defaultCoordinatePrecision, base.coverage,
                base.attribution, at, base.note, base.filters);
    }

    public Support support(String filterKey) {
        Support support = filters.get(filterKey);
        return support == null ? Support.UNSUPPORTED : support;
    }

    public Map<String, Support> filters() { return filters; }

    /** True when the origin applied the filter; the app must not filter twice and then claim both. */
    public boolean isRemote(String filterKey) { return support(filterKey) == Support.REMOTE; }

    public boolean isSupported(String filterKey) { return support(filterKey) != Support.UNSUPPORTED; }

    /** PT-BR explanation used by the UI instead of a silently inert control. */
    public String unsupportedReason(String filterKey) {
        if (isSupported(filterKey)) return "";
        return "Filtro não suportado por " + SourceRegistry.label(sourceId)
                + " (a fonte não informa este campo).";
    }

    public JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("source_id", sourceId);
        o.put("status", status.name().toLowerCase(java.util.Locale.ROOT));
        o.put("origin_kind", originKind.wire);
        o.put("supports_remote_search", supportsRemoteSearch);
        o.put("supports_pagination", supportsPagination);
        o.put("live_feed_available", liveFeedAvailable);
        o.put("supports_webhook", supportsWebhook);
        o.put("coordinate_precision", defaultCoordinatePrecision.wire);
        o.put("coverage", coverage == null ? "" : coverage);
        o.put("attribution", attribution == null ? "" : attribution);
        o.put("last_verified_at", lastVerifiedAt);
        o.put("note", note == null ? "" : note);
        JSONObject f = new JSONObject();
        for (Map.Entry<String, Support> entry : filters.entrySet())
            f.put(entry.getKey(), entry.getValue().name().toLowerCase(java.util.Locale.ROOT));
        o.put("filters", f);
        return o;
    }

    public static final class Builder {
        private final String sourceId;
        private Status status = Status.UNVERIFIED;
        private OriginKind originKind = OriginKind.LIVE_EXTERNAL;
        private boolean supportsRemoteSearch;
        private boolean supportsPagination;
        private boolean liveFeedAvailable;
        private boolean supportsWebhook;
        private CoordinatePrecision precision = SourceEvent.CoordinatePrecision.EXACT;
        private String coverage = "";
        private String attribution = "";
        private long lastVerifiedAt;
        private String note = "";
        private final Map<String, Support> filters = new LinkedHashMap<>();

        private Builder(String sourceId) { this.sourceId = sourceId; }

        public Builder status(Status value) { this.status = value; return this; }
        public Builder originKind(OriginKind value) { this.originKind = value; return this; }
        public Builder remoteSearch(boolean value) { this.supportsRemoteSearch = value; return this; }
        public Builder pagination(boolean value) { this.supportsPagination = value; return this; }
        public Builder liveFeed(boolean value) { this.liveFeedAvailable = value; return this; }
        public Builder webhook(boolean value) { this.supportsWebhook = value; return this; }
        public Builder precision(SourceEvent.CoordinatePrecision value) {
            this.precision = value == null ? SourceEvent.CoordinatePrecision.EXACT : value;
            return this;
        }
        public Builder coverage(String value) { this.coverage = value; return this; }
        public Builder attribution(String value) { this.attribution = value; return this; }
        public Builder lastVerifiedAt(long value) { this.lastVerifiedAt = value; return this; }
        public Builder note(String value) { this.note = value; return this; }
        public Builder supports(String filterKey, Support support) {
            filters.put(filterKey, support);
            return this;
        }

        public SourceCapabilities build() {
            return new SourceCapabilities(sourceId, status, originKind, supportsRemoteSearch,
                    supportsPagination, liveFeedAvailable, supportsWebhook, precision, coverage,
                    attribution, lastVerifiedAt, note, filters);
        }
    }
}
