import dev.pgrazor.BerryPriority;

/**
 * Berry labels of the item screens, without Android.
 *
 * <p>Two screens showed berry names in two different places: the priority picker and the encounter
 * berry button. Both used an English label that the dictionary translated by exact match. That
 * works for the five plain labels and stops working the moment the same name is composed with a
 * count, which is what the picker actually renders. The names now come from {@link
 * BerryPriority#displayName(int)}, so this test pins them against the IDs.
 */
public class ItemBerryLabelTest {
    private static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError(what);
    }
    public static void main(String[] args) {
        // The five IDs the app accepts, each with the Portuguese name used everywhere else.
        int[] ids = {701, 703, 705, 706, 707};
        // Order matters: 703 is the banana sprite (Nanab) and 705 the pineapple sprite (Pinap).
        String[] names = {"Fruta Frambo", "Fruta Caxí", "Fruta Abacaxi", "Fruta Frambo Dourada", "Fruta Abacaxi Prateada"};
        for (int i = 0; i < ids.length; i++) {
            check(BerryPriority.validId(ids[i]), "id should be valid: " + ids[i]);
            check(names[i].equals(BerryPriority.displayName(ids[i])),
                  "name of " + ids[i] + " is '" + BerryPriority.displayName(ids[i]) + "', expected '" + names[i] + "'");
        }
        // Every name must be Portuguese (no English berry word leaks to the screen).
        for (int id : ids) {
            String name = BerryPriority.displayName(id);
            check(name.startsWith("Fruta "), "name must start with Fruta: " + name);
            check(name.indexOf("Razz") < 0 && name.indexOf("Nanab") < 0 && name.indexOf("Pinap") < 0,
                  "English berry word leaked: " + name);
        }
        // The two "golden/silver" variants must not be swapped with the plain berry of the same colour.
        check(!BerryPriority.displayName(701).equals(BerryPriority.displayName(706)), "golden Razz must differ from Razz");
        check(!BerryPriority.displayName(703).equals(BerryPriority.displayName(707)), "silver Pinap must differ from Pinap");
        check(BerryPriority.displayName(706).contains("Dourada"), "706 is the golden one");
        check(BerryPriority.displayName(707).contains("Prateada"), "707 is the silver one");
        // Unknown and "off" ids show nothing, so a caller cannot print a wrong berry name.
        for (int unknown : new int[]{0, 702, 704, 708, -1, 9999}) {
            check("".equals(BerryPriority.displayName(unknown)), "unknown berry must have no name: " + unknown);
        }
        System.out.println("PASS: berry labels by ID");
    }
}
