"""Run the PGBR VGP diagnostic's exact production Java with no Android dependency."""
from pathlib import Path
import subprocess
import tempfile
import unittest

from test_control_java import java_tools

ROOT = Path(__file__).resolve().parents[1]


class VgpHealthReportTests(unittest.TestCase):
    def test_status_and_next_step(self):
        javac, java = java_tools()
        with tempfile.TemporaryDirectory(prefix="pgbr-vgp-health-") as output:
            sources = [
                ROOT / "src/dev/pgrazor/VgpHealthReport.java",
                ROOT / "test/VgpHealthReportTest.java",
            ]
            cp = subprocess.run(
                [str(javac), "-encoding", "UTF-8", "-d", output, *map(str, sources)],
                capture_output=True, text=True, timeout=30,
            )
            self.assertEqual(0, cp.returncode, cp.stderr + cp.stdout)
            run = subprocess.run(
                [str(java), "-cp", output, "dev.pgrazor.VgpHealthReportTest"],
                capture_output=True, text=True, timeout=30,
            )
            self.assertEqual(0, run.returncode, run.stderr + run.stdout)


if __name__ == "__main__":
    unittest.main()
