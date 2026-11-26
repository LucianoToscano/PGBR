"""Synthetic release fixtures; no Android game files or signing keys required."""
import hashlib
import importlib.util
import json
from pathlib import Path
import tempfile
import unittest
import warnings
import zipfile

ROOT = Path(__file__).resolve().parents[2]
spec = importlib.util.spec_from_file_location("verify_release", ROOT / "tools/verify_release.py")
m = importlib.util.module_from_spec(spec)
spec.loader.exec_module(m)


class ReleaseIntegrityBatch(unittest.TestCase):
    tag = "v0.00.49-LT"

    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.folder = Path(self.tmp.name)
        self.normal, self.root, self.module = [self.folder / n for n in m.downloads(self.tag)]
        self.write_zip(self.normal, [("assets/pgrazor/mode.json", json.dumps({"pgrazorVersion": self.tag}))])
        self.write_zip(self.module, [("module.prop", "id=pgrazor\nversion=" + self.tag)])
        self.write_zip(self.root, [("assets/module.zip", self.module.read_bytes())])
        self.refresh()

    def write_zip(self, path, entries):
        with warnings.catch_warnings():
            warnings.simplefilter("ignore", UserWarning)
            with zipfile.ZipFile(path, "w") as archive:
                for name, data in entries:
                    archive.writestr(name, data)

    def refresh(self):
        sha = lambda p: hashlib.sha256(p.read_bytes()).hexdigest()
        values = {p.name: sha(p) for p in (self.normal, self.root, self.module)}
        (self.folder / "SHA256SUMS.txt").write_text("".join(v + "  " + k + "\n" for k, v in values.items()), encoding="utf-8")
        (self.folder / "build-info.json").write_text(json.dumps({"apk_sha256": sha(self.normal), "game_version": "0.439.0", "game_version_code": 2026111602}), encoding="utf-8")
        (self.folder / "root-build-info.json").write_text(json.dumps({"installer_sha256": sha(self.root), "module_sha256": sha(self.module), "target": {"versionName": "0.439.0", "versionCode": 2026111602}, "version": self.tag}), encoding="utf-8")

    def reject(self, message):
        with self.assertRaisesRegex(ValueError, message):
            m.verify(self.folder, self.tag)

    def test_valid_release(self):
        self.assertEqual(set(m.verify(self.folder, self.tag)), set(m.downloads(self.tag)))

    def test_invalid_tag_paths(self):
        for tag in ("../other", "a/b", "a\\b", "", ".", "a\n"):
            with self.subTest(tag=tag), self.assertRaisesRegex(ValueError, "Invalid release tag"):
                m.downloads(tag)

    def test_empty_artifact(self):
        self.normal.write_bytes(b"")
        self.reject("Missing or empty")

    def test_artifact_symlink(self):
        target = self.folder / "copy.apk"
        target.write_bytes(self.normal.read_bytes())
        self.normal.unlink()
        try:
            self.normal.symlink_to(target)
        except OSError:
            self.skipTest("symlink privilege unavailable")
        self.reject("Missing or empty")

    def test_duplicate_checksum(self):
        p = self.folder / "SHA256SUMS.txt"
        s = p.read_text()
        p.write_text(s + s.splitlines()[0] + "\n")
        self.reject("Duplicate checksum")

    def test_unexpected_checksum_name(self):
        p = self.folder / "SHA256SUMS.txt"
        p.write_text(p.read_text() + "0"*64 + "  ../extra\n")
        self.reject("Invalid release checksum")

    def test_malformed_checksum(self):
        p = self.folder / "SHA256SUMS.txt"
        p.write_text(p.read_text().replace(p.read_text().split()[0], "bad"))
        self.reject("Invalid release checksum")

    def test_checksum_size_limit(self):
        (self.folder / "SHA256SUMS.txt").write_bytes(b"x"*(m.MAX_CHECKSUM_BYTES+1))
        self.reject("Metadata too large")

    def test_duplicate_json_receipt(self):
        p = self.folder / "build-info.json"
        p.write_text(p.read_text().replace("{", '{"game_version":"wrong",', 1))
        self.reject("Duplicate metadata key")

    def test_non_object_receipt(self):
        (self.folder / "build-info.json").write_text("[]")
        self.reject("Invalid metadata object")

    def test_invalid_target_object(self):
        p = self.folder / "root-build-info.json"
        obj = json.loads(p.read_text())
        obj["target"] = []
        p.write_text(json.dumps(obj))
        self.reject("Invalid game target")

    def test_receipt_size_limit(self):
        (self.folder / "build-info.json").write_bytes(b"x"*(m.MAX_RECEIPT_BYTES+1))
        self.reject("Metadata too large")

    def test_duplicate_zip_entry(self):
        self.write_zip(self.normal, [("assets/pgrazor/mode.json", "{}"), ("assets/pgrazor/mode.json", "{}")])
        self.refresh()
        self.reject("Duplicate ZIP entry")

    def test_duplicate_mode_key(self):
        self.write_zip(self.normal, [("assets/pgrazor/mode.json", '{"pgrazorVersion":"old","pgrazorVersion":"' + self.tag + '"}')])
        self.refresh()
        self.reject("Duplicate metadata key")

    def test_duplicate_module_property(self):
        self.write_zip(self.module, [("module.prop", "id=other\nid=pgrazor\nversion="+self.tag)])
        self.refresh()
        self.reject("Duplicate metadata key")

    def test_oversized_zip_metadata(self):
        self.write_zip(self.normal, [("assets/pgrazor/mode.json", b"x"*(m.MAX_RECEIPT_BYTES+1))])
        self.refresh()
        self.reject("Metadata too large")

    def test_installer_module_mismatch(self):
        self.write_zip(self.root, [("assets/module.zip", b"wrong")])
        self.refresh()
        self.reject("Installer embeds a different module")

    def test_mixed_game_targets(self):
        p = self.folder / "root-build-info.json"
        obj = json.loads(p.read_text())
        obj["target"]["versionName"] = "0.437.0"
        p.write_text(json.dumps(obj))
        self.reject("game targets differ")


if __name__ == "__main__":
    unittest.main()
