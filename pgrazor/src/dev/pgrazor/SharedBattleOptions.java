package dev.pgrazor;

/** One persisted mask for the three action toggles, shared by Rocket and PvP.
 *  PvP-only strategy bits must never leak into Rocket's 3-bit native ABI. */
final class SharedBattleOptions {
    static final int ACTIONS=7;
    private SharedBattleOptions(){}
    static boolean validActions(int value){return value>=0 && (value&~ACTIONS)==0;}
    static int actionMask(int pvpOptions){return PvpBattleMode.sanitize(pvpOptions)&ACTIONS;}
    static int pvpMask(int sharedActions,int oldPvpOptions){
        if(!validActions(sharedActions))return 0;
        return sharedActions | (PvpBattleMode.sanitize(oldPvpOptions)&PvpBattleMode.PREFER_SUPER);
    }
    static int restoreActions(boolean rocketPropertyExists,int oldRocket,int oldPvp){
        if(rocketPropertyExists) return validActions(oldRocket)?oldRocket:0;
        return actionMask(oldPvp);
    }
}
