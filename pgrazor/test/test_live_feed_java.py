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

    @staticmethod
    def _android_org_json_reusable(result):
        """True quando a falha foi só do org.json do SDK (ambiente), não do código do PGBR.

        Diferencia defeito real de dependência indisponível: se algum arquivo do PGBR falhou,
        isso NÃO é ambiente e o teste precisa falhar.
        """
        text = (result.stdout or '') + (result.stderr or '')
        errors = [line for line in text.splitlines() if '. ERROR in ' in line]
        return bool(errors) and all('org/json' in line for line in errors)

    def test_feed_data(self):
        sdk = Path(os.environ.get('ANDROID_HOME', str(Path(os.environ.get('LOCALAPPDATA', str(Path.home()))) / 'Android/Sdk')))
        candidates = sorted((sdk / 'sources').glob('android-*/org/json'), reverse=True)
        if not candidates:
            self.skipTest('Install Android SDK sources to run the Android JSON parser on the JVM')
        compiler, runtime = java_tools()
        with tempfile.TemporaryDirectory(prefix='pgrazor-feed-test-') as directory:
            work = Path(directory)
            sources = []
            for original in candidates[0].glob('*.java'):
                text = original.read_text(encoding='utf-8')
                text = re.sub(r'^import (?:static )?(?:android\.|libcore\.).*;\s*$', '', text, flags=re.M)
                text = re.sub(r'@(?:UnsupportedAppUsage|SystemApi|NonNull|Nullable)(?:\([^)]*\))?\s*', '', text)
                target = work / original.name
                target.write_text(text, encoding='utf-8')
                sources.append(target)
            sources += [ROOT / 'src/dev/pgrazor/LiveFeedData.java', ROOT / 'src/dev/pgrazor/LiveFeedFilters.java', ROOT / 'src/dev/pgrazor/MapTapSequence.java', ROOT / 'src/dev/pgrazor/BerryPriority.java', ROOT / 'src/dev/pgrazor/SettingsBackupRules.java', ROOT / 'src/dev/pgrazor/AutoRaidPreferences.java', ROOT / 'src/dev/pgrazor/BuddyEmotionPreferences.java', ROOT / 'test/LiveFeedDataTest.java', ROOT / 'test/SettingsBackupRulesTest.java',
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
            # android.jar (SDK) fornece Context/SharedPreferences/Keystore/Base64 usados por
            # LiveFeedSession. Sem ele, compilar a sessão real é impossível — e dublê de segurança
            # não é opção. Ausente => teste inconclusivo, nunca verde.
            android_jar = None
            for platform in ('android-36', 'android-36.1', 'android-35', 'android-34'):
                candidate = sdk / 'platforms' / platform / 'android.jar'
                if candidate.is_file():
                    android_jar = candidate
                    break
            # A sessão real (Keystore/GCM) usa APIs do Android. Quando o SDK está presente, o
            # android.jar é a autoridade. Quando não está, os espelhos de API em
            # tools/sniper_jvm/stubs/android entram como fontes — é o mesmo mecanismo que o
            # test_stub_signature_parity.py vigia, e nenhum dublê substitui a lógica de segurança:
            # só as assinaturas do framework. O que está sob teste (parser, cliente HTTP, sessão)
            # é sempre o arquivo real.
            compile_sources = list(sources)
            compile_args = [str(compiler), '-encoding', 'UTF-8']
            if android_jar is not None:
                compile_args += ['-classpath', str(android_jar)]
            else:
                compile_sources = sorted((REPO / 'tools/sniper_jvm/stubs/android').rglob('*.java')) \
                    + compile_sources
            compile_args += ['-d', str(work), *map(str, compile_sources)]
            result = subprocess.run(compile_args, capture_output=True, text=True, timeout=120)
            if result.returncode != 0:
                # Diagnóstico explícito no log do CI: quais arquivos e por quê.
                print("DIAGNOSTICO: compilacao falhou (rc=%d, android_jar=%s)"
                      % (result.returncode, android_jar))
                for line in ((result.stdout or '') + (result.stderr or '')).splitlines():
                    print("DIAG " + line)
            if result.returncode != 0 and not self._android_org_json_reusable(result):
                self.fail('o parser real não compilou.\n' + result.stdout + result.stderr)
            if result.returncode != 0:
                # As fontes do SDK não compilaram neste JDK (isso já acontecia antes destas
                # mudanças). Cai para a implementação org.json versionada, que segue a mesma API,
                # e registra o fato: o teste continua exercitando o parser real.
                stub_json = sorted((REPO / 'tools/sniper_jvm/stubs/org/json').glob('*.java'))
                self.assertTrue(stub_json, 'sem org.json utilizável: o parser não pode ser testado')
                fallback = [str(compiler), '-encoding', 'UTF-8', '-d', str(work)]
                if android_jar is not None:
                    fallback += ['-classpath', str(android_jar)]
                fallback += [*map(str, stub_json),
                             *map(str, [p for p in compile_sources if 'org/json' not in p.as_posix()])]
                retry = subprocess.run(fallback, capture_output=True, text=True, timeout=120)
                self.assertEqual(retry.returncode, 0,
                                 'nem o org.json versionado compilou:\n' + retry.stdout + retry.stderr)
                self._uses_vendored_json = True
            mains = ['LiveFeedDataTest','SettingsBackupRulesTest','PogoFeedDataTest','PogoFeedFiltersTest'] \
                + (['LiveFeedFiltersTest'] if (ROOT / 'test/LiveFeedFiltersTest.java').exists() else [])
            for main in mains:
                result = subprocess.run([str(runtime), '-cp', str(work), main], capture_output=True, text=True, timeout=30)
                self._require_pass(main, result)
            result = subprocess.run([str(runtime), '-cp', str(work), 'dev.pgrazor.HundoFeedDataTest'],
                                    capture_output=True, text=True, timeout=60)
            self._require_pass('HundoFeedDataTest', result)
            result = subprocess.run([str(runtime), '-cp', str(work), 'dev.pgrazor.SnipeFiltersTest'],
                                    capture_output=True, text=True, timeout=60)
            self._require_pass('SnipeFiltersTest', result)
            result = subprocess.run([str(runtime), '-cp', str(work), 'dev.pgrazor.PokeListFeedDataTest'],
                                    capture_output=True, text=True, timeout=60)
            self._require_pass('PokeListFeedDataTest', result)
            result = subprocess.run([str(runtime), '-cp', str(work), 'dev.pgrazor.DemoLiveFeedDataTest'],
                                    capture_output=True, text=True, timeout=60)
            self._require_pass('DemoLiveFeedDataTest', result)
            result = subprocess.run([str(runtime), '-cp', str(work), 'dev.pgrazor.IFlowGoFeedDataTest'],
                                    capture_output=True, text=True, timeout=60)
            self._require_pass('IFlowGoFeedDataTest', result)
            result = subprocess.run([str(runtime), '-cp', str(work), 'dev.pgrazor.RocketMapFeedDataTest'],
                                    capture_output=True, text=True, timeout=60)
            self._require_pass('RocketMapFeedDataTest', result)
            result = subprocess.run([str(runtime), '-cp', str(work), 'dev.pgrazor.PokemonFeedSearchTest'],
                                    capture_output=True, text=True, timeout=60)
            self._require_pass('PokemonFeedSearchTest', result)
            result = subprocess.run([str(runtime), '-cp', str(work), 'dev.pgrazor.DemoLiveFeedDataTest'],
                                    capture_output=True, text=True, timeout=60)
            self._require_pass('DemoLiveFeedDataTest', result)
            result = subprocess.run([str(runtime), '-cp', str(work), 'SnipeEngineTest'],
                                    capture_output=True, text=True, timeout=60)
            self._require_pass('SnipeEngineTest', result)
            self.assertIn('0 falha', result.stdout, result.stdout)
            result = subprocess.run([str(runtime), '-cp', str(work), 'dev.pgrazor.PokemonSelectorPolicyTest'],
                                    capture_output=True, text=True, timeout=60)
            self._require_pass('PokemonSelectorPolicyTest', result)


if __name__ == '__main__':
    unittest.main()
