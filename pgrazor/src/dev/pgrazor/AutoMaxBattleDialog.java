package dev.pgrazor;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/** Controles temporarios da sessao Batalha Max. Nunca altera as preferencias salvas. */
public final class AutoMaxBattleDialog {
    private AutoMaxBattleDialog(){}

    public static void show(Activity activity){
        if(activity==null||activity.isFinishing()||activity.isDestroyed())return;
        Context ui=MapTheme.wrap(activity);
        LinearLayout body=new LinearLayout(ui);
        body.setOrientation(LinearLayout.VERTICAL);
        int pad=MapTheme.dp(ui,16);body.setPadding(pad,pad,pad,pad);

        TextView description=new TextView(ui);
        description.setText("Ativa temporariamente os ataques do motor de combate para Dinamax e Gigamax. "
            +"Inicie a sessão e entre manualmente numa Batalha Max em um Ponto de Poder.\n\n"
            +"O PGBR não localiza Pontos de Poder, não gasta Partículas Max, não escolhe equipes, "
            +"não inicia batalhas nem realiza a captura das recompensas nesta função.");
        body.addView(description);
        TextView status=new TextView(ui);body.addView(status);

        CheckBox quick=new CheckBox(ui);quick.setText("Ataque Ágil automático");body.addView(quick);
        CheckBox charged=new CheckBox(ui);charged.setText("Ataque Carregado automático");body.addView(charged);
        CheckBox maxAttack=new CheckBox(ui);maxAttack.setText("Ataque Max automático");body.addView(maxAttack);
        CheckBox dodge=new CheckBox(ui);dodge.setText("Esquiva automática");body.addView(dodge);
        int saved=Control.battleOptions()&AutoMaxBattle.OPTION_MASK;
        if(saved==0)saved=AutoMaxBattle.OPTION_MASK;
        quick.setChecked((saved&AutoMaxBattle.QUICK)!=0);
        charged.setChecked((saved&AutoMaxBattle.CHARGED)!=0);
        maxAttack.setChecked((saved&AutoMaxBattle.MAX_ATTACK)!=0);
        dodge.setChecked((saved&AutoMaxBattle.DODGE)!=0);

        Button start=new Button(ui);start.setAllCaps(false);start.setText("Iniciar sessão");body.addView(start);
        Button stop=new Button(ui);stop.setAllCaps(false);stop.setText("Encerrar sessão");body.addView(stop);
        View[] options={quick,charged,maxAttack,dodge};
        Runnable update=()->{
            boolean running=AutoMaxBattle.running();
            status.setText(AutoMaxBattle.status()+"\nSuporte à batalha: "+dev.pgrazor.PtBr.tr(Control.battleStatus()));
            for(View option:options)SettingsUi.enabled(option,!running);
            start.setEnabled(!running);stop.setEnabled(running);
        };
        start.setOnClickListener(v->{
            int mask=(quick.isChecked()?AutoMaxBattle.QUICK:0)
                |(charged.isChecked()?AutoMaxBattle.CHARGED:0)
                |(maxAttack.isChecked()?AutoMaxBattle.MAX_ATTACK:0)
                |(dodge.isChecked()?AutoMaxBattle.DODGE:0);
            try{AutoMaxBattle.start(mask);}
            catch(IllegalArgumentException|IllegalStateException rejected){
                Toast.makeText(activity,rejected.getMessage(),Toast.LENGTH_LONG).show();
            }
            update.run();
        });
        stop.setOnClickListener(v->{AutoMaxBattle.stop();update.run();});
        update.run();

        ScrollView scroll=new ScrollView(ui);scroll.addView(body);
        AlertDialog dialog=new AlertDialog.Builder(ui)
            .setTitle("Batalha Max Automática — Sessão").setView(scroll)
            .setPositiveButton("Concluir",null).create();
        DialogLifecycle.bind(activity,dialog,null);
        dialog.setOnShowListener(unused->{
            MapTheme.style(body);MapTheme.styleButtons(dialog);
            if(dialog.getWindow()!=null)dialog.getWindow().setBackgroundDrawable(new ColorDrawable(MapTheme.surface()));
        });
        dialog.show();
    }
}
