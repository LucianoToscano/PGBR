"""Layout-only adoption: VGP+ options retain their existing listeners/prefs."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1] / "src/dev/pgrazor"

class PokematrixLayoutContract(unittest.TestCase):
    def test_vgp_option_grouping_is_local(self):
        ui = (ROOT / "SettingsUi.java").read_text(encoding="utf-8")
        smart = (ROOT / "SmartCatchSettings.java").read_text(encoding="utf-8")
        self.assertIn("new SettingsUi.OptionList(ui)", smart)
        section = ui.split("static final class OptionList", 1)[1].split("static void styleTree", 1)[0]
        self.assertIn("!note(child)", section)
        self.assertIn("child.getVisibility()!=View.VISIBLE", section)
        self.assertIn("gap>=h", section)
        self.assertNotIn("setEnabled(", section)
        self.assertNotIn("setOnClickListener", section)
        self.assertNotIn("setText(", section)
        self.assertIn("preferences.config()", smart)
        self.assertIn("set(\"Premium\",on)", smart)
        self.assertIn("set(\"ShinyOnly\",on)", smart)

if __name__ == "__main__":
    unittest.main()
