from pathlib import Path
import subprocess
import tempfile
import unittest
from test_control_java import java_tools
ROOT=Path(__file__).resolve().parents[1]
class LocalAuditTests(unittest.TestCase):
    def test_local_policy_and_gpx(self):
        compiler,runtime=java_tools()
        with tempfile.TemporaryDirectory() as d:
            sources=[ROOT/'src/dev/pgrazor'/p for p in ['LocalAuditPolicy.java','GpxPreviewGeometry.java','GpxRoute.java','GpxCodec.java']]
            sources.append(ROOT/'test/LocalAuditTest.java')
            p=subprocess.run([str(compiler),'-encoding','UTF-8','-d',d,*map(str,sources)],capture_output=True,text=True,timeout=30)
            self.assertEqual(p.returncode,0,p.stdout+p.stderr)
            p=subprocess.run([str(runtime),'-cp',d,'LocalAuditTest'],capture_output=True,text=True,timeout=30)
            self.assertEqual(p.returncode,0,p.stdout+p.stderr)
            self.assertIn('PASS: local audit',p.stdout)

    def test_new_ui_is_read_only_and_wired(self):
        for name in ['LocalAuditPolicy.java','LocalAuditDialog.java','GpxPreviewView.java','GpxPreviewGeometry.java']:
            source=(ROOT/'src/dev/pgrazor'/name).read_text(encoding='utf-8')
            for forbidden in ['NativeCore.','Control.engine','HttpURLConnection','transferPokemon','Runtime.getRuntime']:
                self.assertNotIn(forbidden,source)
        self.assertIn('LocalAuditDialog.show(activity)',(ROOT/'src/dev/pgrazor/SettingsPanel.java').read_text(encoding='utf-8'))
        self.assertIn('GpxPreviewView.show(activity,current)',(ROOT/'src/dev/pgrazor/GpxRouteDialog.java').read_text(encoding='utf-8'))
