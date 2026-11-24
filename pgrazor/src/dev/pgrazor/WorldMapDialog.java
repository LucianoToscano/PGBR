package dev.pgrazor;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Rect;
import android.content.Context;
import android.content.SharedPreferences;
import android.widget.CheckBox;
import org.json.JSONObject;
import org.json.JSONArray;
import android.os.Handler;
import android.os.Looper;
import android.text.Html;
import android.text.InputFilter;
import android.text.InputType;
import android.text.method.LinkMovementMethod;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/** In-activity map picker. Merely browsing/selecting does not change location. */
public final class WorldMapDialog {
    /**
     * A GPX route imported or picked for the route builder. The system file picker pauses the host
     * and dismisses this dialog, so the choice is stashed here and restored as the draft when the
     * route builder is reopened. Cleared when the route is started or the draft is cleared.
     */
    static volatile GpxRoute pendingRoute;

    public interface DestinationListener {
        /** Return true after applying the destination, or false to keep the picker open. */
        boolean onDestination(double latitude, double longitude);
    }
    public interface RouteListener {
        /** Coordinates are copied in visit order; loop repeats from the first stop. Return true only after accepting the route. */
        boolean onRoute(double[][] waypoints, boolean loop);
    }

    public static AlertDialog show(Activity activity, double latitude, double longitude, DestinationListener listener) {
        return show(activity, latitude, longitude, listener, null);
    }

    public static AlertDialog show(Activity activity, double latitude, double longitude, DestinationListener listener, DestinationListener walk) {
        return show(activity,latitude,longitude,listener,walk,null);
    }

    public static AlertDialog show(Activity activity, double latitude, double longitude, DestinationListener listener, DestinationListener walk, RouteListener route) {
        return show(activity,latitude,longitude,listener,walk,route,false);
    }
    public static AlertDialog show(Activity activity,double latitude,double longitude,DestinationListener listener,DestinationListener walk,RouteListener route,boolean openLocations) {
        if (!Double.isFinite(latitude) || !Double.isFinite(longitude) || latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180)
            throw new IllegalArgumentException("Invalid map coordinates");
        Context ui = MapTheme.wrap(activity); // themed chrome; the map view itself keeps the real Activity
        ResponsiveLayout layout = new ResponsiveLayout(activity);
        layout.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(activity, 12);
        layout.setPadding(pad, pad, pad, pad);
        TextView hint = label(ui, "Blue dot: you. Red pin: destination. Drag to pan; pinch to zoom.", 13);
        layout.addView(hint);
        TextView selected = label(ui, "", 15);
        selected.setPadding(0, dp(activity, 8), 0, dp(activity, 8));
        LinearLayout selectionRow = new LinearLayout(ui);
        selectionRow.setGravity(Gravity.CENTER_VERTICAL);
        // Selection panel is below the map, next to destination actions.
        Button saved=button(ui,"Locais");
        selectionRow.addView(saved,new LinearLayout.LayoutParams(0,dp(activity,48),1));
        Button routeMode = button(ui, "Voltar ao mapa");
        routeMode.setVisibility(View.GONE);
        // Internal GPX action, invoked by the single Locais library; not a second navigation button.
        Button savedRoutes=button(ui,"Rotas");
        layout.addView(selectionRow);
        TextView attribution = label(ui, "", 12);
        attribution.setGravity(Gravity.RIGHT);
        attribution.setPadding(0, 0, 0, dp(activity, 6));
        attribution.setText(dev.pgrazor.PtBr.tr(Html.fromHtml("© <a href=\"https://www.openstreetmap.org/copyright\">OpenStreetMap contributors</a>", Html.FROM_HTML_MODE_LEGACY)));
        attribution.setMovementMethod(LinkMovementMethod.getInstance());
        layout.addView(attribution);
        TextView status = label(ui, "Loading map…", 12);
        MapView map = new MapView(activity, latitude, longitude, selected, status);
        final AlertDialog[] hotspotsDialog = {null}, cooldownDialog = {null};

        Button search = button(ui, "Search places, addresses or coordinates");
        layout.addView(search, 0, new LinearLayout.LayoutParams(-1, dp(activity, 48)));
        final AlertDialog[] searchDialog = {null};
        search.setOnClickListener(v -> {
            if(searchDialog[0]!=null && searchDialog[0].isShowing()) return;
            searchDialog[0]=MapSearchDialog.show(activity, MapProjection.latitude(map.centerY),
                MapProjection.longitude(map.centerX), (lat,lon) -> map.select(lat,lon));
        });
        LinearLayout layers=new LinearLayout(ui);
        SharedPreferences preferences=AccountProfiles.preferences(activity,"pgrazor-map");
        String[] names={"Stops","Gyms","Raids","Equipe GO Rocket"};
        android.widget.HorizontalScrollView layerScroll=new android.widget.HorizontalScrollView(ui);
        layerScroll.setHorizontalScrollBarEnabled(false);
        for(int i=0;i<names.length;i++){
            final int bit=1<<i;CheckBox toggle=new CheckBox(ui);toggle.setText(dev.pgrazor.PtBr.tr(names[i]));toggle.setTextSize(13);
            boolean enabled=preferences.getBoolean(names[i],i==3&&preferences.getBoolean("Stops",false));if(enabled)map.setPoiLayer(bit,true);toggle.setChecked(enabled);
            final String key=names[i];toggle.setOnCheckedChangeListener((b,on)->{map.setPoiLayer(bit,on);preferences.edit().putBoolean(key,on).apply();map.refreshPois();});
            layers.addView(toggle,new LinearLayout.LayoutParams(-2,-2));
        }
        layerScroll.addView(layers,new android.widget.FrameLayout.LayoutParams(-2,-2));
        layout.addView(layerScroll);
        CheckBox trace=new CheckBox(ui);
        trace.setText("Rota Visual • caminhada e GPX próprios");
        trace.setTextSize(13);
        trace.setChecked(preferences.getBoolean("routeTraceEnabled",false));
        TextView traceSummary=label(ui,"",12);
        LinearLayout traceTools=new LinearLayout(ui);
        traceTools.addView(trace,new LinearLayout.LayoutParams(0,-2,1));
        Button traceOptions=button(ui,"Aparência");
        traceOptions.setContentDescription("Configurar aparência do percurso");
        traceOptions.setEnabled(trace.isChecked());
        traceTools.addView(traceOptions,new LinearLayout.LayoutParams(dp(activity,100),dp(activity,48)));
        layout.addView(traceTools);
        layout.addView(traceSummary);
        map.setTraceStyle(RouteTraceSettings.read(preferences));
        traceOptions.setOnClickListener(v->RouteTraceSettings.show(activity,preferences,
            ()->map.setTraceStyle(RouteTraceSettings.read(preferences))));
        map.setTraceEnabled(trace.isChecked(),traceSummary);
        trace.setOnCheckedChangeListener((v,on)->{
            preferences.edit().putBoolean("routeTraceEnabled",on).apply();
            traceOptions.setEnabled(on);
            map.setTraceEnabled(on,traceSummary);
        });
        TextView coverage=label(ui,"Nearby locations loaded by the game",12);layout.addView(coverage);map.setPoiStatus(coverage);
        map.refreshPois();
        PlaceUi places = new PlaceUi(activity, ui, map, saved);
        CheckBox favoriteLayer=new CheckBox(ui);favoriteLayer.setText("Favoritos");favoriteLayer.setTextSize(13);
        CheckBox powerLayer=new CheckBox(ui);powerLayer.setText("Pontos de Energia importados");powerLayer.setTextSize(13);
        CheckBox officialLayer=new CheckBox(ui);officialLayer.setText("Rotas oficiais");officialLayer.setTextSize(13);
        for(CheckBox toggle:new CheckBox[]{favoriteLayer,powerLayer,officialLayer})layers.addView(toggle,new LinearLayout.LayoutParams(-2,-2));
        favoriteLayer.setChecked(preferences.getBoolean("mapFavorites",false));powerLayer.setChecked(preferences.getBoolean("mapImportedPower",false));officialLayer.setChecked(preferences.getBoolean("mapOfficialRoutes",false));
        favoriteLayer.setOnCheckedChangeListener((v,on)->{preferences.edit().putBoolean("mapFavorites",on).apply();places.setMarkerLayers(on,powerLayer.isChecked());});
        powerLayer.setOnCheckedChangeListener((v,on)->{preferences.edit().putBoolean("mapImportedPower",on).apply();places.setMarkerLayers(favoriteLayer.isChecked(),on);});
        officialLayer.setOnCheckedChangeListener((v,on)->{preferences.edit().putBoolean("mapOfficialRoutes",on).apply();map.setOfficialLayer(on);});
        places.setMarkerLayers(favoriteLayer.isChecked(),powerLayer.isChecked());map.setOfficialLayer(officialLayer.isChecked());

        android.widget.FrameLayout mapFrame=new android.widget.FrameLayout(ui);
        mapFrame.addView(map,new android.widget.FrameLayout.LayoutParams(-1,-1));
        LinearLayout floating=new LinearLayout(ui);floating.setOrientation(LinearLayout.VERTICAL);
        Button plus=button(ui,"+"),minus=button(ui,"−"),locate=button(ui,"◎"),radar=button(ui,"☰");
        plus.setContentDescription("Aumentar zoom");minus.setContentDescription("Diminuir zoom");locate.setContentDescription("Centralizar no jogador");radar.setContentDescription("Lista de recursos próximos");
        plus.setOnClickListener(v->map.zoom(1));minus.setOnClickListener(v->map.zoom(-1));locate.setOnClickListener(v->map.centerCurrentLocation());radar.setOnClickListener(v->places.openResourceCategories());
        for(Button b:new Button[]{plus,minus,locate,radar})floating.addView(b,new LinearLayout.LayoutParams(dp(activity,48),dp(activity,48)));
        android.widget.FrameLayout.LayoutParams floatParams=new android.widget.FrameLayout.LayoutParams(-2,-2,Gravity.TOP|Gravity.RIGHT);floatParams.setMargins(0,dp(activity,4),dp(activity,4),0);
        mapFrame.addView(floating,floatParams);
        layout.map = mapFrame;
        layout.addView(mapFrame,new LinearLayout.LayoutParams(-1,dp(activity,240)));
        LinearLayout walkControls=new LinearLayout(ui);Button pauseWalk=button(ui,"Pausar"),stopWalk=button(ui,"Parar percurso");
        walkControls.addView(pauseWalk,new LinearLayout.LayoutParams(0,dp(activity,48),1));
        walkControls.addView(stopWalk,new LinearLayout.LayoutParams(0,dp(activity,48),1));layout.addView(walkControls);
        map.setActiveWalkControls(walkControls,pauseWalk,stopWalk);
        pauseWalk.setOnClickListener(v->{long token=map.displayedWalkToken;MotionEngine.RouteVisualSnapshot state=Control.engine.routeVisualSnapshot(token);
            if(state==null||state.token!=token||Control.gameRoutesRunning())return;
            if(!Control.engine.setVisualRoutePaused(token,!state.paused))Toast.makeText(activity,"Escolha uma velocidade de caminhada maior que zero para continuar.",Toast.LENGTH_SHORT).show();
            else Control.saveState();map.refreshWalkControls();});
        stopWalk.setOnClickListener(v->{long token=map.displayedWalkToken;if(token==0)return;
            new AlertDialog.Builder(ui).setTitle("Encerrar percurso personalizado?").setMessage("Somente este percurso será interrompido.").setNegativeButton("Cancelar",null).setPositiveButton("Parar",(d,w)->{Control.stopVisualWalk(token);map.refreshWalkControls();}).show();});
        CheckBox loop = new CheckBox(ui); loop.setText(dev.pgrazor.PtBr.tr("Loop route")); loop.setTextSize(13);
        loop.setContentDescription(dev.pgrazor.PtBr.tr("Repeat the route from the first stop after the last")); loop.setVisibility(View.GONE);
        LinearLayout routeTools = new LinearLayout(ui);
        routeTools.setGravity(Gravity.CENTER_VERTICAL);
        routeTools.addView(routeMode,new LinearLayout.LayoutParams(0,dp(activity,48),1));
        routeTools.addView(loop,new LinearLayout.LayoutParams(0,dp(activity,48),1));
        Button clearRoute=button(ui,"Clear route");
        clearRoute.setContentDescription(dev.pgrazor.PtBr.tr("Clear all stops from the route draft"));
        clearRoute.setVisibility(View.GONE);
        routeTools.addView(clearRoute,new LinearLayout.LayoutParams(dp(activity,112),dp(activity,48)));
        layout.addView(routeTools);
        // Copy without moving the map or changing the selected destination.
        LinearLayout copyRow = new LinearLayout(ui);
        Button current=button(ui,"Copiar minha localização"), pin=button(ui,"Copiar coordenadas");
        current.setContentDescription("Copiar coordenadas da localização atual");
        pin.setContentDescription("Copiar coordenadas do destino selecionado");
        current.setOnClickListener(v -> {
            if(Control.locationKnown())CoordinateClipboard.copy(activity, Control.currentLatitude(), Control.currentLongitude(), "Your location copied");
        });
        pin.setOnClickListener(v -> CoordinateClipboard.copy(activity, map.latitude, map.longitude, "Pin location copied"));
        copyRow.addView(current, new LinearLayout.LayoutParams(0, dp(activity, 48), 1));
        copyRow.addView(pin, new LinearLayout.LayoutParams(0, dp(activity, 48), 1));
        // Coordinates and copy actions are the last block inside the map content.
        LinearLayout actions = new LinearLayout(ui);
        Button globe = button(ui, "World"), center = button(ui, "Selected"), retry = button(ui, "Retry");
        // The single Rotas tab owns GPX actions in both browsing and editing modes.
        Button gpx = route == null ? null : savedRoutes;
        Button[] row = new Button[] {globe, center, retry};
        for (Button b : row) actions.addView(b, new LinearLayout.LayoutParams(0, dp(activity, 48), 1));
        globe.setOnClickListener(v -> map.worldView());
        center.setOnClickListener(v -> map.centerSelection()); retry.setOnClickListener(v -> map.retry());
        layout.addView(actions);
        layout.addView(status);
        layout.addView(selected);
        layout.addView(copyRow);
        RouteUi routes = route == null ? null : new RouteUi(activity,ui,map,routeMode,hint,globe,center,retry,gpx,clearRoute,loop,route,walk != null);
        savedRoutes.setEnabled(routes!=null);
        places.routeUi=routes;
        saved.setOnClickListener(v->places.openLocations());
        if(routes!=null)routes.reopen=()->WorldMapDialog.show(activity,Control.engine.latitude,Control.engine.longitude,listener,walk,route);
        AlertDialog.Builder builder = new AlertDialog.Builder(ui).setTitle(dev.pgrazor.PtBr.tr("World map")).setView(layout)
            .setNegativeButton(dev.pgrazor.PtBr.tr("Cancel"), null).setPositiveButton(dev.pgrazor.PtBr.tr("Teleport"), null);
        if (walk != null) builder.setNeutralButton(dev.pgrazor.PtBr.tr("Walk here"), null);
        AlertDialog dialog = builder.create();
        map.poiLayer.setDestinationActions(walk == null ? null : (lat, lon) -> {
            if (!walk.onDestination(lat, lon)) return false;
            dialog.dismiss(); return true;
        }, (latitudePoi, longitudePoi) -> {
            if (cooldownDialog[0] != null && cooldownDialog[0].isShowing()) return false;
            cooldownDialog[0] = TeleportDialog.confirm(activity, latitudePoi, longitudePoi, (lat, lon) -> {
                if (!listener.onDestination(lat, lon)) return false;
                dialog.dismiss(); return true;
            });
            return false; // Keep details visible if the cooldown confirmation is cancelled.
        });
        DialogLifecycle.bind(activity, dialog, () -> { if(hotspotsDialog[0]!=null)hotspotsDialog[0].dismiss(); if(cooldownDialog[0]!=null)cooldownDialog[0].dismiss(); if(searchDialog[0]!=null)searchDialog[0].dismiss(); if(routes!=null)routes.close(); places.close(); map.close(); });
        dialog.setOnShowListener(d -> {
            MapTheme.fullScreen(dialog); MapTheme.style(layout); MapTheme.styleButtons(dialog);
            if(routes!=null)routes.attach(dialog);
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                if(routes!=null && routes.editing) { routes.start(); return; }
                if(cooldownDialog[0]!=null && cooldownDialog[0].isShowing())return;
                cooldownDialog[0]=TeleportDialog.confirm(activity,map.latitude,map.longitude,(lat,lon)->{
                    if(!listener.onDestination(lat,lon))return false;
                    dialog.dismiss();return true;
                });
            });
            if (walk != null) dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> {
                if (walk.onDestination(map.latitude, map.longitude)) dialog.dismiss();
            });
        });
        dialog.show();
        if(openLocations)saved.performClick();
        return dialog;
    }

    static double distanceMeters(double lat1,double lon1,double lat2,double lon2){double p=Math.toRadians(lat2-lat1),q=Math.toRadians(lon2-lon1);double a=Math.sin(p/2)*Math.sin(p/2)+Math.cos(Math.toRadians(lat1))*Math.cos(Math.toRadians(lat2))*Math.sin(q/2)*Math.sin(q/2);return 12742017.6*Math.asin(Math.sqrt(Math.max(0,Math.min(1,a))));}
    private static int dp(Context c, float value) { return Math.round(value * c.getResources().getDisplayMetrics().density); }
    private static TextView label(Context c, String text, int size) { TextView v = new TextView(c); v.setText(dev.pgrazor.PtBr.tr(text)); v.setTextSize(size); return v; }
    private static Button button(Context c, String text) { Button b = new Button(c); b.setText(dev.pgrazor.PtBr.tr(text)); b.setAllCaps(false); b.setTextSize(13); b.setPadding(0, 0, 0, 0); return b; }

    /** Route mode reuses the map toolbar and dialog action rather than consuming another row. */
    private static final class RouteUi {
        /** Manual builder cap; imported GPX routes may be larger, up to {@link #MAX}. */
        static final int LIMIT=50;
        static final int MAX=GpxCodec.MAX_POINTS;
        final Activity activity;
        final Context ui;
        final MapView map;
        final Button mode,add,undo,stops,gpx,clearRoute;
        final TextView hint;
        final CheckBox loop;
        final RouteListener listener;
        final boolean hasWalk;
        final List<double[]> draft=new ArrayList<>();
        final List<AlertDialog> dialogs=new ArrayList<>();
        AlertDialog parent;
        boolean editing;
        /** The current draft came from a GPX import/selection, so it may exceed the manual LIMIT. */
        boolean imported;
        String routeName="Route";
        Runnable reopen;
        boolean closed;
        RouteUi(Activity activity,Context ui,MapView map,Button mode,TextView hint,Button add,Button undo,Button stops,Button gpx,Button clearRoute,CheckBox loop,RouteListener listener,boolean hasWalk) {
            this.activity=activity;this.ui=ui;this.map=map;this.mode=mode;this.hint=hint;this.loop=loop;
            this.add=add;this.undo=undo;this.stops=stops;this.gpx=gpx;this.clearRoute=clearRoute;this.listener=listener;this.hasWalk=hasWalk;
            clearRoute.setOnClickListener(v -> clearDraft());
            loop.setOnCheckedChangeListener((v,on)->{map.setDraftLoop(on);refresh();});
            mode.setOnClickListener(v -> {editing=!editing;if(editing)maybeLoadPending();refresh();});
            add.setOnClickListener(v -> {if(editing)addStop();else map.worldView();});
            undo.setOnClickListener(v -> {if(editing)undo();else map.centerSelection();});
            stops.setOnClickListener(v -> {if(editing)showStops();else map.retry();});
            gpx.setOnClickListener(v -> {
                if(editing){GpxRouteDialog.menu(activity,this::sourceRoute,this::onGpxChosen);return;}
                AlertDialog menu=ChoiceList.attach(new AlertDialog.Builder(ui).setTitle("Rotas GPX").setNegativeButton("Fechar",null).create(),
                    new String[]{"Rotas salvas","Criar percurso","Importar GPX"},i->{
                        if(closed)return;
                        if(i==0)GpxRouteDialog.chooseSaved(activity,this::onGpxChosen);
                        else if(i==1){editing=true;maybeLoadPending();refresh();}
                        else GpxRouteDialog.importGpx(activity,"Mapa",this::onGpxChosen);
                    });
                dialogs.add(menu);menu.setOnDismissListener(d->dialogs.remove(menu));menu.show();
            });
        }
        void attach(AlertDialog parent) {this.parent=parent;if(pendingRoute!=null){editing=true;maybeLoadPending();}refresh();}
        void refresh() {
            if(draft.isEmpty())imported=false;
            boolean big=draft.size()>LIMIT;
            mode.setText("Voltar ao mapa");mode.setVisibility(editing?View.VISIBLE:View.GONE);
            mode.setContentDescription("Sair do editor sem iniciar o percurso");
            hint.setText(dev.pgrazor.PtBr.tr(!editing ? "Blue dot: you. Red pin: destination. Tap to select; drag to pan."
                : big ? "GPX route: "+draft.size()+" stops. Tap Start to walk it in order."
                : "Route: "+draft.size()+"/"+LIMIT+" stops. Tap map, then Add stop."));
            add.setText(dev.pgrazor.PtBr.tr(editing ? "Add stop" : "World")); add.setContentDescription(dev.pgrazor.PtBr.tr(editing ? "Add selected destination as next route stop" : "Show world map"));
            undo.setText(dev.pgrazor.PtBr.tr(editing ? "Undo" : "Selected")); undo.setContentDescription(dev.pgrazor.PtBr.tr(editing ? "Undo last route stop" : "Center selected destination"));
            stops.setText(dev.pgrazor.PtBr.tr(editing ? "Stops ("+draft.size()+")" : "Retry")); stops.setContentDescription(dev.pgrazor.PtBr.tr(editing ? "View ordered route stops and clear route" : "Retry map tiles"));
            gpx.setText("Rotas");
            clearRoute.setVisibility(editing ? View.VISIBLE : View.GONE);clearRoute.setEnabled(!draft.isEmpty());
            add.setEnabled(!editing || draft.size()<LIMIT);undo.setEnabled(!editing || !draft.isEmpty());
            loop.setVisibility(editing ? View.VISIBLE : View.GONE); // route-only toggle; defaults to stopping at the last stop
            if(editing&&!draft.isEmpty()){
                double meters=0;for(int i=1;i<draft.size();i++)meters+=distanceMeters(draft.get(i-1)[0],draft.get(i-1)[1],draft.get(i)[0],draft.get(i)[1]);
                if(loop.isChecked()&&draft.size()>1)meters+=distanceMeters(draft.get(draft.size()-1)[0],draft.get(draft.size()-1)[1],draft.get(0)[0],draft.get(0)[1]);
                hint.setText(hint.getText()+String.format(Locale.forLanguageTag("pt-BR")," • %.2f km",meters));
            }
            if(parent!=null) {
                parent.setTitle(dev.pgrazor.PtBr.tr(editing ? "Build route" : "World map"));
                Button action=parent.getButton(AlertDialog.BUTTON_POSITIVE);
                action.setText(dev.pgrazor.PtBr.tr(editing ? "Start route" : "Teleport"));
                action.setContentDescription(dev.pgrazor.PtBr.tr(editing ? "Start route in numbered stop order" : "Teleport to selected destination"));
                action.setEnabled(!editing || (draft.size()>=(imported?1:2) && draft.size()<=MAX));
                Button walk=parent.getButton(AlertDialog.BUTTON_NEUTRAL);
                if(walk!=null && hasWalk)walk.setVisibility(editing ? View.GONE : View.VISIBLE);
            }
            map.setRoute(draft);
        }
        void addStop() {
            if(draft.size()>=LIMIT)return;
            double lat=map.latitude,lon=map.longitude;
            if(!Double.isFinite(lat) || !Double.isFinite(lon) || Math.abs(lat)>90 || Math.abs(lon)>180) {
                Toast.makeText(activity,dev.pgrazor.PtBr.tr("Select valid coordinates for this stop."),Toast.LENGTH_SHORT).show();return;
            }
            draft.add(new double[]{lat,lon});refresh();
        }
        void undo() {if(!draft.isEmpty())draft.remove(draft.size()-1);refresh();}
        void clearDraft() {draft.clear();imported=false;routeName="Route";WorldMapDialog.pendingRoute=null;refresh();}
        void showStops() {
            AlertDialog.Builder builder=new AlertDialog.Builder(ui).setTitle(dev.pgrazor.PtBr.tr("Route stops ("+draft.size()+(draft.size()>LIMIT?"":"/"+LIMIT)+")"))
                .setNegativeButton(dev.pgrazor.PtBr.tr("Close"),null).setPositiveButton(dev.pgrazor.PtBr.tr("Clear route"),null)
                .setNeutralButton("Inverter percurso",null);
            if(draft.isEmpty())builder.setMessage(dev.pgrazor.PtBr.tr("Select a destination on the map and tap Add stop. Add at least two stops before starting."));
            AlertDialog dialog=builder.create();dialogs.add(dialog);
            if(!draft.isEmpty()){
                String[] rows=new String[draft.size()];
                for(int i=0;i<rows.length;i++)rows[i]=String.format(Locale.US,"Stop %d\n%.6f, %.6f",i+1,draft.get(i)[0],draft.get(i)[1]);
                ChoiceList.attach(dialog,rows,index -> {double[] stop=draft.get(index);map.select(stop[0],stop[1]);});
            }
            dialog.setOnDismissListener(d -> dialogs.remove(dialog));
            dialog.setOnShowListener(d -> {
                Button clear=dialog.getButton(AlertDialog.BUTTON_POSITIVE);clear.setEnabled(!draft.isEmpty());
                clear.setOnClickListener(v -> {clearDraft();dialog.dismiss();});
                Button reverse=dialog.getButton(AlertDialog.BUTTON_NEUTRAL);reverse.setEnabled(draft.size()>1);
                reverse.setOnClickListener(v->{Collections.reverse(draft);refresh();dialog.dismiss();});
            });
            dialog.show();
        }
        /** Load a chosen GPX route as the current draft; it stays an explicit Start, never auto-walks. */
        void onGpxChosen(GpxRoute route) {
            WorldMapDialog.pendingRoute=route; // survive the picker round-trip / dialog dismissal
            if(route==null)return;
            if(closed){if(reopen!=null)GpxRouteDialog.whenResumed(activity,reopen);return;}
            routeName=route.name;
            if(!editing)editing=true;
            apply(route.points());
            refresh();
            if(!draft.isEmpty())map.select(draft.get(0)[0],draft.get(0)[1]); // bring the route into view
        }
        /** Restore an imported/selected route when the builder is reopened, without clobbering a manual draft. */
        void maybeLoadPending() {
            GpxRoute pending=WorldMapDialog.pendingRoute;
            if(pending!=null && draft.isEmpty()){routeName=pending.name;apply(pending.points());if(!draft.isEmpty())map.select(draft.get(0)[0],draft.get(0)[1]);}
        }
        private void apply(double[][] points) {
            draft.clear();
            for(double[] p:points)draft.add(new double[]{p[0],p[1]});
            imported=true;
        }
        /** The current draft offered to the GPX menu for Save/Export; a placeholder name is renamed on Save. */
        GpxRoute sourceRoute() {
            if(draft.isEmpty())return null;
            try {GpxRoute current=new GpxRoute(routeName,toArray());WorldMapDialog.pendingRoute=current;return current;}
            catch(IllegalArgumentException invalid){return null;}
        }
        private double[][] toArray() {
            double[][] points=new double[draft.size()][2];
            for(int i=0;i<points.length;i++) {points[i][0]=draft.get(i)[0];points[i][1]=draft.get(i)[1];}
            return points;
        }
        void start() {
            if(draft.size()<(imported?1:2) || draft.size()>MAX)return;
            if(listener.onRoute(toArray(),loop.isChecked())){WorldMapDialog.pendingRoute=null;parent.dismiss();}
        }
        void close() {closed=true;for(AlertDialog dialog:new ArrayList<>(dialogs))dialog.dismiss();draft.clear();}
    }

    /** Neutral line icons: no emoji glyphs or dependence on a device font. */
    private static final class LibraryIcon extends View {
        final int type;final Paint pen=new Paint(Paint.ANTI_ALIAS_FLAG);
        LibraryIcon(Context context,int type,String description){super(context);this.type=type;setContentDescription(description);setClickable(true);setFocusable(true);}
        @Override protected void onDraw(Canvas c){super.onDraw(c);c.save();float scale=Math.min(getWidth(),getHeight())/32f;c.translate(getWidth()/2f-12*scale,getHeight()/2f-12*scale);c.scale(scale,scale);pen.setColor(MapTheme.ink());pen.setStyle(Paint.Style.STROKE);pen.setStrokeWidth(1.8f);pen.setStrokeCap(Paint.Cap.ROUND);pen.setStrokeJoin(Paint.Join.ROUND);
            android.graphics.Path p=new android.graphics.Path();
            if(type==0){p.moveTo(14,6);p.lineTo(7,12);p.lineTo(14,18);c.drawPath(p,pen);c.drawLine(7,12,20,12,pen);}
            else if(type==1){p.moveTo(4,4);p.lineTo(20,4);p.lineTo(14,12);p.lineTo(14,20);p.lineTo(10,18);p.lineTo(10,12);p.close();c.drawPath(p,pen);}
            else if(type==2){c.drawLine(7,4,7,20,pen);c.drawLine(3,8,7,4,pen);c.drawLine(11,8,7,4,pen);c.drawLine(17,4,17,20,pen);c.drawLine(13,16,17,20,pen);c.drawLine(21,16,17,20,pen);}
            else if(type==3){c.drawLine(5,12,19,12,pen);c.drawLine(12,5,12,19,pen);}
            else {c.drawRoundRect(4,8,15,21,2,2,pen);p.moveTo(8,5);p.lineTo(8,3);p.lineTo(20,3);p.lineTo(20,16);p.lineTo(18,16);c.drawPath(p,pen);}c.restore();}
    }
    /** Child dialogs share the map's activity; selecting a saved place never invokes Teleport. */
    private static final class PlaceUi {
        final Activity activity;
        final Context ui;
        final MapView map;
        final Button opener;
        final Handler main = new Handler(Looper.getMainLooper());
        final ThreadPoolExecutor storage = new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(8));
        final List<AlertDialog> dialogs = new ArrayList<>();
        SavedPlaces places;
        boolean closed,showFavorites,showPowers;
        int markerRevision;
        void setMarkerLayers(boolean favorites,boolean powers){showFavorites=favorites;showPowers=powers;refreshMarkerLayers();}
        void refreshMarkerLayers(){
            final int revision=++markerRevision;final boolean favorites=showFavorites,powers=showPowers;
            if(!favorites)map.setFavoriteMarkers(Collections.emptyList());if(!powers)map.setPowerMarkers(Collections.emptyList());
            if(closed||(!favorites&&!powers))return;
            try{storage.execute(()->{
                List<SavedPlaces.Entry> savedRows=Collections.emptyList();List<SourceEvent> powerRows=new ArrayList<>();long now=System.currentTimeMillis();
                try{if(favorites)savedRows=store().read().entries;
                    if(powers)for(SourceEvent event:SourceDataStore.events(activity,now).live(now))if(event.kind==SourceEvent.Kind.POWERSPOT&&event.isActionable(now))powerRows.add(event);
                }catch(RuntimeException unavailable){}
                final List<SavedPlaces.Entry> f=savedRows;
                main.post(()->{if(!closed&&revision==markerRevision){if(favorites)map.setFavoriteMarkers(f);if(powers)map.setPowerMarkers(powerRows);}});
            });}catch(java.util.concurrent.RejectedExecutionException busy){}
        }
        void openResourceCategories(){
            String[] choices={"Poképaradas / Ginásios / Reides / Rocket","Rotas oficiais carregadas","Pontos de Energia importados"};
            AlertDialog dialog=ChoiceList.attach(new AlertDialog.Builder(ui).setTitle("Radar de recursos").setNegativeButton("Fechar",null).create(),choices,i->{
                if(closed)return;if(i==0)map.poiLayer.openNearbyList();else if(i==1)openOfficialList();else if(i==2)openPowerList();
            });show(dialog);
        }
        void openOfficialList(){
            map.refreshOfficialRoutes();
            List<OfficialPreview> rows=new ArrayList<>(map.officialRoutes);
            if(Control.locationKnown())rows.sort(java.util.Comparator.comparingDouble(r->distanceMeters(Control.currentLatitude(),Control.currentLongitude(),r.points[0][0],r.points[0][1])));
            AlertDialog.Builder builder=new AlertDialog.Builder(ui).setTitle("Rotas oficiais carregadas ("+rows.size()+")").setNegativeButton("Fechar",null);
            if(rows.isEmpty()){show(builder.setMessage("Ative a camada Rotas oficiais e aguarde os dados carregados pelo jogo. Nenhuma disponibilidade é presumida.").create());return;}
            String[] names=new String[rows.size()];for(int i=0;i<rows.size();i++){OfficialPreview row=rows.get(i);names[i]=row.name+(Double.isFinite(row.meters)&&row.meters>0?String.format(Locale.forLanguageTag("pt-BR")," • %.0f m",row.meters):" • distância não informada");}
            show(ChoiceList.attach(builder.create(),names,i->{OfficialPreview row=rows.get(i);map.select(row.points[0][0],row.points[0][1]);}));
        }
        void openPowerList(){
            long now=System.currentTimeMillis();List<SourceEvent> rows=new ArrayList<>();for(SourceEvent e:map.powerMarkers)if(e.isActionable(now))rows.add(e);
            if(Control.locationKnown())rows.sort(java.util.Comparator.comparingDouble(e->e.distanceKm(Control.currentLatitude(),Control.currentLongitude())));
            AlertDialog.Builder builder=new AlertDialog.Builder(ui).setTitle("Pontos de Energia importados ("+rows.size()+")").setNegativeButton("Fechar",null);
            if(rows.isEmpty()){show(builder.setMessage("Ative a camada de Pontos de Energia importados. Só aparecem registros locais válidos; não há confirmação ao vivo.").create());return;}
            String[] labels=new String[rows.size()];for(int i=0;i<rows.size();i++){SourceEvent e=rows.get(i);String name=e.name.isEmpty()?"Ponto de Energia":e.name;labels[i]=name+"\n"+e.originKind.labelPtBr+" • disponibilidade não confirmada"+(Control.locationKnown()?String.format(Locale.forLanguageTag("pt-BR")," • %.2f km",e.distanceKm(Control.currentLatitude(),Control.currentLongitude())):"");}
            show(ChoiceList.attach(builder.create(),labels,i->{SourceEvent e=rows.get(i);if(e.isActionable(System.currentTimeMillis()))map.select(e.lat,e.lon);}));
        }
        PlaceUi(Activity activity, Context ui, MapView map, Button opener) { this.activity = activity; this.ui = ui; this.map = map; this.opener = opener; }
        private SavedPlaces store() { if (places == null) places = new SavedPlaces(activity); return places; }
        private void show(AlertDialog dialog) {
            if (closed) return;
            dialogs.add(dialog);
            dialog.setOnDismissListener(d -> dialogs.remove(dialog));
            dialog.show();
        }
        RouteUi routeUi;
        private int activeTab=1;
        private final int[] orders=new int[4];
        private final java.util.Set<String> savedFilter=new java.util.HashSet<>();
        private final java.util.EnumSet<Hotspots.Category> pointFilter=java.util.EnumSet.noneOf(Hotspots.Category.class);
        private AlertDialog libraryDialog;
        private final java.util.Map<String,Bitmap> categoryIcons=new java.util.HashMap<>();
        private void openLibrary(){activeTab=0;openLocations();}
        void openLocations(){
            if(closed||libraryDialog!=null&&libraryDialog.isShowing())return;
            opener.setEnabled(false);
            storage.execute(()->{
                SavedPlaces.Snapshot saved=null;List<GpxStore.Entry> routes=Collections.emptyList();String error="";
                try{saved=store().read();if(routeUi!=null)routes=new GpxStore(activity).list();}catch(IOException|RuntimeException e){error="Alguns dados não puderam ser lidos.";}
                final SavedPlaces.Snapshot snapshot=saved;final List<GpxStore.Entry> gpx=routes;final String warning=error;
                main.post(()->{if(!closed){opener.setEnabled(true);showLocations(snapshot,gpx,warning);}});
            });
        }
        private void showLocations(SavedPlaces.Snapshot snapshot,List<GpxStore.Entry> gpx,String warning){
            LinearLayout panel=new LinearLayout(ui);panel.setOrientation(LinearLayout.VERTICAL);panel.setPadding(dp(activity,12),dp(activity,4),dp(activity,12),dp(activity,4));panel.setBackgroundColor(MapTheme.surface());
            AlertDialog dialog=new AlertDialog.Builder(ui).setView(panel).create();libraryDialog=dialog;
            LinearLayout header=new LinearLayout(ui);header.setGravity(Gravity.CENTER_VERTICAL);
            LibraryIcon back=new LibraryIcon(ui,0,"Voltar"),filter=new LibraryIcon(ui,1,"Filtrar"),sort=new LibraryIcon(ui,2,"Ordenar por"),add=new LibraryIcon(ui,3,"Adicionar");
            header.addView(back,new LinearLayout.LayoutParams(dp(activity,40),dp(activity,48)));
            TextView title=label(ui,"Locais",20);title.setTypeface(null,android.graphics.Typeface.BOLD);title.setGravity(Gravity.CENTER);
            header.addView(title,new LinearLayout.LayoutParams(0,dp(activity,48),1));
            for(View v:new View[]{filter,sort,add})header.addView(v,new LinearLayout.LayoutParams(dp(activity,40),dp(activity,48)));
            panel.addView(header);back.setOnClickListener(v->dialog.dismiss());
            LinearLayout tabs=new LinearLayout(ui);String[] tabNames={"Favoritos","Pontos","Rotas","Eventos"};
            TextView[] tabLabels=new TextView[4];View[] indicators=new View[4];
            for(int i=0;i<4;i++){final int index=i;LinearLayout tab=new LinearLayout(ui);tab.setOrientation(LinearLayout.VERTICAL);
                TextView name=label(ui,tabNames[i],14);name.setGravity(Gravity.CENTER);tabLabels[i]=name;tab.addView(name,new LinearLayout.LayoutParams(-1,dp(activity,42)));
                View line=new View(ui);indicators[i]=line;tab.addView(line,new LinearLayout.LayoutParams(-1,dp(activity,2)));
                tabs.addView(tab,new LinearLayout.LayoutParams(0,-2,1));tab.setClickable(true);tab.setFocusable(true);tab.setContentDescription(tabNames[i]);
                if(index==2&&routeUi==null)tab.setVisibility(View.GONE);
            }
            panel.addView(tabs);
            EditText search=new EditText(ui);search.setSingleLine(true);search.setHint("Buscar");search.setTextSize(14);panel.addView(search);
            android.widget.ScrollView scroll=new android.widget.ScrollView(ui);LinearLayout rows=new LinearLayout(ui);rows.setOrientation(LinearLayout.VERTICAL);scroll.addView(rows);
            panel.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
            if(activeTab==2&&routeUi==null)activeTab=1;
            Runnable render=()->{
                if(closed)return;
                for(int i=0;i<4;i++){indicators[i].setBackgroundColor(activeTab==i?MapTheme.accent():Color.TRANSPARENT);tabLabels[i].setTextColor(activeTab==i?MapTheme.ink():MapTheme.muted());tabLabels[i].setTypeface(null,activeTab==i?android.graphics.Typeface.BOLD:android.graphics.Typeface.NORMAL);}
                filter.setVisibility(activeTab<2?View.VISIBLE:View.GONE);sort.setVisibility(activeTab<3?View.VISIBLE:View.GONE);add.setVisibility(activeTab<3?View.VISIBLE:View.GONE);
                search.setVisibility(activeTab<3?View.VISIBLE:View.GONE);rows.removeAllViews();
                String query=search.getText().toString().trim().toLowerCase(Locale.ROOT);
                if(!warning.isEmpty())rows.addView(label(ui,warning,13));
                long now=System.currentTimeMillis();boolean known=Control.locationKnown();double lat=Control.currentLatitude(),lon=Control.currentLongitude();
                if(activeTab==0){
                    List<SavedPlaces.Entry> list=new ArrayList<>();
                    if(snapshot!=null)for(SavedPlaces.Entry e:snapshot.entries)if((savedFilter.isEmpty()||savedFilter.contains(e.category))&&(e.name+" "+e.category).toLowerCase(Locale.ROOT).contains(query))list.add(e);
                    java.util.Comparator<SavedPlaces.Entry> byName=java.util.Comparator.comparing((SavedPlaces.Entry e)->e.name,String.CASE_INSENSITIVE_ORDER);
                    switch(orders[0]){case 1:list.sort(byName.reversed());break;case 2:list.sort(java.util.Comparator.comparingLong((SavedPlaces.Entry e)->e.createdAtMs).reversed());break;case 3:if(known)list.sort(java.util.Comparator.comparingDouble(e->distanceMeters(lat,lon,e.latitude,e.longitude)));break;case 4:if(known&&Cooldown.hasHistory())list.sort(java.util.Comparator.comparingLong(e->Cooldown.remainingAt(e.latitude,e.longitude)));break;default:list.sort(byName);}
                    LocalTimeClock clock=new LocalTimeClock();for(SavedPlaces.Entry e:list){
                        String local=clock.label(e.latitude,e.longitude,now,true,Locale.forLanguageTag("pt-BR"));
                        locationRow(rows,e.name,e.category,Collections.emptyList(),local,locationDistance(e.latitude,e.longitude),e.latitude,e.longitude,()->{dialog.dismiss();showPlace(e);});}
                    if(list.isEmpty())rows.addView(label(ui,"Nenhum local salvo neste filtro. Use + para salvar.",14));
                }else if(activeTab==1){
                    List<Hotspots.Spot> list=new ArrayList<>();for(Hotspots.Spot p:Hotspots.ALL)if((pointFilter.isEmpty()||!Collections.disjoint(pointFilter,p.categories))&&(p.name+" "+p.country+" "+p.city+" "+p.categoryText()).toLowerCase(Locale.ROOT).contains(query))list.add(p);
                    java.util.Comparator<Hotspots.Spot> byName=java.util.Comparator.comparing((Hotspots.Spot p)->p.country).thenComparing(p->p.name);
                    switch(orders[1]){case 1:list.sort(byName);break;case 2:list.sort(java.util.Comparator.comparingInt((Hotspots.Spot p)->p.zone.getOffset(now)).reversed().thenComparing(byName));break;case 3:list.sort(java.util.Comparator.comparingInt((Hotspots.Spot p)->p.zone.getOffset(now)).thenComparing(byName));break;case 4:if(known)list.sort(java.util.Comparator.comparingDouble(p->distanceMeters(lat,lon,p.latitude,p.longitude)));break;case 5:if(known&&Cooldown.hasHistory())list.sort(java.util.Comparator.comparingLong(p->Cooldown.remainingAt(p.latitude,p.longitude)));break;default:break;}
                    for(Hotspots.Spot p:list){List<String> icons=new ArrayList<>();for(Hotspots.Category c:p.categories)if(!c.icon.isEmpty()&&!icons.contains(c.icon))icons.add(c.icon);
                        locationRow(rows,p.country+" — "+p.name,p.categoryText(),icons,p.local(now,true),locationDistance(p.latitude,p.longitude),p.latitude,p.longitude,()->{dialog.dismiss();map.select(p.latitude,p.longitude);});}
                    if(list.isEmpty())rows.addView(label(ui,"Nenhum ponto corresponde aos filtros.",14));
                }else if(activeTab==2){
                    List<GpxStore.Entry> list=new ArrayList<>();for(GpxStore.Entry e:gpx)if(e.name.toLowerCase(Locale.ROOT).contains(query))list.add(e);
                    if(orders[2]==1)Collections.reverse(list);else if(orders[2]==2)list.sort(java.util.Comparator.comparingInt(e->e.count));
                    for(GpxStore.Entry e:list)simpleLibraryRow(rows,e.name,e.count+" pontos GPX",()->{dialog.dismiss();GpxRouteDialog.showEntry(activity,e,routeUi::onGpxChosen);});
                    if(list.isEmpty())rows.addView(label(ui,"Nenhuma rota salva. Use + para criar ou importar um GPX.",14));
                }else{
                    simpleLibraryRow(rows,"Missões e Pesquisas","Abrir painel de pesquisas",()->{dialog.dismiss();ResearchSettings.show(activity);});
                    simpleLibraryRow(rows,"Eventos","Abrir seletor de eventos",()->{dialog.dismiss();dev.pgrazor.events.EventPanel.showSelector(activity,Control.darkTheme());});
                }
            };
            for(int i=0;i<4;i++){final int index=i;tabs.getChildAt(i).setOnClickListener(v->{activeTab=index;search.setText("");render.run();scroll.scrollTo(0,0);});}
            filter.setOnClickListener(v->{
                if(activeTab==0){String[] values=SavedPlaces.CATEGORIES;boolean[] selected=new boolean[values.length];for(int i=0;i<values.length;i++)selected[i]=savedFilter.contains(values[i]);
                    show(new AlertDialog.Builder(ui).setTitle("Filtrar favoritos").setMultiChoiceItems(values,selected,(d,i,on)->selected[i]=on).setNeutralButton("Todos",(d,w)->{savedFilter.clear();render.run();}).setNegativeButton("Cancelar",null).setPositiveButton("Aplicar",(d,w)->{savedFilter.clear();for(int i=0;i<values.length;i++)if(selected[i])savedFilter.add(values[i]);render.run();}).create());
                }else if(activeTab==1){Hotspots.Category[] values=Hotspots.Category.values();String[] names=new String[values.length];boolean[] selected=new boolean[values.length];for(int i=0;i<values.length;i++){names[i]=values[i].label;selected[i]=pointFilter.contains(values[i]);}
                    show(new AlertDialog.Builder(ui).setTitle("Filtrar pontos").setMultiChoiceItems(names,selected,(d,i,on)->selected[i]=on).setNeutralButton("Todos",(d,w)->{pointFilter.clear();render.run();}).setNegativeButton("Cancelar",null).setPositiveButton("Aplicar",(d,w)->{pointFilter.clear();for(int i=0;i<values.length;i++)if(selected[i])pointFilter.add(values[i]);render.run();}).create());}
            });
            sort.setOnClickListener(v->{final int tab=activeTab;String[] options=tab==0?new String[]{"Nome A–Z","Nome Z–A","Mais recentes","Distância","Cooldown estimado"}:tab==1?new String[]{"Ordem do catálogo","País e nome","Fuso: mais adiantado","Fuso: mais atrasado","Distância","Cooldown estimado"}:new String[]{"Nome A–Z","Nome Z–A","Quantidade de pontos"};
                show(new AlertDialog.Builder(ui).setTitle("Ordenar por").setSingleChoiceItems(options,orders[tab],(d,i)->{orders[tab]=i;d.dismiss();render.run();}).setNegativeButton("Cancelar",null).create());});
            add.setOnClickListener(v->{if(activeTab==2&&routeUi!=null){dialog.dismiss();routeUi.gpx.performClick();}else showSave(dialog);});
            search.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int count,int after){}public void onTextChanged(CharSequence s,int st,int before,int count){render.run();}public void afterTextChanged(android.text.Editable e){}});
            render.run();dialog.setOnShowListener(d->{MapTheme.fullScreen(dialog);render.run();});show(dialog);
            Runnable[] tick=new Runnable[1];tick[0]=()->{if(!closed&&dialog.isShowing()){render.run();panel.postDelayed(tick[0],30000);}};panel.postDelayed(tick[0],30000);
        }
        private String locationDistance(double lat,double lon){
            if(!Control.locationKnown())return "Distância e cooldown indisponíveis";
            String line=String.format(Locale.forLanguageTag("pt-BR"),"Distância: %.1f km",distanceMeters(Control.currentLatitude(),Control.currentLongitude(),lat,lon)/1000);
            return line+(Cooldown.hasHistory()?" · Cooldown est.: "+((Cooldown.remainingAt(lat,lon)+59999)/60000)+" min":" · Cooldown sem histórico");
        }
        private void simpleLibraryRow(LinearLayout rows,String title,String detail,Runnable action){
            LinearLayout row=new LinearLayout(ui);row.setOrientation(LinearLayout.VERTICAL);row.setPadding(dp(activity,4),dp(activity,14),dp(activity,4),dp(activity,14));
            TextView heading=label(ui,title,17);heading.setTypeface(null,android.graphics.Typeface.BOLD);heading.setTextColor(MapTheme.ink());row.addView(heading);
            TextView sub=label(ui,detail,14);sub.setTextColor(MapTheme.muted());row.addView(sub);row.setClickable(true);row.setFocusable(true);row.setOnClickListener(v->action.run());rows.addView(row);libraryDivider(rows);
        }
        private void locationRow(LinearLayout rows,String name,String categories,List<String> icons,String local,String distance,double lat,double lon,Runnable action){
            LinearLayout card=new LinearLayout(ui);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(dp(activity,4),dp(activity,12),dp(activity,4),dp(activity,12));
            LinearLayout top=new LinearLayout(ui);top.setGravity(Gravity.CENTER_VERTICAL);TextView heading=label(ui,name,17);heading.setTextColor(MapTheme.ink());heading.setTypeface(null,android.graphics.Typeface.BOLD);top.addView(heading,new LinearLayout.LayoutParams(0,-2,1));
            LibraryIcon copy=new LibraryIcon(ui,4,"Copiar coordenadas de "+name);copy.setOnClickListener(v->CoordinateClipboard.copy(activity,lat,lon,"Coordenadas copiadas"));top.addView(copy,new LinearLayout.LayoutParams(dp(activity,40),dp(activity,40)));card.addView(top);
            LinearLayout tags=new LinearLayout(ui);tags.setGravity(Gravity.CENTER_VERTICAL);
            for(String key:icons){if(!categoryIcons.containsKey(key))try(java.io.InputStream in=PgAssets.open(ui,"pgrazor/map-icons/"+key)){categoryIcons.put(key,android.graphics.BitmapFactory.decodeStream(in));}catch(Exception ex){categoryIcons.put(key,null);}
                Bitmap bitmap=categoryIcons.get(key);if(bitmap!=null){android.widget.ImageView image=new android.widget.ImageView(ui);image.setImageBitmap(bitmap);image.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);tags.addView(image,new LinearLayout.LayoutParams(dp(activity,20),dp(activity,20)));}}
            TextView cat=label(ui,categories,13);cat.setTextColor(MapTheme.muted());tags.addView(cat,new LinearLayout.LayoutParams(0,-2,1));card.addView(tags);
            for(String line:new String[]{local,distance,CoordinateClipboard.text(lat,lon)}){TextView text=label(ui,line,14);text.setTextColor(MapTheme.muted());text.setPadding(0,dp(activity,4),0,0);card.addView(text);}
            card.setClickable(true);card.setFocusable(true);card.setOnClickListener(v->action.run());rows.addView(card);libraryDivider(rows);
        }
        private void libraryDivider(LinearLayout rows){View line=new View(ui);line.setBackgroundColor(MapTheme.dark()?0xff394552:0xffd4dce3);rows.addView(line,new LinearLayout.LayoutParams(-1,dp(activity,1)));}
        private void showSave(AlertDialog library) {
            final double latitude = map.latitude, longitude = map.longitude;
            LinearLayout form = new LinearLayout(ui); form.setOrientation(LinearLayout.VERTICAL);
            int pad = dp(activity, 20); form.setPadding(pad, 0, pad, 0);
            form.addView(label(ui, String.format(Locale.US, "%.6f, %.6f", latitude, longitude), 14));
            EditText name = new EditText(ui); name.setHint(dev.pgrazor.PtBr.tr("Place name (up to " + SavedPlaces.NAME_LIMIT + " characters)")); name.setSingleLine(true);
            name.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
            name.setFilters(new InputFilter[] {new InputFilter.LengthFilter(SavedPlaces.NAME_LIMIT)});
            form.addView(name);
            android.widget.Spinner category=new android.widget.Spinner(ui);
            android.widget.ArrayAdapter<String> adapter=new android.widget.ArrayAdapter<>(ui,android.R.layout.simple_spinner_item,SavedPlaces.CATEGORIES);adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);category.setAdapter(adapter);form.addView(category);
            AlertDialog dialog = new AlertDialog.Builder(ui).setTitle(dev.pgrazor.PtBr.tr("Save selected place")).setView(form)
                .setNegativeButton(dev.pgrazor.PtBr.tr("Cancel"), null).setPositiveButton(dev.pgrazor.PtBr.tr("Save"), null).create();
            dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                final SavedPlaces.Entry entry;
                try { entry = new SavedPlaces.Entry(name.getText().toString(), latitude, longitude,(String)category.getSelectedItem()); }
                catch (IllegalArgumentException e) { name.setError(e.getMessage()); return; }
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                storage.execute(() -> {
                    try {
                        store().add(entry);
                        main.post(() -> {
                            if (!closed && dialog.isShowing()) {
                                dialog.dismiss(); if(library!=null)library.dismiss(); Toast.makeText(activity, "Saved " + entry.name, Toast.LENGTH_SHORT).show(); refreshMarkerLayers();openLibrary();
                            }
                        });
                    } catch (IOException | RuntimeException e) {
                        main.post(() -> { if (!closed && dialog.isShowing()) { dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true); name.setError(e.getMessage()); } });
                    }
                });
            }));
            show(dialog);
        }
        private void showPlace(SavedPlaces.Entry entry) {
            AlertDialog dialog = new AlertDialog.Builder(ui).setTitle(dev.pgrazor.PtBr.tr(entry.name))
                .setMessage(dev.pgrazor.PtBr.tr(String.format(Locale.US, "%.6f, %.6f\n\nSelect shows this destination on the map. Choose Teleport or Walk here there to apply it.", entry.latitude, entry.longitude)))
                .setNegativeButton(dev.pgrazor.PtBr.tr("Cancel"), null)
                .setPositiveButton(dev.pgrazor.PtBr.tr("Select on map"), (d, which) -> map.select(entry.latitude, entry.longitude))
                .setNeutralButton(dev.pgrazor.PtBr.tr("Delete"), null).create();
            dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> confirmDelete(entry, dialog)));
            show(dialog);
        }
        private void confirmDelete(SavedPlaces.Entry entry, AlertDialog details) {
            AlertDialog confirm = new AlertDialog.Builder(ui).setTitle(dev.pgrazor.PtBr.tr("Delete saved place?"))
                .setMessage(dev.pgrazor.PtBr.tr(entry.name + " will be removed from your saved places. The current location stays unchanged."))
                .setNegativeButton(dev.pgrazor.PtBr.tr("Cancel"), null).setPositiveButton(dev.pgrazor.PtBr.tr("Delete"), null).create();
            confirm.setOnShowListener(d -> confirm.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                confirm.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                storage.execute(() -> {
                    try {
                        store().remove(entry.id);
                        main.post(() -> { if (!closed && confirm.isShowing()) { confirm.dismiss(); details.dismiss(); refreshMarkerLayers();openLibrary(); } });
                    } catch (IOException | RuntimeException e) {
                        main.post(() -> {
                            if (!closed && confirm.isShowing()) {
                                confirm.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                                Toast.makeText(activity, dev.pgrazor.PtBr.tr("Could not delete place. Please retry."), Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                });
            }));
            show(confirm);
        }
        void close() {
            closed = true;
            for (AlertDialog dialog : new ArrayList<>(dialogs)) dialog.dismiss();
            main.removeCallbacksAndMessages(null);
            storage.shutdown(); // Finish any explicitly requested persistence write; callbacks ignore the closed UI.
        }
    }

    /** Give the map the space left after controls, rather than guessing from physical screen orientation. */
    private static final class ResponsiveLayout extends LinearLayout {
        View map;
        final int maximumMapHeight;
        ResponsiveLayout(Activity activity) { super(activity); maximumMapHeight = dp(activity, 420); }
        protected void onMeasure(int widthSpec, int heightSpec) {
            if (map != null) {
                int reserved = getPaddingTop() + getPaddingBottom();
                for (int i = 0; i < getChildCount(); i++) {
                    View child = getChildAt(i);
                    if (child == map || child.getVisibility() == View.GONE) continue;
                    measureChildWithMargins(child, widthSpec, 0, MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED), 0);
                    LinearLayout.LayoutParams params = (LinearLayout.LayoutParams)child.getLayoutParams();
                    reserved += child.getMeasuredHeight() + params.topMargin + params.bottomMargin;
                }
                // Fill the space left by the controls so the actions stay visible; never a fixed height that pushes them off screen.
                int mode = MeasureSpec.getMode(heightSpec);
                int available;
                if (mode == MeasureSpec.UNSPECIFIED) available = maximumMapHeight;
                else {
                    available = Math.max(0, MeasureSpec.getSize(heightSpec) - reserved);

                }
                map.getLayoutParams().height = available;
            }
            super.onMeasure(widthSpec, heightSpec);
        }
    }

    /** Read-only, bounded official route preview. No movement or completion command is created here. */
    private static final class OfficialPreview {
        final String name;final double meters;final double[][] points;
        OfficialPreview(String name,double meters,double[][] points){this.name=name;this.meters=meters;this.points=points;}
    }
    private static List<OfficialPreview> officialPreviews(String snapshot){
        List<OfficialPreview> out=new ArrayList<>();
        if(snapshot==null||snapshot.isEmpty()||snapshot.length()>700000)return out;
        try{JSONArray routes=new JSONObject(snapshot).optJSONArray("routes");if(routes==null)return out;
            for(int i=0;i<Math.min(100,routes.length());i++){
                JSONObject r=routes.optJSONObject(i);if(r==null)continue;JSONArray points=r.optJSONArray("points");if(points==null||points.length()<2||points.length()>MotionEngine.MAX_ROUTE_STOPS)continue;
                int size=Math.min(100,points.length());double[][] sampled=new double[size][2];boolean valid=true;
                for(int j=0;j<points.length();j++){JSONArray point=points.optJSONArray(j);if(point==null||point.length()<2){valid=false;break;}double lat=point.optDouble(0,Double.NaN),lon=point.optDouble(1,Double.NaN);if(!Double.isFinite(lat)||!Double.isFinite(lon)||Math.abs(lat)>90||Math.abs(lon)>180){valid=false;break;}}
                if(!valid)continue;
                for(int j=0;j<size;j++){JSONArray point=points.optJSONArray((int)Math.round((double)j*(points.length()-1)/(size-1)));sampled[j][0]=point.optDouble(0);sampled[j][1]=point.optDouble(1);}
                out.add(new OfficialPreview(r.optString("name",r.optString("id","Rota oficial")),r.optDouble("distanceMeters",Double.NaN),sampled));
            }
        }catch(org.json.JSONException malformed){}
        return out;
    }
    private static final class MapView extends View {
        private final TextView selectionLabel;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        private final int tileSize, slop;
        private final MapTileSource tiles;
        private final MapPoiLayer poiLayer;
        private List<SavedPlaces.Entry> favoriteMarkers=Collections.emptyList();
        private List<SourceEvent> powerMarkers=Collections.emptyList();
        private List<OfficialPreview> officialRoutes=Collections.emptyList();
        private boolean officialLayer;private String lastOfficialSnapshot="";
        void setFavoriteMarkers(List<SavedPlaces.Entry> rows){favoriteMarkers=new ArrayList<>(rows);invalidate();}
        void setPowerMarkers(List<SourceEvent> rows){powerMarkers=new ArrayList<>(rows);invalidate();}
        void setOfficialLayer(boolean enabled){if(officialLayer==enabled)return;officialLayer=enabled;Control.setMapRoutesOpen(enabled);lastOfficialSnapshot="";officialRoutes=Collections.emptyList();refreshOfficialRoutes();invalidate();}
        void refreshOfficialRoutes(){String raw=officialLayer?Control.gameRoutesMapSnapshot():"";if(!raw.equals(lastOfficialSnapshot)){lastOfficialSnapshot=raw;officialRoutes=officialPreviews(raw);invalidate();}}
        private String selectedResource="";
        private List<double[]> route = Collections.emptyList();
        private List<double[]> activeTrace = Collections.emptyList();
        private MotionEngine.RouteVisualSnapshot traceState;
        private long traceToken;
        private boolean traceEnabled, draftLoop;
        void setDraftLoop(boolean loop){draftLoop=loop;invalidate();}
        private TextView traceSummary;
        private final LocalTimeClock destinationClock=new LocalTimeClock();
        private LinearLayout walkControls;private Button pauseWalk,stopWalk;private long displayedWalkToken;
        void setActiveWalkControls(LinearLayout controls,Button pause,Button stop){walkControls=controls;pauseWalk=pause;stopWalk=stop;refreshWalkControls();}
        private void refreshWalkControls(){if(walkControls==null)return;long token=Control.engine.activeVisualRouteToken();MotionEngine.RouteVisualSnapshot state=token==0?null:Control.engine.routeVisualSnapshot(token);displayedWalkToken=state==null?0:state.token;walkControls.setVisibility(state==null||Control.gameRoutesRunning()?View.GONE:View.VISIBLE);if(state!=null)pauseWalk.setText(state.paused?"Retomar":"Pausar");}
        private String traceLabel="";
        private final Paint tracePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private RouteTraceStyle traceStyle=RouteTraceStyle.defaults();

        void setTraceStyle(RouteTraceStyle style) {
            traceStyle=style;refreshTrace();invalidate();
        }

        void setTraceEnabled(boolean enabled,TextView summary) {
            traceEnabled=enabled;traceSummary=summary;
            refreshTrace();invalidate();
        }
        private void refreshTrace() {
            MotionEngine.RouteVisualSnapshot next=traceEnabled ?
                Control.engine.routeVisualSnapshot(traceToken) : null;
            if(next==null) {
                traceState=null;traceToken=0;activeTrace=Collections.emptyList();
            } else {
                if(next.newPoints!=null) activeTrace=java.util.Arrays.asList(next.newPoints);
                traceToken=next.token;traceState=next;
            }
            if(traceSummary==null)return;
            String label="";
            if(traceEnabled) {
                if(!route.isEmpty()) label=traceStyle.showPreview?"Prévia: caminho planejado (sem progresso real).":"Prévia: estilo original.";
                else if(traceState!=null) label=String.format(Locale.forLanguageTag("pt-BR"),
                    "%s • %d/%d pontos • %.0f%% • %.2f/%.2f km • %.2f km restantes%s",
                    traceState.paused?"Pausado":traceState.returning?"Retorno ao início":traceState.visited==0?"Aproximação ao início":"Em andamento",
                    traceState.visited,traceState.total,
                    traceState.routeMeters>0?100*traceState.completedMeters/traceState.routeMeters:0,
                    traceState.completedMeters/1000,traceState.routeMeters/1000,traceState.remainingMeters/1000,
                    traceState.looping?" • circular":"");
                else label="Nenhum percurso ativo.";
                if(route.isEmpty()&&traceState!=null&&Control.engine.speedKmh>0&&!traceState.paused){long sec=(long)Math.ceil(traceState.remainingMeters/(Control.engine.speedKmh/3.6));label+=String.format(Locale.US," • ETA %d:%02d",sec/60,sec%60);}
            }
            if(!label.equals(traceLabel)){traceSummary.setText(label);traceLabel=label;}
            int visibility=traceEnabled&&traceStyle.showSummary?View.VISIBLE:View.GONE;
            if(traceSummary.getVisibility()!=visibility)traceSummary.setVisibility(visibility);
        }

        private volatile boolean closed;
        private double centerX, centerY, latitude, longitude;
        private int zoom = 14;
        private double zoomScale=1;
        private final ScaleGestureDetector pinch;
        private float downX, downY, previousX, previousY;
        private boolean dragged, multiTouch;
        private final Runnable locationRefresh = new Runnable() {
            public void run() {
                if(closed) return;
                refreshTrace();updateSelection();refreshWalkControls();refreshOfficialRoutes();
                invalidate(); // Refresh the live player marker without requesting new tiles.
                postDelayed(this, 250);
            }
        };

        MapView(Activity activity, double latitude, double longitude, TextView selection, TextView status) {
            super(activity);
            this.latitude = latitude; this.longitude = longitude;
            centerX = MapProjection.x(longitude); centerY = MapProjection.y(latitude);
            selectionLabel = selection;
            tileSize = dp(activity, 256); slop = ViewConfiguration.get(activity).getScaledTouchSlop();
            tiles = new MapTileSource(new MapTileSource.Host() {
                public int zoom() { return zoom; }
                public void onTilesChanged() { invalidate(); }
            }, status, new File(activity.getCacheDir(), "pgrazor-map-tiles-v1"));
            poiLayer = new MapPoiLayer(activity, MapTheme.wrap(activity), new MapPoiLayer.Projector() {
                public float screenX(double lon) { return projectX(lon); }
                public float screenY(double lat) { return projectY(lat); }
                public int width() { return getWidth(); }
                public int height() { return getHeight(); }
            }, new MapPoiLayer.Snapshots() {
                public void setOpen(boolean open) { Control.setMapPoiOpen(open); }
                public String read() { return Control.mapPoiSnapshot(); }
            }, this::invalidate);
            setContentDescription(dev.pgrazor.PtBr.tr("World map. Blue dot is your current location; red pin is the destination. Drag to pan, pinch to zoom, tap to select destination."));
            // Use the activity hardware renderer; tile I/O stays on the worker.
            pinch=new ScaleGestureDetector(activity,new ScaleGestureDetector.SimpleOnScaleGestureListener(){
                public boolean onScaleBegin(ScaleGestureDetector detector){multiTouch=true;return true;}
                public boolean onScale(ScaleGestureDetector detector){
                    double old=world(),minimum=Math.max(1,Math.min(getWidth(),getHeight()));
                    double next=Math.max(minimum,Math.min(tileSize*Math.pow(2,19),old*detector.getScaleFactor()));
                    double dx=detector.getFocusX()-getWidth()/2.0,dy=detector.getFocusY()-getHeight()/2.0;
                    double anchorX=MapProjection.offsetX(centerX,dx,old),anchorY=MapProjection.offsetY(centerY,dy,old);
                    zoom=Math.max(0,Math.min(19,(int)Math.floor(Math.log(next/tileSize)/Math.log(2))));
                    zoomScale=next/((zoom==0?minimum:tileSize)*(1<<zoom));
                    centerX=MapProjection.offsetX(anchorX,-dx,next);centerY=MapProjection.offsetY(anchorY,-dy,next);
                    viewportChanged();return true;
                }
            });
            updateSelection();
        }

        void setPoiLayer(int bit, boolean on) { poiLayer.setLayer(bit, on); }
        void setPoiStatus(TextView view) { poiLayer.setStatusView(view); }
        void refreshPois() { poiLayer.refresh(); }

        private void updateSelection() {
            String info=selectedResource.isEmpty()?"Local selecionado":selectedResource;
            if(Control.locationKnown())info+=String.format(Locale.forLanguageTag("pt-BR"),"\nDistância: %.0f m",distanceMeters(Control.currentLatitude(),Control.currentLongitude(),latitude,longitude));
            if(Control.locationKnown()&&Cooldown.hasHistory()){long seconds=(Cooldown.remainingAt(latitude,longitude)+999)/1000;info+=String.format(Locale.US," • Cooldown est.: %d:%02d:%02d",seconds/3600,seconds/60%60,seconds%60);}else info+=" • Cooldown: sem estimativa";
            info+="\nHorário local: "+destinationClock.label(latitude,longitude,System.currentTimeMillis(),true,Locale.forLanguageTag("pt-BR"));
            info+="\n"+CoordinateClipboard.text(latitude,longitude);
            if(!info.contentEquals(selectionLabel.getText()))selectionLabel.setText(info);
        }
        private double displayedTileSize() { return (zoom == 0 ? Math.max(1, Math.min(getWidth(), getHeight())) : tileSize)*zoomScale; }
        private double world() { return MapProjection.worldPixels(zoom, displayedTileSize()); }
        void zoom(int amount) { int next = Math.max(0, Math.min(19, zoom + amount)); if (next != zoom) { zoom = next; viewportChanged(); } }
        void worldView() { zoomScale=1; zoom = 0; centerX = .5; centerY = .5; viewportChanged(); }
        void centerCurrentLocation() {
            double lat=Control.currentLatitude(),lon=Control.currentLongitude();
            if(!Control.locationKnown()||!Double.isFinite(lat)||!Double.isFinite(lon)||Math.abs(lat)>90||Math.abs(lon)>180)return;
            zoomScale=1;if(zoom==0)zoom=14;
            centerX=MapProjection.x(lon);centerY=MapProjection.y(lat);viewportChanged();
        }
        void centerSelection() { zoomScale=1; if (zoom == 0) zoom = 14; centerX = MapProjection.x(longitude); centerY = MapProjection.y(latitude); viewportChanged(); }
        void select(double latitude, double longitude) {
            this.latitude = latitude; this.longitude = longitude;selectedResource="";
            zoomScale=1; zoom = 14; centerX = MapProjection.x(longitude); centerY = MapProjection.y(latitude);
            updateSelection(); viewportChanged();
        }
        void retry() { tiles.clearFailures(); viewportChanged(); }
        void setRoute(List<double[]> points) {
            List<double[]> copy=new ArrayList<>(points.size());
            for(double[] point:points)copy.add(new double[]{point[0],point[1]});
            route=copy;refreshTrace();invalidate();
        }
        protected void onSizeChanged(int w, int h, int oldw, int oldh) { viewportChanged(); }

        private void viewportChanged() {
            if (closed || getWidth() <= 0 || getHeight() <= 0) return;
            if (zoom == 0 && zoomScale<=1) { centerX = .5; centerY = .5; }
            int count = 1 << zoom;
            double pixels = displayedTileSize();
            double originX = centerX * world() - getWidth() / 2.0;
            double originY = centerY * world() - getHeight() / 2.0;
            int left = zoom == 0 ? 0 : (int)Math.floor(originX / pixels), top = (int)Math.floor(originY / pixels);
            int right = zoom == 0 ? 0 : (int)Math.floor((originX + getWidth() - 1) / pixels), bottom = (int)Math.floor((originY + getHeight() - 1) / pixels);
            List<MapTileSource.Tile> placed = new ArrayList<>();
            for (int y = Math.max(0, top); y <= Math.min(count - 1, bottom); y++) {
                for (int x = left; x <= right && placed.size() < 32; x++) {
                    placed.add(new MapTileSource.Tile(zoom, Math.floorMod(x, count), y, (float)(x * pixels - originX), (float)(y * pixels - originY)));
                }
            }
            tiles.setViewport(placed);
            invalidate();
        }

        protected void onDraw(Canvas canvas) {
            canvas.drawColor(Color.rgb(225, 231, 234));
            paint.setStyle(Paint.Style.FILL);
            for (MapTileSource.Tile tile : tiles.tiles()) {
                Bitmap bitmap = tiles.bitmap(tile.key);
                float pixels = (float)displayedTileSize();
                RectF bounds = new RectF(tile.left, tile.top, tile.left + pixels, tile.top + pixels);
                if (bitmap != null) canvas.drawBitmap(bitmap, null, bounds, paint);
                else if(!drawParent(canvas,tile,bounds)) {
                    paint.setColor(Color.rgb(202, 211, 216)); paint.setStyle(Paint.Style.STROKE); canvas.drawRect(bounds, paint); paint.setStyle(Paint.Style.FILL);
                }
            }
            drawOfficialRoutes(canvas);
            poiLayer.draw(canvas);
            drawSupplementalMarkers(canvas);
            drawRoute(canvas,false);
            float markerX = (float)(getWidth() / 2.0 + MapProjection.deltaX(MapProjection.x(longitude), centerX) * world());
            float markerY = (float)(getHeight() / 2.0 + (MapProjection.y(latitude) - centerY) * world());
            float radius = Math.max(8, tileSize / 28f);
            paint.setColor(Color.WHITE); canvas.drawCircle(markerX, markerY, radius + 3, paint);
            paint.setColor(Color.rgb(197, 35, 52)); canvas.drawCircle(markerX, markerY, radius, paint);
            paint.setColor(Color.WHITE); canvas.drawCircle(markerX, markerY, radius / 3, paint);
            drawRoute(canvas,true);
            drawCurrentLocation(canvas);
        }

        private void drawOfficialRoutes(Canvas canvas){
            if(!officialLayer||officialRoutes.isEmpty())return;float w=(float)world();paint.setColor(0xff4b718d);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(dp(getContext(),3));
            paint.setPathEffect(new android.graphics.DashPathEffect(new float[]{dp(getContext(),7),dp(getContext(),5)},0));
            for(OfficialPreview route:officialRoutes){for(int i=1;i<route.points.length;i++){double[] a=route.points[i-1],b=route.points[i];float x=projectX(a[1]),y=projectY(a[0]);float x2=x+(float)(MapProjection.deltaX(MapProjection.x(b[1]),MapProjection.x(a[1]))*w),y2=projectY(b[0]);canvas.drawLine(x,y,x2,y2,paint);if(x2<0||x2>getWidth()){canvas.drawLine(x-w,y,x2-w,y2,paint);canvas.drawLine(x+w,y,x2+w,y2,paint);}}}
            paint.setPathEffect(null);paint.setStyle(Paint.Style.FILL);
        }
        private void drawSupplementalMarkers(Canvas canvas){
            paint.setStyle(Paint.Style.FILL);paint.setTextAlign(Paint.Align.CENTER);paint.setTextSize(dp(getContext(),11));long now=System.currentTimeMillis();
            for(SavedPlaces.Entry e:favoriteMarkers)drawResourceMarker(canvas,e.latitude,e.longitude,"★",0xffa66b00);
            for(SourceEvent e:powerMarkers)if(e.isActionable(now))drawResourceMarker(canvas,e.lat,e.lon,"D",0xff8140b1);
        }
        private void drawResourceMarker(Canvas canvas,double lat,double lon,String glyph,int color){
            float x=projectX(lon),y=projectY(lat),r=dp(getContext(),10);if(x<-r||x>getWidth()+r||y<-r||y>getHeight()+r)return;
            paint.setColor(Color.WHITE);canvas.drawCircle(x,y,r+dp(getContext(),2),paint);paint.setColor(color);canvas.drawCircle(x,y,r,paint);paint.setColor(Color.WHITE);canvas.drawText(glyph,x,y+dp(getContext(),4),paint);
        }
        private boolean pickResource(float x,float y){
            double best=dp(getContext(),22);String name="";double lat=0,lon=0;long now=System.currentTimeMillis();
            for(SavedPlaces.Entry e:favoriteMarkers){double d=Math.hypot(x-projectX(e.longitude),y-projectY(e.latitude));if(d<best){best=d;name="Favorito • "+e.name;lat=e.latitude;lon=e.longitude;}}
            for(SourceEvent e:powerMarkers){if(!e.isActionable(now))continue;double d=Math.hypot(x-projectX(e.lon),y-projectY(e.lat));if(d<best){best=d;name="Ponto de Energia importado • "+(e.name.isEmpty()?"sem nome":e.name);lat=e.lat;lon=e.lon;}}
            if(name.isEmpty())return false;latitude=lat;longitude=lon;selectedResource=name;updateSelection();invalidate();return true;
        }
        /** Independent of the destination: selecting or searching never changes this position. */
        private void drawCurrentLocation(Canvas canvas) {
            double lat=Control.currentLatitude(),lon=Control.currentLongitude();
            if(!Control.locationKnown()||!Double.isFinite(lat)||!Double.isFinite(lon)||Math.abs(lat)>90||Math.abs(lon)>180)return;
            float x=projectX(lon),y=projectY(lat),radius=dp(getContext(),5);
            if(x < -radius*3 || x > getWidth()+radius*3 || y < -radius*3 || y > getHeight()+radius*3)return;
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(0x403493fa);canvas.drawCircle(x,y,radius*2.5f,paint);
            paint.setColor(Color.WHITE);canvas.drawCircle(x,y,radius+dp(getContext(),2),paint);
            paint.setColor(0xff1674e8);canvas.drawCircle(x,y,radius,paint);
        }

        protected void onAttachedToWindow() {
            super.onAttachedToWindow();
            if(!closed){removeCallbacks(locationRefresh);post(locationRefresh);}
        }

        private boolean drawParent(Canvas canvas,MapTileSource.Tile tile,RectF bounds){
            for(int z=tile.zoom-1;z>=Math.max(0,tile.zoom-5);z--){int factor=1<<(tile.zoom-z);Bitmap parent=tiles.bitmap(z+"-"+(tile.x/factor)+"-"+(tile.y/factor));if(parent==null)continue;
                int size=256/factor,x=(tile.x%factor)*size,y=(tile.y%factor)*size;
                canvas.drawBitmap(parent,new Rect(x,y,x+size,y+size),bounds,paint);return true;
            }return false;
        }
        private float projectX(double lon){return (float)(getWidth()/2.0+MapProjection.deltaX(MapProjection.x(lon),centerX)*world());}
        private float projectY(double lat){return (float)(getHeight()/2.0+(MapProjection.y(lat)-centerY)*world());}

        /** Each segment takes the shortest horizontal wrap, splitting at the dateline when needed. */
        private void drawRoute(Canvas canvas,boolean markers) {
            List<double[]> shown=route.isEmpty()&&traceEnabled?activeTrace:route;
            boolean active=traceEnabled&&route.isEmpty()&&traceState!=null;
            boolean styled=traceEnabled&&(active||traceStyle.showPreview);
            if(shown.isEmpty() || getWidth()<=0 || getHeight()<=0)return;
            double pixels=world();float radius=Math.max(11,tileSize/20f);
            int saved=canvas.save();
            if(zoom==0)canvas.clipRect((float)((getWidth()-pixels)/2),(float)((getHeight()-pixels)/2),(float)((getWidth()+pixels)/2),(float)((getHeight()+pixels)/2));
            paint.setStyle(Paint.Style.FILL);paint.setStrokeWidth(Math.max(2,tileSize/90f));
            boolean closes=styled&&shown.size()>1&&(active?traceState.looping:draftLoop);
            int count=shown.size()+(!markers&&closes?1:0);
            for(int i=0;i<count;i++) {
                double[] point=shown.get(i%shown.size());
                double pointX=MapProjection.x(point[1]);
                double x=getWidth()/2.0+MapProjection.deltaX(pointX,centerX)*pixels;
                double y=getHeight()/2.0+(MapProjection.y(point[0])-centerY)*pixels;
                if(!markers && i>0) {
                    double[] previous=shown.get(i-1);
                    double previousX=MapProjection.x(previous[1]);
                    double start=getWidth()/2.0+MapProjection.deltaX(previousX,centerX)*pixels;
                    double end=start+MapProjection.deltaX(pointX,previousX)*pixels;
                    double startY=getHeight()/2.0+(MapProjection.y(previous[0])-centerY)*pixels;
                    int first=(int)Math.ceil(-Math.max(start,end)/pixels),last=(int)Math.floor((getWidth()-Math.min(start,end))/pixels);
                    if(styled && (Math.max(y,startY)<-radius || Math.min(y,startY)>getHeight()+radius)) continue;
                    paint.setColor(Color.rgb(25,103,190));
                    for(int repeat=first;repeat<=last && repeat<first+32;repeat++) {
                        float sx=(float)(start+repeat*pixels),ex=(float)(end+repeat*pixels);
                        if(!styled)canvas.drawLine(sx,(float)startY,ex,(float)y,paint);
                        else {
                            double fraction=0;
                            if(active&&traceStyle.showProgress) {
                                if(i==shown.size()) fraction=traceState.returning?traceState.legFraction:0;
                                else fraction=traceState.returning||i<traceState.visited?1:i==traceState.visited?traceState.legFraction:0;
                            }
                            drawTraceSegment(canvas,sx,(float)startY,ex,(float)y,fraction);
                        }
                    }
                }
                if(markers && (!styled || (traceStyle.showMarkers && (i==0 || i==shown.size()-1 || shown.size()<=50)))) {
                    int first=(int)Math.ceil((-radius-x)/pixels),last=(int)Math.floor((getWidth()+radius-x)/pixels);
                    for(int repeat=first;repeat<=last && repeat<first+32;repeat++) {
                        float shownX=(float)(x+repeat*pixels),shownY=(float)y;
                        if(shownY < -radius || shownY > getHeight()+radius)continue;
                        paint.setColor(Color.WHITE);canvas.drawCircle(shownX,shownY,radius+2,paint);
                        paint.setColor(styled && i==shown.size()-1 && i!=0 ? 0xffe2574f : Color.rgb(25,103,190));canvas.drawCircle(shownX,shownY,radius,paint);
                        paint.setTextSize(radius*1.1f);paint.setTextAlign(Paint.Align.CENTER);paint.setColor(Color.WHITE);
                        Paint.FontMetrics font=paint.getFontMetrics();canvas.drawText(Integer.toString(i+1),shownX,shownY-(font.ascent+font.descent)/2,paint);
                    }
                }
            }
            canvas.restoreToCount(saved);
        }

        /** Geographic wrapping is handled by drawRoute; this only paints the local segment. */
        private void drawTraceSegment(Canvas canvas,float x1,float y1,float x2,float y2,double fraction) {
            float width=dp(getContext(),traceStyle.width);
            tracePaint.setStrokeCap(Paint.Cap.ROUND);
            tracePaint.setStrokeWidth(width+dp(getContext(),2));
            tracePaint.setColor(traceStyle.color(0xff394653));
            canvas.drawLine(x1,y1,x2,y2,tracePaint);
            tracePaint.setStrokeWidth(width);
            tracePaint.setColor(traceStyle.color(traceStyle.remaining));
            canvas.drawLine(x1,y1,x2,y2,tracePaint);
            if(fraction>0) {
                tracePaint.setColor(traceStyle.color(traceStyle.completed));
                canvas.drawLine(x1,y1,(float)(x1+(x2-x1)*fraction),(float)(y1+(y2-y1)*fraction),tracePaint);
            }
        }

        public boolean onTouchEvent(MotionEvent event) {
            pinch.onTouchEvent(event);
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    getParent().requestDisallowInterceptTouchEvent(true);
                    downX = previousX = event.getX(); downY = previousY = event.getY(); dragged = false; multiTouch = false; return true;
                case MotionEvent.ACTION_POINTER_DOWN: multiTouch = true; return true;
                case MotionEvent.ACTION_MOVE:
                    if (multiTouch) return true;
                    float x = event.getX(), y = event.getY();
                    if (Math.hypot(x - downX, y - downY) > slop) dragged = true;
                    if (dragged && (zoom != 0 || zoomScale>1)) {
                        centerX = MapProjection.offsetX(centerX, previousX - x, world());
                        centerY = MapProjection.offsetY(centerY, previousY - y, world());
                        viewportChanged();
                    }
                    previousX = x; previousY = y; return true;
                case MotionEvent.ACTION_UP:
                    boolean insideWorld = zoom != 0 || (Math.abs(event.getX() - getWidth() / 2.0) <= world() / 2 && Math.abs(event.getY() - getHeight() / 2.0) <= world() / 2);
                    if (!dragged && !multiTouch) {
                        // Browsing marker details never moves the destination; movement needs an explicit action.
                        List<JSONObject> hits = poiLayer.hasLayers() ? poiLayer.hits(event.getX(), event.getY()) : Collections.<JSONObject>emptyList();
                        if (!hits.isEmpty()) { performClick(); poiLayer.openDetails(hits); getParent().requestDisallowInterceptTouchEvent(false); return true; }
                        if(insideWorld&&pickResource(event.getX(),event.getY())){performClick();getParent().requestDisallowInterceptTouchEvent(false);return true;}
                        if (insideWorld) {
                            selectedResource="";longitude = MapProjection.longitude(MapProjection.offsetX(centerX, event.getX() - getWidth() / 2.0, world()));
                            latitude = MapProjection.latitude(MapProjection.offsetY(centerY, event.getY() - getHeight() / 2.0, world()));
                            updateSelection(); invalidate(); performClick();
                        }
                    }
                    getParent().requestDisallowInterceptTouchEvent(false); return true;
                case MotionEvent.ACTION_CANCEL: getParent().requestDisallowInterceptTouchEvent(false); return true;
                default: return true;
            }
        }
        public boolean performClick() { super.performClick(); return true; }

        void close() {
            removeCallbacks(locationRefresh);
            if (closed) return;
            closed = true;
            traceState=null;traceToken=0;activeTrace=Collections.emptyList();traceSummary=null;
            if(officialLayer)Control.setMapRoutesOpen(false);officialLayer=false;officialRoutes=Collections.emptyList();favoriteMarkers=Collections.emptyList();powerMarkers=Collections.emptyList();
            poiLayer.close();
            tiles.close();
        }
        protected void onDetachedFromWindow() { close(); super.onDetachedFromWindow(); }
    }
}
