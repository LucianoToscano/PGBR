"""Regression: streamer metadata is observation-only, not a game-state toggle."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]


class StreamerMetadataPrivacyTests(unittest.TestCase):
    def test_native_probe_is_read_only(self):
        source = (ROOT / "native/trainer_privacy_ui.cpp").read_text(encoding="utf-8")
        self.assertIn('"Niantic.Holoholo.StreamerMode"', source)
        self.assertIn('"StreamerModeService"', source)
        self.assertIn('"get_IsActive"', source)
        self.assertIn('"get_IsHiddenInfoPanelEnabled"', source)
        self.assertIn("md->resolve_class(", source)
        self.assertIn("md->validate_method(", source)
        self.assertIn("probe_game_streamer_mode(md);", source)
        # Never use a service getter's return as proof of visual name concealment.
        self.assertNotIn('"OnToggleStreamerMode"', source)
        self.assertNotIn("field_set_value(", source)
        self.assertNotIn("runtime_invoke(", source)
        self.assertNotIn("get_PlayerName", source)

    def test_user_warning_and_version(self):
        settings = (ROOT / "src/dev/pgrazor/SettingsPanel.java").read_text(encoding="utf-8")
        self.assertIn("A detecção do serviço não comprova ocultação do nome", settings)
        self.assertIn("Proteção em tela não verificada", settings)
        version = (ROOT / "VERSION").read_text(encoding="utf-8").strip()
        self.assertRegex(version, r"^[0-9]+\.[0-9]+\.[0-9]+-LT$")
        readme = (ROOT.parent / "README.md").read_text(encoding="utf-8")
        self.assertIn("PGBR-v" + version, readme)
        self.assertTrue((ROOT.parent / "docs" / "releases" / ("v" + version + ".md")).is_file())


if __name__ == "__main__":
    unittest.main()
