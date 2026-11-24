"""Non-destructive integration contracts for the two independent Snipe slots.

These are structural checks, not Android execution tests; the filter behaviour is
verified separately by SnipeFiltersTest.java on a JVM against Android's org.json.
"""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "src" / "dev" / "pgrazor"


class SnipeSlotsContractTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.menu = (SRC / "FeedChooser.java").read_text(encoding="utf-8")
        cls.slots = (SRC / "SnipeSlotsDialog.java").read_text(encoding="utf-8")
        cls.editor = (SRC / "SnipeFilterDialog.java").read_text(encoding="utf-8")
        cls.filters = (SRC / "SnipeFilters.java").read_text(encoding="utf-8")
        cls.hundo = (SRC / "HundoFeedData.java").read_text(encoding="utf-8")

    def test_previous_feed_choices_keep_their_indices(self):
        for previous_index in range(5):
            with self.subTest(index=previous_index):
                self.assertIn(f"which == {previous_index}", self.menu)
        for item in (
            "LiveFeedDialog.showPokemonSearch(activity)",
            "LiveFeedDialog.showRaidSearch(activity)",
            "LiveFeedDialog.show(activity)",
            "PogoFeedDialog.show(activity)",
            "HundoFeedDialog.show(activity)",
        ):
            with self.subTest(item=item):
                self.assertIn(item, self.menu)
        self.assertIn("which == 5) SnipeSlotsDialog.show(activity)", self.menu)
        self.assertIn("SnipeSlotsDialog.show(activity)", self.menu)

    def test_two_slots_stay_independent(self):
        self.assertIn('SLOT_ONE = "sniper1"', self.filters)
        self.assertIn('SLOT_TWO = "sniper2"', self.filters)
        self.assertIn("SnipeFilters.SLOT_ONE", self.slots)
        self.assertIn("SnipeFilters.SLOT_TWO", self.slots)
        # Each slot writes only its own key and loads only its own key.
        self.assertIn('prefs.edit().putString(slot, parsed.toJson().toString()).apply()', self.slots)
        self.assertIn('SnipeFilterDialog.load(prefs, SnipeFilters.SLOT_ONE)', self.slots)
        self.assertIn('SnipeFilterDialog.load(prefs, SnipeFilters.SLOT_TWO)', self.slots)
        self.assertNotIn("putString(SnipeFilters.SLOT_ONE", self.slots)
        self.assertNotIn("putString(SnipeFilters.SLOT_TWO", self.slots)

    def test_no_new_backend_and_no_automation(self):
        # One source only: the feed the manual 100% IV reader already uses.
        self.assertIn("ShundoSniper.FEED_URL", self.slots)
        self.assertNotIn("pgsharp.com", self.slots + self.editor + self.filters)
        self.assertNotIn("new URL(\"https://", self.slots)
        self.assertNotIn("scheduleAtFixedRate", self.slots)
        self.assertNotIn("scheduleWithFixedDelay", self.slots)
        for automated in ("Control.catch", "ShundoSniper.start(", "Control.autoCatch"):
            with self.subTest(automated=automated):
                self.assertNotIn(automated, self.slots)
        self.assertIn("REQUEST_GAP_MS = 30_000L", self.slots)
        self.assertIn("update.setOnClickListener(v -> refresh());", self.slots)
        # Exactly one automatic request exists: on open, and the countdown tick makes none.
        self.assertIn("refresh(true);                   // The one request this panel makes on its own.",
                      self.slots)
        self.assertEqual(self.slots.count("fetchOnce()"), 2,
                         "fetchOnce must be declared once and called once")
        self.assertIn("Local countdown only; never another request", self.slots)
        self.assertIn("main.postDelayed(expiryTick, 10_000L);", self.slots)

    def test_explicit_confirmation_and_runtime_gate(self):
        self.assertIn("TeleportDialog.confirm(", self.slots)
        self.assertIn("Control.canEnable()", self.slots)
        self.assertIn("Control.teleport(lat, lon)", self.slots)
        self.assertLess(self.slots.find("TeleportDialog.confirm("),
                        self.slots.find("Control.teleport(lat, lon)"))
        self.assertIn("CoordinateClipboard.copy(", self.slots)
        self.assertIn("DialogLifecycle.bind(activity, dialog, this::close)", self.slots)
        self.assertIn("worker.shutdownNow()", self.slots)
        self.assertIn("main.removeCallbacksAndMessages(null)", self.slots)
        self.assertIn("Local countdown only; never another request", self.slots)

    def test_fails_closed_and_never_invents_values(self):
        for guard in ("MAX_SNAPSHOT_AGE_MS", "expired(", "IV_UNKNOWN", "Double.isFinite",
                      "throw new JSONException", "http.setInstanceFollowRedirects(false)",
                      "http.disconnect()"):
            with self.subTest(guard=guard):
                self.assertIn(guard, self.slots + self.filters + self.hundo)
        self.assertIn("return false;", self.filters)
        self.assertIn("HundoFeedData.FEED_CLAIMED_IV", self.filters)
        self.assertIn("FEED_CLAIMED_IV = 100", self.hundo)

    def test_corrupt_or_foreign_state_never_leaks_between_slots(self):
        self.assertIn("SnipeFilters.containsAnyKey(json)", self.editor)
        self.assertIn("catch (JSONException corrupt)", self.editor)
        self.assertIn("return SnipeFilters.defaults();", self.editor)
        self.assertIn("getSharedPreferences(\"pgrazor-snipe-filters\"", self.editor)
        # Invalid editor text keeps the dialog open instead of dismissing with bad state.
        self.assertIn("setPositiveButton(\"Aplicar\", null)", self.editor)
        self.assertIn("dialog.dismiss();", self.editor)

    def test_manual_reader_contract_is_still_satisfied(self):
        # test_hundo_manual_ui_contract.py reads HundoFeedDialog.java and FeedChooser.java.
        self.assertIn("HundoFeedDialog.show(activity)", self.menu)
        self.assertIn("REQUEST_GAP_MS = 30_000L", self.menu + self.slots)


if __name__ == "__main__":
    unittest.main()
