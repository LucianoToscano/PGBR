"""Adversarial & non-mutation tests for Gemini G-049-08 + G-049-09 (DATA-01 / DATA-02).

Preserves ChatGPT's O_BINARY CRLF/Ctrl-Z regression test and covers all 12
hardening items of Despacho G-049-09.
"""
from datetime import datetime, timezone
import hashlib
import json
import os
from pathlib import Path
import sys
import tempfile
import unittest
from unittest import mock

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
        if not (ROOT / "pgrazor" / "i18n" / "pt-BR.json").is_file():
            self.skipTest("Partial checkout without embedded repository datasets")
        before = _snapshot_json_hashes(ROOT)
        now = datetime(2026, 11, 26, 4, 0, 0, tzinfo=timezone.utc)
        refs = [
            "pgrazor/i18n/pt-BR.json",
            "pgrazor/native/schema/gifts.json",
            "tools/game_input_0439_manifest.json",
        ]
        res1 = dataset_auditor.audit_embedded_datasets(ROOT, expected_references=refs, reference_now_utc=now)
        res2 = dataset_auditor.audit_embedded_datasets(ROOT, expected_references=refs, reference_now_utc=now)
        after = _snapshot_json_hashes(ROOT)

        self.assertEqual(before, after, "Dataset auditor must NEVER mutate repository files")
        self.assertFalse(res1["mutated"])
        self.assertFalse(res1["device_tested"])
        self.assertGreaterEqual(res1["summary"]["dataset_count"], 5)
        self.assertEqual(res1["summary"]["missing_reference_count"], 0)
        self.assertEqual(res1["summary"]["unsafe_path_count"], 0)

        m1 = dataset_auditor.build_deterministic_manifest(res1)
        m2 = dataset_auditor.build_deterministic_manifest(res2)
        self.assertEqual(m1["manifest_sha256"], m2["manifest_sha256"])
        self.assertNotIn(str(ROOT.resolve()), m1["manifest_json"])

        report = dataset_auditor.render_ptbr_report(res1)
        self.assertIn("DATA-01 / DATA-02", report)
        self.assertIn("mutated=false", report)
        self.assertTrue((ROOT / "docs" / "validation" / "PGBR_DATASET_AUDIT_GUIA_PTBR.md").is_file())

    def test_g049_09_json_nan_depth_csv_bounds_privacy_naive_dates_and_oserror(self):
        with tempfile.TemporaryDirectory(prefix="pgbr-ds-g049-09-") as td:
            base = Path(td)
            repo = base / "repo"
            repo.mkdir()
            (repo / "NOTICE.txt").write_text("Attribution notice", encoding="utf-8")

            # (1) NaN / Infinity rejected
            (repo / "nan_val.json").write_text('{"x": NaN}', encoding="utf-8")
            (repo / "inf_val.json").write_text('{"x": Infinity}', encoding="utf-8")

            # (2) Deeply nested JSON exceeding max_json_depth handled without crashing inventory
            deep = "[" * 20 + "1" + "]" * 20
            (repo / "deep.json").write_text(deep, encoding="utf-8")

            # (3) Strict incremental CSV: ragged columns & row limit
            (repo / "ragged.csv").write_text("col_secret_a,col_secret_b\n1,2\n3\n", encoding="utf-8")
            (repo / "many_rows.tsv").write_text("c1\tc2\n" + "1\t2\n" * 12, encoding="utf-8")

            # (4) & (5) Personal keys, raw IDs, credentialed URLs MUST NOT appear in manifest
            (repo / "redaction_fixture.json").write_text(json.dumps({
                "schema_version": 2,
                "repository": "https://user:superpass@example.com/repo?token=secret_tok_999",
                "license": "ProprietarySecretLicenseName",
                "personal_trainer_key": "AshSecretName",
                "entries": [{"id": "SECRET_ID_1"}, {"id": "SECRET_ID_1"}, {"id": "SECRET_ID_2"}, {"id": "SECRET_ID_3"}],
            }), encoding="utf-8")

            # (6) Local user/backup/secret files excluded without leaking filenames
            (repo / "user_backup_2026.json").write_text('{"trainer": "hidden"}', encoding="utf-8")
            (repo / "account_session.json").write_text('{"token": "hidden"}', encoding="utf-8")

            # (8) Naive date without timezone offset -> validade_ambigua_sem_fuso (NEVER assumed UTC)
            (repo / "naive_date.json").write_text(json.dumps({
                "updated_at": "2026-11-25T12:00:00",
                "valid_until": "2026-12-31T23:59:59",
            }), encoding="utf-8")

            # Valid public metadata
            (repo / "public_ok.json").write_text(json.dumps({
                "schema_version": 1,
                "repository": "https://github.com/LucianoToscano/PGBR",
                "commit": "a" * 40,
                "license": "MIT",
                "valid_until": "2026-12-31T00:00:00Z",
            }), encoding="utf-8")

            # (10) File that triggers OSError on read must not crash or expose host path
            (repo / "unreadable.json").write_text('{"ok": true}', encoding="utf-8")

            now = datetime(2026, 11, 26, 4, 0, 0, tzinfo=timezone.utc)
            limits = dataset_auditor.DatasetAuditLimits(
                max_file_bytes=4096,
                max_json_depth=10,
                max_id_records=2,  # forces partial_coverage on redaction_fixture.json (4 records)
                max_csv_rows=5,
                max_csv_columns=10,
                max_csv_field_chars=64,
            )

            orig_read = dataset_auditor._read_bounded_regular

            def _flaky_read(path: Path, max_bytes: int):
                if path.name == "unreadable.json":
                    return None, "io_error:PermissionError", 0
                return orig_read(path, max_bytes)

            before = _snapshot_json_hashes(repo)
            with mock.patch.object(dataset_auditor, "_read_bounded_regular", side_effect=_flaky_read):
                res1 = dataset_auditor.audit_embedded_datasets(
                    repo,
                    limits=limits,
                    expected_references=[
                        "public_ok.json",
                        "missing_ok.json",
                        "../etc/passwd",
                        "/abs/host/path.json",
                        "C:\\Windows\\secret.json",
                    ],
                    reference_now_utc=now,
                )
                res2 = dataset_auditor.audit_embedded_datasets(
                    repo,
                    limits=limits,
                    expected_references=[
                        "public_ok.json",
                        "missing_ok.json",
                        "../etc/passwd",
                        "/abs/host/path.json",
                        "C:\\Windows\\secret.json",
                    ],
                    reference_now_utc=now,
                )
            after = _snapshot_json_hashes(repo)
            self.assertEqual(before, after)

            m1 = dataset_auditor.build_deterministic_manifest(res1)
            m2 = dataset_auditor.build_deterministic_manifest(res2)
            self.assertEqual(m1["manifest_sha256"], m2["manifest_sha256"])

            # Privacy & no host-path leakage assertions
            for forbidden in (
                "superpass",
                "secret_tok_999",
                "AshSecretName",
                "personal_trainer_key",
                "SECRET_ID_1",
                "ProprietarySecretLicenseName",
                "col_secret_a",
                "user_backup_2026.json",
                "account_session.json",
                "../etc/passwd",
                "/abs/host/path.json",
                str(repo.resolve()),
            ):
                self.assertNotIn(forbidden, m1["manifest_json"])

            by_path = {d["path"]: d for d in res1["datasets"]}
            self.assertFalse(by_path["nan_val.json"]["valid"])
            self.assertFalse(by_path["inf_val.json"]["valid"])
            self.assertFalse(by_path["deep.json"]["valid"])
            self.assertIn("json_depth_limit_exceeded", by_path["deep.json"]["errors"])

            self.assertFalse(by_path["ragged.csv"]["valid"])
            self.assertIn("csv_inconsistent_column_count", by_path["ragged.csv"]["errors"])
            self.assertFalse(by_path["many_rows.tsv"]["valid"])
            self.assertIn("csv_row_limit_exceeded", by_path["many_rows.tsv"]["errors"])

            # Credentialed URL discarded; partial ID coverage reported
            priv = by_path["redaction_fixture.json"]
            self.assertEqual(priv["documented_source_status"], "descartada_insegura")
            self.assertTrue(priv["id_inspection"]["truncated"])
            self.assertTrue(priv["partial_coverage"])
            self.assertEqual(priv["id_inspection"]["records_inspected"], 2)
            self.assertEqual(priv["id_inspection"]["records_total"], 4)

            # Naive date marked ambiguous (never UTC)
            nd = by_path["naive_date.json"]
            self.assertEqual(nd["temporal"]["date_status"], "ambigua_sem_fuso")
            self.assertEqual(nd["temporal"]["validity_status"], "validade_ambigua_sem_fuso")

            # Public OK dataset
            pub = by_path["public_ok.json"]
            self.assertTrue(pub["valid"])
            self.assertEqual(pub["documented_source_status"], "url_https_publica")
            self.assertEqual(pub["documented_revision_status"], "commit_hex")
            self.assertEqual(pub["license"]["spdx_public"], "MIT")
            self.assertFalse(pub["license"]["license_validated"])

            # Summary counters: 2 excluded sensitive, 3 invalid traversal refs, 1 missing ref, 1 inaccessible
            self.assertEqual(res1["summary"]["excluded_sensitive_count"], 2)
            self.assertEqual(res1["summary"]["invalid_reference_count"], 3)
            self.assertEqual(res1["inconsistencies"]["missing_references"], ["missing_ok.json"])
            self.assertEqual(res1["summary"]["inaccessible_count"], 1)

    def test_v5_white_layer_fix_untouched(self):
        mini = ROOT / "pgrazor" / "native" / "rocket_minigame.cpp"
        if mini.is_file():
            src = mini.read_text(encoding="utf-8")
            self.assertIn("hide_canvas_white_layer", src)
            self.assertIn("enforce_white_layers", src)


if __name__ == "__main__":
    unittest.main()
