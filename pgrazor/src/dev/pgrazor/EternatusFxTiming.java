package dev.pgrazor;

/**
 * Pure timing model for the Eternatus visual storm.
 * Clean -> storm approaches and illuminates -> rotating vortex -> fades to clean.
 * Uses the same established reversible morph cycle, without touching gameplay.
 */
final class EternatusFxTiming {
    private EternatusFxTiming() {}

    static float strength(long uptimeMs, int intensity) {
        if (intensity <= 0) return 0f;
        float morph = EternatusMorph.blend(uptimeMs);
        if (morph <= 0f) return 0f;
        float wave = (float)(.87 + .13 * Math.sin(uptimeMs * .0031));
        return Math.max(0f, Math.min(1f,
            morph * wave * (intensity >= 2 ? 1f : .59f)));
    }

    static float rotation(long uptimeMs, int speed, boolean reverse) {
        // 0 = slow, 1 = normal, 2 = fast. Counter-rotation creates real vortex motion.
        long period = speed >= 2 ? 5500L : speed == 0 ? 12800L : 8000L;
        if (reverse) period = period * 3L / 2L;
        float degrees = (Math.floorMod(uptimeMs, period) * 360f) / period;
        return reverse ? -degrees : degrees;
    }

    static float flash(long uptimeMs, int intensity) {
        if (intensity <= 0) return 0f;
        float storm = EternatusMorph.blend(uptimeMs);
        if (storm < .18f) return 0f;
        // Irregular short glints of diffuse light, not repeated synthetic zigzags.
        long phase = Math.floorMod(uptimeMs, EternatusMorph.PERIOD_MS);
        float a = envelope(phase, 3300, 310);
        float b = envelope(phase, 5150, 235);
        float c = envelope(phase, 7220, 360);
        return Math.min(.48f, storm * (intensity >= 2 ? .40f : .17f)
            * Math.max(a, Math.max(b, c)));
    }

    private static float envelope(long phase, long center, long width) {
        float distance = Math.abs(phase - center) / (float)width;
        if (distance >= 1f) return 0f;
        float falloff = 1f - distance;
        return falloff * falloff;
    }

    static int frameDelayMs(int intensity) {
        return intensity >= 2 ? 33 : 50;
    }
}
