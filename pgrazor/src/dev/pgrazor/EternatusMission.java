package dev.pgrazor;

import android.content.Context;
import java.util.List;

/**
 * Narrow, account-local event research policy. It never renames account identity,
 * claims another quest, or guesses a reward template from its localized caption.
 */
public final class EternatusMission {
    private static volatile String template="";
    private static volatile String state="Desativado";
    private static volatile boolean enabled;
    private static Context context;
    private EternatusMission() {}
    public static synchronized boolean start(Context current,String exactTemplate) {
        if(current==null||!Control.active()||!ResearchPreferences.validTemplate(exactTemplate))return false;
        boolean observed=false;
        List<String[]> quests=ResearchSettings.observedTemplates();
        for(String[] row:quests)if(row.length>=4&&exactTemplate.equals(row[0])){observed=true;break;}
        if(!observed)return false;
        context=current.getApplicationContext();
        template=exactTemplate;
        enabled=true;
        state="Ativo: modelo exato selecionado. A leitura da recompensa de 25 doces ainda depende de confirmação no jogo.";
        Control.event("eternatus-research-start",exactTemplate);
        return true;
    }
    public static synchronized void stop(String reason) {
        if(!enabled)return;
        enabled=false;
        template="";
        state=reason==null?"Interrompido":reason;
        Control.event("eternatus-research-stop",state);
    }
    public static boolean active(){return enabled;}
    public static String selected(){return template;}
    public static String status(){return state;}
    public static String researchConfig(String original) {
        if(!enabled)return original;
        // Suspended/closed game must never auto-reactivate a destructive research policy.
        if(!Control.active()){stop("Modo desligado ao deixar o jogo ou perder conexão.");return original;}
        String chosen=template;
        if(!ResearchPreferences.validTemplate(chosen)){stop("Modelo de pesquisa inválido.");return original;}
        // R4 = selective claim, selective discard. All unselected completed rewards are preserved.
        return "R4|3|1|0|"+chosen;
    }
    /** Compatibility for callers without quest-instance metadata; such claims cannot advance a GPX. */
    public static void onResult(String phase,String observedTemplate) { onResult(phase,observedTemplate,"",""); }
    public static void onResult(String phase,String observedTemplate,String questId) {
        onResult(phase,observedTemplate,questId,"");
    }
    public static void onResult(String phase,String observedTemplate,String questId,String reason) {
        if(!enabled)return;
        if("error".equals(phase)) {
            if(observedTemplate!=null&&!observedTemplate.isEmpty()&&!template.equals(observedTemplate))return;
            String safe=reason==null?"":reason.trim();
            if(safe.length()>140)safe=safe.substring(0,140);
            state=safe.isEmpty()
                ? "Falha na pesquisa. Nenhuma parada avançou; verifique a tarefa no jogo."
                : "Falha na pesquisa: "+safe+" (sem avanço automático).";
            EternatusRouteRunner.onResearchError(observedTemplate,safe);
            Control.event("eternatus-research-error",state);
            return;
        }
        if(!template.equals(observedTemplate)||!"claimed".equals(phase))return;
        // Never count a duplicate native reply or one without a verified quest instance ID.
        // Native callbacks refer to the exact game quest, never to a guessed candy reward.
        if(questId==null||questId.isEmpty()){
            state="O jogo confirmou uma pesquisa sem identificador. Confira manualmente: a rota não avançou.";
            return;
        }
        if(!EternatusRouteRunner.onClaim(observedTemplate,questId)) {
            state="Confirmação ignorada (duplicada, atrasada ou de outra parada). Nenhum doce foi contado.";
            return;
        }
        Context app=context;
        if(app!=null&&EternatusProgress.confirm(app)){
            state="Pesquisa selecionada resgatada. Progresso salvo: "+EternatusProgress.confirmed(app)+
                " resgates (a quantidade real de doces precisa ser conferida no jogo).";
            Control.event("eternatus-research-claim",observedTemplate);
        }else state="Resgate confirmado pelo jogo, mas o registro local não foi salvo.";
    }
}
