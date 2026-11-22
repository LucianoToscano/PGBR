"""Guard VGP's explicit-OFF native recovery contract without a device.

Native C++ behavior is exercised separately by encounter_host_test.cpp in the
native test suite. These static checks keep the critical masks and transitions
in the repository's device-free CI.
"""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
HOST = ROOT / "native/encounter_host.cpp"
CONTROLLER = ROOT / "native/vgp_controller.cpp"
POLICY = ROOT / "native/vgp_policy.cpp"
VISUAL = ROOT / "src/dev/pgrazor/Control.java"


class VgpNativeOffRecoveryTests(unittest.TestCase):
    def test_only_explicit_off_clears_error_latch(self):
        host = HOST.read_text(encoding="utf-8")
        self.assertIn("in.mask = g_opts.vgp_enabled", host)
        self.assertIn("g_last_vgp_enabled && !g_opts.vgp_enabled", host)
        self.assertIn("g_last_vgp_enabled && !options->vgp_enabled", host)
        self.assertEqual(host.count("g_vp->clear_block(tid);"), 2)
        policy = POLICY.read_text(encoding="utf-8")
        self.assertIn("g_blocked = false;  // quarantine (g_seen) deliberately preserved", policy)
        controller = CONTROLLER.read_text(encoding="utf-8")
        self.assertIn("if (in->mask == 0) { POL->clear_block(owner);", controller)
        test = (ROOT / "native/encounter_host_test.cpp").read_text(encoding="utf-8")
        self.assertIn("after_toggle.blocked==0", test)
        self.assertIn("after_off.seen_size==before_off.seen_size", test)

    def test_eternatus_visual_defaults_keep_explicit_choices(self):
        source = VISUAL.read_text(encoding="utf-8")
        self.assertIn("private static volatile int joystickStyle=1;", source)
        self.assertIn("private static volatile int menuLauncherIconStyle=1;", source)
        self.assertIn('readBoundedVisual(p,"menuLauncherIconStyle",1,0,2)', source)
        self.assertIn("saved>=0&&saved<=4?saved:", source)
        self.assertIn('AccountProfiles.file(app,"joystick-custom.png").isFile()?3:1', source)
        self.assertIn("joystickEternatusEffects=true", source)
        image = (ROOT / "src/dev/pgrazor/JoystickImage.java").read_text(encoding="utf-8")
        self.assertIn("if(Control.joystickStyle()==3)Control.setJoystickStyle(1)", image)


if __name__ == "__main__":
    unittest.main()
