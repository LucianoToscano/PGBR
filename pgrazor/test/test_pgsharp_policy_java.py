"""Exercise the actual PGSharp free/Standard entitlement model on the host JVM."""
from pathlib import Path
import subprocess
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[1]


class PgSharpPolicyJavaTests(unittest.TestCase):
    def test_entitlements(self):
        with tempfile.TemporaryDirectory(prefix="pgbr-pgsharp-policy-") as tmp:
            src = ROOT / "src/dev/pgrazor/PgSharpFeedPolicy.java"
            test = ROOT / "test/PgSharpFeedPolicyTest.java"
            javac = subprocess.run(["javac", "-encoding", "UTF-8",
                                    "-d", tmp, str(src), str(test)],
                                   capture_output=True, text=True, timeout=40)
            self.assertEqual(javac.returncode, 0, javac.stdout + javac.stderr)
            run = subprocess.run(["java", "-cp", tmp, "dev.pgrazor.PgSharpFeedPolicyTest"],
                                 capture_output=True, text=True, timeout=20)
            self.assertEqual(run.returncode, 0, run.stdout + run.stderr)
            self.assertIn("passed", run.stdout)


if __name__ == "__main__":
    unittest.main()
