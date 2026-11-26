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


if __name__ == '__main__':
    unittest.main()
