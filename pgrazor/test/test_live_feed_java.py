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


class LiveFeedJavaTests(unittest.TestCase):
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
                            # LiveFeedClient depende de LiveFeedSession (Keystore/GCM). O arquivo REAL é
                            # compilado: ele só usa APIs do android.jar, então compila sem Android e não
                            # há dublê de segurança. Antes ele faltava e o CI quebrava com
                            # LiveFeedSession cannot be resolved (falha A).
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
            compile_args = [str(compiler), '-encoding', 'UTF-8']
            if android_jar is not None:
                compile_args += ['-classpath', str(android_jar)]
            compile_args += ['-d', str(work), *map(str, sources)]
            if android_jar is None:
                self.assertTrue(any(p.name == 'LiveFeedSession.java' for p in sources),
                                'a sessão real precisa estar na lista de fontes')
                self.skipTest('android.jar não encontrado: a sessão real (Keystore) não pode ser '
                              'compilada aqui. Instale o SDK para rodar este teste de verdade.')
            result = subprocess.run(compile_args, capture_output=True, text=True, timeout=60)
            self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
            for main in ['LiveFeedDataTest','SettingsBackupRulesTest','PogoFeedDataTest','PogoFeedFiltersTest'] + (['LiveFeedFiltersTest'] if (ROOT / 'test/LiveFeedFiltersTest.java').exists() else []):
                result = subprocess.run([str(runtime), '-cp', str(work), main], capture_output=True, text=True, timeout=30)
                self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
            result = subprocess.run([str(runtime), '-cp', str(work), 'dev.pgrazor.HundoFeedDataTest'],
                                    capture_output=True, text=True, timeout=30)
            self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
            result = subprocess.run([str(runtime), '-cp', str(work), 'dev.pgrazor.SnipeFiltersTest'],
                                    capture_output=True, text=True, timeout=30)
            self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
            result = subprocess.run([str(runtime), '-cp', str(work), 'dev.pgrazor.PokeListFeedDataTest'],
                                    capture_output=True, text=True, timeout=30)
            self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
            result = subprocess.run([str(runtime), '-cp', str(work), 'dev.pgrazor.DemoLiveFeedDataTest'],
                                    capture_output=True, text=True, timeout=30)
            self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
            result = subprocess.run([str(runtime), '-cp', str(work), 'dev.pgrazor.IFlowGoFeedDataTest'],
                                    capture_output=True, text=True, timeout=30)
            self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
            result = subprocess.run([str(runtime), '-cp', str(work), 'dev.pgrazor.RocketMapFeedDataTest'],
                                    capture_output=True, text=True, timeout=30)
            self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
            result = subprocess.run([str(runtime), '-cp', str(work), 'dev.pgrazor.PokemonFeedSearchTest'],
                                    capture_output=True, text=True, timeout=30)
            self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
            result = subprocess.run([str(runtime), '-cp', str(work), 'dev.pgrazor.DemoLiveFeedDataTest'],
                                    capture_output=True, text=True, timeout=30)
            self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
            result = subprocess.run([str(runtime), '-cp', str(work), 'SnipeEngineTest'],
                                    capture_output=True, text=True, timeout=30)
            self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
            self.assertIn('0 falha', result.stdout, result.stdout)
            result = subprocess.run([str(runtime), '-cp', str(work), 'dev.pgrazor.PokemonSelectorPolicyTest'],
                                    capture_output=True, text=True, timeout=30)
            self.assertEqual(result.returncode, 0, result.stdout + result.stderr)


if __name__ == '__main__':
    unittest.main()
