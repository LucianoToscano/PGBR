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
        {-2,0,0,0,0,0,0,1,0,0,0,0,0,1,0,0,-1,0},
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
    private static double value(Move move,int attacker1,int attacker2,int opponent1,int opponent2){
        double stab=move.type==attacker1||move.type==attacker2?1.2:1.0;
        return move.power*stab*effectiveness(move.type,opponent1,opponent2)/move.cost;
    }
    /** WAIT means continue charging; UNKNOWN means insufficient trustworthy observations. */
    static int select(Move first,Move second,int attacker1,int attacker2,
                      int opponent1,int opponent2,int energy,int fastEnergyGain,int hpPercent,
                      boolean canFarm){
        if(first==null||second==null||!first.valid()||!second.valid()||
           !typeValid(opponent1)||opponent2!=0&&!typeValid(opponent2)||
           energy<0||energy>100||hpPercent<0||hpPercent>100)return UNKNOWN;
        // The observed UI-ready state is authoritative; energy alone cannot force a charge tap.
        boolean oneReady=first.ready&&energy>=first.cost;
        boolean twoReady=second.ready&&energy>=second.cost;
        // Effectiveness is primary: super effective > neutral > resisted.
        // A neutral SECOND move must beat a resisted FIRST move.
        final double effect1=effectiveness(first.type,opponent1,opponent2);
        final double effect2=effectiveness(second.type,opponent1,opponent2);
        final int tier1=effectivenessTier(effect1);
        final int tier2=effectivenessTier(effect2);
        if(tier1==tier2){
            if(oneReady&&twoReady){
                if(tier1==RESISTED&&effect2>effect1+0.00001)return SECOND;
                if(tier1==SUPER_EFFECTIVE&&
                   value(second,attacker1,attacker2,opponent1,opponent2)>
                   value(first,attacker1,attacker2,opponent1,opponent2))return SECOND;
                return FIRST; // Preserve neutral tie behavior.
            }
            if(oneReady)return FIRST;
            if(twoReady)return SECOND;
            return WAIT;
        }
        final boolean preferFirst=tier1>tier2;
        final Move preferred=preferFirst?first:second;
        final boolean preferredReady=preferFirst?oneReady:twoReady;
        final boolean alternativeReady=preferFirst?twoReady:oneReady;
        if(preferredReady)return preferFirst?FIRST:SECOND;
        if(alternativeReady){
            final int gap=preferred.cost-energy;
            // Bounded farming: the controller also enforces a 5500 ms timeout.
            if(canFarm&&hpPercent>=MIN_SAFE_HP&&fastEnergyGain>0&&gap>0&&
               (gap+fastEnergyGain-1)/fastEnergyGain<=MAX_EXTRA_FAST)return WAIT;
            return preferFirst?SECOND:FIRST;
        }
        return WAIT;
    }
}
