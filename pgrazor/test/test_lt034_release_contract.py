"""Fail-closed LT034 prerelease contract; keep historical LT033 unmodified."""
from pathlib import Path
import re
import unittest
ROOT = Path(__file__).resolve().parents[2]
class Lt034ReleaseContract(unittest.TestCase):
    def test_version_and_complete_release_workflow(self):
        version = (ROOT/'pgrazor/VERSION').read_text(encoding='utf-8').strip()
        self.assertIn(version, ('0.00.34-LT','0.00.35-LT','0.00.36-LT','0.00.37-LT','0.00.38-LT','0.00.39-LT','0.00.40-LT','0.00.41-LT','0.00.42-LT','0.00.43-LT','0.00.44-LT','0.00.45-LT','0.00.46-LT'))
        workflow=(ROOT/'.github/workflows/pgbr-lt034-release.yml').read_text(encoding='utf-8')
        for snippet in (
            'PGRAZOR_VERSION: v0.00.34-LT',
            'tools/verify_release.py',
            '--tag v0.00.34-LT',
            'tools/check.py',
            'pgrazor/nonroot.py',
            'pgrazor/rooted.py',
            'PGRAZOR_KEYSTORE_BASE64',
            "github.ref == 'refs/heads/main'",
            '--prerelease',
            'PGBR-v0.00.34-LT.apk',
            'PGBR-v0.00.34-LT-Root.apk',
            'PGBR-v0.00.34-LT-Root-module.zip',
            'SHA256SUMS.txt',
        ):
            with self.subTest(snippet=snippet):
                self.assertIn(snippet, workflow)
        self.assertNotIn('release/pgbr-v0.00.33-lt-20261122',workflow)
        # New workflow must not alter the old published LT033 tag or assets.
        old=(ROOT/'.github/workflows/pgbr-lt033-release.yml').read_text(encoding='utf-8')
        self.assertIn('PGRAZOR_VERSION: v0.00.33-LT',old)
        self.assertIn('PGBR-v0.00.33-LT.apk',old)
    def test_release_notes_master_and_ui_scope(self):
        notes=(ROOT/'docs/releases/v0.00.34-LT.md').read_text(encoding='utf-8')
        self.assertIn('pré-release', notes)
        self.assertIn('sem homologação em aparelho físico', notes)
        self.assertIn('não consulta o servidor PGSharp', notes)
        for name in ('PGBR-v0.00.34-LT.apk',
                     'PGBR-v0.00.34-LT-Root.apk',
                     'PGBR-v0.00.34-LT-Root-module.zip'):
            self.assertIn(name, notes)
        master=(ROOT/'PGBR_ARQUIVO_MESTRE_UNICO.md').read_text(encoding='utf-8')
        self.assertIn('LT034',master)
        self.assertIn('PR #72',master)
        self.assertIn('PR #73',master)
        menu=(ROOT/'pgrazor/src/dev/pgrazor/FeedChooser.java').read_text(encoding='utf-8')
        self.assertIn('HundoFeedDialog.show(activity)',menu)
        self.assertIn('PgSharpFeedDialog.show(activity)',menu)
        policy=(ROOT/'pgrazor/src/dev/pgrazor/PgSharpFeedPolicy.java').read_text(encoding='utf-8')
        self.assertIn('Connector.UNVERIFIED',policy)
if __name__=='__main__': unittest.main()
