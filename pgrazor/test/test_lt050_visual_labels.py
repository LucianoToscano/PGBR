"""LT050: UI terminology and known item labels, without changing IDs or assets."""
import json
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[2]

class VisualLabels(unittest.TestCase):
    def test_teleporte_consistency(self):
        expected = {
    "Teleported to the selected result.": "Teleporte realizado para o resultado selecionado.",
    "Invalid teleport coordinates": "Coordenadas inválidas para Teleporte",
    "Time between teleports": "Intervalo entre teleportes",
    "Time between teleports: ": "Intervalo entre teleportes: "
}
        glossary = json.loads((ROOT / "pgrazor/i18n/pt-BR.json").read_text(encoding="utf-8"))["translations"]
        for key, value in expected.items():
            self.assertEqual(glossary[key], value)
            row = "m.put(" + json.dumps(key, ensure_ascii=False) + ", " + json.dumps(value, ensure_ascii=False) + ");"
            for path in ("pgrazor/src/dev/pgrazor/PtBr.java", "pgrazor/root-installer/src/dev/pgrazor/root/RootPtBr.java"):
                self.assertIn(row, (ROOT / path).read_text(encoding="utf-8"))

    def test_gallery_names_known_berries_and_unknown_fallback(self):
        source = (ROOT / "pgrazor/src/dev/pgrazor/LocalAuditDialog.java").read_text(encoding="utf-8")
        for id_, name in ((701,"Fruta Frambo"), (703,"Fruta Anaba"), (705,"Fruta Caxi"), (706,"Fruta Frambo Dourada"), (708,"Fruta Caxi Prateada")):
            self.assertIn(f'case {id_}:return "{name}";', source)
        self.assertIn('default:return "Item de catálogo (nome não confirmado)";', source)
        self.assertNotIn("Fruta Caxí Prateada", source)
