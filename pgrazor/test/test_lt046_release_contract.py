"""LT046 build candidate: QA compile only, publication explicitly out of scope."""
from pathlib import Path
import unittest

ROOT=Path(__file__).resolve().parents[2]

class Lt046ReleaseContract(unittest.TestCase):
    def test_version_qa_workflow_and_notes(self):
        self.assertIn((ROOT/'pgrazor/VERSION').read_text(encoding='utf-8').strip(),('0.00.46-LT','0.00.47-LT','0.00.48-LT',))
        qa=(ROOT/'.github/workflows/pgbr-lt046-qa.yml').read_text(encoding='utf-8')
        for term in ('PGRAZOR_VERSION: v0.00.46-LT', 'PGBR-v0.00.46-LT.apk',
                     'PGBR-v0.00.46-LT-Root.apk','PGBR-v0.00.46-LT-Root-module.zip',
                     'SHA256SUMS.txt','tools/verify_release.py',
                     'pgrazor/nonroot.py','pgrazor/rooted.py',
                     'pgrazor/test/test_lt046_release_contract.py'):
            self.assertIn(term, qa)
        # QA compile must never publish: manual dispatch only, no push/PR trigger.
        self.assertIn('workflow_dispatch:',qa)
        self.assertNotIn('  push:',qa)
        self.assertNotIn('  pull_request:',qa)
        # Publication belongs to a separate, explicitly authorized delivery; the QA
        # workflow must not create tags, releases or upload to a release.
        for forbidden in ('gh release create','--prerelease','--latest=false',
                          'gh release upload','actions/upload-release-asset'):
            self.assertNotIn(forbidden, qa)
        # The QA workflow must not bundle the isolated GeoLab research subproject.
        self.assertNotIn('geolab',qa.lower())
        # LT045 remains the published reference; its files stay immutable.
        for name in ('pgbr-lt045-qa.yml','pgbr-lt045-release.yml'):
            held=(ROOT/'.github/workflows'/name).read_text(encoding='utf-8')
            self.assertIn('workflow_dispatch:',held)
            self.assertNotIn('  push:',held)
            self.assertNotIn('  pull_request:',held)
        for rel in ('README.md','CHANGELOG.md','PGBR_ARQUIVO_MESTRE_UNICO.md','docs/releases/v0.00.46-LT.md'):
            self.assertTrue((ROOT/rel).is_file())
            self.assertIn('LT046',(ROOT/rel).read_text(encoding='utf-8'))
        self.assertIn('device_tested=false',(ROOT/'docs/releases/v0.00.46-LT.md').read_text(encoding='utf-8'))

if __name__=='__main__': unittest.main()
