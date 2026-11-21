"""Check that the two independent filters display real source data with proper credit."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]

class IFlowGoWiringTests(unittest.TestCase):
    def test_real_feed_wiring(self):
        source = (ROOT / "src/dev/pgrazor/LiveFeedDialog.java").read_text(encoding="utf-8")
        chooser = (ROOT / "src/dev/pgrazor/FeedChooser.java").read_text(encoding="utf-8")
        client = (ROOT / "src/dev/pgrazor/IFlowGoFeedClient.java").read_text(encoding="utf-8")
        parser = (ROOT / "src/dev/pgrazor/IFlowGoFeedData.java").read_text(encoding="utf-8")
        self.assertIn('showPokemonSearch(Activity activity)', source)
        self.assertIn('showRaidSearch(Activity activity)', source)
        self.assertIn('provider == 2', source)
        self.assertIn('iflowgo_pokemon_radius_v1', source)
        self.assertIn('iflowgo_raid_radius_v1', source)
        self.assertIn('provider == 2 ? 3 : 1', source)
        self.assertIn('prefs.getInt(radiusPreferenceKey()', source)
        self.assertIn('iFlowGo.fetch(lat, lon, radiusKm, raidSearchMode)', source)
        self.assertIn('iFlowGo.cancel()', source)
        self.assertIn('sourceName()', source)
        self.assertIn('"iFlowGo"', source)
        self.assertIn('PokeList / Moonani', source)
        self.assertIn('iflowgo.com/iflowgopokecoords/api/v1/nearby', client)
        self.assertIn('setInstanceFollowRedirects(false)', client)
        self.assertIn('Math.max(60000L', client)
        self.assertIn('raid_end_at', parser)
        self.assertIn('expires_at', parser)
        self.assertIn('IFlowGoFeedData.parse', client)
        self.assertIn('IFlowGoFeedData.parseGlobal', client)
        self.assertIn('iFlowGo.fetchGlobal()', source)
        self.assertIn('LiveFeedDialog.showPokemonSearch(activity)', chooser)
        self.assertIn('LiveFeedDialog.showRaidSearch(activity)', chooser)
        self.assertIn('TeleportDialog.confirm(activity, e.lat, e.lon,', source)

if __name__ == "__main__":
    unittest.main()
