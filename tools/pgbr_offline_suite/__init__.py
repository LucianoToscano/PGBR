"""Independent offline PGBR engineering suite (G-049-03).

Covers COL-04, COL-03, BAT-03, AUX-01, AUX-02, SYS-02, DATA-03 and SYS-05.
Zero Android runtime mutations, zero game actions, V5 untouched, zero file
overlap with tools/pgbr_data_tools (G-049-02) or PRs #318/#319/#321/#322/#324.
"""
from . import (
    aux_planners,
    evolution_moves,
    pokedex_catalog,
    pvp_simulator,
    stats_and_cache,
)

__all__ = [
    "aux_planners",
    "evolution_moves",
    "pokedex_catalog",
    "pvp_simulator",
    "stats_and_cache",
]
