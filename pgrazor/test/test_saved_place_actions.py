from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1] / "src/dev/pgrazor"

class SavedPlaceActionsTest(unittest.TestCase):
    def test_rename_preserves_identity_and_metadata(self):
        source = (ROOT / "SavedPlaces.java").read_text(encoding="utf-8")
        method = source.split("public boolean rename(", 1)[1].split("private static String nameKey", 1)[0]
        self.assertIn("new Entry(current.id,name,current.latitude,current.longitude,current.category,current.createdAtMs)", method)
        self.assertIn("if(snapshot.damaged)throw", method)
        self.assertIn("!other.id.equals(id)&&nameKey(other.name).equals(nameKey(updated.name))", method)
        self.assertIn("entries.set(i,updated);write(entries)", method)

    def test_quick_actions_preserve_confirmation_and_id(self):
        source = (ROOT / "QuickLocationDialogs.java").read_text(encoding="utf-8")
        self.assertIn("showPlaceActions(activity,target,sorted.get(which))", source)
        self.assertIn("rename(place.id,requested)", source)
        self.assertIn("remove(place.id)", source)
        self.assertIn('setTitle("Excluir local?")', source)
        self.assertIn("TeleportDialog.confirm(activity,place.latitude,place.longitude,target::teleport)", source)
