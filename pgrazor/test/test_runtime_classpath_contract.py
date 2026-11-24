"""Regressão: compilação e execução precisam do mesmo org.json.

O CI ficou vermelho com

    Exception in thread "main" java.lang.NoClassDefFoundError: org/json/JSONObject
        at dev.pgrazor.PokemonFeedSearchTest.main(PokemonFeedSearchTest.java:21)

depois de uma compilação em rc=0. Nada no código do PGBR estava errado: o teste compilou com
`-classpath android.jar` e executou só com o diretório de saída. Este arquivo impede a volta
desse padrão em duas frentes:

1. `org_json_support.resolution()` nunca devolve fontes E classpath vazio ao mesmo tempo;
2. um experimento real, com um org.json mínimo próprio, prova que repetir o classpath de
   compilação na execução é o que separa sucesso de `NoClassDefFoundError`.
"""

import shutil
import subprocess
import tempfile
import textwrap
import unittest
from pathlib import Path

import org_json_support as oj

try:
    from test_control_java import java_tools
except Exception:  # pragma: no cover - o ambiente sem JDK é tratado pelos testes JVM
    java_tools = None


class ResolutionContractTest(unittest.TestCase):
    def test_every_resolution_feeds_the_runtime(self):
        """Qualquer modo válido dá ao runtime uma forma concreta de achar org.json."""
        with tempfile.TemporaryDirectory() as work:
            options = oj.resolution(work)
            self.assertIn(options['mode'], ('sdk-sources', 'android-jar', 'vendored-stubs', 'missing'))
            if options['mode'] == 'missing':
                self.skipTest('ambiente sem fontes do SDK, sem android.jar e sem stubs')
            self.assertTrue(options['sources'] or options['classpath'],
                            'modo %s não oferece nada ao runtime' % options['mode'])
            self.assertIn('mode', options)

    def test_needs_jar_is_about_android_not_about_json(self):
        """Uma fonte de org.json vinda dos stubs não obriga android.jar no classpath."""
        with tempfile.TemporaryDirectory() as work:
            options = oj.resolution(work)
            if options['mode'] == 'vendored-stubs':
                self.assertFalse(options['needs_jar'])
            if options['mode'] == 'android-jar':
                self.assertTrue(options['needs_jar'])


class RuntimeClasspathRegressionTest(unittest.TestCase):
    """Prova executável: o bug só aparece se o classpath da execução divergir do de compilação."""

    def setUp(self):
        if java_tools is None:
            self.skipTest('JDK ausente')
        try:
            self.compiler, self.runtime = java_tools()
        except unittest.SkipTest:
            self.skipTest('JDK ausente')

    def build_fixture(self, work):
        """Mini org.json + classe que o usa, compilados para um diretório separado."""
        lib = Path(work) / 'libsrc'
        app = Path(work) / 'appsrc'
        classes = Path(work) / 'classes'
        lib.mkdir()
        app.mkdir()
        classes.mkdir()
        (lib / 'Marker.java').write_text(textwrap.dedent('''
            package mini.json;
            public final class Marker {
                public static String name() { return "mini"; }
            }
        '''), encoding='utf-8')
        (app / 'UsesMarker.java').write_text(textwrap.dedent('''
            public final class UsesMarker {
                public static void main(String[] args) {
                    System.out.println("PASS: " + mini.json.Marker.name());
                }
            }
        '''), encoding='utf-8')
        subprocess.run([str(self.compiler), '-d', str(classes), str(lib / 'Marker.java')],
                       check=True, capture_output=True, text=True)
        subprocess.run([str(self.compiler), '-classpath', str(classes), '-d', str(classes),
                        str(app / 'UsesMarker.java')], check=True, capture_output=True, text=True)
        return classes

    def test_divergent_runtime_classpath_reproduces_the_ci_failure(self):
        with tempfile.TemporaryDirectory() as work:
            classes = self.build_fixture(work)
            # Classpath de execução == compilação: verde.
            good = subprocess.run([str(self.runtime), '-cp', str(classes), 'UsesMarker'],
                                  capture_output=True, text=True)
            self.assertEqual(good.returncode, 0, good.stdout + good.stderr)
            self.assertIn('PASS: mini', good.stdout)

            # A forma que quebrou o CI: compilar com a dependência, executar sem ela.
            moved = Path(work) / 'only-app'
            moved.mkdir()
            for item in classes.glob('*.class'):
                shutil.copy(item, moved / item.name)
            bad = subprocess.run([str(self.runtime), '-cp', str(moved), 'UsesMarker'],
                                 capture_output=True, text=True)
            self.assertNotEqual(bad.returncode, 0, 'sumir com a dependência precisa falhar')
            self.assertIn('NoClassDefFoundError', bad.stderr,
                          'a falha precisa ser a mesma classe do CI: ' + bad.stderr)


if __name__ == '__main__':
    unittest.main()
