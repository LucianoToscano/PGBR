"""Offline integration gate for nearby/global iFlowGo scope and invalid numeric filters."""
from pathlib import Path
import subprocess
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[1]

class IFlowGoRequestPolicyTests(unittest.TestCase):
    def test_actual_java_policy(self):
        with tempfile.TemporaryDirectory(prefix="pgbr-iflowgo-policy-") as directory:
            sources = [
                ROOT / "src/dev/pgrazor/IFlowGoRequestPolicy.java",
                ROOT / "test/IFlowGoRequestPolicyTest.java",
            ]
            subprocess.run(["javac", "-encoding", "UTF-8", "-d", directory,
                            *map(str, sources)], check=True)
            subprocess.run(["java", "-cp", directory,
                            "dev.pgrazor.IFlowGoRequestPolicyTest"], check=True)

    def test_ui_never_labels_area_raids_as_global(self):
        ui = (ROOT / "src/dev/pgrazor/LiveFeedDialog.java").read_text(encoding="utf-8")
        self.assertIn("provider == 2 && raidSearchMode && radiusIndex == 3", ui)
        self.assertIn("Global de Reides", ui)
        self.assertIn("provider == 2 && pokemonSearchMode ? 3 : 1", ui)

    def test_client_wires_bounds(self):
        client = (ROOT / "src/dev/pgrazor/IFlowGoFeedClient.java").read_text(encoding="utf-8")
        self.assertIn("IFlowGoRequestPolicy.nearbyRadiusKm(radiusKm)", client)
        self.assertIn("IFlowGoRequestPolicy.globalSpeciesQuery(dexId)", client)

if __name__ == "__main__":
    unittest.main()
