"""Compile and exercise PGBR's real local Pokémon live-feed matcher.

O matcher usa org.json (catalog de nomes), que o JDK não traz. A resolução dessa dependência
está em `org_json_support.py`, com uma regra que não pode ser quebrada: **o classpath da
execução é o mesmo da compilação**.

Regressão que este teste vigia: ele compilava com `-classpath android.jar` e executava só com o
diretório de saída, então o runner falhava em `java.lang.NoClassDefFoundError:
org/json/JSONObject` mesmo com a compilação em rc=0. O erro era do teste, não do matcher.

Sem nenhuma fonte de org.json o teste é INCONCLUSIVO: falha com mensagem explícita em vez de
"passar" sem ter compilado nada.
"""

import subprocess
import tempfile
import unittest
from pathlib import Path

import org_json_support as oj
from test_control_java import java_tools

ROOT = Path(__file__).resolve().parents[1]
REPO = ROOT.parent


class PokemonFeedSearchTests(unittest.TestCase):
    def test_matcher(self):
        compiler, runtime = java_tools()
        with tempfile.TemporaryDirectory(prefix="pgbr-pokemon-search-") as work:
            options = oj.resolution(work)
            if options['mode'] == 'missing':
                self.fail("org.json precisa vir das fontes do SDK, do android.jar ou dos stubs "
                          "versionados — nenhuma das três foi encontrada, então o matcher não pode "
                          "ser testado (inconclusivo, não aprovado)")
            src = [ROOT / "src/dev/pgrazor/PokemonFeedSearch.java",
                   ROOT / "test/PokemonFeedSearchTest.java"]
            build_args = [str(compiler), "-encoding", "UTF-8"]
            if options['classpath']:
                build_args += ["-classpath", ":".join(str(p) for p in options['classpath'])]
            build_args += ["-d", work, *map(str, options['sources']), *map(str, src)]
            build = subprocess.run(build_args, capture_output=True, text=True, timeout=60)
            self.assertEqual(build.returncode, 0,
                             "o matcher real não compilou (org.json via %s).\n%s%s"
                             % (oj.describe(options), build.stdout, build.stderr))

            # Mesmo classpath da compilação, mais o diretório de saída. Sem exceção.
            run_cp = [str(work)] + [str(p) for p in options['classpath']]
            run = subprocess.run(
                [str(runtime), "-cp", ":".join(run_cp), "dev.pgrazor.PokemonFeedSearchTest"],
                capture_output=True, text=True, timeout=60)
            self.assertEqual(run.returncode, 0,
                             "o matcher não executou com o mesmo classpath da compilação "
                             "(org.json via %s).\n%s%s" % (oj.describe(options), run.stdout, run.stderr))
            self.assertIn("PASS:", run.stdout)


if __name__ == "__main__":
    unittest.main()
