"""LT050: preserve user-provided and proper location names without translation."""
from pathlib import Path
import unittest
ROOT = Path(__file__).resolve().parents[1] / "src/dev/pgrazor"
class Lt050Names(unittest.TestCase):
    def test_map_names_are_verbatim(self):
        s = (ROOT / "WorldMapDialog.java").read_text(encoding="utf-8")
        self.assertIn("labelRaw(ui,entry.name,17)", s)
        self.assertIn("labelRaw(ui,name,17)", s)
        self.assertNotIn("label(ui,entry.name,17)", s)
        self.assertNotIn("label(ui,name,17)", s)
    def test_gpx_and_hotspot_names_are_verbatim(self):
        s = (ROOT / "GpxRouteDialog.java").read_text(encoding="utf-8")
        self.assertIn("setTitle(entry.name)", s)
        self.assertIn("name.setText(current.name);", s)
        self.assertNotIn("PtBr.tr(entry.name)", s)
        self.assertNotIn("PtBr.tr(current.name)", s)
        s = (ROOT / "HotspotsDialog.java").read_text(encoding="utf-8")
        self.assertIn("setTitle(spot.name)", s)
        self.assertNotIn("PtBr.tr(spot.name)", s)
if __name__ == "__main__":
    unittest.main()
