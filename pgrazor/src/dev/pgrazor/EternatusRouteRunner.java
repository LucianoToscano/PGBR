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
    private static final int MAX_STOPS = 400;
    private static GpxRoute planned;
    private static String template = "";
    private static boolean running, previousVgpOverlay, previousVgpEnabled, previousQuickSpin;
    private static boolean awaitingClaim, advancementQueued;
    private static int at;
    private static long arrivedAt, lastTick;
    private static String status = "Nenhuma rota GPX selecionada.";

    private EternatusRouteRunner() {}

    public static boolean chooseRoute(GpxRoute value) {
        if (value == null || value.size() > MAX_STOPS) return false;
        synchronized (LOCK) {
            if (running) return false;
            planned = value;
            status = "GPX preparado: " + value.name + " · " + value.size() + " paradas.";
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
    public static int currentStop() { synchronized (LOCK) { return running ? at + 1 : 0; } }
    public static int totalStops() { synchronized (LOCK) { return planned == null ? 0 : planned.size(); } }

    /**
     * Must be invoked following an explicit confirmation in the event UI.
     * A controller for another automated travel mode is never displaced.
     */
    public static boolean start(Activity activity) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()
            || !Control.active() || !Control.canEnable() || !Control.locationKnown()
            || AutoRaid.running() || ShundoSniper.active() || Control.gameRoutesRunning()
            || Control.engine.walking) return false;
        final GpxRoute route;
        final String exactTemplate;
        synchronized (LOCK) {
            if (running || planned == null || planned.size() > MAX_STOPS || template.isEmpty()) return false;
            route = planned;
            exactTemplate = template;
        }
        if (!EternatusMission.start(activity, exactTemplate)) return false;
        synchronized (LOCK) {
            if (running) { EternatusMission.stop("Início concorrente recusado."); return false; }
            previousVgpOverlay = Control.vgpOverlay();
            previousVgpEnabled = Control.vgpEnabled();
            previousQuickSpin = Control.quickSpinEnabled();
            running = true;
            at = 0;
            lastTick = 0;
            awaitingClaim = false;
            advancementQueued = false;
            status = "Preparando a primeira parada.";
        }
        // Preserve old preferences so Cancel restores the user's previous mode.
        Control.setVgpEnabled(false);
        Control.setVgpOverlay(true);
        Control.setQuickSpinEnabled(true);
        if (!travel(0)) {
            stop("Falha ao posicionar a primeira parada.");
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
            arrivedAt = SystemClock.elapsedRealtime();
            status = "Teletransportando para a parada " + (at + 1) + "/" + route.size() + ".";
        }
        double[] point = route.points()[destinationIndex];
        try {
            if (!Control.active() || !Control.canEnable()) return false;
            Control.setVgpEnabled(false);
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
        double[] point = route.points()[index];
        if (CooldownState.distance(point[0], point[1], Control.currentLatitude(), Control.currentLongitude()) > 0.2) {
            stop("Coordenada alterada durante a execução. Nenhuma nova ação será iniciada.");
            return;
        }
        long cooldown = Cooldown.remaining();
        if (cooldown > 0) {
            if (Control.vgpEnabled()) Control.setVgpEnabled(false);
            synchronized (LOCK) {
                awaitingClaim = false;
                status = "Parada " + (index + 1) + "/" + route.size()
                    + " · aguardando cooldown estimado: " + ((cooldown + 999) / 1000) + " s.";
            }
            return;
        }
        // Let the location adapter and nearby world state settle after each teleport.
        if (now - arrived < 3500) {
            synchronized (LOCK) { status = "Aguardando mapa na parada " + (index + 1) + "/" + route.size() + "."; }
            return;
        }
        if (!Control.vgpEnabled()) Control.setVgpEnabled(true);
        boolean available = Control.vgpEnabled() && Control.quickSpinAllowed();
        synchronized (LOCK) {
            awaitingClaim = available;
            status = available
                ? "Parada " + (index + 1) + "/" + route.size()
                    + " · VGP+/giro ativos. Aguardando resgate confirmado da pesquisa."
                : "Parada " + (index + 1) + "/" + route.size()
                    + " · VGP+ ou giro indisponível; verifique o jogo antes de continuar.";
        }
    }

    /** Native research owner sends only a successful claim of the exact selected quest. */
    public static void onClaim(String claimedTemplate) {
        final int completedStop;
        synchronized (LOCK) {
            if (!running || !awaitingClaim || advancementQueued || !template.equals(claimedTemplate)) return;
            advancementQueued = true;
            awaitingClaim = false;
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
    }

    /** Always restores feature preferences on explicit cancellation. */
    public static void stop(String reason) {
        boolean restoreOverlay, restoreQuickSpin, restoreVgp;
        synchronized (LOCK) {
            if (!running) return;
            running = false;
            awaitingClaim = false;
            advancementQueued = false;
            restoreOverlay = previousVgpOverlay;
            restoreQuickSpin = previousQuickSpin;
            restoreVgp = previousVgpEnabled;
            status = reason == null || reason.isEmpty() ? "Execução cancelada." : reason;
        }
        EternatusMission.stop(status());
        Control.setVgpEnabled(false);
        Control.setQuickSpinEnabled(restoreQuickSpin);
        Control.setVgpOverlay(restoreOverlay);
        if (restoreVgp && Control.active() && Control.canEnable() && Cooldown.remaining() == 0)
            Control.setVgpEnabled(true);
        Control.event("eternatus-route", "stopped");
    }
}
