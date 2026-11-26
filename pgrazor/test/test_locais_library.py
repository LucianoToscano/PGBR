"""Static contracts for the "Locais" library of the World Map.

The library is the single surface for Favoritos, Pontos, Rotas and Eventos. Its saved-place tab
now delegates filtering/ordering to `SavedPlaceOrder` (shared with the quick Favoritos list), and
the Rotas tab gained an explicit, confirmed delete. These checks pin the boundaries that make that
safe: no second comparator, deletion off the UI thread, and no movement/teleport as a side effect.
"""
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / 'src' / 'dev' / 'pgrazor'


class LocaisLibraryTests(unittest.TestCase):
    def setUp(self):
        self.library = (SRC / 'WorldMapDialog.java').read_text(encoding='utf-8')
        self.quick = (SRC / 'QuickLocationDialogs.java').read_text(encoding='utf-8')
        self.order = (SRC / 'SavedPlaceOrder.java').read_text(encoding='utf-8')

    def test_saved_place_tab_uses_the_shared_policy(self):
        self.assertIn('SavedPlaceOrder.select(snapshot==null?null:snapshot.entries', self.library)
        self.assertIn('savedOrders[', self.library)
        # The old inline switch over orders[0] with per-case comparators is gone.
        self.assertNotIn('switch(orders[0])', self.library)
        self.assertNotIn('Comparator<SavedPlaces.Entry> byName', self.library)

    def test_both_lists_share_one_ordering_table(self):
        """Every ordering offered by the Locais menu must exist in the shared enum."""
        for name in ('NAME_ASC', 'NAME_DESC', 'NEWEST', 'DISTANCE', 'COOLDOWN', 'OLDEST',
                     'LOCAL_TIME_EARLY', 'LOCAL_TIME_LATE'):
            with self.subTest(ordering=name):
                self.assertIn('SavedPlaceOrder.Sort.' + name, self.library)
                self.assertIn(name, self.order)
        # The stored index is the contract: the menu labels are listed in the same order.
        self.assertIn('"Mais recentes","Distância","Cooldown estimado","Mais antigos"', self.library)

    def test_quick_list_uses_the_same_policy_and_keeps_its_menu_order(self):
        self.assertIn('SavedPlaceOrder.select(snap.entries', self.quick)
        sorts = self.quick.split('SavedPlaceOrder.Sort[] SORTS={', 1)[1].split('};', 1)[0]
        for name in ('NAME_ASC', 'NAME_DESC', 'LOCAL_TIME_EARLY', 'LOCAL_TIME_LATE', 'NEWEST', 'OLDEST',
                     'DISTANCE', 'COOLDOWN'):
            self.assertIn('SavedPlaceOrder.Sort.' + name, sorts)
        names = self.quick.split('SORT_NAMES={', 1)[1].split('};', 1)[0]
        self.assertEqual(sorts.count('SavedPlaceOrder.Sort.'), 8)
        self.assertEqual(names.count('"'), 16, 'eight quoted menu labels')
        self.assertIn('"Distância: mais perto primeiro","Cooldown: menor primeiro"', self.quick)

    def test_route_tab_offers_a_confirmed_delete_off_the_ui_thread(self):
        section = self.library.split('private void routeRow(', 1)[1].split('private void showSave(', 1)[0]
        self.assertIn('confirmRouteDelete(entry)', section)
        self.assertIn('"Delete saved route?"', section)
        self.assertIn('new GpxStore(activity).delete(entry.id)', section)
        self.assertIn('storage.execute(', section, 'deleting must not touch the disk on the UI thread')
        # Deleting a route must never start, stop or steer a walk.
        for forbidden in ('Control.engine', 'walkTo', 'startVisualWalk', 'teleport'):
            self.assertNotIn(forbidden, section)
        # A refusal keeps the list usable instead of dismissing it silently.
        self.assertIn('Toast.makeText(activity,failed,Toast.LENGTH_LONG)', section)

    def test_route_row_does_not_reuse_the_tab_icon(self):
        self.assertIn('new LibraryIcon(ui,5,', self.library)
        icon = self.library.split('private static final class LibraryIcon', 1)[1].split('PlaceUi', 1)[0]
        self.assertIn('else {', icon, 'the delete glyph is the documented fallback branch')

    def test_shared_policy_stays_pure(self):
        for forbidden in ('import android.', 'import org.json', 'Control.', 'HttpURLConnection'):
            self.assertNotIn(forbidden, self.order)
        self.assertIn('public static List<SavedPlaces.Entry> select(', self.order)

    def test_library_touch_targets_are_at_least_48dp(self):
        """Accessibility: every icon button in the library is a 48dp square touch target."""
        panel = self.library.split('private void showLocations(', 1)[1].split('private String locationDistance(', 1)[0]
        for line in panel.splitlines():
            if 'header.addView(' in line or 'top.addView(' in line:
                with self.subTest(line=line.strip()[:60]):
                    self.assertNotIn('dp(activity,40)', line)
                    self.assertIn('dp(activity,48)', line)

    def test_open_library_reopens_after_mutation(self):
        """Saving, renaming and deleting all reopen the library so the list matches the store."""
        for call in ('store().add(entry)', 'store().rename(entry.id,requested)'):
            with self.subTest(call=call):
                self.assertIn(call, self.library)
        self.assertGreaterEqual(self.library.count('openLibrary();'), 3)


if __name__ == '__main__':
    unittest.main()
