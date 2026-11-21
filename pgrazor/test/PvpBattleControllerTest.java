package dev.pgrazor;

public final class PvpBattleControllerTest {
    private static void yes(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    private static PvpBattleController.Observation obs(int mode,long id,long now,
            boolean active,boolean input,boolean charged,boolean shield,
            boolean hasShield,boolean swap){
        return new PvpBattleController.Observation(mode,id,now,now,active,input,true,
            charged,shield,hasShield,swap);
    }
    private static int decision(PvpBattleController x,PvpBattleController.Observation s,int opts){
        return x.decide(s,opts,true,true,true);
    }
    public static void main(String[] args){
        PvpBattleController p=new PvpBattleController();
        yes(decision(p,obs(2,44,500,true,true,true,true,true,true),7)==0,"Rocket scopes denied");
        yes(decision(p,obs(1,44,500,true,true,false,false,false,false),7)==1,"PvP fast attack");
        yes(decision(p,obs(1,44,560,true,true,false,false,false,false),7)==0,"tap throttle");
        yes(decision(p,obs(1,44,676,true,true,false,false,false,false),7)==1,"tap after throttle");
        yes(decision(p,obs(1,44,680,true,true,true,false,false,false),7)==2,"charged overrides fast");
        yes(decision(p,obs(1,44,690,true,true,true,false,false,false),2)==0,"charged only once while ready");
        decision(p,obs(1,44,700,true,true,false,false,false,false),2);
        yes(decision(p,obs(1,44,710,true,true,true,false,false,false),2)==2,"charged resets after energy spent");
        yes(decision(p,obs(1,44,720,true,true,true,true,true,false),7)==3,"shield prompt priority");
        yes(decision(p,obs(1,44,730,true,true,true,true,true,false),7)==0,"one shield per prompt");
        yes(decision(p,obs(1,44,740,true,true,true,true,true,false),3)==0,"no shield means hold modal");
        decision(p,obs(1,44,750,true,true,false,false,false,false),7);
        yes(decision(p,obs(1,44,760,true,true,true,true,true,false),7)==3,"new shield prompt");
        yes(decision(p,obs(1,44,770,true,true,true,true,true,true),7)==4,"faint swap priority");
        yes(decision(p,obs(1,44,780,true,true,true,true,true,true),7)==0,"swap once per prompt");
        yes(decision(p,obs(1,44,790,true,false,true,true,true,true),7)==0,"UI not ready");
        yes(decision(p,obs(1,44,800,true,true,true,false,false,false),0)==0,"all user switches off");
        yes(p.decide(obs(1,44,810,true,true,true,false,false,false),7,false,true,true)==0,"no verified runtime");
        yes(decision(p,obs(1,45,820,true,true,false,false,false,false),1)==1,"new battle resets state");
        yes(decision(p,new PvpBattleController.Observation(1,45,1000,0,true,true,true,false,false,false,false),7)==0,"stale observation denied");
        yes(decision(p,obs(1,45,1010,false,true,true,false,false,false),7)==0,"inactive scope denied");
        yes(decision(p,obs(1,45,1020,true,true,false,false,false,false),1)==1,"reconnect begins fresh");
        System.out.println("PvpBattleControllerTest passed");
    }
}
