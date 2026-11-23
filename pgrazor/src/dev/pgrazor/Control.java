package dev.pgrazor;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.StrictMode;
import android.os.SystemClock;
import android.util.AtomicFile;
import android.util.Log;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/** Module interface shared by the standalone preview and an instrumented host. */
public final class Control {
    private static Application hostApplication;
    public static boolean eventBoxesAllowed() {
        Application app=hostApplication;
        return app!=null && hostLive() && active() && !catchScreenVisible()
            && dev.pgrazor.events.EventSettings.boxCollectorEnabled(app);
    }
    public static String accountCommand(){return AccountProfiles.command();}
    public static String[] accountCommandSnapshot(){return AccountProfiles.commandSnapshot();}
    public static boolean accountManagementAllowed(){return foreground && backendReady && connected();}
    public static void accountIdentity(String id,String name){AccountProfiles.identity(id,name);}
    public static void setAccountsReady(boolean ready){AccountProfiles.support(ready);}
    public static void accountSessionEnded(){AccountProfiles.sessionEnded();}
    public static void accountLogoutComplete(){AccountProfiles.logoutComplete();}
    public static void accountLogoutFailed(){AccountProfiles.logoutFailed();}
    public static void accountSignInResetComplete(){AccountProfiles.signInResetComplete();}
    public static void accountSessionSaved(){AccountProfiles.sessionSaved();}
    static Activity accountActivity(){return pad==null?null:pad.activity;}
    static void quiesceAccounts() throws IOException {
        EternatusRouteRunner.stop("Troca de conta solicitada.");
        OneShotAutomation.reset();
        AutoRaid.reset();
        AutoMaxBattle.reset();
        GiftBatchSettings.reset();
        gameRoutes.stop("Account switch"); gameRoutesPlanning=false; gameRoutes.setPlanning(false); autoWalkPlanning=false;
        vgp.suspend();
        mapTapEpoch++; invalidateQuickBallOutcome();
        engine.stop(); saveState();
        if(stateWriteFailed)throw new IOException("Location save did not finish");
    }
    static void flushAccountWrites() throws IOException { flushSettings(2500); Cooldown.flush(); }
    public static boolean adventureSyncDismissAllowed() {
        Application app=hostApplication;
        // Spoofing owns this policy. Real Android location grants do not enable Adventure Sync.
        return app!=null && hostLive() && desiredEnabled;
    }
    private static volatile boolean mapPoiOpen,autoWalkPlanning;
    private static volatile String mapPoiSnapshot="";
    private static volatile long mapPoiAt;
    public static void setMapPoiOpen(boolean open){mapPoiOpen=open;if(!open&&!autoWalkPlanning){mapPoiSnapshot="";mapPoiAt=0;}}
    public static void setAutoWalkPlanning(boolean open){autoWalkPlanning=open;if(!open&&!mapPoiOpen){mapPoiSnapshot="";mapPoiAt=0;}}
    public static boolean mapPoiAllowed(){return (mapPoiOpen||autoWalkPlanning)&&hostLive();}
    public static void setMapPoiSnapshot(String value){if((mapPoiOpen||autoWalkPlanning)&&value!=null&&value.length()<160000){mapPoiSnapshot=value;mapPoiAt=SystemClock.elapsedRealtime();}}
    public static String mapPoiSnapshot(){return SystemClock.elapsedRealtime()-mapPoiAt<10000?mapPoiSnapshot:"";}
    public static final MotionEngine engine = new MotionEngine();
    private static final GameRoutesController gameRoutes = new GameRoutesController(
        engine, SystemClock::elapsedRealtime, System::currentTimeMillis);
    private static volatile boolean gameRoutesPlanning;
    private static String gameRoutesLastStatus="";
    private static volatile String gameRoutesReadiness="Waiting for Route support";
    public static void setGameRoutesPlanning(boolean open) { gameRoutesPlanning=open; gameRoutes.setPlanning(open); }
    public static boolean gameRoutesReady() { return gameRoutes.ready(); }
    public static boolean gameRoutesRunning() { return gameRoutes.running(); }
    public static String gameRoutesStatus() { return gameRoutes.ready()?gameRoutes.status():gameRoutesReadiness; }
    public static String gameRoutesSnapshot() { return gameRoutes.snapshot(); }
    public static String gameRoutesLastReward() { return gameRoutes.lastReward(); }
    public static String takeGameRoutesRewardNotice() { return gameRoutes.takeRewardNotice(); }
    public static String gameRoutesDetails() { return gameRoutes.details(); }
    public static String gameRoutesCommand() { return gameRoutes.command(); }
    public static boolean gameRoutesAllowed() { return hostLive() && desiredEnabled && (gameRoutesPlanning || gameRoutes.running()); }
    public static void setGameRoutesReady(boolean value,String reason) {
        gameRoutesReadiness=reason==null||reason.isEmpty()?"Waiting for Route support":reason;
        gameRoutes.setReady(value);
    }
    public static void setGameRoutesSnapshot(String value) { gameRoutes.acceptSnapshot(value); }
    public static void gameRoutesAction(String value) { gameRoutes.acceptAction(value); }
    public static void startGameRoutes(int count) {
        if(!hostLive()||!canEnable())throw new IllegalArgumentException("Location adapter is not ready");
        gameRoutes.start(count); saveState();
    }
    public static void stopGameRoutes() { gameRoutes.stop("Stopped"); saveState(); }
    public static void tickGameRoutes() {
        gameRoutes.tick(hostLive()&&canEnable());
        String status=gameRoutes.status();
        if(!status.equals(gameRoutesLastStatus)){gameRoutesLastStatus=status;event("game-routes",status);}
    }
    public static volatile String connection = "PREVIEW - game not connected";
    public static volatile long latitudeReads, longitudeReads;
    private static PrintWriter log;
    private static File logFile;
    private static long logBytes;
    private static int logSegment;
    private static Pad pad;
    private static boolean registered;
    private static volatile long leaseDeadline;
    private static volatile boolean instrumented;
    private static volatile boolean leaseExpired;
    private static volatile boolean preview = true, backendReady;
    private static volatile boolean desiredEnabled = true;
    private static volatile double[] realLocation;
    private static double realLatitude=Double.NaN,realLongitude=Double.NaN;
    private static volatile boolean foreground;
    private static volatile boolean cameraDiagnosticsConfigured;
    private static volatile boolean cameraDiagnosticRunning;
    private static volatile String cameraDiagnosticStatus = "";
    private static final AtomicBoolean cameraSampleRequest = new AtomicBoolean();
    private static volatile boolean interactionDiagnosticsConfigured, interactionDiagnosticRunning;
    private static volatile String interactionDiagnosticStatus="";
    private static final AtomicBoolean interactionTraceRequest=new AtomicBoolean(), interactionStopRequest=new AtomicBoolean();
    private static final Object quickSummaryLock=new Object();
    private static volatile boolean quickSummaryConfigured, quickSummaryReady, quickSummaryEnabled;
    private static volatile boolean quickSummaryAutoDismiss;
    private static volatile boolean autoTransferEnabled, autoTransferReady;
    private static volatile int transferProtect=255;
    private static volatile int transferIvKeepPercent=90;
    private static volatile String transferStatus="Waiting for transfer support";
    // Custom keep list (canonical, sorted/de-duplicated positive dex IDs) plus the persistent Keep All
    // overlay. Keep All holds every catch regardless of the other rules; the keep list protects the
    // listed species. Both persist; Keep All active state is a normal saved preference (unlike VGP).
    private static volatile String transferKeepSpecies="";
    private static volatile boolean transferKeepAll, transferOverlayEnabled;
    // Evolve before transferring (new catches only). Off by default; requires the auto-transfer
    // master. Mode 0 uses the current transfer rules, 1 restricts the behavior to a separate
    // selected-species list. Stages 1 evolves once, 2 evolves through all available candy-only
    // stages. evolveTransferSpecies is a canonical keep-species string (same validation/format as
    // the custom keep list, but an independent list). Existing favorites/keep/IV/PvP protections
    // always apply; this never auto-enables the master and never changes the keep list.
    private static volatile boolean evolveTransferEnabled;
    private static volatile int evolveTransferMode, evolveTransferStages=1;
    private static volatile String evolveTransferSpecies="";
    private static volatile String quickSummaryStatus="";
    private static final Object quickBallLock=new Object();
    private static volatile boolean quickBallConfigured, quickBallReady, quickBallEnabled;
    private static volatile String quickBallStatus="";
    private static volatile boolean throwReady, throwCurve, throwAr, throwArReady;
    // Independent of throw rating, curveball and Quick Catch. Defaults off.
    private static volatile boolean throwAlwaysHit, throwAlwaysHitReady;
    private static volatile int throwGrade, throwVariety;
    // Optional IV/level overlays rendered by the ball-animation agent. Catch screen
    // defaults ON (prior behavior); owned Pokemon details defaults OFF. ivOwnedReady
    // is runtime only: whether the agent resolved the owned-details metadata.
    private static volatile boolean ivCatchEnabled=true, ivOwnedEnabled, ivOwnedReady;
    // What the encounter and details IV panels show, chosen like storageIvFormat: 1 level and
    // attack/defense/stamina, 2 level and percent, 3 all three. Kept while that display is off.
    private static volatile int ivCatchFormat=2, ivOwnedFormat=2;
    // Pokemon storage list IVs: 0 off, 1 level and attack/defense/stamina, 2 level and percent, 3 all three. storageIvFormat is
    // the chosen format (1-3), kept while the display is off. storageIvStatus is the native module's
    // last error, runtime only.
    private static volatile int storageIvMode, storageIvFormat=3;
    private static volatile String storageIvStatus="";
    private static volatile int mapTapMode, mapTapEpoch;
    private static final MapTapSequence mapTapSequence=new MapTapSequence();
    private static volatile boolean mapTapReady;
    private static volatile boolean spawnBooster,spawnReady;
    private static volatile long spawnRangeAt;
    private static volatile double spawnOriginal,spawnEffective;
    public static void setSpawnRange(double original,double effective){if(!spawnBooster||!spawnAllowed()||!Double.isFinite(original)||!Double.isFinite(effective)||original<=0||effective<original)return;spawnOriginal=original;spawnEffective=effective;spawnRangeAt=SystemClock.elapsedRealtime();}
    public static String spawnStatus(){
        if(!spawnBooster)return "Off";
        if(!spawnAllowed())return "Waiting for map support";
        if(spawnRangeAt==0||SystemClock.elapsedRealtime()-spawnRangeAt>5000)return "Waiting for a range check; move a few steps on the map";
        return String.format(java.util.Locale.US,"Range check: %.0f to %.0f m (local)",spawnOriginal,spawnEffective);
    }
    public static boolean spawnBooster(){return spawnBooster;}
    public static void setSpawnBooster(boolean enabled){spawnRangeAt=0;spawnBooster=enabled;saveSettings();}
    public static void setSpawnReady(boolean ready){spawnReady=ready;if(!ready)spawnRangeAt=0;}
    public static boolean spawnAllowed(){return spawnReady&&quickSummaryReady()&&hostLive();}
    private static volatile int giftOptions;
    private static volatile boolean giftsReady;
    private static volatile String giftStatus="Waiting for gift support";
    public static int giftOptions(){return giftOptions;}
    public static void setGiftOptions(int value){if(value<0||value>7)return;giftOptions=value;saveSettings();}
    public static void setGiftsReady(boolean ready,String status){giftsReady=ready;giftStatus=status;}
    public static void setGiftStatus(String status){giftStatus=status;}
    public static String giftStatus(){return giftStatus;}
    public static boolean giftsAllowed(){return giftsReady&&quickSummaryReady()&&hostLive();}
    private static volatile boolean vgpEnabled,vgpReady,vgpOverlay;
    /** Persistent safety preference; when armed a live cooldown keeps VGP off. */
    // Protect the floating VGP icon from accidental activation (runtime automation is unaffected).
    private static volatile boolean vgpTapProtection=true;
    private static volatile int vgpOptions=3;
    // 0=original, 1=simple, 2=glossy, 3=GO Plus. Visual preference only.
    private static volatile int vgpIconStyle=0;
    private static volatile int vgpIconSize=48;
    private static volatile int vgpIconOpacity=100,vgpBackgroundOpacity=75;
    // Map status captions can be hidden independently of the feature itself.
    // The saved master defaults to visible for compatibility with older installations.
    private static volatile boolean hudLabelsVisible=true;
    // Each caption can be shown/hidden without disabling its underlying feature.
    public static final int HUD_VGP=1,HUD_BUDDY=2,HUD_AUTOMATION=4,HUD_GYM=8,
        HUD_CATCH=16,HUD_ENCOUNTERS=32,HUD_SHINY=64,HUD_COOLDOWN=128,HUD_LOCALTIME=256;
    public static final int HUD_ALL=HUD_VGP|HUD_BUDDY|HUD_AUTOMATION|HUD_GYM|
        HUD_CATCH|HUD_ENCOUNTERS|HUD_SHINY|HUD_COOLDOWN|HUD_LOCALTIME;
    private static volatile int hudLabelsMask=HUD_ALL;
    private static volatile int hudLabelOpacity=0; // legacy fallback only
    private static volatile int hudLabelTextSp=11;
    private static final int[] HUD_BITS={HUD_VGP,HUD_BUDDY,HUD_AUTOMATION,HUD_GYM,HUD_CATCH,
        HUD_ENCOUNTERS,HUD_SHINY,HUD_COOLDOWN,HUD_LOCALTIME};
    private static final int[] HUD_OPACITIES={12,12,12,12,12,12,12,15,12};
    private static final int[] HUD_COLORS={0,0,2,0,1,0,2,1,0};
    private static volatile int hudAppearanceRevision=0;
    // Colour values: vivid green, dark orange, hot pink, and red, all with a dark text shadow.
    public static final int[] HUD_PALETTE={0xff00da69,0xffff8500,0xffff1493,0xffff3535};
    public static final String[] HUD_PALETTE_NAMES={"Verde","Laranja forte","Rosa choque","Vermelho"};
    private static int hudIndex(int bit){for(int i=0;i<HUD_BITS.length;i++)if(HUD_BITS[i]==bit)return i;return -1;}
    public static int hudAppearanceRevision(){return hudAppearanceRevision;}
    public static int hudBackgroundOpacity(int bit){int index=hudIndex(bit);return index<0?0:HUD_OPACITIES[index];}
    public static int hudPaletteIndex(int bit){int index=hudIndex(bit);return index<0?0:HUD_COLORS[index];}
    public static int hudLabelColor(int bit){return HUD_PALETTE[hudPaletteIndex(bit)];}
    public static void setHudBackgroundOpacity(int bit,int value){
        int index=hudIndex(bit);if(index<0)return;
        HUD_OPACITIES[index]=Math.max(0,Math.min(100,value));hudAppearanceRevision++;saveSettings();
    }
    public static void setHudPaletteIndex(int bit,int value){
        int index=hudIndex(bit);if(index<0||value<0||value>=HUD_PALETTE.length)return;
        HUD_COLORS[index]=value;hudAppearanceRevision++;saveSettings();
    }
    public static boolean hudLabelsVisible(){return hudLabelsMask!=0;}
    public static boolean hudLabelVisible(int bit){return bit>0&&(bit&(bit-1))==0&&(hudLabelsMask&bit)!=0;}
    public static void setHudLabelVisible(int bit,boolean visible){
        if(bit<=0||(bit&(bit-1))!=0||(bit&HUD_ALL)==0)return;
        hudLabelsMask=visible?hudLabelsMask|bit:hudLabelsMask&~bit;
        hudLabelsVisible=hudLabelsMask!=0;saveSettings();
    }
    public static void setHudLabelsVisible(boolean value){
        hudLabelsVisible=value;hudLabelsMask=value?HUD_ALL:0;saveSettings();
    }
    public static int hudLabelOpacity(){return hudLabelOpacity;}
    public static void setHudLabelOpacity(int value){hudLabelOpacity=Math.max(0,Math.min(100,value));saveSettings();}
    public static int hudLabelTextSp(){return hudLabelTextSp;}
    public static void setHudLabelTextSp(int value){hudLabelTextSp=Math.max(9,Math.min(14,value));saveSettings();}
    /** Manual VGP control: visual preferences persist; active flag never persists. */
    private static final VgpController vgp = new VgpController(
        () -> vgpEnabled, value -> vgpEnabled=value,
        () -> vgpOverlay, value -> vgpOverlay=value,
        () -> foreground, () -> vgpOptions,
        Control::event, Control::saveSettings);
    public static boolean vgpEnabled(){return vgpOverlay&&vgpEnabled;}
    public static boolean vgpOverlay(){return vgpOverlay;}
    public static void setVgpOverlay(boolean value){vgp.setOverlay(value);}
    public static boolean vgpOverlayVisible(){return vgpOverlay&&hostLive();}
    public static int vgpOptions(){return vgpOptions;}
    public static int vgpIconStyle(){return vgpIconStyle;}
    public static void setVgpIconStyle(int value){if(value<0||value>3)return;vgpIconStyle=value;saveSettings();}
    public static int vgpIconOpacity(){return vgpIconOpacity;}
    public static void setVgpIconOpacity(int percent){vgpIconOpacity=Math.max(10,Math.min(100,percent));saveSettings();}
    public static int vgpBackgroundOpacity(){return vgpBackgroundOpacity;}
    public static void setVgpBackgroundOpacity(int percent){vgpBackgroundOpacity=Math.max(0,Math.min(100,percent));saveSettings();}
    public static int vgpIconSize(){return vgpIconSize;}
    public static void setVgpIconSize(int value){vgpIconSize=Math.max(28,Math.min(72,value));saveSettings();}
    /** Legacy API retained for older modules. VGP no longer stops on estimated cooldown. */
    public static boolean vgpStopOnCooldown(){return false;}
    public static boolean vgpTapProtection(){return vgpTapProtection;}
    public static void setVgpTapProtection(boolean value){if(vgpTapProtection!=value){vgpTapProtection=value;saveSettings();}}
    /** User-controlled start/stop; no dependency on estimated cooldown. */
    public static void setVgpEnabled(boolean value){vgp.setEnabled(value);}
    /** Legacy compatibility setter: always disabled, intentionally not persisted. */
    public static void setVgpStopOnCooldown(boolean value){}
    public static void setVgpOptions(int value){vgpOptions=value&7;saveSettings();}
    public static void setVgpReady(boolean ready,String status){vgpReady=ready;vgp.setStatus(status);}
    public static void setVgpStatus(String status){vgp.setStatus(status);}
    /** The overlay/foreground refusal reason is shown ahead of generic native status. */
    public static String vgpStatus(){return vgp.status();}
    /**
     * Human-readable permission diagnosis. Showing the floating icon does NOT turn VGP on.
     * No native calls or automatic retries here: this reads the same gates as vgpAllowed().
     * Keep vgpStatus() unchanged for the runtime protocol and existing tests.
     */
    public static String vgpDiagnosticStatus(){
        if(!vgpOverlay)return "VGP oculta: ative Exibir botão VGP. Isso não inicia a automação.";
        if(!vgpEnabled()){
            String status=vgp.status();
            if(status.contains("foreground")||status.contains("primeiro plano"))
                return "VGP pausada: "+status+"; reative manualmente quando estiver liberada.";
            return "VGP desligada: toque em Iniciar VGP+ ou no ícone flutuante para começar.";
        }
        if((vgpOptions&3)==0)return "VGP ligada, mas sem ações: marque Pokémon e/ou Poképaradas.";
        if(!foreground)return "VGP aguardando: abra o jogo em primeiro plano.";
        if(!backendReady||!connected())return "VGP aguardando: conexão com o jogo ou lease expirado.";
        if(!AccountProfiles.allowed())return "VGP aguardando: conta em validação ou troca.";
        if(!locationKnown())return "VGP aguardando: localização ainda não disponível.";
        // VGP has its own native discovery, inventory/balls admission and action
        // readiness checks. Quick Catch summary and auto-transfer are optional,
        // independent features: they must not veto Pokémon/Stop scanning.
        if(!vgpReady)return "VGP aguardando: mecanismo nativo indisponível. "+vgp.status();
        if(ShundoSniper.active())return "VGP aguardando: Shundo Sniper tem prioridade.";
        if(AutoRaid.running())return "VGP aguardando: Auto Raid tem prioridade.";
        String nativeState=vgp.status();
        if(nativeState==null)nativeState="";
        String nativeLower=nativeState.toLowerCase(java.util.Locale.ROOT);
        if(nativeLower.contains("response timed out") ||
           nativeLower.contains("native vgp error") ||
           nativeLower.contains("paused: invalid vgp status"))
            return "VGP bloqueada por resposta incerta/erro nativo. Pare a VGP+, "
                +"aguarde a ação pendente terminar e inicie-a novamente manualmente. "
                +"Os alvos recentes permanecem protegidos. Estado: "+nativeState;
        if(nativeLower.startsWith("waiting for") ||
           nativeLower.startsWith("initializing") ||
           nativeLower.contains("unavailable") ||
           nativeLower.contains("out of red") ||
           nativeLower.contains("no regular balls") ||
           nativeLower.contains("storage full") ||
           nativeLower.contains("item bag full") ||
           nativeLower.contains("catching disabled"))
            return "VGP ligada, mas aguardando pré-requisito nativo: "+nativeState
                +". Confira conexão, inventário ou suporte indicado.";
        if(nativeLower.startsWith("watching") ||
           nativeLower.startsWith("spinning") ||
           nativeLower.startsWith("catching") ||
           nativeLower.startsWith("checking") ||
           nativeLower.startsWith("cleaning"))
            return "VGP ligada e monitorando. Estado: "+nativeState
                +". Ações só são confirmadas pelos resultados retornados.";
        return "VGP habilitada para ações. Estado: "+nativeState;
    }
    public static void setVgpTarget(boolean visible,double x,double y,boolean spin){vgp.setTarget(visible,x,y,spin);}
    public static boolean vgpMapVisible(){long age=SystemClock.elapsedRealtime()-buddyMapSample;return buddyMapSample>0&&age>=0&&age<1500;}
    // The same icon marks a Pokemon being caught by a map tap, which works with VGP+ off as well.
    public static boolean vgpTargetVisible(){return vgp.targetVisible(vgpAllowed()||mapCatching(),vgpMapVisible());}
    private static boolean mapCatching(){return autoThrow&&autoThrowOnMap&&hostLive()&&mapCatchStatus.catching(SystemClock.elapsedRealtime());}
    public static float vgpTargetX(){return vgp.targetX();}
    public static float vgpTargetY(){return vgp.targetY();}
    public static boolean vgpTargetSpin(){return vgp.targetSpin();}

    public static boolean vgpAllowed(){
        if(!locationKnown())return false;
        // VGP owns native inventory checks and action-gate checks independently.
        // itemAccessAllowed() also requires Quick Catch summary and auto-transfer
        // readiness, neither of which is a prerequisite for VGP capture/spin.
        return vgpEnabled&&vgpReady&&hostLive()
                &&!ShundoSniper.active()&&!AutoRaid.running()&&!AutoMaxBattle.running();
    }  // Shundo Sniper and Auto Raid still pause VGP+
    public static void vgpResult(String action,boolean success){vgp.result(action,success);}
    private static volatile boolean autoIncubate, incubateLongest, incubatorReady;
    private static volatile String incubatorStatus="Waiting for incubation support";
    public static boolean autoIncubate(){return autoIncubate;}
    public static boolean incubateLongest(){return incubateLongest;}
    public static void setAutoIncubate(boolean v){autoIncubate=v;saveSettings();}
    public static void setIncubateLongest(boolean v){incubateLongest=v;saveSettings();}
    public static void setIncubatorReady(boolean ready,String status){incubatorReady=ready;incubatorStatus=status;}
    public static void setIncubatorStatus(String status){incubatorStatus=status;}
    public static String incubatorStatus(){return incubatorStatus;}
    public static boolean incubatorAvailable(){return incubatorReady&&itemAccessAllowed();}
    public static boolean incubatorAllowed(){long age=SystemClock.elapsedRealtime()-buddyMapSample;return autoIncubate&&incubatorAvailable()&&buddyMapSample>0&&age>=0&&age<1500;}
    private static volatile int buddyBerry;
    private static volatile boolean buddyReady;
    private static volatile long buddyMapSample;
    private static volatile String buddyStatus="Waiting for buddy support";
    public static int buddyBerry(){return buddyBerry;}
    public static void setBuddyBerry(int id){if(id!=0&&id!=701&&id!=703&&id!=705&&id!=706&&id!=707)return;buddyBerry=id;saveSettings();}
    public static void setBuddyReady(boolean ready,String status){buddyReady=ready;buddyStatus=status;}
    public static void setBuddyStatus(String status){buddyStatus=status;}
    public static String buddyStatus(){return buddyStatus;}
    public static boolean buddyAvailable(){return buddyReady&&itemAccessAllowed();}
    public static boolean buddySamplingRequested(){return (buddyBerry!=0||vgpEnabled||autoIncubate||healEnabled||ShundoSniper.active()||GymAutomationSettings.enabled()||BatchEvolutionSettings.enabled()||OneShotAutomation.enabled())&&hostLive();}
    // Legacy wire slot remains for ABI compatibility; Buddy Emotion owns all feeding.
    public static boolean buddyAllowed(){return false;}
    private static volatile boolean encounterBerryActive,encounterBerryBusy;
    private static volatile long encounterBerrySeen;
    public static void setEncounterBerry(boolean active,boolean busy){encounterBerryActive=active;encounterBerryBusy=busy;encounterBerrySeen=SystemClock.elapsedRealtime();}
    public static boolean catchScreenVisible(){long age=SystemClock.elapsedRealtime()-encounterBerrySeen;return encounterBerryActive&&age>=0&&age<2000&&foreground&&quickSummaryReady();}
    // Wrap SmartCatchPreferences' version-4 line as version 6: append the Go Wild November 2026
    // "Use Safari Balls in VGP+" flag (0/1). The flag applies only to Mighty Pokemon and is validated
    // independently by the native smart-catch owner, which falls back to the normal VGP+ ball rules
    // when a permitted Safari Ball is unavailable. Null host application => flag off. Then append the
    // VGP+ catch speed (0 normal, 1 fast, 2 fastest), then the catch-only-shinies flag (0/1). Version 7 is
    // always emitted.
    public static String smartCatchConfig(){
        String base=SmartCatchSettings.config();
        if(base.startsWith("4;"))base="7;"+base.substring(2);
        Application app=hostApplication;
        return base+";"+(app!=null&&dev.pgrazor.events.EventSettings.wildVgpSafari(app)?1:0)+";"+SmartCatchSettings.speed()+";"+(SmartCatchSettings.shinyOnly()?1:0);
    }
    public static String gymAutomationConfig(){return GymAutomationSettings.config();}
    public static String buddyEmotionConfig(){return BuddyEmotionSettings.config();}
    public static boolean automationReady(){return hostLive()&&active()&&itemAccessAllowed();}
    public static boolean automationPlanning(){return autoWalkPlanning||gameRoutesPlanning;}
    public static String oneShotAutomationConfig(){String raid=AutoRaid.config();if(raid!=null)return raid;String gift=GiftBatchSettings.config();return gift!=null?gift:OneShotAutomation.config();}
    public static String batchEvolutionConfig(){return BatchEvolutionSettings.config();}
    public static String researchConfig(){return ResearchSettings.config();}
    public static String shundoConfig(){return ShundoSniper.config();}
    public static String shinyScanConfig(){return ShinyScanner.config();}
    /** Where the found shiny is on the screen (0..1 each way), from the native scanner ten times a second. */
    public static void setShinyTarget(boolean visible,double x,double y){ShinyScanner.setTarget(visible,x,y);}
    public static String buddyEmotionHistory(){return BuddyEmotionSettings.history();}
    public static boolean encounterBerryVisible(){return encounterBerryEnabled&&catchScreenVisible();}
    public static boolean encounterBerryBusy(){return encounterBerryBusy;}
    private static volatile boolean cooldownOverlay;
    private static volatile String cooldownEncounter="";
    public static boolean cooldownOverlay(){return cooldownOverlay;}
    public static void setCooldownOverlay(boolean value){cooldownOverlay=value;saveSettings();}
    // Small draggable overlay showing the local wall-clock time at the spoofed coordinates (never the
    // device time). Off by default; the overlay view reads engine coordinates and formats time offline.
    private static volatile boolean localTimeOverlay;
    private static volatile int localTimeFormat, cooldownTextSp;
    private static volatile boolean cooldownEmphasis;
    public static boolean cooldownEmphasis() { return cooldownEmphasis; }
    public static void setCooldownEmphasis(boolean enabled) {
        if (cooldownEmphasis == enabled) return;
        cooldownEmphasis = enabled;
        hudAppearanceRevision++;
        saveSettings();
    }
    public static int cooldownTextSp() { return cooldownTextSp; }
    public static void setCooldownTextSp(int value) {
        if ((value != 0 && (value < 9 || value > 20)) || value == cooldownTextSp) return;
        cooldownTextSp = value;
        hudAppearanceRevision++;
        saveSettings();
    }
    public static int localTimeFormat() { return localTimeFormat; }
    public static void setLocalTimeFormat(int value) {
        if (value < 0 || value > 6 || value == localTimeFormat) return;
        localTimeFormat = value;
        saveSettings();
    }
    public static boolean localTimeOverlay(){return localTimeOverlay;}
    public static void setLocalTimeOverlay(boolean value){localTimeOverlay=value;saveSettings();}
    // Encounter counter (rolling 24 hours, per account) and its optional map badge.
    private static volatile boolean encounterCounter=true,encounterOverlay;
    public static boolean encounterCounter(){return encounterCounter;}
    public static void setEncounterCounter(boolean value){encounterCounter=value;saveSettings();}
    public static boolean encounterOverlay(){return encounterOverlay;}
    public static void setEncounterOverlay(boolean value){encounterOverlay=value;saveSettings();}
    public static void setCooldownEncounter(String value){cooldownEncounter=value==null?"":value.toLowerCase(java.util.Locale.US);}
    // Rate-limited diagnostics only; keep all existing safety guards.
    private static long lastCooldownSkipAt;
    private static String lastCooldownSkip="";
    private static synchronized boolean logCooldownSkipNeeded(String reason){
        long now=SystemClock.elapsedRealtime();
        if(reason.equals(lastCooldownSkip)&&now-lastCooldownSkipAt<15000)return false;
        lastCooldownSkip=reason;lastCooldownSkipAt=now;return true;
    }
    private static void cooldownSkipped(String action,String reason){
        if(logCooldownSkipNeeded(reason))event("cooldown-action-skipped","action="+action+" reason="+reason);
    }
    public static void cooldownAction(String action){
        if(action==null||action.isEmpty())return;
        if(!foreground){cooldownSkipped(action,"background");return;}
        if(!backendReady){cooldownSkipped(action,"backend-not-ready");return;}
        if(!connected()){cooldownSkipped(action,"adapter-lease-expired");return;}
        if(action.equals("Ball throw")||action.equals("Ball drop")||action.equals("Berry feeding")){
            boolean wild=cooldownEncounter.contains("wild"),raid=cooldownEncounter.contains("raid");
            if(!wild&&!(raid&&!action.equals("Ball throw"))){cooldownSkipped(action,"encounter-ineligible");return;}
        }
        Cooldown.record(action);
    }
    private static volatile String berryPriority="";
    private static volatile boolean encounterBerryEnabled;
    private static volatile boolean berryReady;
    /** Effective choice the native feeder reads: 0 (no feeding) whenever the overlay master is off. */
    public static int autoBerry(){return encounterBerryEnabled?autoBerrySelected():0;}
    /** Raw user selection shown in the overlay; retained while the master is off so it can be re-enabled. */
    public static int autoBerrySelected(){int[] ids=BerryPriority.parse(berryPriority);for(int id:ids)if(berryCount(id)>0)return id;return ids.length==0?0:ids[0];}
    public static String berryPriority(){return berryPriority;}
    public static String effectiveBerryPriority(){return encounterBerryEnabled?berryPriority:"";}
    public static boolean encounterBerryEnabled(){return encounterBerryEnabled;}
    public static void setEncounterBerryEnabled(boolean enabled){encounterBerryEnabled=enabled;saveSettings();event("encounter-berry-overlay",enabled?"ON":"OFF; feeding stopped, choice retained");}
    public static void setAutoBerry(int id){if(id!=0&&!BerryPriority.validId(id))return;berryPriority=id==0?"":String.valueOf(id);saveSettings();}
    public static void selectBerryPriority(int id){if(id!=0&&!BerryPriority.validId(id))return;berryPriority=BerryPriority.select(berryPriority,id);saveSettings();}
    public static void setBerryReady(boolean ready){berryReady=ready;}
    public static boolean berriesAllowed(){return berryReady&&itemAccessAllowed();}
    public static int berryCount(int id){for(String row:itemCatalog.split("\n")){String[] b=row.split("\\|",-1);try{if(b.length==3&&Integer.parseInt(b[0])==id)return Integer.parseInt(b[2]);}catch(NumberFormatException ignored){}}return 0;}
    private static volatile int battleOptions;
    private static volatile boolean battlesReady;
    private static volatile String battleStatus="Waiting for battle support";
    /** Temporary Max session only augments runtime options; saved settings stay unchanged. */
    public static int battleOptions(){return battleOptions|AutoMaxBattle.optionMask();}
    public static String battleStatus(){return battleStatus;}
    public static void setBattleOptions(int value){if(value<0||value>1023)return;battleOptions=value;saveSettings();event("battle-options",String.valueOf(value));}
    public static void setBattlesReady(boolean ready,String status){battlesReady=ready;battleStatus=status;if(!ready)AutoMaxBattle.reset();}
    public static void setBattleStatus(String status){battleStatus=status;}
    public static boolean battlesAllowed(){return battlesReady&&quickSummaryReady()&&hostLive();}
    private static volatile boolean gymRepeatEnabled;
    public static boolean gymRepeatEnabled(){return gymRepeatEnabled;}
    public static void setGymRepeatEnabled(boolean enabled){gymRepeatEnabled=enabled;saveSettings();event("gym-repeat",enabled?"ON":"OFF");}
    private static volatile int rocketOptions;
    private static volatile boolean rocketReady;
    private static volatile String rocketStatus="Waiting for battle support";
    public static int rocketOptions(){return rocketOptions;}
    public static String rocketStatus(){return rocketStatus;}
    /** PGSharp-compatible shared fast/charged/shield preferences across Rocket and PvP.
     *  Runtime scope guards remain independent; this never opens a PvP game hook. */
    public static void setRocketOptions(int value){
        if(!SharedBattleOptions.validActions(value))return;
        if(rocketOptions==value && (pvpBattleOptions&PvpBattleMode.ACTIONS)==value)return;
        rocketOptions=value;
        pvpBattleOptions=SharedBattleOptions.pvpMask(value,pvpBattleOptions);
        saveSettings();
        event("rocket-options",String.valueOf(value));
        event("pvp-battle-options",String.valueOf(pvpBattleOptions));
    }
    public static void setRocketReady(boolean ready,String status){rocketReady=ready;rocketStatus=status;}
    public static void setRocketStatus(String status){rocketStatus=status;}
    public static boolean rocketAllowed(){return rocketReady&&quickSummaryReady()&&hostLive();}
    // Shared action preferences; PvP-only super-effective preference stays independent.
    // A PvP gameplay hook has not been attached or validated yet.
    private static volatile int pvpBattleOptions;
    private static volatile boolean pvpBattleReady;
    private static volatile String pvpBattleStatus="Assistente PvP indisponível: aguardando controlador compatível";
    public static int pvpBattleOptions(){return pvpBattleOptions;}
    public static boolean pvpBattleReady(){return pvpBattleReady;}
    public static String pvpBattleStatus(){return pvpBattleStatus;}
    public static void setPvpBattleOptions(int options){
        if(options!=PvpBattleMode.sanitize(options))return;
        int shared=SharedBattleOptions.actionMask(options);
        if(pvpBattleOptions==options && rocketOptions==shared)return;
        rocketOptions=shared;
        pvpBattleOptions=options;
        saveSettings();
        event("rocket-options",String.valueOf(shared));
        event("pvp-battle-options",String.valueOf(options));
    }
    public static boolean pvpBattleAllowed(){
        return PvpBattleMode.allowed(pvpBattleOptions,pvpBattleReady,quickSummaryReady(),hostLive());
    }
    private static volatile boolean itemTransferEnabled,itemTransferReady;
    private static volatile String itemRules="",itemLimits="",itemCatalog="",itemStatus="Waiting for item bag";
    public static boolean itemTransferEnabled(){return itemTransferEnabled;}
    public static String itemRules(){return itemRules;}
    public static String itemLimits(){return itemLimits;}
    public static String itemCatalog(){return itemCatalog;}
    public static String itemStatus(){return itemStatus;}
    public static boolean itemAccessAllowed(){return itemTransferReady&&quickSummaryReady()&&hostLive();}
    private static volatile long itemCatalogUntil,berryCatalogUntil;
    public static void requestItemCatalog(){itemCatalogUntil=SystemClock.elapsedRealtime()+5000;}
    public static void requestBerryCatalog(){berryCatalogUntil=SystemClock.elapsedRealtime()+5000;}
    /** 0: idle, 1: five berry counts only, 2: explicit inventory settings catalog. */
    public static int itemCatalogMode(){
        if(!hostLive())return 0;
        long now=SystemClock.elapsedRealtime();
        if(now<itemCatalogUntil)return 2;
        return now<berryCatalogUntil||encounterBerryVisible()?1:0;
    }
    public static void setItemReady(boolean ready,String status){
        boolean changed=itemTransferReady!=ready;
        itemTransferReady=ready;itemStatus=status;
        if(changed)event("gameplay-readiness","component=item-bag ready="+ready);
    }
    public static void setItemStatus(String status){itemStatus=status==null?"":status;}
    public static void setItemCatalog(String value){
        if(value==null||value.length()>100000)return;
        TreeMap<Integer,String> rows=new TreeMap<>();
        for(String row:itemCatalog.split("\n")){String[] bits=row.split("\\|",-1);try{if(bits.length==3)rows.put(Integer.parseInt(bits[0]),bits[0]+"|"+bits[1]+"|0");}catch(NumberFormatException ignored){}}
        for(String row:value.split("\n")){String[] bits=row.split("\\|",-1);try{if(bits.length==3&&Integer.parseInt(bits[0])>0&&Integer.parseInt(bits[2])>=0)rows.put(Integer.parseInt(bits[0]),row);}catch(NumberFormatException ignored){}}
        itemCatalog=String.join("\n",rows.values());itemStatus="Ready: "+rows.size()+" recyclable item types";
    }
    private static boolean validItemRules(String value){
        if(value==null||value.length()>30000)return false;HashSet<Integer> seen=new HashSet<>();
        for(String row:value.split(";")){if(row.isEmpty())continue;if(!row.matches("[0-9]{1,7}:[0-9]{1,6}"))return false;String[] b=row.split(":");int id=Integer.parseInt(b[0]),cap=Integer.parseInt(b[1]);if(id<=0||cap>100000||!seen.add(id))return false;}return true;
    }
    public static void saveItemRules(boolean enabled,String rules,String limits){
        if(!validItemRules(rules)||!validItemRules(limits))throw new IllegalArgumentException("Invalid item limit");
        itemRules=rules;itemLimits=limits;itemTransferEnabled=enabled;saveSettings();event("item-rules",enabled?"Enabled":"Disabled");
    }
    private static volatile boolean quickSpinEnabled, quickSpinReady;
    // Optional event route behavior. Both are opt-in, persisted, and never imply a candy reward.
    private static volatile boolean eternatusWithoutVgp, eternatusTimedAdvance;
    private static volatile String quickSpinStatus="Waiting for game";
    public static boolean quickSpinEnabled() { return quickSpinEnabled; }
    public static boolean eternatusWithoutVgp(){return eternatusWithoutVgp;}
    public static void setEternatusWithoutVgp(boolean value){
        if(eternatusWithoutVgp!=value){eternatusWithoutVgp=value;saveSettings();}
    }
    public static boolean eternatusTimedAdvance(){return eternatusTimedAdvance;}
    public static void setEternatusTimedAdvance(boolean value){
        if(eternatusTimedAdvance!=value){eternatusTimedAdvance=value;saveSettings();}
    }
    public static String quickSpinStatus() { return quickSpinStatus; }
    public static void setQuickSpinEnabled(boolean value) { quickSpinEnabled=value; saveSettings(); event("quick-spin-setting",value?"ON":"OFF"); }
    public static void setQuickSpinReady(boolean value,String status) { quickSpinReady=value; quickSpinStatus=status; }
    public static boolean quickSpinAllowed() { return quickSpinEnabled && quickSpinReady && quickSummaryReady() && hostLive(); }
    private static volatile boolean cameraSteeringConfigured;
    private static final CameraHeadingState cameraHeading = new CameraHeadingState();
    private static final Handler main = new Handler(Looper.getMainLooper());
    private static final CatchOutcomeController catchOutcome = new CatchOutcomeController(
        main, () -> pad==null ? null : pad.activity,
        Control::quickBallAllowed, Control::autoTransferAllowed,
        detail -> event("quick-ball-toast",detail));
    private static final CatchOutcomeController mapThrowOutcome = new CatchOutcomeController(
        main, () -> pad==null ? null : pad.activity,
        () -> autoThrow() && autoThrowOnMap() && hostLive(), () -> false,
        detail -> event("map-throw-toast",detail));
    private static final Object stateLock = new Object();
    private static final Object logLock = new Object();
    private static AtomicFile stateFile, settingsFile;
    private static String lastSaved;
    private static long lastCheckpoint;
    private static boolean stateWriteFailed;
    private static final AtomicBoolean checkpointPending = new AtomicBoolean();
    private static final ExecutorService stateWriter = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "pgrazor-state-writer"); thread.setDaemon(true); return thread;
    });
    // One daemon worker per burst: SettingsWriter only schedules a drain when idle, so at most one
    // drain writes at a time, and it exits once the queue empties. Nothing permanent blocks JVM exit.
    private static final Executor settingsIo = task -> {
        Thread thread = new Thread(task, "pgrazor-settings-writer"); thread.setDaemon(true); thread.start();
    };
    private static SettingsWriter settingsWriter;
    // Explicit export/import block until durable; a shorter bound keeps a background flush from stalling.
    private static final long SETTINGS_FLUSH_MS = 5000, SETTINGS_BACKGROUND_FLUSH_MS = 1000;
    public static void embedded(String status) {
        setGameRoutesReady(false,"Waiting for game");
        engine.pause(); preview = false; backendReady = false; connection = status;
        setQuickSummaryReady(false,"Waiting for catch summary support");
        setQuickBallReady(false,"Waiting for ball animation support");
        invalidateCameraHeading("Adapter initializing");
        cameraSampleRequest.set(false);
        cameraDiagnosticRunning = false;
        saveState();
    }
    public static boolean preview() { return preview; }
    /** Saved user choices survive readiness and lifecycle changes. */
    public static void configureQuickSummary(boolean configured) {
        boolean wasReady;
        synchronized(quickSummaryLock) {
            wasReady=quickSummaryReady;
            quickSummaryConfigured=configured; quickSummaryReady=false; autoTransferReady=false;
            quickSummaryStatus=configured ? "Checking catch summary support" : "";
        }
        if(wasReady)event("gameplay-readiness","component=catch-summary ready=false reason=reconfigured configured="+configured);
    }
    public static boolean quickSummaryConfigured() { return quickSummaryConfigured; }
    public static void configureQuickSummaryAutoDismiss(boolean enabled) { quickSummaryAutoDismiss=enabled; }
    public static boolean quickSummaryAutoDismiss() { return quickSummaryAutoDismiss; }
    public static boolean quickSummaryReady() { return quickSummaryConfigured && quickSummaryReady; }
    public static boolean quickSummaryEnabled() { return quickSummaryEnabled; }
    public static String quickSummaryStatus() { return quickSummaryStatus; }
    public static void setQuickSummaryReady(boolean ready,String status) {
        boolean disabled,changed,actual,configured;
        synchronized(quickSummaryLock) {
            boolean previous=quickSummaryReady;
            quickSummaryReady=quickSummaryConfigured && ready;
            changed=previous!=quickSummaryReady;actual=quickSummaryReady;configured=quickSummaryConfigured;
            if(!quickSummaryReady)autoTransferReady=false;
            disabled=quickSummaryEnabled && !quickSummaryReady;

            String text=status==null || status.length()==0 ? (quickSummaryReady ? "Ready" : "Unavailable") : status;
            quickSummaryStatus=quickSummaryConfigured ? text.substring(0,Math.min(text.length(),320)) : "";
        }
        if(changed)event("gameplay-readiness","component=catch-summary ready="+actual+" configured="+configured);
        if(disabled)event("quick-summary-setting","Suspended; saved choice retained");
    }
    public static void setQuickSummaryEnabled(boolean enabled) {
        boolean changed,actual;
        synchronized(quickSummaryLock) {
            actual=enabled;
            changed=quickSummaryEnabled!=actual; quickSummaryEnabled=actual;
        }
        // One Quick Catch choice controls dismissal and the 20x animation.
        setQuickBallEnabled(actual);
        if(changed)event("quick-summary-setting",actual ? (quickSummaryAutoDismiss ? "ON; summary and details auto-dismiss; new-species screens remain" : "ON; shorter post-catch wait; summary stays open") : "OFF");
    }
    public static boolean quickSummaryAllowed() {
        return quickSummaryEnabled && quickSummaryReady && hostLive();
    }
    public static boolean autoTransferEnabled(){return autoTransferEnabled;}
    public static boolean autoTransferReady(){return quickSummaryReady()&&autoTransferReady;}
    public static int transferProtect(){return transferProtect;}
    public static final int[] TRANSFER_IV_CHOICES={90,95,97,98,99,100,101};
    public static int transferIvKeepPercent(){return transferIvKeepPercent;}
    public static void setTransferIvKeepPercent(int value){
        boolean valid=value>=0&&value<=101;
        if(!valid||value==transferIvKeepPercent)return;
        transferIvKeepPercent=value;invalidateQuickBallOutcome();saveSettings();
        event("transfer-iv-limit",value==101?"Keep only protected or selected Pokemon":"Keep IV >= "+value+"%");
    }
    /** Original native int reader, with unused high bits carrying the strict IV limit+1. */
    public static int nativeTransferProtect(){return transferProtect|((transferIvKeepPercent+1)<<8);}
    public static String transferStatus(){return transferStatus;}
    public static void setAutoTransferEnabled(boolean enabled){invalidateQuickBallOutcome();autoTransferEnabled=enabled;saveSettings();event("auto-transfer-setting",enabled?"ON; keep IV >= "+transferIvKeepPercent:"OFF");}
    public static void setTransferProtect(int mask){transferProtect=mask>=0&&mask<=255?mask:255;saveSettings();event("auto-transfer-protection",String.valueOf(transferProtect));}
    public static void setAutoTransferReady(boolean ready,String status){autoTransferReady=ready&&quickSummaryReady();transferStatus=status==null?"":status;}
    public static void transferResult(String status){transferStatus=status==null?"":status;}
    /** Main transfer permission; manual Keep All is applied only by the encounter consumer. */
    public static boolean autoTransferAllowed(){return autoTransferEnabled&&autoTransferReady()&&hostLive();}
    /** Canonical keep list: sorted, de-duplicated positive dex IDs (1..2000), comma-separated; empty when none. */
    public static String transferKeepSpecies(){return transferKeepSpecies;}
    public static void setTransferKeepSpecies(String value){
        boolean[] damaged={false};
        String canonical=normalizeKeepSpecies(value,damaged);
        if(damaged[0]){event("transfer-keep-list","Invalid selection ignored; existing keep list preserved");return;}
        if(canonical.equals(transferKeepSpecies))return;
        transferKeepSpecies=canonical;saveSettings();
        event("transfer-keep-species",(canonical.isEmpty()?0:canonical.split(",").length)+" species kept");
    }
    // Evolve before transferring. All setters ignore invalid input; a corrupt loaded config disables
    // the feature (see loadSettings). The feature never enables the auto-transfer master on its own.
    public static boolean evolveTransferEnabled(){return evolveTransferEnabled;}
    public static int evolveTransferMode(){return evolveTransferMode;}
    public static int evolveTransferStages(){return evolveTransferStages;}
    /** Canonical selected-species list: sorted, de-duplicated positive dex IDs; empty when none. Independent of the keep list. */
    public static String evolveTransferSpecies(){return evolveTransferSpecies;}
    public static void setEvolveTransferEnabled(boolean enabled){
        if(enabled==evolveTransferEnabled)return;
        // Never flips the auto-transfer master; the UI explains the master is required.
        evolveTransferEnabled=enabled;saveSettings();
        event("evolve-transfer",enabled?"ON; new catches only; requires auto transfer master":"OFF");
    }
    public static void setEvolveTransferMode(int mode){
        if(mode!=0&&mode!=1||mode==evolveTransferMode)return;
        evolveTransferMode=mode;saveSettings();
        event("evolve-transfer-mode",mode==1?"Selected species only":"Current transfer rules");
    }
    public static void setEvolveTransferStages(int stages){
        if(stages!=1&&stages!=2||stages==evolveTransferStages)return;
        evolveTransferStages=stages;saveSettings();
        event("evolve-transfer-stages",stages==2?"All available stages":"Once");
    }
    public static void setEvolveTransferSpecies(String value){
        boolean[] damaged={false};
        String canonical=normalizeKeepSpecies(value,damaged);
        if(damaged[0]){event("evolve-transfer-species","Invalid selection ignored; existing list preserved");return;}
        if(canonical.equals(evolveTransferSpecies))return;
        evolveTransferSpecies=canonical;saveSettings();
        event("evolve-transfer-species",(canonical.isEmpty()?0:canonical.split(",").length)+" species selected");
    }
    /** "1|enabled|mode|stages|species" for the native evolve-before-transfer path (transport slot 22). */
    public static String evolveTransferConfig(){return "1|"+(evolveTransferEnabled?1:0)+"|"+evolveTransferMode+"|"+evolveTransferStages+"|"+evolveTransferSpecies;}
    /** Keep All holds manual encounter catches; background catches use the main settings. */
    public static boolean transferKeepAll(){return transferKeepAll;}
    public static void setTransferKeepAll(boolean value){
        if(value==transferKeepAll)return;
        // Keep All never enables the auto-transfer master; turning it off only resumes the saved rules.
        transferKeepAll=value;invalidateQuickBallOutcome();saveSettings();
        event("transfer-keep-all",value?"ON; manual catches kept":"OFF; manual transfer rules resume; master unchanged");
    }
    public static boolean transferOverlayEnabled(){return transferOverlayEnabled;}
    public static void setTransferOverlayEnabled(boolean value){
        if(value==transferOverlayEnabled)return;
        transferOverlayEnabled=value;saveSettings();event("transfer-overlay",value?"ON":"OFF");
    }
    public static boolean transferOverlayVisible(){return transferOverlayEnabled&&autoTransferEnabled&&catchScreenVisible()&&backendReady&&connected();}
    /**
     * Canonical keep-species string from arbitrary input: positive dex IDs 1..2000, de-duplicated and
     * sorted, bounded to 2000 entries and 10000 characters. Invalid or out-of-range tokens are dropped;
     * when {@code damaged} is supplied, {@code damaged[0]} is set true if any token was rejected or a
     * bound was exceeded, so a loader can fail safe rather than trust a partially valid list.
     */
    private static String normalizeKeepSpecies(String raw,boolean[] damaged){
        boolean bad=false;
        if(raw==null){bad=true;raw="";}
        if(raw.length()>10000){bad=true;raw=raw.substring(0,10000);}
        java.util.TreeSet<Integer> ids=new java.util.TreeSet<>();
        for(String token:raw.isEmpty()?new String[0]:raw.split(",",-1)){
            if(!token.matches("[1-9][0-9]{0,3}")){bad=true;continue;}
            int id;
            try{id=Integer.parseInt(token);}catch(NumberFormatException invalid){bad=true;continue;}
            if(id<1||id>2000){bad=true;continue;}
            if(ids.size()>=2000){bad=true;break;}
            if(!ids.add(id))bad=true;
        }
        StringBuilder out=new StringBuilder();
        for(int id:ids){if(out.length()>0)out.append(',');out.append(id);}
        if(damaged!=null)damaged[0]=bad;
        return out.toString();
    }
    // IV/level overlay preferences. The stored choice is surfaced to the UI and the
    // agent; the agent itself fails closed when owned-details metadata is absent.
    // "Hide overlay" (Pad) makes the game look stock: the in-game IV text is off while it is set. These
    // getters feed the native transport; the saved preferences are unchanged.
    private static volatile boolean overlaysHidden;
    public static boolean overlaysHidden(){return overlaysHidden;}
    public static void setOverlaysHidden(boolean hidden){overlaysHidden=hidden;}
    public static boolean ivCatchEnabled(){return ivCatchEnabled&&!overlaysHidden;}
    public static boolean ivOwnedEnabled(){return ivOwnedEnabled&&!overlaysHidden;}
    public static boolean ivOwnedReady(){return ivOwnedReady;}
    public static boolean ivDisplayAllowed(){return quickBallReady&&hostLive();}
    public static void setIvCatchEnabled(boolean enabled){ivCatchEnabled=enabled;saveSettings();event("iv-catch-overlay",enabled?"ON":"OFF");}
    public static void setIvOwnedEnabled(boolean enabled){ivOwnedEnabled=enabled;saveSettings();event("iv-owned-overlay",enabled?"ON":"OFF");}
    public static void setIvOwnedReady(boolean ready){ivOwnedReady=ready;}
    public static String ivDisplayStatus(){return (quickBallConfigured&&ivOwnedEnabled&&!ivOwnedReady)?"Details overlay unsupported on this app version":"";}
    public static int storageIvMode(){return storageIvMode;}
    public static void setStorageIvMode(int mode){storageIvMode=mode>=0&&mode<=4?mode:0;if(storageIvMode>0)storageIvFormat=storageIvMode;saveSettings();event("storage-iv-mode",""+storageIvMode);}
    public static boolean storageIvEnabled(){return storageIvMode!=0;}
    public static int storageIvFormat(){return storageIvFormat;}
    public static void setStorageIvEnabled(boolean enabled){setStorageIvMode(enabled?storageIvFormat:0);}
    public static void setStorageIvFormat(int format){
        storageIvFormat=format>=1&&format<=4?format:3;
        if(storageIvMode!=0)setStorageIvMode(storageIvFormat);else saveSettings();
    }
    // One tap-to-cycle mode per screen: 0 off, else the format above.
    public static int ivCatchMode(){return ivCatchEnabled?ivCatchFormat:0;}
    public static int ivOwnedMode(){return ivOwnedEnabled?ivOwnedFormat:0;}
    // Format 4 is all three plus the best PvP rank (on the storage list, on a second row).
    public static void setIvCatchMode(int mode){if(mode>=1&&mode<=4)ivCatchFormat=mode;setIvCatchEnabled(mode>=1&&mode<=4);}
    public static void setIvOwnedMode(int mode){if(mode>=1&&mode<=4)ivOwnedFormat=mode;setIvOwnedEnabled(mode>=1&&mode<=4);}
    /** Off, then all three with PvP, all three, level and percent, level and attack/defense/stamina, back to off. */
    public static int nextIvMode(int mode){return (mode+4)%5;}
    // "S2|<storage mode>|<encounter format>|<details format>"; whether the encounter and details
    // panels show at all travels separately as ivCatchEnabled/ivOwnedEnabled.
    public static String storageIvConfig(){return "S2|"+(overlaysHidden?0:storageIvMode)+"|"+ivCatchFormat+"|"+ivOwnedFormat;}
    public static String storageIvStatus(){return storageIvMode==0?"":storageIvStatus;}
    // Catch chance overlay (on by default) and auto ball (off by default) for manual encounters.
    // autoBallTarget is the chance the cheapest ball must reach, in percent.
    private static volatile boolean catchChanceShown=true, autoBall;
    private static volatile int autoBallTarget=75;
    static final int[] AUTO_BALL_TARGETS={50,65,75,85,95};
    // Auto Throw (off by default) uses the current Throws preferences and selected ball,
    // with Always Hit intrinsic. Auto-Raid keeps its own Excellent/curve policy.
    // Persisted; surfaced to the native owner via C4. Never uses a Master Ball.
    private static volatile boolean autoThrow, autoThrowOnMap;
    public static boolean catchChanceShown(){return catchChanceShown;}
    public static void setCatchChanceShown(boolean shown){catchChanceShown=shown;saveSettings();event("catch-chance-overlay",shown?"ON":"OFF");}
    public static boolean autoBall(){return autoBall;}
    public static void setAutoBall(boolean on){autoBall=on;saveSettings();event("auto-ball",on?"ON":"OFF");}
    public static int autoBallTarget(){return autoBallTarget;}
    public static void setAutoBallTarget(int percent){autoBallTarget=percent>=1&&percent<=99?percent:75;saveSettings();}
    public static boolean autoThrow(){return autoThrow;}
    public static void setAutoThrow(boolean on){autoThrow=on;if(!on){mapThrowOutcome.invalidate();mapCatchStatus.clear();}saveSettings();event("auto-throw",on?"ON":"OFF");}
    public static boolean autoThrowOnMap(){return autoThrowOnMap;}
    public static void setAutoThrowOnMap(boolean on){autoThrowOnMap=on;if(!on){mapThrowOutcome.invalidate();mapCatchStatus.clear();}saveSettings();event("auto-throw-map",on?"ON":"OFF");}
    /**
     * "C4|show|autoBall|target|berry|safari|autoThrow|cooldownMs|onMap". The first six fields keep their C2
     * meaning: berry is the one PGBR's berry picker will feed on the next throw (0 when the picker is
     * off); the game only sees it at release, so the chance needs it from here. While the overlay is
     * hidden the chance line is off; auto ball keeps running. The safari flag (0/1) is the Go Wild
     * November 2026 "Use Safari Balls in Auto Ball" toggle; it applies only to Mighty Pokemon and the
     * native owner falls back to the normal Auto Ball rules when a permitted Safari Ball is unavailable
     * (null host application => flag off). autoThrow (0/1) is the Auto Throw preference. cooldownMs is
     * the live throw cooldown remaining in milliseconds (Math.max(0, Cooldown.remaining())). onMap applies only while Auto Throw is enabled. C4 is always emitted.
     */
    public static String catchChanceConfig(){
        Application app=hostApplication;
        boolean safari=app!=null&&dev.pgrazor.events.EventSettings.wildAutoBallSafari(app);
        long cooldownMs=Math.max(0,Cooldown.remaining());
        return "C4|"+(catchChanceShown&&!overlaysHidden?1:0)+"|"+(autoBall?1:0)+"|"+autoBallTarget+"|"+autoBerry()+"|"+(safari?1:0)+"|"+(autoThrow?1:0)+"|"+cooldownMs+"|"+(autoThrowOnMap?1:0);
    }
    // PvP keep for auto-transfer, per league (0 Great, 1 Ultra, 2 Master): a catch is kept when its
    // stat-product rank is at or below that league's rank for a species in PvPoke's top PVP_META.
    // pvpKeepLeagues has a bit per league that is on; each league keeps its rank while off.
    static final String[] PVP_LEAGUES={"Great League","Ultra League","Master League"};
    static final String[] PVP_RANK_KEYS={"pvpGreatRank","pvpUltraRank","pvpMasterRank"};
    static final int[] PVP_RANKS={1,10,25,50,100}, PVP_DEFAULT_RANKS={50,10,10};
    static final int PVP_META=150;
    private static volatile int pvpKeepLeagues=3;
    private static final int[] pvpRanks=PVP_DEFAULT_RANKS.clone();
    public static boolean pvpKeep(int league){return league>=0&&league<3&&(pvpKeepLeagues&(1<<league))!=0;}
    public static void setPvpKeep(int league,boolean on){if(league<0||league>=3)return;pvpKeepLeagues=on?pvpKeepLeagues|(1<<league):pvpKeepLeagues&~(1<<league);saveSettings();event("pvp-keep",PVP_LEAGUES[league]+(on?" ON":" OFF"));}
    public static int pvpRank(int league){return league>=0&&league<3?pvpRanks[league]:0;}
    public static void setPvpRank(int league,int rank){if(league<0||league>=3)return;pvpRanks[league]=rank>=1&&rank<=4096?rank:PVP_DEFAULT_RANKS[league];saveSettings();}
    /** "P2|great|ultra|master|meta" for the native transfer check (transport slot 16); 0 = league off. */
    public static String pvpKeepConfig(){return "P2|"+(pvpKeep(0)?pvpRanks[0]:0)+"|"+(pvpKeep(1)?pvpRanks[1]:0)+"|"+(pvpKeep(2)?pvpRanks[2]:0)+"|"+PVP_META;}
    // Skip cutscenes (off by default): the native module bypasses the Pokemon cutscenes and the Team GO
    // Rocket dialogue so they are never drawn. cutsceneSkipStatus is the module's last error, if any.
    private static volatile boolean skipCutscenes;
    private static volatile String cutsceneSkipStatus="";
    public static boolean skipCutscenes(){return skipCutscenes;}
    public static void setSkipCutscenes(boolean on){skipCutscenes=on;saveSettings();event("skip-cutscenes",on?"ON":"OFF");}
    public static String cutsceneSkipStatus(){return skipCutscenes?cutsceneSkipStatus:"";}
    /** Raw settings for slot 17; native egg presentation uses skipCutscenes OR autoIncubate. */
    public static String cutsceneSkipConfig(){return "K2|"+(skipCutscenes?1:0)+"|"+(autoIncubate?1:0);}
    private static volatile boolean routeGiftExchange;
    public static boolean routeGiftExchange(){return routeGiftExchange;}
    public static void setRouteGiftExchange(boolean on){routeGiftExchange=on;saveSettings();event("route-gift-setting",on?"ON":"OFF");}
    /** Raw setting for transport string slot 24 (native route_gift). */
    public static String routeGiftConfig(){return "M1|"+(routeGiftExchange?1:0);}
    // Battery usage (each off by default): the native module draws the 3D view at a lower resolution, caps
    // the game at 20 fps, and turns off shadows, anti-aliasing and far detail, putting the game's own
    // values back when a setting is turned off.
    private static volatile boolean batteryLowRes, batteryFps20, batteryLowQuality;
    public static boolean batteryLowRes(){return batteryLowRes;}
    public static boolean batteryFps20(){return batteryFps20;}
    public static boolean batteryLowQuality(){return batteryLowQuality;}
    public static void setBatteryLowRes(boolean on){batteryLowRes=on;saveSettings();event("battery","lower resolution "+(on?"ON":"OFF"));}
    public static void setBatteryFps20(boolean on){batteryFps20=on;saveSettings();event("battery","20 fps cap "+(on?"ON":"OFF"));}
    public static void setBatteryLowQuality(boolean on){batteryLowQuality=on;saveSettings();event("battery","low graphics quality "+(on?"ON":"OFF"));}
    /** "B2|lowRes|fps20|lowQuality|blackScreen" for the native graphics module (transport slot 20). */
    public static String batteryConfig(){return "B2|"+(batteryLowRes?1:0)+"|"+(batteryFps20?1:0)+"|"+(batteryLowQuality?1:0)+"|"+(ShundoSniper.dark()?1:0);}
    // Smart healing (off by default): the native module heals with the cheapest mix of potions and
    // revives. healWhen: 0 after battles, 1 periodically, 2 both. healThreshold: heal below this percent
    // of max HP, 100 = any missing HP. healStatus is the module's last problem or this session's items.
    public static final String[] HEAL_WHEN_LABELS={"After battles","Periodically","After battles & periodically"};
    private static volatile boolean healEnabled,healRevive=true,healFull;
    private static volatile int healWhen,healThreshold=50;
    private static volatile String healStatus="";
    public static boolean healEnabled(){return healEnabled;}
    public static void setHealEnabled(boolean on){healEnabled=on;if(!on)healStatus="";saveSettings();event("smart-heal",(on?"ON ":"OFF ")+healConfig());}
    public static int healWhen(){return healWhen;}
    public static void setHealWhen(int when){healWhen=when>=0&&when<=2?when:0;saveSettings();event("smart-heal","settings "+healConfig());}
    public static int healThreshold(){return healThreshold;}
    /** Next threshold in the cycle 25 -> 50 -> 75 -> 100 (Always) -> 25. */
    public static void cycleHealThreshold(){healThreshold=healThreshold>=100?25:healThreshold+25;saveSettings();event("smart-heal","settings "+healConfig());}
    public static boolean healRevive(){return healRevive;}
    public static void setHealRevive(boolean on){healRevive=on;saveSettings();event("smart-heal","settings "+healConfig());}
    /** Heal fully: use whatever items it takes, instead of saving big potions and revives for big wounds. */
    public static boolean healFull(){return healFull;}
    public static void setHealFull(boolean on){healFull=on;saveSettings();event("smart-heal","settings "+healConfig());}
    public static String healStatus(){return healEnabled?healStatus:"";}
    // Smart battling: one switch, off by default. Native smart_battle.cpp owns all of the behaviour.
    private static volatile boolean smartBattle;
    // One persistent preference for all battle modes; native selection is not yet wired.
    private static volatile boolean smartAttack;
    // Independent timing options: only indices 0..2 are selectable; legacy 3 migrates to default.
    // Defaults intentionally replace LT038's fixed 1.7 s shield / 2.3 s fainted choice.
    private static volatile int smartShieldTiming=1, smartFaintedSwapTiming=1;
    private static volatile String smartBattleStatus="";
    public static boolean smartBattle(){return smartBattle;}
    public static boolean smartAttack(){return smartAttack;}
    public static int smartShieldTiming(){return smartShieldTiming;}
    public static int smartFaintedSwapTiming(){return smartFaintedSwapTiming;}
    public static void setSmartShieldTiming(int choice){
        if(choice<0||choice>2||smartShieldTiming==choice)return;
        smartShieldTiming=choice;saveSettings();
        event("smart-shield-timing",String.valueOf(choice));
    }
    public static void setSmartFaintedSwapTiming(int choice){
        if(choice<0||choice>2||smartFaintedSwapTiming==choice)return;
        smartFaintedSwapTiming=choice;saveSettings();
        event("smart-fainted-swap-timing",String.valueOf(choice));
    }
    public static void setSmartAttack(boolean on){
        if(smartAttack==on)return;
        smartAttack=on;
        // Retain the legacy PvP preference without modifying fast/charged/shield bits.
        pvpBattleOptions=PvpBattleMode.update(pvpBattleOptions,PvpBattleMode.PREFER_SUPER,on);
        saveSettings();
        event("smart-attack",on?"PREFERENCE_ON":"PREFERENCE_OFF");
    }
    public static void setSmartBattle(boolean on){smartBattle=on;if(!on)smartBattleStatus="";saveSettings();event("smart-battle",on?"ON":"OFF");}
    public static String smartBattleStatus(){return smartBattle?smartBattleStatus:"";}
    /** "F3|smart-battle|shield-choice|fainted-swap-choice|smart-attack" (transport slot 21).
     *  Native readers still accept the older F2 snapshot with Smart Attack off. */
    public static String smartBattleConfig(){
        return "F3|"+(smartBattle?1:0)+"|"+smartShieldTiming+"|"+smartFaintedSwapTiming+"|"+(smartAttack?1:0);
    }
    /** "H2|enabled|when|threshold|revive|full" for the native healer (transport slot 18). */
    public static String healConfig(){return "H2|"+(healEnabled?1:0)+"|"+healWhen+"|"+healThreshold+"|"+(healRevive?1:0)+"|"+(healFull?1:0);}
    public static void configureQuickBall(boolean configured) {
        synchronized(quickBallLock) {
            quickBallConfigured=configured; quickBallReady=false; ivOwnedReady=false; setThrowReady(false);setMapTapReady(false);
            quickBallStatus=configured ? "Checking successful ball animation support" : "";
        }
        invalidateQuickBallOutcome();
    }
    public static boolean quickBallConfigured() { return quickBallConfigured; }
    public static boolean quickBallReady() { return quickBallConfigured && quickBallReady; }
    public static boolean quickBallEnabled() { return quickBallEnabled; }
    public static String quickBallStatus() { return quickBallStatus; }
    public static void setQuickBallReady(boolean ready,String status) {
        boolean disabled;
        synchronized(quickBallLock) {
            quickBallReady=quickBallConfigured && ready;
            disabled=quickBallEnabled && !quickBallReady;
            if(!quickBallReady){setThrowReady(false);setMapTapReady(false);}
            String text=status==null || status.length()==0 ? (quickBallReady ? "Ready" : "Unavailable") : status;
            quickBallStatus=quickBallConfigured ? text.substring(0,Math.min(text.length(),320)) : "";
        }
        if(!ready)invalidateQuickBallOutcome();
        if(disabled)event("quick-ball-setting","Suspended; saved choice retained");
    }
    public static void setQuickBallEnabled(boolean enabled) {
        boolean changed,actual;
        synchronized(quickBallLock) {
            actual=enabled;
            changed=quickBallEnabled!=actual; quickBallEnabled=actual;
        }
        if(!actual)invalidateQuickBallOutcome();
        saveSettings();
        if(changed)event("quick-ball-setting",actual ? "ON; experimental 20x catch and break-free ball animation; summary screens remain" : "OFF");
    }
    public static boolean quickBallAllowed() {
        return quickBallEnabled && quickBallReady && hostLive();
    }
    public static boolean mapTapReady() { return quickBallReady() && mapTapReady; }
    public static void setMapTapReady(boolean ready) { mapTapReady=ready; if(!ready)mapTapEpoch++; }
    public static int mapTapMode() { return mapTapMode; }
    public static int mapTapEpoch() { return mapTapEpoch; }
    public static void setMapTapMode(int mode) { mapTapEpoch++; mapTapMode=mode>=0 && mode<=3 ? mode : 0; saveSettings(); event("map-tap-mode",""+mapTapMode); }
    public static boolean mapTapAllowed() { return desiredEnabled && mapTapMode!=0 && mapTapReady() && hostLive(); }
    public static void applyMapTap(double latitude,double longitude,int mode,int epoch,double sampledAt) {
        double age=SystemClock.elapsedRealtime()-sampledAt;
        if(!Double.isFinite(age)||age<0||age>750)return;
        if(!Double.isFinite(latitude)||!Double.isFinite(longitude)||Math.abs(latitude)>90||Math.abs(longitude)>180)return;
        final long received=SystemClock.elapsedRealtime();
        main.post(() -> {
            if(!mapTapAllowed()||mode!=mapTapMode||epoch!=mapTapEpoch||SystemClock.elapsedRealtime()-received>750)return;
            if(engine.activeRouteToken()!=0){
                mapTapEpoch++; // discard queued taps and any pre-dialog double-tap pairing
                if(pad!=null)pad.confirmStopRoute();
                return; // the accidental tap never becomes a walk or teleport
            }
            boolean wantsTeleport=mode==2 || (mode==3 && mapTapSequence.doubleTap(latitude,longitude,sampledAt,epoch,android.view.ViewConfiguration.getDoubleTapTimeout()));
            if(wantsTeleport){if(pad!=null)pad.confirmMapTeleport(latitude,longitude,mode,epoch);return;}
            if(mode==1||mode==3)walkTo(latitude,longitude);else return;
            event("map-tap-applied",String.format(Locale.US,"mode=%d lat=%.7f lon=%.7f",mode,latitude,longitude));
        });
    }
    public static boolean throwReady() { return quickBallReady() && throwReady; }
    public static void setThrowReady(boolean ready) { throwReady=ready; }
    /** The grade the native throw layer applies: the rating 0-3, or 4-6 while throw variety is on. */
    public static int throwGrade() { return throwVariety>0 ? 3+throwVariety : throwGrade; }
    /** The Throw rating setting: 0 off, 1 Nice, 2 Great, 3 Excellent. */
    public static int throwRating() { return throwGrade; }
    /** Throw variety: 0 off, 1 Excellent & Great, 2 Excellent, Great & Nice, 3 all (with a plain throw). */
    public static int throwVariety() { return throwVariety; }
    public static final String[] THROW_VARIETY_LABELS={"Off","Excellent & Great","Excellent, Great & Nice","All"};
    public static boolean throwCurve() { return throwCurve; }
    public static boolean throwAr() { return throwAr; }
    public static boolean throwArReady() { return throwArReady && throwReady(); }
    public static void setThrowArReady(boolean value) { throwArReady=value; }
    public static void setThrowAr(boolean value) { throwAr=value; saveSettings(); }

    public static void setThrowGrade(int grade) { throwGrade=grade>=0 && grade<=3 ? grade : 0; saveSettings(); event("throw-grade",""+throwGrade); }
    public static void setThrowVariety(int variety) { throwVariety=variety>=0 && variety<=3 ? variety : 0; saveSettings(); event("throw-variety",THROW_VARIETY_LABELS[throwVariety]); }
    public static void setThrowCurve(boolean enabled) { throwCurve=enabled; saveSettings(); event("throw-curve",""+throwCurve); }
    public static boolean throwAlwaysHitReady() { return throwReady() && throwAlwaysHitReady; }
    public static void setThrowAlwaysHitReady(boolean ready) { throwAlwaysHitReady=ready; }
    public static boolean throwAlwaysHit() { return throwAlwaysHit; }
    public static void setThrowAlwaysHit(boolean enabled) { throwAlwaysHit=enabled; saveSettings(); event("throw-always-hit",""+throwAlwaysHit); }
    public static boolean throwAllowed() { return throwReady() && hostLive(); }
    /** Visible text for a natively confirmed outcome; anything else is rejected. */
    static String quickBallOutcomeText(String outcome) { return CatchOutcomeController.text(outcome); }
    /**
     * Queues one short post-catch toast; the queue, its cancellation token and the main-thread
     * display gate are owned by {@link CatchOutcomeController}. A newer outcome or any OFF,
     * readiness, background or connection change discards queued work via {@link #invalidateQuickBallOutcome()}.
     */
    public static boolean quickBallOutcome(String outcome) { return catchOutcome.request(outcome); }
    private static void invalidateQuickBallOutcome() { catchOutcome.invalidate(); mapThrowOutcome.invalidate(); mapCatchStatus.clear(); }
    public static void configureInteractionDiagnostics(boolean configured) {
        interactionDiagnosticsConfigured=configured; interactionDiagnosticRunning=false;
        interactionTraceRequest.set(false); interactionStopRequest.set(false);
        interactionDiagnosticStatus=configured ? "Catch/spin diagnostics: checking metadata shortly" : "";
    }
    public static boolean interactionDiagnosticsConfigured() { return interactionDiagnosticsConfigured; }
    public static boolean interactionDiagnosticsReady() { return interactionDiagnosticsConfigured && hostLive(); }
    public static boolean interactionDiagnosticRunning() { return interactionDiagnosticRunning; }
    public static boolean interactionTracePending() { return interactionTraceRequest.get(); }
    public static String interactionDiagnosticStatus() { return interactionDiagnosticStatus; }
    public static void interactionDiagnostic(String text) {
        if (!interactionDiagnosticsConfigured) return;
        interactionDiagnosticStatus=text==null ? "" : text.substring(0,Math.min(text.length(),320));
    }
    public static void setInteractionDiagnosticRunning(boolean running) { interactionDiagnosticRunning=running && interactionDiagnosticsConfigured; }
    public static boolean requestInteractionTrace() {
        if (!interactionDiagnosticsReady() || interactionDiagnosticRunning || interactionTraceRequest.get()) return false;
        engine.stop(); saveState(); interactionStopRequest.set(false); interactionTraceRequest.set(true);
        interactionDiagnosticStatus="Capture requested; waiting for observer";
        event("interaction-trace-requested","Stationary capture for60s; fasterSummary="+quickSummaryEnabled());
        return true;
    }
    public static boolean takeInteractionTraceRequest() { return interactionTraceRequest.getAndSet(false) && interactionDiagnosticsReady() && !interactionDiagnosticRunning; }
    public static void requestInteractionStop() { interactionTraceRequest.set(false); interactionStopRequest.set(true); }
    public static boolean takeInteractionStopRequest() { return interactionStopRequest.getAndSet(false) || (interactionDiagnosticRunning && !interactionDiagnosticsReady()); }
    public static boolean desiredEnabled() { return desiredEnabled; }
    public static synchronized void setLocationSpoofing(boolean enabled) {
        gameRoutes.stop("Location mode changed");
        desiredEnabled=enabled; if(!enabled){realLocation=null;realLatitude=Double.NaN;realLongitude=Double.NaN;} mapTapEpoch++; autoWalkPlanning=false;
        engine.pause(); invalidateCameraHeading("Location mode changed");
        restoreRuntime("location-setting"); saveSettings(); saveState();
        event("location-spoofing",enabled?"ON":"OFF; using device location");
    }
    public static synchronized void observeRealCoordinate(boolean latitude,double value) {
        if(desiredEnabled||!Double.isFinite(value)||Math.abs(value)>(latitude?90:180))return;
        if(latitude)realLatitude=value;else realLongitude=value;
        if(Double.isFinite(realLatitude)&&Double.isFinite(realLongitude))
            realLocation=new double[]{realLatitude,realLongitude};
    }
    public static boolean locationKnown(){return desiredEnabled||realLocation!=null;}
    public static double currentLatitude(){double[] fix=realLocation;return desiredEnabled?engine.latitude:fix==null?Double.NaN:fix[0];}
    public static double currentLongitude(){double[] fix=realLocation;return desiredEnabled?engine.longitude:fix==null?Double.NaN:fix[1];}
    public static void configureCameraSteering(boolean configured) {
        synchronized (cameraHeading) {
            cameraSteeringConfigured=configured;
            cameraHeading.invalidate(); stopManualMotion();
        }
    }
    public static boolean cameraSteeringConfigured() { return cameraSteeringConfigured; }
    /** Camera-relative is the only joystick mode; unavailable headings stop input. */
    public static boolean cameraRelative() { return true; }
    public static boolean cameraSteeringRequested() {
        return cameraSteeringConfigured && hostLive();
    }
    public static void updateCameraHeading(double yawDegrees, int cameraId, boolean mapVisible, long ageMillis) {
        synchronized (cameraHeading) {
            long now=SystemClock.elapsedRealtime();
            long sampledAt=ageMillis>=0 && ageMillis<=now ? now-ageMillis : -1;
            updateCameraHeadingLocked(yawDegrees,cameraId,mapVisible,sampledAt,now);
        }
    }
    /** Native CLOCK_BOOTTIME timestamp, rounded down; includes bridge and heading-lock delay. */
    public static void updateCameraHeadingAt(double yawDegrees, int cameraId, boolean mapVisible, long sampledAtMillis) {
        synchronized (cameraHeading) {
            updateCameraHeadingLocked(yawDegrees,cameraId,mapVisible,sampledAtMillis,SystemClock.elapsedRealtime());
        }
    }
    private static void updateCameraHeadingLocked(double yawDegrees,int cameraId,boolean mapVisible,long sampledAtMillis,long now) {
        buddyMapSample=mapVisible&&sampledAtMillis>0&&sampledAtMillis<=now&&now-sampledAtMillis<1500?sampledAtMillis:0;
        // Expiry is checked before replacement, even if no UI tick observed the gap.
        CameraHeadingState.Sample previous=cameraHeading.sample(now);
        boolean accepted=cameraSteeringRequested() && cameraHeading.updateAt(yawDegrees,cameraId,mapVisible,sampledAtMillis,now);
        if (!accepted) cameraHeading.invalidate();
        CameraHeadingState.Sample current=cameraHeading.sample(now);
        if (cameraRelative() && (!previous.valid || !current.valid || previous.generation!=current.generation)) stopManualMotion();
    }
    public static void invalidateCameraHeading(String reason) {
        buddyMapSample=0;
        synchronized (cameraHeading) {
            cameraHeading.invalidate();
            if (cameraRelative()) stopManualMotion();
        }
    }
    private static void stopManualMotion() { engine.stopManualMotion(); }
    static long beginCameraGesture() {
        synchronized (cameraHeading) {
            return cameraSteeringRequested() ? cameraHeading.beginGesture(SystemClock.elapsedRealtime()) : -1;
        }
    }
    /** Heading validation and input assignment share a lock with asynchronous invalidation. */
    static boolean applyCameraInput(long gesture, double right, double up, long now) {
        synchronized (cameraHeading) {
            CameraSteering.Direction direction=cameraHeading.direction(gesture,right,up,now);
            if (!cameraSteeringRequested() || !direction.valid) { stopManualMotion(); return false; }
            engine.input(direction.east,direction.north); return true;
        }
    }
    static String cameraSteeringStatus() {
        synchronized (cameraHeading) {
            CameraHeadingState.Sample sample=cameraHeading.sample(SystemClock.elapsedRealtime());
            if (!cameraSteeringRequested() || !sample.valid) {
                if (cameraRelative()) stopManualMotion();
                return "Waiting for camera; release and drag again";
            }
            return String.format(Locale.US,"Camera heading %.0f\u00b0; up follows the view",sample.heading);
        }
    }
    public static void configureCameraDiagnostics(boolean configured) {
        cameraDiagnosticsConfigured = configured;
        cameraSampleRequest.set(false);
        cameraDiagnosticRunning = false;
        cameraDiagnosticStatus = configured ? "Camera diagnostic ready" : "";
    }
    public static boolean cameraDiagnosticsConfigured() { return cameraDiagnosticsConfigured; }
    public static boolean cameraDiagnosticsReady() { return cameraDiagnosticsConfigured && hostLive(); }
    public static boolean cameraSamplePending() { return cameraSampleRequest.get() || cameraDiagnosticRunning; }
    public static boolean cameraDiagnosticRunning() { return cameraDiagnosticRunning; }
    public static void setCameraDiagnosticRunning(boolean running) { cameraDiagnosticRunning = running && cameraDiagnosticsConfigured; }
    public static String cameraDiagnosticStatus() { return cameraDiagnosticStatus; }
    public static boolean requestCameraSample() {
        if (!cameraDiagnosticsReady() || cameraSamplePending()) return false;
        if (engine.walking) event("walk-cancelled", "Camera diagnostic requested");
        engine.stop(); saveState();
        cameraDiagnosticStatus = "Camera sample requested (10s)";
        cameraSampleRequest.set(true);
        event("camera-sample-requested", "Stationary sample; desiredEnabled=" + desiredEnabled + "; location override preference unchanged");
        return true;
    }
    public static boolean takeCameraSampleRequest() {
        boolean requested = cameraSampleRequest.getAndSet(false);
        return requested && cameraDiagnosticsReady() && !cameraDiagnosticRunning;
    }
    public static void cameraDiagnostic(String text) {
        if (!cameraDiagnosticsConfigured) return;
        if (text == null || text.length() == 0) text = "Camera diagnostic ready";
        cameraDiagnosticStatus = text.length() > 320 ? text.substring(0, 320) : text;
    }
    public static boolean canEnable() { return desiredEnabled && connected() && (preview || backendReady); }
    public static void connect() {
        engine.pause();
        invalidateCameraHeading("Adapter reconnecting");
        preview = false; backendReady = true;
        instrumented = true;
        connection = "Java location adapter - game acceptance unverified";
        renewLease();
        main.post(() -> restoreRuntime("backend-ready"));
    }
    public static void renewLease() {
        leaseDeadline = android.os.SystemClock.elapsedRealtime() + 8000;
        if(leaseExpired) {
            leaseExpired=false; connection="Java location adapter - game acceptance unverified";
            main.post(() -> { restoreRuntime("heartbeat-restored"); event("heartbeat-restored","Saved preference restored stationary; desiredEnabled="+desiredEnabled); });
        }
    }
    public static void checkLease() {
        if(!connected() && !leaseExpired) { leaseExpired=true; cameraSampleRequest.set(false); invalidateQuickBallOutcome(); invalidateCameraHeading("Connection lost"); engine.pause(); saveState(); connection="Adapter heartbeat late - suspended"; event("heartbeat-expired","Runtime suspended; saved enabled preference unchanged"); }
    }
    public static boolean connected() { return !instrumented || android.os.SystemClock.elapsedRealtime() <= leaseDeadline; }
    /** Shared host gate: foreground, with a ready backend whose connection lease is still live. */
    private static boolean hostLive() { return foreground && backendReady && connected() && AccountProfiles.allowed(); }
    public static synchronized void begin(Application app) {
        if (registered) return;
        registered = true;
        hostApplication=app;
        vgp.setApplication(app);
        try {
            File dir = new File(app.getFilesDir(), "pgrazor-logs");
            if (!dir.isDirectory() && !dir.mkdirs()) throw new IOException("Cannot create log folder");
            logFile = new File(dir, new SimpleDateFormat("yyyyMMdd-HHmmss-SSS", Locale.US).format(new Date()) + ".txt");
            log = new PrintWriter(new FileOutputStream(logFile), true);
        } catch (IOException error) { Log.e("PGBR", "File logging unavailable", error); }
        // Recover recent run tails after a crash without changing or deleting private originals.
        new Thread(() -> recoverRunLogs(app),"pgrazor-log-recovery").start();
        event("session-start", connection);
        AccountProfiles.init(app);
        loadState(app);
        loadSettings(app);
        Cooldown.init(app);
        app.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            public void onActivityResumed(Activity activity) { GoogleBrowserAuth.onHostResumed(activity); attach(activity); }
            public void onActivityPaused(Activity activity) {
                GoogleBrowserAuth.onHostPaused(activity);
                // Paused is not necessarily hidden: pop-up / multi-window resize can
                // leave the game visible and its GPX walk in progress.
                // Tear down only if the Activity is explicitly finishing.
                if (activity.isFinishing() && pad != null && pad.activity == activity) {
                    pad.close();pad=null;
                }
            }
            public void onActivityDestroyed(Activity activity) {
                if (pad != null && pad.activity == activity) { pad.close(); pad = null; }
            }
            public void onActivityCreated(Activity a, Bundle b) {}
            public void onActivityStarted(Activity a) {}
            public void onActivityStopped(Activity a) {
                // Actual loss of visibility: stop automation, VGP and route as before.
                if(pad!=null&&pad.activity==a){pad.close();pad=null;}
            }
            public void onActivitySaveInstanceState(Activity a, Bundle b) {}
        });
    }
    public static void attach(Activity activity) {
        if (activity instanceof LogActivity || activity instanceof GoogleAuthActivity) return;
        if (activity.isFinishing() || activity.isDestroyed()) return;
        if (pad != null && pad.activity == activity) return;
        if (pad != null) pad.close();
        foreground = true;
        restoreRuntime("foreground");
        try {
            pad = new Pad(activity);
            event("panel-ready", activity.getClass().getName());
            UpdateChecker.checkAutomatically(activity);
        }
        catch (RuntimeException error) { suspendForBackground(); event("panel-error", error.toString()); }
    }
    public static void disconnect() {
        engine.pause(); backendReady = false;
        setQuickSummaryReady(false,"Catch summary support disconnected");
        setQuickBallReady(false,"Ball animation support disconnected");
        invalidateCameraHeading("Adapter disconnected");
        cameraSampleRequest.set(false);
        cameraDiagnosticRunning = false;
        if (cameraDiagnosticsConfigured) cameraDiagnosticStatus = "Camera diagnostic unavailable: adapter disconnected";
        saveState();
        connection = "DISCONNECTED - location override off";
        event("disconnected", "Runtime suspended; saved enabled preference unchanged");
    }

    static void autoRaidTravel(long session,long travel,double latitude,double longitude) {
        synchronized(stateLock) { AutoRaid.travel(session,travel,latitude,longitude); }
    }
    public static void teleport(double latitude, double longitude) {
        gameRoutes.stop("Destination changed");
        engine.position(latitude, longitude); engine.stop();
        engine.enabled = foreground && canEnable();
        saveState();
    }
    public static void walkTo(double latitude, double longitude) {
        gameRoutes.stop("Manual walking started");
        engine.walkTo(latitude, longitude);
        if (!foreground || !canEnable()) engine.pause();
        saveState();
    }
    public static void walkRoute(double[][] waypoints) { walkRoute(waypoints, false); }
    /** As {@link #walkRoute(double[][])}, but a looping route repeats from its first stop; same lifecycle/save/readiness. */
    public static void walkRoute(double[][] waypoints, boolean loop) {
        gameRoutes.stop("Another route started");
        engine.walkRoute(waypoints, loop);
        if (!foreground || !canEnable()) engine.pause();
        saveState();
    }
    public static void startAutoWalk(double[][] waypoints,boolean loop) {
        if(!foreground||!canEnable())throw new IllegalArgumentException("Location adapter is not ready");
        gameRoutes.stop("Autowalk started");
        engine.walkRoute(waypoints,loop);saveState();
        event("autowalk-started","stops="+waypoints.length+" loop="+loop+" speed="+engine.speedKmh);
    }
    public static double cycleWalkSpeed() {
        double[] presets={0,9.3,18.6,30,60};double next=0;
        synchronized(engine){for(int i=0;i<presets.length;i++)if(Math.abs(engine.speedKmh-presets[i])<0.001){next=presets[(i+1)%presets.length];break;}engine.speed(next);}
        saveState();return next;
    }
    public static void suspendForBackground() {
        EternatusRouteRunner.stop("O aplicativo deixou o primeiro plano.");
        gameRoutes.stop("App left foreground; check the active in-game Route before restarting");
        gameRoutesPlanning=false;gameRoutes.setPlanning(false);
        requestInteractionStop();
        OneShotAutomation.reset();
        AutoRaid.reset();
        AutoMaxBattle.reset();
        GiftBatchSettings.reset();
        foreground = false; autoWalkPlanning=false;mapPoiOpen=false;mapPoiSnapshot="";mapPoiAt=0; mapTapEpoch++; invalidateQuickBallOutcome(); cameraSampleRequest.set(false); invalidateCameraHeading("App backgrounded");
        // Losing the foreground switches VGP off, clears any target, and never auto-resumes.
        vgp.suspend();
        engine.pause(); saveState();
        // Best effort: give any queued settings a bounded window to reach disk before a possible kill.
        // Writes already run off-thread, so this only waits briefly; failures are logged, never thrown.
        try { flushSettings(SETTINGS_BACKGROUND_FLUSH_MS); }
        catch (IOException pending) { event("settings-save-error",pending.toString()); }
    }
    private static void restoreRuntime(String reason) {
        if (!registered) return;
        LocationState.capture(engine, desiredEnabled).restoreStationary(engine, canEnable(), foreground);
        event("state-activation", reason + "; desiredEnabled=" + desiredEnabled + "; runtimeEnabled=" + engine.enabled + "; stationary");
    }

    /** A moving checkpoint is queued at most once per second; explicit controls call saveState directly. */
    public static void checkpoint(long now) {
        if (now - lastCheckpoint < 1000) return;
        lastCheckpoint = now;
        if (!checkpointPending.compareAndSet(false, true)) return;
        stateWriter.execute(() -> { try { saveState(); } finally { checkpointPending.set(false); } });
    }
    public static void saveState() {
        synchronized (stateLock) {
            if (stateFile == null) return;
            FileOutputStream out = null;
            StrictMode.ThreadPolicy policy = StrictMode.allowThreadDiskWrites();
            try {
                String encoded = LocationState.capture(engine, desiredEnabled).encode();
                if (encoded.equals(lastSaved)) return;
                out = stateFile.startWrite();
                out.write(encoded.getBytes(StandardCharsets.UTF_8));
                stateFile.finishWrite(out); out = null;
                lastSaved = encoded;
                if (stateWriteFailed) { stateWriteFailed = false; event("state-save-restored", "Location checkpoints are being saved again"); }
            } catch (IOException | RuntimeException error) {
                if (out != null) stateFile.failWrite(out);
                if (!stateWriteFailed) { stateWriteFailed = true; event("state-save-error", error.toString()); }
            } finally { StrictMode.setThreadPolicy(policy); }
        }
    }
    private static void loadState(Application app) {
        synchronized (stateLock) {
            stateFile = new AtomicFile(AccountProfiles.file(app, "pgrazor-location-state-v1.txt"));
            LocationState state = LocationState.defaults();
            String source = "defaults";
            StrictMode.ThreadPolicy policy = StrictMode.allowThreadDiskReads();
            try (FileInputStream in = stateFile.openRead()) {
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                byte[] buffer = new byte[LocationState.MAX_RECORD_LENGTH + 1];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    bytes.write(buffer, 0, read);
                    if (bytes.size() > LocationState.MAX_RECORD_LENGTH) throw new IOException("Oversized location state");
                }
                state = LocationState.decode(new String(bytes.toByteArray(), StandardCharsets.UTF_8));
                lastSaved = state.encode(); source = "saved";
            } catch (FileNotFoundException missing) {
                // First install has an enabled preference, but runtime remains gated by readiness.
            } catch (IOException | RuntimeException error) {
                event("state-load-error", "Using defaults: " + error);
            } finally { StrictMode.setThreadPolicy(policy); }
            // The spoofing preference is loaded from settings; coordinates restore stationary.
            state.restoreStationary(engine, false, false);
            event("state-restored", String.format(Locale.US,"source=%s lat=%.7f lon=%.7f speedKmh=%.2f bearing=%.2f desiredEnabled=%s; awaiting ready foreground; movement stopped",source,state.latitude,state.longitude,state.speedKmh,state.bearing,desiredEnabled));
        }
        saveState();
    }
    private static boolean darkTheme=true, joystickVisible=true;
    private static volatile int joystickSizeDp=136,joystickOpacity=60;
    // Visual style is independent from movement, camera-relative steering and saved route state.
    // 0=original, 1=Eternatus morph (clean <-> storm), 2=HUD, 3=user image, 4=prior Eternatus vortex.
    private static volatile int joystickStyle=1; // Default Eternatus morph; respect saved user selection.
    private static volatile boolean joystickEternatusEffects=true;
    // Independent visual-only animation settings. 0=soft, 1=normal, 2=epic for intensity.
    private static volatile int eternatusFxIntensity=2, eternatusFxSpeed=1;
    private static volatile int menuIconSizeDp=22,gymIconSizeDp=54,gymIconOpacity=100,overlayBackgroundOpacity=45;
    private static volatile int menuLauncherSizeDp=48,menuLauncherOpacity=100,menuLauncherBgOpacity=0,gymBackgroundOpacity=45;
    // 0=classic PGBR, 1=Eternatus emblem, 2=account-local chosen image.
    private static volatile int menuLauncherIconStyle=1; // Default Eternatus Pokébola.
    // Client-only privacy mask. Do not override PlayerService, account data or RPC fields.
    private static volatile boolean trainerAliasEnabled;
    private static volatile int trainerPrivacyMode; // 0=original, 1=hidden in PGBR, 2=local alias
    private static volatile String trainerAlias="";
    private static final java.security.SecureRandom trainerAliasRandom=new java.security.SecureRandom();
    public static boolean trainerAliasEnabled(){return trainerAliasEnabled;}
    public static int trainerPrivacyMode(){return trainerPrivacyMode;}
    public static String trainerAlias(){return trainerAlias;}
    /**
     * Push only an authenticated local name and presentation mode to the UI hook.
     * Never override a getter from PlayerService or modify IDs, login or RPCs.
     * A missing native bridge is reported as unavailable, not protected.
     */
    static int syncTrainerPrivacyDisplay(){
        try{
            return NativeCore.trainerPrivacyConfigure(
                trainerPrivacyMode != 0, AccountProfiles.authenticatedTrainerName());
        }catch(UnsatisfiedLinkError | NoClassDefFoundError unavailable){return -2;}
    }
    static int trainerPrivacyDisplayStatus(){
        try{return NativeCore.trainerPrivacyStatus();}
        catch(UnsatisfiedLinkError | NoClassDefFoundError unavailable){return -1;}
    }

    /** PGBR-only presentation, never the identity sent to the game. */
    public static String visibleTrainerName(String original){
        if(trainerPrivacyMode==1)return "*****";
        if(trainerPrivacyMode==2)return TrainerAliasPolicy.validCustom(trainerAlias)?trainerAlias:"*****";
        return original==null?"":original;
    }
    public static void setTrainerPrivacyMode(int mode){
        if(mode<0||mode>2)return;
        if(mode==2&&!TrainerAliasPolicy.validCustom(trainerAlias))trainerAlias=TrainerAliasPolicy.generate(trainerAliasRandom);
        trainerPrivacyMode=mode;
        trainerAliasEnabled=mode==2;
        saveSettings();
        syncTrainerPrivacyDisplay();
    }
    public static boolean setCustomTrainerAlias(String value){
        if(!TrainerAliasPolicy.validCustom(value))return false;
        trainerAlias=value;
        trainerPrivacyMode=2;
        trainerAliasEnabled=true;
        saveSettings();
        syncTrainerPrivacyDisplay();
        return true;
    }
    public static void setTrainerAliasEnabled(boolean value){
        if(value&&!TrainerAliasPolicy.validCustom(trainerAlias))
            trainerAlias=TrainerAliasPolicy.generate(trainerAliasRandom);
        trainerAliasEnabled=value;trainerPrivacyMode=value?2:0;saveSettings();syncTrainerPrivacyDisplay();
    }
    public static void regenerateTrainerAlias(){
        String old=trainerAlias;
        for(int i=0;i<3;i++){
            trainerAlias=TrainerAliasPolicy.generate(trainerAliasRandom);
            if(!trainerAlias.equals(old))break;
        }
        saveSettings();
        syncTrainerPrivacyDisplay();
    }
    // Visibility only. Bits are stable menu-entry IDs, independent of feature settings.
    private static volatile int quickMenuHidden;
    public static int quickMenuHidden(){return quickMenuHidden;}
    public static boolean quickMenuVisible(int bit){return QuickMenuVisibilityPolicy.visible(quickMenuHidden,bit);}
    public static void setQuickMenuVisible(int bit,boolean visible){
        if(!QuickMenuVisibilityPolicy.validBit(bit))return;
        int updated=QuickMenuVisibilityPolicy.withVisibility(quickMenuHidden,bit,visible);
        if(updated==quickMenuHidden)return;
        quickMenuHidden=updated;
        saveSettings();
    }
    private static volatile int uiScalePercent=100;
    public static int uiScalePercent(){return uiScalePercent;}
    public static void setUiScalePercent(int value){if(value!=25&&value!=50&&value!=75&&value!=100)return;uiScalePercent=value;saveSettings();}
    public static boolean darkTheme(){return darkTheme;}
    private static volatile boolean idleWalking;
    public static boolean idleWalking(){return idleWalking;}
    public static void setIdleWalking(boolean value){
        idleWalking=value;engine.setIdleWalking(value);saveSettings();
    }
    /** Idle motion shares the same location integrator and never owns an active route. */
    public static void stepMotion(double seconds,boolean joystickHeld){
        OneShotAutomation.tick(joystickHeld);
        AutoRaid.tick(joystickHeld);
        EternatusRouteRunner.tick();
        engine.setIdleWalkingAllowed(hostLive()&&desiredEnabled&&!joystickHeld&&!OneShotAutomation.idleBlocked()&&!AutoRaid.running()
            &&!AutoMaxBattle.running()&&!autoWalkPlanning&&!gameRoutesPlanning&&!gameRoutes.running()
            &&!cameraSamplePending()&&!interactionDiagnosticRunning());
        engine.step(seconds);
    }
    public static boolean joystickVisible(){return joystickVisible;}
    public static int joystickSizeDp(){return joystickSizeDp;}
    public static void setJoystickSizeDp(int value){joystickSizeDp=Math.max(88,Math.min(220,value));saveSettings();}
    public static int joystickOpacity(){return joystickOpacity;}
    public static int joystickStyle(){return joystickStyle;}
    public static void setJoystickStyle(int style){if(style<0||style>4)return;joystickStyle=style;saveSettings();}
    public static boolean joystickEternatusEffects(){return joystickEternatusEffects;}
    public static void setJoystickEternatusEffects(boolean value){joystickEternatusEffects=value;saveSettings();}
    public static int eternatusFxIntensity(){return eternatusFxIntensity;}
    public static void setEternatusFxIntensity(int value){
        if(value<1||value>2)return;
        eternatusFxIntensity=value;saveSettings();
    }
    public static int eternatusFxSpeed(){return eternatusFxSpeed;}
    public static void setEternatusFxSpeed(int value){
        if(value<0||value>2)return;
        eternatusFxSpeed=value;saveSettings();
    }
    public static int menuLauncherIconStyle(){return menuLauncherIconStyle;}
    public static void setMenuLauncherIconStyle(int style){if(style<0||style>2)return;menuLauncherIconStyle=style;saveSettings();}
    public static int menuLauncherSizeDp(){return menuLauncherSizeDp;}
    public static void setMenuLauncherSizeDp(int size){menuLauncherSizeDp=Math.max(28,Math.min(96,size));saveSettings();}
    public static int menuLauncherOpacity(){return menuLauncherOpacity;}
    public static void setMenuLauncherOpacity(int alpha){menuLauncherOpacity=Math.max(10,Math.min(100,alpha));saveSettings();}
    public static int menuLauncherBgOpacity(){return menuLauncherBgOpacity;}
    public static void setMenuLauncherBgOpacity(int alpha){menuLauncherBgOpacity=Math.max(0,Math.min(100,alpha));saveSettings();}
    public static int gymBackgroundOpacity(){return gymBackgroundOpacity;}
    public static void setGymBackgroundOpacity(int alpha){gymBackgroundOpacity=Math.max(0,Math.min(100,alpha));saveSettings();}
    public static int menuIconSizeDp(){return menuIconSizeDp;}
    public static void setMenuIconSizeDp(int value){menuIconSizeDp=Math.max(16,Math.min(40,value));saveSettings();}
    public static int gymIconSizeDp(){return gymIconSizeDp;}
    public static void setGymIconSizeDp(int value){gymIconSizeDp=Math.max(24,Math.min(110,value));saveSettings();}
    public static int gymIconOpacity(){return gymIconOpacity;}
    public static void setGymIconOpacity(int value){gymIconOpacity=Math.max(10,Math.min(100,value));saveSettings();}
    public static int overlayBackgroundOpacity(){return overlayBackgroundOpacity;}
    public static void setOverlayBackgroundOpacity(int value){overlayBackgroundOpacity=Math.max(0,Math.min(100,value));saveSettings();}
    public static void setJoystickOpacity(int value){joystickOpacity=Math.max(10,Math.min(100,value));saveSettings();}
    public static void setDarkTheme(boolean value){darkTheme=value;saveSettings();}
    public static void setJoystickVisible(boolean value){joystickVisible=value;saveSettings();}
    private static void loadSettings(Application app) {
        try { loadSettings(app,false); }
        catch(IOException impossible) { event("settings-load-error",impossible.toString()); }
    }
    // Import must detect partial reloads; normal startup retains its historical best-effort behavior.
    private static void loadSettings(Application app,boolean strict) throws IOException {
        OneShotAutomation.reset();
        AutoRaid.reset();
        AutoMaxBattle.reset();
        GiftBatchSettings.reset();
        BatchEvolutionSettings.reset();
        synchronized(stateLock) {
            if(settingsFile==null) {
                // Create the destination and its writer once so the sink can close over a stable
                // target; the file path never changes within a process.
                settingsFile=new AtomicFile(AccountProfiles.file(app,"pgrazor-settings-v1.properties"));
                final AtomicFile target=settingsFile;
                settingsWriter=new SettingsWriter(settingsIo,snapshot->writeSettings(target,snapshot),
                                                  error->event("settings-save-error",error.toString()));
            }
            // A reload must reflect every queued write for this file, never a half-finished one.
            try { settingsWriter.flush(SETTINGS_FLUSH_MS); }
            catch(IOException pending) {
                event("settings-load-error","Pending save not flushed: "+pending);
                if(strict)throw pending;
                return;
            }
            StrictMode.ThreadPolicy policy=StrictMode.allowThreadDiskReads();
            localTimeFormat=0;cooldownTextSp=0;cooldownEmphasis=false;
            quickMenuHidden=0;
            vgpIconStyle=0;
            vgpIconSize=48;
            vgpTapProtection=true;
            hudLabelsVisible=true;hudLabelsMask=HUD_ALL;hudLabelOpacity=0;hudLabelTextSp=11;
            joystickSizeDp=136;joystickOpacity=60;
            joystickStyle=1;joystickEternatusEffects=true;eternatusFxIntensity=2;eternatusFxSpeed=1;
            menuIconSizeDp=22;gymIconSizeDp=54;gymIconOpacity=100;overlayBackgroundOpacity=45;
            menuLauncherSizeDp=48;menuLauncherOpacity=100;menuLauncherBgOpacity=0;gymBackgroundOpacity=45;menuLauncherIconStyle=1;
            vgpIconOpacity=100;vgpBackgroundOpacity=75;
            transferIvKeepPercent=90;
            trainerAliasEnabled=false;trainerPrivacyMode=0;trainerAlias="";
            idleWalking=false;engine.setIdleWalking(false);
            SmartCatchSettings.read(new Properties());
            GymAutomationSettings.read(new Properties());
            AutoRaid.read(new Properties());
            BuddyEmotionSettings.read(new Properties());
            ResearchSettings.read(new Properties());
            ShundoSniper.read(new Properties());
            ShinyScanner.read(new Properties());
            try(FileInputStream in=settingsFile.openRead()) {
                Properties p=new Properties(); p.load(in);
                SmartCatchSettings.read(p);
                GymAutomationSettings.read(p);
                AutoRaid.read(p);
                BuddyEmotionSettings.read(p);
                ResearchSettings.read(p);
                ShundoSniper.read(p);
                ShinyScanner.read(p);
                desiredEnabled=Boolean.parseBoolean(p.getProperty("locationSpoofing","true"));
                darkTheme=Boolean.parseBoolean(p.getProperty("darkTheme","true"));
                trainerAliasEnabled=Boolean.parseBoolean(p.getProperty("trainerAliasEnabled","false"));
                trainerAlias=p.getProperty("trainerAlias","");
                if(!TrainerAliasPolicy.validCustom(trainerAlias))trainerAlias="";
                trainerPrivacyMode=readBoundedVisual(p,"trainerPrivacyMode",trainerAliasEnabled?2:0,0,2);
                trainerAliasEnabled=trainerPrivacyMode==2;
                if(trainerAliasEnabled&&trainerAlias.isEmpty())
                    trainerAlias=TrainerAliasPolicy.generate(trainerAliasRandom);
                quickMenuHidden=boundedSetting(p,"quickMenuHidden",QuickMenuVisibilityPolicy.MASK);
                try{uiScalePercent=Integer.parseInt(p.getProperty("uiScalePercent","100"));}catch(NumberFormatException invalid){uiScalePercent=100;}
                if(uiScalePercent!=25&&uiScalePercent!=50&&uiScalePercent!=75&&uiScalePercent!=100)uiScalePercent=100;
                joystickVisible=Boolean.parseBoolean(p.getProperty("joystickVisible","true"));
                joystickEternatusEffects=Boolean.parseBoolean(p.getProperty("joystickEternatusEffects","true"));
                eternatusFxIntensity=readBoundedVisual(p,"eternatusFxIntensity",2,1,2);
                eternatusFxSpeed=readBoundedVisual(p,"eternatusFxSpeed",1,0,2);
                try{
                    int saved=Integer.parseInt(p.getProperty("joystickStyle","-1"));
                    joystickStyle=saved>=0&&saved<=4?saved:
                        AccountProfiles.file(app,"joystick-custom.png").isFile()?3:1;
                }catch(RuntimeException invalid){
                    joystickStyle=AccountProfiles.file(app,"joystick-custom.png").isFile()?3:1;
                }
                menuIconSizeDp=readBoundedVisual(p,"menuIconSizeDp",22,16,40);
                gymIconSizeDp=readBoundedVisual(p,"gymIconSizeDp",54,24,110);
                gymIconOpacity=readBoundedVisual(p,"gymIconOpacity",100,10,100);
                overlayBackgroundOpacity=readBoundedVisual(p,"overlayBackgroundOpacity",45,0,100);
                menuLauncherSizeDp=readBoundedVisual(p,"menuLauncherSizeDp",48,28,96);
                menuLauncherIconStyle=readBoundedVisual(p,"menuLauncherIconStyle",1,0,2);
                menuLauncherOpacity=readBoundedVisual(p,"menuLauncherOpacity",100,10,100);
                menuLauncherBgOpacity=readBoundedVisual(p,"menuLauncherBgOpacity",0,0,100);
                gymBackgroundOpacity=readBoundedVisual(p,"gymBackgroundOpacity",45,0,100);
                vgpIconOpacity=readBoundedVisual(p,"vgpIconOpacity",100,10,100);
                vgpBackgroundOpacity=readBoundedVisual(p,"vgpBackgroundOpacity",75,0,100);
                try{joystickSizeDp=Math.max(88,Math.min(220,Integer.parseInt(p.getProperty("joystickSizeDp","136"))));}
                catch(NumberFormatException invalid){joystickSizeDp=136;}
                try{joystickOpacity=Math.max(10,Math.min(100,Integer.parseInt(p.getProperty("joystickOpacity","60"))));}
                catch(NumberFormatException invalid){joystickOpacity=60;}
                idleWalking=Boolean.parseBoolean(p.getProperty("idleWalking","false"));engine.setIdleWalking(idleWalking);
                cooldownOverlay=Boolean.parseBoolean(p.getProperty("cooldownOverlay","false"));
                cooldownTextSp=readBoundedVisual(p,"cooldownTextSp",0,0,20);
                if(cooldownTextSp<9)cooldownTextSp=0;
                localTimeFormat=readBoundedVisual(p,"localTimeFormat",0,0,6);
                cooldownEmphasis=Boolean.parseBoolean(p.getProperty("cooldownEmphasis","false"));
                localTimeOverlay=Boolean.parseBoolean(p.getProperty("localTimeOverlay","false"));
                encounterCounter=Boolean.parseBoolean(p.getProperty("encounterCounter","true"));encounterOverlay=Boolean.parseBoolean(p.getProperty("encounterOverlay","false"));
                quickSummaryEnabled=Boolean.parseBoolean(p.getProperty("autoDismiss","false"));
                spawnBooster=Boolean.parseBoolean(p.getProperty("spawnBooster","false"));
                try{giftOptions=Integer.parseInt(p.getProperty("giftOptions","0"));}catch(NumberFormatException ignored){giftOptions=0;}
                if(giftOptions<0||giftOptions>7)giftOptions=0;
                // VGP active state is runtime only: always start OFF, ignoring any legacy persisted vgpEnabled.
                autoIncubate=Boolean.parseBoolean(p.getProperty("autoIncubate","false"));
                incubateLongest=Boolean.parseBoolean(p.getProperty("incubateLongest","false"));
                vgpEnabled=false;vgp.clearTarget();
                vgpOverlay=Boolean.parseBoolean(p.getProperty("vgpOverlay","false"));
                try{int legacy=Integer.parseInt(p.getProperty("vgpIconStyle","0"));vgpIconStyle=legacy>=0&&legacy<=3?legacy:0;}
                catch(NumberFormatException ignored){vgpIconStyle=0;}
                try{vgpIconSize=Math.max(28,Math.min(72,Integer.parseInt(p.getProperty("vgpIconSize","48"))));}
                catch(NumberFormatException ignored){vgpIconSize=48;}
                hudLabelsVisible=Boolean.parseBoolean(p.getProperty("hudLabelsVisible","true"));
                hudLabelsMask=readBoundedVisual(p,"hudLabelsMask",hudLabelsVisible?HUD_ALL:0,0,HUD_ALL);
                hudLabelsVisible=hudLabelsMask!=0;
                try{hudLabelOpacity=Math.max(0,Math.min(100,Integer.parseInt(p.getProperty("hudLabelOpacity","0"))));}
                catch(NumberFormatException ignored){hudLabelOpacity=0;}
                // Migrate the global background to independent, less intrusive defaults.
                for(int i=0;i<HUD_BITS.length;i++){
                    HUD_OPACITIES[i]=readBoundedVisual(p,"hudBg."+HUD_BITS[i],
                        Math.min(20,hudLabelOpacity),0,100);
                    HUD_COLORS[i]=readBoundedVisual(p,"hudColor."+HUD_BITS[i],
                        new int[]{0,0,2,0,1,0,2,1,0}[i],0,3);
                }
                hudAppearanceRevision++;
                try{hudLabelTextSp=Math.max(9,Math.min(14,Integer.parseInt(p.getProperty("hudLabelTextSp","11"))));}
                catch(NumberFormatException ignored){hudLabelTextSp=11;}
                // Missing/invalid preference defaults to the safer confirmation mode.
                vgpTapProtection=!"false".equals(p.getProperty("vgpTapProtection","true"));
                try{vgpOptions=Integer.parseInt(p.getProperty("vgpOptions","3"))&7;}catch(NumberFormatException ignored){vgpOptions=3;}
                try{buddyBerry=Integer.parseInt(p.getProperty("buddyBerry","0"));}catch(NumberFormatException ignored){buddyBerry=0;}
                if(buddyBerry!=701&&buddyBerry!=703&&buddyBerry!=705&&buddyBerry!=706&&buddyBerry!=707)buddyBerry=0;
                int legacyBerry;try{legacyBerry=Integer.parseInt(p.getProperty("autoBerry","0"));}catch(NumberFormatException ignored){legacyBerry=0;}
                if(!BerryPriority.validId(legacyBerry))legacyBerry=0;
                berryPriority=p.getProperty("berryPriority",legacyBerry==0?"":String.valueOf(legacyBerry));
                try{BerryPriority.parse(berryPriority);}catch(IllegalArgumentException invalid){berryPriority="";}
                // Migration: installs that had an active berry keep the overlay enabled; otherwise it defaults off.
                encounterBerryEnabled=Boolean.parseBoolean(p.getProperty("encounterBerry",String.valueOf(!berryPriority.isEmpty())));
                try{battleOptions=Integer.parseInt(p.getProperty("battleOptions","0"));}catch(NumberFormatException ignored){battleOptions=0;}
                if(battleOptions<0||battleOptions>1023)battleOptions=0;
                gymRepeatEnabled=Boolean.parseBoolean(p.getProperty("gymRepeatEnabled","false"));
                int legacyRocket;try{legacyRocket=Integer.parseInt(p.getProperty("rocketOptions","0"));}catch(NumberFormatException ignored){legacyRocket=0;}
                int legacyPvP;try{legacyPvP=Integer.parseInt(p.getProperty("pvpBattleOptions","0"));}catch(NumberFormatException ignored){legacyPvP=0;}
                // Existing Rocket preference is canonical when it exists. Older installs with
                // only a PvP setting preserve its action bits; the super preference stays PvP-only.
                rocketOptions=SharedBattleOptions.restoreActions(
                    p.containsKey("rocketOptions"),legacyRocket,legacyPvP);
                pvpBattleOptions=SharedBattleOptions.pvpMask(rocketOptions,legacyPvP);
                // Migrate the old PvP toggle. This stores preference only,
                // never claims that native move selection is connected.
                smartAttack=Boolean.parseBoolean(p.getProperty("smartAttack",
                    String.valueOf((legacyPvP&PvpBattleMode.PREFER_SUPER)!=0)));
                // Older installs have neither key; use the explicit new defaults.
                smartShieldTiming=readBoundedVisual(p,"smartShieldTiming",1,0,2);
                smartFaintedSwapTiming=readBoundedVisual(p,"smartFaintedSwapTiming",1,0,2);
                pvpBattleOptions=PvpBattleMode.update(
                    pvpBattleOptions,PvpBattleMode.PREFER_SUPER,smartAttack);
                pvpBattleReady=false;
                pvpBattleStatus="Assistente PvP indisponível: aguardando controlador compatível";
                itemTransferEnabled=Boolean.parseBoolean(p.getProperty("itemTransfer","false"));
                itemRules=p.getProperty("itemRules","");itemLimits=p.getProperty("itemLimits","");itemCatalog=p.getProperty("itemCatalog","");
                if(!validItemRules(itemRules)||!validItemRules(itemLimits)){itemTransferEnabled=false;itemRules="";itemLimits="";}
                quickSpinEnabled=Boolean.parseBoolean(p.getProperty("quickSpin","false"));
                eternatusWithoutVgp=Boolean.parseBoolean(p.getProperty("eternatusWithoutVgp","false"));
                eternatusTimedAdvance=Boolean.parseBoolean(p.getProperty("eternatusTimedAdvance","false"));
                autoTransferEnabled=Boolean.parseBoolean(p.getProperty("autoTransfer","false"));
                try{
                    int value=Integer.parseInt(p.getProperty("transferIvKeepPercent","90"));
                    boolean valid=value>=0&&value<=101;
                    if(!valid){autoTransferEnabled=false;transferIvKeepPercent=90;event("transfer-safety","Invalid threshold: transfer disabled");}
                    else transferIvKeepPercent=value;
                }catch(NumberFormatException error){autoTransferEnabled=false;transferIvKeepPercent=90;}
                try { transferProtect=Integer.parseInt(p.getProperty("transferProtect","255")); if(transferProtect<0||transferProtect>255)transferProtect=255; } catch(NumberFormatException e){transferProtect=255;}
                // "Keep event / costume" (bit 2) was one option before costume got its own bit (7):
                // settings saved before the split keep costumes whenever they kept events.
                if(p.getProperty("transferProtectSplit")==null&&(transferProtect&4)!=0)transferProtect|=128;
                transferKeepAll=Boolean.parseBoolean(p.getProperty("transferKeepAll","false"));
                transferOverlayEnabled=Boolean.parseBoolean(p.getProperty("transferOverlay","false"));
                boolean[] keepDamaged=new boolean[1];
                transferKeepSpecies=normalizeKeepSpecies(p.getProperty("transferKeepSpecies",""),keepDamaged);
                // Fail safe: a corrupt keep list must not let auto transfer drop a species the user meant
                // to keep. Disable the master even with Keep All on, so toggling it off stays safe.
                if(keepDamaged[0])autoTransferEnabled=false;
                // Evolve before transferring. A corrupt mode/stages/species disables the feature so a
                // damaged config can never auto-evolve or transfer against the user's intent.
                evolveTransferEnabled=Boolean.parseBoolean(p.getProperty("evolveTransferEnabled","false"));
                String evolveEnabledText=p.getProperty("evolveTransferEnabled","false");
                boolean evolveCorrupt=!(evolveEnabledText.equals("true")||evolveEnabledText.equals("false"));
                try{evolveTransferMode=Integer.parseInt(p.getProperty("evolveTransferMode","0"));}catch(NumberFormatException invalid){evolveTransferMode=0;evolveCorrupt=true;}
                if(evolveTransferMode!=0&&evolveTransferMode!=1){evolveTransferMode=0;evolveCorrupt=true;}
                try{evolveTransferStages=Integer.parseInt(p.getProperty("evolveTransferStages","1"));}catch(NumberFormatException invalid){evolveTransferStages=1;evolveCorrupt=true;}
                if(evolveTransferStages!=1&&evolveTransferStages!=2){evolveTransferStages=1;evolveCorrupt=true;}
                boolean[] evolveDamaged=new boolean[1];
                evolveTransferSpecies=normalizeKeepSpecies(p.getProperty("evolveTransferSpecies",""),evolveDamaged);
                if(evolveCorrupt||evolveDamaged[0]){evolveTransferEnabled=false;autoTransferEnabled=false;}
                ivCatchEnabled=Boolean.parseBoolean(p.getProperty("ivCatch","true"));
                ivOwnedEnabled=Boolean.parseBoolean(p.getProperty("ivOwned","false"));
                int catchFormat=boundedSetting(p,"ivCatchFormat",4),ownedFormat=boundedSetting(p,"ivOwnedFormat",4);
                ivCatchFormat=catchFormat>0?catchFormat:2;ivOwnedFormat=ownedFormat>0?ownedFormat:2;
                storageIvMode=boundedSetting(p,"storageIv",4);
                catchChanceShown=Boolean.parseBoolean(p.getProperty("catchChance","true"));autoBall=Boolean.parseBoolean(p.getProperty("autoBall","false"));
                autoThrow=Boolean.parseBoolean(p.getProperty("autoThrow","false"));
                autoThrowOnMap=Boolean.parseBoolean(p.getProperty("autoThrowOnMap","false"));
                int target=boundedSetting(p,"autoBallTarget",99);autoBallTarget=target>0?target:75;
                pvpKeepLeagues=p.getProperty("pvpKeepLeagues")==null?3:boundedSetting(p,"pvpKeepLeagues",7);
                skipCutscenes=Boolean.parseBoolean(p.getProperty("skipCutscenes","false"));
                routeGiftExchange=Boolean.parseBoolean(p.getProperty("routeGiftExchange","false"));
                batteryLowRes=Boolean.parseBoolean(p.getProperty("batteryLowRes","false"));
                batteryFps20=Boolean.parseBoolean(p.getProperty("batteryFps20","false"));
                batteryLowQuality=Boolean.parseBoolean(p.getProperty("batteryLowQuality","false"));
                healEnabled=Boolean.parseBoolean(p.getProperty("smartHealEnabled","false"));
                smartBattle=Boolean.parseBoolean(p.getProperty("smartBattle","false"));
                healRevive=Boolean.parseBoolean(p.getProperty("smartHealRevive","true"));
                healFull=Boolean.parseBoolean(p.getProperty("smartHealFull","false"));
                healWhen=boundedSetting(p,"smartHealWhen",2);
                int threshold=boundedSetting(p,"smartHealThreshold",100);healThreshold=threshold==25||threshold==50||threshold==75||threshold==100?threshold:50;
                for(int i=0;i<3;i++){int rank=boundedSetting(p,PVP_RANK_KEYS[i],4096);pvpRanks[i]=rank>0?rank:PVP_DEFAULT_RANKS[i];}
                int format=boundedSetting(p,"storageIvFormat",4);storageIvFormat=format>0?format:storageIvMode>0?storageIvMode:3;
                // Migrate the former independent animation preference to Quick Catch.
                quickBallEnabled=quickSummaryEnabled;
                throwCurve=Boolean.parseBoolean(p.getProperty("curve","false"));
                throwAlwaysHit=Boolean.parseBoolean(p.getProperty("throwAlwaysHit","false"));
                throwAr=Boolean.parseBoolean(p.getProperty("throwAr","false"));
                throwGrade=boundedSetting(p,"grade",3); throwVariety=boundedSetting(p,"throwVariety",3); mapTapMode=boundedSetting(p,"mapTap",3);
                event("settings-restored","dismiss="+quickSummaryEnabled+" fastBall="+quickBallEnabled+" grade="+throwGrade+" variety="+throwVariety+" curve="+throwCurve+" mapTap="+mapTapMode+" cameraRelative=true heal="+healConfig());
            } catch(FileNotFoundException missing) {
                // Defaults apply only until the user first saves a choice.
                if(strict)throw missing;
                event("settings-defaults","No saved settings for this account profile yet");
            } catch(IOException | RuntimeException error) {
                event("settings-load-error",error.toString());
                if(strict)throw new IOException("Settings reload failed",error);
            }
            finally { StrictMode.setThreadPolicy(policy); }
        }
    }
    private static int readBoundedVisual(Properties p,String key,int fallback,int min,int max){
        try{int n=Integer.parseInt(p.getProperty(key,String.valueOf(fallback)));return n>=min&&n<=max?n:fallback;}
        catch(NumberFormatException ignored){return fallback;}
    }
    private static int boundedSetting(Properties p,String key,int max) {
        try { int n=Integer.parseInt(p.getProperty(key,"0")); return n>=0&&n<=max?n:0; }
        catch(NumberFormatException invalid) { return 0; }
    }
    /** Constructed preference snapshot only: no account stores, saved places or location state. */
    static Properties diagnosticSettings() {
        synchronized(stateLock) {
            Properties result=settingsSnapshot();
            result.remove("itemCatalog"); // Derived inventory catalog, not a user preference.
            result.remove("researchCatalog");
            result.remove("researchCatalogFormat");
            return result;
        }
    }
    static Properties exportSettings() throws IOException {
        synchronized(stateLock) {
            if(settingsFile==null)throw new IOException("Settings are not ready");
            // Persist the live snapshot and wait for it so the read reflects current choices; a failed
            // write surfaces synchronously here rather than silently returning stale settings.
            settingsWriter.submit(settingsSnapshot());
            settingsWriter.flush(SETTINGS_FLUSH_MS);
            Properties p=new Properties();
            try(FileInputStream in=settingsFile.openRead()){p.load(in);}
            return p;
        }
    }
    static void importSettings(Application app,Properties incoming,double speed) throws IOException {
        synchronized(stateLock) {
            // Preserve the entire live snapshot, including non-backup keys, before any mutation.
            Properties previous=exportSettings(), merged=new Properties();merged.putAll(previous);merged.putAll(incoming);
            double previousSpeed=engine.speedKmh;
            if(incoming.containsKey("autoBerry")&&!incoming.containsKey("berryPriority"))merged.remove("berryPriority");
            if(incoming.containsKey("transferProtect")&&!incoming.containsKey("transferProtectSplit"))merged.remove("transferProtectSplit");
            try {
                writeSettings(settingsFile,merged);
                loadSettings(app,true); // Restores preferences and explicitly leaves VGP off.
                engine.stop();engine.speed(speed);mapTapEpoch++;restoreRuntime("settings-import");saveState();
                if(stateFile!=null && stateWriteFailed)throw new IOException("Location state could not be saved");
            } catch(IOException | RuntimeException error) {
                IOException failure=new IOException("Settings import failed",error);
                try {
                    writeSettings(settingsFile,previous);
                    loadSettings(app,true);
                    engine.stop();engine.speed(previousSpeed);mapTapEpoch++;restoreRuntime("settings-import-rollback");saveState();
                    // A failed new checkpoint may have left the last durable checkpoint intact.
                    if(stateFile!=null && stateWriteFailed && LocationState.capture(engine,desiredEnabled).encode().equals(lastSaved))stateWriteFailed=false;
                    if(stateFile!=null && stateWriteFailed)throw new IOException("Previous location state could not be saved");
                } catch(IOException | RuntimeException rollback) { failure.addSuppressed(rollback); }
                throw failure;
            }
        }
    }
    // Capture current preferences under stateLock; serialization and AtomicFile IO run off the main thread.
    static void saveSettings() {
        synchronized(stateLock) {
            if(settingsFile==null)return;
            settingsWriter.submit(settingsSnapshot());
        }
    }
    /** Snapshot of every persisted preference; must be built while holding stateLock. */
    private static Properties settingsSnapshot() {
        Properties p=new Properties();
        SmartCatchSettings.write(p);
        GymAutomationSettings.write(p);
        AutoRaid.write(p);
        BuddyEmotionSettings.write(p);
        ResearchSettings.write(p);
        ShundoSniper.write(p);
        ShinyScanner.write(p);
        p.setProperty("locationSpoofing",String.valueOf(desiredEnabled));
        p.setProperty("darkTheme",String.valueOf(darkTheme));p.setProperty("uiScalePercent",String.valueOf(uiScalePercent));
        p.setProperty("trainerAliasEnabled",String.valueOf(trainerAliasEnabled));
        p.setProperty("trainerPrivacyMode",String.valueOf(trainerPrivacyMode));
        p.setProperty("trainerAlias",trainerAlias);
        p.setProperty("joystickVisible",String.valueOf(joystickVisible));
        p.setProperty("joystickSizeDp",String.valueOf(joystickSizeDp));
        p.setProperty("joystickOpacity",String.valueOf(joystickOpacity));
        p.setProperty("joystickStyle",String.valueOf(joystickStyle));
        p.setProperty("joystickEternatusEffects",String.valueOf(joystickEternatusEffects));
        p.setProperty("eternatusFxIntensity",String.valueOf(eternatusFxIntensity));
        p.setProperty("eternatusFxSpeed",String.valueOf(eternatusFxSpeed));
        p.setProperty("menuIconSizeDp",String.valueOf(menuIconSizeDp));
        p.setProperty("gymIconSizeDp",String.valueOf(gymIconSizeDp));
        p.setProperty("gymIconOpacity",String.valueOf(gymIconOpacity));
        p.setProperty("overlayBackgroundOpacity",String.valueOf(overlayBackgroundOpacity));
        p.setProperty("menuLauncherSizeDp",String.valueOf(menuLauncherSizeDp));
        p.setProperty("menuLauncherIconStyle",String.valueOf(menuLauncherIconStyle));
        p.setProperty("menuLauncherOpacity",String.valueOf(menuLauncherOpacity));
        p.setProperty("menuLauncherBgOpacity",String.valueOf(menuLauncherBgOpacity));
        p.setProperty("gymBackgroundOpacity",String.valueOf(gymBackgroundOpacity));
        p.setProperty("quickMenuHidden",String.valueOf(quickMenuHidden));
        p.setProperty("idleWalking",String.valueOf(idleWalking));
        p.setProperty("spawnBooster",String.valueOf(spawnBooster));
        p.setProperty("giftOptions",String.valueOf(giftOptions));
        p.setProperty("vgpOverlay",String.valueOf(vgpOverlay));
        p.setProperty("vgpIconStyle",String.valueOf(vgpIconStyle));
        p.setProperty("vgpIconSize",String.valueOf(vgpIconSize));
        p.setProperty("vgpIconOpacity",String.valueOf(vgpIconOpacity));
        p.setProperty("vgpBackgroundOpacity",String.valueOf(vgpBackgroundOpacity));
        p.setProperty("hudLabelsVisible",String.valueOf(hudLabelsMask!=0));
        p.setProperty("hudLabelsMask",String.valueOf(hudLabelsMask));
        p.setProperty("hudLabelOpacity",String.valueOf(hudLabelOpacity));
        p.setProperty("cooldownTextSp",String.valueOf(cooldownTextSp));
        p.setProperty("cooldownEmphasis",String.valueOf(cooldownEmphasis));
        p.setProperty("localTimeFormat",String.valueOf(localTimeFormat));
        p.setProperty("hudLabelTextSp",String.valueOf(hudLabelTextSp));
        for(int i=0;i<HUD_BITS.length;i++){
            p.setProperty("hudBg."+HUD_BITS[i],String.valueOf(HUD_OPACITIES[i]));
            p.setProperty("hudColor."+HUD_BITS[i],String.valueOf(HUD_COLORS[i]));
        }
        p.setProperty("vgpTapProtection",String.valueOf(vgpTapProtection));
        p.setProperty("vgpOptions",String.valueOf(vgpOptions));
        p.setProperty("buddyBerry",String.valueOf(buddyBerry));
        p.setProperty("autoIncubate",String.valueOf(autoIncubate));
        p.setProperty("incubateLongest",String.valueOf(incubateLongest));
        p.setProperty("cooldownOverlay",String.valueOf(cooldownOverlay));p.setProperty("localTimeOverlay",String.valueOf(localTimeOverlay));p.setProperty("encounterCounter",String.valueOf(encounterCounter));p.setProperty("encounterOverlay",String.valueOf(encounterOverlay));p.setProperty("berryPriority",berryPriority);
        p.setProperty("encounterBerry",String.valueOf(encounterBerryEnabled));
        p.setProperty("battleOptions",String.valueOf(battleOptions));
        p.setProperty("gymRepeatEnabled",String.valueOf(gymRepeatEnabled));
        p.setProperty("rocketOptions",String.valueOf(rocketOptions));
        p.setProperty("pvpBattleOptions",String.valueOf(pvpBattleOptions));p.setProperty("smartAttack",String.valueOf(smartAttack));
        p.setProperty("smartShieldTiming",String.valueOf(smartShieldTiming));
        p.setProperty("smartFaintedSwapTiming",String.valueOf(smartFaintedSwapTiming));
        p.setProperty("itemTransfer",String.valueOf(itemTransferEnabled));p.setProperty("itemRules",itemRules);p.setProperty("itemLimits",itemLimits);p.setProperty("itemCatalog",itemCatalog);
        p.setProperty("quickSpin",String.valueOf(quickSpinEnabled));
        p.setProperty("eternatusWithoutVgp",String.valueOf(eternatusWithoutVgp));
        p.setProperty("eternatusTimedAdvance",String.valueOf(eternatusTimedAdvance));
        p.setProperty("autoTransfer",String.valueOf(autoTransferEnabled)); p.setProperty("transferProtect",String.valueOf(transferProtect));p.setProperty("transferIvKeepPercent",String.valueOf(transferIvKeepPercent));p.setProperty("transferProtectSplit","1");
        p.setProperty("transferKeepAll",String.valueOf(transferKeepAll)); p.setProperty("transferOverlay",String.valueOf(transferOverlayEnabled)); p.setProperty("transferKeepSpecies",transferKeepSpecies);
        p.setProperty("evolveTransferEnabled",String.valueOf(evolveTransferEnabled)); p.setProperty("evolveTransferMode",String.valueOf(evolveTransferMode)); p.setProperty("evolveTransferStages",String.valueOf(evolveTransferStages)); p.setProperty("evolveTransferSpecies",evolveTransferSpecies);
        p.setProperty("ivCatch",String.valueOf(ivCatchEnabled)); p.setProperty("ivOwned",String.valueOf(ivOwnedEnabled)); p.setProperty("ivCatchFormat",String.valueOf(ivCatchFormat)); p.setProperty("ivOwnedFormat",String.valueOf(ivOwnedFormat)); p.setProperty("storageIv",String.valueOf(storageIvMode)); p.setProperty("storageIvFormat",String.valueOf(storageIvFormat));
        p.setProperty("catchChance",String.valueOf(catchChanceShown)); p.setProperty("autoBall",String.valueOf(autoBall)); p.setProperty("autoBallTarget",String.valueOf(autoBallTarget)); p.setProperty("autoThrow",String.valueOf(autoThrow)); p.setProperty("autoThrowOnMap",String.valueOf(autoThrowOnMap));
        p.setProperty("pvpKeepLeagues",String.valueOf(pvpKeepLeagues)); p.setProperty("skipCutscenes",String.valueOf(skipCutscenes)); p.setProperty("routeGiftExchange",String.valueOf(routeGiftExchange));
        p.setProperty("batteryLowRes",String.valueOf(batteryLowRes)); p.setProperty("batteryFps20",String.valueOf(batteryFps20)); p.setProperty("batteryLowQuality",String.valueOf(batteryLowQuality));
        p.setProperty("smartBattle",String.valueOf(smartBattle));
        p.setProperty("smartHealEnabled",String.valueOf(healEnabled)); p.setProperty("smartHealRevive",String.valueOf(healRevive)); p.setProperty("smartHealFull",String.valueOf(healFull));
        p.setProperty("smartHealWhen",String.valueOf(healWhen)); p.setProperty("smartHealThreshold",String.valueOf(healThreshold));
        for(int i=0;i<3;i++)p.setProperty(PVP_RANK_KEYS[i],String.valueOf(pvpRanks[i]));
        p.setProperty("autoDismiss",String.valueOf(quickSummaryEnabled)); p.setProperty("fastBall",String.valueOf(quickBallEnabled));
        p.setProperty("throwAr",String.valueOf(throwAr)); p.setProperty("curve",String.valueOf(throwCurve)); p.setProperty("grade",String.valueOf(throwGrade)); p.setProperty("throwVariety",String.valueOf(throwVariety));
        p.setProperty("throwAlwaysHit",String.valueOf(throwAlwaysHit));
        p.setProperty("mapTap",String.valueOf(mapTapMode));
        return p;
    }
    /** Writer sink: serializes to the captured AtomicFile off-thread, rolling back on failure. Never takes stateLock. */
    private static void writeSettings(AtomicFile file,Properties snapshot) throws IOException {
        FileOutputStream out=null;
        try { out=file.startWrite(); snapshot.store(out,"PGBR settings"); file.finishWrite(out); out=null; }
        finally { if(out!=null)file.failWrite(out); }
    }
    /** Blocks until queued settings writes are durable; IO failures reach the caller synchronously. */
    static void flushSettings(long timeoutMillis) throws IOException {
        SettingsWriter writer;
        synchronized(stateLock) { writer=settingsWriter; }
        // Flush off stateLock: the writer never needs it, so a timed wait must not pin it either.
        if(writer!=null) writer.flush(timeoutMillis);
    }
    // Small fixed-key startup summary survives rotation; original timestamps distinguish it from new events.
    private static final Map<String,String> logContext=new LinkedHashMap<>();
    private static boolean retainLogContext(String kind) {
        switch(kind) {
            case "account-native-gate":case "account-storage":case "account-support":case "account-state":case "account-command":
            case "rooted-native-state":case "native-runtime-health":case "native-bootstrap-pending":case "native-scheduler-pending":
            case "runtime-gates":case "session-start":case "session-root":case "session-environment":case "bootstrap":case "native-process-abi":
            case "native-bootstrap-ready":case "native-bootstrap-unavailable":
            case "native-scheduler-ready":case "native-scheduler-unavailable":
            case "compiled-gameplay-armed":case "native-transport-error":case "native-runtime-start":return true;
            default:return false;
        }
    }
    // Fixed keys retain the latest hatch outcome across rotation without retaining every GUI trace.
    private static String eggLogContextKey(String kind,String detail) {
        if(!"cutscene-skip".equals(kind))return null;
        try {
            org.json.JSONObject o=new org.json.JSONObject(detail);
            String phase=o.optString("phase"),item=o.optString("kind");
            if("skipped".equals(phase)) {
                switch(item) {
                    case "egg-hatch-intro":case "egg-hatch":case "hatched-pokemon":case "egg-inventory":return "egg/"+item;
                    default:return null;
                }
            }
            switch(phase) {
                case "post-hatch-eggs-ready":case "post-hatch-eggs-tracked":
                case "post-hatch-eggs-waiting":case "post-hatch-eggs-released":return "egg/"+phase;
                case "warning":case "error":
                    String message=o.optString("message","").toLowerCase(Locale.US);
                    return message.contains("hatch")||message.contains("egg inventory")?"egg/"+phase:null;
                default:return null;
            }
        }catch(Exception ignored){return null;}
    }
    private static final MapCatchStatus mapCatchStatus = new MapCatchStatus();
    public static String mapCatchLabel(){return autoThrow && autoThrowOnMap && hostLive() ? mapCatchStatus.label(android.os.SystemClock.elapsedRealtime()) : "";}
    public static void event(String kind, String detail) {
        if("battle-action".equals(kind)&&detail.startsWith("{")){
            try{
                org.json.JSONObject b=new org.json.JSONObject(detail);
                if(b.has("pvpExecutorReady")){
                    pvpBattleReady=b.optBoolean("pvpExecutorReady",false);
                    String battleMode="training".equals(b.optString("mode","pvp"))
                        ?"Treinamento VNext":"PvP VNext";
                    pvpBattleStatus=pvpBattleReady
                        ?battleMode+" em teste: executor conectado, sem confirmação de ataque aceito."
                        :battleMode+" aguardando partida reconhecida: "+b.optString("reason","pronto para observar");
                }else if(b.has("pvpAction")){
                    String mode="training".equals(b.optString("mode","pvp"))?"Treinamento":"PvP";
                    pvpBattleStatus=mode+" VNext: comando "+b.optString("pvpAction")+" invocado (sem confirmação de acerto).";
                }else if(b.has("pvpScene")){
                    String phase=b.optString("pvpScene","candidate");
                    pvpBattleStatus="Assistente: tela "+phase+" reconhecida; aguardando escopo VNext validado.";
                }else if(b.has("pvpCandidateDenied")){
                    pvpBattleReady=false;
                    pvpBattleStatus="Assistente PvP/Treino: bloqueado por "+b.optString("pvpCandidateDenied","motivo não informado");
                }else if(b.has("pvpTypeProbeReady")){
                    if(!b.optBoolean("pvpTypeProbeReady",false))
                        pvpBattleStatus="PvP: identificador tipado indisponível nesta versão; necessário fluxo Hub + Preparação.";
                }else if(b.optBoolean("pvpObserved",false)){
                    pvpBattleStatus="PvP reconhecido por tipo validado; aguardando ativação VNext.";
                }
            }catch(Exception ignored){}
        }
        if("auto-raid".equals(kind))AutoRaid.publish(detail);
        if("map-auto-throw".equals(kind)){
            try{org.json.JSONObject result=new org.json.JSONObject(detail);
                mapCatchStatus.event(result.optString("phase"),result.optInt("value"),android.os.SystemClock.elapsedRealtime());
                if("catch-result".equals(result.optString("phase"))){
                    int status=result.optInt("value");
                    if(status==1)mapThrowOutcome.request("caught");
                    else if(status==2)mapThrowOutcome.request("broke-free");
                    else if(status==3)mapThrowOutcome.request("fled");
                }
            }catch(Exception ignored){}
        }
        if("gym-automation".equals(kind))GymAutomationSettings.publish(detail);
        if("gym-spin".equals(kind)&&SmartCatchSettings.gymSpinActive()){
            try{setVgpStatus(new org.json.JSONObject(detail).optString("message","Waiting for gym disc"));}catch(Exception ignored){}
        }
        if("buddy-emotion".equals(kind))BuddyEmotionSettings.publish(detail);
        if("remote-gym".equals(kind)||"incense-bird".equals(kind)||"incense-session".equals(kind)||"incense-open".equals(kind))OneShotAutomation.publish(kind,detail);
        if("batch-evolution".equals(kind))BatchEvolutionSettings.publish(detail);
        if("gift-batch".equals(kind))GiftBatchSettings.publish(detail);
        if("research".equals(kind))ResearchSettings.publish(detail);
        if("session-stats".equals(kind))SessionStats.publish(detail);
        if("shundo-sniper".equals(kind)&&detail.startsWith("{"))ShundoSniper.publish(detail);
        if("shiny-scan".equals(kind)&&detail.startsWith("{"))ShinyScanner.publish(detail);
        if("catch-chance".equals(kind))CatchChance.publish(detail);
        if("smart-battle".equals(kind)&&detail.startsWith("{")){
            try{org.json.JSONObject o=new org.json.JSONObject(detail);String phase=o.optString("phase");
                if("error".equals(phase))smartBattleStatus=o.optString("message","");
                else if("ready".equals(phase))smartBattleStatus="";
                else if("team".equals(phase))smartBattleStatus="Team set to counter "+o.optInt("foes")+(o.optInt("foes")==1?" opponent":" opponents");
                else if("swapped".equals(phase))smartBattleStatus="Swapped to your best counter";
                else if("swap-refused".equals(phase))smartBattleStatus="The game refused a swap; staying in";
            }catch(Exception ignored){}
        }
        if("smart-heal".equals(kind)){
            try{org.json.JSONObject o=new org.json.JSONObject(detail);String phase=o.optString("phase");
                if("error".equals(phase)||"warning".equals(phase))healStatus=o.optString("message","");
                else if("ready".equals(phase))healStatus="";
                else if("healed".equals(phase)){
                    org.json.JSONArray used=o.optJSONArray("used");String[] names={"Potion","Super Potion","Hyper Potion","Max Potion","Revive","Max Revive"};
                    StringBuilder sb=new StringBuilder();
                    for(int i=0;used!=null&&i<names.length&&i<used.length();i++){int n=used.optInt(i);if(n>0)sb.append(sb.length()==0?"":", ").append(n).append(' ').append(names[i]);}
                    healStatus=sb.length()==0?"":"Used this session: "+sb;
                }}
            catch(Exception ignored){}
        }
        if("cutscene-skip".equals(kind)){
            try{org.json.JSONObject o=new org.json.JSONObject(detail);String phase=o.optString("phase");if("error".equals(phase))cutsceneSkipStatus=o.optString("message","");else if("ready".equals(phase))cutsceneSkipStatus="";}
            catch(Exception ignored){}
        }
        if("storage-iv".equals(kind)){
            try{org.json.JSONObject o=new org.json.JSONObject(detail);storageIvStatus="error".equals(o.optString("phase"))?o.optString("message",""):"";}
            catch(Exception ignored){}
        }
        if("event-boxes".equals(kind)) {
            boolean available=false;
            try { available=new org.json.JSONObject(detail).optBoolean("available",false); }
            catch(Exception ignored) {}
            dev.pgrazor.events.EventSettings.publishBoxCollectorCapability(available);
        }
        if("runtime-gates".equals(kind)||"state-activation".equals(kind)||"panel-closed".equals(kind)||"disconnected".equals(kind)||"heartbeat-expired".equals(kind))
            detail+="; foreground="+foreground+" backendReady="+backendReady+" connected="+connected()+" accountAllowed="+AccountProfiles.allowed();
        synchronized (logLock) {
            String line = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US).format(new Date()) + " [" + kind + "] " + detail;
            byte[] encoded=(line+"\n").getBytes(StandardCharsets.UTF_8);
            // Apply the same bound to logcat and disk before deciding whether to rotate.
            if(encoded.length>65536){line=line.substring(0,Math.min(line.length(),16000))+" [truncated]";encoded=(line+"\n").getBytes(StandardCharsets.UTF_8);}
            if(retainLogContext(kind))logContext.put(kind,line.substring(0,Math.min(line.length(),2048)));
            String eggKey=eggLogContextKey(kind,detail);
            if(eggKey!=null)logContext.put(eggKey,line.substring(0,Math.min(line.length(),2048)));
            Log.i("PGBR", line);
            if (log != null && !interactionMetadataRow(kind,detail)) {
                try {
                    if(logBytes+encoded.length>1048576L){
                        log.close();
                        logFile=new File(logFile.getParentFile(),new SimpleDateFormat("yyyyMMdd-HHmmss-SSS",Locale.US).format(new Date())+"-"+(++logSegment)+".txt");
                        log=new PrintWriter(new FileOutputStream(logFile),true);logBytes=0;
                        for(String retained:logContext.values()) {
                            String contextLine="[retained-context] "+retained;
                            log.println(contextLine);logBytes+=(contextLine+"\n").getBytes(StandardCharsets.UTF_8).length;
                        }
                        pruneLogs(logFile.getParentFile(),logFile,6);
                    }
                    log.println(line);logBytes+=encoded.length;
                    if(log.checkError()){log.close();throw new IOException("File logging write failed");}
                }catch(IOException error){log=null;Log.e("PGBR","File logging unavailable",error);}
            }
        }
    }
    // The interaction probe lists every class, field and method it inspects, about 2 MB per launch: a third
    // of the six kept log files. Those rows stay in logcat; its summaries and results stay in the file.
    private static boolean interactionMetadataRow(String kind,String detail) {
        return "interaction-probe".equals(kind)&&detail!=null&&(detail.startsWith("{\"type\":\"class\"")
            ||detail.startsWith("{\"type\":\"field\"")||detail.startsWith("{\"type\":\"method\""));
    }
    private static void pruneLogs(File directory,File active,int keep) {
        File[] files=directory.listFiles((dir,name)->name.matches("[0-9]{8}-[0-9]{6}-[0-9]{3}(-[0-9]+)?\\.txt"));
        if(files==null)return;
        java.util.Arrays.sort(files,(a,b)->b.getName().compareTo(a.getName()));
        int retained=active==null?0:1;
        for(File f:files){if(f.equals(active))continue;if(retained++>=keep&&!f.delete())Log.w("PGBR","Unable to prune log "+f.getName());}
    }
    private static void recoverRunLogs(Application app) {
      synchronized(logLock) {
        try {
            File[] files=new File(app.getFilesDir(),"pgrazor-logs").listFiles((dir,name)->name.endsWith(".txt"));
            File destination=app.getExternalFilesDir("pgrazor-diagnostics");
            if(files==null||destination==null)return;
            if(!destination.isDirectory()&&!destination.mkdirs())return;
            java.util.Arrays.sort(files,(a,b)->b.getName().compareTo(a.getName()));
            int count=0;
            for(File source:files){
                if(source.equals(logFile))continue;
                if(count++>=6)break;
                try(RandomAccessFile in=new RandomAccessFile(source,"r");FileOutputStream out=new FileOutputStream(new File(destination,source.getName()))){
                    long remaining=Math.min(in.length(),524288L);in.seek(in.length()-remaining);
                    byte[] buffer=new byte[8192];
                    while(remaining>0){int n=in.read(buffer,0,(int)Math.min(buffer.length,remaining));if(n<0)break;out.write(buffer,0,n);remaining-=n;}
                }
                // Bound retained legacy sessions after their recent tail is exported.
                if(source.length()>1048576L){
                    try(RandomAccessFile old=new RandomAccessFile(source,"rw")){
                        byte[] tail=new byte[524288];old.seek(old.length()-tail.length);old.readFully(tail);
                        old.seek(0);old.write(tail);old.setLength(tail.length);
                    }
                }
            }
            synchronized(logLock){pruneLogs(new File(app.getFilesDir(),"pgrazor-logs"),logFile,6);}
            pruneLogs(destination,null,6);
            event("log-recovery","Recent run tails saved; retaining six log segments and six diagnostic tails");
        }catch(IOException|RuntimeException error){event("log-recovery-error",error.toString());}
      }
    }
    public static File currentLog() { return logFile; }
    // The staged profile's stationary fix is needed before login/map identity is available.
    public static boolean active() { return desiredEnabled && foreground && backendReady && connected() && engine.enabled; }
    public static double latitude() { latitudeReads++; return currentLatitude(); }
    public static double longitude() { longitudeReads++; return currentLongitude(); }
    public static float speed() { return (float)engine.metersPerSecond(); }
    public static float bearing() { return (float)engine.bearing; }
}
