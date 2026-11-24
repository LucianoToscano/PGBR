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

    /**
     * National Dex number when the query is an explicit number or a name that matches exactly one
     * species in the local catalog; -1 when it is ambiguous, unknown or empty.
     *
     * <p>Both the remote request and the local list must use this single answer. Previously the request
     * resolved names while the list compared against the raw "<em>#25</em>" label, so a name search could
     * send the right species to the server and then hide its own results.
     */
    static int resolveDex(String query, org.json.JSONObject names) {
        String q = query == null ? "" : query.trim();
        if (q.isEmpty()) return -1;
        String number = q.startsWith("#") ? q.substring(1) : q;
        if (number.matches("[0-9]{1,4}")) {
            try {
                int dex = Integer.parseInt(number);
                return dex >= 1 && dex <= 1025 ? dex : -1;
            } catch (NumberFormatException ignored) { return -1; }
        }
        if (names == null) return -1;
        int found = -1;
        for (int dex = 1; dex <= 1025; dex++) {
            String name = names.optString(Integer.toString(dex), "");
            if (!name.isEmpty() && matches(q, dex, name, name)) {
                if (found >= 0) return -1;   // ambiguous: never over-filter the remote request
                found = dex;
            }
        }
        return found;
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
