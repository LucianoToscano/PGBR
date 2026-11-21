package dev.pgrazor;

/** Pure policy for independent, fail-closed PvP preferences. No game hooks. */
final class PvpBattleMode {
    static final int QUICK=1, CHARGED=2, SHIELD=4, ALL=QUICK|CHARGED|SHIELD;
    private PvpBattleMode(){}
    static int sanitize(int value){return value>=0&&(value&~ALL)==0?value:0;}
    static int update(int current,int option,boolean enabled){
        current=sanitize(current);
        if(option!=QUICK&&option!=CHARGED&&option!=SHIELD)return current;
        return enabled?current|option:current&~option;
    }
    /** A saved preference alone must never enable gameplay automation. */
    static boolean allowed(int options,boolean runtimeReady,boolean summaryReady,boolean hostLive){
        return sanitize(options)!=0&&runtimeReady&&summaryReady&&hostLive;
    }
}
