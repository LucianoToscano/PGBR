package dev.pgrazor;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Filtering and ordering shared by the two saved-place surfaces — the World Map "Locais" library and
 * the quick "Favoritos" list — so a place lands in the same position on both.
 *
 * <p>Pure Java on purpose: the local time zone, the distance and the cooldown estimate are injected,
 * so this class never reads Android, network, game state or the device location. It only reads the
 * entries it is given and never modifies, reorders in place or drops a valid entry: a place without a
 * measurement sorts last instead of disappearing.
 */
public final class SavedPlaceOrder {
    /** Orderings offered by both lists. Each one is total: ties fall back to the name, then the id. */
    public enum Sort {
        NAME_ASC, NAME_DESC, NEWEST, OLDEST, LOCAL_TIME_EARLY, LOCAL_TIME_LATE, DISTANCE, COOLDOWN
    }

    /** Local UTC offset in minutes for a place's own coordinates, or null when the zone is unknown. */
    public interface LocalTime { Integer offsetMinutes(double latitude, double longitude); }
    /** Straight-line distance in metres from the current position, or NaN when unknown. */
    public interface Distance { double meters(double latitude, double longitude); }
    /** Remaining cooldown in milliseconds, or -1 when there is no history for that place. */
    public interface Cooldown { long remainingMillis(double latitude, double longitude); }

    /** A text + category filter with the requested ordering; immutable and null-safe. */
    public static final class Query {
        final String needle;
        final Set<String> categories;
        public final Sort sort;

        public Query(String text, Collection<String> categories, Sort sort) {
            this.needle = normalize(text);
            Set<String> allowed = new HashSet<>();
            if (categories != null) for (String category : categories) if (category != null) allowed.add(category);
            this.categories = Collections.unmodifiableSet(allowed);
            this.sort = sort == null ? Sort.NAME_ASC : sort;
        }

        public boolean allCategories() { return categories.isEmpty(); }
    }

    private static final Pattern MARKS = Pattern.compile("\\p{M}+");
    private static final Pattern SPACES = Pattern.compile("[\\p{Z}\\s]+");

    private SavedPlaceOrder() {}

    /** Accent- and case-insensitive form used for the text filter ({@code praca} finds {@code Praça}). */
    static String normalize(String value) {
        if (value == null || value.isEmpty()) return "";
        String folded = Normalizer.normalize(value.toLowerCase(Locale.ROOT).trim(), Normalizer.Form.NFD);
        return SPACES.matcher(MARKS.matcher(folded).replaceAll("")).replaceAll(" ").trim();
    }

    /** Entries matching the filter, in the requested order. Input order is never trusted. */
    public static List<SavedPlaces.Entry> select(List<SavedPlaces.Entry> entries, Query query,
            LocalTime time, Distance distance, Cooldown cooldown) {
        if (entries == null || entries.isEmpty()) return Collections.emptyList();
        Query wanted = query == null ? new Query("", null, Sort.NAME_ASC) : query;
        List<SavedPlaces.Entry> kept = new ArrayList<>();
        for (SavedPlaces.Entry entry : entries) {
            if (entry == null) continue;
            if (!wanted.allCategories() && !wanted.categories.contains(entry.category)) continue;
            if (!wanted.needle.isEmpty() && !haystack(entry).contains(wanted.needle)) continue;
            kept.add(entry);
        }
        kept.sort(comparator(wanted.sort, time, distance, cooldown));
        return kept;
    }

    static String haystack(SavedPlaces.Entry entry) {
        return normalize(entry.name + ' ' + entry.category);
    }

    private static Comparator<SavedPlaces.Entry> byName() {
        return Comparator.comparing((SavedPlaces.Entry e) -> e.name, String.CASE_INSENSITIVE_ORDER);
    }

    /** Total order: the requested key first; unmeasured values last; then name, then id. */
    static Comparator<SavedPlaces.Entry> comparator(Sort sort, LocalTime time, Distance distance, Cooldown cooldown) {
        Comparator<SavedPlaces.Entry> tie = byName().thenComparing(e -> e.id);
        if (sort == null) return tie;
        switch (sort) {
            case NAME_DESC: return byName().reversed().thenComparing(e -> e.id);
            case NEWEST: return olderFirst().reversed().thenComparing(tie);
            case OLDEST: return olderFirst().thenComparing(tie);
            case LOCAL_TIME_EARLY: return offsetOrder(time, false).thenComparing(tie);
            case LOCAL_TIME_LATE: return offsetOrder(time, true).thenComparing(tie);
            case DISTANCE: return distanceOrder(distance).thenComparing(tie);
            case COOLDOWN: return cooldownOrder(cooldown).thenComparing(tie);
            case NAME_ASC:
            default: return tie;
        }
    }

    private static Comparator<SavedPlaces.Entry> olderFirst() {
        return Comparator.comparingLong((SavedPlaces.Entry e) -> e.createdAtMs);
    }

    /** Orders by the place's own wall clock; places without a resolved zone go last either way. */
    private static Comparator<SavedPlaces.Entry> offsetOrder(LocalTime time, boolean latestFirst) {
        return (a, b) -> {
            Integer first = time == null ? null : offsetOf(time, a), second = time == null ? null : offsetOf(time, b);
            if (first == null || second == null) {
                if (first == null && second == null) return 0;
                return first == null ? 1 : -1; // an unresolved zone never outranks a known one
            }
            return latestFirst ? second - first : first - second;
        };
    }

    private static Integer offsetOf(LocalTime time, SavedPlaces.Entry entry) {
        try { return time.offsetMinutes(entry.latitude, entry.longitude); }
        catch (RuntimeException unknown) { return null; }
    }

    private static Comparator<SavedPlaces.Entry> distanceOrder(Distance distance) {
        return (a, b) -> {
            Double first = distanceOf(distance, a), second = distanceOf(distance, b);
            if (first == null || second == null) {
                if (first == null && second == null) return 0;
                return first == null ? 1 : -1; // unknown distance (no fix yet) stays last
            }
            return Double.compare(first, second);
        };
    }

    private static Double distanceOf(Distance distance, SavedPlaces.Entry entry) {
        if (distance == null) return null;
        try {
            double meters = distance.meters(entry.latitude, entry.longitude);
            return Double.isFinite(meters) && meters >= 0 ? meters : null;
        } catch (RuntimeException unknown) { return null; }
    }

    private static Comparator<SavedPlaces.Entry> cooldownOrder(Cooldown cooldown) {
        return (a, b) -> {
            Long first = cooldownOf(cooldown, a), second = cooldownOf(cooldown, b);
            if (first == null || second == null) {
                if (first == null && second == null) return 0;
                return first == null ? 1 : -1; // no cooldown history is not the same as ready now
            }
            return Long.compare(first, second);
        };
    }

    private static Long cooldownOf(Cooldown cooldown, SavedPlaces.Entry entry) {
        if (cooldown == null) return null;
        try {
            long remaining = cooldown.remainingMillis(entry.latitude, entry.longitude);
            return remaining < 0 ? null : remaining;
        } catch (RuntimeException unknown) { return null; }
    }

    /** Index of a sort in the menu order, so a stored index always maps to the same ordering. */
    public static Sort at(int index) {
        Sort[] values = Sort.values();
        return index >= 0 && index < values.length ? values[index] : Sort.NAME_ASC;
    }
}
