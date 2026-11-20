package dev.pgrazor;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.widget.Toast;
import java.util.Locale;

/** Copies coordinates as "lat, lon" at 6 decimals, the precision the map and Teleport show. */
final class CoordinateClipboard {
    private CoordinateClipboard() {}

    static String text(double latitude, double longitude) {
        return String.format(Locale.US, "%.6f, %.6f", latitude, longitude);
    }

    /** Prefills only a valid coordinate pair. Never sends clipboard data to logs or a server. */
    static String coordinatesOnClipboard(Activity activity) {
        ClipboardManager clipboard = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard == null || !clipboard.hasPrimaryClip()) return "";
        try {
            ClipData data = clipboard.getPrimaryClip();
            if (data == null || data.getItemCount() == 0) return "";
            CharSequence value = data.getItemAt(0).coerceToText(activity);
            if (value == null || value.length() > 240) return "";
            String text = value.toString().trim();
            CoordinateInput.parse(text); // Only valid lat/lon coordinates.
            return text;
        } catch (RuntimeException invalid) { return ""; }
    }

    /** Copies and confirms with a short toast naming what was copied ({@code done}). */
    static void copy(Activity activity, double latitude, double longitude, String done) {
        if (!Double.isFinite(latitude) || !Double.isFinite(longitude)) return;
        ClipboardManager clipboard = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard == null) return;
        String text = text(latitude, longitude);
        clipboard.setPrimaryClip(ClipData.newPlainText("Coordinates", text));
        Toast.makeText(activity, done + ": " + text, Toast.LENGTH_SHORT).show();
    }
}
