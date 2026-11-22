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
            "Search radius",
            "Settings backup",
            "Catch it",
            "Dismiss",
            "Export this run's log (.txt)",
            "Galarian bird found — walking and VGP stopped",
            "No log file to save",
            "Stop Auto-Raid first.",
        ):
            with self.subTest(key=key):
                self.assertIn(key, entries)
                self.assertNotEqual(entries[key], key)
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


if __name__ == "__main__":
    unittest.main()
