"""Read-only validation of a candidate package; never activates a runtime profile."""
import argparse
import hashlib
import json
from pathlib import Path
import re
import shutil
import subprocess
import tempfile
import zipfile

MANIFEST = Path(__file__).resolve().with_name("game_input_0439_manifest.json")


def assemble_parts(parts, expected, destination):
    """Join only exact known parts in order into a private temporary file."""
    if len(parts) != len(expected["parts"]):
        raise ValueError("Wrong part count")
    with Path(destination).open("xb") as output:
        for path, spec in zip(parts, expected["parts"]):
            if Path(path).stat().st_size != spec["bytes"]:
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


def verify_signers(path, expected, apksigner):
    """Verify APK signatures using the caller's trusted Android SDK executable."""
    executable = Path(apksigner).resolve(strict=True)
    pinned = expected["expected_signer_sha256"]
    if not re.fullmatch(r"[0-9a-f]{64}", pinned):
        raise ValueError("Invalid expected signer")
    with tempfile.TemporaryDirectory(prefix="pgbr-candidate-signature-") as directory:
        with zipfile.ZipFile(path) as archive:
            for number, name in enumerate(expected["splits"]):
                target = Path(directory) / f"split-{number}.apk"
                with archive.open(name) as source, target.open("xb") as output:
                    shutil.copyfileobj(source, output, 1024 * 1024)
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


def validate(path, expected, apksigner=None, *, require_signatures=False):
    if require_signatures and apksigner is None:
        raise ValueError("Signature verification required: supply trusted apksigner")
    path = Path(path)
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
        info = json.loads(archive.read("info.json"))
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
        verify_signers(path, expected, apksigner)
    return {"archive_identity_verified": True, "split_hashes_verified": True,
            "archive_metadata_verified": True, "apk_signatures_verified": apksigner is not None,
            "signed_manifest_identity_verified": False,
            "runtime_compatibility_verified": False, "game_version": expected["game_version"],
            "sha256": digest}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("input", type=Path, nargs="?")
    parser.add_argument("--parts", type=Path, nargs=2, metavar=("PART1", "PART2"))
    parser.add_argument("--apksigner", type=Path, help="Trusted executable from the Android SDK")
    parser.add_argument("--require-signatures", action="store_true",
                        help="Reject unsigned preflight reports; requires --apksigner")
    args = parser.parse_args()
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
                report = validate(joined, expected, args.apksigner, require_signatures=args.require_signatures)
                report["parts_verified"] = True
        else:
            report = validate(args.input, expected, args.apksigner, require_signatures=args.require_signatures)
        print(json.dumps(report, indent=2))
    except (OSError, ValueError, KeyError, zipfile.BadZipFile, subprocess.TimeoutExpired) as error:
        parser.exit(1, "Rejected: " + str(error) + "\n")


if __name__ == "__main__":
    main()
