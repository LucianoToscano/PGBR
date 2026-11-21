"""Governança: um único Arquivo Mestre e links de primeira leitura.

Este teste garante presença e referência nos pontos de entrada; não pode
provar que uma pessoa ou IA realmente executou a leitura.
"""
import unittest
from pathlib import Path

ROOT=Path(__file__).resolve().parents[2]
MASTER="PGBR_ARQUIVO_MESTRE_UNICO.md"

class MasterGovernanceTests(unittest.TestCase):
    def test_master_is_canonical(self):
        p=ROOT/MASTER
        self.assertTrue(p.is_file(), "Arquivo Mestre PGBR ausente")
        s=p.read_text(encoding="utf-8")
        for text in ("PGBR-MASTER-001", "LEITURA OBRIGATÓRIA", "Fonte oficial", "PokeSearch", "LT032", "21/11/2026"):
            with self.subTest(text=text):
                self.assertIn(text,s)
    def test_agent_entrypoints_reference_master(self):
        for rel in ("AGENTS.md", ".github/copilot-instructions.md", "README.md"):
            with self.subTest(path=rel):
                p=ROOT/rel
                self.assertTrue(p.is_file(), rel)
                self.assertIn(MASTER, p.read_text(encoding="utf-8"))
        agents=(ROOT/"AGENTS.md").read_text(encoding="utf-8")
        self.assertIn("ANTES",agents)
        self.assertIn("LEIA INTEGRALMENTE",agents)

if __name__=="__main__":
    unittest.main()
