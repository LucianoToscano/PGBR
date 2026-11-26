"""Full read-only offline auditor for PGBR sprites & icons (G-049-06 / DATA-06 / INV-01).

Covers all 12 audit dimensions:
(1) Real file inventory, (2) dimensions, (3) transparency, (4) real format vs extension,
(5) truncated/corrupt files, (6) binary duplicates by SHA-256, (7) Unicode NFC & case
path collisions, (8) traversal & symlink protection, (9) max size/decode bounds,
(10) explicit or unknown provenance/license per group, (11) coverage gaps & explicit
fallback policy, (12) deterministic manifest & PT-BR report.
"""
from __future__ import annotations

import hashlib
import json
import os
from pathlib import Path
import re
from typing import Any, Dict, Iterable, List, Optional, Set
import unicodedata

from .image_probe import DecodeLimits, EXT_TO_FORMAT, probe_image_file

IMAGE_EXTENSIONS = frozenset(EXT_TO_FORMAT.keys())


def _safe_relative_path(root_resolved: Path, entry_path: Path) -> Optional[str]:
    """Return normalized POSIX relative path or None if symlink/traversal/outside root."""
    if entry_path.is_symlink():
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
    """Detect explicit provenance/license files or report 'desconhecida' (never fabricate)."""
    notice = None
    provenance = None
    license_files: List[str] = []
    if group_dir.is_dir():
        for child in sorted(group_dir.iterdir(), key=lambda p: p.name):
            if child.is_symlink() or not child.is_file():
                continue
            low = child.name.lower()
            if low == "notice.txt":
                notice = child.name
            elif low == "provenance.json":
                provenance = child.name
            elif low.startswith("license"):
                license_files.append(child.name)
    has_explicit = bool(notice or provenance or license_files)
    return {
        "status": "explicita" if has_explicit else "desconhecida",
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
    """Run full read-only audit over an assets directory tree (G-049-06)."""
    root = Path(assets_root)
    if not root.is_dir():
        raise FileNotFoundError(f"Diretório de assets não encontrado: {root}")
    root_resolved = root.resolve(strict=True)

    unsafe_paths: List[str] = []
    image_entries: List[Dict[str, Any]] = []
    non_image_files: List[str] = []
    by_sha256: Dict[str, List[str]] = {}
    by_nfc: Dict[str, List[str]] = {}
    by_casefold: Dict[str, List[str]] = {}
    groups_seen: Set[str] = set()

    # Walk with followlinks=False so symlinks are detected and NEVER followed.
    for dirpath, dirnames, filenames in os.walk(root_resolved, followlinks=False):
        dirnames.sort()
        filenames.sort()
        dp = Path(dirpath)

        # Reject symlinked subdirectories immediately and prune them.
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
            if fpath.is_symlink():
                unsafe_paths.append(f"symlink_file:{fpath.relative_to(root_resolved).as_posix()}")
                continue
            rel = _safe_relative_path(root_resolved, fpath)
            if rel is None:
                unsafe_paths.append(f"traversal_or_outside:{fname}")
                continue

            top_group = rel.split("/", 1)[0] if "/" in rel else "_root"
            groups_seen.add(top_group)

            # Track Unicode NFC and case-insensitive path collisions.
            nfc_key = unicodedata.normalize("NFC", rel)
            by_nfc.setdefault(nfc_key, []).append(rel)
            cf_key = nfc_key.casefold()
            by_casefold.setdefault(cf_key, []).append(rel)

            if fpath.suffix.lower() in IMAGE_EXTENSIONS:
                probe = probe_image_file(fpath, limits=limits)
                entry = {"path": rel, "group": top_group, **probe}
                image_entries.append(entry)
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
    extension_mismatches = [e["path"] for e in image_entries if e["extension_mismatch"]]
    opaque_images = [e["path"] for e in image_entries if e["valid"] and not e["has_alpha"]]

    provenance_by_group = {
        g: _detect_group_provenance(root_resolved / g)
        for g in sorted(groups_seen)
        if g != "_root"
    }

    # (11) Coverage gaps (explicit, read-only)
    coverage: Dict[str, Any] = {}
    if expected_item_ids is not None:
        present_item_ids: Set[int] = set()
        for e in image_entries:
            m = re.fullmatch(r"item-icons/item-(\d+)\.png", e["path"])
            if m:
                present_item_ids.add(int(m.group(1)))
        missing_items = sorted(set(expected_item_ids) - present_item_ids)
        coverage["item_icons"] = {
            "expected": len(set(expected_item_ids)),
            "present": len(present_item_ids & set(expected_item_ids)),
            "missing_ids": missing_items,
        }
    if expected_dex_range is not None:
        lo, hi = expected_dex_range
        present_dex: Set[int] = set()
        for e in image_entries:
            m = re.fullmatch(r"feed-icons/pokemon/(\d+)\.png", e["path"])
            if m:
                present_dex.add(int(m.group(1)))
        expected_set = set(range(lo, hi + 1))
        coverage["feed_pokemon_dex"] = {
            "range": [lo, hi],
            "present": len(present_dex & expected_set),
            "missing_ids": sorted(expected_set - present_dex),
        }

    return {
        "schema": 1,
        "mutated": False,
        "device_tested": False,
        "summary": {
            "image_count": len(image_entries),
            "non_image_count": len(non_image_files),
            "valid_image_count": len(image_entries) - len(invalid_images),
            "invalid_image_count": len(invalid_images),
            "extension_mismatch_count": len(extension_mismatches),
            "opaque_image_count": len(opaque_images),
            "duplicate_hash_groups": len(duplicate_groups),
            "unicode_collision_count": len(unicode_collisions),
            "case_collision_count": len(case_collisions),
            "unsafe_path_count": len(unsafe_paths),
        },
        "provenance_by_group": provenance_by_group,
        "invalid_images": [{"path": e["path"], "errors": e["errors"]} for e in invalid_images],
        "extension_mismatches": extension_mismatches,
        "duplicate_groups": duplicate_groups,
        "unicode_collisions": unicode_collisions,
        "case_collisions": case_collisions,
        "unsafe_paths": unsafe_paths,
        "coverage": coverage,
        "entries": image_entries,
    }


def build_deterministic_manifest(audit_result: Dict[str, Any]) -> Dict[str, Any]:
    """Produce a deterministic JSON manifest and its SHA-256 digest (item 12)."""
    payload = {
        "schema": audit_result["schema"],
        "summary": audit_result["summary"],
        "provenance_by_group": audit_result["provenance_by_group"],
        "entries": [
            {
                "path": e["path"],
                "bytes": e["bytes"],
                "sha256": e["sha256"],
                "format": e["detected_format"],
                "width": e["width"],
                "height": e["height"],
                "has_alpha": e["has_alpha"],
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
    """Render concise PT-BR audit report without mutating assets (item 12)."""
    s = audit_result["summary"]
    lines = [
        "PGBR — Relatório Offline de Auditoria de Sprites e Ícones (DATA-06 / INV-01)",
        f"Imagens analisadas: {s['image_count']} (válidas: {s['valid_image_count']}, inválidas: {s['invalid_image_count']})",
        f"Outros arquivos: {s['non_image_count']}",
        f"Divergências formato/extensão: {s['extension_mismatch_count']}",
        f"Imagens sem canal alpha/transparência: {s['opaque_image_count']}",
        f"Grupos de duplicatas binárias (SHA-256): {s['duplicate_hash_groups']}",
        f"Colisões Unicode (NFC/NFD): {s['unicode_collision_count']} · Colisões de caixa (case): {s['case_collision_count']}",
        f"Caminhos inseguros (symlinks/traversal): {s['unsafe_path_count']}",
        "Proveniência/licença por grupo:",
    ]
    for grp, prov in sorted(audit_result["provenance_by_group"].items()):
        lines.append(f"  - {grp}: {prov['status']}")
    lines.append("Modo estritamente somente leitura (mutated=false, device_tested=false).")
    return "\n".join(lines)
