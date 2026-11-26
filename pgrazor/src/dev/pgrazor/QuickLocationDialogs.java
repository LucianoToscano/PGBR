package dev.pgrazor;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.text.InputType;
import android.view.ContextThemeWrapper;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import java.util.Locale;

/**
 * Quick-teleport modals for the {@link QuickMenu}. Both delegate the actual move to a
 * {@link TeleportTarget} (the host validates readiness and calls {@link Control#teleport}, or
 * {@link Control#walkTo} for the coordinate modal's Walk button),
 * and reuse the shared {@link SavedPlaces} store. Persistence reads run off the UI thread.
 */
public final class QuickLocationDialogs {
    private QuickLocationDialogs() {}

    /** Applies a destination; returns false when it was rejected so the modal can stay open. */
    public interface TeleportTarget { boolean teleport(double latitude, double longitude); }

    /** A simple latitude/longitude modal (no speed field): Teleport there, or Walk there at the current speed. */
    public static void showTeleport(Activity activity, TeleportTarget target, TeleportTarget walk) {
        if (activity.isFinishing() || activity.isDestroyed() || !activity.hasWindowFocus()) return;
        Context ctx = themed(activity);
        LinearLayout form = new LinearLayout(ctx); form.setOrientation(LinearLayout.VERTICAL);
        int p = dp(ctx, 22); form.setPadding(p, dp(ctx, 8), p, dp(ctx, 8));
        // Where you are now, with a button to copy it.
        final double hereLat = Control.engine.latitude, hereLon = Control.engine.longitude;
        LinearLayout here = new LinearLayout(ctx); here.setGravity(android.view.Gravity.CENTER_VERTICAL);
        android.widget.TextView hereText = new android.widget.TextView(ctx);
        hereText.setText(String.format(java.util.Locale.ROOT,dev.pgrazor.PtBr.tr("You are at\n%s"),CoordinateClipboard.text(hereLat, hereLon))); hereText.setTextSize(14);
        here.addView(hereText, new LinearLayout.LayoutParams(0, -2, 1));
        android.widget.Button copy = new android.widget.Button(ctx); copy.setText(dev.pgrazor.PtBr.tr("Copy")); copy.setAllCaps(false); copy.setTextSize(13);
        copy.setContentDescription(dev.pgrazor.PtBr.tr("Copy your current coordinates"));
        MapTheme.setButtonBackground(copy);
        copy.setOnClickListener(v -> CoordinateClipboard.copy(activity, hereLat, hereLon, dev.pgrazor.PtBr.tr("Your location copied")));
        here.addView(copy, new LinearLayout.LayoutParams(dp(ctx, 76), dp(ctx, 48)));
        android.widget.Button favorite = new android.widget.Button(ctx); favorite.setText(dev.pgrazor.PtBr.tr("Favorite")); favorite.setAllCaps(false); favorite.setTextSize(13);
        favorite.setContentDescription(dev.pgrazor.PtBr.tr("Save your current location to Favorites"));
        MapTheme.setButtonBackground(favorite);
        favorite.setOnClickListener(v -> saveFavorite(activity, hereLat, hereLon));
        LinearLayout.LayoutParams favoriteLp = new LinearLayout.LayoutParams(dp(ctx, 92), dp(ctx, 48)); favoriteLp.leftMargin = dp(ctx, 6);
        here.addView(favorite, favoriteLp);
        LinearLayout.LayoutParams hereLp = new LinearLayout.LayoutParams(-1, -2); hereLp.bottomMargin = dp(ctx, 8);
        form.addView(here, hereLp);
        EditText coordinates = input(ctx, "Latitude, longitude");
        String pasted = CoordinateClipboard.coordinatesOnClipboard(activity);
        if (!pasted.isEmpty()) {
            coordinates.setText(pasted);
            coordinates.setSelection(pasted.length());
            coordinates.setContentDescription(dev.pgrazor.PtBr.tr("Coordenadas válidas preenchidas da área de transferência"));
        }
        form.addView(coordinates);
        AlertDialog dialog = new AlertDialog.Builder(ctx).setTitle(dev.pgrazor.PtBr.tr("Go to coordinates")).setView(form)
            .setNegativeButton(dev.pgrazor.PtBr.tr("Cancel"), null).setNeutralButton(dev.pgrazor.PtBr.tr("Walk"), null).setPositiveButton(dev.pgrazor.PtBr.tr("Teleport"), null).create();
        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                double[] point;
                try { point=CoordinateInput.parse(coordinates.getText().toString()); }
                catch (IllegalArgumentException e) { coordinates.setError(e.getMessage()); return; }
                TeleportDialog.confirm(activity,point[0],point[1],(lat,lon)->{
                    if(!target.teleport(lat,lon))return false;
                    dialog.dismiss();return true;
                });
            });
            // Walking needs no cooldown check: it starts from where you are at the current speed.
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> {
                double[] point;
                try { point=CoordinateInput.parse(coordinates.getText().toString()); }
                catch (IllegalArgumentException e) { coordinates.setError(e.getMessage()); return; }
                if(walk.teleport(point[0],point[1])){
                    dialog.dismiss();
                    Toast.makeText(activity,"Caminhando até "+CoordinateClipboard.text(point[0],point[1]),Toast.LENGTH_SHORT).show();
                }
            });
        });
        show(activity,dialog);
    }

    private static volatile int favoritesCategoryFilter=0, favoritesSort=0;
    // Menu order is the contract: a stored index must keep meaning the same ordering.
    private static final SavedPlaceOrder.Sort[] SORTS={
        SavedPlaceOrder.Sort.NAME_ASC,SavedPlaceOrder.Sort.NAME_DESC,
        SavedPlaceOrder.Sort.LOCAL_TIME_EARLY,SavedPlaceOrder.Sort.LOCAL_TIME_LATE,
        SavedPlaceOrder.Sort.NEWEST,SavedPlaceOrder.Sort.OLDEST,
        SavedPlaceOrder.Sort.DISTANCE,SavedPlaceOrder.Sort.COOLDOWN};
    private static final String[] SORT_NAMES={"Nome: A–Z","Nome: Z–A",
        "Horário local: mais cedo primeiro","Horário local: mais tarde primeiro",
        "Adicionados recentemente","Adicionados há mais tempo",
        "Distância: mais perto primeiro","Cooldown: menor primeiro"};

    /** Saved places, with a separate category filter and stable sort modes. */
    public static void showFavorites(Activity activity,TeleportTarget target) {
        if(activity.isFinishing()||activity.isDestroyed()||!activity.hasWindowFocus())return;
        new Thread(()->{
            SavedPlaces.Snapshot snapshot;
            try {snapshot=new SavedPlaces(activity).read();}
            catch(RuntimeException failure){snapshot=null;}
            final SavedPlaces.Snapshot snap=snapshot;
            activity.runOnUiThread(()->{
                if(activity.isFinishing()||activity.isDestroyed())return;
                if(snap==null){
                    show(activity,new AlertDialog.Builder(themed(activity))
                        .setTitle(dev.pgrazor.PtBr.tr("Locais favoritos")).setMessage("Não foi possível ler os locais.")
                        .setPositiveButton("Fechar",null).create());
                    return;
                }
                showFavoriteList(activity,target,snap);
                if(snap.damaged)Toast.makeText(activity,
                    dev.pgrazor.PtBr.tr("Alguns locais não puderam ser lidos; os válidos foram preservados."),
                    Toast.LENGTH_LONG).show();
            });
        },"pgrazor-favorites-read").start();
    }

    private static Integer localUtcOffset(double latitude,double longitude,long now){
        try {
            String zone=TimeZoneLookup.zoneId(latitude,longitude);
            return java.util.TimeZone.getTimeZone(zone).getOffset(now);
        }catch(RuntimeException invalid){return null;}
    }
    private static void showFavoriteList(Activity activity,TeleportTarget target,SavedPlaces.Snapshot snap){
        final long now=System.currentTimeMillis();
        // One shared policy with the World Map Locais library: same filter, same order, same ties.
        final java.util.List<SavedPlaces.Entry> filtered=SavedPlaceOrder.select(snap.entries,
            new SavedPlaceOrder.Query("",favoritesCategoryFilter==0?null:
                java.util.Collections.singletonList(SavedPlaces.CATEGORIES[favoritesCategoryFilter-1]),
                SORTS[Math.max(0,Math.min(SORTS.length-1,favoritesSort))]),
            (lat,lon)->localUtcOffset(lat,lon,now),
            (lat,lon)->Control.locationKnown()?
                WorldMapDialog.distanceMeters(Control.currentLatitude(),Control.currentLongitude(),lat,lon):Double.NaN,
            (lat,lon)->Cooldown.hasHistory()?Cooldown.remainingAt(lat,lon):-1);
        final java.util.List<SavedPlaces.Entry> sorted=new java.util.ArrayList<>(filtered);
        String selected=favoritesCategoryFilter==0?dev.pgrazor.PtBr.tr("Todas"):dev.pgrazor.PtBr.tr(SavedPlaces.CATEGORIES[favoritesCategoryFilter-1]);
        AlertDialog.Builder builder=new AlertDialog.Builder(themed(activity))
            .setTitle(dev.pgrazor.PtBr.tr("Locais")+" • "+dev.pgrazor.PtBr.tr(selected))
            .setNegativeButton("Fechar",null)
            .setNeutralButton("Filtrar / ordenar",(d,w)->showFavoriteOptions(activity,target,snap));
        if(sorted.isEmpty()){
            builder.setMessage(dev.pgrazor.PtBr.tr("No saved places in this category. Change the filter or save a favorite."));
            show(activity,builder.create());
            return;
        }
        String[] names=new String[sorted.size()];
        for(int i=0;i<sorted.size();i++){
            SavedPlaces.Entry place=sorted.get(i);
            Integer utc=localUtcOffset(place.latitude,place.longitude,now);
            String zone=utc==null?dev.pgrazor.PtBr.tr("fuso indisponível"):String.format(Locale.US,"UTC%s%02d:%02d",
                utc>=0?"+":"-",Math.abs(utc/60000)/60,Math.abs(utc/60000)%60);
            names[i]=String.format(Locale.US,"%s  •  %s\n%s  |  %.5f, %.5f",
                place.name,place.category,zone,place.latitude,place.longitude);
        }
        show(activity,ChoiceList.attach(builder.create(),names,which->
            showPlaceActions(activity,target,sorted.get(which))));
    }

    private static void showPlaceActions(Activity activity,TeleportTarget target,SavedPlaces.Entry place){
        if(activity.isFinishing()||activity.isDestroyed())return;
        show(activity,new AlertDialog.Builder(themed(activity))
            .setTitle(place.name)
            .setItems(new String[]{"Copiar coordenadas","Renomear","Excluir","Teleporte"},(d,which)->{
                switch(which){
                    case 0:CoordinateClipboard.copy(activity,place.latitude,place.longitude,"Coordenadas copiadas");break;
                    case 1:renamePlace(activity,target,place);break;
                    case 2:confirmRemovePlace(activity,target,place);break;
                    case 3:TeleportDialog.confirm(activity,place.latitude,place.longitude,target::teleport);break;
                    default:break;
                }
            }).setNegativeButton("Voltar",(d,w)->showFavorites(activity,target)).create());
    }

    private static void renamePlace(Activity activity,TeleportTarget target,SavedPlaces.Entry place){
        EditText name=input(themed(activity),"Nome do local");
        name.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(SavedPlaces.NAME_LIMIT)});
        name.setText(place.name);
        AlertDialog dialog=new AlertDialog.Builder(themed(activity)).setTitle("Renomear local")
            .setView(name).setNegativeButton("Cancelar",null).setPositiveButton("Salvar",null).create();
        dialog.setOnShowListener(d->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
            String requested=name.getText().toString();
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
            new Thread(()->{
                String error=null;
                try{
                    if(!new SavedPlaces(activity).rename(place.id,requested))error=dev.pgrazor.PtBr.tr("Local não encontrado.");
                }catch(java.io.IOException|RuntimeException e){error=e.getMessage()==null?dev.pgrazor.PtBr.tr("Não foi possível renomear."):e.getMessage();}
                final String failed=error;
                activity.runOnUiThread(()->{
                    if(activity.isFinishing()||activity.isDestroyed()||!dialog.isShowing())return;
                    if(failed!=null){name.setError(dev.pgrazor.PtBr.tr(failed));dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);return;}
                    dialog.dismiss();showFavorites(activity,target);
                });
            },"pgbr-place-rename").start();
        }));
        show(activity,dialog);
    }

    private static void confirmRemovePlace(Activity activity,TeleportTarget target,SavedPlaces.Entry place){
        AlertDialog dialog=new AlertDialog.Builder(themed(activity)).setTitle("Excluir local?")
            .setMessage(place.name).setNegativeButton("Cancelar",null).setPositiveButton("Excluir",null).create();
        dialog.setOnShowListener(d->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
            new Thread(()->{
                String error=null;
                try{
                    if(!new SavedPlaces(activity).remove(place.id))error=dev.pgrazor.PtBr.tr("Local não encontrado.");
                }catch(java.io.IOException|RuntimeException e){error=dev.pgrazor.PtBr.tr("Não foi possível excluir o local.");}
                final String failed=error;
                activity.runOnUiThread(()->{
                    if(activity.isFinishing()||activity.isDestroyed()||!dialog.isShowing())return;
                    if(failed!=null){Toast.makeText(activity,failed,Toast.LENGTH_LONG).show();dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);return;}
                    dialog.dismiss();showFavorites(activity,target);
                });
            },"pgbr-place-remove").start();
        }));
        show(activity,dialog);
    }

    private static void showFavoriteOptions(Activity activity,TeleportTarget target,SavedPlaces.Snapshot snap){
        String[] choices=new String[SavedPlaces.CATEGORIES.length+1+SORT_NAMES.length];
        choices[0]=(favoritesCategoryFilter==0?"✓  ":"")+dev.pgrazor.PtBr.tr("All categories");
        for(int i=0;i<SavedPlaces.CATEGORIES.length;i++)
            choices[i+1]=(favoritesCategoryFilter==i+1?"✓  ":"")+SavedPlaces.CATEGORIES[i];
        for(int i=0;i<SORT_NAMES.length;i++)
            choices[SavedPlaces.CATEGORIES.length+1+i]=(favoritesSort==i?"✓  ":"")+dev.pgrazor.PtBr.tr(SORT_NAMES[i]);
        show(activity,new AlertDialog.Builder(themed(activity))
            .setTitle(dev.pgrazor.PtBr.tr("Categories and place ordering"))
            .setItems(choices,(dialog,choice)->{
                if(choice<=SavedPlaces.CATEGORIES.length)favoritesCategoryFilter=choice;
                else favoritesSort=choice-SavedPlaces.CATEGORIES.length-1;
                showFavoriteList(activity,target,snap);
            })
            .setNegativeButton("Cancelar",null).create());
    }

    private static void show(Activity activity,AlertDialog dialog){
        DialogLifecycle.bind(activity,dialog,null);dialog.show();
    }

    /** Names and saves a location as a favorite (the World map's saved places). */
    private static void saveFavorite(Activity activity, double latitude, double longitude) {
        if (activity.isFinishing() || activity.isDestroyed()) return;
        Context ctx = themed(activity);
        LinearLayout form = new LinearLayout(ctx); form.setOrientation(LinearLayout.VERTICAL);
        int p = dp(ctx, 22); form.setPadding(p, dp(ctx, 8), p, dp(ctx, 8));
        android.widget.TextView where = new android.widget.TextView(ctx); where.setText(dev.pgrazor.PtBr.tr(CoordinateClipboard.text(latitude, longitude))); where.setTextSize(14);
        form.addView(where);
        EditText name = input(ctx, String.format(java.util.Locale.ROOT, dev.pgrazor.PtBr.tr("Place name (up to %d characters)"), SavedPlaces.NAME_LIMIT));
        name.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        name.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(SavedPlaces.NAME_LIMIT)});
        form.addView(name);
        android.widget.TextView categoryLabel=new android.widget.TextView(ctx);
        categoryLabel.setText(dev.pgrazor.PtBr.tr("Categoria"));categoryLabel.setTextSize(14);form.addView(categoryLabel);
        android.widget.Spinner category=new android.widget.Spinner(ctx);
        android.widget.ArrayAdapter<String> categoryAdapter=new android.widget.ArrayAdapter<>(
            ctx,android.R.layout.simple_spinner_dropdown_item,SavedPlaces.CATEGORIES);
        category.setAdapter(categoryAdapter);form.addView(category);
        AlertDialog dialog = new AlertDialog.Builder(ctx).setTitle(dev.pgrazor.PtBr.tr("Add to Favorites")).setView(form)
            .setNegativeButton(dev.pgrazor.PtBr.tr("Cancel"), null).setPositiveButton(dev.pgrazor.PtBr.tr("Save"), null).create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            final SavedPlaces.Entry entry;
            try { entry = new SavedPlaces.Entry(name.getText().toString(), latitude, longitude,
                SavedPlaces.CATEGORIES[category.getSelectedItemPosition()]); }
            catch (IllegalArgumentException e) { name.setError(dev.pgrazor.PtBr.tr(e.getMessage())); return; }
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
            new Thread(() -> {
                String error = null;
                try { new SavedPlaces(activity).add(entry); }
                catch (java.io.IOException | RuntimeException e) { error = e.getMessage() == null ? dev.pgrazor.PtBr.tr("Could not save this place.") : e.getMessage(); }
                final String failed = error;
                activity.runOnUiThread(() -> {
                    if (!dialog.isShowing()) return;
                    if (failed != null) { dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true); name.setError(dev.pgrazor.PtBr.tr(failed)); return; }
                    dialog.dismiss();
                    Toast.makeText(activity, "Salvo em Favoritos: " + entry.name, Toast.LENGTH_SHORT).show();
                });
            }, "pgrazor-favorites-save").start();
        }));
        show(activity, dialog);
    }

    private static EditText input(Context ctx, String hint) {
        EditText e = new EditText(ctx); e.setSingleLine(true); e.setHint(dev.pgrazor.PtBr.tr(hint));
        e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        return e;
    }
    private static Context themed(Activity a) {
        return new ContextThemeWrapper(a, Control.darkTheme() ? android.R.style.Theme_Material_Dialog_Alert : android.R.style.Theme_Material_Light_Dialog_Alert);
    }
    private static int dp(Context c, float x) { return Math.round(x * c.getResources().getDisplayMetrics().density); }
}
