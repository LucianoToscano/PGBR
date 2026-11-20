package dev.pgrazor;

/** Deterministic visual-only tests: animation must not affect movement, route or permissions. */
public final class EternatusMorphTest {
    private static void check(boolean ok, String label) {
        if (!ok) throw new AssertionError(label);
    }

    private static void near(float actual, float wanted, String label) {
        check(Math.abs(actual - wanted) < 0.003f, label + ": " + actual + " != " + wanted);
    }

    public static void main(String[] args) {
        near(EternatusMorph.blend(0), 0, "initial clean");
        near(EternatusMorph.blend(1400), 0, "fade in start");
        near(EternatusMorph.blend(2900), .5f, "fade in midpoint");
        near(EternatusMorph.blend(4400), 1, "storm reached");
        near(EternatusMorph.blend(5500), 1, "storm hold");
        near(EternatusMorph.blend(8000), .5f, "fade out midpoint");
        near(EternatusMorph.blend(9500), 0, "returned clean");
        near(EternatusMorph.blend(10800), 0, "seamless loop");
        near(EternatusMorph.blend(10800+2900), .5f, "second loop");
        check(EternatusMorph.overlayAlpha(0) == 0, "zero alpha clean");
        check(EternatusMorph.overlayAlpha(4400) == 255, "full alpha storm");
        check(EternatusMorph.overlayAlpha(9500) == 0, "zero alpha on return");

        float last=0;
        for(int millis=1400;millis<4400;millis+=20) {
            float blend=EternatusMorph.blend(millis);
            check(blend>=last && blend<=1,"fade in monotonic");
            last=blend;
        }
        last=1;
        for(int millis=6500;millis<9500;millis+=20) {
            float blend=EternatusMorph.blend(millis);
            check(blend<=last && blend>=0,"fade out monotonic");
            last=blend;
        }
        System.out.println("PASS: EternatusMorph seamless clean/storm transitions");
    }
}
