"""Reject incomplete or mixed Android release bundles before upload/publication."""
import argparse
import hashlib
import json
from pathlib import Path
import re
import zipfile


MAX_RECEIPT_BYTES = 4 * 1024 * 1024
MAX_CHECKSUM_BYTES = 64 * 1024


def downloads(tag):
    if not isinstance(tag, str) or not re.fullmatch(r"[A-Za-z0-9][A-Za-z0-9._-]{0,127}", tag):
        raise ValueError("Invalid release tag")
    return (f'PGBR-{tag}.apk', f'PGBR-{tag}-Root.apk', f'PGBR-{tag}-Root-module.zip')


def digest(path):
    with path.open('rb') as stream:
        return hashlib.file_digest(stream, 'sha256').hexdigest()


def unique_pairs(pairs):
    result = {}
    for key, value in pairs:
        if key in result:
            raise ValueError("Duplicate metadata key: " + key)
        result[key] = value
    return result


def json_object(raw, label):
    value = json.loads(raw, object_pairs_hook=unique_pairs)
    if not isinstance(value, dict):
        raise ValueError("Invalid metadata object: " + label)
    return value


def small_file(path, limit=MAX_RECEIPT_BYTES):
    if path.stat().st_size > limit:
        raise ValueError("Metadata too large: " + path.name)
    with path.open("rb") as stream:
        raw = stream.read(limit + 1)
    if len(raw) > limit:
        raise ValueError("Metadata too large: " + path.name)
    return raw


def checked_archive(archive):
    names = archive.namelist()
    if len(names) != len(set(names)):
        raise ValueError("Duplicate ZIP entry")
    return archive


def small_entry(archive, name):
    if archive.getinfo(name).file_size > MAX_RECEIPT_BYTES:
        raise ValueError("Metadata too large: " + name)
    with archive.open(name) as stream:
        raw = stream.read(MAX_RECEIPT_BYTES + 1)
    if len(raw) > MAX_RECEIPT_BYTES:
        raise ValueError("Metadata too large: " + name)
    return raw


def verify(folder, tag):
    folder = Path(folder)
    names = downloads(tag)
    for name in (*names, 'build-info.json', 'root-build-info.json', 'SHA256SUMS.txt'):
        if (folder / name).is_symlink() or not (folder / name).is_file() or not (folder / name).stat().st_size:
            raise ValueError('Missing or empty release file: ' + name)
    checksums = {}
    for line in small_file(folder / 'SHA256SUMS.txt', MAX_CHECKSUM_BYTES).decode('utf-8').splitlines():
        checksum, name = line.split('  ', 1)
        if not re.fullmatch(r'[0-9a-f]{64}', checksum) or name not in names:
            raise ValueError('Invalid release checksum record')
        if name in checksums:
            raise ValueError('Duplicate checksum: ' + name)
        checksums[name] = checksum
    actual = {name: digest(folder / name) for name in names}
    if checksums != actual:
        raise ValueError('Release checksums do not match the three downloads')
    normal = json_object(small_file(folder / 'build-info.json'), 'build-info.json')
    root = json_object(small_file(folder / 'root-build-info.json'), 'root-build-info.json')
    if (normal['apk_sha256'] != actual[names[0]] or
            root['installer_sha256'] != actual[names[1]] or
            root['module_sha256'] != actual[names[2]]):
        raise ValueError('Build receipts do not match the downloads')
    if not isinstance(root.get('target'), dict) or not isinstance(normal.get('game_version'), str) or not normal['game_version'].strip():
        raise ValueError('Invalid game target receipt')
    if (normal['game_version'] != root['target']['versionName'] or
            normal['game_version_code'] != root['target']['versionCode']):
        raise ValueError('Rooted and non-rooted game targets differ')
    if root['version'] != tag:
        raise ValueError('Rooted build version differs from release tag')
    with zipfile.ZipFile(folder / names[0]) as apk:
        checked_archive(apk)
        mode = json_object(small_entry(apk, 'assets/pgrazor/mode.json'), 'mode.json')
        if mode.get('pgrazorVersion') != tag:
            raise ValueError('Non-rooted build version differs from release tag')
    with zipfile.ZipFile(folder / names[2]) as module:
        checked_archive(module)
        props = unique_pairs(line.split('=', 1) for line in small_entry(module, 'module.prop').decode().splitlines()
                     if '=' in line)
        if props.get('id') != 'pgrazor' or props.get('version') != tag:
            raise ValueError('Module identity/version differs from release tag')
    with zipfile.ZipFile(folder / names[1]) as installer:
        checked_archive(installer)
        embedded = [name for name in installer.namelist() if name.startswith('assets/') and name.endswith('.zip')]
        if len(embedded) != 1:
            raise ValueError('Installer must contain exactly one module ZIP')
        with installer.open(embedded[0]) as stream:
            if hashlib.file_digest(stream, 'sha256').hexdigest() != actual[names[2]]:
                raise ValueError('Installer embeds a different module from the published ZIP')
    return actual


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('folder', type=Path)
    parser.add_argument('--tag', required=True)
    args = parser.parse_args()
    try:
        report = verify(args.folder, args.tag)
    except (OSError, ValueError, KeyError, TypeError, zipfile.BadZipFile) as error:
        parser.exit(1, 'Rejected: ' + str(error) + '\n')
    for name, checksum in report.items():
        print(checksum + '  ' + name)
