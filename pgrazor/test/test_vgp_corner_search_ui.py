"""CI contracts for full-width VGP and safe direct Pokémon teleport UI."""
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


class VgpCornerSearchUiTests(unittest.TestCase):
    def test_corner_position_uses_same_viewport_as_vgp_drag(self):
        pad = (ROOT / "src/dev/pgrazor/Pad.java").read_text(encoding="utf-8")
        drag = (ROOT / "src/dev/pgrazor/OverlayDrag.java").read_text(encoding="utf-8")
        viewport = (ROOT / "src/dev/pgrazor/UiViewport.java").read_text(encoding="utf-8")
        self.assertIn("Rect vgpSafe=UiViewport.vgpBounds(parent);", pad)
        self.assertIn("Rect to=view==vgpToggle?vgpSafe:safe;", pad)
        self.assertIn("Rect from=view==vgpToggle?previousVgpSafe:previousSafe;", pad)
        self.assertIn('"vgp".equals(key)?UiViewport.vgpBounds(parent)', drag)
        self.assertIn("safe.right=parent.getWidth();", viewport)
        self.assertIn("UiViewportPolicy.edgeAlignedCenter(", pad)

    def test_pokemon_search_results_are_from_real_feed_with_confirmation(self):
        chooser = (ROOT / "src/dev/pgrazor/FeedChooser.java").read_text(encoding="utf-8")
        settings = (ROOT / "src/dev/pgrazor/SettingsPanel.java").read_text(encoding="utf-8")
        dialog = (ROOT / "src/dev/pgrazor/LiveFeedDialog.java").read_text(encoding="utf-8")
        icons = (ROOT / "src/dev/pgrazor/LiveFeedIcons.java").read_text(encoding="utf-8")
        self.assertIn("LiveFeedDialog.showPokemonSearch(activity)", chooser)
        self.assertIn("LiveFeedDialog.showPokemonSearch(activity)", settings)
        self.assertIn("PokemonFeedSearch.matches(", dialog)
        self.assertIn("TeleportDialog.confirm(activity, e.lat, e.lon,", dialog)
        self.assertIn("Control.teleport(lat, lon)", dialog)
        self.assertIn("icons.iconFor(e, now)", dialog)
        self.assertIn('"pgrazor/feed-icons/pokemon/" + dexId + ".png"', icons)
        self.assertIn("if (e.expired(System.currentTimeMillis()))", dialog)
        self.assertIn("if (!Control.canEnable())", dialog)
        self.assertIn("PokeListFeedClient pokeList = new PokeListFeedClient()", dialog)
        self.assertIn('segButton("PokeList")', dialog)
        self.assertIn('Fonte: PokeList / Moonani', dialog)
        self.assertIn('segButton("LivePokeMap")', dialog)
        self.assertIn('if (provider == 1) return "PokeList / Moonani";', dialog)
        self.assertIn('if (provider == 2) return "iFlowGo";', dialog)
        self.assertIn("PokeListFeedClient", dialog)
        self.assertIn("entries.clear(); visible.clear(); distCache.clear();", dialog)
        self.assertIn("provider == 1 && lastFetchStartedElapsed > 0", dialog)
        self.assertIn("PokeList / Moonani", (ROOT / "src/dev/pgrazor/PokeListFeedData.java").read_text(encoding="utf-8"))
        self.assertNotIn('"PGSharp" + e.name', dialog)


if __name__ == "__main__":
    unittest.main()
