package dev.pgrazor;
import android.app.Activity;
public final class WorldMapDialog {
    public interface Callback { void apply(double latitude, double longitude); }
    public static void show(Activity activity, double lat, double lon, Callback teleport, Callback walk) { }
}
