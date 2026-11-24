"""LT045 release candidate: immutable LT044 and consolidated post-LT044 QA."""
from pathlib import Path
import unittest

ROOT=Path(__file__).resolve().parents[2]

class Lt045ReleaseContract(unittest.TestCase):
    def test_version_assets_and_workflow(self):
        self.assertIn((ROOT/'pgrazor/VERSION').read_text(encoding='utf-8').strip(),('0.00.45-LT','0.00.46-LT','0.00.47-LT',))
        workflow=(ROOT/'.github/workflows/pgbr-lt045-release.yml').read_text(encoding='utf-8')
        for term in ('PGRAZOR_VERSION: v0.00.45-LT', 'PGBR-v0.00.45-LT.apk',
                     'PGBR-v0.00.45-LT-Root.apk','PGBR-v0.00.45-LT-Root-module.zip',
                     'SHA256SUMS.txt','tools/verify_release.py','--prerelease','--latest=false',
                     'gh release create','if: github.ref == \'refs/heads/main\'',
                     'pgrazor/nonroot.py','pgrazor/rooted.py'):
            self.assertIn(term, workflow)
        self.assertNotIn('PGBR-v0.00.44-LT.apk',workflow)
        # User hold: no automatic APK build or release during implementation.
        for name in ('pgbr-lt045-qa.yml','pgbr-lt045-release.yml'):
            held=(ROOT/'.github/workflows'/name).read_text(encoding='utf-8')
            self.assertIn('workflow_dispatch:',held)
            self.assertNotIn('  push:',held)
            self.assertNotIn('  pull_request:',held)
        delivery=(ROOT/'.github/workflows/pgbr-lt045-delivery-163.yml').read_text(encoding='utf-8')
        self.assertIn('workflow_call:',workflow)
        for term in ('branches: [main]', 'pgbr-lt045-delivery-163.yml', 'pulls/163',
                     'pr["merged"]', 'pr["merge_commit_sha"]==sha', 'parents==[base,head]',
                     'set(latest)==required', 'r["conclusion"]=="success"',
                     'needs: approved-merge', 'uses: ./.github/workflows/pgbr-lt045-release.yml'):
            self.assertIn(term,delivery)
        self.assertNotIn('  pull_request:',delivery)
        for frozen in ('pgbr-lt044-release.yml','pgbr-lt044-qa.yml'):
            previous=(ROOT/'.github/workflows'/frozen).read_text(encoding='utf-8')
            self.assertIn('workflow_dispatch:',previous)
            self.assertNotIn('  push:',previous)
            self.assertNotIn('  pull_request:',previous)
        for rel in ('README.md','CHANGELOG.md','PGBR_ARQUIVO_MESTRE_UNICO.md','docs/releases/v0.00.45-LT.md'):
            self.assertTrue((ROOT/rel).is_file())
            self.assertIn('LT045',(ROOT/rel).read_text(encoding='utf-8'))
        self.assertIn('device_tested=false',(ROOT/'docs/releases/v0.00.45-LT.md').read_text(encoding='utf-8'))

if __name__=='__main__': unittest.main()
