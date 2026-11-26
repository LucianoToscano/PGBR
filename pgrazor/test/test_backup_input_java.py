"""Local backup input bounds and privacy, independent of Android."""
from pathlib import Path
import subprocess
import tempfile
import unittest
from test_control_java import java_tools
ROOT=Path(__file__).resolve().parents[1]
class BackupInputTests(unittest.TestCase):
    def test_bounded_json_input(self):
        compiler,runtime=java_tools()
        with tempfile.TemporaryDirectory(prefix="pgbr-backup-input-") as work:
            paths=[ROOT/"src/dev/pgrazor/BackupInput.java",ROOT/"test/BackupInputTest.java"]
            result=subprocess.run([str(compiler),"-encoding","UTF-8","-d",work,*map(str,paths)],capture_output=True,text=True,timeout=30)
            self.assertEqual(result.returncode,0,result.stdout+result.stderr)
            result=subprocess.run([str(runtime),"-cp",work,"dev.pgrazor.BackupInputTest"],capture_output=True,text=True,timeout=30)
            self.assertEqual(result.returncode,0,result.stdout+result.stderr)
    def test_ui_uses_safe_input(self):
        s=(ROOT/"src/dev/pgrazor/SettingsBackup.java").read_text(encoding="utf-8")
        self.assertIn("BackupInput.readJson(in,LIMIT)",s)
        self.assertIn("BackupInput.safeFailure(e)",s)
        self.assertIn("SettingsBackupStore.rollbackFailed(e)",s)
        self.assertIn("não anonimiza o backup completo",s)
