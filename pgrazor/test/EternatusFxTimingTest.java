package dev.pgrazor;

/** Pure-JVM tests for continuous visual animation without touching game state. */
public final class EternatusFxTimingTest {
    private static void ok(boolean value,String message) {
        if(!value) throw new AssertionError(message);
    }
    private static void near(float actual,float expected,String message) {
        ok(Math.abs(actual-expected)<.005f,message+": "+actual+" expected "+expected);
    }
    public static void main(String[] args) {
        for(long time=0;time<120000;time+=97) {
            ok(EternatusFxTiming.strength(time,2)>.20f,"living vortex has no blackouts");
            near(EternatusFxTiming.strength(time,0),0f,"storm off means zero");
        }
        ok(EternatusFxTiming.strength(5500,1)<EternatusFxTiming.strength(5500,2),
            "soft is less intense");
        near(EternatusFxTiming.rotation(0,1,false),0f,"starts aligned");
        ok(EternatusFxTiming.rotation(2000,1,false)>70f,
            "CW vortex visible progress");
        ok(EternatusFxTiming.rotation(2000,1,true)<0f,"CCW layer");
        ok(EternatusFxTiming.rotation(2000,2,false)>
            EternatusFxTiming.rotation(2000,0,false),"fast setting works");

        for(int lane=0;lane<4;lane++) {
            float before=EternatusFxTiming.flow(1234,1,lane);
            float after=EternatusFxTiming.flow(1334,1,lane);
            ok(before>=0f&&before<1f&&after>=0f&&after<1f,"plume progress bounded");
        }
        near(EternatusFxTiming.flowOpacity(0f),0f,"spawn invisible");
        near(EternatusFxTiming.flowOpacity(1f),0f,"reset invisible");
        ok(EternatusFxTiming.flowOpacity(.5f)>.99f,"plume visible during travel");
        ok(Math.abs(EternatusFxTiming.flow(1234,1,0)
            -EternatusFxTiming.flow(1234,1,1))>.01f,"staggered smoke lanes");
        ok(Math.abs(EternatusFxTiming.strength(4800,2) -
            EternatusFxTiming.strength(15600,2))>.01f,"no fixed common cycle");
        int flashes=0; for(long t=0;t<120000;t+=31)
            if(EternatusFxTiming.flash(t,2)>.03f) flashes++;
        ok(flashes>5,"irregular glints present");
        near(EternatusFxTiming.flash(5000,0),0f,"off disables flashes");
        ok(EternatusFxTiming.frameDelayMs(2)==33,"epic 30fps");
        ok(EternatusFxTiming.frameDelayMs(1)==50,"soft 20fps");
        System.out.println("PASS: EternatusFxTiming asynchronous smoke and vortex");
    }
}
