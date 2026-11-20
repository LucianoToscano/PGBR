package dev.pgrazor;

import java.text.Normalizer;
import java.util.Locale;

/**
 * View-only matching for the Pokémon option chip lists of {@link PogoFeedModeFilterDialog} (raid
 * bosses, quest reward Pokémon, quest Mega Energy species and Power Spot bosses). It decides, for a
 * single option, whether the chosen {@link PokemonCategories} group and the search box would show it;
 * it never changes a selection.
 *
 * <p>Option keys differ by catalog: a Power Spot is a bare National Dex id
 * ({@code "6"}), while a raid boss is a composite {@code id|form|variant} ({@code "6|0|mega"}). The Dex
 * id is read from the whole key when it is numeric, otherwise from the part before the first bar, so
 * the form and variant ids in a composite key can never be mistaken for a Dex number in a {@code #}
 * search. Quest rows carry a packed species/form key plus explicit pokemon_id metadata; the
 * option-array overload uses that metadata without changing the packed selection key.
 * All and Selected keep options whose Dex id is unknown or out of range (saved picks that are
 * no longer in a catalog); the special groups only match valid species.
 */
final class PokemonOptionFilter {
    private PokemonOptionFilter() {}

    /** Catalog rows may supply an explicit Dex separately from their opaque selection key. */
    static int dexOf(String[] option) {
        return dexOf(option.length > 2 ? option[2] : option[1]);
    }

    static boolean visible(int category, String query, String[] option, boolean selected) {
        return visible(category, query, option[0], String.valueOf(dexOf(option)), selected);
    }

    /** The National Dex id encoded in an option key: the full numeric key, or the part before the first bar; -1 when absent. */
    static int dexOf(String key) {
        if (key == null) return -1;
        int bar = key.indexOf('|');
        String head = (bar >= 0 ? key.substring(0, bar) : key).trim();
        if (head.isEmpty()) return -1;
        try {
            return Integer.parseInt(head);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * Whether a category group includes this option. All keeps everything and Selected keeps the
     * currently selected options (both preserve unknown Dex ids); every other group defers to
     * {@link PokemonCategories#matches}, which only accepts valid species ids.
     */
    static boolean matchesCategory(int category, String key, boolean selected) {
        if (category == PokemonCategories.ALL) return true;
        if (category == PokemonCategories.SELECTED) return selected;
        return PokemonCategories.matches(category, dexOf(key), selected);
    }

    /**
     * Whether the search box shows this option. An empty query matches everything; a plain query
     * matches the name or the Dex number; a {@code #}-prefixed query matches the Dex number only.
     * Only the extracted Dex id is compared, so a form or variant id in a composite key never matches.
     */
    /** Fold accents, case and repeated whitespace without altering selection keys. */
    private static String fold(String text) {
        String normalized = Normalizer.normalize(text == null ? "" : text, Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{M}+", "")
            .toLowerCase(Locale.ROOT).trim().replaceAll("\\s+", " ");
    }

    static boolean matchesSearch(String query, String label, String key) {
        String q = fold(query);
        boolean dexOnly = q.startsWith("#");
        if (dexOnly) q = q.substring(1).trim();
        if (q.isEmpty()) return true;

        int dex = dexOf(key);
        String dexText = dex > 0 ? String.valueOf(dex) : "";
        // Preserve partial #Dex lookups while accepting #006 for species #6.
        boolean dexHit = !dexText.isEmpty() && dexText.contains(q);
        if (!dexHit && dex > 0 && q.matches("0+[0-9]+")) {
            try { dexHit = Integer.parseInt(q) == dex; }
            catch (NumberFormatException ignored) { /* oversized query: no match */ }
        }
        if (dexOnly) return dexHit;

        String name = fold(label);
        if (name.contains(q) || dexHit) return true;
        // "alola raichu" and "150 shiny" can match in any order.
        String[] tokens = q.split(" ");
        if (tokens.length < 2) return false;
        for (String token : tokens) {
            if (!name.contains(token) && !dexText.contains(token)) return false;
        }
        return true;
    }

    /** The category group and the search box intersected: an option is shown only when both keep it. */
    static boolean visible(int category, String query, String label, String key, boolean selected) {
        return matchesCategory(category, key, selected) && matchesSearch(query, label, key);
    }
}
