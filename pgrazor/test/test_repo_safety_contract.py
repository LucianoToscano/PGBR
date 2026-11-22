"""Non-invasive repository policy checks; do not touch device, credentials or outputs."""
from pathlib import Path
import unittest

ROOT=Path(__file__).resolve().parents[2]
class PackagingSafetyTests(unittest.TestCase):
    def test_preview_not_debuggable(self):
        manifest=(ROOT/"pgrazor/AndroidManifest.xml").read_text()
        self.assertNotIn('android:debuggable="true"',manifest)
        self.assertIn('android:debuggable="false"',manifest)
    def test_single_pr_template(self):
        import subprocess
        tracked=subprocess.run(['git','ls-tree','-r','--name-only','HEAD','--','.github'],
                               cwd=ROOT,capture_output=True,text=True,check=True).stdout.splitlines()
        self.assertIn('.github/PULL_REQUEST_TEMPLATE.md',tracked)
        # Path.exists() is case-folded on Windows; only git tree entries are authoritative.
        self.assertNotIn('.github/pull_request_template.md',tracked)
    def test_archived_upstream_release_is_repo_guarded(self):
        txt=(ROOT/"upstream-workflows/release.yml").read_text()
        self.assertGreaterEqual(txt.count("github.repository == 'jpitty03/PGRazor'"),2)
        for name in ('ci.yml','discord-release.yml','pages.yml'):
            archived=(ROOT/'upstream-workflows'/name).read_text()
            self.assertIn("github.repository == 'jpitty03/PGRazor'",archived)
    def test_real_ci_contract_and_vendor_bridge(self):
        ci=(ROOT/".github/workflows/pgbr-codigo-ci.yml").read_text()
        check=(ROOT/"tools/check.py").read_text()
        self.assertIn('tools/check.py',ci)
        self.assertIn('pgrazor/rooted_installer.py --check',ci)
        self.assertIn("vendor').glob('test_*.cjs')",check)
        self.assertIn('native_host_tests.py',ci)
