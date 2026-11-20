package dev.pgrazor;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.SystemClock;
import android.text.format.DateFormat;
import android.view.Gravity;
import android.widget.TextView;
import java.util.Locale;

/** Small local clock driven by Pad's existing lifecycle/tick, with no background timer. */
final class LocalTimeOverlay extends TextView {
    private final LocalTimeClock clock = new LocalTimeClock();
    private long lastRefresh;
    private boolean closed;
    private int lastAppearanceKey=Integer.MIN_VALUE;

    LocalTimeOverlay(Activity activity) {
        super(activity);
        setTextSize(11); setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        setGravity(Gravity.CENTER); setSingleLine(true);
        int padding = SettingsUi.dp(activity, 4);
        setPadding(padding, 0, padding, 0);
        setMinWidth(SettingsUi.dp(activity, 78));
        setVisibility(GONE);
        applyTheme();
    }

    void applyTheme() {
        int opacity=Control.hudLabelOpacity(), font=Control.hudLabelTextSp();
        int key=(opacity*100+font)*2+(Control.darkTheme()?1:0);
        if(key==lastAppearanceKey)return;
        lastAppearanceKey=key;
        int alpha=Math.round(opacity*Control.overlayBackgroundOpacity()*255f/10000f);
        int rgb=Control.darkTheme()?0x141b25:0xf1f5f8;
        setBackground(alpha==0?null:SettingsUi.rounded(getContext(),(alpha<<24)|rgb,7));
        setTextSize(font);
        setTextColor(SettingsUi.ink());
        setShadowLayer(SettingsUi.dp(getContext(),1),0,0,Control.darkTheme()?0xff000000:0xffffffff);
    }

    void refresh(boolean allowed) {
        applyTheme();
        boolean visible = !closed && allowed && Control.hudLabelsVisible()
            && Control.localTimeOverlay() && Control.desiredEnabled();
        boolean showing = getVisibility() == VISIBLE;
        if (visible != showing) setVisibility(visible ? VISIBLE : GONE);
        if (!visible || !isShown() || getWindowVisibility() != VISIBLE) return;
        long elapsed = SystemClock.elapsedRealtime();
        if (showing && elapsed - lastRefresh < 1000) return;
        lastRefresh = elapsed;
        String label = clock.label(Control.engine.latitude, Control.engine.longitude,
                System.currentTimeMillis(), DateFormat.is24HourFormat(getContext()), Locale.getDefault());
        if (!label.contentEquals(getText())) {
            setText(label);
            setContentDescription("Local time at spoofed location: " + label + ". Drag to move.");
        }
    }

    void close() { closed = true; setVisibility(GONE); }
}
