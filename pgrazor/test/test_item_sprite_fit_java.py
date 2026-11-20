"""Portable sprite fit test: keeps rectangular item graphics undistorted."""
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[1]

class ItemSpriteFitJavaTest(unittest.TestCase):
    def test_dimensions_and_history_icon_paint(self):
        src = ROOT / "src/dev/pgrazor"
        badge = (src / "ItemBadge.java").read_text(encoding="utf-8")
        cooldown = (src / "Cooldown.java").read_text(encoding="utf-8")
        self.assertIn("ItemSpriteFit.centeredRect(itemSprite.getWidth()", badge)
        self.assertIn("catch(java.io.FileNotFoundException missing)", badge)
        self.assertIn("0xffffffff, 20", cooldown)
        self.assertIn("button.setColorFilter(0xff175448", cooldown)
        javac, java = shutil.which("javac"), shutil.which("java")
        if not javac or not java:
            self.skipTest("Java compiler/runtime unavailable")
        with tempfile.TemporaryDirectory(prefix="pgbr-items-fit-") as d:
            cmd = [javac, "-encoding", "UTF-8", "-d", d,
                str(src / "ItemSpriteFit.java"),
                str(ROOT / "test/ItemSpriteFitTest.java")]
            res = subprocess.run(cmd, capture_output=True, text=True, timeout=30)
            self.assertEqual(res.returncode, 0, res.stdout + res.stderr)
            res = subprocess.run([java, "-cp", d, "dev.pgrazor.ItemSpriteFitTest"],
                                 capture_output=True, text=True, timeout=20)
            self.assertEqual(res.returncode, 0, res.stdout + res.stderr)
            self.assertIn("PASS: ItemSpriteFit", res.stdout)

if __name__ == "__main__":
    unittest.main()
