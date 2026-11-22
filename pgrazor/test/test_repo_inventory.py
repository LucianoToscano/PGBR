"""Testes do inventário conservador: nenhuma operação de exclusão."""
import shutil
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[2]))
from tools.repo_inventory import category, concern, duplicate_groups, inventory


class RepoInventoryTests(unittest.TestCase):
    def test_categories_preserve_build_sources_and_assets(self):
        self.assertEqual(category("pgrazor/native/combat_host.cpp"), "codigo")
        self.assertEqual(category("pgrazor/assets/item-icons/item-701.png"), "assets")
        self.assertEqual(category("upstream-history/PGRazor-upstream-full.bundle"),
                         "historico_terceiros")
        self.assertEqual(category(".github/workflows/build.yml"), "ci_workflows")
        self.assertEqual(category("docs/BUILDING.md"), "documentacao")

    def test_sensitive_file_names_are_review_only(self):
        self.assertEqual(concern("dist/build.apk"), "binario_ou_material_sensivel")
        self.assertEqual(concern(".env.local"), "ambiente_local")
        self.assertEqual(concern("pgrazor/__pycache__/x.pyc"), "residuo_gerado")
        self.assertIsNone(concern("AGENTS.md"))
        self.assertIsNone(concern("pgrazor/vendor/NOTICE.txt"))

    def test_identical_files_are_reported_not_removed(self):
        records = [
            {"path": "src/a", "bytes": 2, "sha256": "sha"},
            {"path": "assets/a", "bytes": 2, "sha256": "sha"},
            {"path": "src/b", "bytes": 3, "sha256": "different"},
        ]
        self.assertEqual(duplicate_groups(records), [["assets/a", "src/a"]])

    @unittest.skipUnless(shutil.which("git"), "git não disponível")
    def test_inventory_does_not_modify_repository_files(self):
        with tempfile.TemporaryDirectory() as d:
            root = Path(d)
            subprocess.run(["git", "init", "-q", str(root)], check=True)
            a = root / "a.png"
            b = root / "b.png"
            a.write_bytes(b"same")
            b.write_bytes(b"same")
            subprocess.run(["git", "-C", str(root), "add", "a.png", "b.png"],
                           check=True)
            before = (a.read_bytes(), b.read_bytes())
            result = inventory(root)
            self.assertEqual(result["tracked"], 2)
            self.assertEqual(len(result["duplicates_identical"]), 1)
            self.assertEqual((a.read_bytes(), b.read_bytes()), before)


if __name__ == "__main__":
    unittest.main()
