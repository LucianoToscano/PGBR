package dev.pgrazor;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.SystemClock;
import android.view.Gravity;
import android.widget.TextView;

/** Small local clock driven by Pad's existing lifecycle/tick, with no background timer. */
final class LocalTimeOverlay extends TextView {
    private final LocalTimeClock clock = new LocalTimeClock();
    private long lastRefresh;
    private int lastFormat = -1;
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
        int opacity=Control.hudBackgroundOpacity(Control.HUD_LOCALTIME), font=Control.hudLabelTextSp();
        int key=(Control.hudAppearanceRevision()*101+font)*2+(Control.darkTheme()?1:0);
        if(key==lastAppearanceKey)return;
        lastAppearanceKey=key;
        int alpha=Math.round(opacity*255f/100f);
        int rgb=0x101924;
        setBackground(alpha==0?null:SettingsUi.rounded(getContext(),(alpha<<24)|rgb,7));
        setTextSize(font);
        setTextColor(Control.hudLabelColor(Control.HUD_LOCALTIME));
        setShadowLayer(SettingsUi.dp(getContext(),2),0,0,0xff000000);
    }

    void refresh(boolean allowed) {
        applyTheme();
        boolean visible = !closed && allowed && Control.hudLabelsVisible()
            && Control.localTimeOverlay() && Control.desiredEnabled();
        boolean showing = getVisibility() == VISIBLE;
        if (visible != showing) setVisibility(visible ? VISIBLE : GONE);
        if (!visible || !isShown() || getWindowVisibility() != VISIBLE) return;
        long elapsed = SystemClock.elapsedRealtime();
        int format = Control.localTimeFormat();
        if (showing && elapsed - lastRefresh < 1000 && format == lastFormat) return;
        lastFormat = format;
        lastRefresh = elapsed;
        double latitude=Control.engine.latitude, longitude=Control.engine.longitude;
        String label = clock.compactLabel(latitude, longitude, System.currentTimeMillis(), format);
        // ICU maps the IANA time zone to a country region; no network or location permission needed.
        if (format < 2 && !"--:--".equals(label)) try {
            String zone=TimeZoneLookup.zoneId(latitude,longitude);
            String region=android.icu.util.TimeZone.getRegion(zone);
            if(region!=null&&region.matches("[A-Z]{2}")){
                String flag=new String(Character.toChars(0x1f1e6+region.charAt(0)-'A'))
                    +new String(Character.toChars(0x1f1e6+region.charAt(1)-'A'));
                label=flag+"  "+label;
            }
        } catch(RuntimeException ignored){ /* Ocean and unknown region: show time without flag. */ }
        if (!label.contentEquals(getText())) {
            setText(label);
            setContentDescription("Hora local: " + label + ". Arraste para mover.");
        }
    }

    void close() { closed = true; setVisibility(GONE); }
}
