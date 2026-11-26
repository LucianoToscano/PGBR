import shutil
import subprocess
import tempfile
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

class ReleaseCheckTest(unittest.TestCase):
    @unittest.skipUnless(shutil.which("javac") and shutil.which("java"), "JDK unavailable")
    def test_rules(self):
        with tempfile.TemporaryDirectory() as out:
            subprocess.run(["javac", "-d", out, str(ROOT / "src/dev/pgrazor/ReleaseCheckRules.java"),
                            str(ROOT / "test/ReleaseCheckRulesTest.java")], check=True)
            subprocess.run(["java", "-cp", out, "dev.pgrazor.ReleaseCheckRulesTest"], check=True)

    def test_user_initiated_and_no_install_permission(self):
        source = (ROOT / "src/dev/pgrazor/ReleaseCheck.java").read_text(encoding="utf-8")
        panel = (ROOT / "src/dev/pgrazor/SettingsPanel.java").read_text(encoding="utf-8")
        self.assertIn("releaseCheck.setOnClickListener", panel)
        self.assertIn("optBoolean(\"prerelease\", true)", source)
        self.assertIn("apk == 1 && checksums == 1", source)
        self.assertNotIn("PackageInstaller", source)
        self.assertNotIn("REQUEST_INSTALL_PACKAGES", source)
