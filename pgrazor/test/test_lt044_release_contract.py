"""LT044 release candidate: immutable LT043 and safety-first PT-BR/GPS QA."""
from pathlib import Path
import unittest

ROOT=Path(__file__).resolve().parents[2]

class Lt044ReleaseContract(unittest.TestCase):
    def test_version_assets_and_workflow(self):
        self.assertIn((ROOT/'pgrazor/VERSION').read_text(encoding='utf-8').strip(),('0.00.44-LT',))
        workflow=(ROOT/'.github/workflows/pgbr-lt044-release.yml').read_text(encoding='utf-8')
        for term in ('PGRAZOR_VERSION: v0.00.44-LT', 'PGBR-v0.00.44-LT.apk',
                     'PGBR-v0.00.44-LT-Root.apk','PGBR-v0.00.44-LT-Root-module.zip',
                     'SHA256SUMS.txt','tools/verify_release.py','--prerelease','--latest=false',
                     'gh release create','if: github.ref == \'refs/heads/main\'',
                     'pgrazor/nonroot.py','pgrazor/rooted.py'):
            self.assertIn(term, workflow)
        self.assertNotIn('PGBR-v0.00.43-LT.apk',workflow)
        for frozen in ('pgbr-lt043-release.yml','pgbr-lt043-pgsharp-qa.yml'):
            previous=(ROOT/'.github/workflows'/frozen).read_text(encoding='utf-8')
            self.assertIn('workflow_dispatch:',previous)
            self.assertNotIn('  push:',previous)
            self.assertNotIn('  pull_request:',previous)
        for rel in ('README.md','CHANGELOG.md','PGBR_ARQUIVO_MESTRE_UNICO.md','docs/releases/v0.00.44-LT.md'):
            self.assertTrue((ROOT/rel).is_file())
            self.assertIn('LT044',(ROOT/rel).read_text(encoding='utf-8'))
        self.assertIn('device_tested=false',(ROOT/'docs/releases/v0.00.44-LT.md').read_text(encoding='utf-8'))

if __name__=='__main__': unittest.main()
