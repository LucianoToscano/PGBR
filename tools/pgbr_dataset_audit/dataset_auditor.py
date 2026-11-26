"""Read-only offline auditor for embedded text/JSON datasets (G-049-08 / DATA-01 / DATA-02).

Delivers all 10 programmatic requirements:
(1) Discover real embedded datasets by paths & local references
(2) Classify dataset type & purpose
(3) Bounded regular-file size & SHA-256 (no unbounded hashing on oversized files)
(4) Documented source & revision only (else 'desconhecida')
(5) Explicit attribution/license presence vs 'desconhecida' (license_validated=False)
(6) Separate declared_schema from inferred_schema
(7) Explicit date/validity vs 'desconhecida' (flags expired datasets against reference time)
(8) Detect ID collisions per namespace & duplicate JSON keys without mutating data
(9) Report inconsistencies between expected local references and existing files
(10) Deterministic manifest & PT-BR report with relative paths only (no personal paths/secrets)
"""
from __future__ import annotations

import csv
from dataclasses import dataclass
from datetime import datetime, timezone
import hashlib
import io
import json
import os
from pathlib import Path
import stat
from typing import Any, Dict, Iterable, List, Optional, Set, Tuple

DATASET_EXTENSIONS = frozenset({".json", ".csv", ".tsv"})
SKIP_DIRS = frozenset({".git", "node_modules", "__pycache__", ".venv", "venv", "build", "dist"})
ID_CANDIDATE_FIELDS = ("id", "dex", "item_id", "task_id", "event_id", "key", "slug")


@dataclass(frozen=True)
class DatasetAuditLimits:
    max_file_bytes: int = 4 * 1024 * 1024  # 4 MiB
    max_keys_inferred: int = 64
    max_id_records: int = 10000


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
    st = os.lstat(path)
    if not stat.S_ISREG(st.st_mode):
        return None, "non_regular_file", 0
    if st.st_size > max_bytes:
        return None, f"file_exceeds_max_bytes:{st.st_size}>{max_bytes}", st.st_size
    # Windows text mode must not translate CRLF or stop at Ctrl-Z when hashing.
    flags = os.O_RDONLY | getattr(os, "O_NOFOLLOW", 0) | getattr(os, "O_NONBLOCK", 0) | getattr(os, "O_BINARY", 0)
    fd = os.open(str(path), flags)
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
    finally:
        os.close(fd)


def _loads_strict_json(text: str) -> Tuple[Any, List[str]]:
    """Parse JSON while recording duplicate object keys without mutating source."""
    dup_keys: List[str] = []

    def _pairs_hook(pairs: List[Tuple[str, Any]]) -> Dict[str, Any]:
        obj: Dict[str, Any] = {}
        for k, v in pairs:
            if k in obj:
                dup_keys.append(k)
            obj[k] = v
        return obj

    parsed = json.loads(text, object_pairs_hook=_pairs_hook)
    return parsed, sorted(set(dup_keys))


def _parse_iso_dt(value: Any) -> Optional[datetime]:
    if not isinstance(value, str) or not value.strip():
        return None
    raw = value.strip()
    if raw.endswith("Z"):
        raw = raw[:-1] + "+00:00"
    try:
        dt = datetime.fromisoformat(raw)
    except ValueError:
        return None
    if dt.tzinfo is None:
        return dt.replace(tzinfo=timezone.utc)
    return dt.astimezone(timezone.utc)


def _extract_metadata_and_schemas(
    parsed: Any,
    sibling_notice_files: List[str],
    reference_now_utc: datetime,
    limits: DatasetAuditLimits,
) -> Dict[str, Any]:
    declared_schema: Optional[str] = None
    inferred_schema: Dict[str, Any] = {"root_type": type(parsed).__name__}
    source = "desconhecida"
    revision = "desconhecida"
    explicit_license: Optional[str] = None
    explicit_date: Optional[str] = None
    explicit_valid_until: Optional[str] = None
    validity_status = "validade_desconhecida"
    id_collisions: List[Dict[str, Any]] = []

    if isinstance(parsed, dict):
        keys = sorted(parsed.keys())
        inferred_schema = {
            "root_type": "object",
            "key_count": len(keys),
            "sample_keys": keys[: limits.max_keys_inferred],
        }
        for k in ("$schema", "schema_version", "schema", "format_version", "version"):
            if k in parsed and parsed[k] is not None and not isinstance(parsed[k], (dict, list)):
                declared_schema = f"{k}={parsed[k]}"
                break
        for k in ("repository", "source", "upstream", "origin"):
            val = parsed.get(k)
            if isinstance(val, str) and val.strip():
                source = val.strip()
                break
        for k in ("commit", "revision", "ref", "tag", "versionName"):
            val = parsed.get(k)
            if isinstance(val, str) and val.strip():
                revision = val.strip()
                break
        for k in ("license", "spdx", "attribution"):
            val = parsed.get(k)
            if isinstance(val, str) and val.strip():
                explicit_license = val.strip()
                break
        for k in ("generated_at", "updated_at", "date", "created_at"):
            val = parsed.get(k)
            if isinstance(val, str) and val.strip():
                explicit_date = val.strip()
                break
        for k in ("valid_until", "expires_at", "validUntil"):
            val = parsed.get(k)
            if isinstance(val, str) and val.strip():
                explicit_valid_until = val.strip()
                dt = _parse_iso_dt(val)
                if dt is not None:
                    validity_status = "expirado" if reference_now_utc >= dt else "vigente"
                break

        # Detect ID collisions in nested list-of-dict namespaces (e.g. sprites, items, tasks)
        for ns_key, val in sorted(parsed.items()):
            if isinstance(val, list):
                id_collisions.extend(_scan_list_id_collisions(ns_key, val, limits.max_id_records))
    elif isinstance(parsed, list):
        inferred_schema = {"root_type": "array", "item_count": len(parsed)}
        id_collisions.extend(_scan_list_id_collisions("root", parsed, limits.max_id_records))

    has_license_evidence = bool(explicit_license or sibling_notice_files)
    license_status = "explicita_nao_validada" if has_license_evidence else "desconhecida"

    return {
        "declared_schema": declared_schema,
        "inferred_schema": inferred_schema,
        "documented_source": source,
        "documented_revision": revision,
        "license": {
            "status": license_status,
            "declared_value": explicit_license,
            "sibling_files": sibling_notice_files,
            "license_validated": False,
        },
        "temporal": {
            "explicit_date": explicit_date or "desconhecida",
            "explicit_valid_until": explicit_valid_until or "desconhecida",
            "validity_status": validity_status,
        },
        "id_collisions": id_collisions,
    }


def _scan_list_id_collisions(namespace: str, items: List[Any], max_records: int) -> List[Dict[str, Any]]:
    collisions: List[Dict[str, Any]] = []
    seen_by_field: Dict[str, Set[str]] = {}
    dup_by_field: Dict[str, Set[str]] = {}
    for item in items[:max_records]:
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
    for field, dups in sorted(dup_by_field.items()):
        collisions.append({
            "namespace": f"{namespace}.{field}",
            "duplicate_ids": sorted(dups),
        })
    return collisions


def _sibling_notice_files(directory: Path) -> List[str]:
    found: List[str] = []
    if directory.is_dir() and not directory.is_symlink():
        for child in sorted(directory.iterdir(), key=lambda p: p.name):
            try:
                st = os.lstat(child)
            except OSError:
                continue
            if not stat.S_ISREG(st.st_mode):
                continue
            low = child.name.lower()
            if low == "notice.txt" or low.startswith("license"):
                found.append(child.name)
    return found


def audit_embedded_datasets(
    repo_root: Path,
    *,
    limits: DatasetAuditLimits = DatasetAuditLimits(),
    expected_references: Optional[Iterable[str]] = None,
    reference_now_utc: Optional[datetime] = None,
) -> Dict[str, Any]:
    """Audit all embedded text/JSON datasets under repo_root without mutating any file."""
    root = Path(repo_root)
    if root.is_symlink():
        raise ValueError("Raiz de auditoria simbólica recusada.")
    st_root = os.lstat(root)
    if not stat.S_ISDIR(st_root.st_mode):
        raise ValueError("Raiz de auditoria não é um diretório regular.")
    root_resolved = root.resolve(strict=True)

    now_utc = (reference_now_utc or datetime.now(timezone.utc)).astimezone(timezone.utc)
    expected_refs = sorted(set(expected_references)) if expected_references is not None else []

    unsafe_paths: List[str] = []
    datasets: List[Dict[str, Any]] = []
    discovered_rel_paths: Set[str] = set()
    by_sha256: Dict[str, List[str]] = {}

    for dirpath, dirnames, filenames in os.walk(root_resolved, followlinks=False):
        dirnames.sort()
        filenames.sort()
        dp = Path(dirpath)

        safe_dirs: List[str] = []
        for d in dirnames:
            if d in SKIP_DIRS:
                continue
            cand = dp / d
            if cand.is_symlink():
                unsafe_paths.append(f"symlink_dir:{cand.relative_to(root_resolved).as_posix()}")
            else:
                safe_dirs.append(d)
        dirnames[:] = safe_dirs

        notices = _sibling_notice_files(dp)

        for fname in filenames:
            fpath = dp / fname
            ext = fpath.suffix.lower()
            if ext not in DATASET_EXTENSIONS:
                continue
            try:
                fst = os.lstat(fpath)
            except OSError:
                unsafe_paths.append(f"unstatable:{fname}")
                continue
            if stat.S_ISLNK(fst.st_mode):
                unsafe_paths.append(f"symlink_file:{fpath.relative_to(root_resolved).as_posix()}")
                continue
            if not stat.S_ISREG(fst.st_mode):
                unsafe_paths.append(f"non_regular_file:{fpath.relative_to(root_resolved).as_posix()}")
                continue

            rel = fpath.relative_to(root_resolved).as_posix()
            discovered_rel_paths.add(rel)
            classification = classify_dataset(rel)
            data, io_err, observed_size = _read_bounded_regular(fpath, limits.max_file_bytes)

            if io_err is not None:
                datasets.append({
                    "path": rel,
                    **classification,
                    "bytes": observed_size,
                    "sha256": None,
                    "hash_status": "indisponivel_oversized" if "max_bytes" in io_err else "indisponivel_nao_regular",
                    "valid": False,
                    "errors": [io_err],
                    "duplicate_json_keys": [],
                    "id_collisions": [],
                    "declared_schema": None,
                    "inferred_schema": None,
                    "documented_source": "desconhecida",
                    "documented_revision": "desconhecida",
                    "license": {"status": "desconhecida", "declared_value": None, "sibling_files": notices, "license_validated": False},
                    "temporal": {"explicit_date": "desconhecida", "explicit_valid_until": "desconhecida", "validity_status": "validade_desconhecida"},
                })
                continue

            assert data is not None
            digest = hashlib.sha256(data).hexdigest()
            by_sha256.setdefault(digest, []).append(rel)
            errors: List[str] = []
            dup_keys: List[str] = []

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
                    "duplicate_json_keys": [],
                    "id_collisions": [],
                    "declared_schema": None,
                    "inferred_schema": None,
                    "documented_source": "desconhecida",
                    "documented_revision": "desconhecida",
                    "license": {"status": "desconhecida", "declared_value": None, "sibling_files": notices, "license_validated": False},
                    "temporal": {"explicit_date": "desconhecida", "explicit_valid_until": "desconhecida", "validity_status": "validade_desconhecida"},
                })
                continue

            if ext == ".json":
                try:
                    parsed, dup_keys = _loads_strict_json(text)
                    if dup_keys:
                        errors.append(f"duplicate_json_keys:{','.join(dup_keys)}")
                    meta = _extract_metadata_and_schemas(parsed, notices, now_utc, limits)
                except json.JSONDecodeError as exc:
                    errors.append(f"invalid_json:line={exc.lineno}")
                    meta = {
                        "declared_schema": None,
                        "inferred_schema": None,
                        "documented_source": "desconhecida",
                        "documented_revision": "desconhecida",
                        "license": {"status": "desconhecida", "declared_value": None, "sibling_files": notices, "license_validated": False},
                        "temporal": {"explicit_date": "desconhecida", "explicit_valid_until": "desconhecida", "validity_status": "validade_desconhecida"},
                        "id_collisions": [],
                    }
            else:
                delim = "\t" if ext == ".tsv" else ","
                rows = list(csv.reader(io.StringIO(text), delimiter=delim))
                header = rows[0] if rows else []
                meta = {
                    "declared_schema": None,
                    "inferred_schema": {"root_type": "tabular", "columns": header[: limits.max_keys_inferred], "row_count": max(0, len(rows) - 1)},
                    "documented_source": "desconhecida",
                    "documented_revision": "desconhecida",
                    "license": {"status": "explicita_nao_validada" if notices else "desconhecida", "declared_value": None, "sibling_files": notices, "license_validated": False},
                    "temporal": {"explicit_date": "desconhecida", "explicit_valid_until": "desconhecida", "validity_status": "validade_desconhecida"},
                    "id_collisions": [],
                }

            datasets.append({
                "path": rel,
                **classification,
                "bytes": len(data),
                "sha256": digest,
                "hash_status": "computado",
                "valid": len(errors) == 0,
                "errors": errors,
                "duplicate_json_keys": dup_keys,
                **meta,
            })

    datasets.sort(key=lambda d: d["path"])
    unsafe_paths.sort()

    missing_references = [ref for ref in expected_refs if ref not in discovered_rel_paths]
    duplicate_files = [
        {"sha256": sha, "paths": sorted(paths)}
        for sha, paths in sorted(by_sha256.items())
        if len(paths) > 1
    ]
    expired_datasets = [d["path"] for d in datasets if d["temporal"]["validity_status"] == "expirado"]
    invalid_datasets = [d["path"] for d in datasets if not d["valid"]]
    collision_datasets = [
        {"path": d["path"], "collisions": d["id_collisions"]}
        for d in datasets
        if d["id_collisions"]
    ]

    return {
        "schema": 1,
        "mutated": False,
        "device_tested": False,
        "summary": {
            "dataset_count": len(datasets),
            "valid_count": len(datasets) - len(invalid_datasets),
            "invalid_count": len(invalid_datasets),
            "expired_count": len(expired_datasets),
            "duplicate_file_groups": len(duplicate_files),
            "namespace_collision_files": len(collision_datasets),
            "missing_reference_count": len(missing_references),
            "unsafe_path_count": len(unsafe_paths),
        },
        "inconsistencies": {
            "missing_references": missing_references,
            "invalid_datasets": invalid_datasets,
            "expired_datasets": expired_datasets,
            "namespace_collisions": collision_datasets,
        },
        "duplicate_files": duplicate_files,
        "unsafe_paths": unsafe_paths,
        "datasets": datasets,
    }


def build_deterministic_manifest(audit_result: Dict[str, Any]) -> Dict[str, Any]:
    """Emit deterministic JSON manifest using strictly relative paths (no personal paths/secrets)."""
    canonical = json.dumps(audit_result, sort_keys=True, separators=(",", ":"), ensure_ascii=False).encode("utf-8")
    return {
        "manifest_sha256": hashlib.sha256(canonical).hexdigest(),
        "manifest_json": canonical.decode("utf-8"),
    }


def render_ptbr_report(audit_result: Dict[str, Any]) -> str:
    s = audit_result["summary"]
    lines = [
        "PGBR — Relatório Offline de Inventário e Proveniência de Datasets (DATA-01 / DATA-02)",
        f"Datasets texto/JSON analisados: {s['dataset_count']} (válidos: {s['valid_count']}, inválidos: {s['invalid_count']})",
        f"Datasets expirados (valid_until vencido): {s['expired_count']}",
        f"Grupos de arquivos duplicados (SHA-256): {s['duplicate_file_groups']}",
        f"Arquivos com colisões de ID por namespace: {s['namespace_collision_files']}",
        f"Referências ausentes: {s['missing_reference_count']} · Caminhos inseguros: {s['unsafe_path_count']}",
        "Nota: presença de NOTICE/licença não equivale a validação jurídica; esquema declarado e inferido são mantidos separados.",
        "Modo estritamente somente leitura (mutated=false, device_tested=false).",
    ]
    return "\n".join(lines)
