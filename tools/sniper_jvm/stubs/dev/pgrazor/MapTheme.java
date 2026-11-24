package dev.pgrazor;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.view.View;
public final class MapTheme {
    public static boolean dark() { return false; }
    public static int ink() { return 0; }
    public static int muted() { return 0; }
    public static int surface() { return 0; }
    public static int accent() { return 0; }
    public static Context wrap(Activity activity) { return activity; }
    public static int dp(Context c, float value) { return Math.round(value); }
    public static void style(View root) { }
    public static void styleButtons(AlertDialog dialog) { }
    public static void fullScreen(AlertDialog dialog) { }
}
