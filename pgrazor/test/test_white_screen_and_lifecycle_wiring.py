"""P0 contracts: charged-attack white screen and battle-executor lifecycle.

These are wiring guards, not gameplay proofs. They exist so a later refactor cannot
silently reintroduce the two P0 regressions:

* BAT-01 — the Rocket tap assist scaled the visual Transform by 1000x whenever the
  collider geometry was not recognised (issue #192). The collector must only ever
  write collider geometry, through the bounded policy in
  ``rocket_minigame_shape.h``, and must perform no mutation at all for unknown geometry.
* BAT-03 — a transient inability to re-verify a running battle (or an option toggle)
  used to tear the verified executor down, so Auto ATK stopped after a battle until the
  app was restarted (issue #105). The host must distinguish an authoritative end from a
  suspension, and must not revoke the user policy on a single racing transport read.

Android device validation stays separate: ``device_tested=false``.
"""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[2]
NATIVE = ROOT / "pgrazor" / "native"


class WhiteScreenContractTests(unittest.TestCase):
    def setUp(self):
        self.mini = (NATIVE / "rocket_minigame.cpp").read_text(encoding="utf-8")
        self.shape = (NATIVE / "rocket_minigame_shape.h").read_text(encoding="utf-8")

    def test_collector_uses_the_bounded_shape_policy(self):
        self.assertIn('#include "rocket_minigame_shape.h"', self.mini)
        self.assertIn("pgr_rocket_shape::enlarge(", self.mini)
        self.assertIn("const pgr_rocket_shape::Result r = pgr_rocket_shape::enlarge(in);", self.mini)

    def test_visual_transform_is_never_scaled(self):
        # The 1000x visual fallback (and its restore) must never come back.
        self.assertNotIn("set_scale", self.mini)
        self.assertNotIn("Transform.set_localScale", self.mini)
        self.assertNotIn("setScale_method, transform", self.mini)

    def test_unknown_geometry_performs_no_mutation(self):
        self.assertIn("if (!r.ok) { g_cfg.free_root(h); return false; }", self.mini)
        self.assertIn("if (!shape_ready || g_failed.load())", self.mini)
        # No fallback branch may publish a snapshot after a failed geometry read.
        self.assertNotIn("if (current_collider) prepare_shape", self.mini)

    def test_policy_bounds_are_declared_and_enforced(self):
        for token in ("kEnlargeTimes", "kMaxEnlarged", "kMinAxisScale", "kMaxAxisScale"):
            self.assertIn(token, self.shape)
        self.assertIn("if (!usable(value, kMaxEnlarged)) return out;", self.shape)
        self.assertIn("if (!(value >= in.dimensions[i])) return out;", self.shape)

    def test_host_fixture_is_registered(self):
        runner = (ROOT / "tools" / "native_host_tests.py").read_text(encoding="utf-8")
        self.assertIn("rocket_minigame_shape_test.cpp", runner)


class ExecutorLifecycleContractTests(unittest.TestCase):
    def setUp(self):
        self.host = (NATIVE / "combat_host.cpp").read_text(encoding="utf-8")
        self.policy = (NATIVE / "pvp_executor_policy.h").read_text(encoding="utf-8")
        self.freshness = (NATIVE / "transport_option_policy.h").read_text(encoding="utf-8")

    def test_host_uses_the_suspend_policy(self):
        self.assertIn('#include "pvp_executor_policy.h"', self.host)
        self.assertIn("pgr_pvp_exec::decide(", self.host)
        self.assertIn("pve_classifier_state(", self.host)
        self.assertIn("pvp_set_suspended(", self.host)

    def test_suspension_never_emits_an_action(self):
        self.assertIn("const uint32_t shared_mask=g_pvp_suspended?0u:g_opts.rocket_mask;", self.host)
        self.assertIn("bool on=active&&!g_pvp_suspended&&rocket", self.host)
        # Only KEEP may invoke the game.
        self.assertIn("inline bool actions_allowed(Action action) { return action == Action::KEEP; }",
                      self.policy)

    def test_a_transient_gap_suspends_instead_of_leaving(self):
        self.assertIn("if (classifier == Classifier::UNREADABLE) return Action::SUSPEND;", self.policy)
        self.assertIn("if (!policy_live) return Action::SUSPEND;", self.policy)
        self.assertIn("if (classifier == Classifier::PVE_ACTIVE) return Action::LEAVE;", self.policy)

    def test_transport_race_does_not_revoke_the_user_policy(self):
        self.assertIn('#include "transport_option_policy.h"', self.host)
        self.assertIn("g_option_freshness.observe(false, now)", self.host)
        self.assertIn("g_option_freshness.observe(true, now)", self.host)
        self.assertIn("constexpr uint64_t kGraceMs = 1000;", self.freshness)
        # A missing clock still fails closed.
        self.assertIn("if (now_ms == 0) return true;", self.freshness)

    def test_diagnostics_stay_sanitized(self):
        # The suspension event carries only a mode and a fixed reason tag.
        self.assertIn('"pvpExecutorSuspended":', self.host)
        for forbidden in ("playerName", "trainer", "latitude", "longitude", "token"):
            self.assertNotIn(forbidden, self.policy)

    def test_host_fixtures_are_registered(self):
        runner = (ROOT / "tools" / "native_host_tests.py").read_text(encoding="utf-8")
        self.assertIn("pvp_executor_policy_test.cpp", runner)
        self.assertIn("transport_option_policy_test.cpp", runner)


class SuperEffectiveDataContractTests(unittest.TestCase):
    """The type chart feeds both the priority policy and the Java selector."""

    def test_both_layers_still_carry_the_chart(self):
        counter = (NATIVE / "battle_counter.h").read_text(encoding="utf-8")
        java = (ROOT / "pgrazor" / "src" / "dev" / "pgrazor"
                / "PvpChargedPreference.java").read_text(encoding="utf-8")
        self.assertIn("kChart[kTypeCount][kTypeCount]", counter)
        self.assertIn("byte[][] TYPE", java)

    def test_priority_policy_never_defaults_to_super(self):
        priority = (NATIVE / "smart_attack_priority.h").read_text(encoding="utf-8")
        self.assertIn("return Effect::UNKNOWN;", priority)
        self.assertIn("total > 0 ? Effect::SUPER : total < 0 ? Effect::RESISTED : Effect::NEUTRAL",
                      priority)


if __name__ == "__main__":
    unittest.main()
