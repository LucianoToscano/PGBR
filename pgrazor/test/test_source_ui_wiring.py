"""Integration contracts: the local engines must be reachable from the interface.

The engines themselves (AlertEngine, PgSharpFilterMapping, SniperSearch, PresetCodec) are verified on
the JVM by PresetCodecTest, PgSharpFilterMappingTest and SniperSearchTest. These checks guarantee the
five-source screen really calls them, so a tested class never ships as unreachable code, and that the
local alerts / preset conversion stay honest about what they did not do.
"""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "src" / "dev" / "pgrazor"


class SourceUiWiringTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.radar = (SRC / "SourceRadarDialog.java").read_text(encoding="utf-8")
        cls.importer = (SRC / "LocalImportDialog.java").read_text(encoding="utf-8")
        cls.sniper = (SRC / "SniperDialog.java").read_text(encoding="utf-8")
        cls.store = (SRC / "SourceDataStore.java").read_text(encoding="utf-8")

    # --- local alerts come from real rows only -------------------------------------------
    def test_radar_evaluates_presets_against_stored_and_received_rows(self):
        self.assertIn("AlertEngine.evaluate(presets, real,", self.radar)
        self.assertIn("SourceDataStore.presets(activity)", self.radar)
        self.assertIn("for (SourceEvent event : store.all())", self.radar)
        self.assertIn("evaluateAlerts(completed.items);", self.radar)

    def test_alert_pane_names_the_preset_the_row_and_the_origin(self):
        self.assertIn('"Alertas locais ("', self.radar)
        self.assertIn(".append(alert.presetName)", self.radar)
        self.assertIn(".append(label(alert.event))", self.radar)
        self.assertIn(".append(alert.reason)", self.radar)

    def test_alert_pane_reports_absence_instead_of_inventing_an_alert(self):
        self.assertIn("nenhum casa os critérios", self.radar)
        self.assertIn("nenhum preset salvo", self.radar)
        self.assertNotIn('"Alerta: shiny avistado', self.radar)

    # --- a PGSharp export becomes local presets, never a live feed -----------------------
    def test_import_maps_a_pgsharp_filters_document_into_presets(self):
        self.assertIn("PgSharpFilterMapping.map(document)", self.importer)
        self.assertIn("SourceDataStore.upsertPreset(activity, preset)", self.importer)
        self.assertIn('document.has("pokemonFilters")', self.importer)
        self.assertIn('document.has("questFilters")', self.importer)
        self.assertIn('document.has("raidFilters")', self.importer)

    def test_pgsharp_conversion_is_only_taken_for_a_filters_document(self):
        self.assertIn('if (!trimmed.startsWith("{")) return null;', self.importer)
        self.assertIn("return null;", self.importer)
        # Rows, coordinates and GPX keep going through the events importer.
        self.assertIn("LocalEventImport.parse(text,", self.importer)
        self.assertIn("LocalEventImport.IMPORT_SOURCE", self.importer)

    def test_pgsharp_conversion_states_that_no_teleport_happened(self):
        self.assertIn("Nenhum teleporte foi feito.", self.importer)
        self.assertIn("Filtros do PGSharp convertidos", self.importer)

    def test_import_report_keeps_rejected_rows_and_their_reason(self):
        self.assertIn("result.summary()", self.importer)
        self.assertIn("Registros guardados:", self.importer)

    # --- the entry points are reachable ---------------------------------------------------
    def test_menu_exposes_radar_import_and_advanced_sniper(self):
        menu = (SRC / "FeedChooser.java").read_text(encoding="utf-8")
        for entry in ("SourceRadarDialog.show(activity)", "LocalImportDialog.show(activity",
                      "SniperDialog.showAdvanced(activity)"):
            with self.subTest(entry=entry):
                self.assertIn(entry, menu)

    def test_presets_are_persisted_for_radar_and_sniper(self):
        self.assertIn("upsertPreset", self.store)
        self.assertIn("deletePreset", self.store)
        self.assertIn("presets", self.sniper)


if __name__ == "__main__":
    unittest.main()
