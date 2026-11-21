package dev.pgrazor;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Pure, local matcher for results already supplied by an authenticated feed.
 * Never guesses spawn locations or performs a request to another provider.
 */
final class PokemonFeedSearch {
    private PokemonFeedSearch() {}

    static boolean matches(String query, int dexId, String sourceName, String localizedName) {
        String needle = clean(query);
        if (needle.isEmpty()) return true;
        String raw = query == null ? "" : query.trim();
        // "#25" or "25": precise National Dex search, not "125" or "250".
        String id = raw.startsWith("#") ? raw.substring(1) : raw;
        if (!id.isEmpty() && id.length() <= 4 && digitsOnly(id)) {
            try { return dexId == Integer.parseInt(id); }
            catch (NumberFormatException ignored) { return false; }
        }
        String name = clean(sourceName), translated = clean(localizedName);
        // Multiple words narrow results; each word must be present in one name.
        String[] words = needle.split("\\s+");
        for (String part : words) {
            if (!name.contains(part) && !translated.contains(part)) return false;
        }
        return true;
    }

    private static boolean digitsOnly(String str) {
        for (int i=0; i<str.length(); i++) if (str.charAt(i)<'0'||str.charAt(i)>'9') return false;
        return !str.isEmpty();
    }

    private static String clean(String input) {
        if (input == null) return "";
        String lower = Normalizer.normalize(input.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD);
        return lower.replaceAll("\\p{M}+", "").replaceAll("[^\\p{L}\\p{N}# ]+", " ").trim();
    }
}
