package dev.pgrazor;

/**
 * Sessao manual de Batalha Max, adaptada de belalsalah74/PGRazor.
 * Reutiliza exclusivamente os comandos nativos de batalha ja existentes.
 * Nao procura Power Spots, gasta Particulas Max, entra na luta ou captura.
 */
public final class AutoMaxBattle {
    private AutoMaxBattle() {}

    public static final int QUICK=1<<4;
    public static final int CHARGED=1<<5;
    public static final int MAX_ATTACK=1<<6;
    public static final int DODGE=1<<9;
    public static final int OPTION_MASK=QUICK|CHARGED|MAX_ATTACK|DODGE;

    private static volatile int sessionOptions;
    private static volatile String message="Sessão Max desativada";

    public static synchronized boolean running(){
        if(sessionOptions==0)return false;
        int state=0;try{state=NativeCore.maxSessionState();}catch(UnsatisfiedLinkError ignored){}
        if(state==0){sessionOptions=0;message="Sessão Max encerrada pelo motor; preferências preservadas.";return false;}
        message=state==2?"Batalha Max confirmada — sessão ativa.":"Sessão armada — entre manualmente em uma Batalha Max.";
        return true;
    }
    /** Nunca injeta bits temporários nas preferências globais: a sessão é nativa e isolada. */
    public static int optionMask(){return 0;}
    public static synchronized int selectedMask(){return running()?sessionOptions:0;}
    public static boolean actionsBlocked(){return running();}

    public static synchronized String status(){running();return message;}

    public static synchronized void start(int requestedOptions){
        int selected=requestedOptions&OPTION_MASK;
        if(selected!=requestedOptions || (selected&(QUICK|CHARGED|MAX_ATTACK))==0)
            throw new IllegalArgumentException("Escolha pelo menos um ataque Max válido.");
        if(running())throw new IllegalStateException("A sessão Max já está em execução.");
        if(!Control.active()||!Control.battlesAllowed())
            throw new IllegalStateException("Aguarde o jogo e o suporte às batalhas estarem prontos.");
        if(AutoRaid.running()||OneShotAutomation.enabled()||GiftBatchSettings.running()
                ||BatchEvolutionSettings.enabled()||ShundoSniper.active()
                ||GymAutomationSettings.running())
            throw new IllegalStateException("Encerre as outras automações antes da Batalha Max.");
        if(Control.engine.walking||Control.gameRoutesRunning()||Control.automationPlanning())
            throw new IllegalStateException("Pare a caminhada ou rota antes de iniciar a Batalha Max.");
        boolean armed=false;try{armed=NativeCore.maxSessionStart(selected);}catch(UnsatisfiedLinkError ignored){}
        if(!armed)throw new IllegalStateException("Motor Max indisponível ou outra batalha ativa. Nenhuma sessão foi iniciada.");
        sessionOptions=selected;
        message="Sessão armada — entre manualmente em uma Batalha Max.";
        Control.event("max-battle-session","Iniciada, entrada/captura manuais");
    }

    public static synchronized void stop(){
        if(sessionOptions==0)return;
        try{NativeCore.maxSessionStop();}catch(UnsatisfiedLinkError ignored){}
        sessionOptions=0;
        message="Sessão Max encerrada; escolhas salvas mantidas.";
        Control.event("max-battle-session","Encerrada, preferencias de batalha preservadas");
    }

    /** Nunca retomar apos segundo plano, troca de conta, perda do motor ou reinicio. */
    public static synchronized void reset(){
        try{NativeCore.maxSessionStop();}catch(UnsatisfiedLinkError ignored){}
        sessionOptions=0;
        message="Sessão Max desativada";
    }
}
