"""Archived upstream releases must be append-only and idempotent."""
from pathlib import Path
import unittest

ROOT=Path(__file__).resolve().parents[2]
class ArchiveIdempotencyTest(unittest.TestCase):
    def test_archive_already_exists_is_noop(self):
        workflow=(ROOT/'.github/workflows/pgbr-archive-root-assets.yml').read_text(encoding='utf-8')
        self.assertIn('gh release view "$tag"',workflow)
        self.assertIn('already exists; preserving all published assets',workflow)
        self.assertIn('exit 0',workflow)
        self.assertIn('archive-pgrazor-source-root-20261118',workflow)
        self.assertIn("'.github/workflows/pgbr-archive-root-assets.yml'",workflow)
        self.assertIn('gh release create "archive-pgrazor-source-root-20261118"',workflow)
    def test_lt033_release_build_is_not_restarted_by_documentation_only_merge(self):
        workflow=(ROOT/'.github/workflows/pgbr-lt033-release.yml').read_text(encoding='utf-8')
        self.assertIn("- 'pgrazor/VERSION'",workflow)
        self.assertIn("- '.github/workflows/pgbr-lt033-release.yml'",workflow)
        self.assertIn('  workflow_dispatch:',workflow)
        self.assertIn("github.ref == 'refs/heads/main'",workflow)
if __name__=='__main__':unittest.main()
