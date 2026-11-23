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

    public static boolean running(){return sessionOptions!=0;}
    /** Os controles permanentes de batalha nunca sao alterados por esta sessao. */
    /**
     * Fail closed: the current host exposes battle readiness but does not
     * attest the active battle kind. Do not inject Max bits into the global
     * battle options until a trustworthy, scene-scoped Max signal exists.
     * The selected mask remains session-only for UI/diagnostics.
     */
    public static int optionMask(){return 0;}
    public static int selectedMask(){return sessionOptions;}
    public static boolean actionsBlocked(){return running();}

    public static String status(){return message;}

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
        sessionOptions=selected;
        message="Sessão preparada, mas ataques automáticos Max bloqueados: contexto Max não verificado.";
        Control.event("max-battle-session","Iniciada, entrada/captura manuais");
    }

    public static synchronized void stop(){
        if(!running())return;
        sessionOptions=0;
        message="Sessão Max encerrada; escolhas salvas mantidas.";
        Control.event("max-battle-session","Encerrada, preferencias de batalha preservadas");
    }

    /** Nunca retomar apos segundo plano, troca de conta, perda do motor ou reinicio. */
    public static synchronized void reset(){
        sessionOptions=0;
        message="Sessão Max desativada";
    }
}
