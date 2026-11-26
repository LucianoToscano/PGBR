"""Full read-only offline auditor for PGBR sprites & icons (G-049-06 / G-049-07).

Hardened guarantees (G-049-07):
- (1) Rejects symlinked audit root (ValueError)
- (2) Rejects non-regular files (FIFOs, sockets, devices) via lstat
- (5) Never groups missing/unavailable hashes (None) as binary duplicates
- (6) Coverage counts ONLY structurally valid images with matching format
- (7) Materializes expected ID iterables/generators exactly once
- (8) Distinguishes presence of NOTICE/license files from legal license validation
- (9) Reports alpha strictly as potential alpha/transparency channel presence
- (10) Explicitly states structural probe scope (not full pixel decode or SVG parse)
"""
from __future__ import annotations

import hashlib
import json
import os
from pathlib import Path
import re
import stat
from typing import Any, Dict, Iterable, List, Optional, Set
import unicodedata

from .image_probe import DecodeLimits, EXT_TO_FORMAT, probe_image_file

IMAGE_EXTENSIONS = frozenset(EXT_TO_FORMAT.keys())


def _safe_relative_path(root_resolved: Path, entry_path: Path) -> Optional[str]:
    """Return normalized POSIX relative path or None if symlink/non-regular/traversal."""
    try:
        st = os.lstat(entry_path)
    except OSError:
        return None
    if stat.S_ISLNK(st.st_mode) or not stat.S_ISREG(st.st_mode):
        return None
    try:
        resolved = entry_path.resolve(strict=True)
        rel = resolved.relative_to(root_resolved)
    except (OSError, ValueError):
        return None
    if any(part == ".." for part in rel.parts):
        return None
    return rel.as_posix()


def _detect_group_provenance(group_dir: Path) -> Dict[str, Any]:
    """Detect presence of provenance/license files without claiming legal validation (item 8)."""
    notice = None
    provenance = None
    license_files: List[str] = []
    if group_dir.is_dir() and not group_dir.is_symlink():
        for child in sorted(group_dir.iterdir(), key=lambda p: p.name):
            try:
                st = os.lstat(child)
            except OSError:
                continue
            if not stat.S_ISREG(st.st_mode):
                continue
            low = child.name.lower()
            if low == "notice.txt":
                notice = child.name
            elif low == "provenance.json":
                provenance = child.name
            elif low.startswith("license"):
                license_files.append(child.name)
    has_files = bool(notice or provenance or license_files)
    return {
        "status": "arquivos_presentes_nao_validados" if has_files else "desconhecida",
        "files_present": has_files,
        "license_validated": False,
        "notice_file": notice,
        "provenance_file": provenance,
        "license_files": license_files,
    }


def resolve_fallback_asset(
    asset_id: int,
    available_ids: Set[int],
    fallback_token: str = "FALLBACK_GENERIC_PLACEHOLDER",
) -> Dict[str, Any]:
    """Explicit fallback policy: NEVER substitute another item's/species' icon for an unknown ID."""
    if asset_id in available_ids:
        return {"asset_id": asset_id, "found": True, "resolved": f"id:{asset_id}", "used_fallback": False}
    return {"asset_id": asset_id, "found": False, "resolved": fallback_token, "used_fallback": True}


def audit_assets_tree(
    assets_root: Path,
    *,
    limits: DecodeLimits = DecodeLimits(),
    expected_item_ids: Optional[Iterable[int]] = None,
    expected_dex_range: Optional[tuple[int, int]] = None,
) -> Dict[str, Any]:
    """Run full read-only audit over an assets directory tree (G-049-06 / G-049-07)."""
    root = Path(assets_root)
    # (1) Reject symlinked audit root explicitly before resolving.
    if root.is_symlink():
        raise ValueError(f"Raiz de auditoria simbólica recusada: {root}")
    try:
        root_st = os.lstat(root)
    except FileNotFoundError as exc:
        raise FileNotFoundError(f"Diretório de assets não encontrado: {root}") from exc
    if not stat.S_ISDIR(root_st.st_mode):
        raise ValueError(f"Raiz de auditoria não é um diretório regular: {root}")
    root_resolved = root.resolve(strict=True)

    # (7) Materialize expected_item_ids iterable exactly once (supports single-use generators).
    expected_item_set: Optional[Set[int]] = set(expected_item_ids) if expected_item_ids is not None else None

    unsafe_paths: List[str] = []
    image_entries: List[Dict[str, Any]] = []
    non_image_files: List[str] = []
    by_sha256: Dict[str, List[str]] = {}
    by_nfc: Dict[str, List[str]] = {}
    by_casefold: Dict[str, List[str]] = {}
    groups_seen: Set[str] = set()

    for dirpath, dirnames, filenames in os.walk(root_resolved, followlinks=False):
        dirnames.sort()
        filenames.sort()
        dp = Path(dirpath)

        safe_dirs: List[str] = []
        for d in dirnames:
            cand = dp / d
            if cand.is_symlink():
                unsafe_paths.append(f"symlink_dir:{cand.relative_to(root_resolved).as_posix()}")
            else:
                safe_dirs.append(d)
        dirnames[:] = safe_dirs

        for fname in filenames:
            fpath = dp / fname
            try:
                fst = os.lstat(fpath)
            except OSError:
                unsafe_paths.append(f"unstatable:{fname}")
                continue
            if stat.S_ISLNK(fst.st_mode):
                unsafe_paths.append(f"symlink_file:{fpath.relative_to(root_resolved).as_posix()}")
                continue
            # (2) Reject non-regular files (FIFOs, sockets, devices) before any open().
            if not stat.S_ISREG(fst.st_mode):
                unsafe_paths.append(f"non_regular_file:{fpath.relative_to(root_resolved).as_posix()}")
                continue
            rel = _safe_relative_path(root_resolved, fpath)
            if rel is None:
                unsafe_paths.append(f"traversal_or_outside:{fname}")
                continue

            top_group = rel.split("/", 1)[0] if "/" in rel else "_root"
            groups_seen.add(top_group)

            nfc_key = unicodedata.normalize("NFC", rel)
            by_nfc.setdefault(nfc_key, []).append(rel)
            cf_key = nfc_key.casefold()
            by_casefold.setdefault(cf_key, []).append(rel)

            if fpath.suffix.lower() in IMAGE_EXTENSIONS:
                probe = probe_image_file(fpath, limits=limits)
                entry = {"path": rel, "group": top_group, **probe}
                image_entries.append(entry)
                # (5) NEVER group unavailable/None hashes as binary duplicates.
                if probe["sha256"] is not None:
                    by_sha256.setdefault(probe["sha256"], []).append(rel)
            else:
                non_image_files.append(rel)

    image_entries.sort(key=lambda e: e["path"])
    unsafe_paths.sort()
    non_image_files.sort()

    duplicate_groups = [
        {"sha256": sha, "paths": sorted(paths)}
        for sha, paths in sorted(by_sha256.items())
        if len(paths) > 1
    ]
    unicode_collisions = [
        {"nfc_path": k, "variants": sorted(v)}
        for k, v in sorted(by_nfc.items())
        if len(v) > 1
    ]
    case_collisions = [
        {"casefold_path": k, "variants": sorted(v)}
        for k, v in sorted(by_casefold.items())
        if len(v) > 1
    ]

    invalid_images = [e for e in image_entries if not e["valid"]]
    unhashed_images = [e["path"] for e in image_entries if e["sha256"] is None]
    extension_mismatches = [e["path"] for e in image_entries if e["extension_mismatch"]]
    no_alpha_channel_images = [e["path"] for e in image_entries if e["valid"] and not e["alpha_channel_present"]]

    provenance_by_group = {
        g: _detect_group_provenance(root_resolved / g)
        for g in sorted(groups_seen)
        if g != "_root"
    }

    # (6) Coverage counts ONLY structurally valid images with matching format.
    coverage: Dict[str, Any] = {}
    if expected_item_set is not None:
        valid_item_ids: Set[int] = set()
        invalid_item_ids: Set[int] = set()
        for e in image_entries:
            m = re.fullmatch(r"item-icons/item-(\d+)\.png", e["path"])
            if m:
                iid = int(m.group(1))
                if e["valid"] and not e["extension_mismatch"]:
                    valid_item_ids.add(iid)
                else:
                    invalid_item_ids.add(iid)
        missing_items = sorted(expected_item_set - valid_item_ids)
        coverage["item_icons"] = {
            "expected": len(expected_item_set),
            "present": len(valid_item_ids & expected_item_set),
            "missing_ids": missing_items,
            "invalid_ids": sorted(invalid_item_ids & expected_item_set),
        }
    if expected_dex_range is not None:
        lo, hi = expected_dex_range
        valid_dex: Set[int] = set()
        invalid_dex: Set[int] = set()
        for e in image_entries:
            m = re.fullmatch(r"feed-icons/pokemon/(\d+)\.png", e["path"])
            if m:
                did = int(m.group(1))
                if e["valid"] and not e["extension_mismatch"]:
                    valid_dex.add(did)
                else:
                    invalid_dex.add(did)
        expected_set = set(range(lo, hi + 1))
        coverage["feed_pokemon_dex"] = {
            "range": [lo, hi],
            "present": len(valid_dex & expected_set),
            "missing_ids": sorted(expected_set - valid_dex),
            "invalid_ids": sorted(invalid_dex & expected_set),
        }

    return {
        "schema": 2,
        "mutated": False,
        "device_tested": False,
        "probe_scope": "structural_header_and_framing_only",
        "pixel_decode_verified": False,
        "summary": {
            "image_count": len(image_entries),
            "non_image_count": len(non_image_files),
            "valid_image_count": len(image_entries) - len(invalid_images),
            "invalid_image_count": len(invalid_images),
            "unhashed_image_count": len(unhashed_images),
            "extension_mismatch_count": len(extension_mismatches),
            "no_alpha_channel_count": len(no_alpha_channel_images),
            "opaque_image_count": len(no_alpha_channel_images),
            "duplicate_hash_groups": len(duplicate_groups),
            "unicode_collision_count": len(unicode_collisions),
            "case_collision_count": len(case_collisions),
            "unsafe_path_count": len(unsafe_paths),
        },
        "provenance_by_group": provenance_by_group,
        "invalid_images": [{"path": e["path"], "errors": e["errors"]} for e in invalid_images],
        "unhashed_images": unhashed_images,
        "extension_mismatches": extension_mismatches,
        "duplicate_groups": duplicate_groups,
        "unicode_collisions": unicode_collisions,
        "case_collisions": case_collisions,
        "unsafe_paths": unsafe_paths,
        "coverage": coverage,
        "entries": image_entries,
    }


def build_deterministic_manifest(audit_result: Dict[str, Any]) -> Dict[str, Any]:
    """Produce a deterministic JSON manifest and its SHA-256 digest."""
    payload = {
        "schema": audit_result["schema"],
        "probe_scope": audit_result["probe_scope"],
        "pixel_decode_verified": audit_result["pixel_decode_verified"],
        "summary": audit_result["summary"],
        "provenance_by_group": audit_result["provenance_by_group"],
        "entries": [
            {
                "path": e["path"],
                "bytes": e["bytes"],
                "sha256": e["sha256"],
                "hash_status": e["hash_status"],
                "format": e["detected_format"],
                "width": e["width"],
                "height": e["height"],
                "alpha_channel_present": e["alpha_channel_present"],
                "valid": e["valid"],
            }
            for e in audit_result["entries"]
        ],
    }
    canonical = json.dumps(payload, sort_keys=True, separators=(",", ":"), ensure_ascii=False).encode("utf-8")
    return {
        "manifest_sha256": hashlib.sha256(canonical).hexdigest(),
        "manifest_json": canonical.decode("utf-8"),
    }


def render_ptbr_report(audit_result: Dict[str, Any]) -> str:
    """Render concise PT-BR audit report clearly stating structural probe limits."""
    s = audit_result["summary"]
    lines = [
        "PGBR — Relatório Offline de Auditoria Estrutural de Sprites e Ícones (DATA-06 / INV-01)",
        "Escopo: probe estrutural de cabeçalho/chunks/CRC (não realiza decodificação completa de pixels nem parser completo de SVG).",
        f"Imagens analisadas: {s['image_count']} (válidas estruturalmente: {s['valid_image_count']}, inválidas: {s['invalid_image_count']}, sem hash/oversized: {s['unhashed_image_count']})",
        f"Outros arquivos: {s['non_image_count']}",
        f"Divergências formato/extensão: {s['extension_mismatch_count']}",
        f"Imagens sem canal alpha/transparência potencial (não comprova pixels transparentes): {s['no_alpha_channel_count']}",
        f"Grupos de duplicatas binárias (SHA-256 computado): {s['duplicate_hash_groups']}",
        f"Colisões Unicode (NFC/NFD): {s['unicode_collision_count']} · Colisões de caixa (case): {s['case_collision_count']}",
        f"Caminhos inseguros (symlinks/não regulares/traversal): {s['unsafe_path_count']}",
        "Proveniência/licença por grupo (presença de arquivo ≠ validação jurídica da licença):",
    ]
    for grp, prov in sorted(audit_result["provenance_by_group"].items()):
        lines.append(f"  - {grp}: {prov['status']} (license_validated={prov['license_validated']})")
    lines.append("Modo estritamente somente leitura (mutated=false, pixel_decode_verified=false, device_tested=false).")
    return "\n".join(lines)
