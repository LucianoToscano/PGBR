package dev.pgrazor;

/**
 * Zoom and world-size limits of the World Map, independent of Android.
 *
 * <p>The view had the same literals ({@code 0..19}, {@code tileSize * 2^19} and the pinch floor)
 * repeated in the zoom buttons, the pinch handler and the world view. Duplicated bounds drift: one
 * edited copy lets the view ask for a zoom the tile source cannot serve, and the map simply stays
 * blank with no error text. Keeping the arithmetic here also makes it testable on a plain JVM.
 *
 * <p>Level 0 is special: it draws the whole world in one tile stretched to the shorter screen edge,
 * so its unit is that screen size instead of the tile size used from level 1 on.
 */
public final class MapOptics {
    /** Deepest tile level requested from the tile source; within the OSM limit. */
    public static final int MAX_ZOOM = 19;
    /** Level 0 shows the whole world in a single tile. */
    public static final int MIN_ZOOM = 0;

    private MapOptics() {}

    /** Clamps any zoom, including the 0 used for the "whole world" view. */
    public static int clamp(int zoom) {
        return Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, zoom));
    }

    /** Zoom reached after a button press; the result is always drawable. */
    public static int step(int zoom, int amount) {
        return clamp(clamp(zoom) + amount);
    }

    /** Pixels one tile occupies at a given level, with the level-0 special case applied. */
    public static double unit(int level, double tileSize, double minimum) {
        double base = clamp(level) == MIN_ZOOM ? Math.max(1, minimum) : Math.max(1, tileSize);
        return MapProjection.worldPixels(clamp(level), base);
    }

    /** Largest world size in pixels the view may zoom to. */
    public static double worldLimit(double tileSize) {
        return Math.max(1, tileSize) * Math.scalb(1.0, MAX_ZOOM);
    }

    /** Keeps a requested world size inside [one screen pixel, {@link #worldLimit}]. */
    public static double clampWorld(double world, double tileSize, double minimum) {
        double floor = Math.max(1, minimum);
        double limit = worldLimit(tileSize);
        if (limit < floor) limit = floor; // absurd density: never invert the interval
        if (!Double.isFinite(world)) return floor;
        return Math.max(floor, Math.min(limit, world));
    }

    /**
     * Tile level that shows a world of {@code world} pixels, floored to the nearest tile grid and
     * clamped, so a pinch can never resolve to a level outside {@link #MIN_ZOOM}..{@link #MAX_ZOOM}.
     */
    public static int levelFor(double world, double tileSize, double minimum) {
        double clamped = clampWorld(world, tileSize, minimum);
        // Anything at or below the shorter screen edge is the whole-world view (level 0); its unit is
        // the screen size, so measuring it against the tile size would report a deeper level.
        if (clamped <= Math.max(1, minimum)) return MIN_ZOOM;
        double base = Math.max(1, tileSize);
        int level = (int) Math.floor(Math.log(clamped / base) / Math.log(2));
        return clamp(level);
    }

    /** Extra scale inside the current level (1 means the tile grid fits the world exactly). */
    public static double scaleFor(double world, double tileSize, double minimum, int level) {
        double clamped = clampWorld(world, tileSize, minimum);
        double unit = unit(level, tileSize, minimum);
        if (!(unit > 0)) return 1;
        double scale = clamped / unit;
        return Double.isFinite(scale) && scale > 0 ? scale : 1;
    }
}
