"""Comprehensive unit tests for Gemini G-049-03 offline suite.

Covers COL-04, COL-03, BAT-03, AUX-01, AUX-02, AUTO-03/04, SYS-02, DATA-03/SYS-05,
and confirms V5 white-layer fix remains untouched.
"""
from datetime import datetime, timezone
import json
from pathlib import Path
import sys
import unittest

ROOT = Path(__file__).resolve().parents[2]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools.pgbr_offline_suite import (
    aux_planners,
    evolution_moves,
    pokedex_catalog,
    pvp_simulator,
    stats_and_cache,
)


class TestGeminiG04903OfflineSuite(unittest.TestCase):
    def test_pokedex_search_checklist_and_export(self):
        dex = pokedex_catalog.PokedexCollection([
            pokedex_catalog.PokedexEntry(6, "Charizard", ("Fire", "Flying"), form="gigantamax", shiny_released=True),
            pokedex_catalog.PokedexEntry(25, "Pikachu", ("Electric",), form="normal", shiny_released=True, has_costume=True),
            pokedex_catalog.PokedexEntry(150, "Mewtwo", ("Psychic",), form="normal", rarity="legendary", shiny_released=True, has_background=True),
            pokedex_catalog.PokedexEntry(890, "Eternatus", ("Poison", "Dragon"), form="normal", rarity="legendary"),
        ])
        dex.set_owned("0006:gigantamax", True)
        dex.set_owned("0025:normal", False)

        self.assertEqual(len(dex.search(generation=1)), 3)
        self.assertEqual([e.name for e in dex.search(rarity="legendary", background_only=True)], ["Mewtwo"])
        self.assertEqual([e.name for e in dex.search(form="gigantamax")], ["Charizard"])
        self.assertEqual([e.name for e in dex.search(costume_only=True)], ["Pikachu"])

        prog = dex.progress_summary(generation=1)
        self.assertEqual(prog["total"], 3)
        self.assertEqual(prog["owned"], 1)
        self.assertEqual(prog["missing"], 1)
        self.assertEqual(prog["unknown"], 1)

        exported_json = json.loads(dex.export_json())
        self.assertEqual(len(exported_json["entries"]), 4)
        self.assertIn("0006:gigantamax", dex.export_csv())

    def test_evolution_branch_preview_and_legacy_moves(self):
        branches = [
            evolution_moves.EvolutionBranch(
                target_dex=197, target_name="Umbreon",
                base_attack=126, base_defense=240, base_stamina=216,
                candy_cost=25, buddy_km_required=10.0, lure_or_time_requirement="night",
                moves=(evolution_moves.MoveEligibility("Last Resort", "Normal", "charged", True, True, "Community Day"),),
            ),
            evolution_moves.EvolutionBranch(
                target_dex=196, target_name="Espeon",
                base_attack=261, base_defense=175, base_stamina=163,
                candy_cost=25, buddy_km_required=10.0, lure_or_time_requirement="day",
            ),
        ]
        res = evolution_moves.preview_evolution_branches(
            branches, iv_atk=0, iv_def=15, iv_sta=15, current_cpm=0.68192836,
            candy_stock=30, league_cap=1500,
        )
        by_name = {r["target_name"]: r for r in res}
        self.assertTrue(by_name["Umbreon"]["fits_league_cap"])
        self.assertFalse(by_name["Espeon"]["fits_league_cap"])
        self.assertTrue(by_name["Umbreon"]["candy_enough"])
        self.assertEqual(by_name["Umbreon"]["legacy_moves"][0]["move_name"], "Last Resort")
        self.assertFalse(by_name["Umbreon"]["resources_spent"])

    def test_pvp_1v1_simulator_and_team_coverage(self):
        # Verify Ghost -> Steel = 1.0x in simulator chart
        self.assertAlmostEqual(pvp_simulator.type_multiplier("Ghost", ("Steel",)), 1.0)
        swampert = pvp_simulator.PvpFighter(
            name="Swampert", types=("Water", "Ground"),
            attack=122.0, defense=109.0, hp=138,
            fast_move=pvp_simulator.FastMove("Mud Shot", "Ground", 3, 9, 2),
            charged_moves=(
                pvp_simulator.ChargedMove("Hydro Cannon", "Water", 80, 40),
                pvp_simulator.ChargedMove("Earthquake", "Ground", 110, 65),
            ),
        )
        registeel = pvp_simulator.PvpFighter(
            name="Registeel", types=("Steel",),
            attack=96.0, defense=190.0, hp=129,
            fast_move=pvp_simulator.FastMove("Lock-On", "Normal", 1, 5, 1),
            charged_moves=(
                pvp_simulator.ChargedMove("Focus Blast", "Fighting", 140, 75),
                pvp_simulator.ChargedMove("Zap Cannon", "Electric", 130, 80),
            ),
        )
        sim = pvp_simulator.simulate_1v1(swampert, registeel, shields_a=1, shields_b=1)
        self.assertEqual(sim["winner"], "Swampert")
        self.assertTrue(sim["offline_simulation"])
        self.assertFalse(sim["device_tested"])

        cov = pvp_simulator.analyze_team_coverage([
            ("Swampert", ("Water", "Ground")),
            ("Azumarill", ("Water", "Fairy")),
            ("Skarmory", ("Steel", "Flying")),
        ])
        self.assertIn("Grass", cov["shared_weaknesses"])
        self.assertNotIn("Grass", cov["uncovered_threats"])  # Skarmory double-resists Grass

    def test_aux_planners_buddy_eggs_medals_and_seasonal(self):
        buddy = aux_planners.plan_buddy_excitement(24, ["play", "feed", "walk_2km"])
        self.assertTrue(buddy["excited_reached"])
        self.assertEqual(buddy["remaining_ep"], 0)

        egg = aux_planners.plan_egg_incubation(10.0, super_incubator=True, event_distance_multiplier=0.5)
        self.assertAlmostEqual(egg["effective_km"], 3.33, places=2)

        med = aux_planners.medal_catch_multiplier(2500, 200)
        self.assertEqual(med["tier_1"], "platinum")
        self.assertEqual(med["tier_2"], "gold")
        self.assertAlmostEqual(med["multiplier"], 1.35)

        prof = aux_planners.SeasonalProfile(
            "HALLOWEEN_2026", "Halloween 2026",
            "2026-10-20T10:00:00Z", "2026-11-01T20:00:00Z",
            is_historical_fixture=True, confirmed_redemptions=2,
        )
        ev = aux_planners.evaluate_seasonal_profile(prof, datetime(2026, 10, 25, 12, 0, tzinfo=timezone.utc))
        self.assertTrue(ev["in_window"])
        self.assertFalse(ev["proves_live_event"])
        self.assertFalse(ev["inferred_progress"])

    def test_session_stats_migration_and_bounded_cache(self):
        t0 = 1_700_000_000_000
        samples = [
            stats_and_cache.SessionSample(t0, xp=1000, caught=10, seen=12, stops=5, stardust_earned=1000, km=1.0),
            stats_and_cache.SessionSample(t0, xp=1000, caught=10, seen=12, stops=5, stardust_earned=1000, km=1.0),  # duplicate
            stats_and_cache.SessionSample(t0 + 1_800_000, xp=11000, caught=50, seen=62, stops=25, stardust_earned=6000, km=3.5),
        ]
        agg = stats_and_cache.aggregate_session_window(samples, 3_600_000, goals={"xp": 10000})
        self.assertEqual(agg["samples"], 2)
        self.assertEqual(agg["deltas"]["xp"], 10000)
        self.assertAlmostEqual(agg["rates"]["xp_per_hour"], 20000.0)
        self.assertAlmostEqual(agg["rates"]["catch_rate"], 0.8)
        self.assertEqual(agg["denominators"]["catch_rate_seen"], 50)
        self.assertTrue(agg["goals"]["xp"]["met"])

        # Schema migration v1 -> v3
        migrations = {
            1: lambda d: {**d, "migrated_v2": True},
            2: lambda d: {**d, "migrated_v3": True},
        }
        migrated = stats_and_cache.migrate_snapshot_schema({"schema_version": 1, "x": 1}, migrations, 3)
        self.assertEqual(migrated["schema_version"], 3)
        self.assertTrue(migrated["migrated_v2"] and migrated["migrated_v3"])

        # Bounded LRU cache + SHA-256 tamper detection
        cache = stats_and_cache.BoundedOfflineCache(max_bytes=64)
        cache.put("dex", "1", {"n": "Bulbasaur"})
        cache.put("dex", "2", {"n": "Ivysaur"})
        cache.put("dex", "3", {"n": "Venusaur"})
        self.assertLessEqual(cache.used_bytes, 64)
        self.assertEqual(cache.get("dex", "3")["n"], "Venusaur")
        # Tamper raw bytes -> must raise integrity error
        cache._entries["dex:3"]["raw"] = b'{"n":"Tampered"}'
        with self.assertRaises(ValueError):
            cache.get("dex", "3")

    def test_v5_white_layer_fix_untouched(self):
        mini = ROOT / "pgrazor" / "native" / "rocket_minigame.cpp"
        if not mini.is_file():
            self.skipTest("Partial checkout without native sources")
        src = mini.read_text(encoding="utf-8")
        self.assertIn("hide_canvas_white_layer", src)
        self.assertIn("enforce_white_layers", src)


if __name__ == "__main__":
    unittest.main()
