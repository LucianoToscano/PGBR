"""LT039 QA release contract: publish complete new artifacts without overwriting LT038."""
from pathlib import Path
import unittest

ROOT=Path(__file__).resolve().parents[2]
class Lt039ReleaseContract(unittest.TestCase):
    def test_version_and_release_workflow(self):
        self.assertIn((ROOT/"pgrazor/VERSION").read_text(encoding="utf-8").strip(),("0.00.39-LT","0.00.40-LT","0.00.41-LT","0.00.42-LT","0.00.43-LT","0.00.44-LT","0.00.45-LT","0.00.46-LT","0.00.47-LT","0.00.48-LT"))
        w=(ROOT/".github/workflows/pgbr-lt039-release.yml").read_text(encoding="utf-8")
        for text in ("PGRAZOR_VERSION: v0.00.39-LT","--tag v0.00.39-LT",
            "PGBR-v0.00.39-LT.apk","PGBR-v0.00.39-LT-Root.apk",
            "PGBR-v0.00.39-LT-Root-module.zip","SHA256SUMS.txt",
            "test_pvp_settings_ui.py","test_manual_battle_choice_grace.py",
            "test_battle_timing_controls.py","battle_timing_policy_test.cpp",
            "tools/check.py","pgrazor/nonroot.py","pgrazor/rooted.py",
            "tools/verify_release.py","--prerelease","--latest=false",
            "gh release view","gh release create","workflow_dispatch:",
            "workflow_dispatch:"):
            with self.subTest(text=text): self.assertIn(text,w)
        self.assertNotIn("  push:",w)  # immutable published LT039
        self.assertIn("github.ref == 'refs/heads/main'",w)
        self.assertNotIn("PGBR-v0.00.38-LT.apk",w)
        for previous in ("pgbr-lt038-release.yml","pgbr-lt037-release.yml"):
            old=(ROOT/".github/workflows"/previous).read_text(encoding="utf-8")
            self.assertNotIn("  push:",old)

    def test_notes_readme_master_and_source_cadence(self):
        notes=(ROOT/"docs/releases/v0.00.39-LT.md").read_text(encoding="utf-8")
        for term in ("device_tested=false","1,5 segundo (padrão)","2 segundos (padrão)",
                     "250 ms","Não implementado" if False else "Ainda não implementado",
                     "LT038"):
            self.assertIn(term,notes)
        readme=(ROOT/"README.md").read_text(encoding="utf-8")
        self.assertIn("PGBR-v0.00.39-LT.apk",readme)
        self.assertIn("LT038",readme)
        self.assertIn("LT039",(ROOT/"PGBR_ARQUIVO_MESTRE_UNICO.md").read_text(encoding="utf-8"))
        actions=(ROOT/"pgrazor/native/rocket_actions.cpp").read_text(encoding="utf-8")
        self.assertIn("action==PGR_ROCKET_QUICK?150",actions)
        self.assertIn("action==PGR_ROCKET_LEGACY_CHARGE?500",actions)
        self.assertIn("action==PGR_ROCKET_VNEXT_CHARGE?600:250",actions)
if __name__=="__main__":unittest.main()
