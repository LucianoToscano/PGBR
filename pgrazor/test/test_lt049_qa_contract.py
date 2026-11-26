"""Keep the new candidate separate from earlier releases and game profiles."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[2]

class Lt049QaContract(unittest.TestCase):
    def test_manual_qa_only(self):
        workflow = (ROOT / ".github/workflows/pgbr-lt049-qa.yml").read_text(encoding="utf-8")
        for term in ("workflow_dispatch:", "PGRAZOR_VERSION: v0.00.49-LT",
                     "github.ref == 'refs/heads/main'", "tools/lt049_pinned_input.py --check-profile",
                     "PGBR-v0.00.49-LT.apk", "PGBR-v0.00.49-LT-Root.apk",
                     "PGBR-v0.00.49-LT-Root-module.zip", "SHA256SUMS.txt",
                     "tools/verify_release.py dist/pgbr-lt049 --tag v0.00.49-LT",
                     "tools/lt049_pinned_input.py", "tools/check.py", "--apksigner"):
            self.assertIn(term, workflow)
        for term in ("  push:", "  pull_request:", "gh release create", "gh release upload"):
            self.assertNotIn(term, workflow)

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
