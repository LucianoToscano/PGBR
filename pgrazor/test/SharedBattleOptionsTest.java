package dev.pgrazor;
public final class SharedBattleOptionsTest {
    private static void expect(int wanted,int actual,String label){
        if(wanted!=actual)throw new AssertionError(label+" expected "+wanted+", actual "+actual);
    }
    private static void assertTrue(boolean b,String label){if(!b)throw new AssertionError(label);}
    public static void main(String[] args){
        for(int rocket=0;rocket<8;rocket++)for(int pvp=0;pvp<16;pvp++){
            int m=SharedBattleOptions.pvpMask(rocket,pvp);
            expect(rocket,m&7,"three settings match");
            expect(pvp&8,m&8,"PvP-only strategy preserved");
        }
        expect(0,SharedBattleOptions.restoreActions(true,0,7),"Rocket off wins migrated disagreement");
        expect(7,SharedBattleOptions.restoreActions(false,0,15),"legacy PvP fallback when Rocket property absent");
        expect(0,SharedBattleOptions.restoreActions(true,16,7),"invalid Rocket property fails closed");
        expect(5,SharedBattleOptions.actionMask(13),"strip PVP strategy on Rocket boundary");
        expect(0,SharedBattleOptions.actionMask(16),"unrecognized PvP mask rejected");
        assertTrue(!SharedBattleOptions.validActions(8),"Never send PVP-only bit to native Rocket");
        assertTrue(SharedBattleOptions.validActions(7),"all three allowed");
        for(int actions=0;actions<8;++actions){
            int pv=SharedBattleOptions.pvpMask(actions,0);
            int enabled=PvpBattleMode.update(pv,PvpBattleMode.PREFER_SUPER,true);
            int disabled=PvpBattleMode.update(enabled,PvpBattleMode.PREFER_SUPER,false);
            expect(actions,SharedBattleOptions.actionMask(enabled),"Smart Attack ON preserves fast/charged/shield");
            expect(actions,SharedBattleOptions.actionMask(disabled),"Smart Attack OFF preserves fast/charged/shield");
        }
        assertTrue(!PvpBattleMode.allowed(7,false,true,true),"shared settings cannot bypass PvP scope gate");
        System.out.println("SharedBattleOptionsTest passed");
    }
}
