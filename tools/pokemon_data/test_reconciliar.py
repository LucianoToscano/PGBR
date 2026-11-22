"""Testes locais do reconciliador, sem dependências de terceiros."""
import json
import tempfile
import unittest
from pathlib import Path
from reconciliar import reconcile


class ReconciliadorTests(unittest.TestCase):
    def test_conserva_originais_e_quarentena(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            pkm = root / 'pogo_pkm.json'
            gm = root / 'latest.json'
            original = [
                {'id': 1, 'form': 'Normal', 'stats': {'a': 1}},
                {'id': 1, 'form': 'Normal', 'stats': {'a': 1}},
                {'id': 2, 'form': 'Normal', 'stats': {'a': 2}},
                {'id': 2, 'form': 'Normal', 'stats': {'a': 3}},
                {'bad': 'missing identifier'},
            ]
            pkm.write_text(json.dumps(original), encoding='utf-8')
            gm.write_text(json.dumps([{'templateId': 'ABC', 'data': {}}]), encoding='utf-8')
            before = pkm.read_bytes()
            stats = reconcile(pkm, gm, root / 'output')['stats']
            self.assertEqual(stats['pokemon_unique_keys'], 2)
            self.assertEqual(stats['pokemon_duplicate_groups'], 2)
            self.assertEqual(stats['pokemon_excess_duplicate_rows'], 2)
            self.assertEqual(stats['pokemon_confirmed_keys'], 1)
            self.assertEqual(stats['pokemon_conflict_groups'], 1)
            self.assertEqual(stats['pokemon_invalid_rows'], 1)
            self.assertEqual(pkm.read_bytes(), before)
            conflicts = json.loads((root / 'output' / 'pokemon_conflitos.json').read_text())
            self.assertEqual(len(conflicts[0]['variants']), 2)

    def test_game_master_duplicate_template_rejected(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            pkm = root / 'pokemon.json'
            gm = root / 'gm.json'
            pkm.write_text('[]')
            gm.write_text('[{"templateId":"X"},{"templateId":"X"}]')
            with self.assertRaisesRegex(ValueError, 'templateId duplicados'):
                reconcile(pkm, gm, root / 'output')


if __name__ == '__main__':
    unittest.main()
