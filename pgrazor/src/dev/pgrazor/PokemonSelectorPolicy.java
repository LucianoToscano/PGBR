package dev.pgrazor;

import java.text.Normalizer;
import java.util.Locale;

/** Pure client-side policy: never teleports to mismatched or expired spawn rows. */
final class PokemonSelectorPolicy {
    private PokemonSelectorPolicy() {}

    static String normalize(String query) {
        if (query == null) return "";
        return Normalizer.normalize(query.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").replaceAll("[\\p{Z}\\s]+", " ").trim();
    }

    static boolean matchesSpecies(int dex, String name, String query) {
        String q = normalize(query).replace("#", "").trim();
        return q.isEmpty() || normalize(name).contains(q) || Integer.toString(dex).startsWith(q);
    }

    static boolean validResult(LiveFeedData.Entry entry, int selectedDex, long now) {
        return entry != null && entry.kind == LiveFeedData.Kind.POKEMON
                && selectedDex >= 1 && entry.dexId == selectedDex
                && entry.expiresAt > now && CooldownState.valid(entry.lat, entry.lon)
                && (entry.lat != 0 || entry.lon != 0);
    }

    static String timeRemaining(long expiresAt, long now) {
        long seconds = Math.max(0L, (expiresAt-now+999L)/1000L);
        return String.format(Locale.US, "%d:%02d", seconds/60L, seconds%60L);
    }
}
