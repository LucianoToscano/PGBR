from pathlib import Path
import unittest
ROOT=Path(__file__).resolve().parents[2]
JAVA=ROOT/"pgrazor"/"src"/"dev"/"pgrazor"
class BattleShortcutContract(unittest.TestCase):
    def test_uses_existing_preference_bits(self):
        pad=(JAVA/"Pad.java").read_text(encoding="utf-8")
        self.assertIn("Control.setRocketOptions(Control.rocketOptions()^bit)",pad)
        self.assertIn("final int bit=1<<i;",pad)
        self.assertIn('String[] labels={"Ágil","Carregado","Escudo"};',pad)
    def test_visibility_depends_on_scope_and_overlay(self):
        control=(JAVA/"Control.java").read_text(encoding="utf-8")
        pad=(JAVA/"Pad.java").read_text(encoding="utf-8")
        self.assertIn("hostLive()&&(rocketScopeActive||pvpBattleReady)",control)
        self.assertIn('if("rocket-scope".equals(kind))',control)
        self.assertIn("Control.battleShortcutsVisible()&&!overlaysHidden&&!settings.isVisible()",pad)
        self.assertIn("refreshBattleShortcuts();",pad)
if __name__=="__main__":
    unittest.main()
