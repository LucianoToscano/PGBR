from pathlib import Path
import unittest

ROOT=Path(__file__).resolve().parents[1]/"src/dev/pgrazor"

class AccountPrivacyUpdateLifecycleTests(unittest.TestCase):
    def test_private_mode_does_not_read_or_render_coordinates(self):
        source=(ROOT/"AccountsDialog.java").read_text(encoding="utf-8")
        self.assertIn("final int openingPrivacyMode=Control.trainerPrivacyMode()",source)
        self.assertIn('if(openingPrivacyMode==0)try{android.util.AtomicFile',source)
        self.assertIn('hidden.setText("Coordenadas ocultas")',source)
        self.assertIn("if(Control.trainerPrivacyMode()!=openingPrivacyMode){dismissChildren(children);dialog.dismiss();return;}",source)

    def test_copy_appearance_and_account_removal_lifecycle(self):
        source=(ROOT/"AccountsDialog.java").read_text(encoding="utf-8")
        self.assertIn("if(copyAppearance!=null)appearanceCopies.remove(copyAppearance)",source)
        profile=(ROOT/"AccountProfiles.java").read_text(encoding="utf-8")
        self.assertIn("static boolean canResetSignIn(){return store!=null&&available&&!verified&&!switching&&!restarting&&!deleting&&!copyingAppearance;}",profile)

    def test_opt_out_rechecked_before_query_and_ui(self):
        source=(ROOT/"UpdateChecker.java").read_text(encoding="utf-8")
        self.assertIn("if(!visible(activity)||!automaticEnabled(activity))return;",source)
        self.assertGreaterEqual(source.count("if(!visible(activity)||(automatic&&!automaticEnabled(activity)))return;"),2)
