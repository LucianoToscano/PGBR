"""Compile and exercise the actual EternatusRouteRunner against deterministic Android/game shims."""
from pathlib import Path
import subprocess
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[1]

ACTIVITY = """package android.app;
public class Activity {
  public boolean isFinishing(){return false;}
  public boolean isDestroyed(){return false;}
}
"""
CLOCK = """package android.os;
public class SystemClock {
  public static long now;
  public static long elapsedRealtime(){return now;}
}
"""
LOOPER = """package android.os;
public class Looper {
  public static Looper getMainLooper(){return new Looper();}
}
"""
HANDLER = """package android.os;
public class Handler {
  public Handler(Looper looper){}
  public void post(Runnable task){task.run();}
}
"""
DEPS = """package dev.pgrazor;
import android.app.Activity;
final class GpxCodec {static final int MAX_POINTS=1000;}
final class AutoRaid {static boolean running(){return false;}}
final class ShundoSniper {static boolean active(){return false;}}
final class Cooldown {
    static long remainingMs;
    static long remaining(){return remainingMs;}
}
final class ResearchPreferences {
    static boolean validTemplate(String s){return s!=null&&s.matches("[A-Z_]+");}
}
final class ResearchSettings {
    static java.util.List<String[]> observedTemplates(){
        return java.util.Collections.singletonList(new String[]{
            "THROW_EXCELLENT","Make an Excellent Throw","25 Eternatus Candy","v"
        });
    }
}
final class EternatusMission {
    static boolean enabled;
    static boolean start(Activity activity,String template){enabled=true;return true;}
    static void stop(String reason){enabled=false;}
    static boolean active(){return enabled;}
}
final class Control {
    static boolean live=true,can=true,location=true,vgpOverlay,vgpEnabled,quickspin;
    static double lat,lon;
    static int teleports;
    static final Engine engine=new Engine();
    static final class Engine {boolean walking;}
    static boolean active(){return live;}
    static boolean canEnable(){return can;}
    static boolean locationKnown(){return location;}
    static boolean gameRoutesRunning(){return false;}
    static boolean vgpOverlay(){return vgpOverlay;}
    static boolean vgpEnabled(){return vgpEnabled&&vgpOverlay;}
    static boolean quickSpinEnabled(){return quickspin;}
    static void setVgpEnabled(boolean value){vgpEnabled=value;}
    static void setVgpOverlay(boolean value){vgpOverlay=value;if(!value)vgpEnabled=false;}
    static void setQuickSpinEnabled(boolean value){quickspin=value;}
    static void teleport(double latitude,double longitude){lat=latitude;lon=longitude;teleports++;}
    static double currentLatitude(){return lat;}
    static double currentLongitude(){return lon;}
    static void event(String type,String message){}
    static boolean quickSpinAllowed(){return quickspin&&live;}
}
"""

class EternatusRouteJavaTests(unittest.TestCase):
    def test_route_coordinator(self):
        with tempfile.TemporaryDirectory(prefix="pgbr-eternatus-java-") as folder:
            work = Path(folder)
            java_sources = {
                "android/app/Activity.java": ACTIVITY,
                "android/os/SystemClock.java": CLOCK,
                "android/os/Looper.java": LOOPER,
                "android/os/Handler.java": HANDLER,
                "dev/pgrazor/RunnerDeps.java": DEPS,
            }
            for filename, source in java_sources.items():
                path = work / filename
                path.parent.mkdir(parents=True, exist_ok=True)
                path.write_text(source, encoding="utf-8")
            sources = list(work.rglob("*.java")) + [
                ROOT / "src/dev/pgrazor/EternatusRouteRunner.java",
                ROOT / "src/dev/pgrazor/GpxRoute.java",
                ROOT / "src/dev/pgrazor/CooldownState.java",
                ROOT / "test/EternatusRouteRunnerTest.java",
            ]
            import shutil
            compiler, runtime = shutil.which("javac"), shutil.which("java")
            self.assertIsNotNone(compiler)
            self.assertIsNotNone(runtime)
            result = subprocess.run(
                [compiler, "-encoding", "UTF-8", "-d", str(work / "classes"), *map(str, sources)],
                capture_output=True, text=True, timeout=45
            )
            self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
            result = subprocess.run(
                [runtime, "-cp", str(work / "classes"), "dev.pgrazor.EternatusRouteRunnerTest"],
                capture_output=True, text=True, timeout=20
            )
            self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
            self.assertIn("PASS: EternatusRouteRunner", result.stdout)

if __name__ == "__main__":
    unittest.main()
