package dev.pgrazor;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

/** Settings apply only to the PGBR-owned World Map route drawing. */
final class RouteTraceSettings {
    private static final String PREFIX="routeTrace.";
    private static final int[] COLORS={0xffeeeeee,0xff168fea,0xff40b97b,0xffffb74d,0xffb388ff,0xff606d79};
    private static final String[] NAMES={"Branco/cinza","Azul","Verde","Laranja","Roxo","Cinza escuro"};
    private RouteTraceSettings() {}

    static RouteTraceStyle read(SharedPreferences p) {
        try {
            return new RouteTraceStyle(p.getInt(PREFIX+"width",5),p.getInt(PREFIX+"opacity",100),
                COLORS[index(p.getInt(PREFIX+"remaining",0),0)],
                COLORS[index(p.getInt(PREFIX+"completed",1),1)],
                p.getBoolean(PREFIX+"progress",true),p.getBoolean(PREFIX+"markers",true),
                p.getBoolean(PREFIX+"preview",true),p.getBoolean(PREFIX+"summary",true));
        } catch(ClassCastException malformed) { return RouteTraceStyle.defaults(); }
    }
    private static int index(int i,int fallback) {return i>=0&&i<COLORS.length?i:fallback;}
    private static int indexOf(int color,int fallback) {
        for(int i=0;i<COLORS.length;i++)if(COLORS[i]==color)return i;
        return fallback;
    }
    private static CheckBox toggle(Context c,LinearLayout panel,String label,boolean value) {
        CheckBox v=new CheckBox(c);v.setText(label);v.setChecked(value);panel.addView(v);return v;
    }
    private static SeekBar slider(Context c,LinearLayout panel,String label,int min,int max,int value,String suffix) {
        TextView text=new TextView(c);panel.addView(text);
        SeekBar seek=new SeekBar(c);seek.setId(android.view.View.generateViewId());text.setLabelFor(seek.getId());
        seek.setContentDescription(label);seek.setMax(max-min);seek.setProgress(value-min);panel.addView(seek);
        text.setText(label+": "+value+suffix);
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onProgressChanged(SeekBar s,int n,boolean user){text.setText(label+": "+(n+min)+suffix);}
            public void onStartTrackingTouch(SeekBar s){}
            public void onStopTrackingTouch(SeekBar s){}
        });return seek;
    }
    private static Spinner palette(Context c,LinearLayout panel,String label,int selected) {
        TextView text=new TextView(c);text.setText(label);panel.addView(text);
        Spinner spin=new Spinner(c);spin.setId(android.view.View.generateViewId());text.setLabelFor(spin.getId());
        ArrayAdapter<String> a=new ArrayAdapter<>(c,android.R.layout.simple_spinner_item,NAMES);
        a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spin.setAdapter(a);spin.setSelection(selected);spin.setContentDescription(label);panel.addView(spin);return spin;
    }
    static void show(Activity a,SharedPreferences p,Runnable changed) {
        if(a.isFinishing()||a.isDestroyed())return;
        RouteTraceStyle s=read(p);Context c=MapTheme.wrap(a);
        LinearLayout panel=new LinearLayout(c);panel.setOrientation(LinearLayout.VERTICAL);
        int pad=MapTheme.dp(a,16);panel.setPadding(pad,pad,pad,pad);
        SeekBar width=slider(c,panel,"Espessura",2,12,s.width," dp");
        SeekBar alpha=slider(c,panel,"Opacidade",30,100,s.opacity,"%");
        Spinner remaining=palette(c,panel,"Cor do caminho restante",indexOf(s.remaining,0));
        Spinner completed=palette(c,panel,"Cor do caminho percorrido",indexOf(s.completed,1));
        CheckBox progress=toggle(c,panel,"Destacar trecho percorrido",s.showProgress);
        CheckBox markers=toggle(c,panel,"Exibir marcadores",s.showMarkers);
        CheckBox preview=toggle(c,panel,"Estilo novo também na prévia",s.showPreview);
        CheckBox summary=toggle(c,panel,"Exibir resumo de progresso",s.showSummary);
        ScrollView scroll=new ScrollView(c);scroll.addView(panel);
        AlertDialog d=new AlertDialog.Builder(c).setTitle("Aparência do percurso — Mapa Mundial")
            .setView(scroll).setNegativeButton("Cancelar",null)
            .setNeutralButton("Restaurar padrão",null)
            .setPositiveButton("Salvar",(dialog,which)->{
                p.edit().putInt(PREFIX+"width",width.getProgress()+2).putInt(PREFIX+"opacity",alpha.getProgress()+30)
                    .putInt(PREFIX+"remaining",remaining.getSelectedItemPosition()).putInt(PREFIX+"completed",completed.getSelectedItemPosition())
                    .putBoolean(PREFIX+"progress",progress.isChecked()).putBoolean(PREFIX+"markers",markers.isChecked())
                    .putBoolean(PREFIX+"preview",preview.isChecked()).putBoolean(PREFIX+"summary",summary.isChecked()).apply();
                changed.run();
            }).create();
        DialogLifecycle.bind(a,d,null);
        d.setOnShowListener(dialog->{MapTheme.styleButtons(d);d.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v->{
            width.setProgress(3);alpha.setProgress(70);remaining.setSelection(0);completed.setSelection(1);
            progress.setChecked(true);markers.setChecked(true);preview.setChecked(true);summary.setChecked(true);
        });});d.show();
    }
}
