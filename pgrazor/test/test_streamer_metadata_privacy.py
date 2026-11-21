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
        # Read-only game metadata must not be rescanned on every Unity frame.
        self.assertIn("streamer_probe.load(std::memory_order_acquire) != 0", source)
        # Never use a service getter's return as proof of visual name concealment.
        self.assertNotIn('"OnToggleStreamerMode"', source)
        self.assertNotIn("field_set_value(", source)
        self.assertNotIn("runtime_invoke(", source)
        self.assertNotIn("get_PlayerName", source)

    def test_user_warning_and_version(self):
        settings = (ROOT / "src/dev/pgrazor/SettingsPanel.java").read_text(encoding="utf-8")
        self.assertIn("A detecção do serviço não comprova ocultação do nome", settings)
        self.assertIn("Proteção em tela não verificada", settings)
        self.assertIn("Verificar privacidade antes da live", settings)
        self.assertIn("o recurso NÃO protege sua live", settings)
        self.assertIn("o nome original voltou", settings)
        self.assertEqual((ROOT / "VERSION").read_text(encoding="utf-8").strip(), "0.00.14-LT")


if __name__ == "__main__":
    unittest.main()
