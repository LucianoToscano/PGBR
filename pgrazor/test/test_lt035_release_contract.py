"""LT035 prerelease contract, preserving both earlier release workflows and tags."""
from pathlib import Path
import unittest

ROOT=Path(__file__).resolve().parents[2]
class Lt035ReleaseContract(unittest.TestCase):
    def test_version_workflow_and_notes(self):
        version=(ROOT/'pgrazor/VERSION').read_text(encoding='utf-8').strip()
        self.assertIn(version,('0.00.35-LT','0.00.36-LT','0.00.37-LT','0.00.38-LT','0.00.39-LT','0.00.40-LT','0.00.41-LT','0.00.42-LT','0.00.43-LT','0.00.44-LT','0.00.45-LT','0.00.46-LT'))
        workflow=(ROOT/'.github/workflows/pgbr-lt035-release.yml').read_text(encoding='utf-8')
        for item in ('PGRAZOR_VERSION: v0.00.35-LT','--tag v0.00.35-LT',
                     'tools/check.py','pgrazor/nonroot.py','pgrazor/rooted.py',
                     'tools/verify_release.py','PGRAZOR_KEYSTORE_BASE64',
                     "github.ref == 'refs/heads/main'",'--prerelease',
                     'PGBR-v0.00.35-LT.apk','PGBR-v0.00.35-LT-Root.apk',
                     'PGBR-v0.00.35-LT-Root-module.zip','SHA256SUMS.txt'):
            with self.subTest(item=item):self.assertIn(item,workflow)
        self.assertIn('branches: [main]',workflow)
        for old in ('33','34'):
            previous=(ROOT/f'.github/workflows/pgbr-lt0{old}-release.yml').read_text(encoding='utf-8')
            self.assertIn(f'PGRAZOR_VERSION: v0.00.{old}-LT',previous)
            # Published historical workflows stay dispatchable, but a later
            # VERSION bump must not rebuild an immutable old tag.
            self.assertNotIn("'pgrazor/VERSION'",previous)
            self.assertNotIn(f"'.github/workflows/pgbr-lt0{old}-release.yml'",previous)
        notes=(ROOT/'docs/releases/v0.00.35-LT.md').read_text(encoding='utf-8')
        self.assertIn('pré-release',notes)
        self.assertIn('device_tested=false',notes)
        self.assertIn('não conectada',notes)
        master=(ROOT/'PGBR_ARQUIVO_MESTRE_UNICO.md').read_text(encoding='utf-8')
        self.assertIn('LT035',master)
    def test_pvp_fails_closed(self):
        scene=(ROOT/'pgrazor/native/pvp_scene.h').read_text(encoding='utf-8')
        host=(ROOT/'pgrazor/native/combat_host.cpp').read_text(encoding='utf-8')
        self.assertIn('MAX_COMBAT_MS=1200000',scene)
        self.assertIn('now-started_ms>=MAX_COMBAT_MS',scene)
        self.assertIn('now<last_vnext_ms',scene)
        self.assertIn('g_pvp_scene.stage!=PgrPvpScene::COMBAT',host)
if __name__=='__main__':unittest.main()
