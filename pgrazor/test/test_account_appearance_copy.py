from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[1]

class AccountAppearanceCopyTests(unittest.TestCase):
    @unittest.skipUnless(shutil.which("javac") and shutil.which("java"), "JDK unavailable")
    def test_allowlist(self):
        with tempfile.TemporaryDirectory() as out:
            subprocess.run(["javac", "-d", out, str(ROOT / "src/dev/pgrazor/AccountAppearancePolicy.java"),
                            str(ROOT / "test/AccountAppearancePolicyTest.java")], check=True)
            subprocess.run(["java", "-cp", out, "dev.pgrazor.AccountAppearancePolicyTest"], check=True)

    def test_copy_does_not_reload_gameplay(self):
        source=(ROOT / "src/dev/pgrazor/Control.java").read_text(encoding="utf-8")
        copy=source.split("static int copyAccountAppearance(",1)[1].split("static void saveSettings()",1)[0]
        self.assertIn("AccountAppearancePolicy.select(source)",copy)
        self.assertIn("saveSettings();",copy)
        self.assertNotIn("loadSettings(",copy)
        self.assertNotIn("importSettings(",copy)
