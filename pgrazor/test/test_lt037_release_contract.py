"""LT037 QA: unified scoped attack must not override published LT036."""
from pathlib import Path
import unittest
ROOT=Path(__file__).resolve().parents[2]

class Lt037ReleaseContract(unittest.TestCase):
    def test_release_and_history(self):
        self.assertEqual((ROOT/"pgrazor/VERSION").read_text(encoding="utf-8").strip(),"0.00.37-LT")
        workflow=(ROOT/".github/workflows/pgbr-lt037-release.yml").read_text(encoding="utf-8")
        for item in ("PGRAZOR_VERSION: v0.00.37-LT","--tag v0.00.37-LT",
                     "PGBR-v0.00.37-LT.apk","PGBR-v0.00.37-LT-Root.apk",
                     "PGBR-v0.00.37-LT-Root-module.zip","SHA256SUMS.txt",
                     "shared_auto_battle_gate_test.cpp","pgrazor/native/pvp_scene_test.cpp",
                     "PGBR-LT037-PGR037-Root-e-SemRoot-QA",
                     "pgrazor/nonroot.py","pgrazor/rooted.py","tools/check.py",
                     "tools/verify_release.py","--prerelease"):
            with self.subTest(item=item):self.assertIn(item,workflow)
        for prior in (33,34,35,36):
            old=(ROOT/f".github/workflows/pgbr-lt0{prior}-release.yml").read_text(encoding="utf-8")
            self.assertIn(f"PGRAZOR_VERSION: v0.00.{prior}-LT",old)
            self.assertNotIn("'pgrazor/VERSION'",old)
        note=(ROOT/"docs/releases/v0.00.37-LT.md").read_text(encoding="utf-8")
        self.assertIn("device_tested=false",note)
        self.assertIn("Batalhas Max",note)
        self.assertIn("LT037",(ROOT/"PGBR_ARQUIVO_MESTRE_UNICO.md").read_text(encoding="utf-8"))
    def test_native_shared_scope(self):
        host=(ROOT/"pgrazor/native/combat_host.cpp").read_text(encoding="utf-8")
        probe=(ROOT/"pgrazor/native/pvp_probe.inc").read_text(encoding="utf-8")
        self.assertIn("PgrSharedAutoBattleGate::effective",host)
        self.assertIn("auto_mask?rocket:0",host)
        self.assertIn("const bool on=active&&rocket&&!rocket_failed&&rocket_active&&(auto_mask&2u)",host)
        self.assertIn("!g_pvp_ui_root",host)
        self.assertIn("g_pvp_scene.positive_pvp(now_ms())",probe)
        self.assertIn("CombatDirectorV2.IsPvPCombat",probe)
if __name__=="__main__":
    unittest.main()
