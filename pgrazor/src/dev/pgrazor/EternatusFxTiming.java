package dev.pgrazor;

/**
 * Pure, allocation-free timing model for the Eternatus atmosphere.
 * Continuous, decoupled orbits + independently travelling smoke plumes.
 * No fixed master cycle forces all effects to disappear and restart together.
 */
final class EternatusFxTiming {
    private EternatusFxTiming() {}

    static float strength(long uptimeMs, int intensity) {
        if (intensity <= 0) return 0f;
        float density = EternatusMorph.blend(uptimeMs);
        double slow = Math.sin(uptimeMs * .00071);
        double turbulence = Math.sin(uptimeMs * .00183 + Math.cos(uptimeMs * .00027));
        float wave = (float)(.89 + .065 * slow + .045 * turbulence);
        return Math.max(0f, Math.min(1f, density * wave
            * (intensity >= 2 ? 1f : .59f)));
    }

    static float rotation(long uptimeMs, int speed, boolean reverse) {
        // Different periods, with gentle continuous velocity modulation.
        long period = speed >= 2 ? 5500L : speed == 0 ? 12800L : 8000L;
        if (reverse) period = period * 3L / 2L;
        double angle = Math.floorMod(uptimeMs, period) * (360.0 / period);
        double drift = 6.2 * Math.sin(uptimeMs * (reverse ? .00031 : .00041))
                     + 2.4 * Math.sin(uptimeMs * (reverse ? .00109 : .00131));
        return (float)(reverse ? -(angle + drift) : angle + drift);
    }

    /**
     * Each plume travels outward, vanishes, then respawns while invisible.
     * Unequal periods and distinct offsets break synchronized, mechanical resets.
     */
    static float flow(long uptimeMs, int speed, int lane) {
        int index = Math.max(0, lane);
        long period = (speed >= 2 ? 4300L : speed == 0 ? 8300L : 6200L)
            + index * 719L;
        long offset = index * 1597L + index * index * 281L;
        return Math.floorMod(uptimeMs + offset, period) / (float)period;
    }

    static float flowOpacity(float progress) {
        float p = Math.max(0f, Math.min(1f, progress));
        float envelope = (float)Math.sin(Math.PI * p);
        return envelope * envelope;
    }

    static float flash(long uptimeMs, int intensity) {
        if (intensity <= 0) return 0f;
        // Deterministic irregular flashes: slot contents vary; endpoints are black.
        // Avoids a 3-flash sequence repeating every 10.8 seconds.
        final long slotMs = 2100L;
        long slot = Math.floorDiv(uptimeMs, slotMs);
        long hash = slot * 0x9E3779B97F4A7C15L + 0xD1B54A32D192ED03L;
        hash ^= hash >>> 30;
        hash *= 0xBF58476D1CE4E5B9L;
        hash ^= hash >>> 27;
        hash *= 0x94D049BB133111EBL;
        hash ^= hash >>> 31;
        if ((hash & 7L) == 0L) return 0f;
        long phase = Math.floorMod(uptimeMs, slotMs);
        long center = 450L + Math.floorMod(hash >>> 10, 1200L);
        long width = 170L + Math.floorMod(hash >>> 25, 181L);
        float distance = Math.abs(phase - center) / (float)width;
        if (distance >= 1f) return 0f;
        float falloff = 1f - distance;
        return Math.min(.5f, EternatusMorph.blend(uptimeMs)
            * (intensity >= 2 ? .48f : .20f) * falloff * falloff);
    }

    static int frameDelayMs(int intensity) {
        return intensity >= 2 ? 33 : 50;
    }
}
