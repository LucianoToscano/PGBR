"""Host-native regression of the exact PGBR C++ transfer policy (no game APK)."""
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest

PROJECT = Path(__file__).resolve().parents[1]


class NativeTransferCutoffTest(unittest.TestCase):
    def test_native_keep_cutoff(self):
        compiler = shutil.which("c++") or shutil.which("g++") or shutil.which("clang++")
        if not compiler:
            self.skipTest("Host C++ compiler not found")
        with tempfile.TemporaryDirectory(prefix="pgbr-iv-") as out:
            exe = str(Path(out) / "keep_iv_test")
            result = subprocess.run(
                [compiler, "-std=c++17", "-O2", "-I", str(PROJECT / "native"),
                 str(PROJECT / "native/transfer_policy.cpp"),
                 str(PROJECT / "test/TransferKeepCutoffTest.cpp"), "-o", exe],
                capture_output=True, text=True, timeout=45)
            self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
            result = subprocess.run([exe], capture_output=True, text=True, timeout=20)
            self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
            self.assertIn("PASS PGBR native IV cutoff", result.stdout)
