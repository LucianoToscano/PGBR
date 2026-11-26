"""Offline Pokédex & Visual Collection Checklist Engine (COL-04).

Supports filtering by generation (1..9), type, form, costume, background,
shiny availability, rarity, manual collection checklist progress, and
sanitized JSON/CSV local export. Never fabricates account state.
"""
from __future__ import annotations

import csv
from dataclasses import dataclass, asdict
import io
import json
from typing import Any, Dict, Iterable, List, Optional, Tuple

GENERATION_RANGES: Dict[int, Tuple[int, int]] = {
    1: (1, 151),
    2: (152, 251),
    3: (252, 386),
    4: (387, 493),
    5: (494, 649),
    6: (650, 721),
    7: (722, 809),
    8: (810, 905),
    9: (906, 1025),
}

VALID_FORMS = frozenset({
    "normal", "alola", "galar", "hisui", "paldea",
    "mega", "primal", "dynamax", "gigantamax", "shadow", "purified",
})
VALID_RARITIES = frozenset({"standard", "legendary", "mythical", "ultra_beast", "regional"})


def generation_for_dex(dex_id: int) -> Optional[int]:
    if not isinstance(dex_id, int) or isinstance(dex_id, bool):
        return None
    for gen, (lo, hi) in GENERATION_RANGES.items():
        if lo <= dex_id <= hi:
            return gen
    return None


UNKNOWN_FORM = "desconhecida"


def classify_pokemon_form(form: str) -> Dict[str, Any]:
    """Classify a form string explicitly without silently coercing unknown forms to 'normal'."""
    if not isinstance(form, str) or not form.strip():
        return {"raw_form": form, "normalized_form": UNKNOWN_FORM, "is_known": False, "status": "unknown_explicit"}
    norm = form.strip().lower()
    if norm in VALID_FORMS:
        return {"raw_form": form, "normalized_form": norm, "is_known": True, "status": "known"}
    return {"raw_form": form, "normalized_form": norm, "is_known": False, "status": "unknown_explicit"}


@dataclass(frozen=True)
class PokedexEntry:
    dex_id: int
    name: str
    types: Tuple[str, ...]
    form: str = "normal"
    rarity: str = "standard"
    shiny_released: bool = False
    has_costume: bool = False
    has_background: bool = False
    sprite_asset_id: str = ""
    provenance: str = "catalog-offline"
    allow_unknown_form: bool = False

    def __post_init__(self) -> None:
        if isinstance(self.dex_id, bool) or not isinstance(self.dex_id, int) or generation_for_dex(self.dex_id) is None:
            raise ValueError(f"National Dex fora do intervalo 1..1025: {self.dex_id}")
        if not isinstance(self.form, str) or not self.form.strip():
            raise ValueError(f"Forma inválida: {self.form}")
        if self.form not in VALID_FORMS and not self.allow_unknown_form:
            raise ValueError(f"Forma inválida: {self.form}")
        if self.rarity not in VALID_RARITIES:
            raise ValueError(f"Raridade inválida: {self.rarity}")
        if not self.types or len(self.types) > 2:
            raise ValueError("Espécie deve ter 1 ou 2 tipos.")

    @property
    def form_status(self) -> str:
        return "known" if self.form in VALID_FORMS else "unknown_explicit"

    @property
    def generation(self) -> int:
        gen = generation_for_dex(self.dex_id)
        assert gen is not None
        return gen

    @property
    def canonical_key(self) -> str:
        return f"{self.dex_id:04d}:{self.form}"


class PokedexCollection:
    """Offline Pokédex catalog + strictly manual collection checklist."""

    def __init__(self, entries: Iterable[PokedexEntry] = ()) -> None:
        self._entries: Dict[str, PokedexEntry] = {}
        # Explicit tri-state per canonical_key: True=owned, False=missing, absent/None=unknown
        self._owned: Dict[str, Optional[bool]] = {}
        for entry in entries:
            self.add_entry(entry)

    def add_entry(self, entry: PokedexEntry, *, allow_overwrite: bool = False) -> None:
        if not allow_overwrite and entry.canonical_key in self._entries:
            raise ValueError(f"canonical_key duplicada no catálogo Pokédex: {entry.canonical_key}")
        self._entries[entry.canonical_key] = entry

    def set_owned(self, canonical_key: str, owned: Optional[bool]) -> None:
        if canonical_key not in self._entries:
            raise KeyError(f"Entrada desconhecida no catálogo: {canonical_key}")
        self._owned[canonical_key] = owned

    def search(
        self,
        *,
        query: str = "",
        generation: Optional[int] = None,
        type_filter: Optional[str] = None,
        form: Optional[str] = None,
        rarity: Optional[str] = None,
        shiny_only: bool = False,
        costume_only: bool = False,
        background_only: bool = False,
        owned_filter: Optional[bool] = None,
    ) -> List[PokedexEntry]:
        q = query.strip().lower()
        tf = type_filter.strip().lower() if type_filter else None
        results: List[PokedexEntry] = []
        for entry in self._entries.values():
            if q and q not in entry.name.lower() and q != str(entry.dex_id):
                continue
            if generation is not None and entry.generation != generation:
                continue
            if tf and tf not in {t.lower() for t in entry.types}:
                continue
            if form is not None and entry.form != form:
                continue
            if rarity is not None and entry.rarity != rarity:
                continue
            if shiny_only and not entry.shiny_released:
                continue
            if costume_only and not entry.has_costume:
                continue
            if background_only and not entry.has_background:
                continue
            if owned_filter is not None and self._owned.get(entry.canonical_key) is not owned_filter:
                continue
            results.append(entry)
        results.sort(key=lambda e: (e.dex_id, e.form))
        return results

    def progress_summary(self, generation: Optional[int] = None) -> Dict[str, Any]:
        entries = self.search(generation=generation)
        total = len(entries)
        owned = sum(1 for e in entries if self._owned.get(e.canonical_key) is True)
        missing = sum(1 for e in entries if self._owned.get(e.canonical_key) is False)
        unknown = total - owned - missing
        pct = round((owned / total) * 100.0, 2) if total > 0 else 0.0
        return {
            "generation": generation,
            "total": total,
            "owned": owned,
            "missing": missing,
            "unknown": unknown,
            "completion_percent": pct,
        }

    def export_json(self) -> str:
        rows = []
        for entry in sorted(self._entries.values(), key=lambda e: (e.dex_id, e.form)):
            d = asdict(entry)
            d["generation"] = entry.generation
            d["canonical_key"] = entry.canonical_key
            d["owned"] = self._owned.get(entry.canonical_key)
            rows.append(d)
        return json.dumps({"schema": 1, "entries": rows}, ensure_ascii=False, indent=2, allow_nan=False)

    def export_csv(self) -> str:
        buf = io.StringIO()
        writer = csv.writer(buf)
        writer.writerow(["canonical_key", "dex_id", "generation", "name", "types", "form", "rarity", "owned"])
        for entry in sorted(self._entries.values(), key=lambda e: (e.dex_id, e.form)):
            owned = self._owned.get(entry.canonical_key)
            owned_str = "unknown" if owned is None else ("yes" if owned else "no")
            writer.writerow([
                entry.canonical_key, entry.dex_id, entry.generation, entry.name,
                "/".join(entry.types), entry.form, entry.rarity, owned_str,
            ])
        return buf.getvalue()
