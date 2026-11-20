package dev.pgrazor.events;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.ContextThemeWrapper;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

/** Event selection stays independent of gameplay policy and native object classification. */
public final class EventPanel {
    private EventPanel() {}

    public static void showSelector(Activity activity, boolean dark) {
        if (activity.isFinishing() || activity.isDestroyed()) return;
        AlertDialog dialog = dev.pgrazor.ChoiceList.attach(new AlertDialog.Builder(theme(activity, dark))
            .setTitle(dev.pgrazor.PtBr.tr("Events"))
            .setNegativeButton(dev.pgrazor.PtBr.tr("Close"), null).create(), new String[]{"Halloween 2026", "Go Wild November 2026", "Evento Rota Doces Eternatus"},
            index -> { if (index == 0) showHalloween(activity, dark); else if(index==1) showGoWild(activity,dark); else showEternatus(activity,dark); });
        bind(activity, dialog, () -> {});
        dialog.show();
    }

    private static Context theme(Activity activity, boolean dark) {
        return new ContextThemeWrapper(activity, dark ? android.R.style.Theme_Material_Dialog_Alert
            : android.R.style.Theme_Material_Light_Dialog_Alert);
    }

    public static void showHalloween(Activity activity, boolean dark) {
        if (activity.isFinishing() || activity.isDestroyed()) return;
        Context context = theme(activity, dark);
        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        int pad = Math.round(20 * context.getResources().getDisplayMetrics().density);
        content.setPadding(pad, pad / 2, pad, pad / 2);
        TextView description = new TextView(context);
        description.setText(dev.pgrazor.PtBr.tr("Opens every visible map box and skips the encounter screen for any Pokemon result."));
        description.setTextSize(15);
        content.addView(description);
        TextView status = new TextView(context);
        status.setPadding(0, pad / 2, 0, pad / 2);
        status.setTextSize(13);
        content.addView(status);
        Switch collector = new Switch(context);
        collector.setText(dev.pgrazor.PtBr.tr("Open boxes and skip encounters"));
        collector.setTextSize(15);
        content.addView(collector);

        ScrollView scroller = new ScrollView(context);
        scroller.addView(content);

        AlertDialog dialog = new AlertDialog.Builder(context).setTitle(dev.pgrazor.PtBr.tr("Halloween 2026"))
            .setView(scroller).setNegativeButton(dev.pgrazor.PtBr.tr("Close"), null).create();
        Handler handler = new Handler(Looper.getMainLooper());
        Runnable refresh = new Runnable() {
            public void run() {
                if (!dialog.isShowing()) return;
                boolean available = EventSettings.boxCollectorAvailable();
                collector.setEnabled(available);
                collector.setChecked(EventSettings.boxCollectorEnabled(activity));
                status.setText(dev.pgrazor.PtBr.tr(available
                    ? "Ready to open visible boxes. Encounter screens are skipped without catching."
                    : "Unavailable: the box collector has not been verified. Collection is off."));
                handler.postDelayed(this, 500);
            }
        };
        collector.setOnCheckedChangeListener((button, checked) -> {
            if (checked == EventSettings.boxCollectorEnabled(activity)) return;
            EventSettings.setBoxCollectorRequested(activity, checked);
            button.setChecked(EventSettings.boxCollectorEnabled(activity));
        });
        bind(activity, dialog, () -> handler.removeCallbacks(refresh));
        dialog.setOnShowListener(ignored -> refresh.run());
        dialog.show();
    }

    public static void showGoWild(Activity activity, boolean dark) {
        if (activity.isFinishing() || activity.isDestroyed()) return;
        Context context = theme(activity, dark);
        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        int pad = Math.round(20 * context.getResources().getDisplayMetrics().density);
        content.setPadding(pad, pad / 2, pad, pad / 2);
        TextView description = new TextView(context);
        description.setText(dev.pgrazor.PtBr.tr("Prefer GO Safari Balls for Mighty Pokemon. If Safari Balls are unavailable"
            + " or not allowed, your normal ball settings apply."));
        description.setTextSize(15);
        content.addView(description);

        Switch vgp = new Switch(context);
        vgp.setText(dev.pgrazor.PtBr.tr("Use Safari Balls in VGP+"));
        vgp.setTextSize(15);
        vgp.setChecked(EventSettings.wildVgpSafari(activity));
        content.addView(vgp);
        TextView vgpHelp = new TextView(context);
        vgpHelp.setText(dev.pgrazor.PtBr.tr("Applies while VGP+ is running."));
        vgpHelp.setTextSize(13);
        vgpHelp.setPadding(0, 0, 0, pad / 2);
        content.addView(vgpHelp);

        Switch autoBall = new Switch(context);
        autoBall.setText(dev.pgrazor.PtBr.tr("Use Safari Balls in Auto Ball"));
        autoBall.setTextSize(15);
        autoBall.setChecked(EventSettings.wildAutoBallSafari(activity));
        content.addView(autoBall);
        TextView autoHelp = new TextView(context);
        autoHelp.setText(dev.pgrazor.PtBr.tr("Applies while Auto Ball is enabled."));
        autoHelp.setTextSize(13);
        content.addView(autoHelp);

        TextView status = new TextView(context);
        status.setPadding(0, pad / 2, 0, 0);
        status.setTextSize(13);
        content.addView(status);

        ScrollView scroller = new ScrollView(context);
        scroller.addView(content);

        AlertDialog dialog = new AlertDialog.Builder(context).setTitle(dev.pgrazor.PtBr.tr("Go Wild November 2026"))
            .setView(scroller).setNegativeButton(dev.pgrazor.PtBr.tr("Close"), null).create();
        vgp.setOnCheckedChangeListener((button, checked) -> {
            if (checked == EventSettings.wildVgpSafari(activity)) return;
            if (!EventSettings.setWildVgpSafari(activity, checked)) {
                status.setText(dev.pgrazor.PtBr.tr("Couldn't save that change. Please try again."));
                button.setChecked(EventSettings.wildVgpSafari(activity));
                return;
            }
            status.setText(dev.pgrazor.PtBr.tr(""));
            dev.pgrazor.Control.event("event-wild-safari", "vgp_plus=" + (checked ? "ON" : "OFF"));
        });
        autoBall.setOnCheckedChangeListener((button, checked) -> {
            if (checked == EventSettings.wildAutoBallSafari(activity)) return;
            if (!EventSettings.setWildAutoBallSafari(activity, checked)) {
                status.setText(dev.pgrazor.PtBr.tr("Couldn't save that change. Please try again."));
                button.setChecked(EventSettings.wildAutoBallSafari(activity));
                return;
            }
            status.setText(dev.pgrazor.PtBr.tr(""));
            dev.pgrazor.Control.event("event-wild-safari", "auto_ball=" + (checked ? "ON" : "OFF"));
        });
        bind(activity, dialog, () -> {});
        dialog.show();
    }


    /** One-at-a-time Eternatus research tracking; no unverified reward recognition. */
    public static void showEternatus(Activity activity,boolean dark){
        if(activity.isFinishing()||activity.isDestroyed())return;
        Context ui=theme(activity,dark);
        LinearLayout layout=new LinearLayout(ui);layout.setOrientation(LinearLayout.VERTICAL);
        int pad=Math.round(16*ui.getResources().getDisplayMetrics().density);
        layout.setPadding(pad,pad,pad,pad);
        TextView information=new TextView(ui);
        information.setText(dev.pgrazor.PtBr.tr("Modo uma por vez: confira a missão de jogada excelente com recompensa de 25 doces Eternatus, complete-a, resgate e só então avance para a próxima parada GPX."));
        information.setTextSize(14);layout.addView(information);
        TextView limitation=new TextView(ui);
        limitation.setText(dev.pgrazor.PtBr.tr("A identificação exata da recompensa ainda não foi validada no jogo. Este painel é um acompanhamento manual: não gira paradas, captura, resgata ou avança coordenadas automaticamente."));
        limitation.setTextSize(12);layout.addView(limitation);
        TextView autoHeader=new TextView(ui);
        autoHeader.setText("PESQUISA AUTOMÁTICA POR MODELO EXATO");
        autoHeader.setTextSize(14);autoHeader.setPadding(0,pad,0,0);layout.addView(autoHeader);
        TextView autoStatus=new TextView(ui);autoStatus.setTextSize(12);layout.addView(autoStatus);
        final String[] selected={dev.pgrazor.EternatusMission.selected()};
        android.widget.Button select=new android.widget.Button(ui);
        select.setText("Selecionar missão do catálogo do jogo");layout.addView(select);
        select.setOnClickListener(v->{
            java.util.List<String[]> catalog=dev.pgrazor.ResearchSettings.observedTemplates();
            if(catalog.isEmpty()){
                android.widget.Toast.makeText(activity,"Nenhuma pesquisa foi detectada. Abra Pesquisas após receber uma missão no jogo.",android.widget.Toast.LENGTH_LONG).show();
                return;
            }
            int limit=Math.min(80,catalog.size());
            String[] labels=new String[limit];
            for(int i=0;i<limit;i++){
                String[] q=catalog.get(i);
                labels[i]=q[1]+" | "+q[2]+"\nModelo: "+q[0];
            }
            new AlertDialog.Builder(ui).setTitle("Escolher pesquisa observada")
                .setItems(labels,(d,choice)->{
                    selected[0]=catalog.get(choice)[0];
                    autoStatus.setText("Modelo selecionado: "+selected[0]);
                }).setNegativeButton("Cancelar",null).show();
        });
        android.widget.Button enableResearch=new android.widget.Button(ui);
        enableResearch.setText("Ativar resgate e filtro de missão exata");layout.addView(enableResearch);
        enableResearch.setOnClickListener(v->{
            if(selected[0]==null||selected[0].isEmpty()){
                android.widget.Toast.makeText(activity,"Primeiro selecione a missão exata no catálogo.",android.widget.Toast.LENGTH_LONG).show();return;
            }
            new AlertDialog.Builder(ui)
                .setTitle("Confirmar pesquisa: 25 doces Eternatus")
                .setMessage("Você confirmou NO JOGO que o modelo "+selected[0]+" concede 25 doces Eternatus? O PGBR ainda não lê o tipo e a quantidade de doces. Ativar exclui automaticamente outras pesquisas de campo NÃO concluídas e resgata somente o modelo escolhido quando estiver concluído. Outras pesquisas concluídas são preservadas. Essas exclusões são irreversíveis. O GPX, a captura e os giros continuam independentes.")
                .setNegativeButton("Cancelar",null)
                .setPositiveButton("Ativar",(d,w)->{
                    if(!dev.pgrazor.EternatusMission.start(activity,selected[0]))
                        android.widget.Toast.makeText(activity,"Ativação recusada: jogo desconectado ou pesquisa desconhecida.",android.widget.Toast.LENGTH_LONG).show();
                    autoStatus.setText(dev.pgrazor.EternatusMission.status());
                }).show();
        });
        android.widget.Button disableResearch=new android.widget.Button(ui);
        disableResearch.setText("Desativar filtro automático do Eternatus");layout.addView(disableResearch);
        disableResearch.setOnClickListener(v->{
            dev.pgrazor.EternatusMission.stop("Desativado pelo treinador.");
            autoStatus.setText(dev.pgrazor.EternatusMission.status());
        });
        autoStatus.setText(dev.pgrazor.EternatusMission.status());
        android.widget.Button route=new android.widget.Button(ui);
        route.setText(dev.pgrazor.PtBr.tr("Abrir caminhada GPX"));layout.addView(route);
        android.widget.Button research=new android.widget.Button(ui);
        research.setText(dev.pgrazor.PtBr.tr("Configurar pesquisas e resgates"));layout.addView(research);
        TextView tally=new TextView(ui);tally.setTextSize(15);layout.addView(tally);
        android.widget.CheckBox received=new android.widget.CheckBox(ui);
        received.setText(dev.pgrazor.PtBr.tr("Recebi a missão correta (25 doces)"));layout.addView(received);
        android.widget.CheckBox excellent=new android.widget.CheckBox(ui);
        excellent.setText(dev.pgrazor.PtBr.tr("Fiz a jogada excelente e concluí a missão"));layout.addView(excellent);
        android.widget.CheckBox claimed=new android.widget.CheckBox(ui);
        claimed.setText(dev.pgrazor.PtBr.tr("Resgatei os 25 doces no jogo"));layout.addView(claimed);
        android.widget.Button next=new android.widget.Button(ui);
        next.setText(dev.pgrazor.PtBr.tr("Confirmar resgate e preparar próxima parada"));layout.addView(next);
        final int[] completed={dev.pgrazor.EternatusProgress.confirmed(activity)};
        Runnable refresh=()->{
            boolean automatic=dev.pgrazor.EternatusMission.active();
            received.setEnabled(!automatic);
            excellent.setEnabled(!automatic&&received.isChecked());
            claimed.setEnabled(!automatic&&received.isChecked()&&excellent.isChecked());
            next.setEnabled(!automatic&&received.isChecked()&&excellent.isChecked()&&claimed.isChecked());
            tally.setText(dev.pgrazor.PtBr.tr("Resgates confirmados nesta conta: "+completed[0]+
                " | Recompensas de 25 doces informadas: "+(completed[0]*25))+
                (automatic?"\nModo automático ativo: checklist manual bloqueado para evitar contagem duplicada.":""));
        };
        received.setOnCheckedChangeListener((v,checked)->{
            if(!checked){excellent.setChecked(false);claimed.setChecked(false);}
            refresh.run();
        });
        excellent.setOnCheckedChangeListener((v,checked)->{
            if(!checked)claimed.setChecked(false);
            refresh.run();
        });
        claimed.setOnCheckedChangeListener((v,checked)->refresh.run());
        next.setOnClickListener(v->{
            if(!received.isChecked()||!excellent.isChecked()||!claimed.isChecked())return;
            if(!dev.pgrazor.EternatusProgress.confirm(activity)){
                android.widget.Toast.makeText(activity,"Não foi possível salvar o progresso. Tente novamente.",android.widget.Toast.LENGTH_LONG).show();
                return;
            }
            completed[0]=dev.pgrazor.EternatusProgress.confirmed(activity);
            received.setChecked(false);excellent.setChecked(false);claimed.setChecked(false);
            refresh.run();
        });
        refresh.run();
        android.widget.Button reset=new android.widget.Button(ui);
        reset.setText(dev.pgrazor.PtBr.tr("Zerar contagem confirmada"));layout.addView(reset);
        reset.setOnClickListener(v->new AlertDialog.Builder(ui)
            .setTitle(dev.pgrazor.PtBr.tr("Zerar progresso da Rota Doces Eternatus?"))
            .setMessage(dev.pgrazor.PtBr.tr("A contagem manual de missões confirmadas será zerada nesta conta. Isso não modifica o inventário nem as pesquisas do jogo."))
            .setNegativeButton(dev.pgrazor.PtBr.tr("Cancelar"),null)
            .setPositiveButton(dev.pgrazor.PtBr.tr("Zerar"),(d,w)->{
                if(dev.pgrazor.EternatusProgress.clear(activity)){
                    completed[0]=0;refresh.run();
                }else android.widget.Toast.makeText(activity,"Não foi possível zerar o progresso.",android.widget.Toast.LENGTH_LONG).show();
            }).show());
        TextView note=new TextView(ui);
        note.setText(dev.pgrazor.PtBr.tr("Se a parada entregar outra missão, não marque como recebida. Confira o tempo de espera e o estado da rota antes de seguir."));
        note.setTextSize(13);layout.addView(note);
        ScrollView scroll=new ScrollView(ui);scroll.addView(layout);
        AlertDialog dialog=new AlertDialog.Builder(ui).setTitle(dev.pgrazor.PtBr.tr("Evento Rota Doces Eternatus"))
            .setView(scroll).setNegativeButton(dev.pgrazor.PtBr.tr("Fechar"),null).create();
        route.setOnClickListener(v->{dialog.dismiss();dev.pgrazor.AutoWalkDialog.show(activity);});
        research.setOnClickListener(v->{dialog.dismiss();dev.pgrazor.ResearchSettings.show(activity);});
        android.os.Handler clock=new android.os.Handler(android.os.Looper.getMainLooper());
        bind(activity,dialog,()->clock.removeCallbacksAndMessages(null));
        dialog.show();
        Runnable poll=new Runnable(){
            public void run(){
                if(!dialog.isShowing())return;
                autoStatus.setText(dev.pgrazor.EternatusMission.status());
                int confirmed=dev.pgrazor.EternatusProgress.confirmed(activity);
                if(completed[0]!=confirmed){completed[0]=confirmed;refresh.run();}
                clock.postDelayed(this,1000);
            }
        };
        clock.post(poll);
    }

    private static void bind(Activity owner, AlertDialog dialog, Runnable cleanup) {
        Application application = owner.getApplication();
        Application.ActivityLifecycleCallbacks lifecycle = new Application.ActivityLifecycleCallbacks() {
            public void onActivityPaused(Activity activity) { if (activity == owner) dialog.dismiss(); }
            public void onActivityDestroyed(Activity activity) { if (activity == owner) dialog.dismiss(); }
            public void onActivityCreated(Activity activity, Bundle state) {}
            public void onActivityStarted(Activity activity) {}
            public void onActivityResumed(Activity activity) {}
            public void onActivityStopped(Activity activity) {}
            public void onActivitySaveInstanceState(Activity activity, Bundle state) {}
        };
        application.registerActivityLifecycleCallbacks(lifecycle);
        dialog.setOnDismissListener(ignored -> {
            cleanup.run();
            application.unregisterActivityLifecycleCallbacks(lifecycle);
        });
    }
}
