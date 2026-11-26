"""Keep the new candidate separate from earlier releases and game profiles."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[2]

class Lt049QaContract(unittest.TestCase):
    def test_qa_only_after_manual_dispatch_or_profile_selection_on_main(self):
        workflow = (ROOT / ".github/workflows/pgbr-lt049-qa.yml").read_text(encoding="utf-8")
        for term in ("workflow_dispatch:", "  push:", "branches: [main]", "\u0027pgrazor/native/schema/game/current.json\u0027", "needs: approved-profile", "PGRAZOR_VERSION: v0.00.49-LT",
                     "github.ref == 'refs/heads/main'", "tools/lt049_pinned_input.py --check-profile",
                     "PGBR-v0.00.49-LT.apk", "PGBR-v0.00.49-LT-Root.apk",
                     "PGBR-v0.00.49-LT-Root-module.zip", "SHA256SUMS.txt",
                     "tools/verify_release.py dist/pgbr-lt049 --tag v0.00.49-LT",
                     "tools/lt049_pinned_input.py", "tools/check.py", "--apksigner"):
            self.assertIn(term, workflow)
        for term in ("  pull_request:", "  pull_request_target:", "gh release create", "gh release upload"):
            self.assertNotIn(term, workflow)

    def test_r2_artifact_does_not_replace_original_qa(self):
        workflow = (ROOT / ".github/workflows/pgbr-lt049-qa.yml").read_text(encoding="utf-8")
        self.assertIn("name: PGBR-LT049-R2-Root-e-SemRoot-QA", workflow)
        self.assertIn("PGRAZOR_VERSION: v0.00.49-LT", workflow)
        self.assertNotIn("gh release upload", workflow)

    def test_historical_recipe_preserved(self):
        old = (ROOT / ".github/workflows/pgbr-lt048-qa.yml").read_text(encoding="utf-8")
        self.assertIn("PGRAZOR_VERSION: v0.00.48-LT", old)
        self.assertIn("PGBR-LT048-Root-e-SemRoot-QA", old)
        notes = (ROOT / "docs/releases/v0.00.49-LT.md").read_text(encoding="utf-8")
        for term in ("0.437.0", "0.439.0", "device_tested=false", "não compilado", "não publicada"):
            self.assertIn(term, notes)

    def test_no_fallback_to_old_game_input(self):
        loader=(ROOT / "tools/lt049_pinned_input.py").read_text(encoding="utf-8")
        workflow=(ROOT / ".github/workflows/pgbr-lt049-qa.yml").read_text(encoding="utf-8")
        self.assertIn("Pokemon_GO_0.439.0_ARM64_ORIGINAL.apkm",loader)
        self.assertIn("profile = check_profile()",loader)
        self.assertIn("profile.get(key) != value",loader)
        self.assertIn("is_draft",loader)
        self.assertNotIn("Pokemon_GO_0.437.0_ARM64_ORIGINAL.apkm",loader)
        self.assertNotIn("tools/lt028_pinned_input.py",workflow)
        self.assertLess(workflow.index("--check-profile"),workflow.index("python -B tools/lt049_pinned_input.py\n"))
        self.assertLess(workflow.index("--apksigner"),workflow.index("run: python -B pgrazor/nonroot.py"))

    def test_signed_manifest_gate_before_build(self):
        workflow = (ROOT / '.github/workflows/pgbr-lt049-qa.yml').read_text(encoding='utf-8')
        gate = 'tools/verify_candidate_input.py "$PGBR_ORIGINAL_TEMP/original.apkm" --require-signatures --require-manifest-identity --apksigner "$signer" --apkanalyzer "$analyzer"'
        self.assertIn(gate, workflow)
        self.assertIn("needs: [native-pvp-fixture, validate-original]", workflow)
        self.assertIn("needs.validate-original.outputs.sha256", workflow)
        self.assertIn("Get-FileHash -LiteralPath 'pgrazor/input/game.apkm' -Algorithm SHA256", workflow)
        self.assertIn("$actual -cne $env:VERIFIED_INPUT_SHA256", workflow)
        self.assertIn('report.get("signed_manifest_identity_verified") is not True', workflow)
        self.assertLess(workflow.index("Get-FileHash -LiteralPath 'pgrazor/input/game.apkm'"), workflow.index("run: python -B pgrazor/nonroot.py"))
        self.assertLess(workflow.index(gate), workflow.index('run: python -B pgrazor/nonroot.py'))
        self.assertLess(workflow.index(gate), workflow.index('run: python -B tools/check.py'))
