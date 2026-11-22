"""Governança: um único Arquivo Mestre e links de primeira leitura.

Este teste garante presença e referência nos pontos de entrada; não pode
provar que uma pessoa ou IA realmente executou a leitura.
"""
import re
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
        self.assertIn("antes",agents.lower())
        self.assertIn("LEIA INTEGRALMENTE",agents)
    def test_local_master_links_and_agent_docs_exist(self):
        master_text = (ROOT / MASTER).read_text(encoding="utf-8")
        for target in re.findall(r"\]\(([^\)]+)\)", master_text):
            local = target.split("#", 1)[0]
            if not local or local.startswith(("https://", "http://", "mailto:")):
                continue
            with self.subTest(master_link=target):
                self.assertTrue((ROOT / local).is_file(), local)

        agents = (ROOT / "AGENTS.md").read_text(encoding="utf-8")
        for local in re.findall(r"`((?:docs/|AUDITORIA_)[^`]+\.md)`", agents):
            with self.subTest(agent_reference=local):
                self.assertTrue((ROOT / local).is_file(), local)

    def test_single_master_and_contributor_entrypoints(self):
        matches = sorted(p.name for p in ROOT.glob("PGBR*ARQUIVO*MESTRE*.md"))
        self.assertEqual(matches, [MASTER])
        for rel in ("CONTRIBUTING.md", ".github/PULL_REQUEST_TEMPLATE.md"):
            with self.subTest(entrypoint=rel):
                self.assertIn(MASTER, (ROOT / rel).read_text(encoding="utf-8"))


if __name__=="__main__":
    unittest.main()
