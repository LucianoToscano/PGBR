"""Regression coverage for a truly spinning, reversible Eternatus storm."""
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest

ROOT=Path(__file__).resolve().parents[1]
SRC=ROOT/"src"/"dev"/"pgrazor"

class EternatusStormFxTests(unittest.TestCase):
    def test_jvm_cycle_lighting_and_vortex(self):
        javac=shutil.which("javac")
        java=shutil.which("java")
        if not javac or not java:
            self.skipTest("JDK is not installed")
        with tempfile.TemporaryDirectory(prefix="pgbr-eternatus-fx-") as directory:
            subprocess.run([javac,"-encoding","UTF-8","-d",directory,
                str(SRC/"EternatusMorph.java"),
                str(SRC/"EternatusFxTiming.java"),
                str(ROOT/"test"/"EternatusFxTimingTest.java")],check=True)
            completed=subprocess.run([java,"-cp",directory,
                "dev.pgrazor.EternatusFxTimingTest"],
                capture_output=True,text=True,check=True)
            self.assertIn("PASS: EternatusFxTiming",completed.stdout)

    def test_joystick_has_two_real_rotating_clipped_layers(self):
        fx=(SRC/"EternatusVisualEffects.java").read_text("utf8")
        pad=(SRC/"Pad.java").read_text("utf8")
        self.assertIn("canvas.clipPath(clip)",fx)
        self.assertIn("canvas.rotate(EternatusFxTiming.rotation(now, speed, false))",fx)
        self.assertIn("canvas.rotate(EternatusFxTiming.rotation(now, speed, true))",fx)
        self.assertIn("canvas.drawBitmap(vortex",fx)
        self.assertIn("canvas.drawBitmap(smoke",fx)
        self.assertIn("RadialGradient",fx)
        self.assertIn("SweepGradient",fx)
        self.assertIn("stormFx.joystick(canvas,fxVortex!=null?fxVortex:stormVortex",pad)
        self.assertIn('fxVortex=art("eternatus-fx-vortex.png")',pad)
        self.assertIn('fxSmoke=art("eternatus-fx-soft-smoke.png")',pad)
        self.assertIn('fxWispy=art("eternatus-fx-smoke-wispy.png")',pad)
        for texture in ("vortex", "soft-smoke", "smoke-wispy"):
            sprite=ROOT/"assets"/"map-icons"/f"eternatus-fx-{texture}.png"
            self.assertTrue(sprite.is_file(),str(sprite))
            self.assertGreater(sprite.stat().st_size,5000)
        self.assertIn("knobX=cx+x*travel,knobY=cy+y*travel",pad)
        self.assertIn("Control.engine.input(x,-y)",pad)
        self.assertIn("canvas.drawBitmap(builtinThumb",pad)
        self.assertNotIn("storm.draw(canvas",pad)

    def test_ball_has_independent_storm_aura_and_glow(self):
        fx=(SRC/"EternatusVisualEffects.java").read_text("utf8")
        pad=(SRC/"Pad.java").read_text("utf8")
        self.assertIn("void ballBehind(",fx)
        self.assertIn("void ballFront(",fx)
        self.assertIn("stormFx.ballBehind(canvas",pad)
        self.assertIn('stormSmoke=read("pgrazor/map-icons/eternatus-fx-smoke-wispy.png")',pad)
        self.assertIn("stormFx.ballFront(canvas",pad)
        self.assertIn("canvas.drawBitmap(morphBallStorm",pad)
        self.assertIn("EternatusMorph.overlayAlpha(now)",pad)

    def test_user_can_control_intensity_speed_and_keep_legacy(self):
        control=(SRC/"Control.java").read_text("utf8")
        settings=(SRC/"SettingsPanel.java").read_text("utf8")
        backup=(SRC/"SettingsBackupRules.java").read_text("utf8")
        for item in ("eternatusFxIntensity","eternatusFxSpeed"):
            self.assertIn('p.setProperty("'+item+'"',control)
            self.assertIn(item,backup)
            self.assertIn(item,settings)
        self.assertIn("Eternatus • Vórtice anterior",settings)
        self.assertIn("Intensidade da tempestade",settings)
        self.assertIn("Velocidade do vórtice",settings)
        self.assertIn("joystickEternatusEffects",control)

if __name__=="__main__":
    unittest.main()
