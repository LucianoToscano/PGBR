package dev.pgrazor;

/**
 * Pure presentation policy for the floating cooldown timer. Green means the displayed
 * cooldown has reached zero at a known location, not that a GPS estimate is missing.
 *
 * A missing history/location is formatted as an em dash by Cooldown.compactLabel(),
 * never as 0:00:00, so it cannot accidentally indicate "safe" to a player.
 */
final class CooldownVisual {
    static final int READY_GREEN = 0xff00da69;
    private CooldownVisual() {}
    static boolean ready(String compactLabel) {
        return "0:00:00".equals(compactLabel);
    }
    static int color(String compactLabel, int configuredColor) {
        return ready(compactLabel) ? READY_GREEN : configuredColor;
    }
}
