"""The shared Locais/Favoritos ordering policy compiled and run on a real JVM.

`SavedPlaceOrder` is the one place that decides which saved places a filter shows and in what
order. Both surfaces (World Map "Locais" and the quick "Favoritos" list) delegate here, so this
suite is what keeps them from disagreeing again.

The class itself reads no Android API, no network, no device location and no game state: the local
time zone, the distance and the cooldown estimate are injected by the test. `SavedPlaces.Entry` is
compiled with the narrowest possible Android/`org.json` shells (the same mirrors the other host
suites use) only because it lives beside its JSON-backed store; nothing simulates Android behaviour.
"""
import os
import subprocess
import tempfile
import unittest
from pathlib import Path

from org_json_support import STUB_JSON, sdk_json_sources
from test_control_java import java_tools

ROOT = Path(__file__).resolve().parents[1]

PREFS_STUB = '''package android.content;
public class Context { public Context getApplicationContext(){return this;} }'''

SHARED_PREFERENCES_STUB = '''package android.content;
public interface SharedPreferences {
 String getString(String key,String fallback);
 Editor edit();
 interface Editor { Editor putString(String key,String value); boolean commit(); }
}'''


class SavedPlaceOrderTests(unittest.TestCase):
    def _compile(self, work, sources):
        compiler, runtime = java_tools()
        stub_root = work / 'stubs'
        (stub_root / 'android' / 'content').mkdir(parents=True, exist_ok=True)
        (stub_root / 'android' / 'content' / 'Context.java').write_text(PREFS_STUB, encoding='utf-8')
        (stub_root / 'android' / 'content' / 'SharedPreferences.java').write_text(
            SHARED_PREFERENCES_STUB, encoding='utf-8')
        (stub_root / 'dev' / 'pgrazor').mkdir(parents=True, exist_ok=True)
        (stub_root / 'dev' / 'pgrazor' / 'AccountProfiles.java').write_text(
            'package dev.pgrazor;\nfinal class AccountProfiles {\n'
            ' static android.content.SharedPreferences preferences(android.content.Context c,String scope){return null;}\n}\n',
            encoding='utf-8')
        json_sources = sdk_json_sources(work)
        if not json_sources:
            json_sources = sorted(STUB_JSON.rglob('*.java'))
        all_sources = [*sorted(stub_root.rglob('*.java')), *json_sources, *sources]
        classes = work / 'classes'
        compiled = subprocess.run([str(compiler), '-encoding', 'UTF-8', '-d', str(classes), *map(str, all_sources)],
                                  capture_output=True, text=True, timeout=180)
        return runtime, classes, compiled

    def test_filter_and_ordering(self):
        with tempfile.TemporaryDirectory(prefix='pgrazor-saved-place-order-') as directory:
            work = Path(directory)
            runtime, classes, compiled = self._compile(work, [
                ROOT / 'src/dev/pgrazor/PtBr.java',
                ROOT / 'src/dev/pgrazor/SavedPlaceOrder.java',
                ROOT / 'src/dev/pgrazor/SavedPlaces.java',
                ROOT / 'test/SavedPlaceOrderTest.java'])
            self.assertEqual(compiled.returncode, 0, compiled.stdout + compiled.stderr)
            result = subprocess.run([str(runtime), '-cp', str(classes), 'SavedPlaceOrderTest'],
                                    capture_output=True, text=True, timeout=60)
            self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
            self.assertIn('PASS: saved-place filter and ordering', result.stdout)

    def test_both_surfaces_use_the_shared_policy(self):
        """A second, hand-rolled comparator in either dialog is the regression this pins."""
        library = (ROOT / 'src/dev/pgrazor/WorldMapDialog.java').read_text(encoding='utf-8')
        quick = (ROOT / 'src/dev/pgrazor/QuickLocationDialogs.java').read_text(encoding='utf-8')
        for name, text in (('WorldMapDialog.java', library), ('QuickLocationDialogs.java', quick)):
            with self.subTest(file=name):
                self.assertIn('SavedPlaceOrder.select(', text)
                self.assertIn('SavedPlaceOrder.Sort.', text)
        self.assertIn('orders[0]', library)

    def test_ordering_stays_pure_and_read_only(self):
        source = (ROOT / 'src/dev/pgrazor/SavedPlaceOrder.java').read_text(encoding='utf-8')
        for forbidden in ('import android.', 'Control.', 'System.currentTimeMillis', 'HttpURLConnection'):
            self.assertNotIn(forbidden, source, 'SavedPlaceOrder must stay free of %s' % forbidden)
        self.assertIn('Collections.emptyList()', source)


if __name__ == '__main__':
    unittest.main()
