"""Run the pure-Java monotonic freshness boundary fixture when a JDK is installed."""
import subprocess
import tempfile
import unittest
from pathlib import Path
from test_control_java import java_tools

ROOT = Path(__file__).resolve().parents[1]


class MapPoiFreshnessJavaTests(unittest.TestCase):
    def test_snapshot_freshness_boundaries_and_safe_expiry(self):
        javac, java = java_tools()
        if not javac or not java:
            self.skipTest("JDK required for executable Java freshness policy fixture")
        with tempfile.TemporaryDirectory() as work:
            subprocess.run([javac, "-encoding", "UTF-8", "-d", work,
                            str(ROOT / "src/dev/pgrazor/MapPoiFreshness.java"),
                            str(ROOT / "test/MapPoiFreshnessTest.java")],
                           check=True, capture_output=True, text=True, timeout=30)
            subprocess.run([java, "-cp", work, "dev.pgrazor.MapPoiFreshnessTest"],
                           check=True, capture_output=True, text=True, timeout=30)

    def test_layer_marks_expiry_separately_from_empty_or_invalid_data(self):
        layer = (ROOT / "src/dev/pgrazor/MapPoiLayer.java").read_text(encoding="utf-8")
        status = layer[layer.index("String message = layers == 0"):layer.index("poiStatus.setText", layer.index("String message = layers == 0"))]
        self.assertIn("MapPoiFreshness.isExpired(snapshotAge)", status)
        self.assertIn('"Nearby game data expired; waiting for refresh"', status)
        self.assertLess(status.index('"Nearby layers unavailable"'), status.index('"Nearby game data expired; waiting for refresh"'))
        self.assertLess(status.index('"Nearby game data expired; waiting for refresh"'), status.index('"Waiting for nearby game locations"'))
        self.assertIn("MapPoiFreshness.Snapshot read()", layer)
        control_test = (ROOT / "test/test_control_java.py").read_text(encoding="utf-8")
        self.assertIn("'Control.java', 'MapPoiFreshness.java'", control_test)


if __name__ == "__main__":
    unittest.main()
