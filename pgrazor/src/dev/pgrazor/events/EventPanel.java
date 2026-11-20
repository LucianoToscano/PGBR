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
            .setNegativeButton(dev.pgrazor.PtBr.tr("Close"), null).create(), new String[]{"Halloween 2026", "Área Selvagem · novembro de 2026", "Evento Rota Doces Eternatus"},
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
        TextView gpxStatus=new TextView(ui);
        gpxStatus.setTextSize(15);
        gpxStatus.setPadding(0,0,0,pad/2);
        layout.addView(gpxStatus);
        android.widget.Button importGpx=new android.widget.Button(ui);
        importGpx.setAllCaps(false);
        importGpx.setText("Importar GPX");
        layout.addView(importGpx);
        android.widget.Button savedGpx=new android.widget.Button(ui);
        savedGpx.setAllCaps(false);
        savedGpx.setText("Carregar GPX salvo");
        layout.addView(savedGpx);
        final String[] selected={dev.pgrazor.EternatusRouteRunner.selectedTemplate()};
        android.widget.Button select=new android.widget.Button(ui);
        select.setAllCaps(false);
        select.setText("Escolher pesquisa");
        for (String[] task : dev.pgrazor.ResearchSettings.observedTemplates()) {
            if (task[0].equals(selected[0])) {
                select.setText(dev.pgrazor.PtBr.tr(task[1]) + "\n" + dev.pgrazor.PtBr.tr(task[2]));
                break;
            }
        }
        layout.addView(select);
        android.widget.CheckBox verifiedReward=new android.widget.CheckBox(ui);
        verifiedReward.setText(dev.pgrazor.PtBr.tr("Conferi que a missão selecionada concede 25 doces Eternatus"));
        layout.addView(verifiedReward);
        select.setOnClickListener(v->{
            java.util.List<String[]> catalog=dev.pgrazor.ResearchSettings.observedTemplates();
            if(catalog.isEmpty()){
                android.widget.Toast.makeText(activity,"Nenhuma pesquisa foi detectada. Abra Pesquisas após receber uma missão no jogo.",android.widget.Toast.LENGTH_LONG).show();
                return;
            }
            String[] labels = new String[catalog.size()];
            java.util.Set<String> seen = new java.util.HashSet<>();
            java.util.Set<String> duplicates = new java.util.HashSet<>();
            for (int i = 0; i < labels.length; i++) {
                String[] q = catalog.get(i);
                labels[i] = dev.pgrazor.PtBr.tr(q[1]) + "\n" + dev.pgrazor.PtBr.tr(q[2]);
                if (!seen.add(labels[i])) duplicates.add(labels[i]);
            }
            for (int i = 0; i < labels.length; i++)
                if (duplicates.contains(labels[i])) labels[i] += "\n" + catalog.get(i)[0];
            new AlertDialog.Builder(ui).setTitle("Escolher pesquisa")
                .setItems(labels,(d,choice)->{
                    String[] q = catalog.get(choice);
                    if (dev.pgrazor.EternatusRouteRunner.chooseTemplate(q[0])) {
                        verifiedReward.setChecked(false);
                        selected[0] = q[0];
                        select.setText(labels[choice]);
                    }
                }).setNegativeButton("Cancelar",null).show();
        });
        android.widget.Button startAuto=new android.widget.Button(ui);
        startAuto.setAllCaps(false);startAuto.setText("Ativar rota");layout.addView(startAuto);
        android.widget.Button stopAuto=new android.widget.Button(ui);
        stopAuto.setAllCaps(false);stopAuto.setText("Desativar");layout.addView(stopAuto);
        android.widget.Button skipMissing=new android.widget.Button(ui);
        skipMissing.setAllCaps(false);skipMissing.setText("Pular parada sem missão (manual)");
        layout.addView(skipMissing);
        TextView diagnostic=new TextView(ui);
        diagnostic.setTextSize(12);
        diagnostic.setText("O avanço automático exige resgate real da missão. Se uma parada não tiver pesquisa, pule manualmente; isso não registra doces.");
        layout.addView(diagnostic);
        TextView tally = new TextView(ui);
        tally.setPadding(0,pad/2,0,0);
        tally.setTextSize(13);
        layout.addView(tally);
        ScrollView scroll=new ScrollView(ui);scroll.addView(layout);
        AlertDialog dialog=new AlertDialog.Builder(ui).setTitle(dev.pgrazor.PtBr.tr("Evento Rota Doces Eternatus"))
            .setView(scroll).setNegativeButton(dev.pgrazor.PtBr.tr("Fechar"),null).create();
        importGpx.setOnClickListener(v->{dialog.dismiss();dev.pgrazor.GpxRouteDialog.importGpx(activity,"Eternatus",selectedRoute->{
            if(!dev.pgrazor.EternatusRouteRunner.chooseRoute(selectedRoute))
                android.widget.Toast.makeText(activity,"Rota indisponível ou com mais de 400 paradas.",android.widget.Toast.LENGTH_LONG).show();
            dev.pgrazor.GpxRouteDialog.whenResumed(activity,()->showEternatus(activity,dark));
        });});
        savedGpx.setOnClickListener(v->{dialog.dismiss();dev.pgrazor.GpxRouteDialog.chooseSaved(activity,selectedRoute->{
            if(!dev.pgrazor.EternatusRouteRunner.chooseRoute(selectedRoute))
                android.widget.Toast.makeText(activity,"Rota indisponível ou com mais de 400 paradas.",android.widget.Toast.LENGTH_LONG).show();
            dev.pgrazor.GpxRouteDialog.whenResumed(activity,()->showEternatus(activity,dark));
        });});
        startAuto.setOnClickListener(v->{
            if(!verifiedReward.isChecked()){
                android.widget.Toast.makeText(activity,"Confirme primeiro a recompensa de 25 doces no jogo.",android.widget.Toast.LENGTH_LONG).show();return;
            }
            new AlertDialog.Builder(ui).setTitle("Iniciar rota Eternatus?")
                .setMessage("Teleportar para a primeira coordenada e ativar temporariamente VGP+, giro e o filtro da missão escolhida? Pesquisas de campo inacabadas diferentes podem ser excluídas. Cada nova coordenada aguardará cooldown estimado.")
                .setNegativeButton("Cancelar",null)
                .setPositiveButton("Iniciar e teleportar",(d,w)->{
                    if(!dev.pgrazor.EternatusRouteRunner.start(activity))
                        android.widget.Toast.makeText(activity,dev.pgrazor.EternatusRouteRunner.status(),android.widget.Toast.LENGTH_LONG).show();
                    else dialog.dismiss();
                }).show();
        });
        stopAuto.setOnClickListener(v->{
            dev.pgrazor.EternatusRouteRunner.stop("Rota desativada.");
            dev.pgrazor.EternatusMission.stop("Desativado.");
        });
        skipMissing.setOnClickListener(v->new AlertDialog.Builder(ui)
            .setTitle("Pular esta parada sem resgate?")
            .setMessage("Avançar manualmente para a próxima coordenada? Esta ação NÃO confirma uma missão, NÃO registra doces e respeita o cooldown.")
            .setNegativeButton("Cancelar",null)
            .setPositiveButton("Pular parada",(d,w)->{
                if(!dev.pgrazor.EternatusRouteRunner.skipCurrentStop())
                    android.widget.Toast.makeText(activity,
                        "Não foi possível pular: aguarde cooldown, verifique a rota ou encerre a última parada.",
                        android.widget.Toast.LENGTH_LONG).show();
            }).show());
        android.os.Handler clock=new android.os.Handler(android.os.Looper.getMainLooper());
        bind(activity,dialog,()->clock.removeCallbacksAndMessages(null));
        dialog.show();
        Runnable poll=new Runnable(){
            public void run(){
                if(!dialog.isShowing())return;
                dev.pgrazor.GpxRoute planned=dev.pgrazor.EternatusRouteRunner.plannedRoute();
                boolean active=dev.pgrazor.EternatusRouteRunner.active();
                gpxStatus.setText(planned == null ? "Escolha uma rota GPX para começar."
                    : planned.name + " · " + planned.size() + " paradas\n" + dev.pgrazor.EternatusRouteRunner.status());
                importGpx.setEnabled(!active);
                savedGpx.setEnabled(!active);
                select.setEnabled(!active);
                verifiedReward.setEnabled(!active);
                startAuto.setEnabled(!active && planned != null && verifiedReward.isChecked() && !selected[0].isEmpty());
                stopAuto.setEnabled(active || dev.pgrazor.EternatusMission.active());
                skipMissing.setEnabled(active && dev.pgrazor.EternatusRouteRunner.totalStops() >
                    dev.pgrazor.EternatusRouteRunner.currentStop());
                tally.setText("Resgates registrados nesta conta: " + dev.pgrazor.EternatusProgress.confirmed(activity));
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
