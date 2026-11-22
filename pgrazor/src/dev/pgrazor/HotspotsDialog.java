package dev.pgrazor;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.Locale;

/** Map selection is read-only; Quick Menu actions require a separate explicit choice. */
public final class HotspotsDialog {
    private HotspotsDialog() {}
    public interface Selection { void select(Hotspots.Spot spot); }
    public interface Action { boolean apply(double latitude,double longitude); }

    public static AlertDialog show(Activity activity,Selection selection) {
        Context ui=MapTheme.wrap(activity);
        LinearLayout root=new LinearLayout(ui);root.setOrientation(LinearLayout.VERTICAL);
        int pad=MapTheme.dp(ui,12);root.setPadding(pad,pad,pad,pad);
        TextView note=new TextView(ui);note.setText("Selecione um local. Data, hora e fuso são atualizados automaticamente.");note.setTextSize(12);root.addView(note);
        ScrollView scroll=new ScrollView(ui);LinearLayout rows=new LinearLayout(ui);rows.setOrientation(LinearLayout.VERTICAL);scroll.addView(rows);
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        AlertDialog dialog=new AlertDialog.Builder(ui).setTitle(dev.pgrazor.PtBr.tr("Hotspots")).setView(root).setNegativeButton(dev.pgrazor.PtBr.tr("Close"),null).create();
        java.util.List<TextView> labels=new java.util.ArrayList<>();
        for(Hotspots.Spot spot:Hotspots.ALL) {
            TextView row=new TextView(ui);row.setTextSize(15);row.setPadding(pad,pad,pad,pad);
            labels.add(row);
            row.setClickable(true);row.setFocusable(true);
            row.setOnClickListener(v->{dialog.dismiss();selection.select(spot);});rows.addView(row);
        }
        // Refresh visible local clocks every 30 seconds; cancel on dialog lifecycle end.
        boolean hours24=android.text.format.DateFormat.is24HourFormat(activity);
        Runnable[] tick=new Runnable[1];
        tick[0]=()->{
            long now=System.currentTimeMillis();
            for(int j=0;j<labels.size();j++){
                Hotspots.Spot spot=Hotspots.ALL.get(j);
                labels.get(j).setText(spot.name+"\\n"+spot.city+", "+spot.country+"\\n"+spot.local(now,hours24));
            }
            if(dialog.isShowing())root.postDelayed(tick[0],30000L);
        };
        tick[0].run();
        DialogLifecycle.bind(activity,dialog,()->root.removeCallbacks(tick[0]));
        dialog.setOnShowListener(d->{
            MapTheme.fullScreen(dialog);MapTheme.style(root);MapTheme.styleButtons(dialog);
            root.removeCallbacks(tick[0]);root.postDelayed(tick[0],30000L);
        });
        dialog.show();return dialog;
    }

    public static void showQuick(Activity activity,Action teleport,Action walk) {
        show(activity,spot->{
            AlertDialog actions=new AlertDialog.Builder(MapTheme.wrap(activity)).setTitle(dev.pgrazor.PtBr.tr(spot.name))
                .setMessage(dev.pgrazor.PtBr.tr(spot.city+", "+spot.country+String.format(Locale.US,"\n%.6f, %.6f",spot.latitude,spot.longitude)))
                .setNegativeButton(dev.pgrazor.PtBr.tr("Cancel"),null).setNeutralButton(dev.pgrazor.PtBr.tr("Walk"),null).setPositiveButton(dev.pgrazor.PtBr.tr("Teleport"),null).create();
            DialogLifecycle.bind(activity,actions,null);
            actions.setOnShowListener(d->{
                MapTheme.styleButtons(actions);
                actions.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v->{if(walk.apply(spot.latitude,spot.longitude))actions.dismiss();});
                actions.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
                    actions.dismiss();TeleportDialog.confirm(activity,spot.latitude,spot.longitude,teleport::apply);
                });
            });actions.show();
        });
    }

}
