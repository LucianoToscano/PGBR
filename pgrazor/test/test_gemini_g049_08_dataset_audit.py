"""Adversarial & non-mutation tests for Gemini G-049-08 (DATA-01 / DATA-02 dataset audit).

Covers all 12 deliverables of Despacho G-049-08:
real repo dataset discovery, classification, bounded regular-file hashing,
documented vs unknown source/revision/license/validity, declared vs inferred schema,
namespace ID collisions, duplicate JSON keys, missing references, deterministic
manifest without host paths, adversarial fixtures, PT-BR docs & non-mutation proof.
"""
from datetime import datetime, timezone
import hashlib
import json
import os
from pathlib import Path
import sys
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[2]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools.pgbr_dataset_audit import dataset_auditor


def _snapshot_json_hashes(root: Path) -> dict[str, str]:
    snap: dict[str, str] = {}
    for p in sorted(root.rglob("*.json")):
        if ".git" in p.parts or p.is_symlink() or not p.is_file():
            continue
        snap[p.relative_to(root).as_posix()] = hashlib.sha256(p.read_bytes()).hexdigest()
    return snap


class TestGeminiG04908DatasetAudit(unittest.TestCase):
    def test_binary_read_hash_preserves_crlf_and_ctrl_z(self):
        with tempfile.TemporaryDirectory() as td:
            path = Path(td) / "bytes.csv"
            payload = b"name,value\r\na,\x1a\r\n"
            path.write_bytes(payload)
            data, error, size = dataset_auditor._read_bounded_regular(path, len(payload))
            self.assertIsNone(error)
            self.assertEqual(data, payload)
            self.assertEqual(size, len(payload))

    def test_real_repository_datasets_deterministic_and_non_mutating(self):
        before = _snapshot_json_hashes(ROOT)
        now = datetime(2026, 11, 26, 4, 0, 0, tzinfo=timezone.utc)
        res1 = dataset_auditor.audit_embedded_datasets(
            ROOT,
            expected_references=[
                "pgrazor/i18n/pt-BR.json",
                "pgrazor/native/schema/gifts.json",
                "tools/game_input_0439_manifest.json",
            ],
            reference_now_utc=now,
        )
        res2 = dataset_auditor.audit_embedded_datasets(
            ROOT,
            expected_references=[
                "pgrazor/i18n/pt-BR.json",
                "pgrazor/native/schema/gifts.json",
                "tools/game_input_0439_manifest.json",
            ],
            reference_now_utc=now,
        )
        after = _snapshot_json_hashes(ROOT)

        # (12) Strict non-mutation proof
        self.assertEqual(before, after, "Dataset auditor must NEVER mutate repository files")
        self.assertFalse(res1["mutated"])
        self.assertFalse(res1["device_tested"])
        self.assertGreaterEqual(res1["summary"]["dataset_count"], 5)
        self.assertEqual(res1["summary"]["missing_reference_count"], 0)
        self.assertEqual(res1["summary"]["unsafe_path_count"], 0)

        # (10) Deterministic manifest & no host absolute paths leaked
        m1 = dataset_auditor.build_deterministic_manifest(res1)
        m2 = dataset_auditor.build_deterministic_manifest(res2)
        self.assertEqual(m1["manifest_sha256"], m2["manifest_sha256"])
        self.assertNotIn(str(ROOT.resolve()), m1["manifest_json"])

        report = dataset_auditor.render_ptbr_report(res1)
        self.assertIn("DATA-01 / DATA-02", report)
        self.assertIn("mutated=false", report)

        doc = ROOT / "docs" / "validation" / "PGBR_DATASET_AUDIT_GUIA_PTBR.md"
        self.assertTrue(doc.is_file())

    def test_adversarial_invalid_duplicate_missing_expired_and_special_files(self):
        with tempfile.TemporaryDirectory(prefix="pgbr-ds-adv-") as td:
            base = Path(td)
            repo = base / "repo"
            repo.mkdir()
            (repo / "NOTICE.txt").write_text("Attribution notice", encoding="utf-8")

            # Valid dataset with declared schema, source, revision, license and valid_until
            (repo / "valid.json").write_text(json.dumps({
                "schema_version": 2,
                "repository": "https://example.invalid/repo",
                "commit": "a" * 40,
                "license": "MIT",
                "updated_at": "2026-11-25T00:00:00Z",
                "valid_until": "2026-12-31T00:00:00Z",
                "items": [{"id": 1}, {"id": 2}],
            }), encoding="utf-8")

            # Expired dataset + namespace ID collision (id=7 duplicated)
            (repo / "expired_dup.json").write_text(json.dumps({
                "valid_until": "2025-01-01T00:00:00Z",
                "entries": [{"id": 7}, {"id": 7}],
            }), encoding="utf-8")

            # Duplicate JSON key
            (repo / "dup_key.json").write_text('{"a": 1, "a": 2}', encoding="utf-8")
            # Invalid JSON syntax
            (repo / "broken.json").write_text('{"unterminated": ', encoding="utf-8")
            # Oversized JSON (exceeds tight 256-byte limit)
            (repo / "oversize.json").write_text('{"pad": "' + ("x" * 512) + '"}', encoding="utf-8")

            # Symlink root rejection
            link_root = base / "repo_link"
            try:
                os.symlink(repo, link_root)
                with self.assertRaises(ValueError):
                    dataset_auditor.audit_embedded_datasets(link_root)
            except OSError:
                pass

            # Symlink file & FIFO inside repo
            outside = base / "secret.json"
            outside.write_text('{"secret": true}', encoding="utf-8")
            try:
                os.symlink(outside, repo / "link.json")
                symlink_ok = True
            except OSError:
                symlink_ok = False
            fifo_ok = False
            if hasattr(os, "mkfifo"):
                try:
                    os.mkfifo(repo / "pipe.json")
                    fifo_ok = True
                except OSError:
                    fifo_ok = False

            now = datetime(2026, 11, 26, 4, 0, 0, tzinfo=timezone.utc)
            res = dataset_auditor.audit_embedded_datasets(
                repo,
                limits=dataset_auditor.DatasetAuditLimits(max_file_bytes=300),
                expected_references=["valid.json", "missing_dataset.json"],
                reference_now_utc=now,
            )
            by_path = {d["path"]: d for d in res["datasets"]}

            # Valid dataset checks: declared vs inferred schema, license not legally validated
            v = by_path["valid.json"]
            self.assertTrue(v["valid"])
            self.assertEqual(v["declared_schema"], "schema_version=2")
            self.assertEqual(v["inferred_schema"]["root_type"], "object")
            self.assertEqual(v["documented_source"], "https://example.invalid/repo")
            self.assertEqual(v["license"]["status"], "explicita_nao_validada")
            self.assertFalse(v["license"]["license_validated"])
            self.assertEqual(v["temporal"]["validity_status"], "vigente")

            # Expired + ID collision
            ex = by_path["expired_dup.json"]
            self.assertEqual(ex["temporal"]["validity_status"], "expirado")
            self.assertEqual(ex["documented_source"], "desconhecida")
            self.assertEqual(ex["id_collisions"][0]["duplicate_ids"], ["7"])

            # Duplicate JSON key & broken JSON & oversized
            self.assertFalse(by_path["dup_key.json"]["valid"])
            self.assertEqual(by_path["dup_key.json"]["duplicate_json_keys"], ["a"])
            self.assertFalse(by_path["broken.json"]["valid"])
            self.assertIsNone(by_path["oversize.json"]["sha256"])
            self.assertEqual(by_path["oversize.json"]["hash_status"], "indisponivel_oversized")

            # Missing reference & unsafe special files
            self.assertEqual(res["inconsistencies"]["missing_references"], ["missing_dataset.json"])
            if symlink_ok:
                self.assertIn("symlink_file:link.json", res["unsafe_paths"])
            if fifo_ok:
                self.assertIn("non_regular_file:pipe.json", res["unsafe_paths"])

    def test_v5_white_layer_fix_untouched(self):
        mini = ROOT / "pgrazor" / "native" / "rocket_minigame.cpp"
        if mini.is_file():
            src = mini.read_text(encoding="utf-8")
            self.assertIn("hide_canvas_white_layer", src)
            self.assertIn("enforce_white_layers", src)


if __name__ == "__main__":
    unittest.main()
