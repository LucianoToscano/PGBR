"""Regression for PT-BR UI presentation instrumentation. No source writes."""
import importlib.util
from pathlib import Path
import unittest

SOURCE=Path(__file__).resolve().parents[2]/"tools"/"ptbr_wire.py"
spec=importlib.util.spec_from_file_location("pgbr_ptbr_wire",SOURCE)
wire=importlib.util.module_from_spec(spec)
spec.loader.exec_module(wire)

class PtBrWireTests(unittest.TestCase):
    def test_bare_accessibility_and_dotted_errors(self):
        original=('setContentDescription("Close PGBR settings");\n'
                  'view.setError("Invalid location");\n'
                  'void setContentDescription(String s) {}\n')
        new,counts=wire.localize(original,wire.PREFIX)
        self.assertIn('setContentDescription(dev.pgrazor.PtBr.tr("Close PGBR settings"))',new)
        self.assertIn('view.setError(dev.pgrazor.PtBr.tr("Invalid location"))',new)
        self.assertIn('void setContentDescription(String s) {}',new)
        self.assertEqual(counts['setContentDescription'],1)
        self.assertEqual(wire.localize(new,wire.PREFIX)[0],new)

    def test_toast_text_is_second_argument_and_idempotent(self):
        original=('Toast.makeText(activity, "Hello", Toast.LENGTH_SHORT).show();\n'
                  'android.widget.Toast.makeText(activity, "World", Toast.LENGTH_LONG).show();\n'
                  'Toast.makeText(activity, R.string.status, Toast.LENGTH_SHORT).show();\n')
        new,counts=wire.localize(original,wire.PREFIX)
        self.assertIn('Toast.makeText(activity, dev.pgrazor.PtBr.tr("Hello"), Toast.LENGTH_SHORT)',new)
        self.assertIn('android.widget.Toast.makeText(activity, dev.pgrazor.PtBr.tr("World"), Toast.LENGTH_LONG)',new)
        self.assertIn('Toast.makeText(activity, R.string.status, Toast.LENGTH_SHORT)',new)
        self.assertEqual(counts['Toast.makeText'],2)
        self.assertEqual(wire.localize(new,wire.PREFIX)[0],new)

    def test_comments_and_method_declarations_not_modified(self):
        original=('// setContentDescription("fake")\n'
                  '/* Toast.makeText(context, "fake", 0) */\n'
                  'void setContentDescription(CharSequence text) {}\n')
        self.assertEqual(wire.localize(original,wire.PREFIX)[0],original)

    def test_root_prefix_never_uses_game_prefix(self):
        source='view.setError("Erro");\n'
        translated,_=wire.localize(source,wire.ROOT_PREFIX)
        self.assertIn('dev.pgrazor.root.RootPtBr.tr("Erro")',translated)
