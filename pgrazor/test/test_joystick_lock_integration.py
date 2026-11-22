"""Verificações de regressão para a integração visual do joystick ao PGBR."""
from pathlib import Path
import unittest

ROOT=Path(__file__).resolve().parents[1]
PAD=ROOT / "src/dev/pgrazor/Pad.java"

class JoystickLockIntegrationTests(unittest.TestCase):
    def test_preserves_existing_styles_and_safely_stops(self):
        text=PAD.read_text(encoding="utf-8")
        self.assertIn("stick.pointer>=0||stick.isLocked",text)
        self.assertIn("drawLockBadge(canvas,cx,cy,outer,knobX,knobY)",text)
        self.assertIn("drawLockBadge(canvas,cx,cy,radius,knobX,knobY)",text)
        self.assertIn("EternatusVisualEffects stormFx",text)
        self.assertIn("JoystickImage.load(getContext())",text)
        self.assertIn("EternatusRouteRunner.stop(",text)
        self.assertIn("removeCallbacks(popLockRunnable)",text)
        self.assertIn("if(isLocked||pointer>=0)release(false)",text)
        self.assertIn("ACTION_CANCEL",text)
        self.assertIn("stick.release(true); Control.suspendForBackground()",text)
