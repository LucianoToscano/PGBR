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
if __name__=='__main__': unittest.main()
