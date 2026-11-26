"""Adversarial offline tests for PGBR Research & Rewards Catalog (G-049-10 / EVT-02)."""

from __future__ import annotations

import hashlib
import json
import os
import tempfile
import unittest
from datetime import datetime, timezone
from pathlib import Path

from tools.pgbr_research_catalog import (
    MAX_EXPORT_BYTES,
    ResearchCatalog,
    ResearchCondition,
    ResearchEntry,
    ResearchManualChecklist,
    ResearchProvenance,
    ResearchReward,
    assess_availability,
    build_synthetic_example_catalog,
    export_catalog_file,
    import_catalog_file,
    inspect_local_research_sources,
    normalize_category,
    sanitize_research_payload,
)

REPO_ROOT = Path(__file__).resolve().parents[2]


def _sha256_file(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


class ResearchCatalogAdversarialTests(unittest.TestCase):
    def test_local_inventory_reports_evidence_limits_and_zero_mutation(self) -> None:
        rp = REPO_ROOT / "pgrazor" / "src" / "dev" / "pgrazor" / "ResearchPreferences.java"
        names = REPO_ROOT / "pgrazor" / "assets" / "map-icons" / "pokemon-names.json"
        before_rp = _sha256_file(rp) if rp.is_file() else None
        before_names = _sha256_file(names) if names.is_file() else None

        inv = inspect_local_research_sources(REPO_ROOT)
        self.assertTrue(inv.repo_root_checked)
        self.assertTrue(inv.research_preferences_present)
        self.assertEqual(inv.research_preferences_format_version, "2")
        self.assertIn("i:item", inv.research_preferences_categories)
        self.assertIn("e:encounter", inv.research_preferences_categories)
        self.assertIn("v:event", inv.research_preferences_categories)
        self.assertFalse(inv.authorized_embedded_research_dataset_present)
        self.assertEqual(inv.supported_local_research_types, ("campo", "temporaria", "especial", "colecao"))
        self.assertGreaterEqual(len(inv.evidence_limits_ptbr), 3)

        if before_rp is not None:
            self.assertEqual(_sha256_file(rp), before_rp)
        if before_names is not None:
            self.assertEqual(_sha256_file(names), before_names)

    def test_types_quantities_and_unknown_rewards_validation(self) -> None:
        with self.assertRaises(ValueError):
            ResearchReward(kind="invalido", display_name_ptbr="Teste")
        for bad_qty in (0, -5, True, False, 1.5, float("nan"), float("inf")):
            with self.assertRaises(ValueError):
                ResearchReward(kind="item", display_name_ptbr="Bola", quantity=bad_qty)  # type: ignore[arg-type]
        with self.assertRaises(ValueError):
            ResearchReward(kind="item", display_name_ptbr="Bola", quantity_min=5, quantity_max=3)
        with self.assertRaises(ValueError):
            ResearchReward(kind="item", display_name_ptbr="Bola", quantity=2, quantity_min=1, quantity_max=3)
        with self.assertRaises(ValueError):
            ResearchCondition(code="BAD CODE!", declared_label_ptbr="Inválido")
        with self.assertRaises(ValueError):
            ResearchCondition(code="CATCH_X", declared_label_ptbr="Inválido", target_count=True)  # type: ignore[arg-type]

        unk = ResearchReward(kind="desconhecido", display_name_ptbr="Desconhecida", is_unknown=False)
        self.assertTrue(unk.is_unknown)

    def test_dates_availability_separation_and_duplicate_unknown_ids(self) -> None:
        with self.assertRaises(ValueError):
            ResearchProvenance(
                source_name="SYNTH",
                source_type="sintetico_exemplo",
                revision="1",
                valid_from_utc="2026-06-01T10:00:00",  # naive timestamp rejected
            )
        with self.assertRaises(ValueError):
            ResearchProvenance(
                source_name="SYNTH",
                source_type="sintetico_exemplo",
                revision="1",
                valid_from_utc="2026-06-05T00:00:00Z",
                valid_until_utc="2026-06-01T00:00:00Z",  # inverted window rejected
            )

        cat = build_synthetic_example_catalog()
        with self.assertRaises(ValueError):
            cat.add(cat.get("SYNTH_FIELD_001"))  # duplicate rejected
        with self.assertRaises(KeyError):
            cat.get("NON_EXISTENT_ID")

        ref_check = cat.validate_references(["SYNTH_FIELD_001", "SYNTH_FIELD_001", "MISSING_99", "bad id!"])
        self.assertFalse(ref_check["ok"])
        self.assertEqual(ref_check["duplicates"], ["SYNTH_FIELD_001"])
        self.assertEqual(ref_check["unknown_ids"], ["MISSING_99"])
        self.assertEqual(ref_check["invalid_ids"], ["bad id!"])

        timed = cat.get("SYNTH_TIMED_001")
        inside = assess_availability(timed, now_utc=datetime(2026, 6, 1, 15, 0, tzinfo=timezone.utc))
        self.assertTrue(inside.documented)
        self.assertFalse(inside.live_availability_confirmed)
        self.assertEqual(inside.window_status, "dentro_da_janela_declarada")

        expired = assess_availability(timed, now_utc=datetime(2026, 6, 3, 0, 0, tzinfo=timezone.utc))
        self.assertFalse(expired.live_availability_confirmed)
        self.assertEqual(expired.window_status, "expirada_por_janela")

        with self.assertRaises(ValueError):
            assess_availability(timed, now_utc=datetime(2026, 6, 1, 15, 0))  # naive now_utc rejected

    def test_unicode_ptbr_search_and_manual_checklist(self) -> None:
        cat = build_synthetic_example_catalog()
        self.assertEqual(normalize_category("Temporária"), "temporaria")
        self.assertEqual(normalize_category("COLEÇÃO"), "colecao")

        # NBSP + narrow NBSP + zero-width space + accent-insensitive query
        res = cat.search(category="temporária", query="\u00a0JOGADAS\u202fOTIMAS\u200b")
        self.assertEqual([e.research_id for e in res], ["SYNTH_TIMED_001"])

        by_species = cat.search(reward_kind="especie", reward_target="pikachu")
        self.assertEqual([e.research_id for e in by_species], ["SYNTH_TIMED_001"])

        by_item = cat.search(category="coleção", reward_target="ultra\u00a0bola")
        self.assertEqual([e.research_id for e in by_item], ["SYNTH_COLLECTION_001"])

        checklist = ResearchManualChecklist(cat)
        checklist.track("SYNTH_FIELD_001", window_key="w1", priority=1)
        checklist.confirm_state("SYNTH_FIELD_001", "concluido")
        self.assertEqual(checklist.summary()["concluido"], 1)
        self.assertEqual(checklist.reset_window("w1"), 1)
        self.assertEqual(checklist.summary()["pendente"], 1)

        with self.assertRaises(KeyError):
            checklist.track("UNKNOWN_TASK")
        with self.assertRaises(ValueError):
            checklist.confirm_state("SYNTH_FIELD_001", "auto_completado_pelo_jogo")

    def test_sanitization_bounded_import_export_and_symlink_rejection(self) -> None:
        dirty = {
            "trainer_name": "AshReal",
            "api_token": "secret123",
            "lat": -5.915,
            "notes_ptbr": (
                "Contato user@example.com com Bearer abcdef1234567890 em "
                "-5.9154, -35.2631 arquivo /Users/luciano/segredo.txt"
            ),
        }
        clean = sanitize_research_payload(dirty)
        self.assertEqual(clean["trainer_name"], "[REDACTED]")
        self.assertEqual(clean["api_token"], "[REDACTED]")
        self.assertEqual(clean["lat"], "[REDACTED]")
        self.assertNotIn("user@example.com", clean["notes_ptbr"])
        self.assertNotIn("abcdef1234567890", clean["notes_ptbr"])
        self.assertNotIn("-5.9154", clean["notes_ptbr"])
        self.assertNotIn("/Users/luciano", clean["notes_ptbr"])

        cat = build_synthetic_example_catalog()
        with tempfile.TemporaryDirectory() as tmp:
            tmp_dir = Path(tmp)
            out_file = tmp_dir / "catalog.json"
            written = export_catalog_file(cat, out_file)
            self.assertGreater(written, 0)
            self.assertLessEqual(written, MAX_EXPORT_BYTES)

            reloaded = import_catalog_file(out_file)
            self.assertEqual(len(reloaded), len(cat))
            self.assertEqual(reloaded.get("SYNTH_FIELD_001").category, "campo")

            # Oversized file rejected cleanly
            big_file = tmp_dir / "big.json"
            big_file.write_bytes(b" " * 2048)
            with self.assertRaises(ValueError):
                import_catalog_file(big_file, max_bytes=512)

            # Symlink rejected
            link = tmp_dir / "link.json"
            os.symlink(out_file, link)
            with self.assertRaises(ValueError):
                import_catalog_file(link)
            with self.assertRaises(ValueError):
                export_catalog_file(cat, link)

            # NaN JSON rejected
            bad_json = json.dumps({"schema_version": 1, "entries": []}).replace("[]", "[NaN]")
            with self.assertRaises(ValueError):
                ResearchCatalog.from_json(bad_json)


if __name__ == "__main__":
    unittest.main()
