"""Offline Auxiliary Planners: Buddy, Eggs, Gifts, Medals & Seasonal Catalog (AUX-01 / AUX-02 / AUTO-03 / AUTO-04).

Strictly manual progress and offline planning calculations.
Never infers account progress, never triggers game actions, and never treats
historical seasonal fixtures as live event proof.
"""
from __future__ import annotations

from dataclasses import dataclass
from datetime import datetime, timezone
from typing import Any, Dict, List, Optional

BUDDY_EXCITED_THRESHOLD = 32
BUDDY_ACTIVITIES: Dict[str, Dict[str, int]] = {
    "play": {"ep": 2, "cooldown_min": 30},
    "feed": {"ep": 2, "cooldown_min": 30},
    "snapshot": {"ep": 2, "cooldown_min": 30},
    "battle": {"ep": 2, "cooldown_min": 30},
    "walk_2km": {"ep": 4, "cooldown_min": 0},
    "new_place": {"ep": 2, "cooldown_min": 30},
    "route": {"ep": 4, "cooldown_min": 30},
}

MEDAL_TIERS = (
    ("none", 0, 1.0),
    ("bronze", 10, 1.1),
    ("silver", 50, 1.2),
    ("gold", 200, 1.3),
    ("platinum", 2500, 1.4),
)


def plan_buddy_excitement(current_ep: int, planned_actions: List[str]) -> Dict[str, Any]:
    """Calculate Emotion Points (EP) toward Excited state (32 EP) manually (AUX-01)."""
    if current_ep < 0 or current_ep > 32:
        raise ValueError("EP atual deve estar entre 0 e 32.")
    gained = 0
    for act in planned_actions:
        if act not in BUDDY_ACTIVITIES:
            raise ValueError(f"Atividade de companheiro desconhecida: {act}")
        gained += BUDDY_ACTIVITIES[act]["ep"]
    total = min(BUDDY_EXCITED_THRESHOLD, current_ep + gained)
    return {
        "current_ep": current_ep,
        "gained_ep": gained,
        "projected_ep": total,
        "excited_reached": total >= BUDDY_EXCITED_THRESHOLD,
        "remaining_ep": max(0, BUDDY_EXCITED_THRESHOLD - total),
    }


def plan_egg_incubation(
    egg_km: float,
    *,
    super_incubator: bool = False,
    event_distance_multiplier: float = 1.0,
) -> Dict[str, Any]:
    """Compute effective hatch distance for 2/5/7/10/12 km eggs (AUX-01)."""
    if egg_km not in {2.0, 5.0, 7.0, 10.0, 12.0}:
        raise ValueError(f"Distância base de ovo inválida: {egg_km}")
    if not (0.1 <= event_distance_multiplier <= 1.0):
        raise ValueError("Multiplicador de evento deve estar em 0.1..1.0.")
    inc_mult = (2.0 / 3.0) if super_incubator else 1.0
    effective = round(egg_km * inc_mult * event_distance_multiplier, 2)
    return {
        "base_km": egg_km,
        "super_incubator": super_incubator,
        "event_multiplier": event_distance_multiplier,
        "effective_km": effective,
    }


def medal_catch_multiplier(catches_type1: int, catches_type2: Optional[int] = None) -> Dict[str, Any]:
    """Compute type catch medal tier and combined multiplier (AUX-01)."""
    def _tier(n: int) -> tuple[str, float]:
        if n < 0:
            raise ValueError("Contagem de medalha não pode ser negativa.")
        tier_name, mult = "none", 1.0
        for name, threshold, m in MEDAL_TIERS:
            if n >= threshold:
                tier_name, mult = name, m
        return tier_name, mult

    t1_name, t1_mult = _tier(catches_type1)
    if catches_type2 is None:
        return {"tier_1": t1_name, "tier_2": None, "multiplier": round(t1_mult, 3)}
    t2_name, t2_mult = _tier(catches_type2)
    return {"tier_1": t1_name, "tier_2": t2_name, "multiplier": round((t1_mult + t2_mult) / 2.0, 3)}


@dataclass(frozen=True)
class SeasonalProfile:
    profile_id: str  # e.g., "ETERNATUS_SEASONAL", "HALLOWEEN_2026"
    title_pt: str
    valid_from_utc: str
    valid_until_utc: str
    is_historical_fixture: bool = True
    confirmed_redemptions: int = 0


def evaluate_seasonal_profile(profile: SeasonalProfile, now_utc: datetime) -> Dict[str, Any]:
    """Validate seasonal profile window without inferring unconfirmed progress (AUX-02 / AUTO-03 / AUTO-04)."""
    if now_utc.tzinfo is None:
        raise ValueError("now_utc exige tzinfo explícito.")
    start = datetime.fromisoformat(profile.valid_from_utc.replace("Z", "+00:00")).astimezone(timezone.utc)
    end = datetime.fromisoformat(profile.valid_until_utc.replace("Z", "+00:00")).astimezone(timezone.utc)
    if end <= start:
        raise ValueError("Janela sazonal inválida.")
    now = now_utc.astimezone(timezone.utc)
    in_window = start <= now < end
    return {
        "profile_id": profile.profile_id,
        "title_pt": profile.title_pt,
        "in_window": in_window,
        "proves_live_event": False if profile.is_historical_fixture else in_window,
        "confirmed_redemptions": profile.confirmed_redemptions,
        "inferred_progress": False,
    }
