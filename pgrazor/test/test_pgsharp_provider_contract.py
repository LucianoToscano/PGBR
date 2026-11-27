"""PGSharp provider integration guards: no guessed remote API and no credential leakage."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "src" / "dev" / "pgrazor"


class PgSharpProviderContractTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.menu = (SRC / "FeedChooser.java").read_text(encoding="utf-8")
        cls.ui = (SRC / "PgSharpFeedDialog.java").read_text(encoding="utf-8")
        cls.vault = (SRC / "PgSharpLicenseVault.java").read_text(encoding="utf-8")
        cls.policy = (SRC / "PgSharpFeedPolicy.java").read_text(encoding="utf-8")

    def test_preserves_existing_five_options(self):
        for snippet in ("LiveFeedDialog.showPokemonSearch(activity)",
                        "LiveFeedDialog.showRaidSearch(activity)",
                        "LiveFeedDialog.show(activity)",
                        "PogoFeedDialog.show(activity)",
                        "HundoFeedDialog.show(activity)"):
            self.assertIn(snippet, self.menu)
        self.assertIn("which == 5", self.menu)
        self.assertIn("PgSharpFeedDialog.show(activity)", self.menu)

    def test_free_and_paid_capabilities_remain_separate(self):
        self.assertIn("FREE_100_IV", self.policy)
        self.assertIn("STANDARD_FILTERS", self.policy)
        self.assertIn("Connector.UNVERIFIED", self.policy)
        self.assertIn("VERIFIED_BY_PROVIDER", self.policy)
        self.assertIn("KeyStatus.STORED_UNVERIFIED", self.policy)
        self.assertIn("key == KeyStatus.VERIFIED_BY_PROVIDER", self.policy)

    def test_no_pgsharp_api_request_or_login_claim(self):
        self.assertNotIn("openConnection(", self.ui)
        self.assertNotIn("HttpURLConnection", self.ui)
        self.assertNotIn("post.auth.lic", self.ui)
        self.assertNotIn("PL.NRL", self.ui)
        self.assertIn("ainda não tem resposta/assinatura de requisição verificadas", self.ui)
        self.assertIn("HundoFeedDialog.show(activity)", self.ui)
        self.assertIn("Não é transmitida", self.ui)

    def test_dedicated_snipers_use_only_imported_pgsharp_rows(self):
        sniper = (SRC / "SniperDialog.java").read_text(encoding="utf-8")
        self.assertIn("SniperDialog.showPgSharpQuick(activity)", self.ui)
        self.assertIn("SniperDialog.showPgSharpAdvanced(activity)", self.ui)
        self.assertIn("show(activity, false, true)", sniper)
        self.assertIn("show(activity, true, true)", sniper)
        self.assertIn("source.setEnabled(false)", sniper)
        self.assertIn("singletonList(new LocalSourceAdapters.PGSharp(store))", sniper)
        self.assertIn("final LocalEventStore local = pgSharpOnly ? null : store;", sniper)
        self.assertIn("sniper1(query, selected, local,", sniper)
        self.assertIn("sniper2(query, selected, local,", sniper)
        self.assertIn("Não é o Sniper original nem consulta ao serviço PGSharp.", sniper)

    def test_key_is_local_encrypted_and_not_an_account_session(self):
        for snippet in ("getNoBackupFilesDir()", "AndroidKeyStore", "AES/GCM/NoPadding",
                        "ATOMIC_MOVE", "GCMParameterSpec", "cipher.updateAAD(HEADER)",
                        "Arrays.fill(clear", "Arrays.fill(contents",
                        "StandardOpenOption.WRITE", "Files.deleteIfExists"):
            with self.subTest(snippet=snippet):
                self.assertIn(snippet, self.vault)
        self.assertNotIn("AccountSessionVault", self.ui + self.vault)
        self.assertNotIn("SharedPreferences", self.vault)
        self.assertNotIn("Log.", self.ui + self.vault)
        self.assertIn("TYPE_TEXT_VARIATION_PASSWORD", self.ui)
        self.assertIn("FLAG_SECURE", self.ui)
        self.assertIn("keyInput.setText(\"\")", self.ui)
        self.assertIn("Arrays.fill(chars", self.ui)

    def test_local_import_shortcut(self):
        self.assertIn("Importar filtros e resultados", self.ui)
        self.assertIn("LocalImportDialog.show(activity, null)", self.ui)
        self.assertIn("São dados locais, não consultas ao serviço PGSharp.", self.ui)

    def test_does_not_enable_premium_from_stored_key(self):
        self.assertIn("STORED_UNVERIFIED", self.ui)
        self.assertNotIn("VERIFIED_BY_PROVIDER", self.ui)
        self.assertNotIn("Control.teleport", self.ui)
        self.assertNotIn("ShundoSniper.start", self.ui)
        self.assertIn("DialogLifecycle.bind(activity, dialog, panel::close)", self.ui)


if __name__ == "__main__":
    unittest.main()
