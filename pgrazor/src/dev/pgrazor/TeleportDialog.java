package dev.pgrazor;
import android.app.Activity;
import android.app.AlertDialog;
import android.os.Handler;
import android.os.Looper;
import java.util.Locale;

/** Shared confirmation for every user teleport; no action runs before explicit confirmation. */
public final class TeleportDialog {
    private TeleportDialog() {}
    public interface Action { boolean apply(double latitude,double longitude); }
    private static AlertDialog active;
    static String duration(long millis) {
        long seconds=(Math.max(0,millis)+999)/1000;
        return String.format(Locale.US,"%dh %02dm %02ds",seconds/3600,(seconds/60)%60,seconds%60);
    }
    private static String warning(double lat,double lon) {
        return String.format(Locale.US,"Destination: %.6f, %.6f\n\n",lat,lon)
            +"Cooldown when you arrive: "+duration(Cooldown.remainingAt(lat,lon));
    }

    public interface Remember { void choice(boolean teleport); }
    /** Explicit walk/teleport selection for double-tapping the game map. */
    public static AlertDialog confirmMapTap(Activity activity,double lat,double lon,
                                             Action teleport,Action walk,Remember remember) {
        if(activity.isFinishing()||activity.isDestroyed())return null;
        if(!CooldownState.valid(lat,lon))throw new IllegalArgumentException("Coordenadas inválidas");
        if(active!=null&&active.isShowing())return active;
        android.content.Context ui=MapTheme.wrap(activity);
        android.widget.LinearLayout body=new android.widget.LinearLayout(ui);
        body.setOrientation(android.widget.LinearLayout.VERTICAL);
        int pad=MapTheme.dp(ui,20);body.setPadding(pad,pad/2,pad,0);
        android.widget.TextView warning=new android.widget.TextView(ui);
        warning.setText(dev.pgrazor.PtBr.tr(warning(lat,lon)));body.addView(warning);
        android.widget.CheckBox rememberChoice=new android.widget.CheckBox(ui);
        rememberChoice.setText("Não perguntar novamente");body.addView(rememberChoice);
        android.widget.TextView hint=new android.widget.TextView(ui);
        hint.setText("Caminhar torna o toque no mapa um comando de caminhada. "
            +"Teleporte dispensa esta escolha em próximos duplos toques. "
            +"Para reativá-la, altere o modo de toque no mapa.");
        hint.setTextSize(12);body.addView(hint);
        Handler main=new Handler(Looper.getMainLooper());
        AlertDialog choice=new AlertDialog.Builder(ui).setTitle("Caminhar ou Teleporte?")
            .setView(body).setNegativeButton("Cancelar",null).setNeutralButton("Caminhar",null)
            .setPositiveButton("Teleporte",null).create();
        Runnable refresh=new Runnable(){public void run(){
            if(choice.isShowing()){warning.setText(dev.pgrazor.PtBr.tr(warning(lat,lon)));main.postDelayed(this,1000);}
        }};
        active=choice;
        DialogLifecycle.bind(activity,choice,()->{main.removeCallbacksAndMessages(null);if(active==choice)active=null;});
        choice.setOnShowListener(v->{
            MapTheme.style(body);MapTheme.styleButtons(choice);main.post(refresh);
            choice.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(w->{
                if(teleport.apply(lat,lon)){if(rememberChoice.isChecked())remember.choice(true);choice.dismiss();}
            });
            choice.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(w->{
                if(walk.apply(lat,lon)){if(rememberChoice.isChecked())remember.choice(false);choice.dismiss();}
            });
        });
        choice.show();return choice;
    }

    public static AlertDialog confirm(Activity activity,double lat,double lon,Action teleport) {
        if(activity.isFinishing()||activity.isDestroyed())return null;
        if(!CooldownState.valid(lat,lon))throw new IllegalArgumentException("Invalid teleport coordinates");
        if(active!=null&&active.isShowing())return active;
        Handler main=new Handler(Looper.getMainLooper());
        AlertDialog dialog=new AlertDialog.Builder(MapTheme.wrap(activity)).setTitle(dev.pgrazor.PtBr.tr("Teleport cooldown"))
            .setMessage(dev.pgrazor.PtBr.tr(warning(lat,lon))).setNegativeButton(dev.pgrazor.PtBr.tr("Cancel"),null).setPositiveButton(dev.pgrazor.PtBr.tr("Teleport"),null).create();
        Runnable refresh=new Runnable(){public void run(){if(dialog.isShowing()){dialog.setMessage(dev.pgrazor.PtBr.tr(warning(lat,lon)));main.postDelayed(this,1000);}}};
        active=dialog;
        DialogLifecycle.bind(activity,dialog,()->{main.removeCallbacksAndMessages(null);if(active==dialog)active=null;});
        dialog.setOnShowListener(d->{MapTheme.styleButtons(dialog);main.post(refresh);
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{if(teleport.apply(lat,lon))dialog.dismiss();});
        });dialog.show();return dialog;
    }
}
