"""Run the real World Map zoom arithmetic on a JVM (no Android, no device).

The map view had the zoom bounds (0..19) and the pinch floor/size written three times
(buttons, pinch, world view). The bounds now live in MapOptics, and this test executes it.
"""
from pathlib import Path
import subprocess
import tempfile
import unittest

from test_control_java import java_tools

ROOT = Path(__file__).resolve().parents[1]


class MapOpticsJavaTests(unittest.TestCase):
    def test_levels_and_scales(self):
        compiler, runtime = java_tools()
        with tempfile.TemporaryDirectory(prefix='pgrazor-map-optics-') as directory:
            work = Path(directory)
            sources = [ROOT / 'src/dev/pgrazor/MapProjection.java',
                       ROOT / 'src/dev/pgrazor/MapOptics.java',
                       ROOT / 'test/MapOpticsTest.java']
            compiled = subprocess.run([str(compiler), '-encoding', 'UTF-8', '-d', str(work), *map(str, sources)],
                                      capture_output=True, text=True, timeout=60)
            self.assertEqual(compiled.returncode, 0, compiled.stdout + compiled.stderr)
            result = subprocess.run([str(runtime), '-cp', str(work), 'MapOpticsTest'],
                                    capture_output=True, text=True, timeout=60)
            self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
            self.assertIn('PASS: MapOptics', result.stdout)

    def test_view_uses_the_shared_bounds(self):
        """The view must not keep its own copies of the zoom limits."""
        dialog = (ROOT / 'src/dev/pgrazor/WorldMapDialog.java').read_text(encoding='utf-8')
        self.assertIn('MapOptics.step(zoom, amount)', dialog)
        self.assertIn('MapOptics.levelFor(next,tileSize,minimum)', dialog)
        self.assertIn('MapOptics.scaleFor(next,tileSize,zoom,minimum)', dialog)
        self.assertIn('MapOptics.clampWorld(old*detector.getScaleFactor(),tileSize,minimum)', dialog)
        self.assertNotIn('Math.pow(2,19)', dialog, 'limite de zoom duplicado na view')
        self.assertNotIn('Math.min(19', dialog, 'limite de zoom duplicado na view')

    def test_limits_match_the_tile_source(self):
        """The deepest level the view asks for is the depth the tile URL policy expects."""
        source = (ROOT / 'src/dev/pgrazor/MapTileSource.java').read_text(encoding='utf-8')
        self.assertIn('/" + tile.zoom + "/', source)
        java = (ROOT / 'src/dev/pgrazor/MapOptics.java').read_text(encoding='utf-8')
        self.assertIn('public static final int MAX_ZOOM = 19', java)
        self.assertIn('public static final int MIN_ZOOM = 0', java)


if __name__ == '__main__':
    unittest.main()
