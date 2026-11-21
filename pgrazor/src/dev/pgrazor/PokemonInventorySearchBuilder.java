package dev.pgrazor;

import java.util.ArrayList;
import java.util.List;

/**
 * Independent, offline generator of Pokémon GO inventory search expressions.
 * It never opens the user's inventory, touches the game identity, or modifies Pokémon.
 * Text keywords may depend on the language configured in the game.
 */
public final class PokemonInventorySearchBuilder {
    private PokemonInventorySearchBuilder() {}

    public static final int ANY = 0;
    public static final int HUNDO = 1;
    public static final int NUNDO = 2;
    public static final int PVP_01515 = 3;
    public static final int THREE_STAR = 4;
    public static final String[] BASE_LABELS = {
        "Qualquer avaliação", "100% · 4★", "0/0/0 · Nundo",
        "0/15/15 · candidato PvP", "3★"
    };
    private static final String[] BASE_TERMS = {
        "", "4*", "0attack&0defense&0hp",
        "0attack&4defense&4hp", "3*"
    };

    /**
     * Combines official collection terms with AND (&). Select only one IV base
     * to avoid ambiguous AND/OR operator precedence. Untrusted species input
     * is constrained to a decimal National Dex number (no query injection).
     */
    public static String build(int ivPreset, boolean shiny, boolean shadow,
                               boolean xxl, boolean background, boolean specialMove,
                               String dexText) {
        if (ivPreset < 0 || ivPreset >= BASE_TERMS.length)
            throw new IllegalArgumentException("Unknown IV preset");
        List<String> terms = new ArrayList<>();
        if (!BASE_TERMS[ivPreset].isEmpty()) terms.add(BASE_TERMS[ivPreset]);
        if (shiny) terms.add("shiny");
        if (shadow) terms.add("shadow");
        if (xxl) terms.add("xxl");
        if (background) terms.add("background");
        if (specialMove) terms.add("@special");

        String dex = dexText == null ? "" : dexText.trim();
        if (!dex.isEmpty()) {
            if (!dex.matches("[0-9]{1,4}"))
                throw new IllegalArgumentException("Número da Pokédex inválido");
            final int id;
            try { id = Integer.parseInt(dex); }
            catch (NumberFormatException e) { throw new IllegalArgumentException("Número inválido"); }
            if (id < 1 || id > 1025)
                throw new IllegalArgumentException("Número da Pokédex fora da biblioteca atual (1–1025)");
            terms.add(Integer.toString(id));
        }
        return String.join("&", terms);
    }
}
