"""Synthetic ZIP fixtures: no copyrighted package or game code in CI."""
import hashlib
import importlib.util
import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch
import subprocess
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
            self.assertFalse(report["signed_manifest_identity_verified"])
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

    def test_parts_and_signature_gate(self):
        with tempfile.TemporaryDirectory() as folder:
            path,expected=self.fixture(folder)
            data=path.read_bytes()
            pieces=[data[:len(data)//2],data[len(data)//2:]]
            parts=[]
            expected["parts"]=[]
            for i,piece in enumerate(pieces):
                part=Path(folder)/f"part{i}"
                part.write_bytes(piece)
                parts.append(part)
                expected["parts"].append({"bytes":len(piece),"sha256":hashlib.sha256(piece).hexdigest()})
            joined=Path(folder)/"joined.apkm"
            module.assemble_parts(parts,expected,joined)
            self.assertEqual(joined.read_bytes(),data)
            self.assertTrue(module.validate(joined,expected)["archive_identity_verified"])
            corrupt=bytearray(pieces[0]);corrupt[0]^=1
            parts[0].write_bytes(corrupt)
            with self.assertRaisesRegex(ValueError,"Part hash"):
                module.assemble_parts(parts,expected,Path(folder)/"bad.apkm")
            expected["expected_signer_sha256"]="a"*64
            sdk=Path(folder)/"apksigner"
            sdk.touch()
            success=subprocess.CompletedProcess([],0,"Signer #1 certificate SHA-256 digest: "+"a"*64+"\n","")
            with patch.object(module.subprocess,"run",return_value=success) as verify:
                report=module.validate(path,expected,sdk,require_signatures=True)
                self.assertFalse(report["signed_manifest_identity_verified"])
                self.assertTrue(report["apk_signatures_verified"])
                self.assertFalse(report["runtime_compatibility_verified"])
                self.assertEqual(verify.call_count,2)
            for failed in [
                subprocess.CompletedProcess([],1,"","bad signature"),
                subprocess.CompletedProcess([],0,"Signer #1 certificate SHA-256 digest: "+"b"*64+"\n",""),
                subprocess.CompletedProcess([],0,success.stdout+"Signer #2 certificate SHA-256 digest: "+"a"*64+"\n",""),
                subprocess.CompletedProcess([],0,"","")]:
                with patch.object(module.subprocess,"run",return_value=failed):
                    with self.assertRaises(ValueError):
                        module.validate(path,expected,sdk)

    def test_required_signature_gate_rejects_missing_tool(self):
        with tempfile.TemporaryDirectory() as folder:
            path,expected=self.fixture(folder)
            with self.assertRaisesRegex(ValueError,"Signature verification required"):
                module.validate(path,expected,require_signatures=True)
            with self.assertRaises(OSError):
                module.validate(path,expected,Path(folder)/"missing",require_signatures=True)

    def test_required_signature_cli_does_not_emit_success_report(self):
        result=subprocess.run([__import__("sys").executable, str(ROOT / "tools/verify_candidate_input.py"),
                               "missing.apkm", "--require-signatures"], capture_output=True, text=True)
        self.assertNotEqual(result.returncode,0)
        self.assertIn("--require-signatures requires --apksigner",result.stderr)
        self.assertEqual(result.stdout,"")

    def test_signed_manifest_identity(self):
        with tempfile.TemporaryDirectory() as folder:
            path,expected=self.fixture(folder)
            expected["expected_signer_sha256"]="a"*64
            sdk=Path(folder)/"apksigner"
            analyzer=Path(folder)/"apkanalyzer"
            sdk.touch()
            analyzer.touch()
            sig=subprocess.CompletedProcess([],0,"Signer #1 certificate SHA-256 digest: "+"a"*64+"\n","")
            def manifest(split="", package="example", code="2026111602", version="0.439.0"):
                return subprocess.CompletedProcess([],0,
                    f'<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="{package}" android:versionCode="{code}" android:versionName="{version}" split="{split}"/>',"")
            with patch.object(module.subprocess,"run",side_effect=[sig,manifest(),sig,manifest("config.arm64_v8a")]) as run:
                report=module.validate(path,expected,sdk,require_signatures=True,apkanalyzer=analyzer)
                self.assertTrue(report["signed_manifest_identity_verified"])
                self.assertFalse(report["runtime_compatibility_verified"])
                self.assertEqual(run.call_count,4)
            for bad in [manifest(package="wrong"),manifest(code="1"),manifest(version="0.437.0"),manifest(split="wrong"),subprocess.CompletedProcess([],0,"broken XML",""),subprocess.CompletedProcess([],1,"","error")]:
                with patch.object(module.subprocess,"run",side_effect=[sig,bad]):
                    with self.assertRaises(ValueError):
                        module.validate(path,expected,sdk,apkanalyzer=analyzer)
            with patch.object(module.subprocess,"run",side_effect=[sig,manifest(),sig,manifest("wrong")]):
                with self.assertRaisesRegex(ValueError,"identity mismatch"):
                    module.validate(path,expected,sdk,apkanalyzer=analyzer)
            with self.assertRaisesRegex(ValueError,"requires apksigner"):
                module.validate(path,expected,apkanalyzer=analyzer)

    def test_reject_ambiguous_metadata(self):
        valid = '{"pname":"example","release_version":"0.439.0","versioncode":"2026111602","arches":["arm64-v8a"]}'
        cases = [
            (valid.replace('"pname":"example"', '"pname":"other","pname":"example"'), "Duplicate metadata key"),
            (valid.replace('"versioncode":"2026111602"', '"versioncode":"0","versioncode":"2026111602"'), "Duplicate metadata key"),
            ("[]", "Invalid archive metadata object"),
            ("null", "Invalid archive metadata object"),
            (valid.replace('"2026111602"', 'true'), "Invalid archive metadata version code"),
            (valid.replace('"2026111602"', '2026111602.0'), "Invalid archive metadata version code"),
        ]
        for metadata, message in cases:
            with self.subTest(metadata=metadata), tempfile.TemporaryDirectory() as folder:
                path, expected = self.fixture(folder)
                with zipfile.ZipFile(path, "w") as archive:
                    archive.writestr("info.json", metadata)
                    archive.writestr("base.apk", b"base")
                    archive.writestr("split_config.arm64_v8a.apk", b"split")
                expected["archive_bytes"] = path.stat().st_size
                expected["archive_sha256"] = hashlib.sha256(path.read_bytes()).hexdigest()
                with self.assertRaisesRegex(ValueError, message):
                    module.validate(path, expected)

    def test_required_manifest_gate_without_both_tools(self):
        with tempfile.TemporaryDirectory() as folder:
            path, expected = self.fixture(folder)
            for signer, analyzer in ((None, None), (Path(folder)/"apksigner", None), (None, Path(folder)/"apkanalyzer")):
                with self.subTest(signer=signer, analyzer=analyzer), self.assertRaisesRegex(ValueError, "Signed manifest identity required"):
                    module.validate(path, expected, signer, apkanalyzer=analyzer, require_manifest_identity=True)

    def test_required_manifest_cli_rejects_incomplete_tool_set(self):
        command = [__import__("sys").executable, str(ROOT / "tools/verify_candidate_input.py"), "missing.apkm", "--require-manifest-identity"]
        for extra in ([], ["--apksigner", "missing"], ["--apkanalyzer", "missing"]):
            with self.subTest(extra=extra):
                result = subprocess.run(command + extra, capture_output=True, text=True)
                self.assertNotEqual(result.returncode, 0)
                self.assertIn("--require-manifest-identity requires --apksigner and --apkanalyzer", result.stderr)
                self.assertEqual(result.stdout, "")

    def test_reject_symbolic_candidate_and_parts(self):
        with tempfile.TemporaryDirectory() as folder:
            path, expected = self.fixture(folder)
            link = Path(folder)/"linked.apkm"
            try:
                link.symlink_to(path)
            except OSError:
                self.skipTest("symlink privilege unavailable")
            with self.assertRaisesRegex(ValueError, "regular file"):
                module.validate(link, expected)
            expected["parts"] = [{"bytes": path.stat().st_size, "sha256": expected["archive_sha256"]}]
            with self.assertRaisesRegex(ValueError, "regular file"):
                module.assemble_parts([link], expected, Path(folder)/"joined.apkm")

    def test_manifest_xml_rejects_dtd_and_excessive_size(self):
        with tempfile.TemporaryDirectory() as folder:
            path, expected = self.fixture(folder)
            for raw in ('<!DOCTYPE manifest [<!ENTITY x "bad">]><manifest/>', "x"*(4*1024*1024+1)):
                with self.subTest(length=len(raw)), patch.object(module.subprocess, "run", return_value=subprocess.CompletedProcess([],0,raw,"")):
                    with self.assertRaisesRegex(ValueError, "Unsafe or oversized"):
                        module.verify_manifest_identity(path, "base.apk", expected, Path(folder)/"apkanalyzer")

    def test_signer_extraction_rejects_changed_split_length(self):
        with tempfile.TemporaryDirectory() as folder:
            path, expected = self.fixture(folder)
            expected["expected_signer_sha256"] = "a"*64
            expected["splits"]["base.apk"]["bytes"] += 1
            sdk = Path(folder)/"apksigner"
            sdk.touch()
            with patch.object(module.subprocess, "run") as run:
                with self.assertRaisesRegex(ValueError, "Extracted split size mismatch"):
                    module.verify_signers(path, expected, sdk)
                run.assert_not_called()
