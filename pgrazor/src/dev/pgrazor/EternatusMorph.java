package dev.pgrazor;

/**
 * Non-repeating, allocation-free atmospheric crossfade for the Eternatus art.
 * The storm stays alive while enabled: no synchronized clean -> storm -> clean reset.
 * Several incommensurate slow waves prevent a short, obvious animation loop.
 */
final class EternatusMorph {
    private EternatusMorph() {}

    static float blend(long uptimeMs) {
        double time = Math.max(0L, uptimeMs);
        double slow = Math.sin(time * .00019);
        double swirl = Math.sin(time * .00047 + 1.1);
        double turbulence = Math.sin(time * .00137 + .8 * Math.sin(time * .00023));
        float blend = (float)(.67 + .105 * slow + .085 * swirl + .055 * turbulence);
        return Math.max(.40f, Math.min(.92f, blend));
    }

    static int overlayAlpha(long uptimeMs) {
        return Math.max(0, Math.min(255, Math.round(255f * blend(uptimeMs))));
    }
}
