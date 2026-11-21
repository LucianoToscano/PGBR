"""Protect both layered, interactive PGBR joystick appearances without Android dependencies."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "src" / "dev" / "pgrazor"
ASSETS = ROOT / "assets" / "map-icons"


def webp_size(data):
    if len(data) < 30 or data[:4] != b"RIFF" or data[8:12] != b"WEBP":
        raise AssertionError("Not a WebP image")
    if int.from_bytes(data[4:8], "little") + 8 != len(data):
        raise AssertionError("Invalid RIFF asset size")
    if data[12:16] != b"VP8X":
        raise AssertionError("Layer must be extended WebP with alpha")
    width = 1 + int.from_bytes(data[24:27], "little")
    height = 1 + int.from_bytes(data[27:30], "little")
    return width, height


class JoystickArtTests(unittest.TestCase):
    def test_all_four_hd_layers(self):
        total = 0
        for theme in ("eternatus", "hud"):
            for part, resolution in (("base", 512), ("thumb", 320)):
                p = ASSETS / f"joystick-{theme}-{part}.webp"
                self.assertTrue(p.is_file(), str(p))
                raw = p.read_bytes()
                total += len(raw)
                self.assertEqual(webp_size(raw), (resolution, resolution))
                self.assertGreater(len(raw), 4000)
        self.assertLess(total, 500000, "Keep the images light for the game overlay")

    def test_rotating_storm_layers(self):
        for layer in ("rim", "vortex"):
            data = (ASSETS / f"joystick-eternatus-{layer}.webp").read_bytes()
            self.assertEqual(webp_size(data), (512, 512))
            self.assertTrue(data[20] & 0x10, f"{layer} needs transparency")
        pad = (SRC / "Pad.java").read_text("utf-8")
        self.assertLess(pad.index("canvas.drawBitmap(stormVortex"), pad.index("canvas.drawBitmap(stormRim"))
        self.assertLess(pad.index("canvas.drawBitmap(stormRim"), pad.index("canvas.drawBitmap(builtinThumb"))

    def test_rendering_layers_drive_real_engine(self):
        pad = (SRC / "Pad.java").read_text("utf-8")
        self.assertIn("canvas.drawBitmap(builtinBase", pad)
        self.assertIn("canvas.drawBitmap(builtinThumb", pad)
        self.assertIn("knobX=cx+x*travel,knobY=cy+y*travel", pad)
        self.assertIn("Control.engine.input(x,-y)", pad)
        self.assertIn("Control.applyCameraInput(", pad)
        self.assertIn("postInvalidateDelayed(80)", pad)
        self.assertIn("Control.joystickEternatusEffects()", pad)
        self.assertIn('"joystick-"+variant+"-base.webp"', pad)

    def test_saved_and_exposed_in_settings(self):
        control = (SRC / "Control.java").read_text("utf-8")
        settings = (SRC / "SettingsPanel.java").read_text("utf-8")
        backup = (SRC / "SettingsBackupRules.java").read_text("utf-8")
        chooser = (SRC / "JoystickImage.java").read_text("utf-8")
        for key in ("joystickStyle", "joystickEternatusEffects"):
            self.assertIn('p.setProperty("' + key + '"', control)
            self.assertIn(key, backup)
        self.assertIn("showJoystickStyles()", settings)
        self.assertIn("Imagem personalizada", settings)
        self.assertIn("joystick-eternatus", (ASSETS / "joystick-eternatus-base.webp").name)
        self.assertIn("Control.setJoystickStyle(3)", chooser)
        build = (ROOT / "build.py").read_text("utf-8")
        self.assertIn("source.glob('*.webp')", build)


if __name__ == "__main__":
    unittest.main()
