from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1] / "src/dev/pgrazor"

class CatchPreviewPreferenceTests(unittest.TestCase):
    def test_default_and_persistence(self):
        code=(ROOT / "Control.java").read_text(encoding="utf-8")
        self.assertIn("catchPreviewShown=true",code)
        self.assertIn('catchPreviewShown=!"false".equals(p.getProperty("catchPreviewShown","true"))',code)
        self.assertIn('p.setProperty("catchPreviewShown",String.valueOf(catchPreviewShown))',code)
        method=code.split("public static void setCatchPreviewShown(boolean value)",1)[1].split("private static void invalidateQuickBallOutcome",1)[0]
        self.assertIn("catchOutcome.invalidate();mapThrowOutcome.invalidate();",method)
        self.assertIn("saveSettings();",method)
        self.assertNotIn("setQuickSummaryEnabled",method)
        self.assertNotIn("mapCatchStatus.clear()",method)
    def test_display_only_existing_outcomes(self):
        code=(ROOT / "Control.java").read_text(encoding="utf-8")
        self.assertIn("() -> catchPreviewShown && quickBallAllowed(), Control::autoTransferAllowed",code)
        self.assertIn("() -> catchPreviewShown && autoThrow() && autoThrowOnMap() && hostLive(), () -> false",code)
        ui=(ROOT / "SettingsPanel.java").read_text(encoding="utf-8")
        self.assertIn('LinearLayout preview=group(page,"Prévia de captura")',ui)
        self.assertIn("Control.setCatchPreviewShown(on)",ui)
