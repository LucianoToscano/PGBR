"""LT037 immutable additive QA release: history and device-evidence limitations."""
from pathlib import Path
import unittest
ROOT=Path(__file__).resolve().parents[2]
class Lt037ReleaseContract(unittest.TestCase):
    def test_release_version_and_download_names(self):
        self.assertIn((ROOT/"pgrazor/VERSION").read_text(encoding="utf-8").strip(),("0.00.37-LT","0.00.38-LT","0.00.39-LT","0.00.40-LT","0.00.41-LT","0.00.42-LT","0.00.43-LT","0.00.44-LT","0.00.45-LT","0.00.46-LT","0.00.47-LT"))
        w=(ROOT/".github/workflows/pgbr-lt037-release.yml").read_text(encoding="utf-8")
        for item in ("PGRAZOR_VERSION: v0.00.37-LT","--tag v0.00.37-LT",
                     "PGBR-v0.00.37-LT.apk","PGBR-v0.00.37-LT-Root.apk",
                     "PGBR-v0.00.37-LT-Root-module.zip","SHA256SUMS.txt",
                     "battle_target_policy_test.cpp","pvp_scene_test.cpp",
                     "tools/check.py","pgrazor/nonroot.py","pgrazor/rooted.py",
                     "tools/verify_release.py","github.ref == 'refs/heads/main'","--prerelease"):
            with self.subTest(item=item):self.assertIn(item,w)
        self.assertIn("workflow_dispatch:",w)
        self.assertNotIn("  push:",w)  # immutable published LT037 cannot restart on docs
        for n in (33,34,35,36):
            hist=(ROOT/f".github/workflows/pgbr-lt0{n}-release.yml").read_text(encoding="utf-8")
            self.assertIn(f"PGRAZOR_VERSION: v0.00.{n}-LT",hist)
            self.assertNotIn("'pgrazor/VERSION'",hist)
        notes=(ROOT/"docs/releases/v0.00.37-LT.md").read_text(encoding="utf-8")
        self.assertIn("device_tested=false",notes)
        self.assertIn("não",notes)
        self.assertIn("LT037",(ROOT/"PGBR_ARQUIVO_MESTRE_UNICO.md").read_text(encoding="utf-8"))
if __name__=="__main__":unittest.main()
