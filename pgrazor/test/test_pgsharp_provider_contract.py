"""Retirement of the unused PGSharp license UI; native searches remain independent."""
from pathlib import Path
import re
import unittest

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "src" / "dev" / "pgrazor"


class PgSharpProviderContractTests(unittest.TestCase):
    def test_menu_keeps_all_searches_in_order(self):
        menu = (SRC / "FeedChooser.java").read_text(encoding="utf-8")
        expected = [
            "LiveFeedDialog.showPokemonSearch(activity)",
            "LiveFeedDialog.showRaidSearch(activity)",
            "LiveFeedDialog.show(activity)",
            "PogoFeedDialog.show(activity)",
            "HundoFeedDialog.show(activity)",
            "SnipeSlotsDialog.show(activity)",
            "SniperPanel.show(activity, SpawnProfiles.SLOT_ONE)",
            "SniperPanel.show(activity, SpawnProfiles.SLOT_TWO)",
        ]
        self.assertEqual(re.findall(r'which == (\d+)\) ([^;]+);', menu),
                         [(str(i), action) for i, action in enumerate(expected)])
        labels = menu.split("String[] labels = {", 1)[1].split("};", 1)[0]
        self.assertEqual(len(re.findall(r'^\s*"', labels, re.M)), len(expected))
        self.assertNotIn("key opcional", menu)
        self.assertNotIn("PGSharp · 100% IV grátis / licença", menu)

    def test_no_license_dialog_or_vault(self):
        self.assertFalse((SRC / "PgSharpFeedDialog.java").exists())
        self.assertFalse((SRC / "PgSharpLicenseVault.java").exists())
        menu = (SRC / "FeedChooser.java").read_text(encoding="utf-8")
        self.assertNotIn("PgSharpFeedDialog", menu)
        self.assertNotIn("PgSharpLicenseVault", menu)

    def test_provider_entitlements_are_not_bypassed(self):
        policy = (SRC / "PgSharpFeedPolicy.java").read_text(encoding="utf-8")
        for snippet in ("FREE_100_IV", "STANDARD_FILTERS", "Connector.UNVERIFIED",
                        "key == KeyStatus.VERIFIED_BY_PROVIDER"):
            self.assertIn(snippet, policy)

    def test_local_import_is_preserved(self):
        self.assertTrue((SRC / "PgSharpDataImport.java").is_file())


if __name__ == "__main__":
    unittest.main()
