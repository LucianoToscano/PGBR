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
    def test_original_simple_vgp_controls(self):
        settings=(ROOT/"src/dev/pgrazor/SettingsPanel.java").read_text(encoding="utf-8")
        pad=(ROOT/"src/dev/pgrazor/Pad.java").read_text(encoding="utf-8")
        control=(ROOT/"src/dev/pgrazor/Control.java").read_text(encoding="utf-8")
        controller=(ROOT/"src/dev/pgrazor/VgpController.java").read_text(encoding="utf-8")
        self.assertNotIn("vgpRunButton",settings)
        self.assertNotIn("vgpCooldownStop",settings)
        self.assertNotIn("showVgpHealthReport",settings)
        self.assertNotIn("Cooldown.remaining()",controller)
        self.assertNotIn("guardCooldown()",controller)
        self.assertNotIn("cooldownAction",controller)
        self.assertNotIn("guardVgpCooldown()",control)
        self.assertIn('public static boolean vgpEnabled(){return vgpOverlay&&vgpEnabled;}',control)
        self.assertIn("private static volatile boolean vgpStopOnCooldown=true",control)
        self.assertIn("&&Cooldown.remaining()>0",control)
        self.assertIn("&&!vgpCooldownPaused()",control)
        self.assertIn('p.setProperty("vgpStopOnCooldown",String.valueOf(vgpStopOnCooldown))',control)
        self.assertIn('switchRow(vgpC,"Stop on cooldown (estimated)")',settings)
        self.assertIn("!Control.vgpCooldownPaused()", (ROOT/"src/dev/pgrazor/SmartCatchSettings.java").read_text(encoding="utf-8"))
        self.assertIn("Control.vgpTapProtection()",pad)
        self.assertIn("enableVgpFromIcon()",pad)
        self.assertIn('"Escolher imagem VGP no celular"',settings)
        self.assertIn('"Opacidade do ícone VGP"',settings)
        self.assertIn('"Tamanho do VGP"',settings)

    def test_vgp_all_four_corners_and_persisted_bounds(self):
        drag=(ROOT/"src/dev/pgrazor/OverlayDrag.java").read_text(encoding="utf-8")
        viewport=(ROOT/"src/dev/pgrazor/UiViewport.java").read_text(encoding="utf-8")
        policy=(ROOT/"src/dev/pgrazor/UiViewportPolicy.java").read_text(encoding="utf-8")
        pad=(ROOT/"src/dev/pgrazor/Pad.java").read_text(encoding="utf-8")
        self.assertIn('return "vgp".equals(key)?UiViewport.vgpBounds(parent)',drag)
        self.assertGreaterEqual(drag.count("dragBounds()"),5)
        self.assertIn("safe.left=0;",viewport)
        self.assertIn("safe.right=parent.getWidth();",viewport)
        self.assertIn("UiViewport.vgpBounds(container)",pad)
        self.assertIn("UiViewportPolicy.edgeAlignedCenter(",pad)
        self.assertIn("static float edgeAlignedCenter(",policy)
        # The touch box remains >=48dp even if the artwork is smaller.
        self.assertIn("dp(Math.max(48,Control.vgpIconSize()))",pad)

    def test_engine_does_not_require_optional_quickcatch_or_transfer(self):
        control=(ROOT/"src/dev/pgrazor/Control.java").read_text(encoding="utf-8")
        pos=control.index("public static boolean vgpAllowed(){")
        end=control.index("public static void vgpResult",pos)
        policy=control[pos:end]
        self.assertIn("vgpEnabled&&vgpReady&&hostLive()",policy)
        self.assertNotIn("itemAccessAllowed()",policy.split("return vgpEnabled",1)[-1])
        self.assertIn("!ShundoSniper.active()&&!AutoRaid.running()",policy)

if __name__=="__main__":
    unittest.main()
