package dev.pgrazor;

/**
 * Fail-closed request parameters for the optional iFlowGo source.
 * No HTTP, credentials, game hooks or provider-specific assumption here.
 */
final class IFlowGoRequestPolicy {
    private IFlowGoRequestPolicy() {}

    /** The nearby endpoint covers at most 25 km; +infinity is a UI sentinel, not global coverage. */
    static double nearbyRadiusKm(double requested) {
        if (Double.isNaN(requested) || requested <= 0.0 || requested == Double.NEGATIVE_INFINITY)
            throw new IllegalArgumentException("Invalid nearby radius");
        return Double.isInfinite(requested) ? 25.0 : Math.max(1.0, Math.min(requested, 25.0));
    }

    /** -1 means no single Dex species selected; other invalid numeric IDs must never broaden a query. */
    static String globalSpeciesQuery(int dexId) {
        if (dexId == -1) return "";
        if (dexId < 1 || dexId > 1025)
            throw new IllegalArgumentException("Invalid Pokémon species ID");
        return "&pokemon_id=" + dexId;
    }
}
