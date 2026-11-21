"""Retrieve the pinned original game archive from our repository draft release.

Uses the GitHub CLI's authenticated asset downloader (supports draft releases),
not urllib's ambiguous release-asset JSON/binary content negotiation.
Never logs tokens or key bytes. Hash is validated against the current game profile.
"""
import base64
import os
from pathlib import Path
import shutil
import subprocess
from game_update import current_profile, verify_input

ROOT = Path(__file__).resolve().parents[1]
SOURCE_NAME = 'Pokemon_GO_0.437.0_ARM64_ORIGINAL.apkm'
RELEASE_TAG = 'build-inputs'
REPOSITORY = 'LucianoToscano/PGBR'

def main():
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
    result = subprocess.run(command, env=env, capture_output=True, text=True)
    if result.returncode != 0:
        # Do not print stderr, which may contain credentials or signed URLs.
        raise SystemExit('Draft release download failed (gh exit %d); check job contents permission and draft asset visibility' % result.returncode)
    if not fetched.is_file():
        raise SystemExit('Release download did not produce the pinned game file')
    # Never package an unexpected file, even if the tag is reassigned.
    try:
        result = verify_input(fetched, current_profile())
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
