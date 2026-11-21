package dev.pgrazor;

/**
 * Deterministic PvP combat policy. This class has NO input hooks, process access or
 * network calls; a separate verified game-version adapter must supply observations
 * and execute returned actions on the game's UI thread.
 *
 * The policy is strictly PvP-only. A Rocket/raid/gym scope can NEVER be reused.
 */
final class PvpBattleController {
    static final int MODE_PVP = 0x505650; // Separate domain tag; never 0/1/2 (raid/gym/max)
    static final int NO_ACTION = 0, QUICK = 1, CHARGED = 2, SHIELD = 3, NEXT_POKEMON = 4;
    private static final long MAX_SAMPLE_AGE_MS = 350;
    private static final long QUICK_INTERVAL_MS = 175;

    static final class Observation {
        final int mode;
        final long battleId, nowMs, observedAtMs;
        final boolean active, inputReady, quickAvailable, chargedReady,
            shieldPrompt, shieldAvailable, chooseNextPrompt;

        Observation(int mode, long battleId, long nowMs, long observedAtMs,
                    boolean active, boolean inputReady, boolean quickAvailable,
                    boolean chargedReady, boolean shieldPrompt,
                    boolean shieldAvailable, boolean chooseNextPrompt) {
            this.mode = mode;
            this.battleId = battleId;
            this.nowMs = nowMs;
            this.observedAtMs = observedAtMs;
            this.active = active;
            this.inputReady = inputReady;
            this.quickAvailable = quickAvailable;
            this.chargedReady = chargedReady;
            this.shieldPrompt = shieldPrompt;
            this.shieldAvailable = shieldAvailable;
            this.chooseNextPrompt = chooseNextPrompt;
        }
    }

    private long liveBattleId, lastQuickMs = Long.MIN_VALUE;
    private boolean chargedConsumed, shieldConsumed, nextConsumed, lastChargedReady;

    void reset() {
        liveBattleId = 0;
        lastQuickMs = Long.MIN_VALUE;
        chargedConsumed = shieldConsumed = nextConsumed = lastChargedReady = false;
    }

    int decide(Observation s, int selectedOptions,
               boolean runtimeReady, boolean summaryReady, boolean hostLive) {
        if (s == null || s.mode != MODE_PVP || !s.active || s.battleId <= 0 ||
                s.nowMs < 0 || s.observedAtMs < 0 || s.nowMs < s.observedAtMs ||
                s.nowMs - s.observedAtMs > MAX_SAMPLE_AGE_MS ||
                !PvpBattleMode.allowed(selectedOptions, runtimeReady, summaryReady, hostLive)) {
            reset();
            return NO_ACTION;
        }
        if (liveBattleId != s.battleId) {
            reset();
            liveBattleId = s.battleId;
        }
        if (!s.shieldPrompt) shieldConsumed = false;
        if (!s.chooseNextPrompt) nextConsumed = false;
        if (!s.chargedReady) chargedConsumed = false;
        if (s.chargedReady && !lastChargedReady) chargedConsumed = false;
        lastChargedReady = s.chargedReady;

        if (!s.inputReady) return NO_ACTION;
        // Never tap through a modal, even if the associated option is disabled.
        if (s.chooseNextPrompt) {
            if (!nextConsumed) { nextConsumed = true; return NEXT_POKEMON; }
            return NO_ACTION;
        }
        if (s.shieldPrompt) {
            if ((selectedOptions & PvpBattleMode.SHIELD) != 0 && s.shieldAvailable && !shieldConsumed) {
                shieldConsumed = true;
                return SHIELD;
            }
            return NO_ACTION;
        }
        if ((selectedOptions & PvpBattleMode.CHARGED) != 0 && s.chargedReady && !chargedConsumed) {
            chargedConsumed = true;
            return CHARGED;
        }
        if ((selectedOptions & PvpBattleMode.QUICK) != 0 && s.quickAvailable &&
                (lastQuickMs == Long.MIN_VALUE || s.nowMs - lastQuickMs >= QUICK_INTERVAL_MS)) {
            lastQuickMs = s.nowMs;
            return QUICK;
        }
        return NO_ACTION;
    }
}
