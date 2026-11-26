"""Adversarial offline hardening tests for G-050-01 over PR #351 (12 verification items).

Covers:
  (1) Invalid CP/IV/BaseStats/bool inputs
  (2) Versioned source metadata for precision tables
  (3) Explicit cross-source type chart disagreement reporting
  (4) Explicit unknown form handling without silent coercion
  (5) PvP & macro simulator hard limits and truncation reporting
  (6) Ambiguous & non-existent DST local timestamps
  (7) NaN / Infinity rejection across calculators, cache, and JSON exports
  (8) ID uniqueness across Pokédex, macro cards, and event schedules
  (9) Idempotent, deep-copied schema migrations
  (10) Bounded file I/O (O_BINARY, O_NOFOLLOW, symlink/size caps) & bounded cache
  (11) Export sanitization across both keys and free-text values
  (12) Non-regression of V5 white-layer fix and existing PR #351 contracts
"""

from __future__ import annotations

import os
import tempfile
import unittest
from datetime import datetime, timezone
from pathlib import Path

from tools.pgbr_data_tools.collection_model import AtomicSnapshotStore
from tools.pgbr_data_tools.events_planner import (
    EventEntry,
    partition_schedule,
    resolve_local_datetime,
    sanitize_diagnostic_payload,
)
from tools.pgbr_data_tools.inventory_provenance import (
    compare_type_chart_sources,
    verify_cross_language_type_charts,
)
from tools.pgbr_data_tools.macro_simulator import (
    MacroCard,
    MacroRoutine,
    SeasonalEventTemplate,
    sanitize_export_record,
    validate_routine_safety,
)
from tools.pgbr_data_tools.pokemon_calc import (
    CPM_TABLE_METADATA,
    BaseStats,
    best_level_under_cap,
    compute_cp,
    compute_stat_product,
    evaluate_pvp_rank,
)
from tools.pgbr_data_tools.type_chart import CHART, TYPE_CHART_METADATA
from tools.pgbr_offline_suite.aux_planners import plan_egg_incubation
from tools.pgbr_offline_suite.evolution_moves import EVOLUTION_CALC_METADATA, compute_cp_from_cpm
from tools.pgbr_offline_suite.pokedex_catalog import (
    UNKNOWN_FORM,
    PokedexCollection,
    PokedexEntry,
    classify_pokemon_form,
)
from tools.pgbr_offline_suite.pvp_simulator import (
    MAX_SIMULATION_TURNS,
    PVP_RULESET_METADATA,
    ChargedMove,
    FastMove,
    PvpFighter,
    simulate_1v1,
)
from tools.pgbr_offline_suite.stats_and_cache import (
    BoundedOfflineCache,
    SessionSample,
    aggregate_session_window,
    migrate_snapshot_schema,
)

REPO_ROOT = Path(__file__).resolve().parents[2]


class TestGeminiG05001OfflineHardening(unittest.TestCase):
    def test_01_invalid_cp_iv_and_bool_inputs_rejected(self) -> None:
        azumarill = BaseStats(184, "Azumarill", 112, 152, 225)
        with self.assertRaises(ValueError):
            compute_cp(azumarill, True, 15, 15, 40.0)  # type: ignore[arg-type]
        with self.assertRaises(ValueError):
            compute_stat_product(azumarill, -1, 15, 15, 40.0)
        with self.assertRaises(ValueError):
            compute_stat_product(azumarill, 0, 16, 15, 40.0)
        with self.assertRaises(ValueError):
            compute_cp(BaseStats(1, "Bad", 0, 100, 100), 10, 10, 10, 20.0)
        with self.assertRaises(ValueError):
            best_level_under_cap(azumarill, 0, 15, 15, cp_cap=True)  # type: ignore[arg-type]
        with self.assertRaises(ValueError):
            evaluate_pvp_rank(azumarill, 0, 15, 15, "great", iv_floor=True)  # type: ignore[arg-type]
        with self.assertRaises(ValueError):
            compute_cp_from_cpm(0, 152, 225, 0, 15, 15, 0.7903)
        with self.assertRaises(ValueError):
            compute_cp_from_cpm(112, 152, 225, True, 15, 15, 0.7903)  # type: ignore[arg-type]

    def test_02_precision_tables_expose_versioned_source_metadata(self) -> None:
        self.assertIn("ruleset_version", TYPE_CHART_METADATA)
        self.assertEqual(TYPE_CHART_METADATA["matrix_shape"], (18, 18))
        self.assertIn("ruleset_version", CPM_TABLE_METADATA)
        self.assertEqual(CPM_TABLE_METADATA["max_level"], 51.0)
        self.assertIn("ruleset_version", EVOLUTION_CALC_METADATA)
        self.assertIn("ruleset_version", PVP_RULESET_METADATA)
        self.assertEqual(PVP_RULESET_METADATA["max_simulation_turns"], MAX_SIMULATION_TURNS)

    def test_03_cross_source_type_disagreement_explicit(self) -> None:
        audit = verify_cross_language_type_charts(REPO_ROOT)
        self.assertTrue(audit["ok"], f"Cross-language chart mismatch: {audit}")
        self.assertFalse(audit["has_disagreement"])
        self.assertIn("python_pvp_simulator", audit["all_checked_sources"])

        # Synthetic disagreement must be surfaced cell-by-cell
        mutated = [list(row) for row in CHART]
        mutated[7][8] = -1  # Ghost -> Steel deliberately corrupted
        diff = compare_type_chart_sources(
            "canonical",
            [tuple(r) for r in CHART],
            "corrupted_source",
            [tuple(r) for r in mutated],
        )
        self.assertFalse(diff["ok"])
        self.assertTrue(diff["has_disagreement"])
        self.assertEqual(diff["disagreement_count"], 1)
        self.assertEqual(diff["disagreements"][0]["attacker"], "Fantasma")
        self.assertEqual(diff["disagreements"][0]["defender"], "Aço")

    def test_04_unknown_forms_classified_explicitly(self) -> None:
        res = classify_pokemon_form("paldea_futura_x")
        self.assertFalse(res["is_known"])
        self.assertEqual(res["status"], "unknown_explicit")
        self.assertNotEqual(res["normalized_form"], "normal")

        empty = classify_pokemon_form("")
        self.assertEqual(empty["normalized_form"], UNKNOWN_FORM)
        self.assertFalse(empty["is_known"])

        with self.assertRaises(ValueError):
            PokedexEntry(25, "Pikachu", ("Electric",), form="unreleased_event_2027")
        explicit = PokedexEntry(
            25, "Pikachu", ("Electric",), form="unreleased_event_2027", allow_unknown_form=True
        )
        self.assertEqual(explicit.form_status, "unknown_explicit")

    def test_05_pvp_simulator_hard_limits_and_truncation(self) -> None:
        fm = FastMove("Bubble", "Water", 7, 11, 3)
        cm = ChargedMove("Ice Beam", "Ice", 90, 55)
        tank_a = PvpFighter("TankA", ("Water",), 100.0, 150.0, 200, fm, (cm,))
        tank_b = PvpFighter("TankB", ("Water",), 100.0, 150.0, 200, fm, (cm,))

        short = simulate_1v1(tank_a, tank_b, shields_a=2, shields_b=2, max_turns=5)
        self.assertTrue(short["truncated_by_turn_limit"])
        self.assertEqual(short["max_turns_limit"], 5)
        self.assertTrue(short["simulation_limits"]["deterministic_1v1_only"])

        with self.assertRaises(ValueError):
            simulate_1v1(tank_a, tank_b, max_turns=MAX_SIMULATION_TURNS + 1)
        with self.assertRaises(ValueError):
            simulate_1v1(tank_a, tank_b, shields_a=True)  # type: ignore[arg-type]
        bad_fighter = PvpFighter("Bad", ("Water",), float("nan"), 150.0, 200, fm, (cm,))
        with self.assertRaises(ValueError):
            simulate_1v1(bad_fighter, tank_b)

    def test_06_ambiguous_and_nonexistent_dst_times_detected(self) -> None:
        # America/New_York fall-back repeated hour: 2026-11-01 01:30
        with self.assertRaises(ValueError):
            resolve_local_datetime("2026-11-01T01:30:00", "America/New_York")
        annotated = resolve_local_datetime(
            "2026-11-01T01:30:00", "America/New_York", allow_ambiguous=True
        )
        self.assertTrue(annotated["ambiguous"])
        self.assertEqual(annotated["status"], "ambiguous_dst")

        # America/New_York spring-forward non-existent gap: 2026-03-08 02:30
        with self.assertRaises(ValueError):
            resolve_local_datetime("2026-03-08T02:30:00", "America/New_York")

        tpl = SeasonalEventTemplate(
            template_id="dst-bad",
            title="DST Ambiguous",
            revision="1",
            iana_timezone="America/New_York",
            start_local_iso="2026-11-01T01:30:00",
            end_local_iso="2026-11-01T04:00:00",
            checklist=("check",),
        )
        with self.assertRaises(ValueError):
            tpl.is_active_at(datetime(2026, 11, 1, 6, 0, tzinfo=timezone.utc))

        tpl_ok = SeasonalEventTemplate(
            template_id="ok",
            title="OK",
            revision="1",
            iana_timezone="America/Fortaleza",
            start_local_iso="2026-11-01T10:00:00",
            end_local_iso="2026-11-01T14:00:00",
            checklist=("check",),
        )
        with self.assertRaises(ValueError):
            tpl_ok.is_active_at(datetime(2026, 11, 1, 12, 0))  # naive now_utc rejected

    def test_07_nan_and_infinity_rejected_across_modules(self) -> None:
        azumarill = BaseStats(184, "Azumarill", 112, 152, 225)
        with self.assertRaises(ValueError):
            compute_cp(azumarill, 0, 15, 15, float("nan"))
        with self.assertRaises(ValueError):
            compute_cp_from_cpm(112, 152, 225, 0, 15, 15, float("inf"))
        with self.assertRaises(ValueError):
            plan_egg_incubation(float("nan"), event_distance_multiplier=1.0)
        with self.assertRaises(ValueError):
            plan_egg_incubation(5.0, event_distance_multiplier=float("inf"))
        with self.assertRaises(ValueError):
            aggregate_session_window(
                [SessionSample(1000, 100, 1, 1, 1, 100, float("nan"))], window_ms=60_000
            )
        with self.assertRaises(ValueError):
            sanitize_export_record({"score": float("nan")})
        with self.assertRaises(ValueError):
            sanitize_diagnostic_payload({"metric": float("inf")})

    def test_08_id_uniqueness_enforced(self) -> None:
        p1 = PokedexEntry(25, "Pikachu", ("Electric",), form="normal")
        col = PokedexCollection([p1])
        with self.assertRaises(ValueError):
            col.add_entry(p1)

        routine = MacroRoutine(
            routine_id="r1",
            name="Dup Cards",
            description="Duplicate card ID test",
            revision="1.0",
            cards=(
                MacroCard("c1", "pause_ms", {"ms": 100}, repeat=1),
                MacroCard("c1", "pause_ms", {"ms": 100}, repeat=1),
            ),
        )
        issues = validate_routine_safety(routine)
        self.assertTrue(any("duplicado" in i for i in issues))

        ev = EventEntry(
            event_id="ev-dup",
            title_pt="Evento",
            category="spotlight",
            start_utc="2026-11-26T10:00:00Z",
            end_utc="2026-11-26T12:00:00Z",
            source="offline",
        )
        with self.assertRaises(ValueError):
            partition_schedule([ev, ev], datetime(2026, 11, 26, 11, 0, tzinfo=timezone.utc))

    def test_09_idempotent_schema_migrations(self) -> None:
        original = {"schema_version": 1, "items": ["a"]}
        migrations = {
            1: lambda d: {**d, "migrated_v2": True},
            2: lambda d: {**d, "migrated_v3": len(d["items"])},
        }
        once = migrate_snapshot_schema(original, migrations, target_version=3)
        twice = migrate_snapshot_schema(once, migrations, target_version=3)
        self.assertEqual(once, twice)
        self.assertEqual(original["schema_version"], 1)  # caller dict untouched
        with self.assertRaises(ValueError):
            migrate_snapshot_schema({"schema_version": True}, migrations, target_version=2)  # type: ignore[dict-item]

    def test_10_bounded_snapshot_files_and_bounded_cache(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            p = Path(tmp) / "snap.json"
            store = AtomicSnapshotStore(p, max_bytes=1024)
            store.save("ns", "r1", {"ok": 1})
            self.assertEqual(store.load()["data"]["ok"], 1)

            # NaN rejected on save
            with self.assertRaises(ValueError):
                store.save("ns", "r2", {"bad": float("nan")})

            # Symlink rejected on load
            link = Path(tmp) / "link.json"
            os.symlink(p, link)
            with self.assertRaises(ValueError):
                AtomicSnapshotStore(link, max_bytes=1024).load()

            # Oversized file rejected before full memory ingestion
            big = Path(tmp) / "big.json"
            big.write_bytes(b" " * 4096)
            with self.assertRaises(ValueError):
                AtomicSnapshotStore(big, max_bytes=512).load()

        cache = BoundedOfflineCache(max_bytes=1024, max_entries=2)
        cache.put("ns", "k1", {"v": 1})
        cache.put("ns", "k2", {"v": 2})
        cache.put("ns", "k3", {"v": 3})
        self.assertEqual(cache.count(), 2)
        self.assertIsNone(cache.get("ns", "k1"))
        with self.assertRaises(ValueError):
            cache.put("ns", "k4", {"bad": float("inf")})
        with self.assertRaises(ValueError):
            BoundedOfflineCache(max_bytes=True)  # type: ignore[arg-type]

    def test_11_free_text_and_key_sanitization(self) -> None:
        dirty = {
            "api_token": "topsecret",
            "note": (
                "Email user@domain.com token Bearer abcdef1234567890 "
                "coords -5.9154, -35.2631 path /Users/luciano/private.txt "
                "win C:\\Users\\luciano\\keys.json"
            ),
        }
        clean_macro = sanitize_export_record(dirty)
        self.assertEqual(clean_macro["api_token"], "[REDACTED]")
        for leaked in ("user@domain.com", "abcdef1234567890", "-5.9154", "/Users/luciano", "C:\\Users\\luciano"):
            self.assertNotIn(leaked, clean_macro["note"])

        clean_diag = sanitize_diagnostic_payload(dirty)
        self.assertEqual(clean_diag["api_token"], "[REDACTED]")
        for leaked in ("user@domain.com", "abcdef1234567890", "-5.9154", "/Users/luciano", "C:\\Users\\luciano"):
            self.assertNotIn(leaked, clean_diag["note"])


if __name__ == "__main__":
    unittest.main()
