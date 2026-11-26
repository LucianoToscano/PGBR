"""Offline Visual Identity & Provenance Auditor for PGBR Icons (P0 / LT050).

Guarantees:
- 100% read-only offline analysis; never modifies PNGs, manifests, or Java/C++ runtime.
- Uses O_RDONLY | O_BINARY | O_NOFOLLOW with strict byte bounds and symlink rejection.
- Explicitly differentiates PNG structural integrity (magic/IHDR/IDAT/IEND/CRC32)
  from semantic visual identity (ID -> image -> named source) and from live 0.439.0
  runtime verification (runtime_0439_verified is ALWAYS False offline).
- Never fabricates correspondences or replaces assets without verified evidence.
"""

from __future__ import annotations

import hashlib
import json
import os
import re
import stat
import struct
import zlib
from collections import defaultdict
from dataclasses import dataclass
from pathlib import Path
from typing import Any, Dict, List, Optional, Sequence, Tuple

MAX_ICON_BYTES = 2_000_000  # 2 MB hard cap per icon read
MAX_MANIFEST_BYTES = 2_000_000

P0_CATEGORIES: Tuple[str, ...] = (
    "poeira_estelar",
    "incensos",
    "modulos",
    "mt",
    "evolucao",
    "frutas",
    "pokebolas",
    "pocoes",
    "outros",
)

VISUAL_IDENTITY_STATUSES: Tuple[str, ...] = (
    "byte_identical_named_ref_unverified_runtime",
    "visual_match_distinct_bytes_unverified_runtime",
    "numeric_ref_only_unverified_semantic_identity",
    "expansion_historical_enum_unverified_runtime",
    "named_asset_unlisted_in_map_provenance",
    "missing_from_manifest",
    "missing_on_disk",
    "png_integrity_failed",
)

_GALLERY_CASE_RE = re.compile(r'case\s+(\d+)\s*:\s*return\s+"([^"]+)"\s*;')
_GALLERY_IDS_RE = re.compile(r"GALLERY_IDS\s*=\s*\{([0-9,\s]+)\}")


def _read_bounded_regular(path: Path, max_bytes: int) -> bytes:
    """Safely read a regular non-symlink file with bounded size and O_BINARY."""
    if isinstance(max_bytes, bool) or not isinstance(max_bytes, int) or max_bytes <= 0:
        raise ValueError("max_bytes deve ser inteiro positivo.")
    if path.is_symlink():
        raise ValueError(f"Symlink recusado por segurança: {path.name}")
    flags = os.O_RDONLY | getattr(os, "O_BINARY", 0) | getattr(os, "O_NOFOLLOW", 0)
    fd = os.open(str(path), flags)
    try:
        st = os.fstat(fd)
        if not stat.S_ISREG(st.st_mode):
            raise ValueError(f"Arquivo não regular recusado: {path.name}")
        chunks: List[bytes] = []
        total = 0
        while total <= max_bytes:
            chunk = os.read(fd, min(65536, max_bytes + 1 - total))
            if not chunk:
                break
            chunks.append(chunk)
            total += len(chunk)
        if total > max_bytes:
            raise ValueError(f"Arquivo excede o limite de {max_bytes} bytes: {path.name}")
        return b"".join(chunks)
    finally:
        os.close(fd)


def verify_png_integrity_bytes(data: bytes) -> Tuple[bool, Optional[Tuple[int, int]], str]:
    """Validate PNG signature, chunk bounds, CRC32, IHDR/IDAT/IEND (integrity only, NOT visual identity)."""
    if len(data) < 33 or data[:8] != b"\x89PNG\r\n\x1a\n":
        return False, None, "assinatura PNG inválida"
    cursor = 8
    seen_ihdr = False
    seen_idat = False
    seen_iend = False
    dims: Optional[Tuple[int, int]] = None

    while cursor < len(data):
        if len(data) - cursor < 12:
            return False, dims, "chunk PNG truncado"
        length, kind = struct.unpack_from(">I4s", data, cursor)
        end = cursor + 12 + length
        if end > len(data):
            return False, dims, f"chunk {kind!r} excede tamanho do arquivo"
        payload = data[cursor + 8 : end - 4]
        expected_crc = struct.unpack_from(">I", data, end - 4)[0]
        actual_crc = zlib.crc32(kind + payload) & 0xFFFFFFFF
        if actual_crc != expected_crc:
            return False, dims, f"CRC32 inválido no chunk {kind!r}"
        if not seen_ihdr:
            if kind != b"IHDR" or length != 13:
                return False, dims, "primeiro chunk não é IHDR válido"
            w, h = struct.unpack_from(">II", payload)
            if not (1 <= w <= 8192 and 1 <= h <= 8192):
                return False, dims, f"dimensões PNG fora do intervalo: {w}x{h}"
            dims = (w, h)
            seen_ihdr = True
        elif kind == b"IHDR":
            return False, dims, "IHDR duplicado"
        if kind == b"IDAT":
            seen_idat = True
        if kind == b"IEND":
            if length != 0 or end != len(data):
                return False, dims, "IEND inválido ou bytes extras após IEND"
            seen_iend = True
            break
        cursor = end

    if not (seen_ihdr and seen_idat and seen_iend):
        return False, dims, "estrutura PNG incompleta (IHDR/IDAT/IEND)"
    return True, dims, "ok"


def _git_blob_sha1(data: bytes) -> str:
    return hashlib.sha1(f"blob {len(data)}\0".encode("ascii") + data).hexdigest()


def classify_p0_category(item_id: Optional[int], asset_relpath: str = "", source_name: str = "") -> str:
    """Classify an item ID or named asset into the owner's P0 visual audit categories."""
    low = f"{asset_relpath} {source_name}".lower()
    if "stardust" in low:
        return "poeira_estelar"
    if "incense" in low or (item_id is not None and item_id in (401, 405, 406, 407)):
        return "incensos"
    if item_id is not None:
        if 1 <= item_id <= 99:
            return "pokebolas"
        if 101 <= item_id <= 199:
            return "pocoes"
        if 501 <= item_id <= 599:
            return "modulos"
        if 701 <= item_id <= 799:
            return "frutas"
        if 1101 <= item_id <= 1199:
            return "evolucao"
        if 1201 <= item_id <= 1299:
            return "mt"
    return "outros"


@dataclass(frozen=True)
class IconIdentityRecord:
    """Auditable ID -> image -> source -> visual-identity-status mapping row."""

    key: str  # e.g. "item:701" or "named:stardust.png"
    item_id: Optional[int]
    asset_path: str
    p0_category: str
    ui_label_ptbr: Optional[str]
    manifest_source: str  # "LT046_ITEM_ICON_REFERENCE" | "LT046_ITEM_ICON_EXPANSION" | "BOTH_OVERLAP" | "NONE"
    source_filename: Optional[str]
    source_scope_note: str
    bytes_on_disk: Optional[int]
    sha256: Optional[str]
    git_blob_sha1: Optional[str]
    dimensions: Optional[Tuple[int, int]]
    png_integrity_ok: bool
    manifest_blob_match: bool
    stale_reference_blob_detected: bool
    visual_identity_status: str
    visual_identity_confirmed: bool  # Always False without live/visual ground-truth verification
    runtime_0439_verified: bool  # Always False offline


@dataclass(frozen=True)
class VisualIdentityReport:
    """Complete offline visual identity audit summary and per-asset receipt."""

    repo_root: str
    total_item_icons_on_disk: int
    total_named_expansion_icons: int
    gallery_ids_count: int
    gallery_missing_from_disk: Tuple[int, ...]
    disk_missing_from_gallery: Tuple[int, ...]
    manifest_overlap_ids: Tuple[int, ...]
    stale_reference_blob_ids: Tuple[int, ...]
    duplicate_sha256_groups: Tuple[Tuple[str, Tuple[str, ...]], ...]
    named_missing_from_map_provenance: Tuple[str, ...]
    counts_by_visual_status: Dict[str, int]
    counts_by_p0_category: Dict[str, Dict[str, int]]
    records: Tuple[IconIdentityRecord, ...]
    evidence_limits_ptbr: Tuple[str, ...]


def detect_duplicate_image_hashes(records: Sequence[IconIdentityRecord]) -> List[Tuple[str, Tuple[str, ...]]]:
    """Detect distinct asset keys sharing the exact same SHA-256 bytes (visual collision check)."""
    by_sha: Dict[str, List[str]] = defaultdict(list)
    for rec in records:
        if rec.sha256 is not None:
            by_sha[rec.sha256].append(rec.key)
    collisions: List[Tuple[str, Tuple[str, ...]]] = []
    for sha, keys in sorted(by_sha.items()):
        if len(keys) > 1:
            collisions.append((sha, tuple(sorted(keys))))
    return collisions


def _parse_local_audit_dialog(dialog_path: Path) -> Tuple[List[int], Dict[int, str]]:
    if not dialog_path.is_file() or dialog_path.is_symlink():
        return [], {}
    text = _read_bounded_regular(dialog_path, MAX_MANIFEST_BYTES).decode("utf-8", errors="replace")
    gallery_ids: List[int] = []
    m = _GALLERY_IDS_RE.search(text)
    if m:
        gallery_ids = [int(tok.strip()) for tok in m.group(1).split(",") if tok.strip().isdigit()]
    labels: Dict[int, str] = {}
    for cm in _GALLERY_CASE_RE.finditer(text):
        labels[int(cm.group(1))] = cm.group(2)
    return gallery_ids, labels


def audit_visual_identity(repo_root: Path) -> VisualIdentityReport:
    """Audit item-icons and named map-icons against LT046 manifests and UI gallery (read-only)."""
    root = Path(repo_root)
    if not root.is_dir():
        raise ValueError("repo_root deve ser um diretório existente.")

    ref_path = root / "docs" / "validation" / "LT046_ITEM_ICON_REFERENCE.json"
    exp_path = root / "docs" / "validation" / "LT046_ITEM_ICON_EXPANSION.json"
    map_prov_path = root / "pgrazor" / "assets" / "map-icons" / "provenance.json"
    icons_dir = root / "pgrazor" / "assets" / "item-icons"
    dialog_path = root / "pgrazor" / "src" / "dev" / "pgrazor" / "LocalAuditDialog.java"

    ref_data = json.loads(_read_bounded_regular(ref_path, MAX_MANIFEST_BYTES).decode("utf-8"))
    exp_data = json.loads(_read_bounded_regular(exp_path, MAX_MANIFEST_BYTES).decode("utf-8"))
    map_prov_names: set[str] = set()
    if map_prov_path.is_file() and not map_prov_path.is_symlink():
        mp = json.loads(_read_bounded_regular(map_prov_path, MAX_MANIFEST_BYTES).decode("utf-8"))
        for row in mp.get("icons", []):
            if isinstance(row, dict) and isinstance(row.get("name"), str):
                map_prov_names.add(row["name"])

    ref_by_id: Dict[int, Dict[str, Any]] = {int(r["id"]): r for r in ref_data.get("items", [])}
    exp_by_id: Dict[int, Dict[str, Any]] = {int(r["id"]): r for r in exp_data.get("items", [])}
    gallery_ids, gallery_labels = _parse_local_audit_dialog(dialog_path)

    disk_by_id: Dict[int, Path] = {}
    if icons_dir.is_dir():
        for p in sorted(icons_dir.iterdir()):
            m = re.fullmatch(r"item-(\d+)\.png", p.name)
            if m:
                disk_by_id[int(m.group(1))] = p

    all_ids = sorted(set(disk_by_id) | set(ref_by_id) | set(exp_by_id))
    overlap_ids = sorted(set(ref_by_id) & set(exp_by_id))
    stale_ref_ids: List[int] = []
    records: List[IconIdentityRecord] = []

    for iid in all_ids:
        rel_path = f"pgrazor/assets/item-icons/item-{iid}.png"
        disk_path = disk_by_id.get(iid)
        in_ref = iid in ref_by_id
        in_exp = iid in exp_by_id

        if in_ref and in_exp:
            manifest_source = "BOTH_OVERLAP"
        elif in_exp:
            manifest_source = "LT046_ITEM_ICON_EXPANSION"
        elif in_ref:
            manifest_source = "LT046_ITEM_ICON_REFERENCE"
        else:
            manifest_source = "NONE"

        ref_row = ref_by_id.get(iid)
        exp_row = exp_by_id.get(iid)
        active_row = exp_row if exp_row is not None else ref_row

        source_filename: Optional[str] = None
        source_scope_note = ""
        if exp_row is not None:
            source_filename = exp_row.get("source")
            source_scope_note = str(exp_data.get("scope", ""))
        elif ref_row is not None:
            named = ref_row.get("drive_named_reference") or {}
            comp = ref_row.get("comparison") or {}
            source_filename = named.get("filename") or comp.get("reference_filename")
            source_scope_note = str(ref_data.get("source_scope", ""))

        ui_label = gallery_labels.get(iid) or (exp_row.get("label") if exp_row else None)
        p0_cat = classify_p0_category(iid, rel_path, source_filename or "")

        if disk_path is None:
            records.append(
                IconIdentityRecord(
                    key=f"item:{iid}",
                    item_id=iid,
                    asset_path=rel_path,
                    p0_category=p0_cat,
                    ui_label_ptbr=ui_label,
                    manifest_source=manifest_source,
                    source_filename=source_filename,
                    source_scope_note=source_scope_note,
                    bytes_on_disk=None,
                    sha256=None,
                    git_blob_sha1=None,
                    dimensions=None,
                    png_integrity_ok=False,
                    manifest_blob_match=False,
                    stale_reference_blob_detected=False,
                    visual_identity_status="missing_on_disk",
                    visual_identity_confirmed=False,
                    runtime_0439_verified=False,
                )
            )
            continue

        raw = _read_bounded_regular(disk_path, MAX_ICON_BYTES)
        png_ok, dims, _ = verify_png_integrity_bytes(raw)
        sha256 = hashlib.sha256(raw).hexdigest()
        blob_sha1 = _git_blob_sha1(raw)

        manifest_blob_match = False
        if active_row is not None:
            manifest_blob_match = (
                active_row.get("git_blob_sha1") == blob_sha1 and active_row.get("bytes") == len(raw)
            )

        stale_ref = False
        if ref_row is not None and ref_row.get("git_blob_sha1") != blob_sha1:
            stale_ref = True
            stale_ref_ids.append(iid)

        if not png_ok:
            vis_status = "png_integrity_failed"
        elif manifest_source == "NONE":
            vis_status = "missing_from_manifest"
        elif exp_row is not None:
            vis_status = "expansion_historical_enum_unverified_runtime"
        else:
            comp = (ref_row or {}).get("comparison") or {}
            comp_status = comp.get("status", "")
            has_named_ref = bool((ref_row or {}).get("drive_named_reference"))
            if comp_status == "visual_match_distinct_bytes":
                vis_status = "visual_match_distinct_bytes_unverified_runtime"
            elif comp_status == "byte_identical" and has_named_ref:
                vis_status = "byte_identical_named_ref_unverified_runtime"
            else:
                vis_status = "numeric_ref_only_unverified_semantic_identity"

        records.append(
            IconIdentityRecord(
                key=f"item:{iid}",
                item_id=iid,
                asset_path=rel_path,
                p0_category=p0_cat,
                ui_label_ptbr=ui_label,
                manifest_source=manifest_source,
                source_filename=source_filename,
                source_scope_note=source_scope_note,
                bytes_on_disk=len(raw),
                sha256=sha256,
                git_blob_sha1=blob_sha1,
                dimensions=dims,
                png_integrity_ok=png_ok,
                manifest_blob_match=manifest_blob_match,
                stale_reference_blob_detected=stale_ref,
                visual_identity_status=vis_status,
                visual_identity_confirmed=False,
                runtime_0439_verified=False,
            )
        )

    # Also audit named expansion assets (e.g. pgrazor/assets/map-icons/stardust.png)
    named_rows = exp_data.get("named", [])
    named_missing_prov: List[str] = []
    for nrow in named_rows:
        rel_path = str(nrow["path"])
        fname = Path(rel_path).name
        disk_path = root / rel_path
        src_name = nrow.get("source")
        p0_cat = classify_p0_category(None, rel_path, src_name or "")
        in_map_prov = fname in map_prov_names
        if not in_map_prov:
            named_missing_prov.append(rel_path)
        if not disk_path.is_file():
            records.append(
                IconIdentityRecord(
                    key=f"named:{fname}",
                    item_id=None,
                    asset_path=rel_path,
                    p0_category=p0_cat,
                    ui_label_ptbr=nrow.get("label"),
                    manifest_source="LT046_ITEM_ICON_EXPANSION",
                    source_filename=src_name,
                    source_scope_note=str(exp_data.get("scope", "")),
                    bytes_on_disk=None,
                    sha256=None,
                    git_blob_sha1=None,
                    dimensions=None,
                    png_integrity_ok=False,
                    manifest_blob_match=False,
                    stale_reference_blob_detected=False,
                    visual_identity_status="missing_on_disk",
                    visual_identity_confirmed=False,
                    runtime_0439_verified=False,
                )
            )
            continue

        raw = _read_bounded_regular(disk_path, MAX_ICON_BYTES)
        png_ok, dims, _ = verify_png_integrity_bytes(raw)
        sha256 = hashlib.sha256(raw).hexdigest()
        blob_sha1 = _git_blob_sha1(raw)
        blob_ok = nrow.get("git_blob_sha1") == blob_sha1 and nrow.get("bytes") == len(raw)
        vis_status = (
            "png_integrity_failed"
            if not png_ok
            else ("named_asset_unlisted_in_map_provenance" if not in_map_prov else "expansion_historical_enum_unverified_runtime")
        )
        records.append(
            IconIdentityRecord(
                key=f"named:{fname}",
                item_id=None,
                asset_path=rel_path,
                p0_category=p0_cat,
                ui_label_ptbr=nrow.get("label"),
                manifest_source="LT046_ITEM_ICON_EXPANSION",
                source_filename=src_name,
                source_scope_note=str(exp_data.get("scope", "")),
                bytes_on_disk=len(raw),
                sha256=sha256,
                git_blob_sha1=blob_sha1,
                dimensions=dims,
                png_integrity_ok=png_ok,
                manifest_blob_match=blob_ok,
                stale_reference_blob_detected=False,
                visual_identity_status=vis_status,
                visual_identity_confirmed=False,
                runtime_0439_verified=False,
            )
        )

    counts_by_status: Dict[str, int] = defaultdict(int)
    counts_by_cat: Dict[str, Dict[str, int]] = {cat: defaultdict(int) for cat in P0_CATEGORIES}
    for rec in records:
        counts_by_status[rec.visual_identity_status] += 1
        counts_by_cat[rec.p0_category][rec.visual_identity_status] += 1

    collisions = detect_duplicate_image_hashes(records)
    gallery_set = set(gallery_ids)
    disk_set = set(disk_by_id)

    limits = (
        "Integridade estrutural PNG (assinatura/IHDR/IDAT/IEND/CRC32) não comprova identidade visual semântica nem correspondência com o runtime 0.439.0.",
        "IDs presentes apenas com referência numérica (Item_XXXX.png) não possuem verificação semântica nomeada (inclui Frutas 701..707 e MTs 1201..1204).",
        "Itens de LT046_ITEM_ICON_EXPANSION.json declaram escopo baseado em enum histórico público, explicitamente não verificado no runtime ativo.",
        "Nenhuma imagem foi substituída ou inferida sem evidência visual/referência autorizada; device_tested=false.",
    )

    return VisualIdentityReport(
        repo_root=str(root),
        total_item_icons_on_disk=len(disk_by_id),
        total_named_expansion_icons=len(named_rows),
        gallery_ids_count=len(gallery_ids),
        gallery_missing_from_disk=tuple(sorted(gallery_set - disk_set)),
        disk_missing_from_gallery=tuple(sorted(disk_set - gallery_set)),
        manifest_overlap_ids=tuple(overlap_ids),
        stale_reference_blob_ids=tuple(sorted(stale_ref_ids)),
        duplicate_sha256_groups=tuple(collisions),
        named_missing_from_map_provenance=tuple(sorted(named_missing_prov)),
        counts_by_visual_status=dict(counts_by_status),
        counts_by_p0_category={k: dict(v) for k, v in counts_by_cat.items() if v},
        records=tuple(records),
        evidence_limits_ptbr=limits,
    )


def format_receipt_text(report: VisualIdentityReport) -> str:
    """Format a deterministic plain-text ID -> image -> source -> status receipt."""
    lines = [
        "MAPA AUDITAVEL ID -> IMAGEM -> FONTE -> STATUS DE IDENTIDADE VISUAL (OFFLINE P0)",
        f"Total item-icons em disco: {report.total_item_icons_on_disk}",
        f"Total named expansion icons: {report.total_named_expansion_icons}",
        f"Overlap entre manifestos LT046: {list(report.manifest_overlap_ids)}",
        f"IDs com git_blob_sha1 obsoleto em REFERENCE: {list(report.stale_reference_blob_ids)}",
        f"Colisoes SHA-256 entre IDs distintos: {len(report.duplicate_sha256_groups)}",
        f"Assets nomeados ausentes em map-icons/provenance.json: {list(report.named_missing_from_map_provenance)}",
        "",
        "CHAVE | CATEGORIA_P0 | IMAGEM | FONTE_MANIFESTO | ROTULO_UI | PNG_OK | STATUS_IDENTIDADE_VISUAL | SHA256_12",
    ]
    for rec in report.records:
        sha12 = rec.sha256[:12] if rec.sha256 else "NONE"
        lines.append(
            f"{rec.key} | {rec.p0_category} | {rec.asset_path} | "
            f"{rec.source_filename or 'NONE'} ({rec.manifest_source}) | "
            f"{rec.ui_label_ptbr or 'SEM_ROTULO'} | {rec.png_integrity_ok} | "
            f"{rec.visual_identity_status} | {sha12}"
        )
    return "\n".join(lines) + "\n"
