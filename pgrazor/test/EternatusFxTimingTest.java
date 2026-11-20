package dev.pgrazor;

/** UI-only storm cycle regression tests. Safe to execute on plain JVM. */
public final class EternatusFxTimingTest {
    private static void ok(boolean value,String message){
        if(!value)throw new AssertionError(message);
    }
    private static void near(float actual,float expected,String message){
        ok(Math.abs(actual-expected)<.005f,message+": "+actual+" expected "+expected);
    }
    public static void main(String[] args){
        // The last storm frame always returns to the original design.
        near(EternatusFxTiming.strength(0,2),0f,"original clean");
        near(EternatusFxTiming.strength(1300,2),0f,"clean hold");
        ok(EternatusFxTiming.strength(2900,2)>.15f,"storm approaches");
        ok(EternatusFxTiming.strength(5500,2)>.7f,"fully lit rotating vortex");
        near(EternatusFxTiming.strength(9600,2),0f,"return to original");
        near(EternatusFxTiming.strength(10800,2),0f,"clean loop seamless");
        near(EternatusFxTiming.strength(10800+1300,2),0f,"next clean hold");

        ok(EternatusFxTiming.strength(5500,1)<EternatusFxTiming.strength(5500,2),
            "soft is less intense");
        near(EternatusFxTiming.strength(5500,0),0f,"off");
        ok(EternatusFxTiming.flash(3300,2)>0f,"diffuse storm illumination");
        near(EternatusFxTiming.flash(1000,2),0f,"no flashes when clean");
        near(EternatusFxTiming.flash(5000,0),0f,"no flashes when disabled");

        near(EternatusFxTiming.rotation(0,1,false),0f,"vortex starts aligned");
        ok(EternatusFxTiming.rotation(2000,1,false)>70f,
            "CW vortex makes visible progress");
        ok(EternatusFxTiming.rotation(2000,1,true)<0f,
            "counter-rotating smoke in opposite direction");
        ok(EternatusFxTiming.rotation(2000,2,false)>
            EternatusFxTiming.rotation(2000,0,false),"faster setting faster");
        near(EternatusFxTiming.rotation(8000,1,false),0f,"rotation wraps smoothly");

        ok(EternatusFxTiming.frameDelayMs(2)==33,"epic redraw budget 30fps");
        ok(EternatusFxTiming.frameDelayMs(1)==50,"soft redraw budget 20fps");
        System.out.println("PASS: EternatusFxTiming clean -> storm -> rotating vortex -> clean");
    }
}
