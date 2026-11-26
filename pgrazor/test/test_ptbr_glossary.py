"""Prevent PT-BR glossary drift between non-rooted and Root Android UIs."""
import json
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[2]
GLOSSARY = ROOT / "pgrazor" / "i18n" / "pt-BR.json"
JAVA = (
    ROOT / "pgrazor" / "src" / "dev" / "pgrazor" / "PtBr.java",
    ROOT / "pgrazor" / "root-installer" / "src" / "dev" / "pgrazor" / "root" / "RootPtBr.java",
)


class PtBrGlossaryTests(unittest.TestCase):
    def test_translations_exist_for_critical_menus(self):
        data = json.loads(GLOSSARY.read_text(encoding="utf-8"))
        entries = data["translations"]
        self.assertEqual(data["locale"], "pt-BR")
        self.assertGreaterEqual(len(entries), 1100)
        for key in (
            "Display & Performance",
            "Auto-transfer",
            "Accounts & Backup",
            "My Shinies filters",
            "Route automation stopped.",
            "Auto-claim completed field research",
            "No root access. Grant root to this app in your root manager, then Re-check.",
            "Batch evolution",
            "Teleport",
            "Teleport to selected destination",
            "Teleport cooldown",
            "Search radius",
            "Settings backup",
            "Catch it",
            "Dismiss",
            "Export this run's log (.txt)",
            "Galarian bird found — walking and VGP stopped",
            "No log file to save",
            "Stop Auto-Raid first.",
            "Not running",
            "Quests",
            "Primal",
            "Shiny Scanner (Experimental)",
            "Any tier",
            "Weather filters",
            "All Pokemon",
            "Gym (Blue)",
            "Your account doesn't have access to this feature.",
            " · Tap map to select",
            "Destination: ",
            "Cooldown when you arrive: ",
            "Team: ",
            "Defenders: ",
            "Scanning",
            "Skipped ",
            "Not caught - broke free",
            " (reversible)",
        ):
            with self.subTest(key=key):
                self.assertIn(key, entries)
                self.assertNotEqual(entries[key], key)
        self.assertEqual(entries["Teleport"], "Teleporte")
        self.assertEqual(entries["Teleport cooldown"], "Tempo de espera do teleporte")
        for key, value in entries.items():
            self.assertTrue(key and value, key)

    def test_both_android_translators_are_synchronized(self):
        entries = json.loads(GLOSSARY.read_text(encoding="utf-8"))["translations"]
        for source in JAVA:
            text = source.read_text(encoding="utf-8")
            with self.subTest(source=source.name):
                self.assertEqual(text.count("        m.put("), len(entries))
                for key in ("Display & Performance", "Auto-transfer", "Settings backup", "Batch evolution"):
                    self.assertIn("m.put(" + json.dumps(key, ensure_ascii=False), text)
                self.assertIn("String prefix = null;", text)
                self.assertIn("String suffix = null;", text)
                self.assertIn('source.endsWith("(")', text)
                self.assertIn('source.endsWith("–")', text)

    def test_locais_missing_format_keys(self):
        entries = json.loads(GLOSSARY.read_text(encoding="utf-8"))["translations"]
        for key, value in (
            ("You are at\n%s", "Você está em\n%s"),
            ("Place name (up to %d characters)", "Nome do local (até %d caracteres)"),
        ):
            self.assertEqual(entries[key], value)
            row = "m.put(" + json.dumps(key, ensure_ascii=False) + ", " + json.dumps(value, ensure_ascii=False) + ");"
            for source in JAVA:
                self.assertIn(row, source.read_text(encoding="utf-8"))

    def test_screen_call_sites_apply_translations(self):
        """Keep the screenshot-visible UI translations attached to their render sites."""
        anchors = {
            "BatchEvolutionSettings.java": 'PtBr.tr(message)',
            "GiftBatchSettings.java": 'PtBr.tr("Skipped ")',
            "MapPoiLayer.java": 'PtBr.tr(poiShortLabel(',
            "GpxRouteDialog.java": 'PtBr.tr("%s\\n%d stops")',
            "ResearchSettings.java": 'PtBr.tr(categoryName(category))',
            "CatchOutcomeController.java": 'Toast.makeText(screen,dev.pgrazor.PtBr.tr(message)',
            "TeleportDialog.java": 'PtBr.tr(warning(lat,lon))',
        }
        root = ROOT / "pgrazor" / "src" / "dev" / "pgrazor"
        for filename, required in anchors.items():
            with self.subTest(file=filename):
                self.assertIn(required, (root / filename).read_text(encoding="utf-8"))


if __name__ == "__main__":
    unittest.main()
