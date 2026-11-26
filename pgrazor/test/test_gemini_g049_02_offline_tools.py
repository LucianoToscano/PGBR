"""Comprehensive offline test suite for Gemini G-049-02 toolkit.

Strictly offline Python tests — zero Android runtime/native mutations,
zero game-action automation, V5 white-layer fix untouched, zero overlap with PR #318.
"""
from datetime import datetime, timezone
from pathlib import Path
import sys
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[2]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools.pgbr_data_tools.type_chart import (
    CHART,
    defender_profile,
    effectiveness_multiplier,
    effectiveness_steps,
)
from tools.pgbr_data_tools.pokemon_calc import (
    BaseStats,
    compute_cp,
    evaluate_pvp_rank,
    power_up_cost,
)
from tools.pgbr_data_tools.collection_model import (
    AtomicSnapshotStore,
    PokemonRecord,
    evaluate_protection,
    plan_berry_fallback,
)
from tools.pgbr_data_tools.battle_centers import (
    AttackerCandidate,
    RaidBoss,
    lookup_rocket_quote,
    plan_max_battle,
    recommend_raid_counters,
)
from tools.pgbr_data_tools.inventory_provenance import (
    build_embedded_inventory,
    translate_glossary,
    verify_cross_language_type_charts,
)
from tools.pgbr_data_tools.macro_simulator import (
    MacroCard,
    MacroRoutine,
    SeasonalEventTemplate,
    sanitize_export_record,
    simulate_routine_offline,
)
from tools.pgbr_data_tools import events_planner


class TestGeminiG04902TypeChart(unittest.TestCase):
    def test_cross_language_18x18_consistency(self):
        audit = verify_cross_language_type_charts(ROOT)
        if audit["missing_sources"]:
            self.skipTest(f"Partial/isolated environment missing repo sources: {audit['missing_sources']}")
        self.assertTrue(audit["ok"], f"Type chart mismatches: {audit['mismatches']}")
        self.assertEqual(len(audit["checked_sources"]), 3)
        self.assertEqual(audit["mismatches"], [])

    def test_canonical_rules_and_fail_closed(self):
        self.assertEqual(effectiveness_steps("Fantasma", "Aço"), 0)
        self.assertAlmostEqual(effectiveness_multiplier("Ghost", "Steel"), 1.0)
        self.assertEqual(effectiveness_steps("Sombrio", "Aço"), 0)
        self.assertEqual(effectiveness_steps("Elétrico", "Água", "Voador"), 2)
        self.assertAlmostEqual(effectiveness_multiplier("Electric", "Water", "Flying"), 2.56)
        self.assertEqual(effectiveness_steps("Ground", "Flying", "Electric"), -1)
        self.assertIsNone(effectiveness_steps("Fogo", "Água", "Água"))
        self.assertIsNone(effectiveness_steps("Desconhecido", "Água"))
        self.assertEqual(sum(1 for row in CHART for v in row if v == -2), 8)
        prof = defender_profile("Dragão", "Voador")
        self.assertIsNotNone(prof)
        self.assertEqual(prof["fraquezas"][0][0], "Gelo")


class TestGeminiG04902PokemonCalc(unittest.TestCase):
    def test_cp_power_up_and_pvp_rank(self):
        azumarill = BaseStats(184, "Azumarill", 112, 152, 225)
        self.assertEqual(compute_cp(azumarill, 0, 15, 15, 45.5), 1499)
        res = evaluate_pvp_rank(azumarill, 0, 15, 15, "great", max_level=50.0)
        self.assertIsNotNone(res)
        self.assertEqual(res.rank, 1)
        self.assertEqual(res.cp, 1499)
        self.assertEqual(res.total_combinations, 4096)

        normal_cost = power_up_cost(20.0, 40.0)
        shadow_cost = power_up_cost(20.0, 40.0, shadow=True)
        lucky_cost = power_up_cost(20.0, 40.0, lucky=True)
        self.assertGreater(shadow_cost.stardust, normal_cost.stardust)
        self.assertEqual(lucky_cost.stardust, normal_cost.stardust // 2)
        with self.assertRaises(ValueError):
            power_up_cost(20.0, 25.0, shadow=True, purified=True)


class TestGeminiG04902CollectionAndCache(unittest.TestCase):
    def test_tristate_protection_and_unloaded_tags(self):
        rec = PokemonRecord(
            uid="pk-1", species_id=25, species_name="Pikachu", family_id=25,
            atk_iv=1, def_iv=2, sta_iv=3,
            shiny=False, rare=False, event=False, costume=False, background=False,
            favorite=False, shadow=False, purified=False, dynamax=False, gigantamax=False,
            xxl=False, xxs=False, legacy_move=False, tags=(), tags_loaded=False,
        )
        self.assertEqual(evaluate_protection(rec, protected_tags={"PvP"}).status, "UNCERTAIN")

        gmax = PokemonRecord(
            uid="pk-2", species_id=6, species_name="Charizard", family_id=4,
            atk_iv=2, def_iv=2, sta_iv=2, gigantamax=True, tags_loaded=True,
        )
        self.assertEqual(evaluate_protection(gmax).status, "PROTECTED")

    def test_berry_fallback_and_atomic_snapshot_rollback(self):
        plan = plan_berry_fallback(
            ["golden_razz", "silver_pinap", "razz"],
            {"golden_razz": None, "silver_pinap": 2, "razz": 15},
            reserves={"silver_pinap": 5, "razz": 5},
        )
        self.assertEqual(plan["selected"], "razz")
        self.assertEqual(plan["available_above_reserve"], 10)
        self.assertEqual(plan["earlier_unknown"], ["golden_razz"])

        with tempfile.TemporaryDirectory(prefix="pgbr-atomic-") as tmp:
            store = AtomicSnapshotStore(Path(tmp) / "collection.json")
            store.save("collection", "v1", {"count": 10})
            store.save("collection", "v2", {"count": 20})
            self.assertEqual(store.load()["data"]["count"], 20)
            store.path.write_text('{"schema":1,"sha256":"bad","data":{}}', encoding="utf-8")
            with self.assertRaises(ValueError):
                store.load()
            self.assertEqual(store.rollback()["data"]["count"], 10)


class TestGeminiG04902BattleCentersAndInventory(unittest.TestCase):
    def test_raid_max_and_rocket_centers(self):
        boss = RaidBoss("Rayquaza", 5, ("Dragon", "Flying"), boss_move_type="Flying", source="manual")
        roster = [
            AttackerCandidate("Mamoswine", ("Ice", "Ground"), "Ice", 18.0, shadow=True),
            AttackerCandidate("Charizard", ("Fire", "Flying"), "Fire", 17.0, gigantamax_eligible=True, max_role="attacker"),
            AttackerCandidate("Blissey", ("Normal",), "Normal", 8.0, dynamax_eligible=True, max_role="healer"),
            AttackerCandidate("Metagross", ("Steel", "Psychic"), "Steel", 16.0, dynamax_eligible=True, max_role="tank"),
        ]
        raid = recommend_raid_counters(boss, roster, weather="snow")
        self.assertEqual(raid["counters"][0]["name"], "Mamoswine")

        max_plan = plan_max_battle("Venusaur", 6, ("Grass", "Poison"), roster, available_particles=1000, planned_move_upgrades=[2])
        self.assertEqual(max_plan["total_particle_cost"], 1400)
        self.assertEqual(max_plan["particle_shortfall"], 400)
        self.assertEqual(max_plan["eligible_count"], 3)
        self.assertFalse(max_plan["device_tested"])

        hits = lookup_rocket_quote("hálito de fogo")
        self.assertEqual(len(hits), 1)
        self.assertEqual(hits[0]["specialty_type"], "Fire")

    def test_embedded_inventory_and_glossary_no_pr318_duplication(self):
        inv = build_embedded_inventory(ROOT)
        ids = {d["id"] for d in inv["datasets"]}
        self.assertNotIn("feed_icons_provenance", ids)
        self.assertTrue(inv["item_icon_ids_unique"])
        if all(d["present"] for d in inv["datasets"]):
            self.assertGreater(inv["item_icon_count"], 50)
        self.assertEqual(translate_glossary("status", "PROTECTED"), "Protegido")
        self.assertEqual(translate_glossary("event_status", "expired"), "Encerrado")
        self.assertEqual(translate_glossary("research_kind", "collection_challenge"), "Desafio de Coleção")
        self.assertIn("sem tradução", translate_glossary("status", "UNKNOWN_KEY"))


class TestGeminiG04902MacroEventsAndSanitization(unittest.TestCase):
    def test_offline_macro_simulator_blocks_game_actions(self):
        routine = MacroRoutine(
            routine_id="r1",
            name="Checklist Diário",
            description="Rotina local de lembrete e cache",
            revision="1.0",
            cards=[
                MacroCard("c1", "refresh_offline_cache", "Atualizar cache"),
                MacroCard("c2", "notify_local", "Avisar", repeat=2, condition_flag="opt_in"),
            ],
        )
        roundtrip = MacroRoutine.from_json(routine.to_json())
        self.assertEqual(roundtrip.duplicate("r2", "Cópia").routine_id, "r2")
        res = simulate_routine_offline(roundtrip, flags={"opt_in": True})
        self.assertEqual(res["status"], "COMPLETED")
        self.assertEqual(res["steps_executed"], 3)

        unsafe = MacroRoutine("r-bad", "Proibida", "", "1.0", [MacroCard("x", "auto_transfer_all", "X")])
        self.assertEqual(simulate_routine_offline(unsafe)["status"], "REJECTED")

        tpl = SeasonalEventTemplate(
            "evt-1", "Dia Comunitário", "2026-11", "America/Fortaleza",
            "2026-11-20T14:00:00", "2026-11-20T17:00:00", ("Separar bolas",),
        )
        self.assertFalse(tpl.is_active_at(datetime(2026, 11, 25, 12, 0, tzinfo=timezone.utc)))
        self.assertTrue(tpl.is_active_at(datetime(2026, 11, 20, 18, 0, tzinfo=timezone.utc)))

        sanitized = sanitize_export_record({"name": "ok", "auth_token": "abc", "lat": -5.9, "nested": {"email": "a@b.c"}})
        self.assertEqual(sanitized["auth_token"], "[REDACTED]")
        self.assertEqual(sanitized["lat"], "[REDACTED]")
        self.assertEqual(sanitized["nested"]["email"], "[REDACTED]")

    def test_events_iana_expiry_checklist_reminders_and_sanitizer(self):
        now = datetime(2026, 11, 26, 18, 0, 0, tzinfo=timezone.utc)
        ev_expired = events_planner.EventEntry(
            event_id="ev-past", title_pt="Hora Destacada Passada", category="spotlight_hour",
            start_utc="2026-11-26T16:00:00Z", end_utc="2026-11-26T17:00:00Z", source="oficial",
        )
        ev_active = events_planner.EventEntry(
            event_id="ev-now", title_pt="Dia Comunitário", category="community_day",
            start_utc="2026-11-26T17:00:00Z", end_utc="2026-11-26T20:00:00Z", source="oficial",
            bonuses=("3x Poeira Estelar",), exclusive_moves=("Hydro Cannon",),
        )
        ev_future = events_planner.EventEntry(
            event_id="ev-next", title_pt="Hora de Reides", category="raid_hour",
            start_utc="2026-11-26T21:00:00Z", end_utc="2026-11-26T22:00:00Z", source="oficial",
        )
        part = events_planner.partition_schedule([ev_expired, ev_active, ev_future], now, "America/Fortaleza")
        self.assertEqual([e["event_id"] for e in part["active"]], ["ev-now"])
        self.assertEqual([e["event_id"] for e in part["expired"]], ["ev-past"])
        self.assertEqual([e["event_id"] for e in part["upcoming"]], ["ev-next"])
        self.assertTrue(part["active"][0]["local_start"].endswith("-03:00"))

        self.assertEqual(events_planner.cache_freshness("2026-11-26T17:30:00Z", now, 3600)["state"], "fresh")
        self.assertEqual(events_planner.cache_freshness("2026-11-26T15:00:00Z", now, 3600)["state"], "stale")

        cl = events_planner.ManualChecklist()
        cl.upsert(events_planner.ChecklistItem("r1", "Capturar 10 Pokémon", "field", priority=1, window_key="2026-11-26"))
        self.assertEqual(cl.summary()["unknown"], 1)
        cl.confirm_state("r1", "done")
        self.assertEqual(cl.summary()["done"], 1)
        self.assertEqual(cl.reset_window("2026-11-26"), 1)
        self.assertEqual(cl.summary()["pending"], 1)

        planner = events_planner.LocalReminderPlanner()
        rem = events_planner.LocalReminder("ev-next", 15, "America/Fortaleza", "Hora de Reides em 15 min", opt_in=True)
        self.assertTrue(planner.schedule(rem))
        self.assertFalse(planner.schedule(rem))
        self.assertEqual(len(planner.active_reminders()), 1)
        self.assertTrue(planner.cancel("ev-next", 15))
        self.assertEqual(len(planner.active_reminders()), 0)

        raw = {
            "trainer": "LucianoReal",
            "email": "user@example.com",
            "note": "Contato user@example.com em -5.9154, -35.2629 token ghp_1234567890abcdef",
            "ok_count": 5,
        }
        clean = events_planner.sanitize_diagnostic_payload(raw)
        self.assertEqual(clean["trainer"], "[REDACTED]")
        self.assertEqual(clean["email"], "[REDACTED]")
        self.assertNotIn("user@example.com", clean["note"])
        self.assertNotIn("-5.9154", clean["note"])
        self.assertNotIn("ghp_1234567890abcdef", clean["note"])
        self.assertEqual(clean["ok_count"], 5)


if __name__ == "__main__":
    unittest.main()
