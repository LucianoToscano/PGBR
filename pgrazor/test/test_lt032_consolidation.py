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
    def test_vgp_explicit_start_requires_confirmation(self):
        src=(ROOT/'pgrazor/src/dev/pgrazor/SettingsPanel.java').read_text(encoding='utf-8')
        self.assertIn('Exibir botão VGP+ (não inicia)',src)
        self.assertIn('vgpStartStopButton=launch',src)
        self.assertIn('Control.vgpTapProtection()',src)
        self.assertIn('Control.setVgpEnabled(true)',src)
        self.assertIn('Control.setVgpEnabled(false)',src)
        self.assertIn('Control.vgpDiagnosticStatus()',src)

    def test_latest_selector_and_ci_requirements(self):
        f=(ROOT/'tools/private_input.py').read_text(encoding='utf-8')
        self.assertIn('urllib.error.HTTPError',f)
        self.assertIn('Authorization',f)
        w=(ROOT/'.github/workflows/pgbr-lt032-consolidado-apk.yml').read_text(encoding='utf-8')
        self.assertIn('PGRAZOR_VERSION: v0.00.32-LT',w)
        self.assertIn('tools/check.py',w)
        self.assertIn('SHA256SUMS.txt',w)
        # LT032 workflow remains preserved as historical QA evidence, while a
        # real release may increment pgrazor/VERSION without invalidating history.
        current=(ROOT/'pgrazor/VERSION').read_text(encoding='utf-8').strip()
        self.assertIn(current,('0.00.32-LT','0.00.33-LT','0.00.34-LT','0.00.35-LT','0.00.36-LT','0.00.37-LT','0.00.38-LT','0.00.39-LT','0.00.40-LT','0.00.41-LT','0.00.42-LT','0.00.43-LT','0.00.44-LT','0.00.45-LT'))
        if current=='0.00.33-LT':
            new=(ROOT/'.github/workflows/pgbr-lt033-release.yml').read_text(encoding='utf-8')
            self.assertIn('PGRAZOR_VERSION: v0.00.33-LT',new)
            self.assertIn('tools/verify_release.py',new)
            self.assertIn('--tag v0.00.33-LT',new)
        if current in ('0.00.38-LT','0.00.39-LT','0.00.40-LT','0.00.41-LT','0.00.42-LT'):
            w=(ROOT/'.github/workflows/pgbr-lt038-release.yml').read_text(encoding='utf-8')
            self.assertIn('PGRAZOR_VERSION: v0.00.38-LT',w)
            self.assertIn('tools/verify_release.py',w)
            self.assertIn('--tag v0.00.38-LT',w)
        if current=='0.00.37-LT':
            new=(ROOT/'.github/workflows/pgbr-lt037-release.yml').read_text(encoding='utf-8')
            self.assertIn('PGRAZOR_VERSION: v0.00.37-LT',new)
            self.assertIn('tools/verify_release.py',new)
            self.assertIn('--tag v0.00.37-LT',new)
        if current=='0.00.36-LT':
            new=(ROOT/'.github/workflows/pgbr-lt036-release.yml').read_text(encoding='utf-8')
            self.assertIn('PGRAZOR_VERSION: v0.00.36-LT',new)
            self.assertIn('tools/verify_release.py',new)
            self.assertIn('--tag v0.00.36-LT',new)
        if current=='0.00.35-LT':
            new=(ROOT/'.github/workflows/pgbr-lt035-release.yml').read_text(encoding='utf-8')
            self.assertIn('PGRAZOR_VERSION: v0.00.35-LT',new)
            self.assertIn('tools/verify_release.py',new)
            self.assertIn('--tag v0.00.35-LT',new)
        if current=='0.00.34-LT':
            new=(ROOT/'.github/workflows/pgbr-lt034-release.yml').read_text(encoding='utf-8')
            self.assertIn('PGRAZOR_VERSION: v0.00.34-LT',new)
            self.assertIn('tools/verify_release.py',new)
            self.assertIn('--tag v0.00.34-LT',new)

if __name__=='__main__':unittest.main()
