"""Keep the new candidate separate from earlier releases and game profiles."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[2]

class Lt049QaContract(unittest.TestCase):
    def test_manual_qa_only(self):
        workflow = (ROOT / ".github/workflows/pgbr-lt049-qa.yml").read_text(encoding="utf-8")
        for term in ("workflow_dispatch:", "PGRAZOR_VERSION: v0.00.49-LT",
                     "github.ref == 'refs/heads/main'", "0.437.0.json",
                     "PGBR-v0.00.49-LT.apk", "PGBR-v0.00.49-LT-Root.apk",
                     "PGBR-v0.00.49-LT-Root-module.zip", "SHA256SUMS.txt",
                     "tools/verify_release.py dist/pgbr-lt049 --tag v0.00.49-LT",
                     "tools/lt028_pinned_input.py", "tools/check.py"):
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
