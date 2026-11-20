package dev.pgrazor;
import android.app.Activity;
import android.os.SystemClock;

public final class EternatusRouteRunnerTest {
    private static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }
    public static void main(String[] args) {
        Activity activity = new Activity();
        SystemClock.now = 10000;
        Control.lat = 0; Control.lon = 0;
        GpxRoute route = new GpxRoute("Eternatus", new double[][]{
            {0, 0}, {0, 0.005}, {0, 0.01}
        });
        check(EternatusRouteRunner.chooseRoute(route), "Valid route should be accepted");
        check(!EternatusRouteRunner.chooseTemplate("UNKNOWN"), "Unknown quest must not be accepted");
        check(EternatusRouteRunner.chooseTemplate("THROW_EXCELLENT"), "Observed exact template required");
        check(EternatusRouteRunner.start(activity), "Start should pass readiness checks");
        check(EternatusRouteRunner.active(), "Route active");
        check(EternatusRouteRunner.currentStop() == 1 && Control.teleports == 1, "First stop teleported");
        check(!Control.quickspin, "Quick-spin must wait for cooldown");
        check(Control.throwGrade == 3 && Control.throwVariety == 0,
            "Eternatus run must request Excellent throws without random variety");
        Cooldown.remainingMs = 60000;
        SystemClock.now += 1000;
        EternatusRouteRunner.tick();
        check(!Control.quickspin && !Control.vgpEnabled, "No spin or VGP under cooldown");
        Cooldown.remainingMs = 0;
        SystemClock.now += 4000;
        EternatusRouteRunner.tick();
        check(Control.quickspin && Control.vgpEnabled, "Start existing tools when ready");
        EternatusRouteRunner.onClaim("UNRELATED","quest0");
        check(EternatusRouteRunner.currentStop() == 1, "Ignore other quest claims");
        EternatusRouteRunner.onClaim("THROW_EXCELLENT","quest1");
        check(EternatusRouteRunner.currentStop() == 2 && Control.teleports == 2, "Advance after exact claim");
        check(!Control.quickspin && !Control.vgpEnabled, "Disable tools immediately after travel");
        EternatusRouteRunner.onClaim("THROW_EXCELLENT","quest1");
        check(EternatusRouteRunner.currentStop() == 2, "Duplicate reply must not skip stop");
        SystemClock.now += 4000;
        EternatusRouteRunner.tick();
        EternatusRouteRunner.onClaim("THROW_EXCELLENT","quest2");
        check(EternatusRouteRunner.currentStop() == 3 && Control.teleports == 3, "Second claim advances");
        SystemClock.now += 4000;
        EternatusRouteRunner.tick();
        EternatusRouteRunner.onClaim("THROW_EXCELLENT","quest3");
        check(!EternatusRouteRunner.active(), "Last stop should finish");
        check(!EternatusMission.enabled, "Native research mode disarmed");
        check(!Control.quickspin && !Control.vgpEnabled && !Control.vgpOverlay,
            "Existing overlay/tool preferences restored");
        check(Control.throwGrade == 2 && Control.throwVariety == 2,
            "Original throw configuration must be restored");
        // A stop without the chosen research may only be skipped by the player,
        // never recorded as a candy claim or advanced by a duplicate native reply.
        check(EternatusRouteRunner.start(activity),"Second route starts after clean shutdown");
        Cooldown.remainingMs=9000;
        check(!EternatusRouteRunner.skipCurrentStop(),"Manual skip obeys cooldown");
        Cooldown.remainingMs=0;
        check(EternatusRouteRunner.skipCurrentStop(),"Manual skip moves only to next stop");
        check(EternatusRouteRunner.currentStop()==2 && Control.teleports==5,
            "Manual skip goes to second stop exactly once");
        check(!EternatusRouteRunner.onClaim("THROW_EXCELLENT","quest1"),
            "Old duplicate quest id never counts on new stop");
        EternatusRouteRunner.onResearchError("THROW_EXCELLENT","Quest refused");
        check(EternatusRouteRunner.status().contains("Quest refused"),"Native error details preserved");
        SystemClock.now+=4500;
        EternatusRouteRunner.tick();
        check(EternatusRouteRunner.status().contains("Quest refused"),"Native error persists after heartbeat");
        check(!EternatusRouteRunner.onClaim("THROW_EXCELLENT",""),
            "Missing quest identity cannot advance or count");
        EternatusRouteRunner.stop("Teste encerrado.");
        Control.live=false;
        check(EternatusRouteRunner.readinessError(activity).contains("desativado"),
            "Preflight identifies the exact reason instead of generic failure");
        Control.live=true;
        try {
            new GpxRoute("invalid", new double[][]{{91, 0}});
            throw new AssertionError("Invalid coordinates were accepted");
        } catch (IllegalArgumentException expected) {
            // Real GPX validation runs before the coordinator is given a route.
        }
        System.out.println("PASS: EternatusRouteRunner");
    }
}
