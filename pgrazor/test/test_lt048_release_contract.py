"""LT048 candidate identity and incremental QA."""
from pathlib import Path
import unittest
ROOT=Path(__file__).resolve().parents[2]
class Lt048ReleaseContract(unittest.TestCase):
    def test_identity(self):
        self.assertEqual((ROOT/'pgrazor/VERSION').read_text(encoding='utf-8').strip(),'0.00.48-LT')
        self.assertIn('PGBR-v0.00.48-LT.apk',(ROOT/'README.md').read_text(encoding='utf-8'))
        self.assertIn('LT048',(ROOT/'CHANGELOG.md').read_text(encoding='utf-8'))
        notes=(ROOT/'docs/releases/v0.00.48-LT.md').read_text(encoding='utf-8')
        for term in ('#207','#208','#209','#210','#213','45482026778','device_tested=false','Não há artefato LT048 confirmado'):
            self.assertIn(term,notes)
    def test_matrix(self):
        matrix=(ROOT/'docs/validation/LT048_HOMOLOGACAO.md').read_text(encoding='utf-8')
        for case in range(1,11):
            self.assertIn(f'QA48-{case:02d}',matrix)
        self.assertIn('device_tested=false',matrix)
    def test_qa_workflow_builds_the_three_lt048_variants(self):
        text=(ROOT/'.github/workflows/pgbr-lt048-qa.yml').read_text(encoding='utf-8')
        for term in ('workflow_dispatch:','PGRAZOR_VERSION: v0.00.48-LT','PGBR-v0.00.48-LT.apk',
                     'PGBR-v0.00.48-LT-Root.apk','PGBR-v0.00.48-LT-Root-module.zip','SHA256SUMS.txt',
                     'tools/verify_release.py'):
            self.assertIn(term,text)
    def test_qa_workflow_never_publishes(self):
        text=(ROOT/'.github/workflows/pgbr-lt048-qa.yml').read_text(encoding='utf-8')
        for forbidden in ('  push:','  pull_request:','gh release create','gh release upload'):
            self.assertNotIn(forbidden,text)
    def test_physical_roteiro_keeps_device_gate_open(self):
        roteiro=(ROOT/'docs/validation/LT048_P0_TESTE_FISICO_20261125.md').read_text(encoding='utf-8')
        self.assertIn('device_tested=false',roteiro)
        for term in ('#192','#105','#90','Boa','Ótima','Excelente'):
            self.assertIn(term,roteiro)
if __name__=='__main__': unittest.main()
