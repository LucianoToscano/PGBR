package dev.pgrazor;

/** Atmosphere never reverses through the same clean/storm/clean frames. */
public final class EternatusMorphTest {
    private static void check(boolean ok, String label) {
        if (!ok) throw new AssertionError(label);
    }
    public static void main(String[] args) {
        float low = 1f, high = 0f, previous = EternatusMorph.blend(0);
        for (long time = 0; time < 120000; time += 37) {
            float blend = EternatusMorph.blend(time);
            check(blend >= .40f && blend <= .92f, "bounded, always living storm");
            check(Math.abs(blend - previous) < .02f, "no jump between frames");
            check(EternatusMorph.overlayAlpha(time) > 0, "no forced clean blackout");
            low = Math.min(low, blend);
            high = Math.max(high, blend);
            previous = blend;
        }
        check(high - low > .12f, "organic density must still change");
        check(Math.abs(EternatusMorph.blend(2900) -
            EternatusMorph.blend(13700)) > .01f, "no 10.8 second repeat");
        System.out.println("PASS: EternatusMorph continuous nonrepeating atmospheric density");
    }
}
