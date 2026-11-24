package dev.pgrazor;

import android.app.Application;
import android.os.SystemClock;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntSupplier;

/**
 * Original-style Virtual GO Plus: a manual ON/OFF state, optional notifications
 * and map marker. Control gates new actions on the optional estimated cooldown
 * without changing manual intent. The native layer retains response timeouts and protection
 * against duplicate, unconfirmed catch/spin operations.
 */
final class VgpController {
    /** Serializes enabled, overlay and target states. */
    private final Object lock = new Object();
    /** Sticky reason for the current VGP pause/refusal; takes priority over native status when set. */
    private volatile String guardReason = "";
    private volatile String status = "Waiting for VGP support";
    private volatile float targetX, targetY;
    private volatile long targetAt;
    private volatile boolean targetSpin;
    private volatile Application application;

    private final BooleanSupplier enabled;
    private final Consumer<Boolean> enabledWriter;
    private final BooleanSupplier overlay;
    private final Consumer<Boolean> overlayWriter;
    private final BooleanSupplier foreground;
    private final IntSupplier options;
    private final BiConsumer<String,String> event;
    private final Runnable save;

    VgpController(BooleanSupplier enabled, Consumer<Boolean> enabledWriter,
                  BooleanSupplier overlay, Consumer<Boolean> overlayWriter,
                  BooleanSupplier foreground, IntSupplier options,
                  BiConsumer<String,String> event, Runnable save) {
        this.enabled = enabled; this.enabledWriter = enabledWriter;
        this.overlay = overlay; this.overlayWriter = overlayWriter;
        this.foreground = foreground; this.options = options;
        this.event = event; this.save = save;
    }

    void setApplication(Application app) { application = app; }

    /** The overlay toggle persists; turning it off also clears the runtime enable, target and guard. */
    void setOverlay(boolean value) {
        synchronized(lock) { overlayWriter.accept(value); if(!value)enabledWriter.accept(false); targetAt=0; guardReason=""; }
        save.run();
    }
    /**
     * VGP intent is started/stopped manually; the action gate is separate.
     * The game must be in foreground and the overlay enabled to start.
     */
    void setEnabled(boolean value) {
        if(!value){
            synchronized(lock){enabledWriter.accept(false);targetAt=0;guardReason="";status="Off";}
            return;
        }
        boolean refused=false,on=false;
        synchronized(lock){
            if(!overlay.getAsBoolean()){
                enabledWriter.accept(false);targetAt=0;
                guardReason="VGP overlay hidden; turn on the VGP button first";refused=true;
            }else if(!foreground.getAsBoolean()){
                enabledWriter.accept(false);targetAt=0;
                guardReason="App left foreground; open the game before starting VGP";refused=true;
            }else{
                enabledWriter.accept(true);guardReason="";on=true;
            }
        }
        if(refused)event.accept("vgp-enable-refused",status());
        else if(on)event.accept("vgp-enabled","ON; runtime only");
    }
    void setStatus(String value) { status = value; }
    /** An overlay/foreground reason takes priority over generic native status. */
    String status() { String reason=guardReason; return reason.length()>0?reason:status; }

    void setTarget(boolean visible,double x,double y,boolean spin) {
        if(!visible){if(targetAt>0)targetAt=Math.min(targetAt,SystemClock.elapsedRealtime()-600);return;}
        if(!Double.isFinite(x)||!Double.isFinite(y)||x<0||x>1||y<0||y>1){targetAt=0;return;}
        targetX=(float)x;targetY=(float)y;targetSpin=spin;targetAt=SystemClock.elapsedRealtime();
    }
    /** Drops the pending target without changing enabled state (settings reload). */
    void clearTarget() { targetAt=0; }
    boolean targetVisible(boolean allowed,boolean mapVisible) {
        long age=SystemClock.elapsedRealtime()-targetAt;
        return allowed&&mapVisible&&targetAt>0&&age>=0&&age<1200;
    }
    float targetX() { return targetX; }
    float targetY() { return targetY; }
    boolean targetSpin() { return targetSpin; }

    void result(String action,boolean success) {
        Application app=application;if(!enabled.getAsBoolean()||(options.getAsInt()&4)==0||app==null)return;
        if(android.os.Build.VERSION.SDK_INT>=33&&app.checkSelfPermission("android.permission.POST_NOTIFICATIONS")!=android.content.pm.PackageManager.PERMISSION_GRANTED)return;
        try{android.app.NotificationManager manager=(android.app.NotificationManager)app.getSystemService(android.content.Context.NOTIFICATION_SERVICE);
            manager.createNotificationChannel(new android.app.NotificationChannel("pgrazor-vgp","PGBR VGP",android.app.NotificationManager.IMPORTANCE_LOW));
            String message=action.equals("catch")?(success?"Pokémon caught":"Pokémon not caught"):(success?"PokéStop spun":"Spin unsuccessful");
            manager.notify(7301,new android.app.Notification.Builder(app,"pgrazor-vgp").setSmallIcon(android.R.drawable.stat_notify_sync).setContentTitle("PGBR VGP").setContentText(message).setAutoCancel(true).build());
        }catch(RuntimeException error){event.accept("vgp-notification-error",error.toString());}
    }
    /** Losing the foreground switches VGP off, clears any target, and never auto-resumes. */
    void suspend() {
        synchronized(lock){enabledWriter.accept(false);targetAt=0;guardReason="App left foreground; tap VGP to start";}
    }
}
