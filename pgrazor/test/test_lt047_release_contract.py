"""LT047 identity: retain historical contracts and require explicit QA naming."""
from pathlib import Path
import unittest
ROOT=Path(__file__).resolve().parents[2]
class Lt047ReleaseContract(unittest.TestCase):
    def test_identity(self):
        self.assertEqual((ROOT/'pgrazor/VERSION').read_text(encoding='utf-8').strip(),'0.00.47-LT')
        self.assertIn('PGBR-v0.00.47-LT.apk',(ROOT/'README.md').read_text(encoding='utf-8'))
        self.assertIn('device_tested=false',(ROOT/'docs/releases/v0.00.47-LT.md').read_text(encoding='utf-8'))
        self.assertIn('LT047',(ROOT/'CHANGELOG.md').read_text(encoding='utf-8'))
    def test_qa_workflow(self):
        qa=(ROOT/'.github/workflows/pgbr-lt047-qa.yml').read_text(encoding='utf-8')
        for term in ('workflow_dispatch:', 'PGRAZOR_VERSION: v0.00.47-LT', 'PGBR-v0.00.47-LT.apk', 'PGBR-v0.00.47-LT-Root.apk', 'PGBR-v0.00.47-LT-Root-module.zip', 'SHA256SUMS.txt', 'tools/verify_release.py'):
            self.assertIn(term,qa)
        for forbidden in ('  push:', '  pull_request:', 'gh release create', 'gh release upload'):
            self.assertNotIn(forbidden,qa)
if __name__=='__main__': unittest.main()
