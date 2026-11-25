"""PT-BR sweep: the interface must not fall back to English for these labels.

`PtBr.tr` returns its argument unchanged when a key is missing, so a translated call
site is not proof of a translated interface. This suite pins the sentences that were
still English, and keeps the dictionary, the non-rooted UI and the Root UI in sync
(mirrors the invariant enforced by .github/workflows/pgbr-ptbr.yml).

No source file is written here.
"""
import json
import re
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
GLOSSARY = ROOT / "pgrazor" / "i18n" / "pt-BR.json"
JAVA = (
    ROOT / "pgrazor" / "src" / "dev" / "pgrazor" / "PtBr.java",
    ROOT / "pgrazor" / "root-installer" / "src" / "dev" / "pgrazor" / "root" / "RootPtBr.java",
)

# Sentences that were still presented in English to the user. Each one is reached from
# a PtBr.tr(...) call site, so the dictionary entry is the whole fix.
REGRESSION_KEYS = (
    "Catch",
    "Transfer",
    "Xfer",
    "Fallback",
    "Feed berries",
    "Catch berry: ",
    "Applies while Auto Ball is enabled.",
    "Applies while VGP+ is running.",
    "Black out the screen to save battery while the sniper runs. "
    "Tap the black screen to bring the game back.",
    "Couldn't save that change. Please try again.",
    "Map cache unavailable. Close map and retry.",
    "No log file is available. Android logcat may contain startup errors.",
    "Open boxes and skip encounters",
    "Opens every visible map box and skips the encounter screen for any Pokemon result.",
    "Prefer GO Safari Balls for Mighty Pokemon. If Safari Balls are unavailable",
    "Stop Shundo Sniper and hide this card",
    "Synthetic location consumer\n",
)

# Already required by the owner: VGP+ keeps a slow speed of about three seconds.
VGP_SPEED_KEYS = ("Slow (about 3 s)",)


def _escape(text):
    return text.replace("\\", "\\\\").replace('"', '\\"').replace("\n", "\\n")


class PtBrSweepTests(unittest.TestCase):
    def setUp(self):
        data = json.loads(GLOSSARY.read_text(encoding="utf-8"))
        self.assertEqual(data["locale"], "pt-BR")
        self.translations = data["translations"]

    def test_regression_labels_are_translated(self):
        for key in REGRESSION_KEYS + VGP_SPEED_KEYS:
            with self.subTest(key=key):
                self.assertIn(key, self.translations)
                self.assertNotEqual(self.translations[key], key)

    def test_translations_are_not_accidentally_english(self):
        for key in REGRESSION_KEYS:
            value = self.translations[key]
            self.assertTrue(value.strip(), key)
            if len(key) > 12:
                # A long sentence must actually change in Portuguese.
                self.assertNotEqual(value, key)

    def test_dictionary_and_both_uis_stay_in_sync(self):
        expected = len(self.translations)
        for path in JAVA:
            text = path.read_text(encoding="utf-8")
            self.assertEqual(
                text.count("m.put("),
                expected,
                "%s is out of sync with pt-BR.json" % path.name,
            )

    def test_new_keys_exist_in_both_ui_files(self):
        for key in REGRESSION_KEYS + VGP_SPEED_KEYS:
            line = 'm.put("%s"' % _escape(key)
            for path in JAVA:
                self.assertIn(line, path.read_text(encoding="utf-8"), path.name)

    def test_no_empty_translation(self):
        for key, value in self.translations.items():
            self.assertTrue(key.strip(), "empty key")
            self.assertTrue(value.strip(), "empty value for %r" % key)

    def test_keys_are_not_whitespace_or_symbol_only(self):
        for key in self.translations:
            self.assertTrue(re.search(r"[A-Za-zÀ-ÿ]", key), repr(key))


if __name__ == "__main__":
    unittest.main()
