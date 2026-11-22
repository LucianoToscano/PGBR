"""Non-root APK cleanup only removes old, redundant APKs with a valid rollback pin.

All fixtures live under the ignored pgrazor/build tree; never use real builds.
"""
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import time
import unittest

ROOT = Path(__file__).resolve().parents[1]


@unittest.skipUnless(os.name == "nt", "PowerShell cleanup requires Windows")
class NonrootCleanupTests(unittest.TestCase):
    def make_fixture(self, folder, pins):
        root = Path(folder).resolve()
        shutil.copyfile(ROOT / "cleanup.ps1", root / "cleanup.ps1")
        (root / "build/nonroot").mkdir(parents=True)
        runs = []
        old = time.time() - 7200
        for i in range(5):
            run = root / "build/nonroot" / f"20260101-00000{i}-1234abcd"
            run.mkdir()
            apk = run / "base.apk"
            apk.write_bytes(f"fixture APK {i}".encode())
            report = {
                "package": "com.nianticlabs.pokemongo",
                "runtime_abi": "arm64-v8a",
                "outputs": [{
                    "file": str(apk),
                    "sha256": hashlib.sha256(apk.read_bytes()).hexdigest(),
                }],
            }
            (run / "report.json").write_text(json.dumps(report), encoding="utf-8")
            (run / "diagnostic.txt").write_text("keep for diagnosis", encoding="utf-8")
            for item in run.iterdir():
                os.utime(item, (old, old))
            os.utime(run, (old, old))
            runs.append(run)
        config = {
            "schema": 1,
            "pinnedRuns": [
                {"run": runs[i].name, "reason": "validated rollback"} for i in pins
            ],
            "pinnedRootedRuns": [],
        }
        (root / "build-retention.json").write_text(json.dumps(config), encoding="utf-8")
        return root, runs

    def invoke(self, root, apply=False):
        exe = shutil.which("pwsh") or "powershell.exe"
        return subprocess.run(
            [exe, "-NoProfile", "-ExecutionPolicy", "Bypass",
             "-File", str(root / "cleanup.ps1"), *(["-Apply"] if apply else [])],
            capture_output=True, text=True, timeout=30,
        )

    def test_empty_pins_skip_without_deleting_or_failing(self):
        (ROOT / "build").mkdir(exist_ok=True)
        with tempfile.TemporaryDirectory(prefix="nonroot-cleanup-test-", dir=ROOT / "build") as folder:
            root, runs = self.make_fixture(folder, pins=[])
            result = self.invoke(root, apply=True)
            self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
            self.assertIn("Cleanup skipped", result.stdout)
            self.assertTrue(all((r / "base.apk").exists() for r in runs))

    def test_pins_latest_three_and_non_apk_evidence_survive(self):
        (ROOT / "build").mkdir(exist_ok=True)
        with tempfile.TemporaryDirectory(prefix="nonroot-cleanup-test-", dir=ROOT / "build") as folder:
            root, runs = self.make_fixture(folder, pins=[0])
            preview = self.invoke(root)
            self.assertEqual(preview.returncode, 0, preview.stdout + preview.stderr)
            self.assertTrue(all((r / "base.apk").exists() for r in runs))
            applied = self.invoke(root, apply=True)
            self.assertEqual(applied.returncode, 0, applied.stdout + applied.stderr)
            # Index 0 is pinned; indices 2-4 are the latest complete runs.
            self.assertEqual(
                [(r / "base.apk").exists() for r in runs],
                [True, False, True, True, True],
            )
            self.assertTrue(all((r / "report.json").exists() for r in runs))
            self.assertTrue(all((r / "diagnostic.txt").exists() for r in runs))
            receipts = list((root / "build/cleanup").glob("*.json"))
            self.assertEqual(len(receipts), 2)
            self.assertEqual(
                json.loads(max(receipts, key=lambda p: p.stat().st_mtime).read_text(encoding="utf-8-sig"))["deletedBytes"],
                len(b"fixture APK 1"),
            )

    def test_invalid_config_does_not_delete(self):
        (ROOT / "build").mkdir(exist_ok=True)
        with tempfile.TemporaryDirectory(prefix="nonroot-cleanup-test-", dir=ROOT / "build") as folder:
            root, runs = self.make_fixture(folder, pins=[0])
            (root / "build-retention.json").write_text(
                json.dumps({"schema": 1, "pinnedRootedRuns": []}), encoding="utf-8"
            )
            result = self.invoke(root, apply=True)
            self.assertNotEqual(result.returncode, 0)
            self.assertTrue(all((r / "base.apk").exists() for r in runs))


if __name__ == "__main__":
    unittest.main()
