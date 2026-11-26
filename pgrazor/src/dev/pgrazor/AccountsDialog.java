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
        TextView status=new TextView(ui);status.setText(dev.pgrazor.PtBr.tr(Control.visibleTrainerName(AccountProfiles.status())));status.setTextSize(16);body.addView(status);
        TextView explanation=new TextView(ui);
        explanation.setText(dev.pgrazor.PtBr.tr("Each account keeps its settings, location and saved sign-in. Switching restarts the game. Sign in once per account; expired sessions may require sign-in again."));
        explanation.setPadding(0,pad,0,pad);body.addView(explanation);
        ScrollView scroll=new ScrollView(ui);LinearLayout rows=new LinearLayout(ui);rows.setOrientation(1);scroll.addView(rows);body.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        AccountProfileStore store=AccountProfiles.store();
        java.util.ArrayList<Button> switches=new java.util.ArrayList<>();
        java.util.ArrayList<Button> appearanceCopies=new java.util.ArrayList<>();
        java.util.LinkedHashMap<Button,String> deletions=new java.util.LinkedHashMap<>();
        final int openingPrivacyMode=Control.trainerPrivacyMode();
        final java.util.ArrayList<AlertDialog> children=new java.util.ArrayList<>();
        int accountNumber=0;
        if(store!=null)for(AccountProfileStore.Profile profile:store.list()){
            final int rowNumber=++accountNumber;
            LinearLayout row=new LinearLayout(ui);row.setOrientation(1);row.setPadding(0,pad/2,0,pad/2);
            TextView label=new TextView(ui);label.setText(dev.pgrazor.PtBr.tr((Control.trainerPrivacyMode()==1?"Conta "+rowNumber:Control.visibleTrainerName(profile.label))+(profile.id.equals(AccountProfiles.currentId())?" (current)":"")));label.setTextSize(17);row.addView(label);
            if(openingPrivacyMode==0)try{android.util.AtomicFile f=new android.util.AtomicFile(store.dataFile(profile.id,"pgrazor-location-state-v1.txt"));
                LocationState location=LocationState.decode(new String(f.readFully(),java.nio.charset.StandardCharsets.UTF_8));
                TextView coords=new TextView(ui);coords.setText(dev.pgrazor.PtBr.tr(String.format(Locale.US,"%.5f, %.5f",location.latitude,location.longitude)));row.addView(coords);
            }catch(Exception ignored){}
            else {TextView hidden=new TextView(ui);hidden.setText("Coordenadas ocultas");row.addView(hidden);}
            LinearLayout actions=new LinearLayout(ui);actions.setGravity(Gravity.END);row.addView(actions);
            Button rename=new Button(ui);rename.setText(dev.pgrazor.PtBr.tr("Rename"));actions.addView(rename);
            rename.setOnClickListener(v -> {
                if(Control.trainerPrivacyMode()!=openingPrivacyMode)return;
                EditText name = new EditText(ui);
                name.setSingleLine(true);
                boolean privateName = Control.trainerPrivacyMode() != 0;
                if (privateName) name.setHint("Novo nome desta conta");
                else name.setText(profile.label);
                AlertDialog editor = new AlertDialog.Builder(ui).setTitle("Nome da conta no PGBR")
                    .setView(name).setNegativeButton("Cancelar", null).setPositiveButton("Salvar", null).create();
                editor.setOnShowListener(d -> editor.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(save -> {
                    String value = name.getText().toString().trim();
                    if (value.isEmpty()) {
                        name.setError(dev.pgrazor.PtBr.tr("Digite um nome para salvar."));
                        return;
                    }
                    AccountProfiles.rename(profile.id, value, () -> {
                        String display = Control.trainerPrivacyMode() == 1 ? "Conta " + rowNumber : Control.visibleTrainerName(value);
                        label.setText(display + (profile.id.equals(AccountProfiles.currentId()) ? " (atual)" : ""));
                        editor.dismiss();
                    });
                }));
                showChild(children,editor);
            });
            Button change=new Button(ui);change.setText(dev.pgrazor.PtBr.tr("Switch"));change.setEnabled(AccountProfiles.canSwitch()&&!profile.id.equals(AccountProfiles.currentId()));actions.addView(change);
            if(!profile.id.equals(AccountProfiles.currentId()))switches.add(change);
            change.setOnClickListener(v->{if(Control.trainerPrivacyMode()==openingPrivacyMode)confirm(activity,profile,children);});rows.addView(row);
            final Button copyAppearance;
            if(!profile.id.equals(AccountProfiles.currentId())){
                copyAppearance=new Button(ui);copyAppearance.setText("Copiar aparência para a conta atual");
                copyAppearance.setEnabled(AccountProfiles.canSwitch());row.addView(copyAppearance);appearanceCopies.add(copyAppearance);
                copyAppearance.setOnClickListener(v->new AlertDialog.Builder(ui)
                    .setTitle("Copiar aparência?")
                    .setMessage("Substitui somente tema, escala, tamanhos, opacidade e efeitos visuais da conta atual. Login, privacidade, localização, rotas e outras preferências permanecem separados. A conta de origem não muda.")
                    .setNegativeButton("Cancelar",null).setPositiveButton("Copiar",(d,w)->AccountProfiles.copyAppearance(activity,profile.id,
                        result->{if(!activity.isFinishing()&&!activity.isDestroyed())Toast.makeText(activity,result,Toast.LENGTH_LONG).show();})).show());
            }else copyAppearance=null;
            Button delete=new Button(ui);delete.setText(dev.pgrazor.PtBr.tr("Delete"));delete.setEnabled(AccountProfiles.canDelete(profile.id));actions.addView(delete);deletions.put(delete,profile.id);
            delete.setContentDescription(dev.pgrazor.PtBr.tr(profile.id.equals(AccountProfiles.currentId())?"Switch accounts before deleting the current account":"Delete saved account"));
            delete.setOnClickListener(v->{if(Control.trainerPrivacyMode()!=openingPrivacyMode)return;
                showChild(children,new AlertDialog.Builder(ui).setTitle(dev.pgrazor.PtBr.tr("Delete "+Control.visibleTrainerName(profile.label)+"?"))
                .setMessage(dev.pgrazor.PtBr.tr("Remove this saved sign-in, settings, location, favorites and routes from this device? Your Pokémon GO account will not be deleted."))
                .setNegativeButton(dev.pgrazor.PtBr.tr("Cancel"),null).setPositiveButton(dev.pgrazor.PtBr.tr("Delete"),(d,w)->AccountProfiles.delete(activity,profile.id,()->{switches.remove(change);if(copyAppearance!=null)appearanceCopies.remove(copyAppearance);deletions.remove(delete);rows.removeView(row);})).create());});
        }
        Button add=new Button(ui);add.setText(dev.pgrazor.PtBr.tr("Add account"));add.setEnabled(AccountProfiles.canSwitch());body.addView(add);
        Button reset=new Button(ui);reset.setText(dev.pgrazor.PtBr.tr("Reset sign-in choices"));body.addView(reset);
        reset.setOnClickListener(v->new AlertDialog.Builder(ui).setTitle(dev.pgrazor.PtBr.tr("Reset sign-in choices?"))
            .setMessage(dev.pgrazor.PtBr.tr("The game will restart and ask for your date of birth again. Enter your own date of birth to see the sign-in methods available to you."))
            .setNegativeButton(dev.pgrazor.PtBr.tr("Cancel"),null).setPositiveButton(dev.pgrazor.PtBr.tr("Reset"),(d,w)->AccountProfiles.resetSignIn()).show());
        add.setOnClickListener(v->{EditText label=new EditText(ui);label.setSingleLine(true);label.setHint(dev.pgrazor.PtBr.tr("Account name"));new AlertDialog.Builder(ui).setTitle(dev.pgrazor.PtBr.tr("Add account")).setMessage(dev.pgrazor.PtBr.tr("Create a separate profile, then sign in with Google or PTC.")).setView(label).setNegativeButton(dev.pgrazor.PtBr.tr("Cancel"),null).setPositiveButton(dev.pgrazor.PtBr.tr("Continue"),(d,w)->{try{String name=label.getText().toString().trim();AccountProfiles.add(activity,name.isEmpty()?"New account":name);}catch(Exception error){Toast.makeText(activity,dev.pgrazor.PtBr.tr("Could not add account"),Toast.LENGTH_SHORT).show();}}).show();});
        AlertDialog dialog=new AlertDialog.Builder(ui).setTitle(dev.pgrazor.PtBr.tr("Accounts")).setView(body).setPositiveButton(dev.pgrazor.PtBr.tr("Close"),null).create();
        android.os.Handler handler=new android.os.Handler(android.os.Looper.getMainLooper());
        Runnable refresh=new Runnable(){public void run(){
            if(!dialog.isShowing())return;
            // Do not retain a previously rendered private label or coordinates after a mode change.
            if(Control.trainerPrivacyMode()!=openingPrivacyMode){dismissChildren(children);dialog.dismiss();return;}
            status.setText(dev.pgrazor.PtBr.tr(Control.visibleTrainerName(AccountProfiles.status())));
            boolean enabled=AccountProfiles.canSwitch();add.setEnabled(enabled);
            reset.setEnabled(AccountProfiles.canResetSignIn());reset.setVisibility(enabled?android.view.View.GONE:android.view.View.VISIBLE);
            for(Button button:switches)button.setEnabled(enabled);
            for(Button button:appearanceCopies)button.setEnabled(enabled);
            for(java.util.Map.Entry<Button,String> entry:deletions.entrySet())entry.getKey().setEnabled(AccountProfiles.canDelete(entry.getValue()));
            handler.postDelayed(this,500);
        }};
        dialog.setOnShowListener(d->{MapTheme.fullScreen(dialog);MapTheme.style(body);MapTheme.styleButtons(dialog);handler.post(refresh);});
        DialogLifecycle.bind(activity,dialog,()->{handler.removeCallbacksAndMessages(null);dismissChildren(children);});dialog.show();
    }
    private static void showChild(java.util.ArrayList<AlertDialog> children,AlertDialog child){
        children.add(child);
        child.setOnDismissListener(d->children.remove(child));
        child.show();
    }
    private static void dismissChildren(java.util.ArrayList<AlertDialog> children){
        for(AlertDialog child:new java.util.ArrayList<>(children))if(child.isShowing())child.dismiss();
        children.clear();
    }
    private static void confirm(Activity activity,AccountProfileStore.Profile profile,java.util.ArrayList<AlertDialog> children){
        showChild(children,new AlertDialog.Builder(MapTheme.wrap(activity)).setTitle(dev.pgrazor.PtBr.tr("Switch to "+Control.visibleTrainerName(profile.label)+"?"))
            .setMessage(dev.pgrazor.PtBr.tr("Movement and automation will stop. The game will restart with this account's saved sign-in and location. Sign in if this account hasn't been saved yet."))
            .setNegativeButton(dev.pgrazor.PtBr.tr("Cancel"),null).setPositiveButton(dev.pgrazor.PtBr.tr("Switch"),(d,w)->AccountProfiles.switchTo(activity,profile.id)).create());
    }
}
