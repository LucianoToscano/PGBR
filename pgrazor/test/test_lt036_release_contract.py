"""LT036 additive QA release contract: do not overwrite historical versions."""
from pathlib import Path
import unittest
ROOT=Path(__file__).resolve().parents[2]
class Lt036ReleaseContract(unittest.TestCase):
    def test_release_version_and_assets(self):
        self.assertEqual((ROOT/"pgrazor/VERSION").read_text().strip(),"0.00.36-LT")
        current=(ROOT/".github/workflows/pgbr-lt036-release.yml").read_text()
        for name in ("PGRAZOR_VERSION: v0.00.36-LT","--tag v0.00.36-LT",
                     "PGBR-v0.00.36-LT.apk","PGBR-v0.00.36-LT-Root.apk",
                     "PGBR-v0.00.36-LT-Root-module.zip","SHA256SUMS.txt",
                     "tools/check.py","pgrazor/nonroot.py","pgrazor/rooted.py",
                     "tools/verify_release.py","github.ref == 'refs/heads/main'",
                     "--prerelease","pgrazor/native/pvp_scene_test.cpp"):
            with self.subTest(name=name):self.assertIn(name,current)
        for num in (33,34,35):
            historic=(ROOT/f".github/workflows/pgbr-lt0{num}-release.yml").read_text()
            self.assertIn(f"PGRAZOR_VERSION: v0.00.{num}-LT",historic)
            self.assertNotIn("'pgrazor/VERSION'",historic)
        notes=(ROOT/"docs/releases/v0.00.36-LT.md").read_text()
        self.assertIn("device_tested=false",notes)
        self.assertIn("App não instalado",notes)
        self.assertIn("LT036",(ROOT/"PGBR_ARQUIVO_MESTRE_UNICO.md").read_text())
if __name__=="__main__": unittest.main()
