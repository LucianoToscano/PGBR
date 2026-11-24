package dev.pgrazor;

/** Stub of the real client, exposing only the exception contract used by the engine tests. */
public class LiveFeedClient {
    public static class FeedException extends Exception {
        public final int status;
        public final long retryAfterMs;

        public FeedException(String message) { this(message, 0, 0); }

        public FeedException(String message, int status, long retryAfterMs) {
            super(message);
            this.status = status;
            this.retryAfterMs = retryAfterMs;
        }
    }
}
