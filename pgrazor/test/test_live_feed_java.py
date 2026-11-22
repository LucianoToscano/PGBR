"""Run the actual feed parser against Android's org.json implementation, offline.

Uses the installed SDK sources; only Android annotation imports/annotations are
removed in a temporary copy so the same JSON implementation runs on the JVM.
No captured account data, credentials, or network requests are used.
"""
import os
from pathlib import Path
import re
import subprocess
import tempfile
import unittest

import org_json_support as oj
from test_control_java import java_tools

ROOT = Path(__file__).resolve().parents[1]
REPO = ROOT.parent


class LiveFeedJavaTests(unittest.TestCase):
    _uses_vendored_json = False

    def _require_pass(self, main, result):
        """Um main de teste do PGBR precisa terminar com 0. Falha nunca é ignorada em silêncio.

        Se falhar, mostra a saída completa: é isso que diferencia "código quebrado" de
        "ambiente sem dependência", e é o que faltava quando o CI ficava vermelho sem detalhe.
        """
        self.assertEqual(result.returncode, 0,
                         '%s falhou (rc=%d)\nSTDOUT:\n%s\nSTDERR:\n%s'
                         % (main, result.returncode, result.stdout, result.stderr))

    def test_feed_data(self):
        compiler, runtime = java_tools()
        with tempfile.TemporaryDirectory(prefix='pgrazor-feed-test-') as directory:
            work = Path(directory)
            # org_json_support decide a fonte de org.json (fontes do SDK -> android.jar -> stubs).
            # Antes, a ausência das fontes do SDK fazia este teste ser IGNORADO, e um teste ignorado
            # no runner é indistinguível de um teste que nunca rodou.
            sources = []
            sources += [ROOT / 'src/dev/pgrazor/LiveFeedData.java', ROOT / 'src/dev/pgrazor/LiveFeedFilters.java', ROOT / 'src/dev/pgrazor/MapTapSequence.java', ROOT / 'src/dev/pgrazor/BerryPriority.java', ROOT / 'src/dev/pgrazor/SettingsBackupRules.java', ROOT / 'src/dev/pgrazor/QuickMenuVisibilityPolicy.java', ROOT / 'src/dev/pgrazor/AutoRaidPreferences.java', ROOT / 'src/dev/pgrazor/BuddyEmotionPreferences.java', ROOT / 'test/LiveFeedDataTest.java', ROOT / 'test/SettingsBackupRulesTest.java',
                        ROOT / 'src/dev/pgrazor/PogoFeedData.java', ROOT / 'src/dev/pgrazor/PogoFeedFilters.java', ROOT / 'test/PogoFeedDataTest.java', ROOT / 'test/PogoFeedFiltersTest.java',
                        ROOT / 'src/dev/pgrazor/PokeListFeedData.java', ROOT / 'test/PokeListFeedDataTest.java',
                        ROOT / 'src/dev/pgrazor/IFlowGoFeedData.java', ROOT / 'test/IFlowGoFeedDataTest.java',
                        ROOT / 'src/dev/pgrazor/DemoLiveFeedData.java', ROOT / 'test/DemoLiveFeedDataTest.java',
                        ROOT / 'src/dev/pgrazor/RocketMapFeedData.java', ROOT / 'test/RocketMapFeedDataTest.java',
                        ROOT / 'src/dev/pgrazor/CooldownState.java',
                        ROOT / 'src/dev/pgrazor/PokemonSelectorPolicy.java',
                        ROOT / 'test/PokemonSelectorPolicyTest.java']
            sources.append(ROOT / 'src/dev/pgrazor/TrainerAliasPolicy.java')
            sources.extend([ROOT / 'src/dev/pgrazor/HundoFeedData.java',
                            ROOT / 'test/HundoFeedDataTest.java',
                            ROOT / 'src/dev/pgrazor/SnipeFilters.java',
                            ROOT / 'test/SnipeFiltersTest.java',
                            ROOT / 'src/dev/pgrazor/SpawnContract.java',
                            ROOT / 'src/dev/pgrazor/SpawnSearch.java',
                            ROOT / 'src/dev/pgrazor/SpawnFilters.java',
                            ROOT / 'src/dev/pgrazor/SpawnProfiles.java',
                            ROOT / 'src/dev/pgrazor/SpawnSourceRegistry.java',
                            ROOT / 'src/dev/pgrazor/SpeciesGroups.java',
                            # LiveFeedClient referencia LiveFeedSession, então as duas entram juntas
                            # ou o parser inteiro deixa de compilar. Era esta a falha A do CI
                            # ("LiveFeedSession cannot be resolved"). O arquivo de sessão é o REAL;
                            # nunca um dublê de segurança.
                            ROOT / 'src/dev/pgrazor/LiveFeedSession.java',
                            ROOT / 'src/dev/pgrazor/LiveFeedClient.java',
                            ROOT / 'src/dev/pgrazor/PokemonFeedSearch.java',
                            ROOT / 'test/PokemonFeedSearchTest.java',
                            ROOT / 'test/SnipeEngineTest.java'])
            if (ROOT / 'test/LiveFeedFiltersTest.java').exists(): sources.append(ROOT / 'test/LiveFeedFiltersTest.java')
            # org.json e APIs do Android andam juntos: quem compila é quem executa. Era a
            # assimetria entre os dois classpaths que produzia "NoClassDefFoundError:
            # org/json/JSONObject" com a compilação em rc=0.
            options = oj.resolution(work)
            print('DIAGNOSTICO: org.json via ' + oj.describe(options))
            if options['mode'] == 'missing':
                self.fail('nenhuma fonte de org.json disponível (SDK sources, android.jar ou stubs '
                          'versionados): teste inconclusivo, não aprovado')
            android_jar = oj.android_jar()
            compile_sources = list(sources) + list(options['sources'])
            compile_args = [str(compiler), '-encoding', 'UTF-8']
            if options['classpath']:
                compile_args += ['-classpath', os.pathsep.join(str(p) for p in options['classpath'])]
            elif android_jar is not None and options['needs_jar']:
                # A sessão real (Keystore/SharedPreferences) precisa do framework mesmo quando
                # org.json vem de outra fonte.
                compile_args += ['-classpath', str(android_jar)]
            else:
                # Sem android.jar: os espelhos de API versionados entram como fontes. Nenhum dublê
                # substitui lógica de segurança — só as assinaturas do framework. O parser, o
                # cliente HTTP e a sessão sob teste são sempre os arquivos reais.
                compile_sources = sorted((REPO / 'tools/sniper_jvm/stubs/android').rglob('*.java')) \
                    + compile_sources
            compile_args += ['-d', str(work), *map(str, compile_sources)]
            result = subprocess.run(compile_args, capture_output=True, text=True, timeout=180)
            if result.returncode != 0:
                print("DIAGNOSTICO: compilacao falhou (rc=%d, org.json=%s)"
                      % (result.returncode, options['mode']))
                for line in ((result.stdout or '') + (result.stderr or '')).splitlines():
                    print("DIAG " + line)
                self.fail('o parser real não compilou.\n' + result.stdout + result.stderr)
            # Classpath de execução == classpath de compilação, sempre.
            run_cp = [str(work)] + [str(p) for p in options['classpath']]
            run_classpath = os.pathsep.join(run_cp)
            mains = ['LiveFeedDataTest','SettingsBackupRulesTest','PogoFeedDataTest','PogoFeedFiltersTest'] \
                + (['LiveFeedFiltersTest'] if (ROOT / 'test/LiveFeedFiltersTest.java').exists() else [])
            for main in mains:
                result = subprocess.run([str(runtime), '-cp', run_classpath, main], capture_output=True, text=True, timeout=30)
                self._require_pass(main, result)
            result = subprocess.run([str(runtime), '-cp', run_classpath, 'dev.pgrazor.HundoFeedDataTest'],
                                    capture_output=True, text=True, timeout=60)
            self._require_pass('HundoFeedDataTest', result)
            result = subprocess.run([str(runtime), '-cp', run_classpath, 'dev.pgrazor.SnipeFiltersTest'],
                                    capture_output=True, text=True, timeout=60)
            self._require_pass('SnipeFiltersTest', result)
            result = subprocess.run([str(runtime), '-cp', run_classpath, 'dev.pgrazor.PokeListFeedDataTest'],
                                    capture_output=True, text=True, timeout=60)
            self._require_pass('PokeListFeedDataTest', result)
            result = subprocess.run([str(runtime), '-cp', run_classpath, 'dev.pgrazor.DemoLiveFeedDataTest'],
                                    capture_output=True, text=True, timeout=60)
            self._require_pass('DemoLiveFeedDataTest', result)
            result = subprocess.run([str(runtime), '-cp', run_classpath, 'dev.pgrazor.IFlowGoFeedDataTest'],
                                    capture_output=True, text=True, timeout=60)
            self._require_pass('IFlowGoFeedDataTest', result)
            result = subprocess.run([str(runtime), '-cp', run_classpath, 'dev.pgrazor.RocketMapFeedDataTest'],
                                    capture_output=True, text=True, timeout=60)
            self._require_pass('RocketMapFeedDataTest', result)
            result = subprocess.run([str(runtime), '-cp', run_classpath, 'dev.pgrazor.PokemonFeedSearchTest'],
                                    capture_output=True, text=True, timeout=60)
            self._require_pass('PokemonFeedSearchTest', result)
            result = subprocess.run([str(runtime), '-cp', run_classpath, 'dev.pgrazor.DemoLiveFeedDataTest'],
                                    capture_output=True, text=True, timeout=60)
            self._require_pass('DemoLiveFeedDataTest', result)
            result = subprocess.run([str(runtime), '-cp', run_classpath, 'SnipeEngineTest'],
                                    capture_output=True, text=True, timeout=60)
            self._require_pass('SnipeEngineTest', result)
            self.assertIn('0 falha', result.stdout, result.stdout)
            result = subprocess.run([str(runtime), '-cp', run_classpath, 'dev.pgrazor.PokemonSelectorPolicyTest'],
                                    capture_output=True, text=True, timeout=60)
            self._require_pass('PokemonSelectorPolicyTest', result)


if __name__ == '__main__':
    unittest.main()
