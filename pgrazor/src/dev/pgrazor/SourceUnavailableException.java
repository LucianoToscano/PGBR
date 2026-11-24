package dev.pgrazor;

/**
 * Thrown when a source cannot search remotely. Carries the honest reason and, when the provider
 * refused the request, the HTTP status — the UI shows it instead of inventing results or quietly
 * switching to another provider.
 */
public final class SourceUnavailableException extends Exception {
    public final int httpStatus;
    public final long retryAfterMs;
    public final boolean refused;

    public SourceUnavailableException(String reason) {
        this(reason, 0, 0L, false);
    }

    public SourceUnavailableException(String reason, int httpStatus, long retryAfterMs, boolean refused) {
        super(reason);
        this.httpStatus = httpStatus;
        this.retryAfterMs = retryAfterMs;
        this.refused = refused;
    }

    /** True when the provider answered 401/403/429; the app must not retry aggressively. */
    public boolean isRefusal() { return refused; }
}
