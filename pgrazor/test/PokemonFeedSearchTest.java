package dev.pgrazor;

/** Local filter: no backend calls, no fabricated Pokémon or coordinates. */
public final class PokemonFeedSearchTest {
    private static void check(boolean ok, String label) {
        if (!ok) throw new AssertionError(label);
    }
    public static void main(String[] args) {
        check(PokemonFeedSearch.matches("", 25, "Pikachu", "Pikachu"), "blank shows all");
        check(PokemonFeedSearch.matches("Pika", 25, "Pikachu", ""), "substring case insensitive");
        check(PokemonFeedSearch.matches("#25", 25, "Pikachu", ""), "exact Dex ID");
        check(PokemonFeedSearch.matches("25", 25, "Pikachu", ""), "numeric Dex ID");
        check(!PokemonFeedSearch.matches("#25", 125, "Electabuzz", ""), "no Dex substring");
        check(!PokemonFeedSearch.matches("#25", 250, "Ho-Oh", ""), "no approximate Dex ID");
        check(PokemonFeedSearch.matches("Flabebe", 669, "Flabébé", ""), "diacritic normalisation");
        check(PokemonFeedSearch.matches("farfetch d", 83, "Farfetch'd", ""), "punctuation");
        check(PokemonFeedSearch.matches("mr mime", 122, "Mr. Mime", ""), "multi-word name");
        check(PokemonFeedSearch.matches("Toxtricity", 849, "", "Toxtricity"), "fallback name");
        check(!PokemonFeedSearch.matches("Eevee", 25, "Pikachu", ""), "reject wrong species");
        check(!PokemonFeedSearch.matches("01234", 25, "Pikachu", ""), "invalid Dex query");
        check(PokemonFeedSearch.matches(null, 25, "Pikachu", ""), "null query safe");
        System.out.println("PASS: Pokémon live name/Dex filter");
    }
}
