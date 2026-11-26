"""Adversarial & non-mutation tests for Gemini G-049-06 + G-049-07 (DATA-06 / INV-01).

Covers all G-049-06 and G-049-07 requirements:
- Symlinked audit root rejection
- Non-regular file (POSIX FIFO) rejection without blocking
- Bounded reads & no unbounded hashing on oversized files (sha256=None, never grouped as duplicates)
- Coverage counting only structurally valid matching-format images + single-use ID generator
- Provenance presence vs license validation distinction
- Alpha potential channel vs transparent pixel proof distinction
- Structural probe scope & SVG fail-closed without parser
- Deterministic manifest & strict non-mutation proof
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
    def test_binary_io_preserves_crlf_and_ctrl_z_bytes(self):
        with tempfile.TemporaryDirectory(prefix="pgbr-binary-io-") as td:
            path = Path(td) / "binary.png"
            payload = _make_png(1, 1)
            self.assertIn(b"\r\n\x1a\n", payload[:8])
            path.write_bytes(payload)
            observed, error, size = image_probe._read_bounded_regular(path, len(payload))
            self.assertIsNone(error)
            self.assertEqual(observed, payload)
            self.assertEqual(size, len(payload))
            self.assertEqual(image_probe.probe_image_file(path)["sha256"], hashlib.sha256(payload).hexdigest())
            self.assertTrue(image_probe.probe_image_file(path)["valid"])

    def test_real_repository_assets_audit_deterministic_and_non_mutating(self):
        assets_dir = ROOT / "pgrazor" / "assets"
        if not assets_dir.is_dir():
            self.skipTest("Partial checkout without pgrazor/assets")

        before = _snapshot_tree_hashes(assets_dir)
        res1 = auditor.audit_assets_tree(
            assets_dir,
            expected_item_ids=(i for i in (1, 2, 3, 101, 701, 705)),
            expected_dex_range=(1, 1025),
        )
        res2 = auditor.audit_assets_tree(
            assets_dir,
            expected_item_ids=(i for i in (1, 2, 3, 101, 701, 705)),
            expected_dex_range=(1, 1025),
        )
        after = _snapshot_tree_hashes(assets_dir)

        self.assertEqual(before, after, "Auditor must NEVER mutate any file in pgrazor/assets")
        self.assertFalse(res1["mutated"])
        self.assertFalse(res1["device_tested"])
        self.assertFalse(res1["pixel_decode_verified"])

        s = res1["summary"]
        self.assertGreater(s["image_count"], 1000)
        self.assertEqual(s["invalid_image_count"], 0, f"Invalid repo images: {res1['invalid_images']}")
        self.assertEqual(s["extension_mismatch_count"], 0)
        self.assertEqual(s["unsafe_path_count"], 0)
        self.assertEqual(s["unicode_collision_count"], 0)
        self.assertEqual(s["case_collision_count"], 0)

        # Presence of NOTICE/provenance != legal license validation
        feed_prov = res1["provenance_by_group"]["feed-icons"]
        self.assertEqual(feed_prov["status"], "arquivos_presentes_nao_validados")
        self.assertFalse(feed_prov["license_validated"])

        m1 = auditor.build_deterministic_manifest(res1)
        m2 = auditor.build_deterministic_manifest(res2)
        self.assertEqual(m1["manifest_sha256"], m2["manifest_sha256"])
        report = auditor.render_ptbr_report(res1)
        self.assertIn("probe estrutural", report)
        self.assertIn("mutated=false", report)

    def test_adversarial_corruption_mismatch_bounds_duplicates_and_collisions(self):
        with tempfile.TemporaryDirectory(prefix="pgbr-asset-adv-") as td:
            root = Path(td)
            grp = root / "custom-icons"
            grp.mkdir()

            valid_png = _make_png(32, 32, color_type=6)
            opaque_png = _make_png(16, 16, color_type=2)
            bad_crc_png = _make_png(16, 16, corrupt_crc=True)
            trunc_png = _make_png(16, 16, truncate_iend=True)
            bomb_png = _make_png(9000, 9000)
            fake_jpeg_as_png = b"\xff\xd8\xff\xe0\x00\x10JFIF\x00\x01\x01\x00\x00\x01\x00\x01\x00\x00\xff\xd9"

            (grp / "ok.png").write_bytes(valid_png)
            (grp / "dup_of_ok.png").write_bytes(valid_png)
            (grp / "opaque.png").write_bytes(opaque_png)
            (grp / "bad_crc.png").write_bytes(bad_crc_png)
            (grp / "trunc.png").write_bytes(trunc_png)
            (grp / "bomb.png").write_bytes(bomb_png)
            (grp / "wrong_ext.png").write_bytes(fake_jpeg_as_png)
            # Two distinct oversized files must NOT be hashed or grouped as duplicates!
            (grp / "oversize_a.png").write_bytes(valid_png + b"\x00" * 4096)
            (grp / "oversize_b.png").write_bytes(opaque_png + b"\x01" * 4096)
            # SVG fails closed without dedicated parser.
            (grp / "vector.svg").write_text('<svg xmlns="http://www.w3.org/2000/svg"></svg>', encoding="utf-8")

            (grp / "Icon.png").write_bytes(opaque_png)
            if not (grp / "icon.png").exists():
                (grp / "icon.png").write_bytes(opaque_png)
            nfc_name = "caf\u00e9.png"
            nfd_name = "cafe\u0301.png"
            (grp / nfc_name).write_bytes(valid_png)
            if not (grp / nfd_name).exists():
                (grp / nfd_name).write_bytes(valid_png)

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
            self.assertTrue(by_path["custom-icons/ok.png"]["alpha_channel_present"])
            self.assertFalse(by_path["custom-icons/ok.png"]["pixel_decode_verified"])
            self.assertTrue(by_path["custom-icons/opaque.png"]["valid"])
            self.assertFalse(by_path["custom-icons/opaque.png"]["alpha_channel_present"])

            self.assertFalse(by_path["custom-icons/bad_crc.png"]["valid"])
            self.assertFalse(by_path["custom-icons/trunc.png"]["valid"])
            self.assertFalse(by_path["custom-icons/bomb.png"]["valid"])
            self.assertTrue(by_path["custom-icons/wrong_ext.png"]["extension_mismatch"])

            # Oversized files: sha256 is None, hash_status explicit, NEVER grouped as duplicates
            self.assertIsNone(by_path["custom-icons/oversize_a.png"]["sha256"])
            self.assertEqual(by_path["custom-icons/oversize_a.png"]["hash_status"], "indisponivel_oversized")
            self.assertIsNone(by_path["custom-icons/oversize_b.png"]["sha256"])
            self.assertEqual(res["summary"]["unhashed_image_count"], 2)
            for dup in res["duplicate_groups"]:
                self.assertIsNotNone(dup["sha256"])
                self.assertNotIn("custom-icons/oversize_a.png", dup["paths"])
                self.assertNotIn("custom-icons/oversize_b.png", dup["paths"])

            # SVG fails closed without parser
            self.assertFalse(by_path["custom-icons/vector.svg"]["valid"])
            self.assertIn("svg_full_parser_not_implemented", by_path["custom-icons/vector.svg"]["errors"])

            self.assertGreaterEqual(res["summary"]["duplicate_hash_groups"], 1)
            self.assertGreaterEqual(res["summary"]["case_collision_count"], 1)
            self.assertGreaterEqual(res["summary"]["unicode_collision_count"], 1)
            if symlink_created:
                self.assertGreaterEqual(res["summary"]["unsafe_path_count"], 1)
            self.assertEqual(res["provenance_by_group"]["custom-icons"]["status"], "desconhecida")

    def test_g049_07_symlink_root_fifo_invalid_id_coverage_and_generator(self):
        with tempfile.TemporaryDirectory(prefix="pgbr-g049-07-") as td:
            base = Path(td)
            real_root = base / "assets"
            items = real_root / "item-icons"
            items.mkdir(parents=True)

            # (1) Symlinked audit root MUST be rejected
            link_root = base / "assets_link"
            try:
                os.symlink(real_root, link_root)
                with self.assertRaises(ValueError):
                    auditor.audit_assets_tree(link_root)
            except OSError:
                pass

            # (2) Non-regular file (FIFO) MUST be rejected into unsafe_paths without blocking
            fifo_path = items / "pipe.png"
            fifo_created = False
            if hasattr(os, "mkfifo"):
                try:
                    os.mkfifo(fifo_path)
                    fifo_created = True
                except OSError:
                    fifo_created = False

            # (6) & (7) Invalid image at existing ID (item-1.png) must NOT count as covered;
            # single-use generator (i for i in [1, 2, 3]) must work properly.
            (items / "item-1.png").write_bytes(_make_png(16, 16, corrupt_crc=True))
            (items / "item-2.png").write_bytes(_make_png(16, 16, color_type=6))

            before = _snapshot_tree_hashes(real_root)
            res1 = auditor.audit_assets_tree(real_root, expected_item_ids=(i for i in (1, 2, 3)))
            res2 = auditor.audit_assets_tree(real_root, expected_item_ids=(i for i in (1, 2, 3)))
            after = _snapshot_tree_hashes(real_root)

            # Non-mutation & determinism
            self.assertEqual(before, after)
            self.assertEqual(
                auditor.build_deterministic_manifest(res1)["manifest_sha256"],
                auditor.build_deterministic_manifest(res2)["manifest_sha256"],
            )

            cov = res1["coverage"]["item_icons"]
            self.assertEqual(cov["expected"], 3)
            self.assertEqual(cov["present"], 1)  # ONLY valid item-2
            self.assertEqual(cov["missing_ids"], [1, 3])
            self.assertEqual(cov["invalid_ids"], [1])

            if fifo_created:
                self.assertIn("non_regular_file:item-icons/pipe.png", res1["unsafe_paths"])

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
