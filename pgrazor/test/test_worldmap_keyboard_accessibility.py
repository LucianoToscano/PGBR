"""Contract checks for keyboard access to the map picker without causing movement."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "src/dev/pgrazor/WorldMapDialog.java"
PTBR = ROOT / "src/dev/pgrazor/PtBr.java"


class WorldMapKeyboardAccessibilityTests(unittest.TestCase):
    def test_map_is_focusable_and_describes_keyboard_controls_in_ptbr(self):
        source = SOURCE.read_text(encoding="utf-8")
        self.assertIn("setFocusable(true);", source)
        self.assertIn("setFocusableInTouchMode(true);", source)
        key = ("World map. Blue dot is your current location; red pin is the destination. "
               "Drag to pan, pinch to zoom, tap to select destination. Use arrow keys to pan "
               "and Enter to select the map center.")
        self.assertIn(f'setContentDescription(dev.pgrazor.PtBr.tr("{key}"))', source)
        self.assertIn(f'm.put("{key}"', PTBR.read_text(encoding="utf-8"))

    def test_arrows_pan_viewport_and_enter_only_selects_destination(self):
        source = SOURCE.read_text(encoding="utf-8")
        handler = source.split("@Override public boolean onKeyDown(int keyCode,KeyEvent event) {", 1)[1].split(
            "public boolean onTouchEvent(MotionEvent event)", 1)[0]
        for key in ("KEYCODE_DPAD_LEFT", "KEYCODE_DPAD_RIGHT", "KEYCODE_DPAD_UP", "KEYCODE_DPAD_DOWN"):
            self.assertIn("KeyEvent." + key, handler)
        self.assertIn("KeyEvent.KEYCODE_DPAD_CENTER", handler)
        self.assertIn("KeyEvent.KEYCODE_ENTER", handler)
        self.assertIn("MapProjection.offsetX", handler)
        self.assertIn("MapProjection.offsetY", handler)
        self.assertIn("viewportChanged();return true;", handler)
        self.assertIn("selectMapCenter();return true;", handler)
        self.assertNotIn("onDestination(", handler)
        self.assertNotIn("walk.onDestination(", handler)
        self.assertNotIn("MotionEngine", handler)

    def test_touch_and_keyboard_selection_share_non_moving_update(self):
        source = SOURCE.read_text(encoding="utf-8")
        selection = source.split("private void selectCoordinates(double latitude,double longitude) {", 1)[1].split(
            "void select(double latitude, double longitude)", 1)[0]
        self.assertIn("this.latitude=latitude;this.longitude=longitude;selectedResource=\"\";", selection)
        self.assertIn("updateSelection();invalidate();performClick();", selection)
        self.assertIn("selectionLabel.announceForAccessibility(selectionLabel.getText())", selection)
        self.assertNotIn("onDestination(", selection)
        self.assertIn("selectCoordinates(selectedLatitude,selectedLongitude);", source)
        self.assertIn("selectCoordinates(MapProjection.latitude(centerY),MapProjection.longitude(centerX));", source)


if __name__ == "__main__":
    unittest.main()
