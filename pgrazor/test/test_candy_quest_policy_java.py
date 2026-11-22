"""Compile and exercise the Candy quest validator on JVM without Android or network."""
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest

PROJECT = Path(__file__).resolve().parents[1]


class CandyQuestPolicyJavaTest(unittest.TestCase):
    def test_candy_quest_guard(self):
        javac = shutil.which("javac")
        java = shutil.which("java")
        if not javac or not java:
            self.skipTest("JDK 17 not available")
        with tempfile.TemporaryDirectory(prefix="pgbr-quest-") as out:
            compiled = subprocess.run(
                [javac, "-encoding", "UTF-8", "-d", out,
                 str(PROJECT / "src/dev/pgrazor/CandyQuestPolicy.java"),
                 str(PROJECT / "test/CandyQuestPolicyTest.java")],
                capture_output=True, text=True, timeout=25)
            self.assertEqual(compiled.returncode, 0, compiled.stdout + compiled.stderr)
            ran = subprocess.run([java, "-cp", out, "dev.pgrazor.CandyQuestPolicyTest"],
                                 capture_output=True, text=True, timeout=15)
            self.assertEqual(ran.returncode, 0, ran.stdout + ran.stderr)
            self.assertIn("PASS Candy quest", ran.stdout)
