"""Prevent polling after vendor access refusal in the combined Pokémon search."""
import unittest
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]

class PokeListAccessGuardTests(unittest.TestCase):
    def test_401_403_are_not_interpreted_as_a_rate_limit(self):
        client=(ROOT/"src/dev/pgrazor/PokeListFeedClient.java").read_text(encoding="utf-8")
        ui=(ROOT/"src/dev/pgrazor/LiveFeedDialog.java").read_text(encoding="utf-8")
        self.assertIn("if (status == 401 || status == 403)", client)
        self.assertIn("verifique", client.lower())
        self.assertIn("pokeListAccessDenied = true;", ui)
        self.assertIn("provider == 1 && pokeListAccessDenied", ui)
        self.assertIn("if (provider == 1) {\n                    pokeListAccessDenied = false;", ui)
        self.assertIn("if (!fetchInFlight && !(provider == 1 && pokeListAccessDenied)", ui)
        self.assertIn("Não há resultados confirmados", ui)
        self.assertIn("entries.clear(); visible.clear(); distCache.clear();", ui)
        self.assertNotIn("backoffUntilElapsed = SystemClock.elapsedRealtime() + 300000L", ui)
        self.assertIn('prefs.getInt("pokemon_provider_v1", 0)', ui)
        self.assertIn("if (closed || showingLogin || g != generation) return;", ui)
        self.assertIn("if (closed || g != generation) return;", ui)
        self.assertIn("client.cancel(); pokeList.cancel();", ui)
        self.assertIn("fetchInFlight = false; // previous filter request is obsolete", ui)
        # Refresh after 403 must not clear a separate 429 rate limit timer.
        self.assertNotIn("pokeListAccessDenied = false;\n                    backoffUntilElapsed = 0;", ui)

    def test_probe_logs_aggregates_not_private_spawn_data(self):
        probe=(ROOT.parent/"tools/pokelist_live_probe.py").read_text(encoding="utf-8")
        self.assertIn("MAX_BYTES = 512_000",probe)
        self.assertIn("MAX_RESULTS", (ROOT/"src/dev/pgrazor/PokeListFeedData.java").read_text(encoding="utf-8"))
        self.assertIn("RESULT=not_confirmed HTTP_STATUS=",probe)
        self.assertIn("SCHEMA_RESULT=",probe)
        self.assertNotIn("print(raw",probe)
        self.assertNotIn("print(payload",probe)

if __name__=="__main__":
    unittest.main()
