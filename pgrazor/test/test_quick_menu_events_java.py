"""Compile and execute the real pure-Java menu visibility policy with no Android SDK."""
import subprocess
import tempfile
import unittest
from pathlib import Path
from test_control_java import java_tools

ROOT=Path(__file__).resolve().parents[1]


class QuickMenuEventsTests(unittest.TestCase):
    def test_real_policy_jvm(self):
        javac,java=java_tools()
        with tempfile.TemporaryDirectory(prefix="pgbr-menu-bits-") as temp:
            sources=[ROOT/"src/dev/pgrazor/QuickMenuVisibilityPolicy.java",
                     ROOT/"test/QuickMenuVisibilityPolicyTest.java"]
            compiled=subprocess.run([str(javac),"-encoding","UTF-8","-d",temp,
                                     *[str(x) for x in sources]],text=True,
                                     capture_output=True,timeout=35)
            self.assertEqual(compiled.returncode,0,compiled.stdout+compiled.stderr)
            tested=subprocess.run([str(java),"-cp",temp,"dev.pgrazor.QuickMenuVisibilityPolicyTest"],
                                  text=True,capture_output=True,timeout=20)
            self.assertEqual(tested.returncode,0,tested.stdout+tested.stderr)
            self.assertIn("PASS QuickMenuVisibilityPolicy",tested.stdout)

    def test_actual_ui_wires_event_shortcut_and_sources(self):
        quick=(ROOT/"src/dev/pgrazor/QuickMenu.java").read_text(encoding="utf-8")
        panel=(ROOT/"src/dev/pgrazor/SettingsPanel.java").read_text(encoding="utf-8")
        control=(ROOT/"src/dev/pgrazor/Control.java").read_text(encoding="utf-8")
        self.assertIn('row(EVENTS, "Events", true',quick)
        self.assertIn("QuickMenuVisibilityPolicy.EVENTS_BIT",quick)
        self.assertIn('page(MenuIcon.EVENTS, "Events")',panel)
        self.assertIn('"Mostrar Eventos no Menu Pokébola"',panel)
        self.assertIn('"Abrir seletor de Eventos"',panel)
        self.assertIn("setQuickMenuVisible(QuickMenuVisibilityPolicy.EVENTS_BIT,checked)",panel)
        self.assertIn("quickMenuChoices[9].setChecked(checked)",panel)
        for method in ("showHalloween","showGoWild","showEternatus"):
            self.assertIn("EventPanel."+method,panel)
        self.assertIn("quickMenuHidden=boundedSetting(p,\"quickMenuHidden\",QuickMenuVisibilityPolicy.MASK);",control)
        self.assertIn("QuickMenuVisibilityPolicy.validBit(bit)",control)
        self.assertIn("QuickMenuVisibilityPolicy.withVisibility",control)


if __name__=="__main__":
    unittest.main()
