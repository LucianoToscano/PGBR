"""Run the canonical source model, filters, import, Snipers, presets, alerts and adapters on a JVM.

These are the device-free tests for the five-source integration: the same production classes the app
uses, compiled with Android's org.json implementation (from the installed SDK sources, with Android
annotation imports removed in a temporary copy). No network, no credentials, no captured account data.
Fixtures are synthetic and marked as such inside the tests.
"""
import os
from pathlib import Path
import re
import subprocess
import tempfile
import unittest

from test_control_java import java_tools

ROOT = Path(__file__).resolve().parents[1]

SOURCES = [
    'OriginKind.java', 'SourceEvent.java', 'SourceQuery.java', 'SourceCapabilities.java',
    'SourceRegistry.java', 'EventFilterEngine.java', 'LocalEventImport.java', 'LocalEventStore.java',
    'SourceSearchResult.java', 'SourceAdapter.java', 'SourceUnavailableException.java',
    'SourceAdapterSupport.java', 'PresetCodec.java', 'AlertEngine.java', 'SniperSearch.java',
    'PgSharpFilterMapping.java', 'LocalStoreAdapter.java', 'LocalSourceAdapters.java',
    'PgSharpHundoBridge.java', 'PgSharpSnapshotImport.java',
    'PoGoCoordinatesSourceAdapter.java', 'PGBRHundoSourceAdapter.java',
    'LiveFeedData.java', 'PokeListFeedData.java', 'IFlowGoFeedData.java', 'PogoFeedData.java',
    'HundoFeedData.java',
]

TESTS = [
    'SourceModelTest', 'EventFilterEngineTest', 'LocalEventImportTest', 'SniperSearchTest',
    'PresetCodecTest', 'PgSharpFilterMappingTest', 'SourceAdaptersTest', 'FeedSourceEventsTest',
    'PgSharpSnapshotImportTest',
]


class SourceModelJavaTests(unittest.TestCase):
    def test_canonical_sources_filters_import_snipers_presets_alerts(self):
        default_sdk = Path(os.environ.get('LOCALAPPDATA', str(Path.home()))) / 'Android/Sdk'
        sdk = Path(os.environ.get('ANDROID_HOME', str(default_sdk)))
        candidates = sorted((sdk / 'sources').glob('android-*/org/json'), reverse=True)
        if not candidates:
            self.skipTest('Install Android SDK sources to run the Android JSON parser on the JVM')
        compiler, runtime = java_tools()
        with tempfile.TemporaryDirectory(prefix='pgrazor-source-model-') as directory:
            work = Path(directory)
            sources = []
            for original in candidates[0].glob('*.java'):
                text = original.read_text(encoding='utf-8')
                text = re.sub(r'^import (?:static )?(?:android\.|libcore\.).*;\s*$', '', text, flags=re.M)
                text = re.sub(r'@(?:UnsupportedAppUsage|SystemApi|NonNull|Nullable)(?:\([^)]*\))?\s*', '', text)
                target = work / original.name
                target.write_text(text, encoding='utf-8')
                sources.append(target)
            sources += [ROOT / 'src/dev/pgrazor' / name for name in SOURCES]
            sources += [ROOT / 'test' / (name + '.java') for name in TESTS]
            result = subprocess.run([str(compiler), '-encoding', 'UTF-8', '-d', str(work),
                                     *map(str, sources)], capture_output=True, text=True, timeout=60)
            self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
            for main in TESTS:
                result = subprocess.run([str(runtime), '-cp', str(work), 'dev.pgrazor.' + main],
                                        capture_output=True, text=True, timeout=60)
                self.assertEqual(result.returncode, 0, main + '\n' + result.stdout + result.stderr)


if __name__ == '__main__':
    unittest.main()
