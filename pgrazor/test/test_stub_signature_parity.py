"""Impede que um stub do harness invente assinatura e esconda um erro de compilação real.

Motivo: o stub de WorldMapDialog declarava `Callback { void apply(...) }`, contrato que NÃO
existe no código real. Com ele o painel "compilava" no harness e quebrava no build Android
de verdade com "Cannot return a void result" em DestinationListener. Um stub que inventa
assinatura não prova nada — o contrato tem de ser o do arquivo real.

Este teste lê, dos dois lados, os tipos que atravessam a fronteira (interfaces públicas e
métodos estáticos públicos) e exige que todo tipo de callback declarado no stub exista com
o mesmo nome no arquivo real, e que todo método público usado pelo app exista no stub.
"""
import re
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
REPO = ROOT.parent
STUBS = REPO / "tools/sniper_jvm/stubs/dev/pgrazor"
REAL = ROOT / "src/dev/pgrazor"

# Pares (stub, real) cuja fronteira é verificada. Stubs sem classe real correspondente
# (porque a real depende de Android pesado) ficam de fora e são listados explicitamente.
PAIRS = [
    "WorldMapDialog",
    "TeleportDialog",
    "DialogLifecycle",
    "Control",
    "MapTheme",
    "CoordinateClipboard",
    "AccountProfiles",
    "LiveFeedIcons",
]

# Interfaces de callback que o app usa e que precisam existir nos dois lados.
CALLBACK_INTERFACES = ["DestinationListener", "RouteListener", "Action"]


def interface_names(text):
    """Só declarações de interface, nunca "interface" dentro de nome de campo/método."""
    return set(re.findall(r"(?:^|[;{}\s])interface\s+([A-Z]\w*)\s*\{", text, re.M))


def public_static_methods(text):
    return set(re.findall(r"public\s+static\s+[\w<>\[\],. ?]+\s+(\w+)\s*\(", text))


class StubSignatureParityTests(unittest.TestCase):
    def test_every_paired_stub_compiles_against_the_real_contract(self):
        for name in PAIRS:
            with self.subTest(stub=name):
                stub_file, real_file = STUBS / f"{name}.java", REAL / f"{name}.java"
                self.assertTrue(stub_file.is_file(), f"stub ausente: {stub_file}")
                self.assertTrue(real_file.is_file(), f"classe real ausente: {real_file}")
                stub = stub_file.read_text(encoding="utf-8")
                real = real_file.read_text(encoding="utf-8")
                # Todo callback declarado no real precisa existir no stub com o mesmo nome.
                missing = interface_names(real) - interface_names(stub)
                self.assertFalse(
                    missing,
                    f"{name}: o stub não declara {sorted(missing)}, que existem no código real. "
                    "Um stub incompleto compila aqui e falha no build Android.")
                # E o inverso: o stub não pode inventar um callback que o real não tem.
                invented = interface_names(stub) - interface_names(real)
                self.assertFalse(
                    invented,
                    f"{name}: o stub inventa {sorted(invented)}, que não existem no código real. "
                    "Foi assim que 'Cannot return a void result' passou despercebido.")

    def test_map_callbacks_return_boolean(self):
        real = (REAL / "WorldMapDialog.java").read_text(encoding="utf-8")
        stub = (STUBS / "WorldMapDialog.java").read_text(encoding="utf-8")
        for text, who in ((real, "real"), (stub, "stub")):
            self.assertRegex(
                text, r"interface\s+DestinationListener\s*\{\s*(?:/\*.*?\*/\s*)?boolean\s+onDestination",
                f"{who}: DestinationListener precisa devolver boolean")
        # O painel precisa usar o retorno do jeito certo: nunca devolver void no lambda.
        panel = (REAL / "SniperPanel.java").read_text(encoding="utf-8")
        self.assertNotIn("(lat, lon) -> { }", panel,
                         "lambda void em DestinationListener não compila no Android")
        call = re.search(r"WorldMapDialog\.show\([^;]*?;", panel, re.S)
        self.assertIsNotNone(call, "o painel precisa abrir o mapa")
        self.assertIn("requestTeleportFromMap", call.group(0),
                      "o mapa deve passar pelo mesmo pedido de teleporte validado")
        self.assertIn("-> false", call.group(0),
                      "ação que não foi executada (só solicitada) não pode reportar sucesso")

    def test_panel_and_dialog_use_only_stubbed_public_api(self):
        """Toda chamada estática X.y(...) no painel para uma classe-stub precisa existir no stub."""
        panel = (REAL / "SniperPanel.java").read_text(encoding="utf-8")
        called = set(re.findall(r"\b(Control|MapTheme|DialogLifecycle|TeleportDialog|"
                                r"WorldMapDialog|CoordinateClipboard|AccountProfiles|"
                                r"LiveFeedIcons)\.(\w+)\s*\(", panel))
        for klass, method in sorted(called):
            stub_file = STUBS / f"{klass}.java"
            if not stub_file.is_file():
                continue
            stub = stub_file.read_text(encoding="utf-8")
            self.assertIn(method, stub,
                          f"{klass}.{method} é usado pelo painel e não existe no stub: "
                          "o painel não compilaria no harness.")

    def test_callback_interface_semantics_are_documented(self):
        real = (REAL / "WorldMapDialog.java").read_text(encoding="utf-8")
        # O contrato precisa estar escrito: "true depois de aplicar, false para manter aberto".
        self.assertRegex(real, r"Return true after applying",
                         "o contrato de retorno precisa continuar documentado no código real")


if __name__ == "__main__":
    unittest.main()
