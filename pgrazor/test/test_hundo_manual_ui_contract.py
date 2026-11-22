"""Non-destructive integration contracts for the optional manual 100% IV reader.

These structural checks are not Android execution tests. The JSON parser's
behaviour is verified separately by HundoFeedDataTest.java on the JVM.
"""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "src" / "dev" / "pgrazor"


class HundoManualUiContractTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.menu = (SRC / "FeedChooser.java").read_text(encoding="utf-8")
        cls.ui = (SRC / "HundoFeedDialog.java").read_text(encoding="utf-8")
        cls.data = (SRC / "HundoFeedData.java").read_text(encoding="utf-8")

    def test_prior_feed_choices_remain(self):
        for item in (
            "LiveFeedDialog.showPokemonSearch(activity)",
            "LiveFeedDialog.showRaidSearch(activity)",
            "LiveFeedDialog.show(activity)",
            "PogoFeedDialog.show(activity)",
        ):
            with self.subTest(item=item):
                self.assertIn(item, self.menu)
        self.assertIn("HundoFeedDialog.show(activity)", self.menu)
        for previous_index in range(4):
            self.assertIn(f"which == {previous_index}", self.menu)
        self.assertIn("which == 4", self.menu)

    def test_no_new_backend_or_automated_game_actions(self):
        self.assertIn("ShundoSniper.FEED_URL", self.ui)
        self.assertNotIn("new URL(\"https://", self.ui)
        self.assertIn("refresh();", self.ui)
        self.assertIn("REQUEST_GAP_MS = 30_000L", self.ui)
        self.assertNotIn("scheduleAtFixedRate", self.ui)
        self.assertNotIn("scheduleWithFixedDelay", self.ui)
        self.assertNotIn("Control.catch", self.ui)
        self.assertNotIn("ShundoSniper.start(", self.ui)

    def test_explicit_confirmation_and_runtime_gate(self):
        self.assertIn("TeleportDialog.confirm(", self.ui)
        self.assertIn("Control.canEnable()", self.ui)
        self.assertIn("Control.teleport(lat, lon)", self.ui)
        self.assertLess(self.ui.find("TeleportDialog.confirm("),
                        self.ui.find("Control.teleport(lat, lon)"))
        self.assertIn("CoordinateClipboard.copy(", self.ui)
        self.assertIn("DialogLifecycle.bind(activity, dialog, screen::close)", self.ui)

    def test_fails_closed_for_expired_or_malformed_results(self):
        for guard in ("MAX_SNAPSHOT_AGE_MS", "entry.expired(", "MAX_RESULTS",
                      "MAX_ROWS_INSPECTED", "seen.contains(id)", "Double.isFinite",
                      "despawn <= now", "throw new JSONException"):
            self.assertIn(guard, self.ui + self.data)

    def test_lifecycle_and_no_background_refresh(self):
        self.assertIn("worker.shutdownNow()", self.ui)
        self.assertIn("main.removeCallbacksAndMessages(null)", self.ui)
        self.assertIn("Refresh expiration/countdown without another HTTP request", self.ui)
        self.assertIn("http.setInstanceFollowRedirects(false)", self.ui)
        self.assertIn("http.disconnect()", self.ui)


if __name__ == "__main__":
    unittest.main()
