"""Synthetic ZIP fixtures: no copyrighted package or game code in CI."""
import hashlib
import importlib.util
import json
from pathlib import Path
import tempfile
import unittest
import zipfile

ROOT = Path(__file__).resolve().parents[2]
spec = importlib.util.spec_from_file_location("verify_candidate_input", ROOT / "tools/verify_candidate_input.py")
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)

class CandidateInputTests(unittest.TestCase):
    def fixture(self, folder, extra=False, wrong_metadata=False):
        path=Path(folder)/"sample.apkm"
        data={"base.apk":b"base", "split_config.arm64_v8a.apk":b"split"}
        info={"pname":"example", "release_version":"0.439.0", "versioncode":"2026111602", "arches":["arm64-v8a"]}
        if wrong_metadata:
            info["versioncode"]="0"
        with zipfile.ZipFile(path,"w") as archive:
            archive.writestr("info.json",json.dumps(info))
            for name,blob in data.items():
                archive.writestr(name,blob)
            if extra:
                archive.writestr("unexpected.apk",b"extra")
        expected={"package_name":"example","game_version":"0.439.0","version_code":"2026111602","architecture":"arm64-v8a",
                  "archive_bytes":path.stat().st_size,"archive_sha256":hashlib.sha256(path.read_bytes()).hexdigest(),
                  "splits":{name:{"bytes":len(blob),"sha256":hashlib.sha256(blob).hexdigest()} for name,blob in data.items()}}
        return path,expected

    def test_exact_identity_and_scope(self):
        with tempfile.TemporaryDirectory() as folder:
            path,expected=self.fixture(folder)
            report=module.validate(path,expected)
            self.assertTrue(report["split_hashes_verified"])
            self.assertFalse(report["apk_signatures_verified"])
            self.assertFalse(report["runtime_compatibility_verified"])
            expected["splits"]["base.apk"]["sha256"]="0"*64
            with self.assertRaisesRegex(ValueError,"Split hash"):
                module.validate(path,expected)
            expected["archive_sha256"]="0"*64
            with self.assertRaisesRegex(ValueError,"Archive SHA"):
                module.validate(path,expected)

    def test_unexpected_split_and_metadata(self):
        with tempfile.TemporaryDirectory() as folder:
            path,expected=self.fixture(folder,extra=True)
            with self.assertRaisesRegex(ValueError,"split set"):
                module.validate(path,expected)
            path,expected=self.fixture(folder,wrong_metadata=True)
            with self.assertRaisesRegex(ValueError,"metadata"):
                module.validate(path,expected)
