"""Session stats rolling history: hourly and 24-hour changes, storage and resets."""
from pathlib import Path
import subprocess
import tempfile
import unittest
from test_control_java import java_tools

ROOT = Path(__file__).resolve().parents[1]

class SessionStatsHistoryJavaTests(unittest.TestCase):
    def test_rolling_changes(self):
        compiler, runtime = java_tools()
        with tempfile.TemporaryDirectory(prefix='pgrazor-session-stats-') as work:
            sources = [ROOT / 'src/dev/pgrazor/SessionStatsHistory.java', ROOT / 'test/SessionStatsHistoryTest.java']
            result = subprocess.run([str(compiler), '-encoding', 'UTF-8', '-d', work, *map(str, sources)], capture_output=True, text=True, timeout=30)
            self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
            result = subprocess.run([str(runtime), '-cp', work, 'dev.pgrazor.SessionStatsHistoryTest'], capture_output=True, text=True, timeout=30)
            self.assertEqual(result.returncode, 0, result.stdout + result.stderr)

    def test_profile_statistics_privacy_contract(self):
        source = (ROOT / 'src/dev/pgrazor/SessionStats.java').read_text(encoding='utf-8')
        deletion = (ROOT / 'src/dev/pgrazor/AccountSessions.java').read_text(encoding='utf-8')
        for fragment in (
            'catch (ClassCastException incompatible)',
            'writableHistory = false;',
            'history.load(stored, now);',
            'writableHistory = store != null && !current.isEmpty() && !"unknown".equals(current);',
            'if (store == null || !writableHistory || !dirty || account.isEmpty()) return;',
            'if (detail == null || detail.length() > 4096) return;',
            'now < savedAt || now - savedAt >= SAVE_MS',
            'id.equals(AccountProfiles.currentId()) || id.equals(account)',
            'prefs.edit().remove(id).commit()',
            '!AccountProfiles.allowed()',
        ):
            self.assertIn(fragment, source)
        self.assertEqual(source.count('.putString(account, history.save())'), 1)
        self.assertNotIn('prefs.edit().clear()', source)
        self.assertLess(deletion.index('profiles.requireRemovable(id)'), deletion.index('SessionStats.deleteInactiveProfile(context,id)'))
        self.assertLess(deletion.index('SessionStats.deleteInactiveProfile(context,id)'), deletion.index('profiles.remove(id)'))
