package dev.pgrazor;

public final class PvpBattleModeTest {
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    public static void main(String[] args){
        check(PvpBattleMode.sanitize(-1)==0,"negative masks are rejected");
        check(PvpBattleMode.sanitize(16)==0,"foreign bits rejected");
        check(PvpBattleMode.sanitize(7)==7,"three valid PvP action bits");
        check(PvpBattleMode.sanitize(15)==15,"priority bit accepted");
        check(PvpBattleMode.update(7,8,true)==15,"enable super-effective priority");
        check(!PvpBattleMode.allowed(8,true,true,true),"preference alone is not automation");
        check(PvpBattleMode.update(0,PvpBattleMode.QUICK,true)==1,"enable quick");
        check(PvpBattleMode.update(1,PvpBattleMode.CHARGED,true)==3,"enable charged without overwriting quick");
        check(PvpBattleMode.update(7,PvpBattleMode.SHIELD,false)==3,"disable shield only");
        check(PvpBattleMode.update(7,16,true)==7,"ignore unsupported bits");
        check(!PvpBattleMode.allowed(7,false,true,true),"unavailable PvP hooks never execute");
        check(!PvpBattleMode.allowed(0,true,true,true),"all PvP controls off");
        check(!PvpBattleMode.allowed(7,true,false,true),"no catch-summary gate");
        check(!PvpBattleMode.allowed(7,true,true,false),"host not live");
        check(PvpBattleMode.allowed(7,true,true,true),"ready PvP may execute saved choices");
        System.out.println("PvpBattleModeTest passed");
    }
}
