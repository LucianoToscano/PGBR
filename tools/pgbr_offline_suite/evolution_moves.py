"""Offline Evolution Comparison & Legacy/Elite TM Move Eligibility Preview (COL-03).

Previews evolution branches, CP outcomes, resource costs, and legacy/Elite TM
move windows strictly offline — NEVER spends candy, stardust, TMs, or items.
"""
from __future__ import annotations

from dataclasses import dataclass
import math
from typing import Any, Dict, List, Optional, Tuple


@dataclass(frozen=True)
class MoveEligibility:
    move_name: str
    move_type: str
    category: str  # "fast" | "charged"
    legacy: bool
    elite_tm_eligible: bool
    event_window: Optional[str] = None
    provenance: str = "gamemaster-offline"


@dataclass(frozen=True)
class EvolutionBranch:
    target_dex: int
    target_name: str
    base_attack: int
    base_defense: int
    base_stamina: int
    candy_cost: int
    item_required: Optional[str] = None
    buddy_km_required: float = 0.0
    lure_or_time_requirement: Optional[str] = None
    moves: Tuple[MoveEligibility, ...] = ()


def compute_cp_from_cpm(
    base_atk: int,
    base_def: int,
    base_sta: int,
    iv_atk: int,
    iv_def: int,
    iv_sta: int,
    cpm: float,
) -> int:
    for iv in (iv_atk, iv_def, iv_sta):
        if not (0 <= iv <= 15):
            raise ValueError(f"IV fora do intervalo 0..15: {iv}")
    if not math.isfinite(cpm) or cpm <= 0.0:
        raise ValueError("CPM inválido.")
    raw = (base_atk + iv_atk) * math.sqrt(base_def + iv_def) * math.sqrt(base_sta + iv_sta) * (cpm ** 2) / 10.0
    return max(10, int(math.floor(raw)))


def preview_evolution_branches(
    branches: List[EvolutionBranch],
    *,
    iv_atk: int,
    iv_def: int,
    iv_sta: int,
    current_cpm: float,
    candy_stock: Optional[int] = None,
    owned_items: Optional[Dict[str, int]] = None,
    league_cap: int = 1500,
) -> List[Dict[str, Any]]:
    """Compare all evolution branches without spending any resource."""
    items = owned_items or {}
    previews: List[Dict[str, Any]] = []
    for br in branches:
        cp_after = compute_cp_from_cpm(
            br.base_attack, br.base_defense, br.base_stamina,
            iv_atk, iv_def, iv_sta, current_cpm,
        )
        candy_enough: Optional[bool] = None if candy_stock is None else (candy_stock >= br.candy_cost)
        item_enough: Optional[bool] = True
        if br.item_required:
            if owned_items is None or br.item_required not in items:
                item_enough = None  # unknown stock != missing
            else:
                item_enough = items[br.item_required] >= 1
        legacy_moves = [
            {
                "move_name": m.move_name,
                "move_type": m.move_type,
                "category": m.category,
                "elite_tm_eligible": m.elite_tm_eligible,
                "event_window": m.event_window,
                "provenance": m.provenance,
            }
            for m in br.moves if m.legacy
        ]
        previews.append({
            "target_dex": br.target_dex,
            "target_name": br.target_name,
            "predicted_cp": cp_after,
            "fits_league_cap": cp_after <= league_cap,
            "league_cap": league_cap,
            "candy_cost": br.candy_cost,
            "candy_enough": candy_enough,
            "item_required": br.item_required,
            "item_enough": item_enough,
            "buddy_km_required": br.buddy_km_required,
            "lure_or_time_requirement": br.lure_or_time_requirement,
            "legacy_moves": legacy_moves,
            "resources_spent": False,
        })
    previews.sort(key=lambda r: (not r["fits_league_cap"], -r["predicted_cp"], r["target_name"]))
    return previews
