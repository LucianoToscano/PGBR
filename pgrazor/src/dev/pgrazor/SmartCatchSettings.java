package dev.pgrazor;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.text.InputType;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.Properties;

/** Account settings are persisted by Control alongside its other properties. */
public final class SmartCatchSettings {
    private SmartCatchSettings() {}
    private static final SmartCatchPreferences preferences=new SmartCatchPreferences();
    private static final int[] BERRIES={0,701,703,705,706,708};
    private static final String[] SPEED_NAMES={"Normal (about 1.5 s)","Fast (about 1 s)","Fastest (about 0.5 s)","Slow (about 3 s)"};
    private static final String[] BERRY_NAMES={"Off","Razz","Nanab","Pinap","Golden Razz","Silver Pinap"};
    public static void init(Context context) {}
    public static void read(Properties p){preferences.read(p);}
    public static void write(Properties p){preferences.write(p);}
    public static String config(){return preferences.config();}
    /** Gym-disc spinning follows VGP's actual run/cooldown gate, not catch availability. */
    public static boolean gymSpinActive(){return preferences.enabled()&&preferences.spinGyms()&&Control.vgpEnabled()&&!Control.vgpCooldownPaused();}
    public static String normalizeSpecies(String value){return SmartCatchPreferences.normalizeSpecies(value);}
    static int speed(){return preferences.speed();}
    static boolean shinyOnly(){return preferences.shinyOnly();}
    static String rareConfig(){return preferences.rareConfig();}
    static String tagConfig(){return preferences.tagConfig();}
    static boolean manualBerry(){return preferences.manualBerry();}
    static String candySpecies(){return preferences.candySpecies();}
    static void setCandySpecies(String value){set("CandySpecies",value);}
    static String catchSpecies(){return preferences.catchSpecies();}
    static void setCatchSpecies(String value){set("CatchSpecies",value);}
    private static String count(String ids,String none){return ids.isEmpty()?none:ids.split(",").length+" selected";}
    private static void set(String key,Object value){preferences.set(key,String.valueOf(value));persist();}
    private static void persist(){Control.saveSettings();}
    public static Runnable populate(Activity activity,LinearLayout body){
        Context ui=body.getContext();
        CheckBox mode=new CheckBox(ui);mode.setText(dev.pgrazor.PtBr.tr("VGP+"));mode.setChecked(preferences.enabled());body.addView(mode);
        LinearLayout options=new SettingsUi.OptionList(ui);body.addView(options);
        TextView description=new TextView(ui);description.setText(dev.pgrazor.PtBr.tr("Use the VGP overlay to start or stop. VGP+ adds ball, berry and throw choices. High CP/level targets use Ultra Balls; set a threshold to 0 to disable it."));options.addView(description);
        TextView standard=row(activity,options,"Standard ball",new String[]{"Poké Ball","Great Ball","Ultra Ball"},preferences.ball()-1,index->{set("Ball",index+1);});
        CheckBox auto=new CheckBox(ui);auto.setText(dev.pgrazor.PtBr.tr("VGP+ auto ball"));auto.setChecked(preferences.autoBall());options.addView(auto);
        TextView autoTarget=button(options,"VGP+ auto ball target: "+preferences.autoBallTarget()+"%");
        TextView autoHelp=new TextView(ui);autoHelp.setText(dev.pgrazor.PtBr.tr("Replaces the standard ball: throws the cheapest ball whose catch chance reaches the target, from the server's catch rate with your berry, throw rating and curveball. The Ultra Ball rules below still apply, and a Master Ball is never used."));options.addView(autoHelp);
        autoTarget.setOnClickListener(v->{
            int[] targets=Control.AUTO_BALL_TARGETS;int next=targets[0];
            for(int i=0;i<targets.length;i++)if(targets[i]==preferences.autoBallTarget()){next=targets[(i+1)%targets.length];break;}
            set("AutoBallTarget",next);autoTarget.setText(dev.pgrazor.PtBr.tr("VGP+ auto ball target: "+next+"%"));});
        int berryIndex=0;for(int i=0;i<BERRIES.length;i++)if(BERRIES[i]==preferences.berry())berryIndex=i;
        row(activity,options,"Standard berry",BERRY_NAMES,berryIndex,index->{set("Berry",BERRIES[index]);});
        CheckBox special=new CheckBox(ui);special.setText("Regra premium: Ultra Ball + fruta");special.setChecked(preferences.premium());options.addView(special);
        special.setOnCheckedChangeListener((v,on)->{set("Premium",on);});
        TextView legacyHelp=new TextView(ui);legacyHelp.setText("Padrão: Shiny ou IV ≥90%, com Fruta Frambo Dourada. Personalize abaixo sem alterar os filtros de espécies ou somente Shiny.");options.addView(legacyHelp);
        CheckBox customRare=new CheckBox(ui);customRare.setText("Personalizar critérios de raridade");customRare.setChecked(preferences.rareCustom());options.addView(customRare);
        LinearLayout rareOptions=new SettingsUi.OptionList(ui);options.addView(rareOptions);
        TextView rareCriteria=button(rareOptions,"Critérios de raridade: "+Integer.bitCount(preferences.rareMask()));
        String[] rareNames={"Shiny","IV 100% (Hundo)","Shiny 100% (Shundo)","IV 0% (Nundo)","IV mínimo","Lendário, mítico ou Ultra Beast","Fantasia","Fundo especial"};
        rareCriteria.setOnClickListener(v->{
            boolean[] selected=new boolean[rareNames.length];int mask=preferences.rareMask();
            for(int i=0;i<selected.length;i++)selected[i]=(mask&(1<<i))!=0;
            AlertDialog d=new AlertDialog.Builder(MapTheme.wrap(activity)).setTitle("Critérios de raridade")
                .setMultiChoiceItems(rareNames,selected,(dialog,index,checked)->selected[index]=checked)
                .setPositiveButton("Salvar",(dialog,which)->{int next=0;for(int i=0;i<selected.length;i++)if(selected[i])next|=1<<i;
                    set("RareMask",next);rareCriteria.setText("Critérios de raridade: "+Integer.bitCount(next));})
                .setNegativeButton("Cancelar",null).create();
            DialogLifecycle.bind(activity,d,null);d.setOnShowListener(x->MapTheme.styleButtons(d));d.show();
        });
        input(activity,rareOptions,"IV mínimo de raridade (1–100)",String.valueOf(preferences.rareIvMin()),value->{int n=bounded(value,100);if(n==0)throw new IllegalArgumentException("Use um valor de 1 a 100.");set("RareIvMin",n);},true);
        int rareBerryIndex=0;for(int i=0;i<BERRIES.length;i++)if(BERRIES[i]==preferences.rareBerry())rareBerryIndex=i;
        row(activity,rareOptions,"Fruta para Pokémon raro",BERRY_NAMES,rareBerryIndex,index->set("RareBerry",BERRIES[index]));
        TextView rareHelp=new TextView(ui);rareHelp.setText("Os IVs precisam vir de um encontro confirmado; dados ausentes não contam como Nundo. O estoque e as regras Safari continuam sendo verificados.");rareOptions.addView(rareHelp);
        customRare.setOnCheckedChangeListener((v,on)->{if(on!=preferences.rareCustom())set("RareCustom",on);rareOptions.setVisibility(on?android.view.View.VISIBLE:android.view.View.GONE);});
        rareOptions.setVisibility(preferences.rareCustom()?android.view.View.VISIBLE:android.view.View.GONE);
        input(activity,options,"High CP threshold (0–10000)",String.valueOf(preferences.highCp()),value->{int n=bounded(value,10000);set("HighCp",n);},true);
        input(activity,options,"High level threshold (0–50)",String.valueOf(preferences.highLevel()),value->{int n=bounded(value,50);set("HighLevel",n);},true);
        TextView rating=row(activity,options,"Throw rating",new String[]{"Default","Nice","Great","Excellent"},preferences.grade(),index->{set("Grade",index);});
        Runnable[] afterVariety={null};
        row(activity,options,"Throw variety",Control.THROW_VARIETY_LABELS,preferences.variety(),index->{set("Variety",index);if(afterVariety[0]!=null)afterVariety[0].run();});
        TextView varietyHelp=new TextView(ui);varietyHelp.setText(dev.pgrazor.PtBr.tr("Throw variety picks a rating at random for each throw, with even odds. All includes a plain throw. It replaces the throw rating while on."));options.addView(varietyHelp);
        CheckBox curved=new CheckBox(ui);curved.setText(dev.pgrazor.PtBr.tr("Curveball"));curved.setChecked(preferences.curve());options.addView(curved);
        curved.setOnCheckedChangeListener((v,on)->{set("Curve",on);});
        row(activity,options,"Catch speed",SPEED_NAMES,preferences.speed(),index->{set("Speed",index);});
        TextView speedHelp=new TextView(ui);speedHelp.setText(dev.pgrazor.PtBr.tr("How long VGP+ waits before each throw and before looking for the next Pokémon. Faster catching is less like normal play."));options.addView(speedHelp);
        CheckBox gymSpin=new CheckBox(ui);gymSpin.setText(dev.pgrazor.PtBr.tr("Auto-spin Gym discs"));gymSpin.setChecked(preferences.spinGyms());options.addView(gymSpin);
        TextView gymSpinHelp=new TextView(ui);gymSpinHelp.setText(dev.pgrazor.PtBr.tr("While VGP is running on the map, spin nearby Gym discs of any team within 80 m when the item bag has room."));options.addView(gymSpinHelp);
        gymSpin.setOnCheckedChangeListener((v,on)->{if(on!=preferences.spinGyms())set("SpinGyms",on);});
        TextView candy=button(options,"Candy species (Pinap): "+count(preferences.candySpecies(),"None"));
        candy.setOnClickListener(v->SmartCatchSpeciesDialog.show(activity,()->candy.setText(dev.pgrazor.PtBr.tr("Candy species (Pinap): "+count(preferences.candySpecies(),"None")))));
        TextView tagHelp=new TextView(ui);tagHelp.setText("Etiquetas: informe os nomes exatos criados no jogo, separados por vírgula. A seleção abrange as famílias dos Pokémon etiquetados e respeita os filtros de espécie e Shiny. Se uma etiqueta estiver ausente ou a leitura não terminar, a captura fica bloqueada.");options.addView(tagHelp);
        tagInput(activity,options,"Capturar famílias das etiquetas","CatchTags",preferences.catchTags());
        tagInput(activity,options,"Fruta nas famílias das etiquetas","CandyTags",preferences.candyTags());
        int tagBerryIndex=0;for(int i=0;i<BERRIES.length;i++)if(BERRIES[i]==preferences.tagBerry())tagBerryIndex=i;
        row(activity,options,"Fruta para etiquetas",BERRY_NAMES,tagBerryIndex,index->{set("TagBerry",BERRIES[index]);});
        CheckBox manual=new CheckBox(ui);manual.setText("Aplicar frutas de raridade e etiquetas ao lançar manualmente");manual.setChecked(preferences.manualBerry());options.addView(manual);manual.setOnCheckedChangeListener((v,on)->set("ManualBerry",on));
        TextView manualHelp=new TextView(ui);manualHelp.setText("Requer Auto Frutas ativo e uma prioridade normal configurada. A fruta especial tem preferência quando disponível; senão a ordem de estoque normal é preservada. Não lança bola automaticamente.");options.addView(manualHelp);
        CheckBox shinyOnly=new CheckBox(ui);shinyOnly.setText(dev.pgrazor.PtBr.tr("Catch only shinies"));shinyOnly.setChecked(preferences.shinyOnly());options.addView(shinyOnly);
        TextView shinyOnlyHelp=new TextView(ui);shinyOnlyHelp.setText(dev.pgrazor.PtBr.tr("VGP+ still opens every Pokémon to see whether it is shiny, but throws only at shinies and leaves the rest on the map. Each one it opens counts as an encounter."));options.addView(shinyOnlyHelp);
        shinyOnly.setOnCheckedChangeListener((v,on)->{if(on!=preferences.shinyOnly())set("ShinyOnly",on);});
        TextView only=button(options,"Catch only: "+count(preferences.catchSpecies(),"All species"));
        TextView onlyHelp=new TextView(ui);onlyHelp.setText(dev.pgrazor.PtBr.tr("When species are selected, VGP+ catches only those and leaves every other Pokémon on the map, shiny or not. With \"Catch only shinies\" on as well, it catches only shinies of those species."));options.addView(onlyHelp);
        only.setOnClickListener(v->SmartCatchSpeciesDialog.showCatchOnly(activity,()->only.setText(dev.pgrazor.PtBr.tr("Catch only: "+count(preferences.catchSpecies(),"All species")))));
        Runnable refresh=()->{mode.setChecked(preferences.enabled());gymSpin.setChecked(preferences.spinGyms());auto.setChecked(preferences.autoBall());SettingsUi.enabled(mode,Control.vgpOverlay());SettingsUi.enabled(options,Control.vgpOverlay()&&preferences.enabled());if(Control.vgpOverlay()&&preferences.enabled()){SettingsUi.enabled(autoTarget,preferences.autoBall());SettingsUi.enabled(standard,!preferences.autoBall());SettingsUi.enabled(rating,preferences.variety()==0);}};
        afterVariety[0]=refresh;
        auto.setOnCheckedChangeListener((v,on)->{if(on!=preferences.autoBall())set("AutoBall",on);refresh.run();});
        mode.setOnCheckedChangeListener((v,on)->{if(on!=preferences.enabled())set("Enabled",on);refresh.run();});
        refresh.run();return refresh;
    }
    public static void show(Activity activity){
        if(activity==null||activity.isFinishing()||activity.isDestroyed())return;
        Context ui=MapTheme.wrap(activity);LinearLayout body=new LinearLayout(ui);body.setOrientation(LinearLayout.VERTICAL);
        int pad=MapTheme.dp(ui,16);body.setPadding(pad,pad,pad,pad);Runnable refresh=populate(activity,body);
        ScrollView scroll=new ScrollView(ui);scroll.addView(body);
        AlertDialog dialog=new AlertDialog.Builder(ui).setTitle(dev.pgrazor.PtBr.tr("VGP+")).setView(scroll).setPositiveButton(dev.pgrazor.PtBr.tr("Done"),null).create();
        DialogLifecycle.bind(activity,dialog,null);dialog.setOnShowListener(d->{MapTheme.style(body);MapTheme.styleButtons(dialog);refresh.run();});dialog.show();
    }
    private interface Choice {void set(int index);}
    private interface Value {void set(String value);}
    private static int bounded(String value,int max){
        try{int n=Integer.parseInt(value.trim());if(n>=0&&n<=max)return n;}catch(NumberFormatException ignored){}
        throw new IllegalArgumentException("Enter a whole number from 0 to "+max+".");
    }
    private static TextView button(LinearLayout body,String text){
        android.widget.Button b=new android.widget.Button(body.getContext());b.setText(dev.pgrazor.PtBr.tr(text));b.setAllCaps(false);body.addView(b);return b;
    }
    private static TextView row(Activity a,LinearLayout body,String title,String[] names,int selected,Choice save){
        TextView button=button(body,title+": "+names[selected]);final int[] current={selected};
        button.setOnClickListener(v->{
            String[] localizedNames=new String[names.length];
            for(int i=0;i<names.length;i++)localizedNames[i]=dev.pgrazor.PtBr.tr(names[i]);
            AlertDialog d=new AlertDialog.Builder(MapTheme.wrap(a)).setTitle(dev.pgrazor.PtBr.tr(title)).setSingleChoiceItems(localizedNames,current[0],(dialog,index)->{save.set(index);current[0]=index;button.setText(dev.pgrazor.PtBr.tr(title+": "+names[index]));dialog.dismiss();}).setNegativeButton(dev.pgrazor.PtBr.tr("Cancel"),null).create();
            DialogLifecycle.bind(a,d,null);d.setOnShowListener(x->MapTheme.styleButtons(d));d.show();
        });
        return button;
    }
    private static void tagInput(Activity a,LinearLayout body,String title,String key,String initial){
        TextView b=button(body,title+": "+(initial.isEmpty()?"Nenhuma":initial));String[] current={initial};
        b.setOnClickListener(v->{
            EditText edit=new EditText(MapTheme.wrap(a));edit.setSingleLine(true);edit.setHint("Ex.: doces, raros");edit.setText(current[0]);
            AlertDialog d=new AlertDialog.Builder(MapTheme.wrap(a)).setTitle(title).setView(edit).setPositiveButton("Salvar",null).setNegativeButton("Cancelar",null).create();
            DialogLifecycle.bind(a,d,null);d.setOnShowListener(x->{MapTheme.style(edit);MapTheme.styleButtons(d);d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(w->{
                try{String names=SmartCatchPreferences.normalizeTags(edit.getText().toString());set(key,names);current[0]=names;b.setText(title+": "+(names.isEmpty()?"Nenhuma":names));d.dismiss();}
                catch(IllegalArgumentException invalid){edit.setError(invalid.getMessage());}
            });});d.show();
        });
    }
    private static void input(Activity a,LinearLayout body,String title,String initial,Value save,boolean numeric){
        TextView button=button(body,title+": "+(initial.isEmpty()?"None":initial));final String[] current={initial};
        button.setOnClickListener(v->{
            Context ui=MapTheme.wrap(a);EditText edit=new EditText(ui);edit.setSingleLine(true);edit.setText(dev.pgrazor.PtBr.tr(current[0]));
            edit.setInputType(numeric?InputType.TYPE_CLASS_NUMBER:InputType.TYPE_CLASS_TEXT);
            AlertDialog d=new AlertDialog.Builder(ui).setTitle(dev.pgrazor.PtBr.tr(title)).setView(edit).setPositiveButton(dev.pgrazor.PtBr.tr("Save"),null).setNegativeButton(dev.pgrazor.PtBr.tr("Cancel"),null).create();
            DialogLifecycle.bind(a,d,null);d.setOnShowListener(x->{MapTheme.style(edit);MapTheme.styleButtons(d);d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(w->{
                try{String value=edit.getText().toString();save.set(value);current[0]=numeric?value.trim():normalizeSpecies(value);button.setText(dev.pgrazor.PtBr.tr(title+": "+(current[0].isEmpty()?"None":current[0])));d.dismiss();}
                catch(IllegalArgumentException e){edit.setError(e.getMessage());}
            });});d.show();
        });
    }
}

/** Feature-specific entry points; all species lists share PokemonSpeciesDialog. */
final class SmartCatchSpeciesDialog {
    private SmartCatchSpeciesDialog() {}
    static void show(Activity activity, Runnable onSaved) {
        PokemonSpeciesDialog.show(activity, "Candy species (Pinap)", "species", 200,
            SmartCatchSettings::candySpecies, SmartCatchSettings::setCandySpecies, onSaved,
            "None (no candy species selected)", null);
    }
    static void showCatchOnly(Activity activity, Runnable onSaved) {
        PokemonSpeciesDialog.show(activity, "Catch only these species", "species", 200,
            SmartCatchSettings::catchSpecies, SmartCatchSettings::setCatchSpecies, onSaved,
            "All species (no selection)", null);
    }
    static void show(Activity activity, String title, String noun, int max,
                     java.util.function.Supplier<String> load, java.util.function.Consumer<String> save, Runnable onSaved) {
        PokemonSpeciesDialog.show(activity, title, noun, max, load, save, onSaved, "All species (no selection)", null);
    }
}
