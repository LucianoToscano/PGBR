"""Exercise the real GPX codec and store on a JVM, offline and without Android or a device.

The codec (GpxRoute/GpxCodec) is plain Java and runs on the standard SAX parser bundled with the
JDK, so it needs no stubs. The store (GpxStore) touches only android.content.Context, so it compiles
against a minimal shell that just supplies getApplicationContext()/getFilesDir(); no Android
behaviour is simulated. No captured account data, credentials, or network requests are used.
"""
from pathlib import Path
import subprocess
import tempfile
import unittest

from test_control_java import java_tools

ROOT = Path(__file__).resolve().parents[1]
SAMPLE = ROOT.parent / '14-barcelona-spain.gpx'

CONTEXT_STUB = '''package android.content;
public class Context {
 private final java.io.File files;
 public Context(java.io.File files){this.files=files;}
 public Context getApplicationContext(){return this;}
 public java.io.File getFilesDir(){return files;}
}'''


class GpxJavaTests(unittest.TestCase):
    def test_export_preflight_before_truncation(self):
        dialog = (ROOT / "src/dev/pgrazor/GpxRouteDialog.java").read_text(encoding="utf-8")
        export = dialog.split("private static void writeExport(", 1)[1].split("// ---- saved routes", 1)[0]
        self.assertLess(export.index("GpxCodec.encode(route)"), export.index("openOutputStream(uri"))
        self.assertIn("out.write(encoded)", export)

    def test_route_dialog_sentences_come_from_the_dictionary(self):
        """Toda frase com contagem de paradas vem do dicionário único, não de plural inglês montado.

        Antes, a lista de rotas salvas e o detalhe da rota montavam a frase em português dentro do
        Java ("%d paradas"). Isso impedia reutilizar a mesma tela em outra língua e deixava a
        contagem de uma parada errada em inglês. Agora a chave carrega o %d e o texto inteiro vem
        do dicionário.
        """
        import json
        dialog = (ROOT / "src/dev/pgrazor/GpxRouteDialog.java").read_text(encoding="utf-8")
        entries = json.loads((ROOT / "i18n/pt-BR.json").read_text(encoding="utf-8"))["translations"]
        for key in ("%s\n%d stops",
                    "%d stops.\n\nUse this route loads it as a draft; Start it yourself afterwards.",
                    "Selected route: “%s” (%d stops).",
                    "Select a route before previewing.",
                    "Route %d"):
            with self.subTest(key=key):
                self.assertIn(key, entries, "chave ausente do dicionário: " + key)
                self.assertNotEqual(entries[key], key)
        # A frase aparece no código apenas como chave (dentro de PtBr.tr), nunca já traduzida.
        self.assertIn('PtBr.tr("%s\\n%d stops")', dialog)
        self.assertIn('PtBr.tr("Selected route: “%s” (%d stops).")', dialog)
        self.assertNotIn("%d paradas", dialog, "frase em português montada no Java")

    def test_codec_and_route(self):
        compiler, runtime = java_tools()
        with tempfile.TemporaryDirectory(prefix='pgrazor-gpx-codec-') as directory:
            work = Path(directory)
            sources = [ROOT / 'src/dev/pgrazor/GpxRoute.java', ROOT / 'src/dev/pgrazor/GpxCodec.java',
                       ROOT / 'test/GpxRouteTest.java', ROOT / 'test/GpxCodecTest.java']
            compiled = subprocess.run([str(compiler), '-encoding', 'UTF-8', '-d', str(work), *map(str, sources)],
                                      capture_output=True, text=True, timeout=30)
            self.assertEqual(compiled.returncode, 0, compiled.stdout + compiled.stderr)
            result = subprocess.run([str(runtime), '-cp', str(work), 'GpxRouteTest'],
                                    capture_output=True, text=True, timeout=30)
            self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
            self.assertIn('PASS: GpxRoute', result.stdout)
            # Pass the user's real sample; GpxCodecTest treats a missing file as a skip.
            result = subprocess.run([str(runtime), '-cp', str(work), 'GpxCodecTest', str(SAMPLE)],
                                    capture_output=True, text=True, timeout=30)
            self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
            self.assertIn('PASS: GpxCodec parsing', result.stdout)

    def test_store(self):
        compiler, runtime = java_tools()
        with tempfile.TemporaryDirectory(prefix='pgrazor-gpx-store-') as directory:
            work = Path(directory)
            stub = work / 'android/content/Context.java'
            stub.parent.mkdir(parents=True, exist_ok=True)
            stub.write_text(CONTEXT_STUB, encoding='utf-8')
            profiles = work / 'AccountProfiles.java'
            profiles.write_text('''package dev.pgrazor;
final class AccountProfiles {
 static java.io.File file(android.content.Context context,String name) {
  return new java.io.File(context.getFilesDir(),name);
 }
}''', encoding='utf-8')
            data = work / 'data'
            data.mkdir()
            classes = work / 'classes'
            # The real PtBr is compiled with the store: the store's user-facing errors are translated
            # through the same dictionary the rest of the UI uses, so the harness must not stub it.
            sources = [stub, profiles, ROOT / 'src/dev/pgrazor/PtBr.java', ROOT / 'src/dev/pgrazor/GpxRoute.java',
                       ROOT / 'src/dev/pgrazor/GpxCodec.java', ROOT / 'src/dev/pgrazor/GpxStore.java',
                       ROOT / 'test/GpxStoreTest.java']
            compiled = subprocess.run([str(compiler), '-encoding', 'UTF-8', '-d', str(classes), *map(str, sources)],
                                      capture_output=True, text=True, timeout=30)
            self.assertEqual(compiled.returncode, 0, compiled.stdout + compiled.stderr)
            result = subprocess.run([str(runtime), '-cp', str(classes), 'GpxStoreTest', str(data)],
                                    capture_output=True, text=True, timeout=60)
            self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
            self.assertIn('PASS: GpxStore', result.stdout)


if __name__ == '__main__':
    unittest.main()
