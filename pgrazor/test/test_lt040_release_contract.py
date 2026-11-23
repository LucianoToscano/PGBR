"""LT040 QA packaging: new immutable release, no rewrite of LT037/LT038/LT039."""
from pathlib import Path
import unittest

ROOT=Path(__file__).resolve().parents[2]

class Lt040ReleaseContract(unittest.TestCase):
    def test_version_workflow_and_assets(self):
        version=(ROOT/"pgrazor/VERSION").read_text(encoding="utf-8").strip()
        self.assertIn(version,("0.00.40-LT","0.00.41-LT","0.00.42-LT"))
        new=(ROOT/".github/workflows/pgbr-lt040-release.yml").read_text(encoding="utf-8")
        for term in (
            "PGRAZOR_VERSION: v0.00.40-LT","--tag v0.00.40-LT",
            "PGBR-v0.00.40-LT.apk","PGBR-v0.00.40-LT-Root.apk",
            "PGBR-v0.00.40-LT-Root-module.zip",
            "SHA256SUMS.txt","tools/verify_release.py",
            "pgrazor/nonroot.py","pgrazor/rooted.py",
            "native-pvp-fixture:","pvp_scene_test.cpp",
            "battle_timing_policy_test.cpp",
            "test_pvp_settings_ui.py","test_manual_battle_choice_grace.py",
            "test_battle_timing_controls.py",
            "workflow_dispatch:","--prerelease","--latest=false",
            "gh release view","gh release create"):
            with self.subTest(term=term):self.assertIn(term,new)
        self.assertNotIn("  push:",new)  # published LT040 is immutable
        self.assertIn("github.ref == 'refs/heads/main'",new)
        self.assertNotIn("PGBR-v0.00.39-LT.apk",new)
        for old in ("pgbr-lt037-release.yml","pgbr-lt038-release.yml","pgbr-lt039-release.yml"):
            historic=(ROOT/".github/workflows"/old).read_text(encoding="utf-8")
            self.assertNotIn("  push:",historic)
            self.assertIn("workflow_dispatch:",historic)

    def test_release_notes_and_preservation(self):
        notes=(ROOT/"docs/releases/v0.00.40-LT.md").read_text(encoding="utf-8")
        for term in ("device_tested=false","PGBR","LT037","LT039",
                     "0/1500/3000","400/2000/6000",
                     "não","Super eficaz"):
            with self.subTest(term=term):self.assertIn(term,notes)
        readme=(ROOT/"README.md").read_text(encoding="utf-8")
        self.assertIn("v0.00.40-LT.md",readme)
        self.assertIn("PGBR-v0.00.39-LT.apk",readme)
        changelog=(ROOT/"CHANGELOG.md").read_text(encoding="utf-8")
        self.assertIn("v0.00.40-LT",changelog)
        master=(ROOT/"PGBR_ARQUIVO_MESTRE_UNICO.md").read_text(encoding="utf-8")
        self.assertIn("LT040",master)

if __name__=="__main__":unittest.main()
