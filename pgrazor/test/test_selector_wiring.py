"""Static UI/wiring checks for PGBR Selector; network is never mocked as PGSharp."""
from pathlib import Path
import unittest

ROOT=Path(__file__).resolve().parents[1]

class PokemonSelectorWiringTest(unittest.TestCase):
    def test_quick_menu_and_real_feed_flow(self):
        menu=(ROOT/"src/dev/pgrazor/QuickMenu.java").read_text(encoding="utf-8")
        dialog=(ROOT/"src/dev/pgrazor/PokemonSelectorDialog.java").read_text(encoding="utf-8")
        provider=(ROOT/"src/dev/pgrazor/IFlowGoFeedClient.java").read_text(encoding="utf-8")
        policy=(ROOT/"src/dev/pgrazor/PokemonSelectorPolicy.java").read_text(encoding="utf-8")
        self.assertIn('"Seletor de Pokémon", true',menu)
        self.assertIn('PokemonSelectorDialog.show(activity)',menu)
        self.assertIn('8192',menu)
        self.assertIn('listener::onFeeds',menu,"Advanced legacy feed must remain available")
        self.assertIn('provider.fetchGlobal(dex)',dialog)
        self.assertIn('PokemonSelectorPolicy.validResult(',dialog)
        self.assertIn('TeleportDialog.confirm(',dialog)
        self.assertIn('Control.teleport(lat,lon)',dialog)
        self.assertIn('Control.canEnable()',dialog)
        self.assertIn('DialogLifecycle.bind(',dialog)
        self.assertIn('provider.cancel()',dialog)
        self.assertIn('A API direta de PGSharp/Pokémod ainda não está conectada',dialog)
        self.assertIn('pokemon-search?page=',provider)
        self.assertIn('page=" + page',provider)
        self.assertIn('IFlowGoFeedData.hasNextPage',provider)
        self.assertIn('IFlowGoRequestPolicy.globalSpeciesQuery(dexId)',provider)
        request_policy=(ROOT/'src/dev/pgrazor/IFlowGoRequestPolicy.java').read_text(encoding='utf-8')
        self.assertIn('&pokemon_id=',request_policy)
        self.assertIn('entry.expiresAt > now',policy)
        self.assertIn('entry.dexId == selectedDex',policy)
        self.assertIn('CooldownState.valid',policy)
        self.assertNotIn('tens.pgsharp.com',dialog)
        self.assertNotIn('discovery.pokemod.dev',dialog)

if __name__=="__main__":
    unittest.main()
