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
            sources=[ROOT/'src/dev/pgrazor'/p for p in ['LocalAuditPolicy.java','LocalCollectionPreview.java','OverlayGesturePolicy.java','GpxPreviewGeometry.java','GpxRoute.java','GpxCodec.java']]
            sources.extend([ROOT/'test/LocalAuditTest.java',ROOT/'test/OverlayGestureTest.java'])
            p=subprocess.run([str(compiler),'-encoding','UTF-8','-d',d,*map(str,sources)],capture_output=True,text=True,timeout=30)
            self.assertEqual(p.returncode,0,p.stdout+p.stderr)
            p=subprocess.run([str(runtime),'-cp',d,'LocalAuditTest'],capture_output=True,text=True,timeout=30)
            self.assertEqual(p.returncode,0,p.stdout+p.stderr)
            self.assertIn('PASS: local audit',p.stdout)
            p=subprocess.run([str(runtime),'-cp',d,'dev.pgrazor.OverlayGestureTest'],capture_output=True,text=True,timeout=30)
            self.assertEqual(p.returncode,0,p.stdout+p.stderr)
            self.assertIn('PASS: overlay gesture',p.stdout)

    def test_species_protection_is_available_in_both_previews(self):
        source=(ROOT/'src/dev/pgrazor/LocalAuditDialog.java').read_text(encoding='utf-8')
        individual=source.split(' static void collection(Activity a){',1)[1].split(' static void batch(Activity a){',1)[0]
        batch=source.split(' static void batch(Activity a){',1)[1].split(' static void reason(Activity a)',1)[0]
        self.assertIn('LocalAuditPolicy.withSpeciesProtection(',individual)
        self.assertIn('LocalAuditPolicy.protectedNames(names.getText().toString())',individual)
        self.assertIn('invalidateOnEdit(result,name,cp,atk,def,sta,limit,names)',individual)
        self.assertIn('LocalAuditPolicy.protectedNames(protectedNames.getText().toString())',batch)

    def test_manual_tag_selector(self):
        source=(ROOT/'src/dev/pgrazor/LocalAuditDialog.java').read_text(encoding='utf-8')
        panel=source.split(' static void tagReview(Activity a){',1)[1].split(' static void summarize(Activity a){',1)[0]
        self.assertIn('LocalAuditPolicy.tagCatalog(',panel)
        self.assertIn('LocalAuditPolicy.reviewTags(',panel)
        self.assertIn('complete.setChecked(false)',panel)
        self.assertIn('TAG=?',panel)
        self.assertNotIn('Control.',panel)

    def test_local_panel_accessibility_and_revalidation(self):
        source=(ROOT/'src/dev/pgrazor/LocalAuditDialog.java').read_text(encoding='utf-8')
        self.assertGreaterEqual(source.count('label.setLabelFor('),2)
        self.assertIn('TYPE_NUMBER_FLAG_SIGNED',source)
        self.assertIn('TYPE_NUMBER_FLAG_DECIMAL',source)
        self.assertIn('invalidateOnEdit(r,code)',source)
        self.assertIn('seen.setOnCheckedChangeListener((v,on)->{complete.setChecked(false)',source)
        self.assertNotIn('.setView(b).setPositiveButton("Fechar",null)',source)
        self.assertIn('dd/MM/yyyy HH:mm:ss Z',source)
        gpx=(ROOT/'src/dev/pgrazor/GpxPreviewView.java').read_text(encoding='utf-8')
        self.assertIn('hint.setLabelFor(km.getId())',gpx)
        self.assertIn('Azul: trecho percorrido',gpx)

    def test_local_privacy_boundaries(self):
        source=(ROOT/'src/dev/pgrazor/LocalAuditDialog.java').read_text(encoding='utf-8')
        self.assertIn('FLAG_SECURE',source)
        self.assertIn('setSaveEnabled(false)',source)
        self.assertIn('IMPORTANT_FOR_AUTOFILL_NO',source)
        self.assertIn('IME_FLAG_NO_PERSONALIZED_LEARNING',source)
        self.assertIn('privateInput(records)',source)
        self.assertIn('privateInput(source)',source)
        for title in ['Prévia local de filtros','Prévia de coleção em lote','Resumo local de diagnóstico','Seleção visual de etiquetas']:
            self.assertIn('presentPrivate(a,new AlertDialog.Builder(MapTheme.wrap(a)).setTitle("'+title+'")',source)
        self.assertIn('present(a,new AlertDialog.Builder(MapTheme.wrap(a)).setTitle("Conferência visual de itens")',source)
        self.assertIn('evaluatedAt[0]=evaluationTime()',source)

    def test_new_ui_is_read_only_and_wired(self):
        for name in ['LocalAuditPolicy.java','LocalCollectionPreview.java','LocalAuditDialog.java','LocalDiagnosticSummary.java','GpxPreviewView.java','GpxPreviewGeometry.java']:
            source=(ROOT/'src/dev/pgrazor'/name).read_text(encoding='utf-8')
            for forbidden in ['NativeCore.','Control.engine','HttpURLConnection','transferPokemon','Runtime.getRuntime']:
                self.assertNotIn(forbidden,source)
        self.assertIn('LocalAuditDialog.show(activity)',(ROOT/'src/dev/pgrazor/SettingsPanel.java').read_text(encoding='utf-8'))
        self.assertIn('GpxPreviewView.show(activity,current)',(ROOT/'src/dev/pgrazor/GpxRouteDialog.java').read_text(encoding='utf-8'))
