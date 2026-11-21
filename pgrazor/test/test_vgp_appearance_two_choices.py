"""Regression checks for VGP's two distinct icon choices and independent background."""
from pathlib import Path
import unittest

ROOT=Path(__file__).resolve().parents[1]

class VgpAppearanceTests(unittest.TestCase):
    def test_distinct_icon_choices_and_independent_background(self):
        settings=(ROOT/"src/dev/pgrazor/SettingsPanel.java").read_text(encoding="utf-8")
        self.assertIn('final ImageView[] choices=new ImageView[2]',settings)
        self.assertIn('final String[] files={"vgp-ring.png","vgp-hd-states.webp"}',settings)
        self.assertNotIn('"vgp-hd-states.webp","vgp-hd-states.webp"',settings)
        self.assertIn('"Fundo cinza (0% = transparente)"',settings)
        self.assertIn('if(Control.vgpIconStyle()==2)Control.setVgpIconStyle(1)',settings)
        self.assertIn('Control.setVgpBackgroundOpacity(value)',settings)
        self.assertIn('int current=Control.vgpIconStyle()==0?0:1',settings)
        pad=(ROOT/"src/dev/pgrazor/Pad.java").read_text(encoding="utf-8")
        self.assertIn('if((style==1||style==3)&&Control.vgpBackgroundOpacity()>0)',pad)
    def test_engine_does_not_require_optional_quickcatch_or_transfer(self):
        control=(ROOT/"src/dev/pgrazor/Control.java").read_text(encoding="utf-8")
        pos=control.index("public static boolean vgpAllowed(){")
        end=control.index("public static void vgpResult",pos)
        policy=control[pos:end]
        self.assertIn("vgpEnabled&&vgpReady&&hostLive()",policy)
        self.assertNotIn("itemAccessAllowed()",policy.split("return ",1)[-1])
        self.assertIn("!ShundoSniper.active()&&!AutoRaid.running()",policy)

if __name__=="__main__":
    unittest.main()
