package dev.pgrazor;

/** Small pure-Java policy for Candy research requests; does not call external servers. */
final class CandyQuestPolicy {
    private CandyQuestPolicy() {}
    /** The provider rejects an unscoped Candy search without a task or Pokémon. */
    static boolean selectionRequired(String rewardType, boolean nearby, int speciesCount, int taskCount) {
        return "Candy".equals(rewardType) && !nearby && speciesCount == 0 && taskCount == 0;
    }
}
