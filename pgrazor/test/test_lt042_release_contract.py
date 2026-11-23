"""LT042 QA release candidate: preserved immutable LT040, no prior tag overwritten."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[2]

class Lt042ReleaseContract(unittest.TestCase):
    def test_candidate_version_and_immutable_prior_release(self):
        self.assertEqual((ROOT / "pgrazor/VERSION").read_text(encoding="utf-8").strip(), "0.00.42-LT")
        release = (ROOT / ".github/workflows/pgbr-lt042-release.yml").read_text(encoding="utf-8")
        for text in ("PGRAZOR_VERSION: v0.00.42-LT", "--tag v0.00.42-LT",
                     "PGBR-v0.00.42-LT.apk", "PGBR-v0.00.42-LT-Root.apk",
                     "PGBR-v0.00.42-LT-Root-module.zip",
                     "SHA256SUMS.txt", "tools/verify_release.py",
                     "pgrazor/nonroot.py", "pgrazor/rooted.py", "native-pvp-fixture:",
                     "test_pvp_settings_ui.py", "test_iflowgo_request_policy_java.py",
                     "workflow_dispatch:", "branches: [main]",
                     "gh release view", "gh release create", "--prerelease",
                     "--latest=false"):
            with self.subTest(text=text):
                self.assertIn(text, release)
        self.assertIn("'.github/workflows/pgbr-lt042-release.yml'", release)
        self.assertIn("charge_effect_ui_test.cpp", release)
        self.assertIn("smart_attack_priority_test.cpp", release)
        self.assertNotIn("PGBR-v0.00.41-LT.apk", release)
        old = (ROOT / ".github/workflows/pgbr-lt041-release.yml").read_text(encoding="utf-8")
        self.assertIn("workflow_dispatch:", old)
        self.assertNotIn("  push:", old)
        self.assertIn("PGRAZOR_VERSION: v0.00.41-LT", old)

    def test_notes_and_master(self):
        notes = (ROOT / "docs/releases/v0.00.42-LT.md").read_text(encoding="utf-8")
        for text in ("device_tested=false", "iFlowGo", "Reides", "Ataque Inteligente",
                     "issue #90", "issue #105"):
            self.assertIn(text, notes)
        master = (ROOT / "PGBR_ARQUIVO_MESTRE_UNICO.md").read_text(encoding="utf-8")
        self.assertIn("Auditoria de regressões pós-LT040", master)
        self.assertIn("PR #124", master)

if __name__ == "__main__":
    unittest.main()
