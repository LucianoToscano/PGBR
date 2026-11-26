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

def check_profile():
    profile = current_profile()
    expected = json.loads((ROOT / 'tools/game_input_0439_manifest.json').read_text(encoding='utf-8'))
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
    if fetched.exists() or target.exists():
        raise SystemExit('Input path must start empty to avoid stale game builds')
    command = [cli, 'release', 'download', RELEASE_TAG, '--repo', REPOSITORY,
               '--pattern', SOURCE_NAME, '--dir', str(folder)]
    env = dict(os.environ)
    env['GH_TOKEN'] = token
    visibility = subprocess.run([cli, 'release', 'view', RELEASE_TAG, '--repo', REPOSITORY,
                                 '--json', 'isDraft'], env=env, capture_output=True, text=True)
    if visibility.returncode != 0:
        raise SystemExit('Private draft release unavailable')
    try:
        is_draft = json.loads(visibility.stdout).get('isDraft') is True
    except (ValueError, AttributeError):
        is_draft = False
    if not is_draft:
        raise SystemExit('Refusing original package from a non-draft release')
    result = subprocess.run(command, env=env, capture_output=True, text=True)
    if result.returncode != 0:
        # Do not print stderr, which may contain credentials or signed URLs.
        raise SystemExit('Draft release download failed (gh exit %d); check job contents permission and draft asset visibility' % result.returncode)
    if not fetched.is_file():
        raise SystemExit('Release download did not produce the pinned game file')
    # Never package an unexpected file, even if the tag is reassigned.
    try:
        result = verify_input(fetched, profile)
    except Exception as error:
        raise SystemExit('Game archive failed pinned integrity verification: ' + type(error).__name__) from None
    fetched.replace(target)
    encoded = os.environ.get('PGRAZOR_KEYSTORE_BASE64')
    if not encoded:
        raise SystemExit('Signing key was not configured')
    key_path = Path(os.environ['PGRAZOR_KEYSTORE'])
    key_path.parent.mkdir(parents=True, exist_ok=True)
    key_path.write_bytes(base64.b64decode(encoded, validate=True))
    print('Pinned original verified:', result['game_version'], result['sha256'])
    print('Signing key loaded into isolated runner temp path')
if __name__ == '__main__':
    main()
