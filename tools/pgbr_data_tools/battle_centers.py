"""Offline Battle Planning Centers: Raids/Gyms (BAT-04), Max (BAT-05) & Rocket (BAT-06)."""
from __future__ import annotations

from dataclasses import dataclass
from datetime import datetime, timezone
from typing import Any, Dict, List, Optional, Sequence, Tuple

from .type_chart import TYPES_PT, defender_profile, effectiveness_multiplier, resolve_type_id

# Weather -> boosted type names (EN)
WEATHER_BOOSTS: Dict[str, Tuple[str, ...]] = {
    "sunny": ("Grass", "Ground", "Fire"),
    "clear": ("Grass", "Ground", "Fire"),
    "rainy": ("Water", "Electric", "Bug"),
    "partly_cloudy": ("Normal", "Rock"),
    "cloudy": ("Fairy", "Fighting", "Poison"),
    "windy": ("Dragon", "Flying", "Psychic"),
    "snow": ("Ice", "Steel"),
    "fog": ("Dark", "Ghost"),
}

# Max Battle particle entry costs by tier (manual planning reference).
MAX_BATTLE_PARTICLE_COST: Dict[int, int] = {
    1: 250,
    2: 400,
    3: 400,
    4: 400,
    5: 800,
    6: 800,  # Gigantamax
}

# Max Move upgrade particle cost by target level (1=unlock, 2=Lv2, 3=Lv3).
MAX_MOVE_PARTICLE_COST: Dict[int, int] = {1: 400, 2: 600, 3: 800}


@dataclass(frozen=True)
class AttackerCandidate:
    name: str
    types: Tuple[str, ...]
    move_type: str
    base_dps: float
    shadow: bool = False
    mega: bool = False
    dynamax_eligible: bool = False
    gigantamax_eligible: bool = False
    max_role: str = "attacker"  # "attacker" | "tank" | "healer"


@dataclass(frozen=True)
class RaidBoss:
    name: str
    tier: int
    types: Tuple[str, ...]
    boss_move_type: Optional[str] = None
    source: str = "manual"
    expires_utc: Optional[str] = None


def is_expired(expires_utc: Optional[str], now_utc: Optional[datetime] = None) -> Optional[bool]:
    """Return True if expired, False if active, None if unknown."""
    if not expires_utc:
        return None
    ref = now_utc or datetime.now(timezone.utc)
    dt = datetime.fromisoformat(expires_utc.replace("Z", "+00:00"))
    return ref >= dt


def recommend_raid_counters(
    boss: RaidBoss,
    candidates: Sequence[AttackerCandidate],
    *,
    weather: Optional[str] = None,
    top_n: int = 6,
    now_utc: Optional[datetime] = None,
) -> Dict[str, Any]:
    """Rank raid/gym counters using type effectiveness, STAB, Shadow and weather (BAT-04 / Pokematrix)."""
    d1 = boss.types[0]
    d2 = boss.types[1] if len(boss.types) > 1 else None
    profile = defender_profile(d1, d2)
    boosted = set(WEATHER_BOOSTS.get((weather or "").strip().lower(), ()))
    ranked: List[Dict[str, Any]] = []
    for c in candidates:
        mult = effectiveness_multiplier(c.move_type, d1, d2)
        if mult is None:
            continue
        stab = 1.2 if any(
            resolve_type_id(t) == resolve_type_id(c.move_type) for t in c.types
        ) else 1.0
        shadow_bonus = 1.2 if c.shadow else 1.0
        weather_bonus = 1.2 if any(
            resolve_type_id(b) == resolve_type_id(c.move_type) for b in boosted
        ) else 1.0
        incoming = 1.0
        if boss.boss_move_type:
            c2 = c.types[1] if len(c.types) > 1 else None
            inc = effectiveness_multiplier(boss.boss_move_type, c.types[0], c2)
            if inc is not None:
                incoming = inc
        score = round(c.base_dps * mult * stab * shadow_bonus * weather_bonus / (incoming ** 0.25), 2)
        ranked.append({
            "name": c.name,
            "move_type": c.move_type,
            "effectiveness": mult,
            "incoming_multiplier": incoming,
            "shadow": c.shadow,
            "mega": c.mega,
            "score": score,
        })
    ranked.sort(key=lambda item: (-item["score"], item["name"]))
    return {
        "boss": boss.name,
        "tier": boss.tier,
        "source": boss.source,
        "expired": is_expired(boss.expires_utc, now_utc),
        "weaknesses_pt": profile["fraquezas"] if profile else [],
        "counters": ranked[: max(1, top_n)],
    }


def plan_max_battle(
    boss_name: str,
    tier: int,
    boss_types: Tuple[str, ...],
    roster: Sequence[AttackerCandidate],
    *,
    available_particles: Optional[int] = None,
    planned_move_upgrades: Sequence[int] = (),
) -> Dict[str, Any]:
    """Plan a manual Max Battle team (Tank + Attacker + Healer) and particle budget (BAT-05).

    Strictly filters to Dynamax/Gigantamax eligible Pokemon and never spends particles.
    """
    if tier not in MAX_BATTLE_PARTICLE_COST:
        raise ValueError(f"Unsupported Max Battle tier: {tier}")
    eligible = [c for c in roster if c.dynamax_eligible or c.gigantamax_eligible]
    d1 = boss_types[0]
    d2 = boss_types[1] if len(boss_types) > 1 else None
    tanks = [c.name for c in eligible if c.max_role == "tank"]
    healers = [c.name for c in eligible if c.max_role == "healer"]
    attackers: List[Tuple[float, str]] = []
    for c in eligible:
        if c.max_role != "attacker":
            continue
        mult = effectiveness_multiplier(c.move_type, d1, d2) or 1.0
        gmax_bonus = 1.28 if c.gigantamax_eligible else 1.0
        attackers.append((round(c.base_dps * mult * gmax_bonus, 2), c.name))
    attackers.sort(key=lambda x: (-x[0], x[1]))

    entry_cost = MAX_BATTLE_PARTICLE_COST[tier]
    upgrade_cost = 0
    for lv in planned_move_upgrades:
        if lv not in MAX_MOVE_PARTICLE_COST:
            raise ValueError(f"Unsupported Max Move level: {lv}")
        upgrade_cost += MAX_MOVE_PARTICLE_COST[lv]
    total_cost = entry_cost + upgrade_cost
    shortfall = None if available_particles is None else max(0, total_cost - available_particles)

    return {
        "boss": boss_name,
        "tier": tier,
        "entry_particle_cost": entry_cost,
        "upgrade_particle_cost": upgrade_cost,
        "total_particle_cost": total_cost,
        "available_particles": available_particles,
        "particle_shortfall": shortfall,
        "eligible_count": len(eligible),
        "recommended_team": {
            "tanks": tanks[:2],
            "attackers": [name for _, name in attackers[:2]],
            "healers": healers[:1],
        },
        "device_tested": False,
    }


@dataclass(frozen=True)
class RocketEncounterEntry:
    role: str  # "grunt" | "leader" | "giovanni"
    quote_pt: str
    quote_en: str
    specialty_type: Optional[str]
    possible_slots: Tuple[Tuple[str, ...], ...]
    source: str
    revision_date: str


# Baseline offline Rocket quote catalog (manual lookup with explicit provenance/uncertainty).
ROCKET_CATALOG: Tuple[RocketEncounterEntry, ...] = (
    RocketEncounterEntry(
        role="grunt",
        quote_pt="Você sabe o quanto o hálito de fogo de um Pokémon pode ser quente?",
        quote_en="Do you know how hot Pokemon fire breath can get?",
        specialty_type="Fire",
        possible_slots=(("Charmander", "Cyndaquil", "Torchic"), ("Charmeleon", "Houndoom"), ("Charizard", "Typhlosion")),
        source="PGBR offline baseline (sujeito a rotação sazonal)",
        revision_date="2026-11-25",
    ),
    RocketEncounterEntry(
        role="grunt",
        quote_pt="Essas águas são traiçoeiras!",
        quote_en="These waters are treacherous!",
        specialty_type="Water",
        possible_slots=(("Squirtle", "Totodile", "Mudkip"), ("Wartortle", "Marshtomp"), ("Blastoise", "Swampert")),
        source="PGBR offline baseline (sujeito a rotação sazonal)",
        revision_date="2026-11-25",
    ),
    RocketEncounterEntry(
        role="grunt",
        quote_pt="Não nos subestime!",
        quote_en="Don't tangle with us!",
        specialty_type="Grass",
        possible_slots=(("Bulbasaur", "Treecko", "Turtwig"), ("Ivysaur", "Grovyle"), ("Venusaur", "Torterra")),
        source="PGBR offline baseline (sujeito a rotação sazonal)",
        revision_date="2026-11-25",
    ),
    RocketEncounterEntry(
        role="grunt",
        quote_pt="Prepare-se para ficar chocado!",
        quote_en="Get ready to be shocked!",
        specialty_type="Electric",
        possible_slots=(("Magnemite", "Voltorb", "Mareep"), ("Electabuzz", "Magneton"), ("Ampharos", "Electivire")),
        source="PGBR offline baseline (sujeito a rotação sazonal)",
        revision_date="2026-11-25",
    ),
    RocketEncounterEntry(
        role="grunt",
        quote_pt="RUGIDO! ... Como foi isso?",
        quote_en="ROAR! ...How'd that sound?",
        specialty_type="Dragon",
        possible_slots=(("Dratini", "Bagon", "Gible"), ("Dragonair", "Gabite"), ("Dragonite", "Salamence")),
        source="PGBR offline baseline (sujeito a rotação sazonal)",
        revision_date="2026-11-25",
    ),
)


def lookup_rocket_quote(
    query: str, catalog: Sequence[RocketEncounterEntry] = ROCKET_CATALOG
) -> List[Dict[str, Any]]:
    """Search Rocket Grunt/Leader quotes (PT-BR or EN) and return weaknesses + provenance (BAT-06)."""
    q = query.strip().lower()
    if not q:
        return []
    matches: List[Dict[str, Any]] = []
    for entry in catalog:
        if (
            q in entry.quote_pt.lower()
            or q in entry.quote_en.lower()
            or (entry.specialty_type and q == entry.specialty_type.lower())
        ):
            prof = defender_profile(entry.specialty_type) if entry.specialty_type else None
            matches.append({
                "role": entry.role,
                "quote_pt": entry.quote_pt,
                "quote_en": entry.quote_en,
                "specialty_type": entry.specialty_type,
                "possible_slots": [list(s) for s in entry.possible_slots],
                "weaknesses_pt": prof["fraquezas"] if prof else [],
                "source": entry.source,
                "revision_date": entry.revision_date,
                "uncertainty_note": "Escalação real depende da rotação Rocket ativa no jogo.",
            })
    return matches
