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
        hereText.setText(dev.pgrazor.PtBr.tr("You are at\n" + CoordinateClipboard.text(hereLat, hereLon))); hereText.setTextSize(14);
        here.addView(hereText, new LinearLayout.LayoutParams(0, -2, 1));
        android.widget.Button copy = new android.widget.Button(ctx); copy.setText(dev.pgrazor.PtBr.tr("Copy")); copy.setAllCaps(false); copy.setTextSize(13);
        copy.setContentDescription(dev.pgrazor.PtBr.tr("Copy your current coordinates"));
        MapTheme.setButtonBackground(copy);
        copy.setOnClickListener(v -> CoordinateClipboard.copy(activity, hereLat, hereLon, "Your location copied"));
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
            coordinates.setContentDescription("Coordenadas válidas preenchidas da área de transferência");
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
                    Toast.makeText(activity,"Walking to "+CoordinateClipboard.text(point[0],point[1]),Toast.LENGTH_SHORT).show();
                }
            });
        });
        show(activity,dialog);
    }

    private static volatile int favoritesCategoryFilter=0, favoritesSort=0;
    private static final String[] SORT_NAMES={"Nome: A–Z","Nome: Z–A",
        "Horário local: mais cedo primeiro","Horário local: mais tarde primeiro",
        "Adicionados recentemente","Adicionados há mais tempo"};

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
                        .setTitle("Locais favoritos").setMessage("Não foi possível ler os locais.")
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

    private static int localUtcOffset(SavedPlaces.Entry place,long now){
        try {
            String zone=TimeZoneLookup.zoneId(place.latitude,place.longitude);
            return java.util.TimeZone.getTimeZone(zone).getOffset(now);
        }catch(RuntimeException invalid){return 0;}
    }
    private static void showFavoriteList(Activity activity,TeleportTarget target,SavedPlaces.Snapshot snap){
        java.util.ArrayList<SavedPlaces.Entry> sorted=new java.util.ArrayList<>();
        for(SavedPlaces.Entry place:snap.entries)
            if(favoritesCategoryFilter==0||
                place.category.equals(SavedPlaces.CATEGORIES[favoritesCategoryFilter-1]))sorted.add(place);
        final long now=System.currentTimeMillis();
        java.util.Comparator<SavedPlaces.Entry> byName=(a,b)->a.name.compareToIgnoreCase(b.name);
        switch(favoritesSort){
            case 1:sorted.sort(byName.reversed());break;
            case 2:sorted.sort(java.util.Comparator.comparingInt((SavedPlaces.Entry e)->localUtcOffset(e,now)).thenComparing(byName));break;
            case 3:sorted.sort(java.util.Comparator.comparingInt((SavedPlaces.Entry e)->localUtcOffset(e,now)).reversed().thenComparing(byName));break;
            case 4:sorted.sort(java.util.Comparator.comparingLong((SavedPlaces.Entry e)->e.createdAtMs).reversed().thenComparing(byName));break;
            case 5:sorted.sort(java.util.Comparator.comparingLong((SavedPlaces.Entry e)->e.createdAtMs).thenComparing(byName));break;
            default:sorted.sort(byName);
        }
        String selected=favoritesCategoryFilter==0?"Todas":SavedPlaces.CATEGORIES[favoritesCategoryFilter-1];
        AlertDialog.Builder builder=new AlertDialog.Builder(themed(activity))
            .setTitle("Locais • "+selected)
            .setNegativeButton("Fechar",null)
            .setNeutralButton("Filtrar / ordenar",(d,w)->showFavoriteOptions(activity,target,snap));
        if(sorted.isEmpty()){
            builder.setMessage("Nenhum local nesta categoria. Mude o filtro ou salve um favorito.");
            show(activity,builder.create());
            return;
        }
        String[] names=new String[sorted.size()];
        for(int i=0;i<sorted.size();i++){
            SavedPlaces.Entry place=sorted.get(i);
            int offset=localUtcOffset(place,now)/60000;
            String zone=String.format(Locale.US,"UTC%s%02d:%02d",
                offset>=0?"+":"-",Math.abs(offset)/60,Math.abs(offset)%60);
            names[i]=String.format(Locale.US,"%s  •  %s\n%s  |  %.5f, %.5f",
                place.name,place.category,zone,place.latitude,place.longitude);
        }
        show(activity,ChoiceList.attach(builder.create(),names,which->
            TeleportDialog.confirm(activity,sorted.get(which).latitude,
                sorted.get(which).longitude,target::teleport)));
    }

    private static void showFavoriteOptions(Activity activity,TeleportTarget target,SavedPlaces.Snapshot snap){
        String[] choices=new String[SavedPlaces.CATEGORIES.length+1+SORT_NAMES.length];
        choices[0]=(favoritesCategoryFilter==0?"✓  ":"")+"Todas as categorias";
        for(int i=0;i<SavedPlaces.CATEGORIES.length;i++)
            choices[i+1]=(favoritesCategoryFilter==i+1?"✓  ":"")+SavedPlaces.CATEGORIES[i];
        for(int i=0;i<SORT_NAMES.length;i++)
            choices[SavedPlaces.CATEGORIES.length+1+i]=(favoritesSort==i?"✓  ":"")+SORT_NAMES[i];
        show(activity,new AlertDialog.Builder(themed(activity))
            .setTitle("Categorias e ordem de locais")
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
        EditText name = input(ctx, "Place name (up to " + SavedPlaces.NAME_LIMIT + " characters)");
        name.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        name.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(SavedPlaces.NAME_LIMIT)});
        form.addView(name);
        android.widget.TextView categoryLabel=new android.widget.TextView(ctx);
        categoryLabel.setText("Categoria");categoryLabel.setTextSize(14);form.addView(categoryLabel);
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
            catch (IllegalArgumentException e) { name.setError(e.getMessage()); return; }
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
            new Thread(() -> {
                String error = null;
                try { new SavedPlaces(activity).add(entry); }
                catch (java.io.IOException | RuntimeException e) { error = e.getMessage() == null ? "Could not save this place." : e.getMessage(); }
                final String failed = error;
                activity.runOnUiThread(() -> {
                    if (!dialog.isShowing()) return;
                    if (failed != null) { dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true); name.setError(failed); return; }
                    dialog.dismiss();
                    Toast.makeText(activity, "Saved " + entry.name + " to Favorites", Toast.LENGTH_SHORT).show();
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
