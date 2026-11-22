"""Static contract tests for the native Sniper panel and its search engine.

These are source-level assertions: they cannot prove a provider answers, and they are written so they
fail loudly if someone later removes an honesty guard (unknown != zero, no 100% claim without proof,
teleport only after confirmation, "source empty" never reported as "everything filtered").
"""
from pathlib import Path
import re
import unittest

ROOT = Path(__file__).resolve().parents[1] / 'src/dev/pgrazor'
TESTS = Path(__file__).resolve().parents[1] / 'test'


def read(name):
    return (ROOT / name).read_text(encoding='utf-8')


class SniperEngineContractTest(unittest.TestCase):
    def test_contract_keeps_unknown_apart_from_zero(self):
        text = read('SpawnContract.java')
        self.assertIn('public static final int UNKNOWN = -1;', text)
        self.assertIn('public static final double UNKNOWN_LEVEL = Double.NaN;', text)
        # 0,0 must never be accepted as a real coordinate.
        self.assertIn('Math.abs(lat) < 1e-7 && Math.abs(lon) < 1e-7', text)

    def test_perfect_requires_proof(self):
        text = read('SpawnContract.java')
        self.assertRegex(text, r'public boolean perfect\(\)\s*\{\s*return ivAttack == 15')
        operator = read('SpawnFilters.java')
        self.assertIn('!s.perfect() && !s.providerSaysPerfect()', operator)

    def test_unknown_never_satisfies_a_bound(self):
        text = read('SpawnFilters.java')
        self.assertIn('if (pct == SpawnContract.UNKNOWN) return false;', text)
        self.assertIn('if (!Double.isFinite(s.level)) return false;', text)
        self.assertIn('if (value == SpawnContract.UNKNOWN) return false;', text)

    def test_declared_but_unverified_groups_are_refused(self):
        text = read('SpawnFilters.java')
        self.assertIn('Filtro de regionais indisponível', text)
        self.assertIn('Filtro de raros indisponível', text)
        self.assertIn('Filtro de evento indisponível', text)

    def test_engine_counts_are_separate(self):
        text = read('SpawnSearch.java')
        for field in ('received', 'droppedByFilter', 'duplicates', 'expired'):
            self.assertIn('public final int %s;' % field, text)
        self.assertIn('SOURCE_EMPTY', text)
        self.assertIn('ALL_FILTERED_OUT', text)
        self.assertIn('COORDINATES_BLOCKED', text)
        self.assertIn('RATE_LIMITED', text)
        self.assertIn('ACCESS_DENIED', text)

    def test_first_page_is_never_called_worldwide(self):
        text = read('SpawnSearch.java')
        self.assertIn('FIRST_PAGE', text)
        self.assertIn('não é uma busca mundial completa', text)
        self.assertIn('Isso não significa ausência mundial', text)

    def test_retry_after_is_preserved(self):
        text = read('SpawnSearch.java')
        self.assertIn('feed.retryAfterMs', text)

    def test_sources_are_registered_with_honest_status(self):
        text = read('SpawnSourceRegistry.java')
        self.assertIn('REVEAL_REQUIRED', text)
        self.assertIn('NOT_INTEGRATED', text)
        for vendor in ('pgsharp', 'ipogo', 'pokemod'):
            self.assertIn('"%s"' % vendor, text)
        self.assertIn('não é API pública', text)

    def test_pvp_local_rank_is_labelled(self):
        panel = read('SniperPanel.java')
        self.assertIn('(calculado)', panel)
        self.assertIn('calculado localmente', panel)

    def test_shiny_possible_is_not_confirmed(self):
        panel = read('SniperPanel.java')
        self.assertIn('shiny confirmado', panel)
        self.assertIn('shiny possível', panel)
        self.assertIn('Shiny: possível (não confirmado)', panel)


class SniperPanelContractTest(unittest.TestCase):
    def test_panel_is_reachable_from_pokemon_search(self):
        text = read('FeedChooser.java')
        self.assertIn('Sniper 1 · busca rápida nativa', text)
        self.assertIn('Sniper 2 · filtros independentes', text)
        self.assertIn('SniperPanel.show(activity, SpawnProfiles.SLOT_ONE)', text)
        self.assertIn('SniperPanel.show(activity, SpawnProfiles.SLOT_TWO)', text)

    def test_slots_never_share_preferences(self):
        text = read('SpawnProfiles.java')
        self.assertIn('public static final String SLOT_ONE = "slot1";', text)
        self.assertIn('public static final String SLOT_TWO = "slot2";', text)
        self.assertIn('PREF_FILE = "pgrazor-snipe-v2"', text)
        panel = read('SniperPanel.java')
        self.assertIn('prefs.getString(slot, null)', panel)
        self.assertIn('prefs.edit().putString(slot,', panel)

    def test_teleport_needs_confirmation_and_revalidates(self):
        panel = read('SniperPanel.java')
        self.assertIn('TeleportDialog.confirm(activity, spawn.latitude, spawn.longitude', panel)
        self.assertIn('Control.canEnable()', panel)
        self.assertIn('Control.teleport(lat, lon)', panel)
        # the confirmation callback must re-check expiry/freshness and coordinate validity
        self.assertRegex(panel, r'spawn\.expired\(System\.currentTimeMillis\(\)\) \|\| isStale\(\)')

    def test_no_request_per_keystroke(self):
        panel = read('SniperPanel.java')
        self.assertIn('DEBOUNCE_MS', panel)
        self.assertNotRegex(panel, r'afterTextChanged\(Editable s\) \{\s*search\(')

    def test_stale_queries_are_cancelled_and_discarded(self):
        panel = read('SniperPanel.java')
        self.assertIn('generation.incrementAndGet()', panel)
        self.assertIn('if (source != null) source.cancel();', panel)
        self.assertIn('token != generation.get()', panel)

    def test_rate_limit_blocks_the_source(self):
        panel = read('SniperPanel.java')
        self.assertIn('blockedUntilElapsed', panel)
        self.assertIn('MIN_REQUEST_GAP_MS', panel)
        self.assertIn('SpawnSearch.State.RATE_LIMITED', panel)

    def test_coordinates_blocked_blocks_teleport(self):
        panel = read('SniperPanel.java')
        self.assertIn('Coordenadas inválidas: teleporte bloqueado.', panel)
        self.assertIn('Esta linha não tem coordenadas reveladas.', panel)

    def test_filter_dialog_shows_source_capabilities(self):
        text = read('SnipeAdvancedFilterDialog.java')
        self.assertIn('Entrega:', text)
        self.assertIn('setEnabled(false)', text)

    def test_filters_run_server_side_only_when_single_species(self):
        filters = read('SpawnFilters.java')
        self.assertIn('return species.size() == 1 ? species.get(0) : -1;', filters)
        connectors = read('SpawnConnectors.java')
        self.assertIn('client.fetchGlobal(request.species)', connectors)

    def test_hundo_source_is_fetched_not_faked(self):
        text = read('SpawnConnectors.java')
        self.assertIn('ShundoSniper.FEED_URL', text)
        self.assertIn('HundoFeedData.parse(', text)

    def test_panel_never_claims_teleport_success(self):
        panel = read('SniperPanel.java')
        self.assertIn('Teleporte solicitado; confirme a posição no jogo.', panel)
        self.assertNotRegex(panel, r'[Tt]eleport(?:e|ado) com sucesso')


class SniperEngineFixturesTest(unittest.TestCase):
    """The JVM test file must exist, be registered, and stay clearly labelled as test data."""

    def test_jvm_suite_exists_and_is_registered(self):
        harness = (Path(__file__).resolve().parents[1] / 'test/test_live_feed_java.py').read_text(encoding='utf-8')
        self.assertIn('SnipeEngineTest', harness)
        for source in ('SpawnContract.java', 'SpawnSearch.java', 'SpawnFilters.java',
                       'SpawnProfiles.java', 'SpawnSourceRegistry.java', 'SpeciesGroups.java'):
            self.assertIn(source, harness)

    def test_fixtures_are_labelled(self):
        suite = (TESTS / 'SnipeEngineTest.java').read_text(encoding='utf-8')
        self.assertIn('test data', suite)
        self.assertIn('Nenhum dado', suite.replace('None of it', 'Nenhum dado'))
        self.assertNotIn('http://', suite)
        self.assertNotIn('https://', suite)

    def test_suite_covers_required_cases(self):
        suite = (TESTS / 'SnipeEngineTest.java').read_text(encoding='utf-8')
        for needle in ('unknownsStayUnknown', 'hundredPercentNeedsProof', 'zeroIsNotUnknown',
                       'invalidCoordinatesRejected', 'expiryAndDedupe', 'orderingPutsUnknownLast',
                       'ivAndComponentFilters', 'cpLevelGenderFormFilters', 'pvpAndShinyFilters',
                       'distanceAndCoordinatesFilters', 'engineStates', 'engineFailures',
                       'profilesAreIndependent', 'groupsAndGuards'):
            self.assertIn(needle, suite)


if __name__ == '__main__':
    unittest.main()
