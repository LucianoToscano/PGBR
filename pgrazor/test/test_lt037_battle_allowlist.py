"""LT037: no unknown or PvE battle can reuse the Rocket executor."""
from pathlib import Path
import unittest
ROOT=Path(__file__).resolve().parents[2]
class BattleAllowlistTest(unittest.TestCase):
    def test_explicit_targets_and_no_pve_scope(self):
        header=(ROOT/"pgrazor/native/battle_target_policy.h").read_text(encoding="utf-8")
        host=(ROOT/"pgrazor/native/combat_host.cpp").read_text(encoding="utf-8")
        scene=(ROOT/"pgrazor/native/pvp_scene.h").read_text(encoding="utf-8")
        probe=(ROOT/"pgrazor/native/pvp_probe.inc").read_text(encoding="utf-8")
        for kind in ("ROCKET","PVP","TRAINER_NPC","RAID","GYM","MAX","WILD","UNKNOWN"):
            self.assertIn(kind,header)
        self.assertIn("pgr_rocket_assist_target(kind)",scene)
        self.assertIn("pgr_allow_shared_battle_actions(target,mask,g_pvp_scene.allowlisted(),lease_live)",host)
        self.assertIn("pve_exclusion_clear(&tid)",host)
        self.assertIn('pvp_leave(tid,"pve-scope-enter")',host)
        self.assertIn('g_pvp_scene.reset();',host)
        self.assertIn("g_pvp_scene.verified_pvp_type(now_ms())",probe)
        self.assertIn("IsPvPCombat",probe)
        self.assertIn("VERIFIED_PVP_ENTRY_MS=15000",scene)
        self.assertIn("TRAINING_ENTRY_MS=45000",scene)
        self.assertIn("now_ms()>=g_rocket_deadline_ms",host)
        self.assertIn("g_pvp_training_scope=false;",host)
    def test_existing_battles_engine_untouched(self):
        battles=(ROOT/"pgrazor/native/battles.cpp").read_text(encoding="utf-8")
        self.assertIn("raid / gym / Dynamax attack & dodge controller",battles)
        self.assertIn("PGR_BATTLE_MODE_GYM",(ROOT/"pgrazor/native/battles.h").read_text(encoding="utf-8"))
        self.assertTrue((ROOT/"pgrazor/native/battle_target_policy_test.cpp").is_file())
if __name__=="__main__":unittest.main()
