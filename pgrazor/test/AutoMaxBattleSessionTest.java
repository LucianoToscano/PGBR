package dev.pgrazor;

/** Verifica a sessao Max real em Control; nao simula Power Spot nem Unity. */
public final class AutoMaxBattleSessionTest {
    private static void check(boolean b,String message){if(!b)throw new AssertionError(message);}
    private static void flag(String name,boolean value)throws Exception {
        java.lang.reflect.Field f=Control.class.getDeclaredField(name);
        f.setAccessible(true);f.setBoolean(null,value);
    }
    private static void reject(int options,String expected){
        try{AutoMaxBattle.start(options);throw new AssertionError(expected);}
        catch(IllegalArgumentException|IllegalStateException ok){}
    }
    public static void main(String[] args)throws Exception {
        int original=Control.battleOptions();
        check(!AutoMaxBattle.running()&&AutoMaxBattle.optionMask()==0,"Sessao deve iniciar desligada");
        reject(0,"Aceitou sessão sem ataque");
        reject(AutoMaxBattle.DODGE,"Aceitou esquiva sem ataque");
        reject(1,"Aceitou bit de reides");
        reject(AutoMaxBattle.QUICK,"Aceitou início sem jogo pronto");
        check(Control.battleOptions()==original&&!AutoMaxBattle.running(),"Inicio inválido modificou opcoes");

        flag("foreground",true);
        Control.connect();Control.engine.enabled=true;
        Control.configureQuickSummary(true);
        Control.setQuickSummaryEnabled(true);
        Control.setQuickSummaryReady(true,"Pronto");
        Control.setBattlesReady(true,"Pronto");
        check(Control.active()&&Control.battlesAllowed(),"Pré-condição do motor inválida");
        Control.setBattleOptions(1); // Opcao de reides permanente
        int session=AutoMaxBattle.CHARGED|AutoMaxBattle.MAX_ATTACK|AutoMaxBattle.DODGE;
        AutoMaxBattle.start(session);
        check(AutoMaxBattle.running()&&AutoMaxBattle.selectedMask()==session,"Sessao nao foi ativada");
        check(AutoMaxBattle.optionMask()==0&&AutoMaxBattle.actionsBlocked(),"Sem contexto Max confiável, ações devem permanecer bloqueadas");
        check(Control.battleOptions()==1,"Bits Max vazaram para opções globais sem contexto confiável");
        check(!Control.vgpAllowed(),"VGP nao pode executar em paralelo com a sessao Max");
        reject(AutoMaxBattle.QUICK,"Aceitou duas sessoes simultaneas");
        AutoMaxBattle.stop();
        check(!AutoMaxBattle.running()&&Control.battleOptions()==1,"Parada nao preservou opcoes de reides");

        AutoMaxBattle.start(AutoMaxBattle.QUICK);
        Control.setBattlesReady(false,"Indisponível");
        check(!AutoMaxBattle.running()&&Control.battleOptions()==1,"Perda da prontidão rearmou sessao Max");
        Control.setBattlesReady(true,"Pronto");
        check(!AutoMaxBattle.running(),"Sessao retornou sem consentimento");

        AutoMaxBattle.start(AutoMaxBattle.MAX_ATTACK);
        check(Control.battleOptions()==1,"Batalha desconhecida nao pode receber ataque Max");
        Control.suspendForBackground();
        check(!AutoMaxBattle.running()&&Control.battleOptions()==1,"Segundo plano nao revogou sessao");
        System.out.println("PASS: Max manual session policy, readiness, masks, opt-in, stop and revocation");
    }
}
