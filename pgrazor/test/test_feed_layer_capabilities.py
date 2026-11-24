"""A UI só pode prometer as camadas que o parser daquela fonte realmente produz.

Regressão: as abas Ginásios/Paradas ficavam eternamente vazias em fontes que não
publicam essas camadas, sem nenhuma explicação. Agora `LiveFeedDialog.sourcePublishes`
diz o que a fonte publica e a lista vazia mostra o motivo — e este teste garante que
essa declaração continua igual ao que cada parser entrega de fato.
"""
import re
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "src/dev/pgrazor"

# provider -> classe que realmente monta as entradas
PARSERS = {
    0: "LiveFeedClient",
    1: "PokeListFeedData",
    2: "IFlowGoFeedData",
    3: "RocketMapFeedData",
    4: "DemoLiveFeedData",
}


def kinds_of(java_class):
    text = (SRC / f"{java_class}.java").read_text(encoding="utf-8")
    return set(re.findall(r"LiveFeedData\.Kind\.([A-Z]+)", text))


def declared_layers(dialog_text):
    """Lê o switch de sourcePublishes e devolve {provider: {KIND}}."""
    start = dialog_text.index("private boolean sourcePublishes(")
    body = dialog_text[start:dialog_text.index("private LiveFeedData.Kind requestedLayer")]
    declared = {}
    for match in re.finditer(r"case (\d+):\s*(.*?);\s*(?=case \d+:|default:)", body, re.S):
        provider = int(match.group(1))
        declared[provider] = set(re.findall(r"Kind\.([A-Z]+)", match.group(2)))
    default = re.search(r"default:\s*(.*?);", body, re.S)
    declared["default"] = set(re.findall(r"Kind\.([A-Z]+)", default.group(1)))
    # O switch só lista as fontes que diferem do padrão; 0 usa o default.
    declared.setdefault(0, declared["default"])
    return declared


class FeedLayerCapabilityTests(unittest.TestCase):
    def setUp(self):
        self.dialog = (SRC / "LiveFeedDialog.java").read_text(encoding="utf-8")
        self.declared = declared_layers(self.dialog)

    def test_declaration_matches_every_parser(self):
        for provider, java_class in PARSERS.items():
            produced = kinds_of(java_class)
            claimed = self.declared[provider]
            self.assertEqual(
                claimed, produced,
                f"provider {provider} ({java_class}): a UI declara {sorted(claimed)} "
                f"mas o parser produz {sorted(produced)}")

    def test_default_case_matches_online_feed(self):
        self.assertEqual(self.declared["default"], kinds_of("LiveFeedClient"))

    def test_sources_without_gym_or_stop_layer_are_known(self):
        for java_class in ("IFlowGoFeedData", "PokeListFeedData"):
            produced = kinds_of(java_class)
            self.assertNotIn("GYM", produced)
            self.assertNotIn("STOP", produced)
        self.assertIn("GYM", kinds_of("RocketMapFeedData"))
        self.assertIn("STOP", kinds_of("RocketMapFeedData"))

    def test_empty_list_states_the_cause(self):
        # Nunca um único texto genérico: a razão específica precisa existir.
        self.assertIn("private String emptyReason()", self.dialog)
        self.assertIn("não publica", self.dialog)
        self.assertIn("Esta aba fica vazia por limitação da fonte", self.dialog)
        self.assertIn("A fonte respondeu com 0 aparições", self.dialog)
        self.assertIn("nenhum passou nos filtros", self.dialog)
        self.assertIn("emptyView.setText", self.dialog)

    def test_each_drop_reason_is_counted_separately(self):
        """Um único "Nenhum Pokémon encontrado" é proibido: cada causa tem seu contador."""
        for counter in ("droppedExpired", "droppedCategory", "droppedSpecies",
                        "droppedFilters", "droppedRadius", "droppedCoordinates", "droppedRaid"):
            self.assertIn(f"{counter}++", self.dialog, f"{counter} precisa ser contado")
            self.assertIn(f"{counter} > 0", self.dialog, f"{counter} precisa ser exibido")
        self.assertIn("private String droppedSummary()", self.dialog)
        self.assertIn("Descartados: ", self.dialog)
        # As contagens são zeradas a cada montagem, senão acumulariam entre atualizações.
        self.assertIn("droppedExpired = droppedCategory = droppedSpecies = droppedFilters = 0;",
                      self.dialog)

    def test_every_category_tab_maps_to_a_layer(self):
        # Toda aba precisa de um Kind; "All" é o único null.
        body = self.dialog[self.dialog.index("private LiveFeedData.Kind requestedLayer"):]
        block = body[:body.index("private String emptyReason")]
        self.assertEqual(len(re.findall(r"case \d+:", block)), 5)
        self.assertIn("default: return null;", block)


if __name__ == "__main__":
    unittest.main()
