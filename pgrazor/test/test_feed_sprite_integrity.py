"""Offline provenance checks for the feed sprites used by both package variants."""
import hashlib
import json
from pathlib import Path
import sys
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))
import build


class FeedSpriteIntegrityTests(unittest.TestCase):
    def test_repository_assets_match_manifest(self):
        self.assertTrue(build.feed_icon_assets(ROOT))

    def test_manifest_and_files(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            folder = root / 'assets/feed-icons'
            sprites = folder / 'pokemon'
            sprites.mkdir(parents=True)
            data = b'example'
            entry = {'dex': 25, 'bytes': len(data), 'sha256': hashlib.sha256(data).hexdigest()}
            manifest = {'commit': 'a'*40, 'repository': 'https://example.invalid', 'sprites': [entry]}
            def save():
                (folder / 'provenance.json').write_text(json.dumps(manifest), encoding='utf-8')
            save()
            file = sprites / '25.png'
            file.write_bytes(data)
            self.assertEqual({'25.png': file}, build.feed_icon_assets(root))
            file.write_bytes(b'changed')
            with self.assertRaisesRegex(ValueError, 'hash mismatch'):
                build.feed_icon_assets(root)
            file.write_bytes(b'x')
            with self.assertRaisesRegex(ValueError, 'size mismatch'):
                build.feed_icon_assets(root)
            file.unlink()
            with self.assertRaisesRegex(ValueError, 'Missing feed sprites'):
                build.feed_icon_assets(root)
            file.write_bytes(data)
            extra = sprites / '025.png'
            extra.write_bytes(data)
            with self.assertRaisesRegex(ValueError, 'Unlisted feed sprite'):
                build.feed_icon_assets(root)
            extra.unlink()
            manifest['sprites'].append(dict(entry))
            save()
            with self.assertRaisesRegex(ValueError, 'Duplicate feed sprite ID'):
                build.feed_icon_assets(root)
            manifest['sprites'] = [dict(entry, dex=True)]
            save()
            with self.assertRaisesRegex(ValueError, 'Invalid feed sprite ID'):
                build.feed_icon_assets(root)


    def test_invalid_manifest_schema(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            folder = root / 'assets/feed-icons'
            (folder / 'pokemon').mkdir(parents=True)
            path = folder / 'provenance.json'
            entry = {'dex': 25, 'bytes': 7, 'sha256': 'a'*64}
            valid = {'commit': 'a'*40, 'repository': 'https://example.invalid', 'sprites': [entry]}
            cases = [
                ([], 'manifest object'),
                (dict(valid, commit=None), 'revision missing'),
                (dict(valid, repository=[]), 'revision missing'),
                (dict(valid, repository='  '), 'revision missing'),
                (dict(valid, sprites={}), 'manifest list'),
                (dict(valid, sprites=[None]), 'manifest entry'),
                (dict(valid, sprites=[{}]), 'Invalid feed sprite ID'),
                (dict(valid, sprites=[dict(entry, bytes=True)]), 'Invalid feed sprite provenance'),
                (dict(valid, sprites=[dict(entry, sha256=123)]), 'Invalid feed sprite provenance'),
                (dict(valid, sprites=[{'dex': 25}]), 'Invalid feed sprite provenance'),
            ]
            for payload, message in cases:
                with self.subTest(payload=payload):
                    path.write_text(json.dumps(payload), encoding='utf-8')
                    with self.assertRaisesRegex(ValueError, message):
                        build.feed_icon_assets(root)
            path.write_text('{"sprites":[],"sprites":[]}', encoding='utf-8')
            with self.assertRaisesRegex(ValueError, 'Duplicate feed manifest key'):
                build.feed_icon_assets(root)

    def test_manifest_symlink_is_rejected(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            folder = root / 'assets/feed-icons'
            (folder / 'pokemon').mkdir(parents=True)
            target = root / 'external.json'
            target.write_text('{}', encoding='utf-8')
            try:
                (folder / 'provenance.json').symlink_to(target)
            except (OSError, NotImplementedError):
                self.skipTest('Symlink creation unavailable on this host')
            with self.assertRaisesRegex(ValueError, 'path is a symlink'):
                build.feed_icon_assets(root)


if __name__ == '__main__':
    unittest.main()
