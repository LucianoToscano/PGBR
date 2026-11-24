"""Contract checks for the opt-in, read-only automatic visual-route anchors.

No device is simulated here: these tests only prove that the native feature, transport schema, mailbox and
Java source agree on bounded indices and that the prototype stays behind explicit gates.
"""
import json
import re
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


class AutoAnchorSchemaTest(unittest.TestCase):
    def test_schema_indices_are_append_only(self):
        js = (ROOT / "runtime/native-transport.js").read_text(encoding="utf-8")
        schema = json.loads(re.search(r"const SCHEMA=(\{.*?\});", js, re.S).group(1))
        self.assertEqual(len(schema["BOOL_READERS"]), 40)
        self.assertEqual(schema["BOOL_READERS"][39], "routeAnchorsAllowed")
        self.assertEqual(len(schema["ACTIONS"]), 60)
        self.assertEqual(schema["ACTIONS"][59], "setMapAnchorSnapshot:String:void")
        # Every original opcode remains in place: only the two new entries were appended.
        self.assertEqual(schema["ACTIONS"][41], "setMapPoiSnapshot:String:void")
        self.assertEqual(schema["ACTIONS"][58], "vgpResult:String,boolean:void")

        mailbox = (ROOT / "src/dev/pgrazor/RuntimeMailbox.java").read_text(encoding="utf-8")
        self.assertIn('"routeAnchorsAllowed::boolean",', mailbox)
        self.assertIn('"setMapAnchorSnapshot:String:void"', mailbox)
        translated = (ROOT / "runtime/translated.js").read_text(encoding="utf-8")
        self.assertIn("vgpMapVisible routeAnchorsAllowed vgpOptions", translated)
        self.assertIn("vgpResult setMapAnchorSnapshot", translated)

    def test_native_reader_is_gated_and_owned(self):
        host = (ROOT / "native/encounter_host.cpp").read_text(encoding="utf-8")
        self.assertIn("bval(39) > 0", host)
        self.assertIn("if (!g_route_anchors_allowed || !g_map_anchor || !lease_ms) return;", host)
        self.assertIn("transport_emit_op(59, nullptr, 0, args, 1);", host)
        self.assertIn("if (!fresh) g_route_anchors_allowed = false;", host)
        self.assertIn("install_map_anchor_leaf();", host)
        control = (ROOT / "src/dev/pgrazor/Control.java").read_text(encoding="utf-8")
        for token in ["public static boolean routeAnchorsAllowed()", "public static void setMapAnchorSnapshot(",
                      "public static long mapAnchorReceivedAt()", "engine.activeVisualRouteToken()>0"]:
            self.assertIn(token, control)
        # Existing official-map gate is unchanged: the anchor gate is a separate reader.
        self.assertIn("public static boolean mapPoiAllowed(){return (mapPoiOpen||autoWalkPlanning)&&hostLive();}", control)

    def test_native_module_is_bounded_and_read_only(self):
        header = (ROOT / "native/map_anchor.h").read_text(encoding="utf-8")
        for token in ["PGR_MA_FEATURE_ID       = 72", "PGR_MA_MAX_ANCHORS      = 128",
                      "static_assert(sizeof(PgrMapAnchorConfig) == 288", "const void *pgrazor_map_anchor_api"]:
            self.assertIn(token, header)
        source = (ROOT / "native/map_anchor.cpp").read_text(encoding="utf-8")
        for forbidden in ["DobbyHook", "runtime_invoke(nullptr", "delete ", "free("]:
            self.assertNotIn(forbidden, source)
        for token in ["owner_ready(request->tid)", "busy.compare_exchange_strong", "within(deadline, &err)",
                      "projected.z <= 0.0f", "PGR_MA_FLAG_VISIBLE"]:
            self.assertIn(token, source)
        self.assertIn("  map_anchor.cpp\n", (ROOT / "native/CMakeLists.txt").read_text(encoding="utf-8"))

    def test_java_visual_source_is_fail_closed(self):
        source = (ROOT / "src/dev/pgrazor/RouteAutoAnchorSource.java").read_text(encoding="utf-8")
        for token in ["RouteTraceOverlay.ProjectedFrameSource", "Control.mapAnchorReceivedAt()",
                      "MAX_ANCHOR_AGE_MS", "if (projection == null) return null;",
                      "Control.engine.activeVisualRouteToken()", "RouteScreenFrameBuilder.build(",
                      "routeVisualSnapshot(cachedToken)", "if (points == null || points.length < 2) return null;"]:
            self.assertIn(token, source)
        for forbidden in ["PixelCopy", "NativeCore", "inject", ".walkRoute(", ".teleport("]:
            self.assertNotIn(forbidden, source)
        parser = (ROOT / "src/dev/pgrazor/RouteAnchorSnapshot.java").read_text(encoding="utf-8")
        for token in ["static RouteAnchorSnapshot parse", "new RouteGroundProjection(six, width, height)",
                      "if (n < 6) return null;", "catch (IllegalArgumentException invalid)"]:
            self.assertIn(token, parser)
        pad = (ROOT / "src/dev/pgrazor/Pad.java").read_text(encoding="utf-8")
        self.assertIn('prefs.getBoolean("routeAutoProjectionEnabled",false)', pad)
        dialog = (ROOT / "src/dev/pgrazor/WorldMapDialog.java").read_text(encoding="utf-8")
        self.assertIn('putBoolean("routeAutoProjectionEnabled",on)', dialog)


if __name__ == "__main__":
    unittest.main()
