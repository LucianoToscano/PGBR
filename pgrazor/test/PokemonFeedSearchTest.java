package dev.pgrazor;

import org.json.JSONObject;

/**
 * Contract for the local search used by both search screens. Fixtures are written by hand; no provider
 * is contacted.
 *
 * <p>Regression covered here: the remote request used to resolve a species name while the list compared
 * the raw "#25" label, so a name search could ask the server for Pikachu and then hide the answer.
 */
public final class PokemonFeedSearchTest {
    private static int checks;

    static void ok(boolean truth, String message) {
        checks++;
        if (!truth) throw new AssertionError(message);
    }

    public static void main(String[] args) throws Exception {
        JSONObject names = new JSONObject()
                .put("25", "Pikachu").put("26", "Raichu").put("6", "Charizard")
                .put("150", "Mewtwo").put("125", "Electabuzz").put("250", "Ho-Oh");

        ok(PokemonFeedSearch.resolveDex("#25", names) == 25, "#25 resolves to 25");
        ok(PokemonFeedSearch.resolveDex("25", names) == 25, "plain 25 resolves");
        ok(PokemonFeedSearch.resolveDex("pikachu", names) == 25, "name resolves");
        ok(PokemonFeedSearch.resolveDex("  PIKACHU  ", names) == 25, "case/space insensitive");
        ok(PokemonFeedSearch.resolveDex("charizard", names) == 6, "other name resolves");
        ok(PokemonFeedSearch.resolveDex("", names) == -1, "empty is unresolved");
        ok(PokemonFeedSearch.resolveDex(null, names) == -1, "null is unresolved");
        ok(PokemonFeedSearch.resolveDex("naoexiste", names) == -1, "unknown name is unresolved");
        ok(PokemonFeedSearch.resolveDex("#1026", names) == -1, "out-of-dex number is rejected");
        ok(PokemonFeedSearch.resolveDex("#0", names) == -1, "zero is rejected");
        ok(PokemonFeedSearch.resolveDex("pikachu", null) == -1, "no catalog means no guess");

        // Ambiguity must never pick one species silently.
        JSONObject ambiguous = new JSONObject().put("25", "Pikachu").put("26", "Pikachuclone");
        ok(PokemonFeedSearch.resolveDex("pikachu", ambiguous) == -1,
                "a query matching two species stays unresolved");

        // Accents and punctuation are ignored; exact dex numbers never match neighbours.
        ok(PokemonFeedSearch.matches("flabebe", 669, "Flabébé", "Flabébé"), "accents are ignored");
        ok(!PokemonFeedSearch.matches("25", 125, "Electabuzz", "Electabuzz"),
                "25 does not match 125");
        ok(PokemonFeedSearch.matches("#25", 25, "#25", "Pikachu"), "#25 matches its own row");
        ok(PokemonFeedSearch.matches("", 25, "#25", "Pikachu"), "empty query matches everything");
        ok(PokemonFeedSearch.matches("pika chu", 25, "Pikachu", "Pikachu"),
                "words may be split by spaces");
        ok(!PokemonFeedSearch.matches("pikachu", 6, "Charizard", "Charizard"),
                "wrong species does not match");

        System.out.println("PASS: " + checks + " verificações de busca por nome e número da Pokédex");
    }
}
