package dev.pgrazor;

import android.app.Activity;
import android.os.SystemClock;
import java.util.List;

/**
 * Opt-in Eternatus GPX coordinator. Uses only existing location, VGP, quick-spin
 * and field-research owners; it never forges quest outcomes or bypasses cooldown.
 *
 * The selected exact quest template MUST have been observed by the game.
 * Rewards are not inferred from a quest caption: the player verifies the
 * observed 25-candy reward before arming the run. Every claim is acknowledged
 * by the native research owner before advancing to the next GPX coordinate.
 */
public final class EternatusRouteRunner {
    private static final Object LOCK = new Object();
    private static final int MAX_STOPS = GpxCodec.MAX_POINTS; // Full GPX route capacity, including 550+ stops.
    private static GpxRoute planned;
    private static String template = "";
    private static boolean running, previousVgpOverlay, previousVgpEnabled, previousQuickSpin;
    private static boolean previousAutoThrow, previousAutoThrowOnMap, withoutVgp, timedAdvance;
    private static int previousThrowGrade, previousThrowVariety;
    private static boolean awaitingClaim, advancementQueued;
    private static final java.util.HashSet<String> confirmedQuestIds=new java.util.HashSet<>();
    private static int at, resumeAt;
    private static long arrivedAt, lastTick, cooldownClearedAt, actionsSince;
    // The 3-second margin starts when the estimate actually reaches zero, not at teleport.
    static final long SAFETY_MARGIN_MS=3000L, UNVERIFIED_DWELL_MS=22000L;
    private static int unverifiedStops;
    private static String status = "Nenhuma rota GPX selecionada.";
    private static String lastResearchIssue = "";

    private EternatusRouteRunner() {}

    public static boolean chooseRoute(GpxRoute value) {
        if (value == null || value.size() > MAX_STOPS) return false;
        synchronized (LOCK) {
            if (running) return false;
            if(planned != value)resumeAt = 0;
            planned = value;
            status = "GPX preparado: " + value.name + " · " + value.size()
                + " paradas. Reinício selecionável de 1 a " + value.size() + ".";
            return true;
        }
    }

    public static boolean chooseTemplate(String value) {
        if (!ResearchPreferences.validTemplate(value)) return false;
        boolean observed = false;
        List<String[]> tasks = ResearchSettings.observedTemplates();
        for (String[] task : tasks)
            if (task.length >= 4 && value.equals(task[0])) { observed = true; break; }
        if (!observed) return false;
        synchronized (LOCK) {
            if (running) return false;
            template = value;
            return true;
        }
    }

    public static GpxRoute plannedRoute() { synchronized (LOCK) { return planned; } }
    public static String selectedTemplate() { synchronized (LOCK) { return template; } }
    public static boolean active() { synchronized (LOCK) { return running; } }
    public static String status() { synchronized (LOCK) { return status; } }
    public static String compactStatus(){
        String detail=status();
        int separator=detail.indexOf(" · ");
        return detail.startsWith("Parada ") && separator>0
            ? detail.substring(separator+3) : detail;
    }
    public static int currentStop() { synchronized (LOCK) { return running ? at + 1 : 0; } }
    /** Last visited 1-based position remains available after a failure or cancellation. */
    public static int resumePosition() {
        synchronized (LOCK) { return planned == null ? 1 : Math.max(1,Math.min(planned.size(),resumeAt+1)); }
    }
    public static int totalStops() { synchronized (LOCK) { return planned == null ? 0 : planned.size(); } }
    public static int unverifiedStops() { synchronized (LOCK) { return unverifiedStops; } }
    public static String actionMode(){
        synchronized (LOCK) { return withoutVgp ? "Sem VGP (giro rápido + captura no mapa)"
            : "VGP+ e giro rápido"; }
    }

    /** Human-readable, specific preflight reason used by the Event panel. */
    public static String readinessError(Activity activity) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed())
            return "Tela do jogo indisponível para iniciar a rota.";
        if (!Control.active()) return "PGBR está desativado. Ative o PGBR antes de iniciar.";
        if (!Control.canEnable()) return "Adaptador de localização indisponível.";
        if (!Control.locationKnown()) return "A localização atual ainda não foi obtida.";
        if (AutoRaid.running() || ShundoSniper.active() || Control.gameRoutesRunning()
            || Control.engine.walking) return "Há outra rota, caminhada ou automação ativa. Encerre-a antes.";
        final GpxRoute route;
        final String chosen;
        synchronized (LOCK) {
            if (running) return "A rota Eternatus já está em execução.";
            route = planned;
            chosen = template;
        }
        if (route == null) return "Escolha ou importe uma rota GPX primeiro.";
        if (route.size() <= 0 || route.size() > MAX_STOPS)
            return "A rota GPX precisa conter de 1 a " + MAX_STOPS + " paradas válidas.";
        if (!ResearchPreferences.validTemplate(chosen))
            return "Selecione a missão exata observada no jogo.";
        boolean present = false;
        for (String[] row : ResearchSettings.observedTemplates())
            if (row.length >= 4 && chosen.equals(row[0])) { present = true; break; }
        if (!present) return "A missão selecionada não aparece no catálogo atual. Abra Pesquisas e aguarde o PGBR detectar a tarefa.";
        return "";
    }

    /** Updates the current route status without advancing or pretending a quest was claimed. */
    public static void onResearchError(String selectedTemplate, String explanation) {
        synchronized (LOCK) {
            if (!running || (selectedTemplate != null && !selectedTemplate.isEmpty()
                && !template.equals(selectedTemplate))) return;
            String safe = explanation == null ? "" : explanation.trim();
            if (safe.length() > 140) safe = safe.substring(0, 140);
            lastResearchIssue = safe.isEmpty() ? "Falha de pesquisa (sem avanço automático)."
                : "Erro de pesquisa: " + safe + " (sem avanço automático).";
            status = "Parada " + (at + 1) + "/" +
                (planned == null ? 0 : planned.size()) + " · " + lastResearchIssue;
        }
    }

    /**
     * Must be invoked following an explicit confirmation in the event UI.
     * A controller for another automated travel mode is never displaced.
     */
    public static boolean start(Activity activity) {
        return start(activity, resumePosition());
    }

    /** Start at a user-selected 1-based GPX point, without replaying prior coordinates. */
    public static boolean start(Activity activity,int startAtOneBased) {
        String problem = readinessError(activity);
        if (!problem.isEmpty()) {
            synchronized (LOCK) { status = problem; }
            return false;
        }
        final GpxRoute route;
        final String exactTemplate;
        synchronized (LOCK) {
            if (running || planned == null || planned.size() > MAX_STOPS || template.isEmpty()) return false;
            if(startAtOneBased<1 || startAtOneBased>planned.size()){
                status="Escolha uma posição válida entre 1 e "+planned.size()+".";
                return false;
            }
            route = planned;
            exactTemplate = template;
        }
        if (!EternatusMission.start(activity, exactTemplate)) {
            synchronized (LOCK) {
                status = "A missão selecionada não pôde ser ativada no jogo. Abra Pesquisas, confirme que foi observada e tente novamente.";
            }
            return false;
        }
        synchronized (LOCK) {
            if (running) { EternatusMission.stop("Início concorrente recusado."); return false; }
            previousVgpOverlay = Control.vgpOverlay();
            previousVgpEnabled = Control.vgpEnabled();
            previousQuickSpin = Control.quickSpinEnabled();
            previousAutoThrow = Control.autoThrow();
            previousAutoThrowOnMap = Control.autoThrowOnMap();
            withoutVgp = Control.eternatusWithoutVgp();
            timedAdvance = Control.eternatusTimedAdvance();
            unverifiedStops = 0;
            previousThrowGrade = Control.throwRating();
            previousThrowVariety = Control.throwVariety();
            running = true;
            at = startAtOneBased - 1;
            resumeAt = at;
            lastTick = 0;
            cooldownClearedAt = 0;
            actionsSince = 0;
            lastResearchIssue = "";
            awaitingClaim = false;
            advancementQueued = false;
            confirmedQuestIds.clear();
            status = "Preparando a parada " + (at+1) + "/" + route.size() + ".";
        }
        // Preserve old preferences so Cancel restores the user's previous mode.
        Control.setVgpEnabled(false);
        if(!withoutVgp){
            Control.setVgpOverlay(true);
        }else{
            // Native quick spin runs when a spinner is opened; auto throw operates on
            // clicked map encounters. Neither fakes a spin/catch or requires VGP.
            Control.setAutoThrowOnMap(false);
            Control.setAutoThrow(false);
        }
        // Request the native throwing layer's Excellent grade, not the random variety mode.
        Control.setThrowVariety(0);
        Control.setThrowGrade(3);
        Control.setQuickSpinEnabled(false); // Never spin until the destination cooldown is clear.
        if (!travel(startAtOneBased-1)) {
            stop("Falha ao posicionar a parada selecionada.");
            return false;
        }
        Control.event("eternatus-route", "started; stops=" + route.size());
        return true;
    }

    private static boolean travel(int destinationIndex) {
        final GpxRoute route;
        synchronized (LOCK) {
            if (!running || planned == null || destinationIndex < 0 || destinationIndex >= planned.size()) return false;
            route = planned;
            awaitingClaim = false;
            advancementQueued = false;
            at = destinationIndex;
            resumeAt = at;
            cooldownClearedAt = 0;
            actionsSince = 0;
            lastResearchIssue = "";
            arrivedAt = SystemClock.elapsedRealtime();
            status = "Teletransportando para a parada " + (at + 1) + "/" + route.size() + ".";
        }
        double[] point = route.point(destinationIndex);
        try {
            if (!Control.active() || !Control.canEnable()) return false;
            Control.setVgpEnabled(false);
            Control.setQuickSpinEnabled(false);
            if(withoutVgp){
                Control.setAutoThrowOnMap(false);
                Control.setAutoThrow(false);
            }
            Control.teleport(point[0], point[1]);
            tick();
            return true;
        } catch (IllegalArgumentException | IllegalStateException error) {
            return false;
        }
    }

    /**
     * Called by Control's existing foreground heartbeat. A cooldown is a wait,
     * never a reason to reduce an internal timer or send a premature spin/catch.
     */
    public static void tick() {
        if (!active()) return;
        long now = SystemClock.elapsedRealtime();
        synchronized (LOCK) {
            if (now - lastTick < 750) return;
            lastTick = now;
        }
        if (!Control.active() || !Control.canEnable() || !Control.locationKnown()
            || AutoRaid.running() || ShundoSniper.active() || Control.gameRoutesRunning()
            || Control.engine.walking || !EternatusMission.active()) {
            stop("Execução interrompida por mudança de estado ou automação concorrente.");
            return;
        }
        final GpxRoute route;
        final int index;
        final long arrived;
        synchronized (LOCK) { route = planned; index = at; arrived = arrivedAt; }
        if (route == null) { stop("Rota GPX indisponível."); return; }
        double[] point = route.point(index);
        if (CooldownState.distance(point[0], point[1], Control.currentLatitude(), Control.currentLongitude()) > 0.2) {
            stop("Coordenada alterada durante a execução. Nenhuma nova ação será iniciada.");
            return;
        }
        long cooldown = Cooldown.remaining();
        if (cooldown > 0) {
            if (Control.vgpEnabled()) Control.setVgpEnabled(false);
            if (Control.quickSpinEnabled()) Control.setQuickSpinEnabled(false);
            if(withoutVgp && Control.autoThrowOnMap())Control.setAutoThrowOnMap(false);
            if(withoutVgp && Control.autoThrow())Control.setAutoThrow(false);
            synchronized (LOCK) {
                cooldownClearedAt=0;
                actionsSince=0;
                awaitingClaim = false;
                status = "Parada " + (index + 1) + "/" + route.size()
                    + " · aguardando cooldown estimado: " + ((cooldown + 999) / 1000) + " s."
                    + (lastResearchIssue.isEmpty() ? "" : " " + lastResearchIssue);
            }
            return;
        }
        // Cooldown is an estimate. Always require an EXTRA 3s AFTER it turns green,
        // and at least 3.5s after the last teleport for the game map to settle.
        synchronized (LOCK) {
            if (cooldownClearedAt == 0L) cooldownClearedAt = now;
            if (now-arrived < 3500L || now-cooldownClearedAt < SAFETY_MARGIN_MS) {
                status = "Parada " + (index+1) + "/" + route.size()
                    + " · cooldown liberado; margem de segurança "
                    + Math.max(1,(SAFETY_MARGIN_MS-(now-cooldownClearedAt)+999)/1000) + " s."
                    + (lastResearchIssue.isEmpty() ? "" : " " + lastResearchIssue);
                return;
            }
        }
        if (!Control.quickSpinEnabled()) Control.setQuickSpinEnabled(true);
        if (!withoutVgp) {
            if (!Control.vgpEnabled()) Control.setVgpEnabled(true);
        } else {
            if (Control.vgpEnabled()) Control.setVgpEnabled(false);
            if (!Control.autoThrow()) Control.setAutoThrow(true);
            if (!Control.autoThrowOnMap()) Control.setAutoThrowOnMap(true);
        }
        boolean ready=Control.quickSpinAllowed()
            && (withoutVgp ? Control.autoThrow() && Control.autoThrowOnMap()
                : Control.vgpEnabled());
        boolean advance=false;
        int next=-1;
        synchronized (LOCK) {
            awaitingClaim=ready;
            if(ready && actionsSince==0L)actionsSince=now;
            // This opt-in mode advances WITHOUT crediting any candy or research.
            // Quick Spin requires opening the disc and Map Auto Throw requires
            // tapping the spawn; noVgp alone cannot initiate those gestures.
            if(ready && timedAdvance && !advancementQueued && !Control.catchScreenVisible()
                && actionsSince>0 && now-actionsSince >= UNVERIFIED_DWELL_MS) {
                advancementQueued=true;
                awaitingClaim=false;
                unverifiedStops++;
                next=index+1;
                advance=true;
            }else{
                String mode=!ready
                    ? "Giro/captura ainda indisponíveis ("+Control.quickSpinStatus()+")."
                    : withoutVgp
                        ? "VGP desligada; abra PokéParada e toque Pokémon no mapa (assistido)."
                        : "VGP+ e giro habilitados.";
                status="Parada "+(index+1)+"/"+route.size()+" · "+mode
                    +(timedAdvance && ready
                        ? " Continuação por tempo em "
                          +Math.max(0,(UNVERIFIED_DWELL_MS-(now-actionsSince)+999)/1000)+" s; sem confirmação de doces."
                        : " Aguardando resgate da pesquisa.")
                    +(lastResearchIssue.isEmpty() ? "" : " "+lastResearchIssue);
            }
        }
        if(advance){
            Control.event("eternatus-route","unverified-stop; no candy credited; position="+(index+1));
            if(next>=route.size())stop("GPX percorrido. "+unverifiedStops()
                +" paradas sem resgate confirmado; nenhum doce presumido.");
            else if(!travel(next))stop("Não foi possível avançar à próxima coordenada após a espera.");
        }
    }

    /** A verified native success for this exact quest instance is required. */
    public static boolean onClaim(String claimedTemplate,String questId) {
        final int completedStop;
        synchronized (LOCK) {
            if (!running || !awaitingClaim || advancementQueued || !template.equals(claimedTemplate)
                || questId == null || questId.isEmpty() || questId.length() > 256
                || !confirmedQuestIds.add(questId)) return false;
            advancementQueued = true;
            awaitingClaim = false;
            lastResearchIssue = "";
            completedStop = at;
        }
        // Return to the UI thread. Never teleport inside the native response callback.
        new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
            synchronized (LOCK) {
                if (!running || at != completedStop || !advancementQueued) return;
            }
            final int next = completedStop + 1;
            if (next >= totalStops()) {
                stop("GPX concluído: todas as paradas tiveram um resgate confirmado.");
            } else if (!travel(next)) {
                stop("Não foi possível avançar à próxima coordenada.");
            }
        });
        return true;
    }

    /**
     * Explicit, user-initiated skip for a stop without the selected research.
     * Does not increment candy/claim counters or bypass cooldown.
     */
    public static boolean skipCurrentStop() {
        final int next;
        synchronized (LOCK) {
            if (!running || advancementQueued || planned == null || Cooldown.remaining() > 0)
                return false;
            if (at + 1 >= planned.size()) {
                status = "Última parada sem resgate. Encerre a rota ou confira as pesquisas.";
                return false;
            }
            advancementQueued = true;
            awaitingClaim = false;
            next = at + 1;
        }
        if (!travel(next)) {
            stop("Falha ao pular a parada; nenhuma missão foi registrada.");
            return false;
        }
        Control.event("eternatus-route", "manual-skip-without-claim");
        return true;
    }

    /** Always restores feature preferences on explicit cancellation. */
    public static void stop(String reason) {
        boolean restoreOverlay, restoreQuickSpin, restoreVgp, restoreAutoThrow, restoreMapThrow, usedWithoutVgp;
        int restoreThrowGrade, restoreThrowVariety;
        synchronized (LOCK) {
            if (!running) return;
            running = false;
            lastResearchIssue = "";
            awaitingClaim = false;
            advancementQueued = false;
            restoreOverlay = previousVgpOverlay;
            restoreQuickSpin = previousQuickSpin;
            restoreVgp = previousVgpEnabled;
            restoreAutoThrow=previousAutoThrow;
            restoreMapThrow=previousAutoThrowOnMap;
            usedWithoutVgp=withoutVgp;
            restoreThrowGrade = previousThrowGrade;
            restoreThrowVariety = previousThrowVariety;
            status = reason == null || reason.isEmpty() ? "Execução cancelada." : reason;
        }
        EternatusMission.stop(status());
        Control.setVgpEnabled(false);
        Control.setQuickSpinEnabled(restoreQuickSpin);
        if(usedWithoutVgp){
            Control.setAutoThrowOnMap(restoreMapThrow);
            Control.setAutoThrow(restoreAutoThrow);
        }
        Control.setThrowGrade(restoreThrowGrade);
        Control.setThrowVariety(restoreThrowVariety);
        Control.setVgpOverlay(restoreOverlay);
        if (restoreVgp && Control.active() && Control.canEnable() && Cooldown.remaining() == 0)
            Control.setVgpEnabled(true);
        Control.event("eternatus-route", "stopped");
    }
}
