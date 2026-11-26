"""Read-only validation of a candidate package; never activates a runtime profile."""
import argparse
import hashlib
import json
from pathlib import Path
import re
import subprocess
import tempfile
import zipfile
import xml.etree.ElementTree as ET

MANIFEST = Path(__file__).resolve().with_name("game_input_0439_manifest.json")


def assemble_parts(parts, expected, destination):
    """Join only exact known parts in order into a private temporary file."""
    if len(parts) != len(expected["parts"]):
        raise ValueError("Wrong part count")
    with Path(destination).open("xb") as output:
        for path, spec in zip(parts, expected["parts"]):
            path = Path(path)
            if path.is_symlink() or not path.is_file():
                raise ValueError("Part must be a regular file, not a symbolic link")
            if path.stat().st_size != spec["bytes"]:
                raise ValueError("Part size mismatch")
            digest = hashlib.sha256()
            copied = 0
            with Path(path).open("rb") as stream:
                while block := stream.read(1024 * 1024):
                    copied += len(block)
                    if copied > spec["bytes"]:
                        raise ValueError("Part exceeds expected size")
                    digest.update(block)
                    output.write(block)
            if copied != spec["bytes"] or digest.hexdigest() != spec["sha256"]:
                raise ValueError("Part hash mismatch")


def verify_manifest_identity(target, name, expected, analyzer):
    """Inspect the signed split's own manifest, not the APKM envelope."""
    result = subprocess.run([str(analyzer), "manifest", "print", str(target)],
                            capture_output=True, text=True, encoding="utf-8",
                            errors="strict", timeout=120, check=False)
    if result.returncode != 0:
        raise ValueError("APK manifest inspection failed: " + name)
    if len(result.stdout) > 4 * 1024 * 1024 or "<!DOCTYPE" in result.stdout or "<!ENTITY" in result.stdout:
        raise ValueError("Unsafe or oversized APK manifest XML: " + name)
    try:
        root = ET.fromstring(result.stdout)
    except ET.ParseError as error:
        raise ValueError("Invalid APK manifest XML: " + name) from error
    ns = "{http://schemas.android.com/apk/res/android}"
    split = "" if name == "base.apk" else name.removeprefix("split_").removesuffix(".apk")
    version_name = root.get(ns + "versionName")
    if (root.tag != "manifest" or root.get("package") != expected["package_name"] or
            root.get(ns + "versionCode") != expected["version_code"] or
            root.get("split", "") != split or
            (name == "base.apk" and version_name != expected["game_version"]) or
            (version_name is not None and version_name != expected["game_version"]) or
            root.get(ns + "versionCodeMajor", "0") != "0"):
        raise ValueError("Signed APK manifest identity mismatch: " + name)


def verify_signers(path, expected, apksigner, apkanalyzer=None):
    """Verify APK signatures using the caller's trusted Android SDK executable."""
    executable = Path(apksigner).resolve(strict=True)
    analyzer = Path(apkanalyzer).resolve(strict=True) if apkanalyzer is not None else None
    pinned = expected["expected_signer_sha256"]
    if not re.fullmatch(r"[0-9a-f]{64}", pinned):
        raise ValueError("Invalid expected signer")
    with tempfile.TemporaryDirectory(prefix="pgbr-candidate-signature-") as directory:
        with zipfile.ZipFile(path) as archive:
            for number, name in enumerate(expected["splits"]):
                target = Path(directory) / f"split-{number}.apk"
                if archive.getinfo(name).file_size != expected["splits"][name]["bytes"]:
                    raise ValueError("Extracted split size mismatch: " + name)
                copied = 0
                with archive.open(name) as source, target.open("xb") as output:
                    while block := source.read(1024 * 1024):
                        copied += len(block)
                        if copied > expected["splits"][name]["bytes"]:
                            raise ValueError("Extracted split exceeds expected size: " + name)
                        output.write(block)
                if copied != expected["splits"][name]["bytes"]:
                    raise ValueError("Extracted split size mismatch: " + name)
                # Recheck bytes after extraction, including if source changed.
                with target.open("rb") as stream:
                    if hashlib.file_digest(stream, "sha256").hexdigest() != expected["splits"][name]["sha256"]:
                        raise ValueError("Extracted split hash mismatch")
                result = subprocess.run([str(executable), "verify", "--print-certs", str(target)],
                                        capture_output=True, text=True, encoding="utf-8",
                                        errors="replace", timeout=120, check=False)
                if result.returncode != 0:
                    raise ValueError("APK signature verification failed: " + name)
                signers = re.findall(r"(?m)^Signer #([0-9]+) certificate SHA-256 digest: ([0-9a-fA-F]{64})\s*$", result.stdout)
                if [(number, digest.lower()) for number, digest in signers] != [("1", pinned)]:
                    raise ValueError("Unexpected APK signer: " + name)
                if analyzer is not None:
                    verify_manifest_identity(target, name, expected, analyzer)


def validate(path, expected, apksigner=None, *, require_signatures=False, apkanalyzer=None, require_manifest_identity=False):
    if require_manifest_identity and (apksigner is None or apkanalyzer is None):
        raise ValueError("Signed manifest identity required: supply trusted apksigner and apkanalyzer")
    if apkanalyzer is not None and apksigner is None:
        raise ValueError("Manifest identity verification requires apksigner")
    if require_signatures and apksigner is None:
        raise ValueError("Signature verification required: supply trusted apksigner")
    path = Path(path)
    if path.is_symlink() or not path.is_file():
        raise ValueError("Candidate must be a regular file, not a symbolic link")
    if path.stat().st_size != expected["archive_bytes"]:
        raise ValueError("Archive size mismatch")
    with path.open("rb") as stream:
        digest = hashlib.file_digest(stream, "sha256").hexdigest()
    if digest != expected["archive_sha256"]:
        raise ValueError("Archive SHA-256 mismatch")
    with zipfile.ZipFile(path) as archive:
        names = archive.namelist()
        if len(set(names)) != len(names):
            raise ValueError("Duplicate archive entry")
        if set(n for n in names if n.endswith(".apk")) != set(expected["splits"]):
            raise ValueError("Unexpected APK split set")
        if archive.getinfo("info.json").file_size > 65536:
            raise ValueError("Metadata exceeds size limit")
        def unique_object(pairs):
            result = {}
            for key, value in pairs:
                if key in result:
                    raise ValueError("Duplicate metadata key: " + key)
                result[key] = value
            return result
        info = json.loads(archive.read("info.json"), object_pairs_hook=unique_object)
        if not isinstance(info, dict):
            raise ValueError("Invalid archive metadata object")
        code = info.get("versioncode")
        if type(code) not in (str, int):
            raise ValueError("Invalid archive metadata version code")
        if (info.get("pname") != expected["package_name"] or
            info.get("release_version") != expected["game_version"] or
            str(info.get("versioncode")) != expected["version_code"] or
            info.get("arches") != [expected["architecture"]]):
            raise ValueError("Archive metadata mismatch")
        for name, spec in expected["splits"].items():
            if archive.getinfo(name).file_size != spec["bytes"]:
                raise ValueError("Split size mismatch: " + name)
            with archive.open(name) as stream:
                if hashlib.file_digest(stream, "sha256").hexdigest() != spec["sha256"]:
                    raise ValueError("Split hash mismatch: " + name)
        if archive.testzip() is not None:
            raise ValueError("ZIP CRC mismatch")
    if apksigner is not None:
        verify_signers(path, expected, apksigner, apkanalyzer)
    return {"archive_identity_verified": True, "split_hashes_verified": True,
            "archive_metadata_verified": True, "apk_signatures_verified": apksigner is not None,
            "signed_manifest_identity_verified": apkanalyzer is not None,
            "runtime_compatibility_verified": False, "game_version": expected["game_version"],
            "sha256": digest}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("input", type=Path, nargs="?")
    parser.add_argument("--parts", type=Path, nargs=2, metavar=("PART1", "PART2"))
    parser.add_argument("--apksigner", type=Path, help="Trusted executable from the Android SDK")
    parser.add_argument("--require-signatures", action="store_true",
                        help="Reject unsigned preflight reports; requires --apksigner")
    parser.add_argument("--apkanalyzer", type=Path, help="Trusted SDK manifest inspection tool; requires --apksigner")
    parser.add_argument("--require-manifest-identity", action="store_true",
                        help="Require both trusted SDK tools; reject signature-only reports")
    args = parser.parse_args()
    if args.require_manifest_identity and (args.apksigner is None or args.apkanalyzer is None):
        parser.error("--require-manifest-identity requires --apksigner and --apkanalyzer")
    if args.apkanalyzer is not None and args.apksigner is None:
        parser.error("--apkanalyzer requires --apksigner")
    if args.require_signatures and args.apksigner is None:
        parser.error("--require-signatures requires --apksigner")
    if (args.input is None) == (args.parts is None):
        parser.error("Supply one input or --parts PART1 PART2")
    try:
        expected = json.loads(MANIFEST.read_text(encoding="utf-8"))
        if args.parts is not None:
            with tempfile.TemporaryDirectory(prefix="pgbr-candidate-join-") as directory:
                joined = Path(directory) / "candidate.apkm"
                assemble_parts(args.parts, expected, joined)
                report = validate(joined, expected, args.apksigner, require_signatures=args.require_signatures, apkanalyzer=args.apkanalyzer, require_manifest_identity=args.require_manifest_identity)
                report["parts_verified"] = True
        else:
            report = validate(args.input, expected, args.apksigner, require_signatures=args.require_signatures, apkanalyzer=args.apkanalyzer, require_manifest_identity=args.require_manifest_identity)
        print(json.dumps(report, indent=2))
    except (OSError, ValueError, KeyError, zipfile.BadZipFile, subprocess.TimeoutExpired) as error:
        parser.exit(1, "Rejected: " + str(error) + "\n")


if __name__ == "__main__":
    main()
