"""Offline bounded log viewer and privacy lifecycle checks."""
from pathlib import Path
import subprocess
import tempfile
import unittest
from test_control_java import java_tools

ROOT=Path(__file__).resolve().parents[1]

class LogTailTests(unittest.TestCase):
    def test_bounded_snapshot_java(self):
        compiler,runtime=java_tools()
        with tempfile.TemporaryDirectory(prefix="pgbr-log-tail-") as work:
            files=[ROOT/"src/dev/pgrazor/LogTail.java",ROOT/"test/LogTailTest.java"]
            result=subprocess.run([str(compiler),"-encoding","UTF-8","-d",work,*map(str,files)],capture_output=True,text=True,timeout=30)
            self.assertEqual(result.returncode,0,result.stdout+result.stderr)
            result=subprocess.run([str(runtime),"-cp",work,"dev.pgrazor.LogTailTest"],capture_output=True,text=True,timeout=30)
            self.assertEqual(result.returncode,0,result.stdout+result.stderr)
    def test_both_viewers_use_bounded_snapshot(self):
        for name in ("LogActivity.java","RootedLogDialog.java"):
            source=(ROOT/"src/dev/pgrazor"/name).read_text(encoding="utf-8")
            self.assertIn("LogTail.read(file)",source)
            self.assertIn("LogExport.displayLog(tail.bytes,Control.trainerPrivacyMode()!=0)",source)
            self.assertNotIn("error.getMessage()",source)
            self.assertNotIn("input.length()-",source)
    def test_privacy_lifecycle(self):
        activity=(ROOT/"src/dev/pgrazor/LogActivity.java").read_text(encoding="utf-8")
        root=(ROOT/"src/dev/pgrazor/RootedLogDialog.java").read_text(encoding="utf-8")
        self.assertIn("if(text!=null)refresh()",activity)
        self.assertIn('if(Control.trainerPrivacyMode()!=openingPrivacyMode){text.setText("");dialog.dismiss();return;}',root)
        self.assertIn("handler.removeCallbacks(privacyCheck)",root)
