package dev.pgrazor;

import java.text.NumberFormat;
import java.util.Locale;

/** Read-only presentation. No trainer name, account key, exact timestamp or location is exported. */
final class SessionStatsPresentation {
    private SessionStatsPresentation() {}
    private static final Locale PT = new Locale("pt","BR");
    private static String number(double value,int digits) {
        if(!Double.isFinite(value)||value<0)return "indisponível";
        NumberFormat f=NumberFormat.getNumberInstance(PT);
        f.setMaximumFractionDigits(digits);
        f.setMinimumFractionDigits(digits);
        return f.format(value);
    }
    private static String count(SessionStatsHistory.Change c,int index){return number(c.get(index),0);}
    static String describe(SessionStatsHistory.Change c,long window) {
        if(c==null||window<=0)return "Aguardando estatísticas do jogo…";
        long covered=Math.max(0,Math.min(window,c.coveredMs));
        String period=covered==0?"Aguardando segunda leitura":covered<60_000?"Menos de 1 min observado":
                number(covered/60_000.0,0)+" min observados";
        String coverage=period+(covered<window?" · período parcial":" · período completo");
        return coverage+"\nXP +"+count(c,SessionStatsHistory.XP)+" · Poeira +"+count(c,SessionStatsHistory.DUST)
            +" / −"+count(c,SessionStatsHistory.DUST_SPENT)+" (estimativa pelo saldo)"
            +"\nCapturas "+count(c,SessionStatsHistory.CAUGHT)+" · Encontros "+count(c,SessionStatsHistory.SEEN)
            +"\nPoképaradas "+count(c,SessionStatsHistory.STOPS)+" · Caminhada "+number(c.get(SessionStatsHistory.KM),2)
            +" km · Ovos "+count(c,SessionStatsHistory.HATCHED)
            +(covered>=60_000?"\nXP/h estimado: "+number(c.get(SessionStatsHistory.XP)/(covered/3_600_000.0),0)
                +" (tempo observado)":"")
            +"\nInclui variações da conta entre leituras, inclusive em outros aparelhos.";
    }
    static String exportSummary(SessionStatsHistory.Change c,long window) {
        if(c==null||window<=0||c.coveredMs<=0)return null;
        return "PGBR — Resumo local ("+(window==SessionStatsHistory.DAY_MS?"24 h":window==SessionStatsHistory.HOUR_MS?"1 h":"janela selecionada")+")\n"+describe(c,window);
    }
}
