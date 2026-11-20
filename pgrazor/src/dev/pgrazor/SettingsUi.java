package dev.pgrazor;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;

/**
 * Shared PGBR settings styling. Single home for the theme palette and the recursive
 * {@code styleTree} implementation so {@link Pad} (overlays/launcher/joystick) and
 * {@link SettingsPanel} (the settings body) apply identical colors without duplicating it.
 */
final class SettingsUi {
    private SettingsUi() {}

    static void enabled(View view,boolean enabled){
        view.setAlpha(enabled?1f:0.68f); enableChildren(view,enabled);
    }
    private static void enableChildren(View view,boolean enabled){
        view.setEnabled(enabled);
        if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++)enableChildren(group.getChildAt(i),enabled);}
    }

    static int dp(Context context, float x) { return Math.round(x * context.getResources().getDisplayMetrics().density); }
    static int ink() { return FeedTheme.chromeInk(); }
    static int muted() { return FeedTheme.chromeMuted(); }
    static int surface() { return FeedTheme.chromeSurface(); }
    static int accent() { return FeedTheme.chromeAccent(); }
    static GradientDrawable rounded(Context context,int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(context,radius));return d;}

    static Context dialogContext(Context context){return new ContextThemeWrapper(context,Control.darkTheme()?android.R.style.Theme_Material_Dialog_Alert:android.R.style.Theme_Material_Light_Dialog_Alert);}

    static TextView text(Context context,String label,int size,int color){
        TextView v = new TextView(context); v.setText(dev.pgrazor.PtBr.tr(label)); v.setTextSize(size); v.setTextColor(color); return v;
    }
    static Button button(Context context,String label){
        Button v = new Button(context); v.setText(dev.pgrazor.PtBr.tr(label)); v.setAllCaps(false); v.setTextSize(13); return v;
    }

    static void styleTree(Context context,View view){
        if(view instanceof TextView){TextView t=(TextView)view;
            t.setTextColor(new android.content.res.ColorStateList(new int[][]{new int[]{-android.R.attr.state_enabled},new int[]{}},new int[]{muted(),ink()}));
            if(!(view instanceof Button))t.setPadding(dp(context,4),dp(context,3),dp(context,4),dp(context,3));
        }
        if(view instanceof Button){TextView t=(TextView)view;t.setTextSize(14);t.setMinHeight(dp(context,48));t.setPadding(dp(context,10),dp(context,6),dp(context,10),dp(context,6));t.setGravity(Gravity.CENTER_VERTICAL|Gravity.START);
            if(!(view instanceof CompoundButton))MapTheme.setButtonBackground(view);
        }
        if(view instanceof CompoundButton)((CompoundButton)view).setButtonTintList(new android.content.res.ColorStateList(new int[][]{new int[]{-android.R.attr.state_enabled},new int[]{android.R.attr.state_checked},new int[]{}},new int[]{muted(),accent(),muted()}));
        if(view instanceof Switch){Switch s=(Switch)view;int off=Control.darkTheme()?0xff4a5a6a:0xffb4c2ce;s.setText(dev.pgrazor.PtBr.tr(""));s.setMinHeight(0);s.setPadding(dp(context,8),0,0,0);
            s.setThumbTintList(new android.content.res.ColorStateList(new int[][]{new int[]{-android.R.attr.state_enabled},new int[]{android.R.attr.state_checked},new int[]{}},new int[]{0xff6b7883,accent(),off}));
            s.setTrackTintList(new android.content.res.ColorStateList(new int[][]{new int[]{-android.R.attr.state_enabled},new int[]{android.R.attr.state_checked},new int[]{}},new int[]{0x33808080,(accent()&0x00ffffff)|0x66000000,(off&0x00ffffff)|0x55000000}));
        }
        if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++)styleTree(context,group.getChildAt(i));}
    }
}
