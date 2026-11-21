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
    static final int NO_ACTION = 0, QUICK = 1, CHARGED = 2, SHIELD = 3, NEXT_POKEMON = 4, CHARGED_SECOND = 5;
    private static final long MAX_SAMPLE_AGE_MS = 350;
    private static final long QUICK_INTERVAL_MS = 175;
    private static final long MAX_PREFERRED_WAIT_MS = 5500;

    static final class Observation {
        final int mode;
        final long battleId, nowMs, observedAtMs;
        final boolean active, inputReady, quickAvailable, chargedReady,
            shieldPrompt, shieldAvailable, chooseNextPrompt;
        final int attacker1, attacker2, opponent1, opponent2, energy, fastEnergyGain, hpPercent;
        final PvpChargedPreference.Move firstMove, secondMove;

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
            this.attacker1=0;this.attacker2=0;this.opponent1=0;this.opponent2=0;
            this.energy=-1;this.fastEnergyGain=0;this.hpPercent=-1;
            this.firstMove=null;this.secondMove=null;
        }
        Observation(int mode,long battleId,long nowMs,long observedAtMs,
                    boolean active,boolean inputReady,boolean quickAvailable,
                    boolean chargedReady,boolean shieldPrompt,boolean shieldAvailable,
                    boolean chooseNextPrompt,int attacker1,int attacker2,
                    int opponent1,int opponent2,int energy,int fastEnergyGain,int hpPercent,
                    PvpChargedPreference.Move firstMove,PvpChargedPreference.Move secondMove){
            this.mode=mode;this.battleId=battleId;this.nowMs=nowMs;
            this.observedAtMs=observedAtMs;this.active=active;this.inputReady=inputReady;
            this.quickAvailable=quickAvailable;this.chargedReady=chargedReady;
            this.shieldPrompt=shieldPrompt;this.shieldAvailable=shieldAvailable;
            this.chooseNextPrompt=chooseNextPrompt;
            this.attacker1=attacker1;this.attacker2=attacker2;
            this.opponent1=opponent1;this.opponent2=opponent2;
            this.energy=energy;this.fastEnergyGain=fastEnergyGain;this.hpPercent=hpPercent;
            this.firstMove=firstMove;this.secondMove=secondMove;
        }
    }

    private long liveBattleId, lastQuickMs = Long.MIN_VALUE;
    private boolean chargedConsumed, secondConsumed, shieldConsumed, nextConsumed, lastChargedReady, lastSecondReady;
    private long priorityWaitSince=-1;
    private int previousOpponent1,previousOpponent2;

    void reset() {
        liveBattleId = 0;
        lastQuickMs = Long.MIN_VALUE;
        chargedConsumed = secondConsumed = shieldConsumed = nextConsumed = false;
        lastChargedReady = lastSecondReady = false;
        priorityWaitSince=-1;previousOpponent1=previousOpponent2=0;
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
        boolean firstReady=s.firstMove!=null?s.firstMove.ready:s.chargedReady;
        boolean secondReady=s.secondMove!=null&&s.secondMove.ready;
        if (!firstReady || (firstReady&&!lastChargedReady)) chargedConsumed=false;
        if (!secondReady || (secondReady&&!lastSecondReady)) secondConsumed=false;
        lastChargedReady=firstReady;lastSecondReady=secondReady;
        if(previousOpponent1!=s.opponent1||previousOpponent2!=s.opponent2){
            priorityWaitSince=-1;previousOpponent1=s.opponent1;previousOpponent2=s.opponent2;
        }

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
        if ((selectedOptions & PvpBattleMode.CHARGED) != 0) {
            int choice=PvpChargedPreference.UNKNOWN;
            if ((selectedOptions & PvpBattleMode.PREFER_SUPER)!=0) {
                boolean mayFarm=(selectedOptions&PvpBattleMode.QUICK)!=0&&s.quickAvailable;
                choice=PvpChargedPreference.select(s.firstMove,s.secondMove,
                    s.attacker1,s.attacker2,s.opponent1,s.opponent2,
                    s.energy,s.fastEnergyGain,s.hpPercent,mayFarm);
                if(choice==PvpChargedPreference.WAIT){
                    if(priorityWaitSince<0)priorityWaitSince=s.nowMs;
                    if(s.nowMs-priorityWaitSince>=MAX_PREFERRED_WAIT_MS)
                        choice=PvpChargedPreference.select(s.firstMove,s.secondMove,
                            s.attacker1,s.attacker2,s.opponent1,s.opponent2,
                            s.energy,s.fastEnergyGain,s.hpPercent,false);
                } else priorityWaitSince=-1;
                if(choice==PvpChargedPreference.FIRST&&!chargedConsumed){
                    chargedConsumed=true;return CHARGED;
                }
                if(choice==PvpChargedPreference.SECOND&&!secondConsumed){
                    secondConsumed=true;return CHARGED_SECOND;
                }
            }
            // Fall back only when typed move data is incomplete; never guess slot 2.
            if(choice==PvpChargedPreference.UNKNOWN&&s.chargedReady&&!chargedConsumed){
                chargedConsumed=true;return CHARGED;
            }
        }
        if ((selectedOptions & PvpBattleMode.QUICK) != 0 && s.quickAvailable &&
                (lastQuickMs == Long.MIN_VALUE || s.nowMs - lastQuickMs >= QUICK_INTERVAL_MS)) {
            lastQuickMs = s.nowMs;
            return QUICK;
        }
        return NO_ACTION;
    }
}
