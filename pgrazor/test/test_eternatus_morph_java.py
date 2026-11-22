"""Check the imported layered art, Android renderer integration and the easing curve."""
from pathlib import Path
import subprocess
import shutil
import tempfile
import unittest

ROOT=Path(__file__).resolve().parents[1]
SRC=ROOT/"src"/"dev"/"pgrazor"
ASSET=ROOT/"assets"/"map-icons"

def webp_dims(path):
    b=path.read_bytes()
    assert len(b)>4000 and b[:4]==b"RIFF" and b[8:12]==b"WEBP",path
    assert b[12:16]==b"VP8X",f"Expected extended alpha WebP: {path}"
    assert b[20]&0x10,f"Missing transparent alpha: {path}"
    return 1+int.from_bytes(b[24:27],"little"),1+int.from_bytes(b[27:30],"little")

class EternatusMorphTests(unittest.TestCase):
    def test_user_supplied_art_is_bundled(self):
        names=(
            "joystick-eternatus-morph-clean.webp",
            "joystick-eternatus-morph-storm.webp",
            "joystick-eternatus-morph-thumb.webp",
            "eternatus-ball-morph-clean.webp",
            "eternatus-ball-morph-storm.webp",
        )
        for name in names:
            with self.subTest(name=name):
                self.assertTrue((ASSET/name).is_file(),name)
                w,h=webp_dims(ASSET/name)
                self.assertEqual(w,h)
                self.assertGreaterEqual(w,150)
                self.assertLess((ASSET/name).stat().st_size,300000)
        build=(ROOT/"build.py").read_text("utf8")
        self.assertIn("source.glob('*.webp')",build)

    def test_renderer_keeps_live_thumb_and_uses_image_based_storm(self):
        pad=(SRC/"Pad.java").read_text("utf8")
        control=(SRC/"Control.java").read_text("utf8")
        settings=(SRC/"SettingsPanel.java").read_text("utf8")
        self.assertIn("canvas.drawBitmap(morphStorm",pad)
        self.assertIn("canvas.drawBitmap(morphBallStorm",pad)
        self.assertIn("knobX=cx+x*travel,knobY=cy+y*travel",pad)
        self.assertIn("Control.engine.input(x,-y)",pad)
        self.assertIn("EternatusMorph.overlayAlpha",pad)
        self.assertNotIn("storm.draw(canvas",pad)
        self.assertIn("Eternatus • Vórtice anterior",settings)
        self.assertIn("style<=4",settings)
        self.assertIn("style>4",control)

    def test_jvm_easing_is_reversible_and_deterministic(self):
        javac=shutil.which("javac")
        java=shutil.which("java")
        if not javac or not java:
            self.skipTest("JDK not installed locally")
        with tempfile.TemporaryDirectory(prefix="pgbr-morph-jvm-") as directory:
            work=Path(directory)
            subprocess.run([javac,"-encoding","UTF-8","-d",str(work),
                str(SRC/"EternatusMorph.java"),
                str(ROOT/"test"/"EternatusMorphTest.java")],check=True)
            result=subprocess.run([java,"-cp",str(work),"dev.pgrazor.EternatusMorphTest"],
                text=True,capture_output=True,check=True)
            self.assertIn("PASS: EternatusMorph",result.stdout)

if __name__=="__main__":
    unittest.main()
