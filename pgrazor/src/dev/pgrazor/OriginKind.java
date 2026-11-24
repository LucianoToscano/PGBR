package dev.pgrazor;

/**
 * Provenance of a record. The four values are never interchangeable: a personal import is not a
 * live external feed, a static catalogue is not an active spawn, and a synthetic fixture may only
 * exist inside automated tests.
 */
public enum OriginKind {
    /** Data received live from a third-party service that permitted the request. */
    LIVE_EXTERNAL("live_external", "Fonte remota ao vivo"),
    /** The user's own file or clipboard, imported on this device. */
    PERSONAL_IMPORT("personal_import", "Importação pessoal"),
    /** Static reference data (catalogue, route, saved place); never an active spawn. */
    STATIC_CATALOG("static_catalog", "Catálogo estático"),
    /** Synthetic row used by automated tests only. Never rendered as a real result. */
    SIMULATED_FIXTURE("simulated_fixture", "Demonstração sintética");

    public final String wire;
    public final String labelPtBr;

    OriginKind(String wire, String labelPtBr) {
        this.wire = wire;
        this.labelPtBr = labelPtBr;
    }

    /** Wire value, or {@code null} for anything unrecognised (never silently coerced). */
    public static OriginKind fromWire(String value) {
        if (value == null) return null;
        for (OriginKind kind : values()) if (kind.wire.equals(value)) return kind;
        return null;
    }
}
