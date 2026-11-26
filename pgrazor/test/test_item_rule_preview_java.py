"""Offline inventory review fixtures; no game or item actions."""
from pathlib import Path
import subprocess
import tempfile
import unittest
from test_control_java import java_tools

ROOT = Path(__file__).resolve().parents[1]

class ItemRulePreviewTests(unittest.TestCase):
    def test_preview_java(self):
        compiler, runtime = java_tools()
        with tempfile.TemporaryDirectory(prefix="pgbr-item-review-") as work:
            result = subprocess.run([str(compiler), "-encoding", "UTF-8", "-d", work,
                str(ROOT/"src/dev/pgrazor/ItemRulePreview.java"),
                str(ROOT/"test/ItemRulePreviewTest.java")], capture_output=True, text=True, timeout=30)
            self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
            result = subprocess.run([str(runtime), "-cp", work, "dev.pgrazor.ItemRulePreviewTest"], capture_output=True, text=True, timeout=30)
            self.assertEqual(result.returncode, 0, result.stdout + result.stderr)

    def test_review_is_before_existing_save(self):
        source=(ROOT/"src/dev/pgrazor/SettingsPanel.java").read_text(encoding="utf-8")
        self.assertIn("new java.util.TreeMap<>(limits)",source)
        self.assertIn("new java.util.HashSet<>(selected)",source)
        self.assertIn("if(!active){apply.run();return;}",source)
        self.assertIn('setPositiveButton("Confirmar limites",(review,which)->apply.run())',source)
        self.assertIn("!originalRules.equals(Control.itemRules())",source)
