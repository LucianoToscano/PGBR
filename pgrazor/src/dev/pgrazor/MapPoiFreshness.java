package dev.pgrazor;

/** Monotonic-age policy for the read-only nearby map snapshot. */
final class MapPoiFreshness {
    static final long FRESH_FOR_MILLIS = 10_000L;

    static final class Snapshot {
        final String value;
        final long ageMillis;
        final boolean fresh;

        Snapshot(String value, long ageMillis, boolean fresh) {
            this.value = fresh && value != null ? value : "";
            this.ageMillis = ageMillis;
            this.fresh = fresh;
        }
    }

    private MapPoiFreshness() {}

    static boolean isFresh(long ageMillis) {
        return ageMillis >= 0 && ageMillis < FRESH_FOR_MILLIS;
    }

    static boolean isExpired(long ageMillis) {
        return ageMillis >= FRESH_FOR_MILLIS;
    }

    static long wholeSeconds(long ageMillis) {
        return ageMillis < 0 ? 0 : ageMillis / 1_000L;
    }
}
