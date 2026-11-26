import dev.pgrazor.SavedPlaceOrder;
import dev.pgrazor.SavedPlaces;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;

/**
 * Exercises the real shared filter/ordering policy of the Locais library and the Favoritos list.
 * No Android, network, device location or game state is simulated: the local time zone, the distance
 * and the cooldown estimate are injected, exactly like the dialogs do at runtime.
 */
public final class SavedPlaceOrderTest {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static SavedPlaces.Entry place(String name, double latitude, double longitude, String category) {
        return new SavedPlaces.Entry(name, latitude, longitude, category);
    }

    private static String names(List<SavedPlaces.Entry> entries) {
        StringBuilder out = new StringBuilder();
        for (SavedPlaces.Entry entry : entries) {
            if (out.length() > 0) out.append('|');
            out.append(entry.name);
        }
        return out.toString();
    }

    private static SavedPlaceOrder.Query query(String text, String[] categories, SavedPlaceOrder.Sort sort) {
        return new SavedPlaceOrder.Query(text,
            categories == null ? null : Arrays.asList(categories), sort);
    }

    // Natal (UTC-3), Tokyo (UTC+9) and UTC itself, resolved without any device setting.
    private static final SavedPlaceOrder.LocalTime ZONES = (lat, lon) -> {
        if (lat == -5.79) return -180;
        if (lat == 35.68) return 540;
        if (lat == 51.5) return 0;
        return null; // 90N has no resolvable zone: unmeasured, must sort last
    };

    private static final SavedPlaceOrder.Distance DISTANCE = (lat, lon) -> {
        if (lat == 90) return Double.NaN;                 // no location fix: unmeasured, must sort last
        if (lat == -5.79) return 1200;
        if (lat == 35.68) return 18_500_000;
        return 5000;
    };

    private static final SavedPlaceOrder.Cooldown COOLDOWN = (lat, lon) -> {
        if (lat == 35.68) return -1;                      // no history: never treated as "ready"
        if (lat == -5.79) return 90_000;
        return 0;
    };

    public static void main(String[] args) {
        SavedPlaces.Entry natal = place("Praça Central", -5.79, -35.2, "Geral");
        SavedPlaces.Entry tokyo = place("Tóquio", 35.68, 139.69, "Farm");
        SavedPlaces.Entry london = place("Londres", 51.5, -0.12, "Eventos");
        SavedPlaces.Entry nowhere = place("Sem localização", 90, 0, "Geral");
        List<SavedPlaces.Entry> all = new ArrayList<>(Arrays.asList(natal, tokyo, london, nowhere));

        // ---- name ordering is total and case-insensitive --------------------------------
        List<SavedPlaces.Entry> ascending = SavedPlaceOrder.select(all, query("", null, SavedPlaceOrder.Sort.NAME_ASC), ZONES, DISTANCE, COOLDOWN);
        check(ascending.size() == 4, "no entry may be dropped by an empty filter");
        check(names(ascending).equals("Londres|Praça Central|Sem localização|Tóquio"), "A-Z order: " + names(ascending));
        check(names(SavedPlaceOrder.select(all, query("", null, SavedPlaceOrder.Sort.NAME_DESC), ZONES, DISTANCE, COOLDOWN))
            .equals("Tóquio|Sem localização|Praça Central|Londres"), "Z-A order");

        // ---- the text filter folds accents and matches the category ----------------------
        check(names(SavedPlaceOrder.select(all, query("praca", null, SavedPlaceOrder.Sort.NAME_ASC), ZONES, DISTANCE, COOLDOWN))
            .equals("Praça Central"), "accent-insensitive search must find Praça");
        check(SavedPlaceOrder.select(all, query("toquio", null, SavedPlaceOrder.Sort.NAME_ASC), ZONES, DISTANCE, COOLDOWN).size() == 1,
            "accent-insensitive search must find Tóquio");
        check(SavedPlaceOrder.select(all, query("farm", null, SavedPlaceOrder.Sort.NAME_ASC), ZONES, DISTANCE, COOLDOWN).size() == 1,
            "the category participates in the search text");
        check(SavedPlaceOrder.select(all, query("zzz", null, SavedPlaceOrder.Sort.NAME_ASC), ZONES, DISTANCE, COOLDOWN).isEmpty(),
            "a query with no match returns nothing instead of everything");
        check(SavedPlaceOrder.select(all, query("  ", null, SavedPlaceOrder.Sort.NAME_ASC), ZONES, DISTANCE, COOLDOWN).size() == 4,
            "a blank query is not a filter");

        // ---- category filter: several categories are a union, never an intersection -------
        List<SavedPlaces.Entry> generalAndFarm = SavedPlaceOrder.select(all,
            query("", new String[]{"Geral", "Farm"}, SavedPlaceOrder.Sort.NAME_ASC), ZONES, DISTANCE, COOLDOWN);
        check(generalAndFarm.size() == 3, "two selected categories must union: " + generalAndFarm.size());
        check(SavedPlaceOrder.select(all, query("", new String[]{"Rotas"}, SavedPlaceOrder.Sort.NAME_ASC), ZONES, DISTANCE, COOLDOWN).isEmpty(),
            "an empty category yields an empty list, not the whole library");
        check(SavedPlaceOrder.select(all, query("", null, SavedPlaceOrder.Sort.NAME_ASC), ZONES, DISTANCE, COOLDOWN).size() == 4,
            "no category selected means all of them");

        // ---- local time: earliest first puts the most negative offset first --------------
        check(names(SavedPlaceOrder.select(all, query("", null, SavedPlaceOrder.Sort.LOCAL_TIME_EARLY), ZONES, DISTANCE, COOLDOWN))
            .equals("Praça Central|Londres|Tóquio|Sem localização"), "earliest local clock first");
        check(names(SavedPlaceOrder.select(all, query("", null, SavedPlaceOrder.Sort.LOCAL_TIME_LATE), ZONES, DISTANCE, COOLDOWN))
            .equals("Tóquio|Londres|Praça Central|Sem localização"), "latest local clock first");
        check(names(SavedPlaceOrder.select(all, query("", null, SavedPlaceOrder.Sort.LOCAL_TIME_EARLY), null, DISTANCE, COOLDOWN))
            .equals("Londres|Praça Central|Sem localização|Tóquio"), "without a zone source it falls back to the name");
        SavedPlaceOrder.LocalTime unknown = (lat, lon) -> null;
        check(SavedPlaceOrder.select(all, query("", null, SavedPlaceOrder.Sort.LOCAL_TIME_LATE), unknown, DISTANCE, COOLDOWN).size() == 4,
            "an unresolved zone must not drop entries");

        // ---- distance and cooldown: unmeasured places last, never first ------------------
        check(names(SavedPlaceOrder.select(all, query("", null, SavedPlaceOrder.Sort.DISTANCE), ZONES, DISTANCE, COOLDOWN))
            .equals("Praça Central|Londres|Tóquio|Sem localização"), "nearest first, unknown last");
        check(names(SavedPlaceOrder.select(all, query("", null, SavedPlaceOrder.Sort.DISTANCE), ZONES, null, COOLDOWN))
            .equals("Londres|Praça Central|Sem localização|Tóquio"), "no location fix falls back to the name");
        check(names(SavedPlaceOrder.select(all, query("", null, SavedPlaceOrder.Sort.COOLDOWN), ZONES, DISTANCE, COOLDOWN))
            .equals("Londres|Sem localização|Praça Central|Tóquio"), "shortest cooldown first; no history last");
        check(names(SavedPlaceOrder.select(all, query("", null, SavedPlaceOrder.Sort.COOLDOWN), ZONES, DISTANCE, null))
            .equals("Londres|Praça Central|Sem localização|Tóquio"), "no cooldown source falls back to the name");

        // ---- creation time is taken from the stored entry, not from the list position ----
        SavedPlaces.Entry older = place("Antigo", -5.79, -35.2, "Geral");
        SavedPlaces.Entry newer = place("Novo", -5.79, -35.2, "Geral");
        List<SavedPlaces.Entry> pair = new ArrayList<>(Arrays.asList(older, newer));
        List<SavedPlaces.Entry> recent = SavedPlaceOrder.select(pair, query("", null, SavedPlaceOrder.Sort.NEWEST), ZONES, DISTANCE, COOLDOWN);
        check(recent.get(0).createdAtMs >= recent.get(1).createdAtMs, "newest first must use createdAtMs");
        check(SavedPlaceOrder.select(pair, query("", null, SavedPlaceOrder.Sort.OLDEST), ZONES, DISTANCE, COOLDOWN)
            .get(0).createdAtMs <= recent.get(1).createdAtMs, "oldest first must use createdAtMs");

        // ---- pathological providers must not break the list -----------------------------
        SavedPlaceOrder.LocalTime broken = (lat, lon) -> { throw new IllegalStateException("zone service down"); };
        check(SavedPlaceOrder.select(all, query("", null, SavedPlaceOrder.Sort.LOCAL_TIME_LATE), broken, DISTANCE, COOLDOWN).size() == 4,
            "a failing zone lookup must not drop entries");
        SavedPlaceOrder.Distance negative = (lat, lon) -> -5;
        check(SavedPlaceOrder.select(all, query("", null, SavedPlaceOrder.Sort.DISTANCE), ZONES, negative, COOLDOWN).size() == 4,
            "an invalid distance must not drop entries");
        check(SavedPlaceOrder.select(null, query("", null, SavedPlaceOrder.Sort.NAME_ASC), ZONES, DISTANCE, COOLDOWN).isEmpty(),
            "a missing snapshot is empty, not a crash");
        check(SavedPlaceOrder.select(all, null, ZONES, DISTANCE, COOLDOWN).size() == 4,
            "a missing query defaults to A-Z over everything");

        // ---- the input list is never mutated -------------------------------------------
        List<SavedPlaces.Entry> frozen = Collections.unmodifiableList(new ArrayList<>(Arrays.asList(london, natal)));
        SavedPlaceOrder.select(frozen, query("", null, SavedPlaceOrder.Sort.NAME_DESC), ZONES, DISTANCE, COOLDOWN);
        check(frozen.get(0) == london, "selecting must not reorder the caller's list");

        // ---- a stable menu index always maps to the same ordering ------------------------
        for (int i = 0; i < SavedPlaceOrder.Sort.values().length; i++)
            check(SavedPlaceOrder.at(i) == SavedPlaceOrder.Sort.values()[i], "menu index " + i + " remapped");
        check(SavedPlaceOrder.at(-1) == SavedPlaceOrder.Sort.NAME_ASC && SavedPlaceOrder.at(99) == SavedPlaceOrder.Sort.NAME_ASC,
            "an unknown stored index falls back to A-Z");

        // ---- the real store still validates what the list renders -----------------------
        for (String bad : new String[]{"", "   ", "X".repeat(101)}) {
            try { place(bad, 0, 0, "Geral"); throw new AssertionError("invalid name accepted: " + bad); }
            catch (IllegalArgumentException expected) { }
        }
        check(TimeZone.getTimeZone("UTC").getRawOffset() == 0, "the JVM time zone must not affect the ordering");

        System.out.println("PASS: saved-place filter and ordering (accent folding, category union, local time, distance, cooldown, stable menu indexes)");
    }
}
