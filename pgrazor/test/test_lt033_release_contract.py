"""Fail-closed documentation/version contract for the signed LT033 QA prerelease."""
from pathlib import Path
import re
import unittest

ROOT=Path(__file__).resolve().parents[2]

class Lt033ReleaseContract(unittest.TestCase):
    def test_version_manifest_release_and_master_agree(self):
        v=(ROOT/'pgrazor/VERSION').read_text(encoding='utf-8').strip()
        self.assertIn(v,('0.00.33-LT','0.00.34-LT','0.00.35-LT','0.00.36-LT','0.00.37-LT','0.00.38-LT','0.00.39-LT','0.00.40-LT','0.00.41-LT','0.00.42-LT','0.00.43-LT','0.00.44-LT','0.00.45-LT'))
        wf=(ROOT/'.github/workflows/pgbr-lt033-release.yml').read_text(encoding='utf-8')
        self.assertIn('PGRAZOR_VERSION: v0.00.33-LT',wf)
        self.assertIn('--tag v0.00.33-LT',wf)
        self.assertIn('dist/pgbr-lt033',wf)
        self.assertIn("github.ref == 'refs/heads/main'",wf)
        self.assertIn('--prerelease',wf)
        self.assertIn('tools/verify_release.py',wf)
        for asset in ('PGBR-v0.00.33-LT.apk','PGBR-v0.00.33-LT-Root.apk',
                      'PGBR-v0.00.33-LT-Root-module.zip','SHA256SUMS.txt'):
            self.assertIn(asset,wf)
        master=(ROOT/'PGBR_ARQUIVO_MESTRE_UNICO.md').read_text(encoding='utf-8')
        self.assertIn('REGRA PERMANENTE DO USUÁRIO',master)
        self.assertIn('v0.00.33-LT',master)
        agents=(ROOT/'AGENTS.md').read_text(encoding='utf-8')
        self.assertIn('REGRA PERMANENTE',agents)
        contributing=(ROOT/'CONTRIBUTING.md').read_text(encoding='utf-8')
        self.assertIn('Regra obrigatória de registro no Mestre',contributing)
        notes=(ROOT/'docs/releases/v0.00.33-LT.md').read_text(encoding='utf-8')
        self.assertIn('pré-release',notes)
        self.assertIn('sem homologação em aparelho físico',notes)
        self.assertEqual(len(re.findall(r'^- \[.*\]\(https://github.com/LucianoToscano/PGBR/releases/download/v0.00.33-LT/',notes,flags=re.M)),4)

if __name__=='__main__': unittest.main()
