"""LT032 consolidation guard: structural checks, NOT device functionality proofs."""
from pathlib import Path
import unittest
ROOT=Path(__file__).resolve().parents[2]

class ConsolidationIntegrityTest(unittest.TestCase):
    def test_shared_features_present(self):
        java=ROOT/'pgrazor'/'src'/'dev'/'pgrazor'
        for name in ('VgpController.java','PvpBattleController.java','PvpChargedPreference.java','SharedBattleOptions.java','PokemonSelectorDialog.java','IFlowGoFeedClient.java','RocketMapFeedClient.java','LiveFeedDialog.java'):
            with self.subTest(file=name):
                self.assertTrue((java/name).is_file(),name)
    def test_runtime_scopes_and_privacy_gate(self):
        native=ROOT/'pgrazor'/'native'
        self.assertTrue((native/'pvp_scene.h').is_file())
        self.assertTrue((native/'pvp_probe.inc').is_file())
        control=(ROOT/'pgrazor/src/dev/pgrazor/Control.java').read_text(encoding='utf-8')
        self.assertIn('vgpDiagnosticStatus()',control)
        self.assertIn('pvpBattleOptions()',control)
        docs=(ROOT/'docs/PRIVACIDADE_STREAMER_MOD_APURACAO.md').read_text(encoding='utf-8')
        self.assertIn('experimental',docs.lower())
    def test_latest_selector_and_ci_requirements(self):
        f=(ROOT/'tools/private_input.py').read_text(encoding='utf-8')
        self.assertIn('urllib.error.HTTPError',f)
        self.assertIn('Authorization',f)
        w=(ROOT/'.github/workflows/pgbr-lt032-consolidado-apk.yml').read_text(encoding='utf-8')
        self.assertIn('PGRAZOR_VERSION: v0.00.32-LT',w)
        self.assertIn('tools/check.py',w)
        self.assertIn('SHA256SUMS.txt',w)
        self.assertEqual((ROOT/'pgrazor/VERSION').read_text(encoding='utf-8').strip(),'0.00.32-LT')

if __name__=='__main__':unittest.main()
