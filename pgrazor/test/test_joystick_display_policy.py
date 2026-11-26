from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[1]

class JoystickDisplayPolicyTests(unittest.TestCase):
    @unittest.skipUnless(shutil.which("javac") and shutil.which("java"), "JDK unavailable")
    def test_policy(self):
        with tempfile.TemporaryDirectory() as out:
            subprocess.run(["javac", "-d", out, str(ROOT / "src/dev/pgrazor/JoystickDisplayPolicy.java"),
                            str(ROOT / "test/JoystickDisplayPolicyTest.java")], check=True)
            subprocess.run(["java", "-cp", out, "dev.pgrazor.JoystickDisplayPolicyTest"], check=True)

    def test_temporary_only_and_release_before_tick(self):
        source=(ROOT / "src/dev/pgrazor/Pad.java").read_text(encoding="utf-8")
        gate=source.split("private void refreshJoystickVisibility()",1)[1].split("private void refresh()",1)[0]
        self.assertIn("Control.joystickVisible()",gate)
        self.assertIn("Control.catchScreenVisible()",gate)
        self.assertIn("activity.hasWindowFocus()",gate)
        self.assertIn("stick.release(true)",gate)
        self.assertNotIn("Control.setJoystickVisible",gate)
        self.assertIn("refreshJoystickVisibility();\n            stick.applyMotion(now);",source)
