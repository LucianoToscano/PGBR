package dev.pgrazor;

/**
 * Deterministic, allocation-free bidirectional crossfade between user-provided clean and storm art.
 * No timers, threads, randomness or new animation state: callers draw only while their View is visible.
 */
final class EternatusMorph {
    static final long PERIOD_MS = 10800L;

    private EternatusMorph() {}

    static float blend(long uptimeMs) {
        long phase = Math.floorMod(uptimeMs, PERIOD_MS);
        if (phase < 1400L) return 0f; // hold clean
        if (phase < 4400L) return ease((phase - 1400f) / 3000f); // clean -> storm
        if (phase < 6500L) return 1f; // hold storm
        if (phase < 9500L) return 1f - ease((phase - 6500f) / 3000f); // storm -> clean
        return 0f; // hold clean before looping
    }

    private static float ease(float progress) {
        float p = Math.max(0f, Math.min(1f, progress));
        return p * p * (3f - 2f * p);
    }

    static int overlayAlpha(long uptimeMs) {
        return Math.max(0, Math.min(255, Math.round(255f * blend(uptimeMs))));
    }
}
