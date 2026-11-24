package dev.pgrazor;

/**
 * Device-free helpers shared by the remote adapters, so the classification and refusal policy can be
 * unit-tested without Android or network: which PokeList category a query asks for, and how a
 * provider refusal (401/403/429) becomes an honest, non-retriable error.
 */
final class SourceAdapterSupport {
    private SourceAdapterSupport() {}

    /** The one PokeList category a query asks for; a mixed query is answered by the first match. */
    static PokeListFeedData.Category categoryFor(SourceQuery query) {
        if (query == null || query.kinds.isEmpty()) return PokeListFeedData.Category.POKEMON;
        if (query.kinds.contains(SourceEvent.Kind.RAID)) return PokeListFeedData.Category.RAID;
        if (query.kinds.contains(SourceEvent.Kind.QUEST)) return PokeListFeedData.Category.QUEST;
        if (query.kinds.contains(SourceEvent.Kind.ROCKET)) return PokeListFeedData.Category.ROCKET;
        return PokeListFeedData.Category.POKEMON;
    }

    /** 401/403/429 are refusals: the app must report them, never retry aggressively or bypass. */
    static boolean isRefusal(int status) {
        return status == 401 || status == 403 || status == 429;
    }

    static SourceUnavailableException refusal(String sourceLabel, int status, long retryAfterMs) {
        boolean refused = isRefusal(status);
        return new SourceUnavailableException(
                sourceLabel + " recusou a consulta (HTTP " + status + "). "
                        + "O PGBR não contorna 401/403/429 nem troca de fornecedor por conta própria.",
                status, retryAfterMs, refused);
    }

    /** Normalises an HTTP error into a refusal-aware exception for a non-refusal status. */
    static SourceUnavailableException unavailable(String sourceLabel, int status, String detail) {
        if (isRefusal(status)) return refusal(sourceLabel, status, 0L);
        return new SourceUnavailableException(
                sourceLabel + " indisponível (HTTP " + status + "): " + detail, status, 0L, false);
    }
}
