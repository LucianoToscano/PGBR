"""Compile the separate raid search engine and guard two search UIs."""
import subprocess
import tempfile
import unittest
from pathlib import Path
from test_control_java import java_tools

ROOT = Path(__file__).resolve().parents[1]

class RaidFeedFiltersTests(unittest.TestCase):
    def test_raid_filter_model(self):
        javac, java = java_tools()
        with tempfile.TemporaryDirectory(prefix="pgbr-raid-search-") as folder:
            sources=[ROOT/"src/dev/pgrazor/RaidFeedFilters.java",
                     ROOT/"test/RaidFeedFiltersTest.java"]
            compilation=subprocess.run(
                [str(javac),"-encoding","UTF-8","-d",folder,*map(str,sources)],
                capture_output=True,text=True,timeout=30)
            self.assertEqual(compilation.returncode,0,compilation.stdout+compilation.stderr)
            execution=subprocess.run([str(java),"-cp",folder,"dev.pgrazor.RaidFeedFiltersTest"],
                                      capture_output=True,text=True,timeout=30)
            self.assertEqual(execution.returncode,0,execution.stdout+execution.stderr)
            self.assertIn("PASS:",execution.stdout)

    def test_both_searches_with_attribution_and_teleport_confirmation(self):
        dialog=(ROOT/"src/dev/pgrazor/LiveFeedDialog.java").read_text(encoding="utf-8")
        chooser=(ROOT/"src/dev/pgrazor/FeedChooser.java").read_text(encoding="utf-8")
        settings=(ROOT/"src/dev/pgrazor/SettingsPanel.java").read_text(encoding="utf-8")
        self.assertIn("showRaidSearch(Activity activity)",dialog)
        self.assertIn("showPokemonSearch(Activity activity)",dialog)
        self.assertIn("LiveFeedDialog.showRaidSearch(activity)",chooser)
        self.assertIn("LiveFeedDialog.showPokemonSearch(activity)",chooser)
        self.assertIn("LiveFeedDialog.showRaidSearch(activity)",settings)
        self.assertIn("LiveFeedDialog.showPokemonSearch(activity)",settings)
        self.assertIn("raidFilters.matches(",dialog)
        self.assertIn("if (pokemonSearchMode) confirmPokemonTeleport(",dialog)
        self.assertIn("else if (raidSearchMode) confirmRaidTeleport(",dialog)
        self.assertIn("TeleportDialog.confirm(activity, e.lat, e.lon,",dialog)
        self.assertIn("Control.teleport(lat, lon);",dialog)
        self.assertIn("Fonte dos resultados: LivePokeMap",dialog)
        self.assertIn("Não utiliza o servidor privado do PGSharp",dialog)
        self.assertIn("raid_search_boss_v1",dialog)
        self.assertIn("raid_search_hatched_v1",dialog)
        self.assertIn("e.kind != LiveFeedData.Kind.RAID",dialog)

if __name__=="__main__":
    unittest.main()
