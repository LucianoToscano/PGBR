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

from .type_chart import CHART, TYPES_EN, TYPES_PT

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
    # Also cross-check pgbr_offline_suite.pvp_simulator type chart if present
    extra_checked: List[str] = []
    disagreements: List[Dict[str, Any]] = []
    for msg in mismatches:
        disagreements.append({"detail": msg})
    try:
        from tools.pgbr_offline_suite import pvp_simulator as _pvp_sim

        extra_checked = ["python_pvp_simulator"]
        for r in range(18):
            atk_en = TYPES_EN[r]
            for c in range(18):
                dfn_en = TYPES_EN[c]
                sim_mult = _pvp_sim.type_multiplier(atk_en, (dfn_en,))
                expected_mult = 1.6 ** CHART[r][c]
                if abs(sim_mult - expected_mult) > 1e-9:
                    msg = (
                        f"python_pvp_simulator[{TYPES_PT[r]}->{TYPES_PT[c]}]: "
                        f"{sim_mult} != {expected_mult}"
                    )
                    mismatches.append(msg)
                    disagreements.append(
                        {
                            "source": "python_pvp_simulator",
                            "attacker": TYPES_PT[r],
                            "defender": TYPES_PT[c],
                            "observed": sim_mult,
                            "expected": expected_mult,
                            "detail": msg,
                        }
                    )
    except Exception:
        missing.append("python_pvp_simulator")

    return {
        "ok": len(mismatches) == 0 and len(missing) == 0 and len(checked) == len(files) and len(extra_checked) == 1,
        "has_disagreement": len(mismatches) > 0,
        "checked_sources": checked,
        "all_checked_sources": checked + extra_checked,
        "missing_sources": missing,
        "mismatches": mismatches,
        "disagreements": disagreements,
    }


def compare_type_chart_sources(
    source_a_name: str,
    matrix_a: List[Tuple[int, ...]],
    source_b_name: str,
    matrix_b: List[Tuple[int, ...]],
) -> Dict[str, Any]:
    """Explicitly compare two 18x18 type matrices and surface every disagreement cell-by-cell."""
    extra_checked: List[str] = []
    disagreements: List[Dict[str, Any]] = []
    if len(matrix_a) != 18 or len(matrix_b) != 18:
        raise ValueError("Ambas as matrizes devem ter exatamente 18 linhas.")
    for r in range(18):
        if len(matrix_a[r]) != 18 or len(matrix_b[r]) != 18:
            raise ValueError(f"Linha {r} deve ter exatamente 18 colunas.")
        for c in range(18):
            if matrix_a[r][c] != matrix_b[r][c]:
                disagreements.append(
                    {
                        "attacker": TYPES_PT[r],
                        "defender": TYPES_PT[c],
                        source_a_name: matrix_a[r][c],
                        source_b_name: matrix_b[r][c],
                    }
                )
    return {
        "ok": len(disagreements) == 0,
        "has_disagreement": len(disagreements) > 0,
        "sources": [source_a_name, source_b_name],
        "disagreement_count": len(disagreements),
        "disagreements": disagreements,
    }
