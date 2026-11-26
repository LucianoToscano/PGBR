"""Offline Pokemon GO CP, Power-Up Cost, Evolution Preview & PvP Rank calculator (COL-03 / COL-05).

Pure deterministic calculation without touching game state or spending resources.
"""
from __future__ import annotations

from dataclasses import dataclass
import math
from typing import Dict, List, Optional, Tuple

# Official Pokemon GO CP Multiplier table for levels 1.0..51.0 (step 0.5).
CPM_TABLE: Dict[float, float] = {
    1.0: 0.094, 1.5: 0.1351374318, 2.0: 0.16639787, 2.5: 0.192650919,
    3.0: 0.21573247, 3.5: 0.2365726613, 4.0: 0.25572005, 4.5: 0.2735303812,
    5.0: 0.29024988, 5.5: 0.3060573775, 6.0: 0.3210876, 6.5: 0.3354450362,
    7.0: 0.34921268, 7.5: 0.3624577511, 8.0: 0.3752356, 8.5: 0.387592416,
    9.0: 0.39956728, 9.5: 0.4111935514, 10.0: 0.4225, 10.5: 0.4329264091,
    11.0: 0.44310755, 11.5: 0.4530599591, 12.0: 0.4627984, 12.5: 0.472336093,
    13.0: 0.48168495, 13.5: 0.4908558003, 14.0: 0.49985844, 14.5: 0.508701765,
    15.0: 0.51739395, 15.5: 0.5259425113, 16.0: 0.5343543, 16.5: 0.5426357375,
    17.0: 0.5507927, 17.5: 0.5588305862, 18.0: 0.5667545, 18.5: 0.5745691333,
    19.0: 0.5822789, 19.5: 0.5898879072, 20.0: 0.5974, 20.5: 0.6048236651,
    21.0: 0.6121573, 21.5: 0.6194041216, 22.0: 0.6265671, 22.5: 0.6336491432,
    23.0: 0.64065295, 23.5: 0.6475809666, 24.0: 0.65443563, 24.5: 0.6612192524,
    25.0: 0.667934, 25.5: 0.6745818959, 26.0: 0.6811649, 26.5: 0.6876849038,
    27.0: 0.69414365, 27.5: 0.70054287, 28.0: 0.7068842, 28.5: 0.7131691091,
    29.0: 0.7193991, 29.5: 0.7255756136, 30.0: 0.7317, 30.5: 0.7347410093,
    31.0: 0.7377695, 31.5: 0.7407855938, 32.0: 0.74378943, 32.5: 0.7467812109,
    33.0: 0.74976104, 33.5: 0.7527290867, 34.0: 0.7556855, 34.5: 0.7586303683,
    35.0: 0.76156384, 35.5: 0.7644860647, 36.0: 0.76739717, 36.5: 0.7702972656,
    37.0: 0.7731865, 37.5: 0.7760649616, 38.0: 0.77893275, 38.5: 0.7817900548,
    39.0: 0.784637, 39.5: 0.7874736075, 40.0: 0.7903, 40.5: 0.792803968,
    41.0: 0.79530001, 41.5: 0.797800015, 42.0: 0.8003, 42.5: 0.802799995,
    43.0: 0.8053, 43.5: 0.8078, 44.0: 0.81029999, 44.5: 0.812799985,
    45.0: 0.81529999, 45.5: 0.81779999, 46.0: 0.82029999, 46.5: 0.82279999,
    47.0: 0.82529999, 47.5: 0.82779999, 48.0: 0.83029999, 48.5: 0.83279999,
    49.0: 0.83529999, 49.5: 0.83779999, 50.0: 0.84029999, 50.5: 0.84279999,
    51.0: 0.84529999,
}

# Base power-up cost per half-level step: (stardust, candy, xl_candy)
def _step_cost(level: float) -> Tuple[int, int, int]:
    """Return base (stardust, candy, xl_candy) to advance from `level` to `level + 0.5`."""
    if level < 1.0 or level >= 50.0:
        raise ValueError(f"Unsupported power-up start level: {level}")
    if level < 3.0:
        return (200, 1, 0)
    if level < 5.0:
        return (400, 1, 0)
    if level < 7.0:
        return (600, 1, 0)
    if level < 9.0:
        return (800, 1, 0)
    if level < 11.0:
        return (1000, 1, 0)
    if level < 13.0:
        return (1300, 2, 0)
    if level < 15.0:
        return (1600, 2, 0)
    if level < 17.0:
        return (1900, 2, 0)
    if level < 19.0:
        return (2200, 2, 0)
    if level < 21.0:
        return (2500, 2, 0)
    if level < 23.0:
        return (3000, 3, 0)
    if level < 25.0:
        return (3500, 3, 0)
    if level < 27.0:
        return (4000, 4, 0)
    if level < 29.0:
        return (4500, 4, 0)
    if level < 31.0:
        return (5000, 4, 0)
    if level < 33.0:
        return (6000, 6, 0)
    if level < 35.0:
        return (7000, 8, 0)
    if level < 37.0:
        return (8000, 10, 0)
    if level < 39.0:
        return (9000, 12, 0)
    if level < 40.0:
        return (10000, 15, 0)
    if level < 42.0:
        return (10000, 0, 10)
    if level < 44.0:
        return (11000, 0, 12)
    if level < 46.0:
        return (12000, 0, 15)
    if level < 48.0:
        return (13000, 0, 17)
    return (14000, 0, 20)


@dataclass(frozen=True)
class BaseStats:
    species_id: int
    name: str
    attack: int
    defense: int
    stamina: int


@dataclass(frozen=True)
class PowerUpCost:
    from_level: float
    to_level: float
    stardust: int
    candy: int
    xl_candy: int
    steps: int


@dataclass(frozen=True)
class PvpRankResult:
    league: str
    cp_cap: int
    max_level: float
    level: float
    cp: int
    stat_product: float
    rank: int
    total_combinations: int
    percent_of_max: float


LEAGUE_CAPS: Dict[str, int] = {
    "little": 500,
    "great": 1500,
    "ultra": 2500,
    "master": 999999,
}


def compute_cp(base: BaseStats, atk_iv: int, def_iv: int, sta_iv: int, level: float) -> int:
    """Compute Pokemon GO CP for given base stats, IVs (0..15), and half-level (1.0..51.0)."""
    if not (0 <= atk_iv <= 15 and 0 <= def_iv <= 15 and 0 <= sta_iv <= 15):
        raise ValueError("IVs must be in 0..15")
    if level not in CPM_TABLE:
        raise ValueError(f"Unsupported level: {level}")
    cpm = CPM_TABLE[level]
    atk = base.attack + atk_iv
    dfn = base.defense + def_iv
    sta = base.stamina + sta_iv
    raw = math.floor(atk * math.sqrt(dfn) * math.sqrt(sta) * (cpm ** 2) / 10.0)
    return max(10, raw)


def compute_stat_product(
    base: BaseStats, atk_iv: int, def_iv: int, sta_iv: int, level: float
) -> float:
    """Compute PvP stat product: effective_atk * effective_def * floor(effective_hp)."""
    cpm = CPM_TABLE[level]
    atk = (base.attack + atk_iv) * cpm
    dfn = (base.defense + def_iv) * cpm
    hp = max(10, math.floor((base.stamina + sta_iv) * cpm))
    return atk * dfn * hp


def power_up_cost(
    from_level: float,
    to_level: float,
    *,
    shadow: bool = False,
    purified: bool = False,
    lucky: bool = False,
) -> PowerUpCost:
    """Calculate total Stardust, Candy, and XL Candy without spending resources."""
    if shadow and purified:
        raise ValueError("Pokemon cannot be both Shadow and Purified")
    if from_level not in CPM_TABLE or to_level not in CPM_TABLE:
        raise ValueError("Levels must be valid half-steps")
    if to_level < from_level or to_level > 50.0:
        raise ValueError("Invalid level range (max power-up is 50.0; 51.0 is Best Buddy)")
    dust_total = 0
    candy_total = 0
    xl_total = 0
    steps = 0
    cur = from_level
    while cur < to_level:
        d, c, xl = _step_cost(cur)
        if shadow:
            d = math.ceil(d * 1.2)
            c = math.ceil(c * 1.2)
            xl = math.ceil(xl * 1.2)
        elif purified:
            d = math.ceil(d * 0.9)
            c = math.ceil(c * 0.9)
            xl = math.ceil(xl * 0.9)
        if lucky:
            d = d // 2
        dust_total += d
        candy_total += c
        xl_total += xl
        steps += 1
        cur = round(cur + 0.5, 1)
    return PowerUpCost(from_level, to_level, dust_total, candy_total, xl_total, steps)


def best_level_under_cap(
    base: BaseStats, atk_iv: int, def_iv: int, sta_iv: int, cp_cap: int, max_level: float = 50.0
) -> Optional[Tuple[float, int, float]]:
    """Find highest half-level <= max_level whose CP <= cp_cap."""
    best: Optional[Tuple[float, int, float]] = None
    lvl = 1.0
    while lvl <= max_level:
        cp = compute_cp(base, atk_iv, def_iv, sta_iv, lvl)
        if cp <= cp_cap:
            best = (lvl, cp, compute_stat_product(base, atk_iv, def_iv, sta_iv, lvl))
        else:
            break
        lvl = round(lvl + 0.5, 1)
    return best


def evaluate_pvp_rank(
    base: BaseStats,
    atk_iv: int,
    def_iv: int,
    sta_iv: int,
    league: str = "great",
    *,
    max_level: float = 50.0,
    iv_floor: int = 0,
) -> Optional[PvpRankResult]:
    """Compute exact PvP stat-product rank (1..4096) for a species and league (COL-05)."""
    key = league.strip().lower()
    if key not in LEAGUE_CAPS:
        raise ValueError(f"Unknown league: {league}")
    if not (0 <= iv_floor <= 15):
        raise ValueError("iv_floor must be in 0..15")
    if atk_iv < iv_floor or def_iv < iv_floor or sta_iv < iv_floor:
        raise ValueError("IVs below iv_floor")
    cap = LEAGUE_CAPS[key]
    target = best_level_under_cap(base, atk_iv, def_iv, sta_iv, cap, max_level)
    if target is None:
        return None
    t_lvl, t_cp, t_sp = target
    better = 0
    max_sp = t_sp
    total = 0
    for a in range(iv_floor, 16):
        for d in range(iv_floor, 16):
            for s in range(iv_floor, 16):
                res = best_level_under_cap(base, a, d, s, cap, max_level)
                if res is None:
                    continue
                total += 1
                sp = res[2]
                if sp > max_sp:
                    max_sp = sp
                if sp > t_sp + 1e-9:
                    better += 1
    pct = round((t_sp / max_sp) * 100.0, 2) if max_sp > 0 else 0.0
    return PvpRankResult(
        league=key,
        cp_cap=cap,
        max_level=max_level,
        level=t_lvl,
        cp=t_cp,
        stat_product=round(t_sp, 2),
        rank=better + 1,
        total_combinations=total,
        percent_of_max=pct,
    )
