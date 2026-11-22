"""LT036 guard for training VNext, Rocket precedence and installation caveats."""
from pathlib import Path
import unittest
ROOT=Path(__file__).resolve().parents[2]

class TrainingScopePolicyTest(unittest.TestCase):
    def test_training_is_a_candidate_not_generic_npc_action(self):
        scene=(ROOT/"pgrazor/native/pvp_scene.h").read_text(encoding="utf-8")
        host=(ROOT/"pgrazor/native/combat_host.cpp").read_text(encoding="utf-8")
        java=(ROOT/"pgrazor/src/dev/pgrazor/Control.java").read_text(encoding="utf-8")
        for fragment in ("TRAINING_ENTRY_MS=45000",'"Niantic.Holoholo.Combat.NpcTrainers"',
                         '"CombatPreparationRootGuiController"',
                         "(stage==HUB || stage==TRAINING_ENTRY || stage==VERIFIED_PVP_ENTRY)",
                         "&& live(now)",
                         "stage==PREPARATION","MAX_COMBAT_MS=1200000"):
            self.assertIn(fragment,scene)
        self.assertIn("g_pvp_scene.training()",host)
        self.assertIn('"mode":"training"',host)
        self.assertIn('g_pvp_scene.reset();',host)
        self.assertIn('if(g_pvp_ui_root)pvp_leave(tid,"rocket-scope-override");',host)
        self.assertIn("pvpBattleOptions=SharedBattleOptions.pvpMask(value,pvpBattleOptions)",java)
        self.assertIn("rocketOptions=shared;",java)
        self.assertIn("Treinamento VNext",java)
    def test_installation_is_not_misrepresented_as_signature_fix(self):
        nonroot=(ROOT/"pgrazor/nonroot.py").read_text(encoding="utf-8")
        root=(ROOT/"pgrazor/rooted.py").read_text(encoding="utf-8")
        note=(ROOT/"docs/QA_LT036_TREINAMENTO_INSTALACAO.md").read_text(encoding="utf-8")
        self.assertIn("package_name!='com.nianticlabs.pokemongo'",nonroot)
        self.assertIn("Signed with local development key",nonroot)
        self.assertIn("INSTALLER_PACKAGE = 'dev.pgrazor.root'",root)
        self.assertIn("INSTALL_FAILED_UPDATE_INCOMPATIBLE",note)
        self.assertIn("device_tested=false",note)
if __name__=="__main__":
    unittest.main()
