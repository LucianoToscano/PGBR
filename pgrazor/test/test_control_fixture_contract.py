"""Regressão do fixture JVM de Control: eventos novos precisam de colaboradores inertes."""
from pathlib import Path
import unittest

ROOT=Path(__file__).resolve().parents[1]

class ControlFixtureContractTests(unittest.TestCase):
    def test_session_stats_dispatch_has_test_stub(self):
        control=(ROOT / "src/dev/pgrazor/Control.java").read_text(encoding="utf-8")
        fixture=(ROOT / "test/test_control_java.py").read_text(encoding="utf-8")
        self.assertIn('SessionStats.publish(detail)',control)
        self.assertIn("'dev/pgrazor/SessionStats.java'",fixture)
        self.assertIn('static void publish(String detail){}',fixture)
