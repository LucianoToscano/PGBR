"""Exercise the actual PGSharp free/Standard entitlement model on the host JVM."""
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[1]


def jdk_tools():
    """Returns (javac, java) or skips: a missing JDK must not be reported as a code failure."""
    compiler, runtime = shutil.which("javac"), shutil.which("java")
    if not compiler or not runtime:
        raise unittest.SkipTest("JDK ausente: este teste compila Java de verdade")
    return compiler, runtime


class PgSharpPolicyJavaTests(unittest.TestCase):
    def test_entitlements(self):
        javac_bin, java_bin = jdk_tools()
        with tempfile.TemporaryDirectory(prefix="pgbr-pgsharp-policy-") as tmp:
            src = ROOT / "src/dev/pgrazor/PgSharpFeedPolicy.java"
            test = ROOT / "test/PgSharpFeedPolicyTest.java"
            javac = subprocess.run([javac_bin, "-encoding", "UTF-8",
                                    "-d", tmp, str(src), str(test)],
                                   capture_output=True, text=True, timeout=40)
            self.assertEqual(javac.returncode, 0, javac.stdout + javac.stderr)
            run = subprocess.run([java_bin, "-cp", tmp, "dev.pgrazor.PgSharpFeedPolicyTest"],
                                 capture_output=True, text=True, timeout=20)
            self.assertEqual(run.returncode, 0, run.stdout + run.stderr)
            self.assertIn("passed", run.stdout)


if __name__ == "__main__":
    unittest.main()
