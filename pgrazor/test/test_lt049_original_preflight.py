"""LT049 original validation is independent of runtime approval and publication."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[2]

class OriginalPreflightContract(unittest.TestCase):
    def test_independent_original_preflight(self):
        workflow = (ROOT / ".github/workflows/pgbr-lt049-original-preflight.yml").read_text(encoding="utf-8")
        for term in ("workflow_dispatch:", "branches: [main]", "refs/heads/main",
                     "release.get('draft') is not True", "452254190",
                     "a.get('digest')", "a.get('size')", "a.get('state')",
                     "--require-signatures", "--require-manifest-identity",
                     "--apksigner", "--apkanalyzer", "verify_candidate_input.py",
                     "if: always()", "persist-credentials: false"):
            self.assertIn(term, workflow)
        for forbidden in ("secrets.", "upload-artifact", "release upload",
                          "release create", "nonroot.py", "rooted.py", "current.json",
                          "lt049_pinned_input.py", "pull_request_target:"):
            self.assertNotIn(forbidden, workflow)
