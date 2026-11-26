import dev.pgrazor.MapOptics;

/** Zoom/world arithmetic of the World Map, without Android. */
public class MapOpticsTest {
    private static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError(what);
    }
    private static void near(double actual, double expected, String what) {
        if (Math.abs(actual - expected) > 1e-9) throw new AssertionError(what + ": " + actual + " != " + expected);
    }
    public static void main(String[] args) {
        // Buttons stop at both ends instead of producing a negative or unservable level.
        check(MapOptics.step(MapOptics.MAX_ZOOM, 1) == MapOptics.MAX_ZOOM, "zoom above the limit");
        check(MapOptics.step(MapOptics.MIN_ZOOM, -1) == MapOptics.MIN_ZOOM, "zoom below zero");
        check(MapOptics.step(5, 1) == 6 && MapOptics.step(5, -1) == 4, "plain steps");
        check(MapOptics.clamp(-40) == 0 && MapOptics.clamp(400) == 19, "clamp bounds");

        // A pinch beyond the deepest level must not request tiles the source cannot serve.
        double tile = 256, minimum = 1080;
        near(MapOptics.clampWorld(1e30, tile, minimum), MapOptics.worldLimit(tile), "pinch upper bound");
        near(MapOptics.clampWorld(0, tile, minimum), minimum, "pinch lower bound");
        check(MapOptics.levelFor(1e30, tile, minimum) == MapOptics.MAX_ZOOM, "level saturates");
        check(MapOptics.levelFor(1, tile, minimum) == 0, "level floor is the world view");

        // Level 0 uses the shorter screen edge, deeper levels use the tile size.
        near(MapOptics.unit(0, tile, minimum), minimum, "level 0 unit");
        near(MapOptics.unit(3, tile, minimum), tile * 8, "level 3 unit");
        double world = MapOptics.unit(3, tile, minimum);
        check(MapOptics.levelFor(world, tile, minimum) == 3, "world maps back to its level");
        near(MapOptics.scaleFor(world, tile, minimum, 3), 1, "exact grid scale");

        // A scale that happens to be 1 for a pinch result keeps the grid aligned.
        double bigger = MapOptics.unit(4, tile, minimum) * 1.5;
        int level = MapOptics.levelFor(bigger, tile, minimum);
        check(level == 4, "pinch level");
        near(MapOptics.scaleFor(bigger, tile, minimum, level), 1.5, "pinch scale");

        // Degenerate inputs never produce NaN/Infinity or an inverted interval.
        check(MapOptics.levelFor(Double.NaN, tile, minimum) == 0, "NaN world");
        check(Double.isFinite(MapOptics.scaleFor(Double.NaN, tile, minimum, 7)), "NaN scale");
        check(MapOptics.levelFor(1e30, 0, 0) == MapOptics.MAX_ZOOM, "zero tile size");
        System.out.println("PASS: MapOptics clamp/level/scale");
    }
}
