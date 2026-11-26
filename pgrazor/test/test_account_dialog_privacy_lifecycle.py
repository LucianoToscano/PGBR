"""Account chooser privacy lifecycle contracts; no credentials or runtime needed."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]

class AccountDialogPrivacyLifecycle(unittest.TestCase):
    def test_sensitive_children_tracked_and_dismissed(self):
        s = (ROOT / "src/dev/pgrazor/AccountsDialog.java").read_text(encoding="utf-8")
        self.assertIn("showChild(children,editor);", s)
        self.assertIn("confirm(activity,profile,children)", s)
        self.assertIn('showChild(children,new AlertDialog.Builder(ui).setTitle(dev.pgrazor.PtBr.tr("Delete "', s)
        self.assertIn("dismissChildren(children);dialog.dismiss();return;", s)
        self.assertIn("handler.removeCallbacksAndMessages(null);dismissChildren(children);", s)
        self.assertIn("child.setOnDismissListener(d->children.remove(child))", s)
        self.assertIn("new java.util.ArrayList<>(children)", s)
        self.assertIn("if(Control.trainerPrivacyMode()!=openingPrivacyMode)return;", s)
        self.assertIn("if(openingPrivacyMode==0)", s)
        self.assertIn('if (privateName) name.setHint("Novo nome desta conta");', s)
