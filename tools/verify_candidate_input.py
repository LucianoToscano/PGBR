"""Read-only validation of a candidate package; never activates a runtime profile."""
import argparse
import hashlib
import json
from pathlib import Path
import zipfile

MANIFEST = Path(__file__).resolve().with_name("game_input_0439_manifest.json")


def validate(path, expected):
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
    return {"archive_identity_verified": True, "split_hashes_verified": True,
            "archive_metadata_verified": True, "apk_signatures_verified": False,
            "runtime_compatibility_verified": False, "game_version": expected["game_version"],
            "sha256": digest}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("input", type=Path)
    args = parser.parse_args()
    try:
        expected = json.loads(MANIFEST.read_text(encoding="utf-8"))
        print(json.dumps(validate(args.input, expected), indent=2))
    except (OSError, ValueError, KeyError, zipfile.BadZipFile) as error:
        parser.exit(1, "Rejected: " + str(error) + "\n")


if __name__ == "__main__":
    main()
