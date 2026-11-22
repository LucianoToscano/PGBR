package dev.pgrazor;

/**
 * Runs real Control VGP policy to exercise VgpController state transitions; the Android shells and
 * the controllable Cooldown clock are supplied by test_control_java.py. It proves
 * that the clock cannot stop/start VGP, while foreground and manual-state gates remain active.
 */
public final class VgpControllerTest {
    private static void check(boolean value,String message) { if(!value)throw new AssertionError(message); }
    private static void foreground(boolean value) throws Exception {
        java.lang.reflect.Field field=Control.class.getDeclaredField("foreground");
        field.setAccessible(true); field.setBoolean(null,value);
    }
    private static boolean logged(String kind,String detail) {
        return android.util.Log.lines.stream().anyMatch(line->line.contains("["+kind+"] "+detail));
    }
    public static void main(String[] args) throws Exception {
        // Overlay off: the feature cannot be active regardless of an enable request.
        check(!Control.vgpOverlay() && !Control.vgpEnabled(),"Default overlay/active off");
        check(Control.vgpDiagnosticStatus().contains("não inicia"),"Overlay-off message distinguishes display and execution");
        foreground(true);
        Control.setVgpEnabled(true);
        check(!Control.vgpEnabled(),"Enable refused while overlay off");
        check(Control.vgpStatus().contains("overlay hidden"),"Explicit reason when overlay hidden");

        // Overlay on, no cooldown armed: an enable request turns the runtime flag on.
        Control.setVgpOverlay(true);
        check(Control.vgpDiagnosticStatus().contains("desligada"),"Showing overlay alone must not start VGP");
        Control.setVgpEnabled(true);
        check(Control.vgpEnabled() && Control.vgpOverlay(),"Enabled with overlay on in foreground");
        check(Control.vgpDiagnosticStatus().contains("aguardando"),"Enabled is not the same as runtime-ready");
        check(logged("vgp-enabled","ON; runtime only"),"Enable logged");
        java.util.Properties spin=new java.util.Properties();
        spin.setProperty("smartCatchEnabled","true");spin.setProperty("smartCatchSpinGyms","true");
        SmartCatchSettings.read(spin);
        Control.event("gym-spin","{\"message\":\"Gym spun\"}");
        check(Control.vgpStatus().equals("Gym spun"),"Gym spin reports through VGP status");
        SmartCatchSettings.read(new java.util.Properties());Control.setVgpStatus("Ready");
        Control.event("gym-spin","{\"message\":\"Gym spun\"}");
        check(Control.vgpStatus().equals("Ready"),"Late gym spin status ignored when VGP+ gym spin is disabled");

        // Old settings may still contain the removed cooldown option.
        // Neither an active global cooldown nor the legacy setter may stop VGP.
        Control.setVgpStopOnCooldown(true);
        check(!Control.vgpStopOnCooldown(),"Deprecated stop-on-cooldown option remains disabled");
        Cooldown.remaining=900000;
        Control.setVgpStatus("Ready");
        check(Control.vgpEnabled(),"Cooldown clock must not disable manually started VGP");
        Control.setVgpEnabled(false);
        check(!Control.vgpEnabled(),"Manual OFF stops VGP immediately");
        Control.setVgpEnabled(true);
        check(Control.vgpEnabled(),"Manual ON succeeds despite estimated cooldown");

        // Background switches VGP off with its own reason and never auto-resumes.
        Cooldown.remaining=0;
        Control.setVgpEnabled(true);
        check(Control.vgpEnabled(),"On before backgrounding");
        Control.suspendForBackground();
        android.os.Handler.drain();
        check(!Control.vgpEnabled(),"Background switches VGP off");
        check(Control.vgpStatus().equals("App left foreground; tap VGP to start"),"Background reason shown");

        // Returning to the foreground and turning the overlay off clears the runtime enable.
        foreground(true);
        Control.setVgpOverlay(true);
        Control.setVgpEnabled(true);
        check(Control.vgpEnabled(),"Re-enabled after returning to foreground");
        Control.setVgpOverlay(false);
        check(!Control.vgpOverlay() && !Control.vgpEnabled(),"Overlay off clears active VGP");

        // Target marker state round-trips and rejects out-of-range coordinates without clobbering.
        Control.setVgpTarget(true,0.25,0.75,true);
        check(Control.vgpTargetX()==0.25f && Control.vgpTargetY()==0.75f && Control.vgpTargetSpin(),"Marker stored");
        Control.setVgpTarget(true,2.0,0.5,false);
        check(Control.vgpTargetX()==0.25f,"Out-of-range marker leaves prior coordinates intact");

        // The option mask is a saved preference, independent of the runtime flag.
        Control.setVgpOptions(5);
        check(Control.vgpOptions()==5,"Option mask stored");

        // Exercise the actual Java permission boundary, not a source slice evaluated as JS.
        Control.configureQuickSummary(true); Control.setQuickSummaryReady(true,"Ready");
        Control.setItemReady(true,"Ready"); Control.setVgpReady(true,"Ready");
        Control.connect(); foreground(true); Control.setVgpOverlay(true); Control.setVgpEnabled(true);
        check(Control.vgpAllowed(),"Off-map foreground may run VGP");
        // The former permission gate incorrectly depended on Quick Catch
        // summary + the independent auto-transfer inventory adapter.
        Control.setItemReady(false,"Inventory auto-transfer unavailable");
        Control.configureQuickSummary(false);
        Control.setQuickSummaryReady(false,"Quick Catch unavailable");
        check(Control.vgpAllowed(),
                "VGP must use its own native readiness rather than unrelated transfer/summary readiness");
        check(Control.vgpDiagnosticStatus().contains("habilitada para ações"),
                "Diagnostic must not falsely block VGP on optional modules");
        // An actually unavailable native VGP must still be blocked.
        Control.setVgpReady(false,"Native VGP unavailable");
        check(!Control.vgpAllowed(),"VGP native engine unavailable still blocks actions");
        Control.setVgpReady(true,"Ready");
        check(Control.vgpDiagnosticStatus().contains("habilitada para ações"),"Ready state distinguishes activation from permission");
        Control.setVgpStatus("Initializing encounter support");
        check(Control.vgpDiagnosticStatus().contains("aguardando pré-requisito nativo"),
                "Native prerequisites must not be reported as a working VGP");
        Control.setVgpStatus("Watching nearby Pokemon and PokeStops");
        check(Control.vgpDiagnosticStatus().contains("monitorando"),
                "Scanning alone must not be reported as confirmed catches/spins");
        Control.setVgpStatus("Response timed out; toggle VGP to resume");
        check(Control.vgpDiagnosticStatus().contains("bloqueada"),
                "Uncertain native response must remain a stop-and-restart warning");
        Control.setVgpStatus("Ready");
        Control.setVgpOptions(0);
        check(!Control.vgpDiagnosticStatus().contains("habilitada"),"No actions cannot be reported as ready");
        check(Control.vgpDiagnosticStatus().contains("sem ações"),"Missing catch/spin selections explained");
        Control.setVgpOptions(3);
        foreground(false); check(!Control.vgpAllowed(),"Background blocks permission"); foreground(true);
        Control.setVgpReady(false,"Unavailable"); check(!Control.vgpAllowed(),"Native readiness blocks permission");
        check(Control.vgpDiagnosticStatus().contains("mecanismo nativo indisponível"),"Native readiness failure surfaced");
        Control.setVgpReady(true,"Ready");
        java.lang.reflect.Field desired=Control.class.getDeclaredField("desiredEnabled"); desired.setAccessible(true);
        desired.setBoolean(null,false); check(!Control.vgpAllowed(),"Unknown location blocks permission"); desired.setBoolean(null,true);
        Control.setVgpReady(true,"Ready");
        Cooldown.remaining=600000;
        Control.setVgpEnabled(true);
        check(Control.vgpAllowed() && Control.vgpEnabled(),
                "Estimated global cooldown does not gate VGP permission");

        System.out.println("PASS: VGP simple manual toggle, no cooldown gate, background off, native readiness and marker bounds");
    }
}
