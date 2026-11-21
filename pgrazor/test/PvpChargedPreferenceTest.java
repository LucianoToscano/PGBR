package dev.pgrazor;

public final class PvpChargedPreferenceTest {
    private static void eq(int expected,int actual,String label){
        if(expected!=actual)throw new AssertionError(label+" expected "+expected+", got "+actual);
    }
    private static void yes(boolean value,String label){if(!value)throw new AssertionError(label);}
    private static PvpChargedPreference.Move m(int type,int power,int cost,boolean ready){
        return new PvpChargedPreference.Move(type,power,cost,ready);
    }
    private static PvpBattleController.Observation sample(long id,long now,int energy,int hp,
            boolean aReady,boolean bReady,int targetType){
        return new PvpBattleController.Observation(PvpBattleController.MODE_PVP,id,now,now,true,true,true,
            aReady,false,false,false,11,5,targetType,0,energy,9,hp,
            m(11,80,40,aReady),m(4,80,55,bReady));
    }
    private static int decide(PvpBattleController c,PvpBattleController.Observation s){
        return c.decide(s,PvpBattleMode.QUICK|PvpBattleMode.CHARGED|PvpBattleMode.PREFER_SUPER,true,true,true);
    }
    public static void main(String[] args){
        yes(Math.abs(PvpChargedPreference.effectiveness(13,11,3)-2.56)<0.00001,
            "electric vs water/flying double weakness");
        yes(Math.abs(PvpChargedPreference.effectiveness(11,12,0)-0.625)<0.00001,
            "water vs grass resisted");
        yes(Math.abs(PvpChargedPreference.effectiveness(4,12,0)-1.6)<0.00001,
            "poison vs grass super-effective");
        yes(Math.abs(PvpChargedPreference.effectiveness(13,5,0)-0.390625)<0.00001,
            "electric vs ground double resistance");
        yes(Double.isNaN(PvpChargedPreference.effectiveness(13,0,0)),
            "unknown opponent must not invent advantage");
        eq(PvpChargedPreference.WAIT,PvpChargedPreference.select(m(11,80,40,true),
            m(4,80,55,false),11,5,12,0,45,9,95,true),"save ready first to farm for super move");
        eq(PvpChargedPreference.SECOND,PvpChargedPreference.select(m(11,80,40,true),
            m(4,80,55,true),11,5,12,0,60,9,95,true),"choose super second");
        eq(PvpChargedPreference.FIRST,PvpChargedPreference.select(m(11,80,40,true),
            m(4,80,55,false),11,5,12,0,45,9,20,true),"low HP safety fallback");
        eq(PvpChargedPreference.FIRST,PvpChargedPreference.select(m(11,80,40,true),
            m(4,80,90,false),11,5,12,0,45,5,95,true),"far-away move fallback");
        eq(PvpChargedPreference.FIRST,PvpChargedPreference.select(m(11,80,40,true),
            m(4,80,55,false),11,5,12,0,45,9,95,false),"no quick attack fallback");
        eq(PvpChargedPreference.UNKNOWN,PvpChargedPreference.select(m(11,80,40,true),
            m(4,80,55,false),11,5,0,0,45,9,95,true),"unknown target type fail closed");
        eq(PvpChargedPreference.FIRST,PvpChargedPreference.select(m(11,80,40,true),
            m(4,80,55,true),11,5,11,0,60,9,90,true),"no super keeps first button");
        PvpBattleController c=new PvpBattleController();
        eq(PvpBattleController.QUICK,decide(c,sample(100,1000,45,90,true,false,12)),
            "hold charged first and use quick");
        eq(PvpBattleController.QUICK,decide(c,sample(100,1300,54,90,true,false,12)),
            "keep farming");
        eq(PvpBattleController.CHARGED_SECOND,decide(c,sample(100,1600,63,90,true,true,12)),
            "fire second charged when super effective");
        yes(decide(c,sample(100,1700,63,90,true,true,12))!=PvpBattleController.CHARGED_SECOND,
            "never repeat the same charged prompt");
        c.reset();
        eq(PvpBattleController.CHARGED,decide(c,sample(101,1000,45,20,true,false,12)),
            "low HP triggers ready first");
        c.reset();
        eq(PvpBattleController.QUICK,decide(c,sample(102,1000,45,90,true,false,12)),
            "begin preference wait");
        eq(PvpBattleController.CHARGED,decide(c,sample(102,7000,45,90,true,false,12)),
            "timeout falls back to ready first");
        c.reset();
        eq(PvpBattleController.CHARGED,decide(c,new PvpBattleController.Observation(
            PvpBattleController.MODE_PVP,103,1000,1000,true,true,true,true,false,false,false)),
            "missing metadata retains legacy first-button behavior");
        System.out.println("PvpChargedPreferenceTest passed");
    }
}
