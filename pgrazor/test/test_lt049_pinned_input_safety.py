"""LT049 input preparation tests: synthetic files only, no original APK or signing material."""
import base64
import hashlib
import importlib
import json
from pathlib import Path
import sys
import tempfile
import unittest
from unittest.mock import patch

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / "tools"))
loader = importlib.import_module("lt049_pinned_input")


class PinnedInputSafety(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name).resolve()
        self.manifest = self.root / "manifest.json"
        self.expected = json.loads((ROOT / "tools/game_input_0439_manifest.json").read_text(encoding="utf-8"))

    def write_manifest(self, value):
        self.manifest.write_text(json.dumps(value), encoding="utf-8")

    def test_manifest_identity_and_hashes(self):
        self.write_manifest(self.expected)
        self.assertEqual(loader.read_manifest(self.manifest), self.expected)
        for key, value in (("schema", True), ("game_version", "0.437.0"),
                           ("version_code", 2026111602), ("package_name", "unexpected"),
                           ("architecture", "x86"), ("archive_bytes", True),
                           ("archive_bytes", 0), ("archive_bytes", 2**31),
                           ("archive_sha256", "bad"), ("library_sha256", None),
                           ("metadata_sha256", "A"*64), ("expected_signer_sha256", "0"*63)):
            with self.subTest(key=key, value=value):
                self.write_manifest(dict(self.expected, **{key:value}))
                with self.assertRaises(ValueError):
                    loader.read_manifest(self.manifest)

    def test_duplicate_nonobject_and_oversized_manifest(self):
        for raw in (b"[]", b'{"schema":1,"schema":1}', b" "* (loader.MAX_MANIFEST_BYTES+1)):
            self.manifest.write_bytes(raw)
            with self.assertRaises(ValueError):
                loader.read_manifest(self.manifest)

    def test_wrong_size_rejected_before_digest(self):
        archive = self.root / "original.apkm"
        archive.write_bytes(b"synthetic")
        with patch.object(loader, "verify_input") as verify:
            with self.assertRaisesRegex(ValueError, "size"):
                loader.verified_download(archive, {}, {"archive_bytes": 1})
            verify.assert_not_called()

    def test_digest_and_size_both_required(self):
        archive = self.root / "original.apkm"
        raw = b"synthetic"
        archive.write_bytes(raw)
        expected = {"archive_bytes": len(raw)}
        profile = {"input_sha256": hashlib.sha256(raw).hexdigest(), "game_version": "0.439.0"}
        self.assertTrue(loader.verified_download(archive, profile, expected)["identity_verified"])
        with self.assertRaises(ValueError):
            loader.verified_download(archive, dict(profile, input_sha256="0"*64), expected)

    def test_exclusive_key_path_and_no_overwrite(self):
        path = self.root / "key.jks"
        data = base64.b64encode(b"synthetic only").decode()
        loader.prepare_key(data, path, self.root)
        self.assertEqual(path.read_bytes(), b"synthetic only")
        with self.assertRaises(ValueError):
            loader.prepare_key(base64.b64encode(b"replacement").decode(), path, self.root)
        self.assertEqual(path.read_bytes(), b"synthetic only")

    def test_key_validation_does_not_create_file(self):
        path = self.root / "key.jks"
        for raw in ("", "not-base64!", "A" * (2 * loader.MAX_KEYSTORE_BYTES)):
            with self.subTest(length=len(raw)), self.assertRaises(ValueError):
                loader.prepare_key(raw, path, self.root)
            self.assertFalse(path.exists())
        folder = self.root / "other"
        folder.mkdir()
        with self.assertRaises(ValueError):
            loader.prepare_key("YQ==", folder / "key.jks", self.root)
        self.assertFalse((folder / "key.jks").exists())

    def test_symlink_paths_rejected(self):
        target = self.root / "target"
        target.write_bytes(b"original")
        link = self.root / "link"
        try:
            link.symlink_to(target)
        except OSError:
            self.skipTest("symlink unavailable")
        with self.assertRaises(ValueError):
            loader.require_empty_path(link)
        with self.assertRaises(ValueError):
            loader.verified_download(link, {}, {"archive_bytes":8})
        with self.assertRaises(ValueError):
            loader.prepare_key("YQ==", link, self.root)
        self.assertEqual(target.read_bytes(), b"original")
