"""Retrieve the pinned original game archive from our repository draft release.

Uses the GitHub CLI's authenticated asset downloader (supports draft releases),
not urllib's ambiguous release-asset JSON/binary content negotiation.
Never logs tokens or key bytes. Hash is validated against the current game profile.
"""
import argparse
import base64
import json
import os
from pathlib import Path
import shutil
import subprocess
from game_update import current_profile, verify_input

ROOT = Path(__file__).resolve().parents[1]
SOURCE_NAME = 'Pokemon_GO_0.439.0_ARM64_ORIGINAL.apkm'
RELEASE_TAG = 'build-inputs'
REPOSITORY = 'LucianoToscano/PGBR'

MAX_MANIFEST_BYTES = 64 * 1024
MAX_KEYSTORE_BYTES = 1024 * 1024


def unique_pairs(pairs):
    result = {}
    for key, value in pairs:
        if key in result:
            raise ValueError("Duplicate input manifest key")
        result[key] = value
    return result


def read_manifest(path):
    path = Path(path)
    if path.is_symlink() or not path.is_file():
        raise ValueError("Input manifest must be a regular file")
    with path.open("rb") as stream:
        raw = stream.read(MAX_MANIFEST_BYTES + 1)
    if len(raw) > MAX_MANIFEST_BYTES:
        raise ValueError("Input manifest exceeds size limit")
    obj = json.loads(raw, object_pairs_hook=unique_pairs)
    if not isinstance(obj, dict) or type(obj.get("schema")) is not int or obj["schema"] != 1:
        raise ValueError("Invalid input manifest schema")
    if (obj.get("game_version") != "0.439.0" or obj.get("version_code") != "2026111602"
            or obj.get("package_name") != "com.nianticlabs.pokemongo"
            or obj.get("architecture") != "arm64-v8a"):
        raise ValueError("Unexpected LT049 input identity")
    size = obj.get("archive_bytes")
    if type(size) is not int or not 0 < size <= 1024 * 1024 * 1024:
        raise ValueError("Invalid input archive size")
    for key in ("archive_sha256", "library_sha256", "metadata_sha256", "expected_signer_sha256"):
        value = obj.get(key)
        if not isinstance(value, str) or len(value) != 64 or any(ch not in "0123456789abcdef" for ch in value):
            raise ValueError("Invalid pinned input checksum")
    return obj


def require_empty_path(path):
    if path.exists() or path.is_symlink():
        raise ValueError("Input or signing path must start empty")


def verified_download(path, profile, expected):
    path = Path(path)
    if path.is_symlink() or not path.is_file():
        raise ValueError("Downloaded input must be a regular file")
    if path.stat().st_size != expected["archive_bytes"]:
        raise ValueError("Downloaded input size does not match manifest")
    return verify_input(path, profile)


def prepare_key(encoded, path, runner_temp):
    if not encoded or len(encoded) > ((MAX_KEYSTORE_BYTES + 2) // 3) * 4:
        raise ValueError("Signing key missing or exceeds size limit")
    raw = base64.b64decode(encoded, validate=True)
    if not raw or len(raw) > MAX_KEYSTORE_BYTES:
        raise ValueError("Invalid signing key size")
    path = Path(path)
    temp = Path(runner_temp).resolve(strict=True)
    if not temp.is_dir() or not path.is_absolute() or path.parent.resolve(strict=True) != temp:
        raise ValueError("Signing key must be directly inside runner temporary directory")
    require_empty_path(path)
    fd = os.open(path, os.O_WRONLY | os.O_CREAT | os.O_EXCL | getattr(os, "O_BINARY", 0), 0o600)
    try:
        with os.fdopen(fd, "wb") as output:
            output.write(raw)
    except Exception:
        path.unlink(missing_ok=True)
        raise


def check_profile():
    profile = current_profile()
    expected = read_manifest(ROOT / 'tools/game_input_0439_manifest.json')
    required = {
        'game_version': expected['game_version'],
        'version_code': int(expected['version_code']),
        'input_sha256': expected['archive_sha256'],
        'library_sha256': expected['library_sha256'],
        'metadata_sha256': expected['metadata_sha256'],
        'game_signer_sha256': expected['expected_signer_sha256'],
    }
    if any(profile.get(key) != value for key, value in required.items()):
        raise SystemExit('LT049 requires the reviewed 0.439.0 profile; old or incomplete profile rejected')
    return profile


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--check-profile', action='store_true')
    args = parser.parse_args()
    profile = check_profile()
    if args.check_profile:
        print('0.439.0 identity gate passed; runtime compatibility is separate')
        return
    token = os.environ.get('GH_TOKEN') or os.environ.get('GITHUB_TOKEN')
    if not token:
        raise SystemExit('GitHub release access unavailable (GH_TOKEN missing)')
    cli = shutil.which('gh')
    if not cli:
        raise SystemExit('GitHub CLI not found on build runner')
    folder = ROOT / 'pgrazor' / 'input'
    folder.mkdir(parents=True, exist_ok=True)
    fetched = folder / SOURCE_NAME
    target = folder / 'game.apkm'
    if folder.is_symlink() or not folder.is_dir():
        raise SystemExit('Invalid input folder')
    try:
        require_empty_path(fetched)
        require_empty_path(target)
        expected = read_manifest(ROOT / 'tools/game_input_0439_manifest.json')
    except (ValueError, OSError) as error:
        raise SystemExit('Input preparation rejected: ' + type(error).__name__) from None
    command = [cli, 'release', 'download', RELEASE_TAG, '--repo', REPOSITORY,
               '--pattern', SOURCE_NAME, '--dir', str(folder)]
    env = dict(os.environ)
    env['GH_TOKEN'] = token
    try:
        visibility = subprocess.run([cli, 'release', 'view', RELEASE_TAG, '--repo', REPOSITORY,
                                     '--json', 'isDraft'], env=env, capture_output=True, text=True, timeout=60)
    except (OSError, subprocess.TimeoutExpired):
        raise SystemExit('Private draft release lookup failed or timed out') from None
    if visibility.returncode != 0:
        raise SystemExit('Private draft release unavailable')
    try:
        is_draft = json.loads(visibility.stdout).get('isDraft') is True
    except (ValueError, AttributeError):
        is_draft = False
    if not is_draft:
        raise SystemExit('Refusing original package from a non-draft release')
    try:
        result = subprocess.run(command, env=env, capture_output=True, text=True, timeout=600)
    except (OSError, subprocess.TimeoutExpired):
        raise SystemExit('Draft release download failed or timed out') from None
    if result.returncode != 0:
        # Do not print stderr, which may contain credentials or signed URLs.
        raise SystemExit('Draft release download failed (gh exit %d); check job contents permission and draft asset visibility' % result.returncode)
    if not fetched.is_file():
        raise SystemExit('Release download did not produce the pinned game file')
    # Never package an unexpected file, even if the tag is reassigned.
    try:
        result = verified_download(fetched, profile, expected)
    except Exception as error:
        raise SystemExit('Game archive failed pinned integrity verification: ' + type(error).__name__) from None
    fetched.replace(target)
    encoded = os.environ.get('PGRAZOR_KEYSTORE_BASE64')
    if not encoded:
        raise SystemExit('Signing key was not configured')
    try:
        prepare_key(encoded, os.environ['PGRAZOR_KEYSTORE'], os.environ['RUNNER_TEMP'])
    except (OSError, ValueError, KeyError):
        raise SystemExit('Signing key preparation rejected; check runner configuration') from None
    print('Pinned original verified:', result['game_version'], result['sha256'])
    print('Signing key loaded into isolated runner temp path')
if __name__ == '__main__':
    main()
