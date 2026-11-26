"""Adversarial & non-mutation tests for Gemini G-049-06 (DATA-06 / INV-01 asset audit).

Covers all 13 requirements of Despacho G-049-06:
real repo audit + non-mutation proof, adversarial fixtures (truncation, bad CRC,
extension mismatch, oversize/pixel bomb, duplicates, Unicode/case collisions,
symlink/traversal rejection, unknown provenance, coverage/fallback, deterministic
manifest & PT-BR report), and V5 white-layer untouched check.
"""
import hashlib
import os
from pathlib import Path
import struct
import sys
import tempfile
import unittest
import zlib

ROOT = Path(__file__).resolve().parents[2]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools.pgbr_asset_audit import auditor, image_probe


def _make_png(width: int, height: int, *, color_type: int = 6, corrupt_crc: bool = False, truncate_iend: bool = False) -> bytes:
    def chunk(ctype: bytes, data: bytes, bad_crc: bool = False) -> bytes:
        crc = zlib.crc32(ctype + data) & 0xFFFFFFFF
        if bad_crc:
            crc ^= 0x12345678
        return struct.pack(">I", len(data)) + ctype + data + struct.pack(">I", crc)

    ihdr = struct.pack(">IIBBBBB", width, height, 8, color_type, 0, 0, 0)
    # Minimal 1x1 scanline payload (valid deflate stream even if dimensions are synthetic for bomb test)
    idat = zlib.compress(b"\x00\x00\x00\x00\x00")
    out = image_probe.PNG_SIG + chunk(b"IHDR", ihdr) + chunk(b"IDAT", idat, bad_crc=corrupt_crc)
    if not truncate_iend:
        out += chunk(b"IEND", b"")
    return out


def _snapshot_tree_hashes(root: Path) -> dict[str, str]:
    snap: dict[str, str] = {}
    for p in sorted(root.rglob("*")):
        if p.is_file() and not p.is_symlink():
            snap[p.relative_to(root).as_posix()] = hashlib.sha256(p.read_bytes()).hexdigest()
    return snap


class TestGeminiG04906AssetAudit(unittest.TestCase):
    def test_real_repository_assets_audit_deterministic_and_non_mutating(self):
        assets_dir = ROOT / "pgrazor" / "assets"
        if not assets_dir.is_dir():
            self.skipTest("Partial checkout without pgrazor/assets")

        before = _snapshot_tree_hashes(assets_dir)
        res1 = auditor.audit_assets_tree(
            assets_dir,
            expected_item_ids=[1, 2, 3, 101, 701, 705],
            expected_dex_range=(1, 1025),
        )
        res2 = auditor.audit_assets_tree(
            assets_dir,
            expected_item_ids=[1, 2, 3, 101, 701, 705],
            expected_dex_range=(1, 1025),
        )
        after = _snapshot_tree_hashes(assets_dir)

        # (13) Strict non-mutation proof
        self.assertEqual(before, after, "Auditor must NEVER mutate any file in pgrazor/assets")
        self.assertFalse(res1["mutated"])
        self.assertFalse(res1["device_tested"])

        # Real inventory checks
        s = res1["summary"]
        self.assertGreater(s["image_count"], 1000)
        self.assertEqual(s["invalid_image_count"], 0, f"Invalid repo images: {res1['invalid_images']}")
        self.assertEqual(s["extension_mismatch_count"], 0)
        self.assertEqual(s["unsafe_path_count"], 0)
        self.assertEqual(s["unicode_collision_count"], 0)
        self.assertEqual(s["case_collision_count"], 0)

        # (10) Provenance: feed-icons has explicit NOTICE.txt + provenance.json
        self.assertEqual(res1["provenance_by_group"]["feed-icons"]["status"], "explicita")

        # (12) Deterministic manifest & PT-BR report
        m1 = auditor.build_deterministic_manifest(res1)
        m2 = auditor.build_deterministic_manifest(res2)
        self.assertEqual(m1["manifest_sha256"], m2["manifest_sha256"])
        report = auditor.render_ptbr_report(res1)
        self.assertIn("Relatório Offline de Auditoria", report)
        self.assertIn("mutated=false", report)

    def test_adversarial_corruption_mismatch_bounds_duplicates_and_collisions(self):
        with tempfile.TemporaryDirectory(prefix="pgbr-asset-adv-") as td:
            root = Path(td)
            grp = root / "custom-icons"
            grp.mkdir()

            valid_png = _make_png(32, 32, color_type=6)
            opaque_png = _make_png(16, 16, color_type=2)  # RGB without alpha
            bad_crc_png = _make_png(16, 16, corrupt_crc=True)
            trunc_png = _make_png(16, 16, truncate_iend=True)
            bomb_png = _make_png(9000, 9000)
            fake_jpeg_as_png = b"\xff\xd8\xff\xe0\x00\x10JFIF\x00\x01\x01\x00\x00\x01\x00\x01\x00\x00\xff\xd9"

            (grp / "ok.png").write_bytes(valid_png)
            (grp / "dup_of_ok.png").write_bytes(valid_png)  # binary duplicate
            (grp / "opaque.png").write_bytes(opaque_png)
            (grp / "bad_crc.png").write_bytes(bad_crc_png)
            (grp / "trunc.png").write_bytes(trunc_png)
            (grp / "bomb.png").write_bytes(bomb_png)
            (grp / "wrong_ext.png").write_bytes(fake_jpeg_as_png)
            (grp / "oversize.png").write_bytes(valid_png + b"\x00" * 4096)

            # Case collision (case-sensitive POSIX FS) & Unicode NFC/NFD collision
            (grp / "Icon.png").write_bytes(opaque_png)
            if not (grp / "icon.png").exists():
                (grp / "icon.png").write_bytes(opaque_png)
            nfc_name = "caf\u00e9.png"
            nfd_name = "cafe\u0301.png"
            (grp / nfc_name).write_bytes(valid_png)
            if not (grp / nfd_name).exists():
                (grp / nfd_name).write_bytes(valid_png)

            # Symlink escape test
            outside = root / "outside_secret.txt"
            outside.write_text("secret", encoding="utf-8")
            try:
                os.symlink(outside, grp / "escape_link.png")
                symlink_created = True
            except OSError:
                symlink_created = False

            limits = image_probe.DecodeLimits(max_file_bytes=2048, max_dimension=4096, max_pixels=4096 * 4096)
            res = auditor.audit_assets_tree(grp.parent, limits=limits)

            by_path = {e["path"]: e for e in res["entries"]}
            self.assertTrue(by_path["custom-icons/ok.png"]["valid"])
            self.assertTrue(by_path["custom-icons/ok.png"]["has_alpha"])
            self.assertTrue(by_path["custom-icons/opaque.png"]["valid"])
            self.assertFalse(by_path["custom-icons/opaque.png"]["has_alpha"])

            self.assertFalse(by_path["custom-icons/bad_crc.png"]["valid"])
            self.assertTrue(any("png_bad_crc" in err for err in by_path["custom-icons/bad_crc.png"]["errors"]))
            self.assertFalse(by_path["custom-icons/trunc.png"]["valid"])
            self.assertIn("png_truncated_no_iend", by_path["custom-icons/trunc.png"]["errors"])
            self.assertFalse(by_path["custom-icons/bomb.png"]["valid"])
            self.assertTrue(any("dimension_limit_exceeded" in err for err in by_path["custom-icons/bomb.png"]["errors"]))
            self.assertTrue(by_path["custom-icons/wrong_ext.png"]["extension_mismatch"])
            self.assertFalse(by_path["custom-icons/oversize.png"]["valid"])

            # Binary duplicates, case/Unicode collisions, symlink rejection, unknown provenance
            self.assertGreaterEqual(res["summary"]["duplicate_hash_groups"], 1)
            self.assertGreaterEqual(res["summary"]["case_collision_count"], 1)
            self.assertGreaterEqual(res["summary"]["unicode_collision_count"], 1)
            if symlink_created:
                self.assertGreaterEqual(res["summary"]["unsafe_path_count"], 1)
            self.assertEqual(res["provenance_by_group"]["custom-icons"]["status"], "desconhecida")

    def test_explicit_fallback_never_reuses_wrong_asset_and_v5_untouched(self):
        fb_hit = auditor.resolve_fallback_asset(1, {1, 2, 3})
        fb_miss = auditor.resolve_fallback_asset(99999, {1, 2, 3})
        self.assertTrue(fb_hit["found"] and not fb_hit["used_fallback"])
        self.assertFalse(fb_miss["found"])
        self.assertTrue(fb_miss["used_fallback"])
        self.assertEqual(fb_miss["resolved"], "FALLBACK_GENERIC_PLACEHOLDER")

        mini = ROOT / "pgrazor" / "native" / "rocket_minigame.cpp"
        if mini.is_file():
            src = mini.read_text(encoding="utf-8")
            self.assertIn("hide_canvas_white_layer", src)
            self.assertIn("enforce_white_layers", src)


if __name__ == "__main__":
    unittest.main()
