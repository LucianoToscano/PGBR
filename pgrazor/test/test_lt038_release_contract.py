"""LT038 QA packaging contract: confirmed scope versus unsupported universal attack."""
from pathlib import Path
import unittest
ROOT=Path(__file__).resolve().parents[2]
class Lt038ReleaseContract(unittest.TestCase):
    def test_source_contract(self):
        self.assertEqual((ROOT/"pgrazor/VERSION").read_text(encoding="utf-8").strip(),"0.00.38-LT")
        w=(ROOT/".github/workflows/pgbr-lt038-release.yml").read_text(encoding="utf-8")
        for item in ("PGRAZOR_VERSION: v0.00.38-LT","--tag v0.00.38-LT",
                     "PGBR-v0.00.38-LT.apk","PGBR-v0.00.38-LT-Root.apk",
                     "PGBR-v0.00.38-LT-Root-module.zip","SHA256SUMS.txt",
                     "test_pvp_settings_ui.py","test_manual_battle_choice_grace.py",
                     "tools/check.py","pgrazor/nonroot.py","pgrazor/rooted.py",
                     "tools/verify_release.py","--prerelease"):
            with self.subTest(item=item): self.assertIn(item,w)
        self.assertIn("workflow_dispatch:", w)
        self.assertNotIn("  push:", w)  # no rebuild on later version/doc commits
        notes=(ROOT/"docs/releases/v0.00.38-LT.md").read_text(encoding="utf-8")
        self.assertIn("device_tested=false",notes)
        self.assertIn("ainda guarda somente a preferência",notes)
        self.assertIn("2,3 segundos",notes)
        self.assertIn("1,7 segundo",notes)
        self.assertIn("Ataques carregados permanecem imediatos",notes)
        self.assertIn("LT038",(ROOT/"PGBR_ARQUIVO_MESTRE_UNICO.md").read_text(encoding="utf-8"))
if __name__=="__main__":unittest.main()
