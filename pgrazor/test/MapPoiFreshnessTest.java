package dev.pgrazor;

/** Boundary fixture for the map's freshness status and fail-closed expiry. */
public final class MapPoiFreshnessTest {
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }

    public static void main(String[] args) {
        check(!MapPoiFreshness.isFresh(-1), "unknown age is not fresh");
        check(!MapPoiFreshness.isExpired(-1), "unknown age is not labelled expired");
        check(MapPoiFreshness.isFresh(0), "new snapshot is fresh");
        check(MapPoiFreshness.isFresh(9_999), "snapshot remains fresh below ten seconds");
        check(!MapPoiFreshness.isFresh(10_000), "ten-second boundary expires");
        check(MapPoiFreshness.isExpired(10_000), "expired boundary is observable");
        check(MapPoiFreshness.wholeSeconds(9_999) == 9, "age display floors elapsed seconds");
        check(MapPoiFreshness.wholeSeconds(12_300) == 12, "stale age display floors elapsed seconds");
        check(MapPoiFreshness.wholeSeconds(-1) == 0, "unknown age has safe display value");
        check("fresh-snapshot".equals(new MapPoiFreshness.Snapshot("fresh-snapshot", 9_999, true).value), "fresh data remains available");
        check("".equals(new MapPoiFreshness.Snapshot("fresh-snapshot", 10_000, false).value), "expired points fail closed");
    }
}
