"""Adversarial & real-repo tests for P0 Visual Identity Icon Audit (G-050-02 / LT050)."""

from __future__ import annotations

import hashlib
import os
import tempfile
import unittest
from pathlib import Path

from tools.pgbr_visual_identity_audit import (
    IconIdentityRecord,
    audit_visual_identity,
    classify_p0_category,
    detect_duplicate_image_hashes,
    format_receipt_text,
    verify_png_integrity_bytes,
)

REPO_ROOT = Path(__file__).resolve().parents[2]


def _sha256_file(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


class TestGeminiG05002VisualIdentityAudit(unittest.TestCase):
    def test_real_repo_audit_detects_known_p0_gaps_without_mutating_assets(self) -> None:
        sample_icon = REPO_ROOT / "pgrazor" / "assets" / "item-icons" / "item-401.png"
        stardust = REPO_ROOT / "pgrazor" / "assets" / "map-icons" / "stardust.png"
        before_401 = _sha256_file(sample_icon)
        before_dust = _sha256_file(stardust)

        report = audit_visual_identity(REPO_ROOT)

        self.assertEqual(report.total_item_icons_on_disk, 91)
        self.assertEqual(report.total_named_expansion_icons, 2)
        self.assertEqual(report.gallery_missing_from_disk, ())
        self.assertEqual(report.disk_missing_from_gallery, ())

        # Confirmed P0 findings on origin/main
        self.assertIn(401, report.manifest_overlap_ids)
        self.assertIn(401, report.stale_reference_blob_ids)
        self.assertIn("pgrazor/assets/map-icons/stardust.png", report.named_missing_from_map_provenance)
        self.assertIn(
            "pgrazor/assets/map-icons/daily-adventure-incense.png",
            report.named_missing_from_map_provenance,
        )

        # Must NEVER conflate PNG integrity with confirmed visual identity or 0.439.0 runtime
        self.assertTrue(all(r.png_integrity_ok for r in report.records))
        self.assertTrue(all(not r.visual_identity_confirmed for r in report.records))
        self.assertTrue(all(not r.runtime_0439_verified for r in report.records))

        # Status breakdown matches real repo manifests
        self.assertEqual(report.counts_by_visual_status.get("byte_identical_named_ref_unverified_runtime"), 3)
        self.assertEqual(report.counts_by_visual_status.get("visual_match_distinct_bytes_unverified_runtime"), 7)
        self.assertEqual(report.counts_by_visual_status.get("numeric_ref_only_unverified_semantic_identity"), 26)
        self.assertEqual(report.counts_by_visual_status.get("expansion_historical_enum_unverified_runtime"), 55)
        self.assertEqual(report.counts_by_visual_status.get("named_asset_unlisted_in_map_provenance"), 2)

        # Priority P0 categories all represented
        for cat in ("poeira_estelar", "incensos", "modulos", "mt", "evolucao", "frutas", "pokebolas", "pocoes"):
            self.assertIn(cat, report.counts_by_p0_category)

        receipt = format_receipt_text(report)
        self.assertIn("item:401", receipt)
        self.assertIn("item:701", receipt)
        self.assertIn("item:1201", receipt)
        self.assertIn("named:stardust.png", receipt)

        # Strictly read-only: zero mutation
        self.assertEqual(_sha256_file(sample_icon), before_401)
        self.assertEqual(_sha256_file(stardust), before_dust)

    def test_duplicate_hash_collision_and_corrupted_png_detection(self) -> None:
        ok, dims, msg = verify_png_integrity_bytes(b"not a png at all")
        self.assertFalse(ok)
        self.assertIsNone(dims)
        self.assertTrue(msg)

        # Synthetic collision between two distinct item IDs must be flagged
        r1 = IconIdentityRecord(
            key="item:701",
            item_id=701,
            asset_path="pgrazor/assets/item-icons/item-701.png",
            p0_category="frutas",
            ui_label_ptbr="Fruta Frambo",
            manifest_source="LT046_ITEM_ICON_REFERENCE",
            source_filename="Item_0701.png",
            source_scope_note="synthetic",
            bytes_on_disk=100,
            sha256="a" * 64,
            git_blob_sha1="b" * 40,
            dimensions=(64, 64),
            png_integrity_ok=True,
            manifest_blob_match=True,
            stale_reference_blob_detected=False,
            visual_identity_status="numeric_ref_only_unverified_semantic_identity",
            visual_identity_confirmed=False,
            runtime_0439_verified=False,
        )
        r2 = IconIdentityRecord(
            key="item:703",
            item_id=703,
            asset_path="pgrazor/assets/item-icons/item-703.png",
            p0_category="frutas",
            ui_label_ptbr="Fruta Anaba",
            manifest_source="LT046_ITEM_ICON_REFERENCE",
            source_filename="Item_0703.png",
            source_scope_note="synthetic",
            bytes_on_disk=100,
            sha256="a" * 64,
            git_blob_sha1="b" * 40,
            dimensions=(64, 64),
            png_integrity_ok=True,
            manifest_blob_match=True,
            stale_reference_blob_detected=False,
            visual_identity_status="numeric_ref_only_unverified_semantic_identity",
            visual_identity_confirmed=False,
            runtime_0439_verified=False,
        )
        collisions = detect_duplicate_image_hashes([r1, r2])
        self.assertEqual(len(collisions), 1)
        self.assertEqual(collisions[0][1], ("item:701", "item:703"))

    def test_category_classification_and_symlink_manifest_rejected(self) -> None:
        self.assertEqual(classify_p0_category(None, "pgrazor/assets/map-icons/stardust.png"), "poeira_estelar")
        self.assertEqual(classify_p0_category(401), "incensos")
        self.assertEqual(classify_p0_category(501), "modulos")
        self.assertEqual(classify_p0_category(1201), "mt")
        self.assertEqual(classify_p0_category(1106), "evolucao")
        self.assertEqual(classify_p0_category(701), "frutas")
        self.assertEqual(classify_p0_category(1), "pokebolas")
        self.assertEqual(classify_p0_category(101), "pocoes")

        with tempfile.TemporaryDirectory() as tmp:
            fake_root = Path(tmp)
            val_dir = fake_root / "docs" / "validation"
            val_dir.mkdir(parents=True)
            real_json = val_dir / "real.json"
            real_json.write_text('{"items": []}', encoding="utf-8")
            os.symlink(real_json, val_dir / "LT046_ITEM_ICON_REFERENCE.json")
            (val_dir / "LT046_ITEM_ICON_EXPANSION.json").write_text('{"items": [], "named": []}', encoding="utf-8")
            with self.assertRaises(ValueError):
                audit_visual_identity(fake_root)


if __name__ == "__main__":
    unittest.main()
