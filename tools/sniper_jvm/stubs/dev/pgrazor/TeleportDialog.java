package dev.pgrazor;
import android.app.Activity;
import android.app.AlertDialog;
public final class TeleportDialog {
    public interface Action { boolean apply(double latitude, double longitude); }
    public static AlertDialog confirm(Activity activity, double lat, double lon, Action teleport) { return null; }
}
