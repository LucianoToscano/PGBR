"""White layer fix must stay appearance-only (LT048, issue #192 follow-up).

The milky white that covers the battle is the tap object drawn 1000x bigger by the historical
collector. The only sanctioned correction is to make that layer invisible; the collector itself
(scale, SetActive, collider, running field) must remain byte-for-byte the reference behavior of
PR #297. This suite fails if a future edit turns the visual correction into a functional change.
"""
import re
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
NATIVE = ROOT / "pgrazor" / "native"
MINI = (NATIVE / "rocket_minigame.cpp").read_text(encoding="utf-8")
HEADER = (NATIVE / "rocket_minigame.h").read_text(encoding="utf-8")
HOST = (NATIVE / "combat_host.cpp").read_text(encoding="utf-8")


class WhiteLayerVisualOnlyTest(unittest.TestCase):
    def test_collector_scale_is_unchanged(self):
        """The reference functional line from PR #297 must still be there."""
        self.assertIn("scaled[i] = a * 1000.0f;", MINI)
        self.assertIn("set_scale(g_cfg.target(g_mini.transform), scaled);", MINI)
        self.assertIn("set_active(g_cfg.target(g_mini.go), 1);", MINI)

    def test_only_renderer_enabled_is_written(self):
        """No collider, transform, shader or canvas property may be written by the fix."""
        block = MINI[MINI.index("bool hide_white_layer("):MINI.index("void restore_alpha(")]
        self.assertIn("set_enabled", block)
        self.assertIn("get_enabled", block)
        self.assertIn("get_components_in_children", block)
        for forbidden in ("setScale", "set_scale", "set_size", "set_color", "SetActive",
                          "set_localScale", "set_enabled_collider", "collider.enabled"):
            self.assertNotIn(forbidden, block, "appearance fix must not touch " + forbidden)

    def test_void_setter_is_not_read_as_failure(self):
        """A successful Renderer.set_enabled returns null; only a managed exception is failure."""
        block = MINI[MINI.index("bool write_enabled("):MINI.index("bool hide_white_layer(")]
        self.assertIn("void *exc = nullptr;", block)
        self.assertIn("if (exc)", block)
        self.assertNotIn("if (!visual_invoke(g_visual_ex.set_enabled", block)

    def test_every_hidden_renderer_is_restored(self):
        self.assertIn("void restore_white_layer(MiniState &snap)", MINI)
        restore = MINI[MINI.index("void restore_white_layer("):MINI.index("void restore_alpha(")]
        self.assertIn("hidden_was_enabled[i]", restore)
        self.assertIn("hidden_index[i]", restore)
        self.assertIn("g_cfg.free_root(snap.hidden);", restore)
        # restore must run inside do_restore, next to the alpha restore
        body = MINI[MINI.index("void do_restore()"):MINI.index("void do_collect(")]
        self.assertIn("restore_white_layer(snap);", body)

    def test_already_disabled_and_unverified_writes_are_skipped(self):
        block = MINI[MINI.index("bool hide_white_layer("):MINI.index("void restore_white_layer(")]
        self.assertIn("if (!was) continue;", block)
        self.assertIn("if (!write_enabled(renderer, 0)) continue;", block)
        self.assertIn("PGR_MINI_MAX_HIDDEN", block)

    def test_fix_cannot_disable_the_collector(self):
        """The call must be independent: no failure path may latch g_failed or skip collection."""
        call = MINI[MINI.index("if(current_collider)hide_white_layer"):]
        call = call[:call.index("g_have_mini = true;")]
        self.assertNotIn("g_failed.store(1)", call)
        self.assertNotIn("return;", call)

    def test_config_v4_is_backward_compatible(self):
        self.assertIn('static_assert(sizeof(PgrRocketMiniConfigV3) == 352', HEADER)
        self.assertIn('static_assert(sizeof(PgrRocketMiniConfigV4) == 376', HEADER)
        self.assertIn("const bool v3=cfg->version==3", MINI)
        self.assertIn("const bool v4=cfg->version==4", MINI)
        self.assertIn("(!v1 && !v2 && !v3 && !v4)", MINI)

    def test_host_resolves_the_new_binding_with_fallback(self):
        self.assertIn('method(renderer.klass,"get_enabled","System.Boolean",nullptr)', HOST)
        self.assertIn('method(renderer.klass,"set_enabled","System.Void","System.Boolean")', HOST)
        self.assertIn('method(component.klass,"GetComponentsInChildren","UnityEngine.Component[]","System.Type")', HOST)
        self.assertIn("white_hide_ready?4:visual_ready?3:shape_ready?2:1", HOST)
        self.assertIn("miniWhiteHideBound", HOST)

    def test_stage_names_cover_the_new_diagnostics(self):
        """Every emitted white-layer bit must be named in the host stage table, in order."""
        stages = re.search(r"static constexpr const char \*stages\[\]=\{(.*?)\};", HOST, re.S).group(1)
        emitted = sorted(set(re.findall(r'"white-layer-[a-z-]+"', MINI)))
        self.assertTrue(emitted, "the fix must report what it did")
        for name in emitted:
            self.assertIn(name, stages, name)

if __name__ == "__main__":
    unittest.main()
