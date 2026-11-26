from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1] / "src/dev/pgrazor"

class WorldMapPlaceActionsTests(unittest.TestCase):
    def test_saved_place_actions_reachable_from_library(self):
        source=(ROOT / "WorldMapDialog.java").read_text(encoding="utf-8")
        self.assertIn("dialog.dismiss();showPlace(e);",source)
        self.assertIn("showPlaceActions(entry, dialog)",source)
        self.assertIn("CoordinateClipboard.copy(activity,entry.latitude,entry.longitude",source)
        self.assertIn("store().rename(entry.id,requested)",source)
        self.assertIn("confirmDelete(entry,details)",source)
        self.assertIn("dialog.dismiss();details.dismiss();refreshMarkerLayers();openLibrary();",source)
        self.assertIn("map.select(entry.latitude, entry.longitude)",source)
