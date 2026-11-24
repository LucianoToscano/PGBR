package dev.pgrazor;

/**
 * The small adapter contract every source implements: capabilities, one page of search, cancel,
 * status and attribution. It deliberately does not promise endpoints that no provider offers — an
 * adapter that cannot search remotely throws {@link SourceUnavailableException} with the reason.
 */
public interface SourceAdapter {
    /** Stable id from {@link SourceRegistry}. */
    String sourceId();

    /** Declared capabilities; the runtime status is refreshed after a real request. */
    SourceCapabilities capabilities();

    /**
     * One page of results. {@code cursor} is {@code null} for the first page; the returned
     * {@code nextCursor} is passed back verbatim for the next one.
     *
     * @throws SourceUnavailableException when this source cannot search remotely (refused, blocked,
     *         unverified or local-only). Never a silent empty list.
     */
    SourceSearchResult search(SourceQuery query, String cursor, long now) throws Exception;

    /** Cancels the in-flight request, if any. Safe to call from the UI thread. */
    void cancel();

    /** Capability snapshot after the last request; never upgrades without evidence. */
    SourceCapabilities status();

    /** Human-readable attribution, including the provider's own terms page when known. */
    String attribution();
}
