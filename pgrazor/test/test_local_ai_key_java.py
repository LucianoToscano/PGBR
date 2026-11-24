from pathlib import Path
import subprocess
import tempfile
import unittest
from test_control_java import java_tools
ROOT=Path(__file__).resolve().parents[1]
class AiKeyTests(unittest.TestCase):
    def test_envelope(self):
        compiler,runtime=java_tools()
        with tempfile.TemporaryDirectory() as d:
            p=subprocess.run([str(compiler),'-encoding','UTF-8','-d',d,str(ROOT/'src/dev/pgrazor/LocalAiKeyEnvelope.java'),str(ROOT/'test/LocalAiKeyEnvelopeTest.java')],capture_output=True,text=True,timeout=30)
            self.assertEqual(p.returncode,0,p.stdout+p.stderr)
            p=subprocess.run([str(runtime),'-cp',d,'dev.pgrazor.LocalAiKeyEnvelopeTest'],capture_output=True,text=True,timeout=30)
            self.assertEqual(p.returncode,0,p.stdout+p.stderr)
            self.assertIn('PASS: local AI credential envelope',p.stdout)
    def test_boundaries(self):
        src=ROOT/'src/dev/pgrazor'
        vault=(src/'LocalAiKeyVault.java').read_text(encoding='utf-8')
        ui=(src/'LocalAiKeyDialog.java').read_text(encoding='utf-8')
        for term in ['dev.pgrazor.preview','getNoBackupFilesDir()','AndroidKeyStore','ATOMIC_MOVE','Arrays.fill(plaintext']:
            self.assertIn(term,vault)
        self.assertIn('LocalAiKeyVault.allowed(a)',ui)
        self.assertIn('TYPE_TEXT_VARIATION_PASSWORD',ui)
        self.assertIn('presentPrivate',ui)
        self.assertIn('if(rejected[0])',ui)
        self.assertIn('if(ds==0&&de==dst.length())',ui)
        self.assertLess(ui.index('if(rejected[0])'),ui.index('value.getChars('))
        self.assertNotIn('LocalAiKeyDialog.show',(src/'SettingsPanel.java').read_text(encoding='utf-8'))
        for source in [vault,ui]:
            for forbidden in ['HttpURLConnection','openConnection(','Control.','SharedPreferences','Log.']:
                self.assertNotIn(forbidden,source)
