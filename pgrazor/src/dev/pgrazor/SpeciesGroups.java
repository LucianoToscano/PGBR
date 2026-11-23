package dev.pgrazor;

import java.util.Arrays;

/**
 * National Dex membership for the species-level groups the sniper filters offer.
 *
 * <p><strong>Provenance and limits.</strong> This repository ships no official species taxonomy: the
 * game bindings ({@code native/schema/game}) and the PvP snapshot carry stats and rankings, not a
 * legendary/mythical flag. The two tables below are therefore a <em>curated PGBR table</em>, written
 * from the National Dex generations 1-9 and checked only for internal consistency by
 * {@code SpeciesGroupsTest}. They are not provider data and are not a claim of official status.
 *
 * <p>Two requested groups are deliberately <strong>not</strong> implemented, because a wrong answer
 * would silently hide valid Pokémon:
 * <ul>
 *   <li><strong>Regionais</strong> are regional <em>forms</em> of ordinary species (Alolan, Galarian…),
 *       not a species subset, and form ids differ per source.</li>
 *   <li><strong>Raros</strong> and <strong>evento</strong> have no stable definition; availability
 *       changes per event and per provider.</li>
 * </ul>
 * {@link SpawnFilters.Builder#build()} refuses those flags instead of pretending to filter.
 */
public final class SpeciesGroups {
    private SpeciesGroups() {}

    private static final int[] LEGENDARY = sorted(
            144, 145, 146, 150,
            243, 244, 245, 249, 250,
            377, 378, 379, 380, 381, 382, 383, 384,
            480, 481, 482, 483, 484, 485, 486, 487, 488,
            638, 639, 640, 641, 642, 643, 644, 645, 646,
            716, 717, 718,
            772, 773,
            785, 786, 787, 788, 789, 790, 791, 792,
            793, 794, 795, 796, 797, 798, 799,
            800,
            803, 804, 805, 806,
            888, 889, 890, 891, 892, 894, 895, 896, 897, 898,
            905,
            1001, 1002, 1003, 1004,
            1007, 1008,
            1014, 1015, 1016,
            1017, 1024);

    private static final int[] MYTHICAL = sorted(
            151,
            251,
            385, 386,
            489, 490, 491, 492, 493, 494,
            647, 648, 649,
            719, 720, 721,
            801, 802, 807, 808, 809,
            893,
            1025);

    private static int[] sorted(int... ids) {
        int[] copy = ids.clone();
        Arrays.sort(copy);
        return copy;
    }

    public static boolean legendary(int dex) { return Arrays.binarySearch(LEGENDARY, dex) >= 0; }

    public static boolean mythical(int dex) { return Arrays.binarySearch(MYTHICAL, dex) >= 0; }

    /**
     * Group predicate used by {@link SpawnFilters}. Only validated groups participate; the flags for
     * regional/rare/event are rejected when the filter set is built, so they never reach this call.
     */
    public static boolean matchesAny(int dex, boolean legendaryOnly, boolean mythicalOnly,
                                     boolean regionalOnly, boolean rareOnly, boolean eventOnly) {
        if (!legendaryOnly && !mythicalOnly) return true;   // nothing to enforce here
        return (legendaryOnly && legendary(dex)) || (mythicalOnly && mythical(dex));
    }

    /** Copies for tests and for a future generator; callers must not mutate the result. */
    public static int[] legendaryIds() { return LEGENDARY.clone(); }

    public static int[] mythicalIds() { return MYTHICAL.clone(); }
}
