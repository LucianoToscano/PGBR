"""Compile and exercise PGBR's real local Pokémon live-feed matcher."""
import subprocess
import tempfile
import unittest
from pathlib import Path

from test_control_java import java_tools

ROOT = Path(__file__).resolve().parents[1]


class PokemonFeedSearchTests(unittest.TestCase):
    def test_matcher(self):
        compiler, runtime = java_tools()
        with tempfile.TemporaryDirectory(prefix="pgbr-pokemon-search-") as work:
            src = [ROOT / "src/dev/pgrazor/PokemonFeedSearch.java",
                   ROOT / "test/PokemonFeedSearchTest.java"]
            build = subprocess.run(
                [str(compiler), "-encoding", "UTF-8", "-d", work, *map(str, src)],
                capture_output=True, text=True, timeout=30)
            self.assertEqual(build.returncode, 0, build.stdout + build.stderr)
            run = subprocess.run(
                [str(runtime), "-cp", work, "dev.pgrazor.PokemonFeedSearchTest"],
                capture_output=True, text=True, timeout=30)
            self.assertEqual(run.returncode, 0, run.stdout + run.stderr)
            self.assertIn("PASS:", run.stdout)


if __name__ == "__main__":
    unittest.main()
