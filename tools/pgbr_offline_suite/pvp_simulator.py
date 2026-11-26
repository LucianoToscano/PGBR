"""Offline 1v1 PvP Turn-Based Simulator & 3-Pokemon Team Coverage Analyzer (BAT-03).

Pure local mathematical simulation (0.5s turn model, energy cap 100, STAB 1.2x,
Shadow 1.2x ATK / 0.833333x DEF, 0v0/1v1/2v2 shields, CMP tiebreaker).
NEVER touches game runtime or battle execution; device_tested=False always.
"""
from __future__ import annotations

from dataclasses import dataclass
import math
from typing import Any, Dict, List, Optional, Tuple

TYPES_EN: Tuple[str, ...] = (
    "Normal", "Fighting", "Flying", "Poison", "Ground", "Rock",
    "Bug", "Ghost", "Steel", "Fire", "Water", "Grass",
    "Electric", "Psychic", "Ice", "Dragon", "Dark", "Fairy",
)
_IDX = {t.lower(): i for i, t in enumerate(TYPES_EN)}

# Canonical Pokémon GO Gen VI+ 18x18 step matrix (Ghost->Steel=0, Dark->Steel=0).
_CHART: Tuple[Tuple[int, ...], ...] = (
    ( 0,  0,  0,  0,  0, -1,  0, -2, -1,  0,  0,  0,  0,  0,  0,  0,  0,  0),
    ( 1,  0, -1, -1,  0,  1, -1, -2,  1,  0,  0,  0,  0, -1,  1,  0,  1, -1),
    ( 0,  1,  0,  0,  0, -1,  1,  0, -1,  0,  0,  1, -1,  0,  0,  0,  0,  0),
    ( 0,  0,  0, -1, -1, -1,  0, -1, -2,  0,  0,  1,  0,  0,  0,  0,  0,  1),
    ( 0,  0, -2,  1,  0,  1, -1,  0,  1,  1,  0, -1,  1,  0,  0,  0,  0,  0),
    ( 0, -1,  1,  0, -1,  0,  1,  0, -1,  1,  0,  0,  0,  0,  1,  0,  0,  0),
    ( 0, -1, -1, -1,  0,  0,  0, -1, -1, -1,  0,  1,  0,  1,  0,  0,  1, -1),
    (-2,  0,  0,  0,  0,  0,  0,  1,  0,  0,  0,  0,  0,  1,  0,  0, -1,  0),
    ( 0,  0,  0,  0,  0,  1,  0,  0, -1, -1, -1,  0, -1,  0,  1,  0,  0,  1),
    ( 0,  0,  0,  0,  0, -1,  1,  0,  1, -1, -1,  1,  0,  0,  1, -1,  0,  0),
    ( 0,  0,  0,  0,  1,  1,  0,  0,  0,  1, -1, -1,  0,  0,  0, -1,  0,  0),
    ( 0,  0, -1, -1,  1,  1, -1,  0, -1, -1,  1, -1,  0,  0,  0, -1,  0,  0),
    ( 0,  0,  1,  0, -2,  0,  0,  0,  0,  0,  1, -1, -1,  0,  0, -1,  0,  0),
    ( 0,  1,  0,  1,  0,  0,  0,  0, -1,  0,  0,  0,  0, -1,  0,  0, -2,  0),
    ( 0,  0,  1,  0,  1,  0,  0,  0, -1, -1, -1,  1,  0,  0, -1,  1,  0,  0),
    ( 0,  0,  0,  0,  0,  0,  0,  0, -1,  0,  0,  0,  0,  0,  0,  1,  0, -2),
    ( 0, -1,  0,  0,  0,  0,  0,  1,  0,  0,  0,  0,  0,  1,  0,  0, -1, -1),
    ( 0,  1,  0, -1,  0,  0,  0,  0, -1, -1,  0,  0,  0,  0,  0,  1,  1,  0),
)

PVP_BONUS_MULTIPLIER = 1.3
STAB_MULTIPLIER = 1.2
SHADOW_ATK_MULT = 1.2
SHADOW_DEF_MULT = 1.0 / 1.2


def type_multiplier(move_type: str, defender_types: Tuple[str, ...]) -> float:
    mi = _IDX.get(move_type.strip().lower())
    if mi is None or not defender_types or len(defender_types) > 2:
        raise ValueError("Tipo de golpe ou defensor inválido.")
    steps = 0
    seen = set()
    for dt in defender_types:
        di = _IDX.get(dt.strip().lower())
        if di is None or di in seen:
            raise ValueError(f"Tipo defensor inválido ou duplicado: {defender_types}")
        seen.add(di)
        steps += _CHART[mi][di]
    return 1.6 ** steps


@dataclass(frozen=True)
class FastMove:
    name: str
    move_type: str
    power: int
    energy_gain: int
    turns: int  # 1..5 (each turn = 0.5s)


@dataclass(frozen=True)
class ChargedMove:
    name: str
    move_type: str
    power: int
    energy_cost: int  # 35..100


@dataclass(frozen=True)
class PvpFighter:
    name: str
    types: Tuple[str, ...]
    attack: float  # effective stat = (base + iv) * cpm
    defense: float
    hp: int
    fast_move: FastMove
    charged_moves: Tuple[ChargedMove, ...]
    shadow: bool = False


def calc_pvp_damage(attacker: PvpFighter, defender: PvpFighter, move_type: str, power: int) -> int:
    eff_atk = attacker.attack * (SHADOW_ATK_MULT if attacker.shadow else 1.0)
    eff_def = defender.defense * (SHADOW_DEF_MULT if defender.shadow else 1.0)
    stab = STAB_MULTIPLIER if move_type.lower() in {t.lower() for t in attacker.types} else 1.0
    eff = type_multiplier(move_type, defender.types)
    return int(math.floor(0.5 * power * (eff_atk / eff_def) * stab * eff * PVP_BONUS_MULTIPLIER)) + 1


def _best_ready_charged(attacker: PvpFighter, defender: PvpFighter, energy: int) -> Optional[ChargedMove]:
    ready = [m for m in attacker.charged_moves if energy >= m.energy_cost]
    if not ready:
        return None
    return max(ready, key=lambda m: calc_pvp_damage(attacker, defender, m.move_type, m.power) / m.energy_cost)


def simulate_1v1(
    a: PvpFighter,
    b: PvpFighter,
    *,
    shields_a: int = 1,
    shields_b: int = 1,
    max_turns: int = 480,
) -> Dict[str, Any]:
    """Run a deterministic offline 1v1 PvP simulation."""
    if not (0 <= shields_a <= 2 and 0 <= shields_b <= 2):
        raise ValueError("Escudos devem estar em 0..2.")
    hp_a, hp_b = a.hp, b.hp
    en_a, en_b = 0, 0
    sh_a, sh_b = shields_a, shields_b
    cd_a, cd_b = 0, 0
    turn = 0

    while turn < max_turns and hp_a > 0 and hp_b > 0:
        turn += 1
        # Charged move priority (CMP tiebreaker by higher effective Attack)
        cm_a = _best_ready_charged(a, b, en_a) if cd_a == 0 else None
        cm_b = _best_ready_charged(b, a, en_b) if cd_b == 0 else None
        if cm_a or cm_b:
            order = []
            if cm_a and cm_b:
                atk_a = a.attack * (SHADOW_ATK_MULT if a.shadow else 1.0)
                atk_b = b.attack * (SHADOW_ATK_MULT if b.shadow else 1.0)
                order = [("a", cm_a), ("b", cm_b)] if atk_a >= atk_b else [("b", cm_b), ("a", cm_a)]
            elif cm_a:
                order = [("a", cm_a)]
            else:
                order = [("b", cm_b)]

            for who, move in order:
                if hp_a <= 0 or hp_b <= 0:
                    break
                if who == "a":
                    en_a -= move.energy_cost
                    if sh_b > 0:
                        sh_b -= 1
                        hp_b = max(0, hp_b - 1)
                    else:
                        hp_b = max(0, hp_b - calc_pvp_damage(a, b, move.move_type, move.power))
                else:
                    en_b -= move.energy_cost
                    if sh_a > 0:
                        sh_a -= 1
                        hp_a = max(0, hp_a - 1)
                    else:
                        hp_a = max(0, hp_a - calc_pvp_damage(b, a, move.move_type, move.power))
            continue

        # Fast move execution
        if cd_a == 0:
            hp_b = max(0, hp_b - calc_pvp_damage(a, b, a.fast_move.move_type, a.fast_move.power))
            en_a = min(100, en_a + a.fast_move.energy_gain)
            cd_a = a.fast_move.turns
        if cd_b == 0 and hp_b > 0:
            hp_a = max(0, hp_a - calc_pvp_damage(b, a, b.fast_move.move_type, b.fast_move.power))
            en_b = min(100, en_b + b.fast_move.energy_gain)
            cd_b = b.fast_move.turns
        cd_a = max(0, cd_a - 1)
        cd_b = max(0, cd_b - 1)

    winner = "tie" if hp_a == hp_b else (a.name if hp_a > hp_b else b.name)
    return {
        "winner": winner,
        "turns": turn,
        "hp_remaining": {a.name: hp_a, b.name: hp_b},
        "shields_remaining": {a.name: sh_a, b.name: sh_b},
        "energy_remaining": {a.name: en_a, b.name: en_b},
        "offline_simulation": True,
        "device_tested": False,
    }


def analyze_team_coverage(team: List[Tuple[str, Tuple[str, ...]]]) -> Dict[str, Any]:
    """Analyze shared weaknesses and coverage gaps for a 3-Pokemon PvP team."""
    if not (1 <= len(team) <= 3):
        raise ValueError("Equipe PvP deve ter de 1 a 3 Pokémon.")
    weak_counts: Dict[str, int] = {}
    resist_counts: Dict[str, int] = {}
    for atk_type in TYPES_EN:
        w = 0
        r = 0
        for _, def_types in team:
            mult = type_multiplier(atk_type, def_types)
            if mult > 1.0001:
                w += 1
            elif mult < 0.9999:
                r += 1
        if w > 0:
            weak_counts[atk_type] = w
        if r > 0:
            resist_counts[atk_type] = r
    shared_weaknesses = sorted([t for t, c in weak_counts.items() if c >= 2])
    uncovered_threats = sorted([t for t in shared_weaknesses if resist_counts.get(t, 0) == 0])
    return {
        "team_size": len(team),
        "weak_counts": weak_counts,
        "resist_counts": resist_counts,
        "shared_weaknesses": shared_weaknesses,
        "uncovered_threats": uncovered_threats,
    }
