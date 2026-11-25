"""PT-BR UI strings that were still English at runtime (P1, lote 2).

`PtBr.tr` returns its argument when a key is missing, so a translated call site is not
proof of a translated interface. This suite pins the strings that were still shown in
English on device, and pins the selection-cap noun translation that used to print the
raw English noun ("species", "bosses") inside a Portuguese sentence.

No source file is written here.
"""
import json
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SRC = ROOT / "pgrazor" / "src" / "dev" / "pgrazor"
GLOSSARY = ROOT / "pgrazor" / "i18n" / "pt-BR.json"

# file -> sentences that must NOT appear in English anymore
ENGLISH_GONE = {
    "LogActivity.java": ("Save failed: ",),
    "PreviewActivity.java": ("Export failed: ",),
    "RootedLogDialog.java": ("Saved to ",),
    "QuickLocationDialogs.java": ("Walking to ", '"Saved " + entry.name'),
    "GymFeedOverlay.java": ("Gym Slotter and Berry Feeder", "tap to stop", "tap to start"),
    "ShundoSniper.java": ("Shundo Sniper is running", "taps are blocked"),
    "GameRoutesOverlay.java": ("In-game Routes: ", "Tap for current path"),
    "PokemonSpeciesDialog.java": ("Choose at most ", "That would exceed ", "narrow the list first"),
}

# Portuguese text that must be present, so removing the English is not "fixed" by deleting UI
PORTUGUESE_PRESENT = {
    "LogActivity.java": ("Falha ao salvar: ",),
    "PreviewActivity.java": ("Falha ao exportar: ",),
    "RootedLogDialog.java": ("Salvo em ",),
    "QuickLocationDialogs.java": ("Caminhando até ", "Salvo em Favoritos: "),
    "GymFeedOverlay.java": ("Ginásio e alimentador de frutas",),
    "ShundoSniper.java": ("Shundo Sniper ativo",),
    "GameRoutesOverlay.java": ("Rotas do jogo: ",),
    "PokemonSpeciesDialog.java": ("Escolha no máximo ", "Isso ultrapassa ", "reduza a lista antes"),
}


class PtbrUiStringsTest(unittest.TestCase):
    def test_english_sentences_are_gone(self):
        for name, sentences in ENGLISH_GONE.items():
            text = (SRC / name).read_text(encoding="utf-8")
            for sentence in sentences:
                with self.subTest(file=name, sentence=sentence):
                    self.assertNotIn(sentence, text)

    def test_portuguese_sentences_are_present(self):
        for name, sentences in PORTUGUESE_PRESENT.items():
            text = (SRC / name).read_text(encoding="utf-8")
            for sentence in sentences:
                with self.subTest(file=name, sentence=sentence):
                    self.assertIn(sentence, text)

    def test_selection_cap_uses_translated_noun(self):
        text = (SRC / "PokemonSpeciesDialog.java").read_text(encoding="utf-8")
        self.assertIn("nounPt(noun)", text)
        # The noun is only translated, never invented: unknown labels pass through.
        self.assertIn('case "species": return "espécies";', text)
        self.assertIn('case "bosses": return "chefes de Reide";', text)
        self.assertIn("default: return noun;", text)

    def test_empty_selection_label_is_translated(self):
        data = json.loads(GLOSSARY.read_text(encoding="utf-8"))
        key = "All species (no selection)"
        self.assertIn(key, data["translations"])
        self.assertNotEqual(data["translations"][key], key)
        for path in (SRC / "PtBr.java",
                     ROOT / "pgrazor" / "root-installer" / "src" / "dev" / "pgrazor" / "root" / "RootPtBr.java"):
            self.assertIn('m.put("%s"' % key, path.read_text(encoding="utf-8"), path.name)

    def test_privacy_and_internal_ids_untouched(self):
        """A cosmetic fix must not change stored keys, settings names or backup fields."""
        settings = (ROOT / "pgrazor" / "src" / "dev" / "pgrazor" / "SettingsBackupRules.java").read_text(encoding="utf-8")
        for key in ("trainerPrivacyMode", "vgpOptions", "smartAttack", "battleOptions"):
            self.assertIn(key, settings)
        # The new dictionary key is a UI sentence, never a settings/backup identifier.
        self.assertNotIn("All species", settings)
        # No touched file may gain a translation around the trainer name service.
        for name in ENGLISH_GONE:
            self.assertNotIn("get_PlayerName(", (SRC / name).read_text(encoding="utf-8"))


if __name__ == "__main__":
    unittest.main()
