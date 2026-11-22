"""Revisão estática do patch ART ARM64 isolado; não equivale a teste em dispositivo."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]

class ArtHookChainSourceTests(unittest.TestCase):
    def test_art_relocation_is_private_and_guarded(self):
        cmake = (ROOT / "art/CMakeLists.txt").read_text(encoding="utf-8")
        fixture = (ROOT / "art/tests/hook_chain_test.cpp").read_text(encoding="utf-8")
        self.assertIn("dobby_routing_old", cmake)
        self.assertIn("GetTrampolineBuffer()->GetBufferSize()", cmake)
        self.assertIn("if (tramp_size < 16) tramp_size = 16;", cmake)
        self.assertIn("Pinned Dobby routing changed", cmake)
        self.assertIn('dobby-art-routing.cc', cmake)
        self.assertIn('PGRAZOR_ART_CHAIN_TEST', cmake)
        self.assertIn('if(NOT ANDROID_ABI STREQUAL "arm64-v8a")', cmake)
        self.assertIn('add_executable(pgrazor_art_chain_test tests/hook_chain_test.cpp)', cmake)
        self.assertIn('DobbyHook(', fixture)
        self.assertIn('absolute_jump[4]', fixture)
        self.assertIn('backup(5)', fixture)
