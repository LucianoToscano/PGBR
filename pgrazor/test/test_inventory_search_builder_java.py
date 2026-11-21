"""Compile/test the production collection query builder on a plain JVM."""
from pathlib import Path
import subprocess
import tempfile
import unittest
from test_control_java import java_tools

ROOT = Path(__file__).resolve().parents[1]


class InventorySearchBuilderTests(unittest.TestCase):
    def test_queries_and_input_validation(self):
        javac, java = java_tools()
        files = [
            ROOT / "src/dev/pgrazor/PokemonInventorySearchBuilder.java",
            ROOT / "test/PokemonInventorySearchBuilderTest.java",
        ]
        with tempfile.TemporaryDirectory(prefix="pgbr-inventory-query-") as out:
            result = subprocess.run([str(javac), "-encoding", "UTF-8", "-d", out,
                                     *map(str, files)], capture_output=True, text=True, timeout=30)
            self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
            result = subprocess.run([str(java), "-cp", out,
                                     "dev.pgrazor.PokemonInventorySearchBuilderTest"],
                                    capture_output=True, text=True, timeout=30)
            self.assertEqual(result.returncode, 0, result.stdout + result.stderr)


if __name__ == "__main__":
    unittest.main()
