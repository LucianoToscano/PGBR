"""Read-only offline auditor for embedded text/JSON datasets (G-049-08 / G-049-09).

Hardened guarantees (G-049-09):
(1) Rejects non-standard JSON NaN/Infinity and unrepresentable numbers
(2) Handles nesting depth / RecursionError / node limits without crashing audit
(3) Strict incremental CSV/TSV parsing with column, row & field-length bounds
(4) Never copies arbitrary/potentially personal keys, IDs, columns, metadata,
    errors, or refs into the manifest — reports counts, categories & public allowlists
(5) Discards URLs with credentials, tokens, queries, fragments or host paths
(6) Scopes audit to authorized embedded datasets; excludes local user/backup/secret files
(7) Validates relative expected references and rejects path traversal
(8) Never infers UTC on naive dates; marks them 'validade_ambigua_sem_fuso'
(9) Explicitly reports ID inspection limits and partial_coverage flags
(10) Handles OSError / disappeared / inaccessible files without exposing host paths
"""
from __future__ import annotations

import csv
from dataclasses import dataclass
from datetime import datetime, timezone
import hashlib
import io
import json
import math
import os
from pathlib import Path
import re
import stat
from typing import Any, Dict, Iterable, List, Optional, Set, Tuple
from urllib.parse import urlsplit

DATASET_EXTENSIONS = frozenset({".json", ".csv", ".tsv"})
SKIP_DIRS = frozenset({
    ".git", "node_modules", "__pycache__", ".venv", "venv", "build", "dist",
    "downloads", "artifacts", ".cache", "backups", "user_data", "private",
})
EXCLUDED_SENSITIVE_NAME_TOKENS = (
    "backup", "secret", "credential", "token", "password", "passwd",
    "account", "profile", "session", "private", "local_user", ".env",
)
ID_CANDIDATE_FIELDS = ("id", "dex", "item_id", "task_id", "event_id", "key", "slug")
PUBLIC_SPDX_ALLOWLIST = frozenset({
    "MIT", "Apache-2.0", "BSD-2-Clause", "BSD-3-Clause", "ISC",
    "GPL-3.0", "GPL-3.0-only", "LGPL-3.0", "MPL-2.0", "CC0-1.0",
    "CC-BY-4.0", "CC-BY-SA-4.0", "Unlicense",
})
_WIN_DRIVE_RE = re.compile(r"^[A-Za-z]:")
_HEX_COMMIT_RE = re.compile(r"^[0-9a-fA-F]{7,40}$")
_SAFE_TAG_RE = re.compile(r"^[vV]?\d+\.\d+(?:\.\d+){0,2}(?:-[A-Za-z0-9.]+)?$")


@dataclass(frozen=True)
class DatasetAuditLimits:
    max_file_bytes: int = 4 * 1024 * 1024  # 4 MiB
    max_json_depth: int = 64
    max_json_nodes: int = 200000
    max_int_digits: int = 300
    max_id_records: int = 10000
    max_csv_rows: int = 50000
    max_csv_columns: int = 256
    max_csv_field_chars: int = 4096


def _is_excluded_sensitive_filename(filename: str) -> bool:
    low = filename.lower()
    return any(tok in low for tok in EXCLUDED_SENSITIVE_NAME_TOKENS)


def _is_safe_relative_ref(ref: str) -> bool:
    """Validate relative POSIX reference path (item 7); reject traversal, drive letters, abs paths."""
    if not isinstance(ref, str) or not ref or ref.strip() != ref:
        return False
    if ref.startswith("/") or ref.startswith("\\") or "\\" in ref or _WIN_DRIVE_RE.match(ref):
        return False
    if any(ord(ch) < 32 for ch in ref):
        return False
    parts = ref.split("/")
    if any(p in ("", ".", "..") for p in parts):
        return False
    return True


def _classify_public_url(value: Any) -> str:
    """Allow only clean public HTTPS URLs without credentials, queries, fragments or host paths (item 5)."""
    if not isinstance(value, str) or not value.strip():
        return "desconhecida"
    raw = value.strip()
    if raw.startswith("/") or raw.startswith("\\") or _WIN_DRIVE_RE.match(raw):
        return "descartada_insegura"
    try:
        parts = urlsplit(raw)
    except ValueError:
        return "descartada_insegura"
    if parts.scheme.lower() != "https" or not parts.netloc:
        return "descartada_insegura"
    if parts.username or parts.password or "@" in parts.netloc or parts.query or parts.fragment:
        return "descartada_insegura"
    host = (parts.hostname or "").lower()
    if not host or host in ("localhost", "127.0.0.1", "::1"):
        return "descartada_insegura"
    return "url_https_publica"


def _classify_revision(value: Any) -> str:
    if not isinstance(value, str) or not value.strip():
        return "desconhecida"
    raw = value.strip()
    if _HEX_COMMIT_RE.fullmatch(raw):
        return "commit_hex"
    if _SAFE_TAG_RE.fullmatch(raw):
        return "tag_versao"
    return "texto_nao_padronizado"


def classify_dataset(rel_path: str) -> Dict[str, str]:
    """Classify dataset type and purpose deterministically from its relative POSIX path."""
    low = rel_path.lower()
    if low.endswith(".csv") or low.endswith(".tsv"):
        return {"kind": "tabular_text", "purpose": "tabela_offline"}
    if "/i18n/" in f"/{low}":
        return {"kind": "i18n_dictionary", "purpose": "traducao_interface"}
    if low.endswith("provenance.json") or "provenance" in low:
        return {"kind": "provenance_manifest", "purpose": "proveniencia_ativos_ou_dependencias"}
    if "/native/schema/" in f"/{low}":
        return {"kind": "native_schema", "purpose": "contrato_esquema_nativo"}
    if "manifest" in low:
        return {"kind": "integrity_manifest", "purpose": "validacao_integridade"}
    if low.startswith("docs/validation/") or "catalogo" in low or "registro" in low or "pokemon-names" in low:
        return {"kind": "reference_catalog", "purpose": "catalogo_ou_validacao"}
    if low.endswith("package.json") or low.endswith("package-lock.json") or "dependencies.json" in low:
        return {"kind": "package_metadata", "purpose": "metadados_dependencias"}
    if "retention" in low or "bundles.json" in low or "queue.json" in low:
        return {"kind": "operational_config", "purpose": "configuracao_build_ou_fila"}
    return {"kind": "json_dataset", "purpose": "dados_estruturados"}


def _read_bounded_regular(path: Path, max_bytes: int) -> Tuple[Optional[bytes], Optional[str], int]:
    try:
        st = os.lstat(path)
    except OSError as exc:
        return None, f"io_error:{type(exc).__name__}", 0
    if not stat.S_ISREG(st.st_mode):
        return None, "non_regular_file", 0
    if st.st_size > max_bytes:
        return None, f"file_exceeds_max_bytes:{st.st_size}>{max_bytes}", st.st_size
    # Windows text mode must not translate CRLF or stop at Ctrl-Z when hashing.
    flags = os.O_RDONLY | getattr(os, "O_NOFOLLOW", 0) | getattr(os, "O_NONBLOCK", 0) | getattr(os, "O_BINARY", 0)
    try:
        fd = os.open(str(path), flags)
    except OSError as exc:
        return None, f"io_error:{type(exc).__name__}", 0
    try:
        fst = os.fstat(fd)
        if not stat.S_ISREG(fst.st_mode):
            return None, "non_regular_file", 0
        if fst.st_size > max_bytes:
            return None, f"file_exceeds_max_bytes:{fst.st_size}>{max_bytes}", fst.st_size
        chunks: List[bytes] = []
        total = 0
        cap = max_bytes + 1
        while total < cap:
            buf = os.read(fd, min(65536, cap - total))
            if not buf:
                break
            chunks.append(buf)
            total += len(buf)
        if total > max_bytes:
            return None, f"file_grew_over_max_bytes:>{max_bytes}", total
        return b"".join(chunks), None, total
    except OSError as exc:
        return None, f"io_error:{type(exc).__name__}", 0
    finally:
        try:
            os.close(fd)
        except OSError:
            pass


def _check_json_depth_and_nodes(obj: Any, max_depth: int, max_nodes: int) -> Optional[str]:
    """Iterative depth and node-count guard (item 2); immune to Python RecursionError."""
    stack: List[Tuple[Any, int]] = [(obj, 1)]
    nodes = 0
    while stack:
        cur, depth = stack.pop()
        if depth > max_depth:
            return "json_depth_limit_exceeded"
        nodes += 1
        if nodes > max_nodes:
            return "json_node_limit_exceeded"
        if isinstance(cur, dict):
            for v in cur.values():
                if isinstance(v, (dict, list)):
                    stack.append((v, depth + 1))
        elif isinstance(cur, list):
            for v in cur:
                if isinstance(v, (dict, list)):
                    stack.append((v, depth + 1))
    return None


def _loads_strict_json(text: str, limits: DatasetAuditLimits) -> Tuple[Any, int]:
    """Parse JSON strictly rejecting NaN/Infinity, huge ints and recording duplicate key count (items 1 & 2)."""
    dup_key_count = 0

    def _reject_constant(token: str) -> Any:
        raise ValueError(f"nonstandard_json_constant:{token}")

    def _parse_float(raw: str) -> float:
        val = float(raw)
        if not math.isfinite(val):
            raise ValueError("non_finite_float")
        return val

    def _parse_int(raw: str) -> int:
        digits = raw[1:] if raw.startswith("-") else raw
        if len(digits) > limits.max_int_digits:
            raise ValueError("integer_digit_limit_exceeded")
        return int(raw)

    def _pairs_hook(pairs: List[Tuple[str, Any]]) -> Dict[str, Any]:
        nonlocal dup_key_count
        obj: Dict[str, Any] = {}
        for k, v in pairs:
            if k in obj:
                dup_key_count += 1
            obj[k] = v
        return obj

    parsed = json.loads(
        text,
        object_pairs_hook=_pairs_hook,
        parse_constant=_reject_constant,
        parse_float=_parse_float,
        parse_int=_parse_int,
    )
    return parsed, dup_key_count


def _parse_iso_dt_strict(value: Any) -> Tuple[Optional[datetime], str]:
    """Parse ISO-8601 datetime WITHOUT inferring timezone on naive dates (item 8)."""
    if not isinstance(value, str) or not value.strip():
        return None, "ausente"
    raw = value.strip()
    if raw.endswith("Z"):
        raw = raw[:-1] + "+00:00"
    try:
        dt = datetime.fromisoformat(raw)
    except ValueError:
        return None, "formato_invalido"
    if dt.tzinfo is None:
        return None, "sem_fuso"
    return dt.astimezone(timezone.utc), "ok"


def _scan_list_id_collisions(
    items: List[Any],
    max_records: int,
) -> Tuple[List[Dict[str, Any]], bool, int, int]:
    """Scan ID collisions reporting ONLY allowlisted field names and counts (items 4 & 9)."""
    total = len(items)
    inspected = min(total, max_records)
    truncated = total > max_records
    seen_by_field: Dict[str, Set[str]] = {}
    dup_by_field: Dict[str, Set[str]] = {}
    for item in items[:inspected]:
        if not isinstance(item, dict):
            continue
        for field in ID_CANDIDATE_FIELDS:
            if field in item and item[field] is not None and not isinstance(item[field], (dict, list)):
                val = str(item[field])
                seen = seen_by_field.setdefault(field, set())
                if val in seen:
                    dup_by_field.setdefault(field, set()).add(val)
                else:
                    seen.add(val)
    collisions = [
        {"field": field, "duplicate_count": len(dups)}
        for field, dups in sorted(dup_by_field.items())
    ]
    return collisions, truncated, inspected, total


def _extract_sanitized_metadata(
    parsed: Any,
    sibling_notice_count: int,
    reference_now_utc: datetime,
    limits: DatasetAuditLimits,
) -> Dict[str, Any]:
    """Extract metadata using ONLY safe categories, counts and allowlisted values (items 4, 5, 6, 8, 9)."""
    declared_schema_kind: Optional[str] = None
    inferred_schema: Dict[str, Any] = {"root_type": type(parsed).__name__}
    source_status = "desconhecida"
    revision_status = "desconhecida"
    spdx_public: Optional[str] = None
    has_declared_license = False
    date_status = "desconhecida"
    validity_status = "validade_desconhecida"
    id_collisions: List[Dict[str, Any]] = []
    id_truncated = False
    id_inspected = 0
    id_total = 0

    if isinstance(parsed, dict):
        inferred_schema = {"root_type": "object", "key_count": len(parsed)}
        for k in ("$schema", "schema_version", "schema", "format_version", "version"):
            if k in parsed and parsed[k] is not None and not isinstance(parsed[k], (dict, list)):
                declared_schema_kind = k
                break
        for k in ("repository", "source", "upstream", "origin"):
            if k in parsed and parsed[k] is not None:
                source_status = _classify_public_url(parsed[k])
                break
        for k in ("commit", "revision", "ref", "tag", "versionName"):
            if k in parsed and parsed[k] is not None:
                revision_status = _classify_revision(parsed[k])
                break
        for k in ("license", "spdx", "attribution"):
            val = parsed.get(k)
            if isinstance(val, str) and val.strip():
                has_declared_license = True
                if val.strip() in PUBLIC_SPDX_ALLOWLIST:
                    spdx_public = val.strip()
                break
        for k in ("generated_at", "updated_at", "date", "created_at"):
            if k in parsed and parsed[k] is not None:
                _dt, st = _parse_iso_dt_strict(parsed[k])
                date_status = "explicita_com_fuso" if st == "ok" else ("ambigua_sem_fuso" if st == "sem_fuso" else "invalida")
                break
        for k in ("valid_until", "expires_at", "validUntil"):
            if k in parsed and parsed[k] is not None:
                dt, st = _parse_iso_dt_strict(parsed[k])
                if st == "ok" and dt is not None:
                    validity_status = "expirado" if reference_now_utc >= dt else "vigente"
                elif st == "sem_fuso":
                    validity_status = "validade_ambigua_sem_fuso"
                else:
                    validity_status = "validade_invalida"
                break

        for _ns_key, val in sorted(parsed.items()):
            if isinstance(val, list):
                cols, trunc, insp, tot = _scan_list_id_collisions(val, limits.max_id_records)
                id_collisions.extend(cols)
                id_truncated = id_truncated or trunc
                id_inspected += insp
                id_total += tot
    elif isinstance(parsed, list):
        inferred_schema = {"root_type": "array", "item_count": len(parsed)}
        cols, trunc, insp, tot = _scan_list_id_collisions(parsed, limits.max_id_records)
        id_collisions.extend(cols)
        id_truncated = trunc
        id_inspected = insp
        id_total = tot

    has_license_evidence = has_declared_license or sibling_notice_count > 0
    return {
        "declared_schema": {"present": declared_schema_kind is not None, "kind": declared_schema_kind},
        "inferred_schema": inferred_schema,
        "documented_source_status": source_status,
        "documented_revision_status": revision_status,
        "license": {
            "status": "explicita_nao_validada" if has_license_evidence else "desconhecida",
            "spdx_public": spdx_public,
            "sibling_notice_count": sibling_notice_count,
            "license_validated": False,
        },
        "temporal": {
            "date_status": date_status,
            "validity_status": validity_status,
        },
        "id_inspection": {
            "collisions": id_collisions,
            "max_records_limit": limits.max_id_records,
            "records_inspected": id_inspected,
            "records_total": id_total,
            "truncated": id_truncated,
            "partial_coverage": id_truncated,
        },
    }


def _parse_csv_incremental(text: str, delimiter: str, limits: DatasetAuditLimits) -> Tuple[Dict[str, Any], List[str], bool]:
    """Strict incremental CSV/TSV parsing with column, row & field length limits (item 3)."""
    errors: List[str] = []
    reader = csv.reader(io.StringIO(text), delimiter=delimiter, strict=True)
    header_cols: Optional[int] = None
    row_count = 0
    ragged_rows = 0
    truncated = False

    try:
        for row in reader:
            if len(row) > limits.max_csv_columns:
                errors.append("csv_column_limit_exceeded")
                truncated = True
                break
            if any(len(cell) > limits.max_csv_field_chars for cell in row):
                errors.append("csv_field_length_limit_exceeded")
                truncated = True
                break
            if header_cols is None:
                header_cols = len(row)
                continue
            row_count += 1
            if row_count > limits.max_csv_rows:
                errors.append("csv_row_limit_exceeded")
                truncated = True
                break
            if len(row) != header_cols:
                ragged_rows += 1
    except csv.Error:
        errors.append("csv_strict_parse_error")

    if ragged_rows > 0:
        errors.append("csv_inconsistent_column_count")

    inferred = {
        "root_type": "tabular",
        "column_count": header_cols or 0,
        "row_count": min(row_count, limits.max_csv_rows),
        "ragged_row_count": ragged_rows,
        "partial_coverage": truncated,
    }
    return inferred, errors, truncated


def _sibling_notice_count(directory: Path) -> int:
    count = 0
    if directory.is_dir() and not directory.is_symlink():
        try:
            children = sorted(directory.iterdir(), key=lambda p: p.name)
        except OSError:
            return 0
        for child in children:
            try:
                st = os.lstat(child)
            except OSError:
                continue
            if not stat.S_ISREG(st.st_mode):
                continue
            low = child.name.lower()
            if low == "notice.txt" or low.startswith("license"):
                count += 1
    return count


def _empty_meta(sibling_notice_count: int, limits: DatasetAuditLimits) -> Dict[str, Any]:
    return {
        "declared_schema": {"present": False, "kind": None},
        "inferred_schema": None,
        "documented_source_status": "desconhecida",
        "documented_revision_status": "desconhecida",
        "license": {
            "status": "explicita_nao_validada" if sibling_notice_count > 0 else "desconhecida",
            "spdx_public": None,
            "sibling_notice_count": sibling_notice_count,
            "license_validated": False,
        },
        "temporal": {"date_status": "desconhecida", "validity_status": "validade_desconhecida"},
        "id_inspection": {
            "collisions": [],
            "max_records_limit": limits.max_id_records,
            "records_inspected": 0,
            "records_total": 0,
            "truncated": False,
            "partial_coverage": False,
        },
    }


def audit_embedded_datasets(
    repo_root: Path,
    *,
    limits: DatasetAuditLimits = DatasetAuditLimits(),
    expected_references: Optional[Iterable[str]] = None,
    reference_now_utc: Optional[datetime] = None,
) -> Dict[str, Any]:
    """Audit embedded text/JSON datasets under repo_root (read-only, sanitized, bounded)."""
    root = Path(repo_root)
    if root.is_symlink():
        raise ValueError("Raiz de auditoria simbólica recusada.")
    try:
        st_root = os.lstat(root)
    except OSError as exc:
        raise ValueError(f"Raiz de auditoria inacessível:{type(exc).__name__}") from exc
    if not stat.S_ISDIR(st_root.st_mode):
        raise ValueError("Raiz de auditoria não é um diretório regular.")
    root_resolved = root.resolve(strict=True)

    now_utc = (reference_now_utc or datetime.now(timezone.utc)).astimezone(timezone.utc)

    # (7) Validate expected_references: separate safe relative refs from rejected traversal/abs refs.
    safe_expected_refs: List[str] = []
    invalid_reference_count = 0
    if expected_references is not None:
        seen_refs: Set[str] = set()
        for ref in expected_references:
            if _is_safe_relative_ref(ref):
                if ref not in seen_refs:
                    seen_refs.add(ref)
                    safe_expected_refs.append(ref)
            else:
                invalid_reference_count += 1
        safe_expected_refs.sort()

    unsafe_path_categories: Dict[str, int] = {}
    excluded_sensitive_count = 0
    inaccessible_count = 0
    datasets: List[Dict[str, Any]] = []
    discovered_rel_paths: Set[str] = set()
    by_sha256: Dict[str, List[str]] = {}

    def _note_unsafe(category: str) -> None:
        unsafe_path_categories[category] = unsafe_path_categories.get(category, 0) + 1

    def _on_walk_error(_exc: OSError) -> None:
        nonlocal inaccessible_count
        inaccessible_count += 1

    for dirpath, dirnames, filenames in os.walk(root_resolved, followlinks=False, onerror=_on_walk_error):
        dirnames.sort()
        filenames.sort()
        dp = Path(dirpath)

        safe_dirs: List[str] = []
        for d in dirnames:
            if d in SKIP_DIRS or _is_excluded_sensitive_filename(d):
                if _is_excluded_sensitive_filename(d):
                    excluded_sensitive_count += 1
                continue
            cand = dp / d
            try:
                dst = os.lstat(cand)
            except OSError:
                inaccessible_count += 1
                continue
            if stat.S_ISLNK(dst.st_mode):
                _note_unsafe("symlink_dir")
            elif stat.S_ISDIR(dst.st_mode):
                safe_dirs.append(d)
        dirnames[:] = safe_dirs

        notices = _sibling_notice_count(dp)

        for fname in filenames:
            fpath = dp / fname
            ext = fpath.suffix.lower()
            if ext not in DATASET_EXTENSIONS:
                continue
            # (6) Exclude local user/backup/secret/config files without leaking filenames
            if _is_excluded_sensitive_filename(fname):
                excluded_sensitive_count += 1
                continue
            try:
                fst = os.lstat(fpath)
            except OSError:
                inaccessible_count += 1
                continue
            if stat.S_ISLNK(fst.st_mode):
                _note_unsafe("symlink_file")
                continue
            if not stat.S_ISREG(fst.st_mode):
                _note_unsafe("non_regular_file")
                continue

            rel = fpath.relative_to(root_resolved).as_posix()
            discovered_rel_paths.add(rel)
            classification = classify_dataset(rel)
            data, io_err, observed_size = _read_bounded_regular(fpath, limits.max_file_bytes)

            if io_err is not None:
                if io_err.startswith("io_error:"):
                    inaccessible_count += 1
                hash_status = "indisponivel_oversized" if "max_bytes" in io_err else "indisponivel_io"
                datasets.append({
                    "path": rel,
                    **classification,
                    "bytes": observed_size,
                    "sha256": None,
                    "hash_status": hash_status,
                    "valid": False,
                    "errors": [io_err.split(":", 1)[0]],
                    "duplicate_json_key_count": 0,
                    "partial_coverage": True,
                    **_empty_meta(notices, limits),
                })
                continue

            assert data is not None
            digest = hashlib.sha256(data).hexdigest()
            by_sha256.setdefault(digest, []).append(rel)
            errors: List[str] = []
            dup_key_count = 0
            partial_cov = False

            try:
                text = data.decode("utf-8")
            except UnicodeDecodeError:
                datasets.append({
                    "path": rel,
                    **classification,
                    "bytes": len(data),
                    "sha256": digest,
                    "hash_status": "computado",
                    "valid": False,
                    "errors": ["invalid_utf8"],
                    "duplicate_json_key_count": 0,
                    "partial_coverage": True,
                    **_empty_meta(notices, limits),
                })
                continue

            if ext == ".json":
                try:
                    parsed, dup_key_count = _loads_strict_json(text, limits)
                    if dup_key_count > 0:
                        errors.append("duplicate_json_keys")
                    depth_err = _check_json_depth_and_nodes(parsed, limits.max_json_depth, limits.max_json_nodes)
                    if depth_err is not None:
                        errors.append(depth_err)
                        partial_cov = True
                        meta = _empty_meta(notices, limits)
                    else:
                        meta = _extract_sanitized_metadata(parsed, notices, now_utc, limits)
                        partial_cov = bool(meta["id_inspection"]["partial_coverage"])
                except RecursionError:
                    errors.append("json_recursion_limit")
                    partial_cov = True
                    meta = _empty_meta(notices, limits)
                except ValueError as exc:
                    tag = "nonstandard_json_number" if "constant" in str(exc) or "float" in str(exc) or "integer" in str(exc) else "invalid_json"
                    errors.append(tag)
                    partial_cov = True
                    meta = _empty_meta(notices, limits)
            else:
                delim = "\t" if ext == ".tsv" else ","
                inferred_csv, csv_errors, csv_trunc = _parse_csv_incremental(text, delim, limits)
                errors.extend(csv_errors)
                partial_cov = csv_trunc
                meta = _empty_meta(notices, limits)
                meta["inferred_schema"] = inferred_csv

            datasets.append({
                "path": rel,
                **classification,
                "bytes": len(data),
                "sha256": digest,
                "hash_status": "computado",
                "valid": len(errors) == 0,
                "errors": errors,
                "duplicate_json_key_count": dup_key_count,
                "partial_coverage": partial_cov,
                **meta,
            })

    datasets.sort(key=lambda d: d["path"])

    missing_references = [ref for ref in safe_expected_refs if ref not in discovered_rel_paths]
    duplicate_files = [
        {"sha256": sha, "paths": sorted(paths)}
        for sha, paths in sorted(by_sha256.items())
        if len(paths) > 1
    ]
    expired_datasets = [d["path"] for d in datasets if d["temporal"]["validity_status"] == "expirado"]
    ambiguous_validity = [d["path"] for d in datasets if d["temporal"]["validity_status"] == "validade_ambigua_sem_fuso"]
    invalid_datasets = [d["path"] for d in datasets if not d["valid"]]
    collision_datasets = [
        {"path": d["path"], "collisions": d["id_inspection"]["collisions"]}
        for d in datasets
        if d["id_inspection"]["collisions"]
    ]
    partial_coverage_files = [d["path"] for d in datasets if d["partial_coverage"]]
    unsafe_total = sum(unsafe_path_categories.values())

    return {
        "schema": 2,
        "mutated": False,
        "device_tested": False,
        "limits": {
            "max_file_bytes": limits.max_file_bytes,
            "max_json_depth": limits.max_json_depth,
            "max_json_nodes": limits.max_json_nodes,
            "max_id_records": limits.max_id_records,
            "max_csv_rows": limits.max_csv_rows,
            "max_csv_columns": limits.max_csv_columns,
            "max_csv_field_chars": limits.max_csv_field_chars,
        },
        "summary": {
            "dataset_count": len(datasets),
            "valid_count": len(datasets) - len(invalid_datasets),
            "invalid_count": len(invalid_datasets),
            "expired_count": len(expired_datasets),
            "ambiguous_validity_count": len(ambiguous_validity),
            "partial_coverage_count": len(partial_coverage_files),
            "duplicate_file_groups": len(duplicate_files),
            "namespace_collision_files": len(collision_datasets),
            "missing_reference_count": len(missing_references),
            "invalid_reference_count": invalid_reference_count,
            "unsafe_path_count": unsafe_total,
            "excluded_sensitive_count": excluded_sensitive_count,
            "inaccessible_count": inaccessible_count,
        },
        "inconsistencies": {
            "missing_references": missing_references,
            "invalid_reference_count": invalid_reference_count,
            "invalid_datasets": invalid_datasets,
            "expired_datasets": expired_datasets,
            "ambiguous_validity_datasets": ambiguous_validity,
            "namespace_collisions": collision_datasets,
            "partial_coverage_files": partial_coverage_files,
        },
        "duplicate_files": duplicate_files,
        "unsafe_path_categories": dict(sorted(unsafe_path_categories.items())),
        "datasets": datasets,
    }


def build_deterministic_manifest(audit_result: Dict[str, Any]) -> Dict[str, Any]:
    """Emit deterministic JSON manifest using strictly relative paths and sanitized categories."""
    canonical = json.dumps(audit_result, sort_keys=True, separators=(",", ":"), ensure_ascii=False).encode("utf-8")
    return {
        "manifest_sha256": hashlib.sha256(canonical).hexdigest(),
        "manifest_json": canonical.decode("utf-8"),
    }


def render_ptbr_report(audit_result: Dict[str, Any]) -> str:
    s = audit_result["summary"]
    lines = [
        "PGBR — Relatório Offline de Inventário e Proveniência de Datasets (DATA-01 / DATA-02)",
        f"Datasets embarcados analisados: {s['dataset_count']} (válidos: {s['valid_count']}, inválidos: {s['invalid_count']})",
        f"Expirados: {s['expired_count']} · Validade ambígua (sem fuso): {s['ambiguous_validity_count']} · Cobertura parcial: {s['partial_coverage_count']}",
        f"Duplicatas (SHA-256): {s['duplicate_file_groups']} · Colisões de ID: {s['namespace_collision_files']}",
        f"Referências ausentes: {s['missing_reference_count']} · Referências inválidas/traversal: {s['invalid_reference_count']}",
        f"Caminhos inseguros: {s['unsafe_path_count']} · Excluídos sensíveis: {s['excluded_sensitive_count']} · Inacessíveis (OSError): {s['inaccessible_count']}",
        "Privacidade: nenhuma chave arbitrária, valor de ID, URL com credencial/query ou caminho absoluto do host é copiado para o manifesto.",
        "Modo estritamente somente leitura (mutated=false, device_tested=false).",
    ]
    return "\n".join(lines)
