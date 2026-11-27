"""Compile and exercise the real read-only Hundo bridge without Android or network."""
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[1]


class PgSharpHundoBridgeTests(unittest.TestCase):
    def test_bridge(self):
        javac, java = shutil.which("javac"), shutil.which("java")
        if not javac or not java:
            self.skipTest("JDK unavailable")
        with tempfile.TemporaryDirectory() as out:
            subprocess.run([javac, "-encoding", "UTF-8", "-d", out,
                            str(ROOT / "src/dev/pgrazor/PgSharpHundoBridge.java"),
                            str(ROOT / "test/PgSharpHundoBridgeTest.java")], check=True, timeout=40)
            result = subprocess.run([java, "-cp", out, "dev.pgrazor.PgSharpHundoBridgeTest"],
                                    check=True, capture_output=True, text=True, timeout=20)
            self.assertIn("passed", result.stdout)


if __name__ == "__main__":
    unittest.main()
