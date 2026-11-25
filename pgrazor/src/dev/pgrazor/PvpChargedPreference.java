package dev.pgrazor;

/** Pure PvP charged-move selector using the same type order/chart as battle_counter.h. */
final class PvpChargedPreference {
    static final int UNKNOWN=-1, WAIT=0, FIRST=1, SECOND=2;
    static final int MAX_EXTRA_FAST=4, MIN_SAFE_HP=35;
    static final int RESISTED=0, NEUTRAL=1, SUPER_EFFECTIVE=2;
    static int effectivenessTier(double multiplier){
        if(!Double.isFinite(multiplier)||multiplier<=0)return -1;
        if(multiplier>1.00001)return SUPER_EFFECTIVE;
        return multiplier<0.99999?RESISTED:NEUTRAL;
    }
    // HoloPokemonType: 1 Normal, 2 Fighting, 3 Flying, 4 Poison, 5 Ground,
    // 6 Rock, 7 Bug, 8 Ghost, 9 Steel, 10 Fire, 11 Water, 12 Grass,
    // 13 Electric, 14 Psychic, 15 Ice, 16 Dragon, 17 Dark, 18 Fairy.
    private static final byte[][] TYPE = {
        {0,0,0,0,0,-1,0,-2,-1,0,0,0,0,0,0,0,0,0},
        {1,0,-1,-1,0,1,-1,-2,1,0,0,0,0,-1,1,0,1,-1},
        {0,1,0,0,0,-1,1,0,-1,0,0,1,-1,0,0,0,0,0},
        {0,0,0,-1,-1,-1,0,-1,-2,0,0,1,0,0,0,0,0,1},
        {0,0,-2,1,0,1,-1,0,1,1,0,-1,1,0,0,0,0,0},
        {0,-1,1,0,-1,0,1,0,-1,1,0,0,0,0,1,0,0,0},
        {0,-1,-1,-1,0,0,0,-1,-1,-1,0,1,0,1,0,0,1,-1},
        {-2,0,0,0,0,0,0,1,0,0,0,0,1,0,0,-1,0},
        {0,0,0,0,0,1,0,0,-1,-1,-1,0,-1,0,1,0,0,1},
        {0,0,0,0,0,-1,1,0,1,-1,-1,1,0,0,1,-1,0,0},
        {0,0,0,0,1,1,0,0,0,1,-1,-1,0,0,0,-1,0,0},
        {0,0,-1,-1,1,1,-1,0,-1,-1,1,-1,0,0,0,-1,0,0},
        {0,0,1,0,-2,0,0,0,0,0,1,-1,-1,0,0,-1,0,0},
        {0,1,0,1,0,0,0,0,-1,0,0,0,0,-1,0,0,-2,0},
        {0,0,1,0,1,0,0,0,-1,-1,-1,1,0,0,-1,1,0,0},
        {0,0,0,0,0,0,0,0,-1,0,0,0,0,0,0,1,0,-2},
        {0,-1,0,0,0,0,0,1,0,0,0,0,0,1,0,0,-1,-1},
        {0,1,0,-1,0,0,0,0,-1,-1,0,0,0,0,0,1,1,0}
    };

    static final class Move {
        final int type, power, cost;
        final boolean ready;
        Move(int type,int power,int cost,boolean ready){
            this.type=type;this.power=power;this.cost=cost;this.ready=ready;
        }
        boolean valid(){return type>=1&&type<=18&&power>0&&power<=1000&&cost>0&&cost<=100;}
    }
    private PvpChargedPreference(){}
    static boolean typeValid(int type){return type>=1&&type<=18;}
    static double effectiveness(int attack,int first,int second){
        if(!typeValid(attack)||!typeValid(first)||second!=0&&!typeValid(second))return Double.NaN;
        int score=TYPE[attack-1][first-1];
        if(second!=0&&second!=first)score+=TYPE[attack-1][second-1];
        return Math.pow(1.6,score);
    }
    static boolean superEffective(Move move,int opponent1,int opponent2){
        return move!=null&&move.valid()&&effectiveness(move.type,opponent1,opponent2)>1.00001;
    }
    /** The simplified setting compares only confirmed effectiveness tiers.
     *  A ready charged move is never delayed to farm energy for a stronger tier.
     *  The UI-ready bit remains authoritative; equal tiers preserve button 1.
     *  Data and slot identity must still be verified by the caller.
     */
    static int select(Move first,Move second,int attacker1,int attacker2,
                      int opponent1,int opponent2,int energy,int fastEnergyGain,int hpPercent,
                      boolean canFarm){
        if(first==null||!first.valid()||energy<0||energy>100)return UNKNOWN;
        final boolean firstReady=first.ready&&energy>=first.cost;
        // A single known move needs no type comparison; never assume there is a second button.
        if(second==null)return firstReady?FIRST:WAIT;
        if(!second.valid())return UNKNOWN;
        final boolean secondReady=second.ready&&energy>=second.cost;
        // When only one verified charged move is ready, no effectiveness comparison
        // or opponent-type inference is needed. Never wait for the unavailable move.
        if(firstReady&&!secondReady)return FIRST;
        if(secondReady&&!firstReady)return SECOND;
        if(!firstReady)return WAIT; // Neither ready: normal quick-attack policy continues.
        // HP and fast-energy gain are intentionally irrelevant to this tier-only choice.
        if(!typeValid(opponent1)||opponent2!=0&&!typeValid(opponent2)||
           opponent1==opponent2)return UNKNOWN;
        final int tier1=effectivenessTier(effectiveness(first.type,opponent1,opponent2));
        final int tier2=effectivenessTier(effectiveness(second.type,opponent1,opponent2));
        if(tier1<0||tier2<0)return UNKNOWN;
        // SUPER > NEUTRAL > RESISTED; equal tiers use the verified first slot.
        return tier2>tier1?SECOND:FIRST;
    }
}
