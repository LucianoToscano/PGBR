"""Canonical 18x18 Pokemon GO type effectiveness engine (BAT-02 / DATA-05).

Synchronized 1:1 with pgrazor/native/battle_counter.h and
pgrazor/src/dev/pgrazor/LocalAuditPolicy.java (Gen VI+ / Pokemon GO:
Ghost -> Steel is Neutral 0 = 1.0x, Dark -> Steel is Neutral 0 = 1.0x).
Fail-closed: invalid or duplicate defender types return None.
"""
from __future__ import annotations

from typing import Dict, List, Optional, Tuple

TYPES_EN: Tuple[str, ...] = (
    "Normal", "Fighting", "Flying", "Poison", "Ground", "Rock",
    "Bug", "Ghost", "Steel", "Fire", "Water", "Grass",
    "Electric", "Psychic", "Ice", "Dragon", "Dark", "Fairy",
)

TYPES_PT: Tuple[str, ...] = (
    "Normal", "Lutador", "Voador", "Venenoso", "Terrestre", "Pedra",
    "Inseto", "Fantasma", "Aço", "Fogo", "Água", "Planta",
    "Elétrico", "Psíquico", "Gelo", "Dragão", "Sombrio", "Fada",
)

# 1-based index lookup (case-insensitive, EN & PT-BR)
_TYPE_INDEX: Dict[str, int] = {}
for _i, (_en, _pt) in enumerate(zip(TYPES_EN, TYPES_PT), start=1):
    _TYPE_INDEX[_en.lower()] = _i
    _TYPE_INDEX[_pt.lower()] = _i
_TYPE_INDEX["aco"] = 9
_TYPE_INDEX["agua"] = 11
_TYPE_INDEX["eletrico"] = 13
_TYPE_INDEX["psiquico"] = 14
_TYPE_INDEX["dragao"] = 16

TYPE_CHART_METADATA: Dict[str, object] = {
    "source": "pgrazor/native/battle_counter.h + LocalAuditPolicy.java + PvpChargedPreference.java",
    "ruleset_version": "pogo-gen6-1.6x-v1",
    "multiplier_base": 1.6,
    "matrix_shape": (18, 18),
    "precision_note": "Multiplicador exato por passo inteiro 1.6**steps; fail-closed para tipos inválidos ou duplicados.",
}

# Canonical 18x18 step matrix (-2 immunity, -1 resisted, 0 neutral, +1 super).
CHART: Tuple[Tuple[int, ...], ...] = (
    ( 0,  0,  0,  0,  0, -1,  0, -2, -1,  0,  0,  0,  0,  0,  0,  0,  0,  0),  # Normal
    ( 1,  0, -1, -1,  0,  1, -1, -2,  1,  0,  0,  0,  0, -1,  1,  0,  1, -1),  # Fighting
    ( 0,  1,  0,  0,  0, -1,  1,  0, -1,  0,  0,  1, -1,  0,  0,  0,  0,  0),  # Flying
    ( 0,  0,  0, -1, -1, -1,  0, -1, -2,  0,  0,  1,  0,  0,  0,  0,  0,  1),  # Poison
    ( 0,  0, -2,  1,  0,  1, -1,  0,  1,  1,  0, -1,  1,  0,  0,  0,  0,  0),  # Ground
    ( 0, -1,  1,  0, -1,  0,  1,  0, -1,  1,  0,  0,  0,  0,  1,  0,  0,  0),  # Rock
    ( 0, -1, -1, -1,  0,  0,  0, -1, -1, -1,  0,  1,  0,  1,  0,  0,  1, -1),  # Bug
    (-2,  0,  0,  0,  0,  0,  0,  1,  0,  0,  0,  0,  0,  1,  0,  0, -1,  0),  # Ghost (Steel=0)
    ( 0,  0,  0,  0,  0,  1,  0,  0, -1, -1, -1,  0, -1,  0,  1,  0,  0,  1),  # Steel
    ( 0,  0,  0,  0,  0, -1,  1,  0,  1, -1, -1,  1,  0,  0,  1, -1,  0,  0),  # Fire
    ( 0,  0,  0,  0,  1,  1,  0,  0,  0,  1, -1, -1,  0,  0,  0, -1,  0,  0),  # Water
    ( 0,  0, -1, -1,  1,  1, -1,  0, -1, -1,  1, -1,  0,  0,  0, -1,  0,  0),  # Grass
    ( 0,  0,  1,  0, -2,  0,  0,  0,  0,  0,  1, -1, -1,  0,  0, -1,  0,  0),  # Electric
    ( 0,  1,  0,  1,  0,  0,  0,  0, -1,  0,  0,  0,  0, -1,  0,  0, -2,  0),  # Psychic
    ( 0,  0,  1,  0,  1,  0,  0,  0, -1, -1, -1,  1,  0,  0, -1,  1,  0,  0),  # Ice
    ( 0,  0,  0,  0,  0,  0,  0,  0, -1,  0,  0,  0,  0,  0,  0,  1,  0, -2),  # Dragon
    ( 0, -1,  0,  0,  0,  0,  0,  1,  0,  0,  0,  0,  0,  1,  0,  0, -1, -1),  # Dark (Steel=0)
    ( 0,  1,  0, -1,  0,  0,  0,  0, -1, -1,  0,  0,  0,  0,  0,  1,  1,  0),  # Fairy
)

POGO_STEP_BASE = 1.6


def resolve_type_id(value: int | str | None) -> Optional[int]:
    """Return 1..18 type ID or None when invalid/unknown."""
    if value is None or isinstance(value, bool):
        return None
    if isinstance(value, int):
        return value if 1 <= value <= 18 else None
    if isinstance(value, str):
        return _TYPE_INDEX.get(value.strip().lower())
    return None


def effectiveness_steps(
    move_type: int | str,
    defender_type1: int | str,
    defender_type2: int | str | None = None,
) -> Optional[int]:
    """Return integer effectiveness steps or None (fail-closed) on invalid/duplicate types."""
    m = resolve_type_id(move_type)
    d1 = resolve_type_id(defender_type1)
    if m is None or d1 is None:
        return None
    if defender_type2 is None or defender_type2 == 0 or defender_type2 == "":
        return CHART[m - 1][d1 - 1]
    d2 = resolve_type_id(defender_type2)
    if d2 is None or d2 == d1:
        return None
    return CHART[m - 1][d1 - 1] + CHART[m - 1][d2 - 1]


def effectiveness_multiplier(
    move_type: int | str,
    defender_type1: int | str,
    defender_type2: int | str | None = None,
) -> Optional[float]:
    """Return Pokemon GO multiplier (1.6 ** steps) or None when unknown/invalid."""
    steps = effectiveness_steps(move_type, defender_type1, defender_type2)
    if steps is None:
        return None
    return round(POGO_STEP_BASE ** steps, 6)


def defender_profile(
    defender_type1: int | str,
    defender_type2: int | str | None = None,
) -> Optional[Dict[str, List[Tuple[str, float]]]]:
    """Return sorted weaknesses, resistances and neutral matchups in PT-BR."""
    d1 = resolve_type_id(defender_type1)
    if d1 is None:
        return None
    if defender_type2 is not None and defender_type2 != 0 and defender_type2 != "":
        d2 = resolve_type_id(defender_type2)
        if d2 is None or d2 == d1:
            return None
    weaknesses: List[Tuple[str, float]] = []
    resistances: List[Tuple[str, float]] = []
    neutral: List[Tuple[str, float]] = []
    for tid in range(1, 19):
        steps = effectiveness_steps(tid, defender_type1, defender_type2)
        assert steps is not None
        mult = round(POGO_STEP_BASE ** steps, 4)
        label = TYPES_PT[tid - 1]
        if steps > 0:
            weaknesses.append((label, mult))
        elif steps < 0:
            resistances.append((label, mult))
        else:
            neutral.append((label, mult))
    weaknesses.sort(key=lambda item: (-item[1], item[0]))
    resistances.sort(key=lambda item: (item[1], item[0]))
    return {"fraquezas": weaknesses, "resistencias": resistances, "neutro": neutral}
