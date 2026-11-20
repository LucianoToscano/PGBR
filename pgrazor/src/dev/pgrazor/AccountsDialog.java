package dev.pgrazor;

import android.app.*;
import android.widget.*;
import android.view.Gravity;
import java.util.Locale;

/** Account chooser; authentication remains in the game's Google/PTC sign-in flow. */
final class AccountsDialog {
    static void show(Activity activity) {
        android.content.Context ui=MapTheme.wrap(activity);
        LinearLayout body=new LinearLayout(ui);body.setOrientation(1);int pad=MapTheme.dp(activity,16);body.setPadding(pad,pad,pad,pad);
        TextView status=new TextView(ui);status.setText(dev.pgrazor.PtBr.tr(AccountProfiles.status()));status.setTextSize(16);body.addView(status);
        TextView explanation=new TextView(ui);
        explanation.setText(dev.pgrazor.PtBr.tr("Each account keeps its settings, location and saved sign-in. Switching restarts the game. Sign in once per account; expired sessions may require sign-in again."));
        explanation.setPadding(0,pad,0,pad);body.addView(explanation);
        ScrollView scroll=new ScrollView(ui);LinearLayout rows=new LinearLayout(ui);rows.setOrientation(1);scroll.addView(rows);body.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        AccountProfileStore store=AccountProfiles.store();
        java.util.ArrayList<Button> switches=new java.util.ArrayList<>();
        java.util.LinkedHashMap<Button,String> deletions=new java.util.LinkedHashMap<>();
        if(store!=null)for(AccountProfileStore.Profile profile:store.list()){
            LinearLayout row=new LinearLayout(ui);row.setOrientation(1);row.setPadding(0,pad/2,0,pad/2);
            TextView label=new TextView(ui);label.setText(dev.pgrazor.PtBr.tr(profile.label+(profile.id.equals(AccountProfiles.currentId())?" (current)":"")));label.setTextSize(17);row.addView(label);
            try{android.util.AtomicFile f=new android.util.AtomicFile(store.dataFile(profile.id,"pgrazor-location-state-v1.txt"));
                LocationState location=LocationState.decode(new String(f.readFully(),java.nio.charset.StandardCharsets.UTF_8));
                TextView coords=new TextView(ui);coords.setText(dev.pgrazor.PtBr.tr(String.format(Locale.US,"%.5f, %.5f",location.latitude,location.longitude)));row.addView(coords);
            }catch(Exception ignored){}
            LinearLayout actions=new LinearLayout(ui);actions.setGravity(Gravity.END);row.addView(actions);
            Button rename=new Button(ui);rename.setText(dev.pgrazor.PtBr.tr("Rename"));actions.addView(rename);
            rename.setOnClickListener(v->{EditText name=new EditText(ui);name.setSingleLine(true);name.setText(dev.pgrazor.PtBr.tr(profile.label));new AlertDialog.Builder(ui).setTitle(dev.pgrazor.PtBr.tr("Account name")).setView(name).setNegativeButton(dev.pgrazor.PtBr.tr("Cancel"),null).setPositiveButton(dev.pgrazor.PtBr.tr("Save"),(d,w)->{String value=name.getText().toString();AccountProfiles.rename(profile.id,value,()->label.setText(dev.pgrazor.PtBr.tr(value)));}).show();});
            Button change=new Button(ui);change.setText(dev.pgrazor.PtBr.tr("Switch"));change.setEnabled(AccountProfiles.canSwitch()&&!profile.id.equals(AccountProfiles.currentId()));actions.addView(change);
            if(!profile.id.equals(AccountProfiles.currentId()))switches.add(change);
            change.setOnClickListener(v->confirm(activity,profile));rows.addView(row);
            Button delete=new Button(ui);delete.setText(dev.pgrazor.PtBr.tr("Delete"));delete.setEnabled(AccountProfiles.canDelete(profile.id));actions.addView(delete);deletions.put(delete,profile.id);
            delete.setContentDescription(dev.pgrazor.PtBr.tr(profile.id.equals(AccountProfiles.currentId())?"Switch accounts before deleting the current account":"Delete saved account"));
            delete.setOnClickListener(v->new AlertDialog.Builder(ui).setTitle(dev.pgrazor.PtBr.tr("Delete "+profile.label+"?"))
                .setMessage(dev.pgrazor.PtBr.tr("Remove this saved sign-in, settings, location, favorites and routes from this device? Your Pokémon GO account will not be deleted."))
                .setNegativeButton(dev.pgrazor.PtBr.tr("Cancel"),null).setPositiveButton(dev.pgrazor.PtBr.tr("Delete"),(d,w)->AccountProfiles.delete(activity,profile.id,()->{switches.remove(change);deletions.remove(delete);rows.removeView(row);})).show());
        }
        Button add=new Button(ui);add.setText(dev.pgrazor.PtBr.tr("Add account"));add.setEnabled(AccountProfiles.canSwitch());body.addView(add);
        Button reset=new Button(ui);reset.setText(dev.pgrazor.PtBr.tr("Reset sign-in choices"));body.addView(reset);
        reset.setOnClickListener(v->new AlertDialog.Builder(ui).setTitle(dev.pgrazor.PtBr.tr("Reset sign-in choices?"))
            .setMessage(dev.pgrazor.PtBr.tr("The game will restart and ask for your date of birth again. Enter your own date of birth to see the sign-in methods available to you."))
            .setNegativeButton(dev.pgrazor.PtBr.tr("Cancel"),null).setPositiveButton(dev.pgrazor.PtBr.tr("Reset"),(d,w)->AccountProfiles.resetSignIn()).show());
        add.setOnClickListener(v->{EditText label=new EditText(ui);label.setSingleLine(true);label.setHint(dev.pgrazor.PtBr.tr("Account name"));new AlertDialog.Builder(ui).setTitle(dev.pgrazor.PtBr.tr("Add account")).setMessage(dev.pgrazor.PtBr.tr("Create a separate profile, then sign in with Google or PTC.")).setView(label).setNegativeButton(dev.pgrazor.PtBr.tr("Cancel"),null).setPositiveButton(dev.pgrazor.PtBr.tr("Continue"),(d,w)->{try{String name=label.getText().toString().trim();AccountProfiles.add(activity,name.isEmpty()?"New account":name);}catch(Exception error){Toast.makeText(activity,"Could not add account",Toast.LENGTH_SHORT).show();}}).show();});
        AlertDialog dialog=new AlertDialog.Builder(ui).setTitle(dev.pgrazor.PtBr.tr("Accounts")).setView(body).setPositiveButton(dev.pgrazor.PtBr.tr("Close"),null).create();
        android.os.Handler handler=new android.os.Handler(android.os.Looper.getMainLooper());
        Runnable refresh=new Runnable(){public void run(){
            if(!dialog.isShowing())return;
            status.setText(dev.pgrazor.PtBr.tr(AccountProfiles.status()));
            boolean enabled=AccountProfiles.canSwitch();add.setEnabled(enabled);
            reset.setEnabled(AccountProfiles.canResetSignIn());reset.setVisibility(enabled?android.view.View.GONE:android.view.View.VISIBLE);
            for(Button button:switches)button.setEnabled(enabled);
            for(java.util.Map.Entry<Button,String> entry:deletions.entrySet())entry.getKey().setEnabled(AccountProfiles.canDelete(entry.getValue()));
            handler.postDelayed(this,500);
        }};
        dialog.setOnShowListener(d->{MapTheme.fullScreen(dialog);MapTheme.style(body);MapTheme.styleButtons(dialog);handler.post(refresh);});
        DialogLifecycle.bind(activity,dialog,()->handler.removeCallbacksAndMessages(null));dialog.show();
    }
    private static void confirm(Activity activity,AccountProfileStore.Profile profile){
        new AlertDialog.Builder(MapTheme.wrap(activity)).setTitle(dev.pgrazor.PtBr.tr("Switch to "+profile.label+"?"))
            .setMessage(dev.pgrazor.PtBr.tr("Movement and automation will stop. The game will restart with this account's saved sign-in and location. Sign in if this account hasn't been saved yet."))
            .setNegativeButton(dev.pgrazor.PtBr.tr("Cancel"),null).setPositiveButton(dev.pgrazor.PtBr.tr("Switch"),(d,w)->AccountProfiles.switchTo(activity,profile.id)).show();
    }
}
