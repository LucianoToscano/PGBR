"""Embedded Dataset Inventory, Provenance Manifest, PT-BR Glossary & Cross-Language Type Audit (DATA-01..DATA-05).

Note: Feed sprite manifest validation before APK packaging (DATA-06) is owned by
PR #318 (pgrazor/build.py & pgrazor/test/test_feed_sprite_integrity.py) and is
intentionally NOT duplicated here.
"""
from __future__ import annotations

import hashlib
from pathlib import Path
import re
from typing import Any, Dict, List, Tuple

from .type_chart import CHART, TYPES_PT

# Categorized PT-BR glossary with explicit fallback (DATA-04).
PTBR_DATA_GLOSSARY: Dict[str, Dict[str, str]] = {
    "status": {
        "PROTECTED": "Protegido",
        "UNCERTAIN": "Incerto (requer confirmação)",
        "MATCH": "Elegível ao filtro (nenhuma ação executada)",
    },
    "max_role": {
        "attacker": "Atacante Max",
        "tank": "Tanque",
        "healer": "Suporte / Escudo",
    },
    "league": {
        "little": "Copa Pequena (500 PC)",
        "great": "Grande Liga (1500 PC)",
        "ultra": "Ultra-Liga (2500 PC)",
        "master": "Liga Mestra (sem limite)",
    },
    "event_status": {
        "active": "Ativo agora",
        "upcoming": "Próximo evento",
        "expired": "Encerrado",
    },
    "research_kind": {
        "field": "Pesquisa de Campo",
        "timed": "Pesquisa Temporária",
        "special": "Pesquisa Especial",
        "collection_challenge": "Desafio de Coleção",
    },
}


def translate_glossary(category: str, key: str) -> str:
    """Return PT-BR glossary label or explicit untranslated fallback (DATA-04)."""
    table = PTBR_DATA_GLOSSARY.get(category)
    if table and key in table:
        return table[key]
    return f"[sem tradução:{category}/{key}]"


def _file_sha256(path: Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as fh:
        for chunk in iter(lambda: fh.read(65536), b""):
            h.update(chunk)
    return h.hexdigest()


def build_embedded_inventory(repo_root: Path) -> Dict[str, Any]:
    """Inventory embedded datasets and i18n dictionaries with SHA-256 (DATA-01 / DATA-02).

    Does not duplicate PR #318 feed-sprite packaging validation.
    """
    root = Path(repo_root)
    targets = [
        ("i18n_ptbr", root / "pgrazor/i18n/pt-BR.json", "PGBR/PGRazor", "project-license"),
        ("pvp_data_native", root / "pgrazor/native/pvp_data.h", "PGBR/PGRazor", "project-license"),
        ("battle_counter_native", root / "pgrazor/native/battle_counter.h", "PGBR/PGRazor", "project-license"),
        ("game_input_0439_manifest", root / "tools/game_input_0439_manifest.json", "PGBR-input-check", "metadata-only"),
    ]
    entries: List[Dict[str, Any]] = []
    for key, path, source, license_ref in targets:
        if path.is_file():
            entries.append({
                "id": key,
                "path": str(path.relative_to(root)),
                "present": True,
                "bytes": path.stat().st_size,
                "sha256": _file_sha256(path),
                "source": source,
                "license": license_ref,
            })
        else:
            entries.append({
                "id": key,
                "path": str(path.relative_to(root)),
                "present": False,
                "bytes": 0,
                "sha256": None,
                "source": source,
                "license": license_ref,
            })

    item_icons_dir = root / "pgrazor/assets/item-icons"
    item_icon_ids: List[int] = []
    if item_icons_dir.is_dir():
        for png in sorted(item_icons_dir.glob("item-*.png")):
            m = re.fullmatch(r"item-(\d+)\.png", png.name)
            if m:
                item_icon_ids.append(int(m.group(1)))

    return {
        "schema": 1,
        "datasets": entries,
        "item_icon_count": len(item_icon_ids),
        "item_icon_ids_unique": len(item_icon_ids) == len(set(item_icon_ids)),
    }


def _parse_c_or_java_18x18_matrix(text: str) -> List[Tuple[int, ...]]:
    rows: List[Tuple[int, ...]] = []
    for match in re.finditer(r"\{\s*(-?\d+(?:\s*,\s*-?\d+){17})\s*\}", text):
        vals = tuple(int(x.strip()) for x in match.group(1).split(","))
        rows.append(vals)
    return rows[:18]


def verify_cross_language_type_charts(repo_root: Path) -> Dict[str, Any]:
    """Verify 18x18 type chart consistency across C++, Java and Python (DATA-05 / BAT-02)."""
    root = Path(repo_root)
    files = {
        "cpp_battle_counter": root / "pgrazor/native/battle_counter.h",
        "java_local_audit": root / "pgrazor/src/dev/pgrazor/LocalAuditPolicy.java",
        "java_pvp_charged": root / "pgrazor/src/dev/pgrazor/PvpChargedPreference.java",
    }
    mismatches: List[str] = []
    checked: List[str] = []
    missing: List[str] = []
    for name, path in files.items():
        if not path.is_file():
            missing.append(name)
            continue
        rows = _parse_c_or_java_18x18_matrix(path.read_text(encoding="utf-8"))
        if len(rows) != 18:
            mismatches.append(f"{name}: esperado 18 linhas, encontrado {len(rows)}")
            continue
        checked.append(name)
        for r in range(18):
            for c in range(18):
                if rows[r][c] != CHART[r][c]:
                    mismatches.append(
                        f"{name}[{TYPES_PT[r]}->{TYPES_PT[c]}]: {rows[r][c]} != {CHART[r][c]}"
                    )
    return {
        "ok": len(mismatches) == 0 and len(missing) == 0 and len(checked) == len(files),
        "checked_sources": checked,
        "missing_sources": missing,
        "mismatches": mismatches,
    }
