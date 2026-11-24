package dev.pgrazor;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.Locale;

/** Map selection is read-only; Quick Menu actions require a separate explicit choice. */
public final class HotspotsDialog {
    private HotspotsDialog() {}
    public interface Selection { void select(Hotspots.Spot spot); }
    public interface Action { boolean apply(double latitude,double longitude); }

    public static AlertDialog show(Activity activity,Selection selection) {
        Context ui=MapTheme.wrap(activity);
        LinearLayout root=new LinearLayout(ui);root.setOrientation(LinearLayout.VERTICAL);
        int pad=MapTheme.dp(ui,12);root.setPadding(pad,pad,pad,pad);
        TextView note=new TextView(ui);note.setText("Catálogo de pontos predefinidos. Categorias são sugestões, não informações ao vivo. Selecione um local para ver no mapa.");note.setTextSize(12);root.addView(note);
        android.widget.EditText search=new android.widget.EditText(ui);search.setSingleLine(true);search.setHint("Buscar país, cidade ou ponto");root.addView(search);
        LinearLayout toolbar=new LinearLayout(ui);android.widget.Button filter=new android.widget.Button(ui);filter.setText("Filtrar");filter.setAllCaps(false);
        android.widget.Button order=new android.widget.Button(ui);order.setText("Ordenar por");order.setAllCaps(false);
        toolbar.addView(filter,new LinearLayout.LayoutParams(0,-2,1));toolbar.addView(order,new LinearLayout.LayoutParams(0,-2,1));root.addView(toolbar);
        ScrollView scroll=new ScrollView(ui);LinearLayout rows=new LinearLayout(ui);rows.setOrientation(LinearLayout.VERTICAL);scroll.addView(rows);
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        AlertDialog dialog=new AlertDialog.Builder(ui).setTitle("Pontos").setView(root).setNegativeButton(dev.pgrazor.PtBr.tr("Close"),null).create();
        java.util.List<TextView> labels=new java.util.ArrayList<>();
        java.util.List<Hotspots.Spot> visible=new java.util.ArrayList<>();
        final Hotspots.Category[] category={null};final int[] sort={0};
        final java.util.Map<String,android.graphics.Bitmap> icons=new java.util.HashMap<>();
        boolean hours24=android.text.format.DateFormat.is24HourFormat(activity);
        Runnable[] tick=new Runnable[1];
        Runnable render=()->{
            rows.removeAllViews();labels.clear();visible.clear();
            String needle=search.getText().toString().trim().toLowerCase(Locale.ROOT);long now=System.currentTimeMillis();
            for(Hotspots.Spot spot:Hotspots.ALL)if(spot.matches(category[0])&&(spot.name+" "+spot.country+" "+spot.city+" "+spot.categoryText()).toLowerCase(Locale.ROOT).contains(needle))visible.add(spot);
            java.util.Comparator<Hotspots.Spot> byName=java.util.Comparator.comparing((Hotspots.Spot p)->p.country,String.CASE_INSENSITIVE_ORDER).thenComparing(p->p.name,String.CASE_INSENSITIVE_ORDER);
            switch(sort[0]){
                case 1:visible.sort(byName);break;
                case 2:visible.sort(java.util.Comparator.comparingInt((Hotspots.Spot p)->p.zone.getOffset(now)).reversed().thenComparing(byName));break;
                case 3:visible.sort(java.util.Comparator.comparingInt((Hotspots.Spot p)->p.zone.getOffset(now)).thenComparing(byName));break;
                case 4:if(Control.locationKnown())visible.sort(java.util.Comparator.comparingDouble(p->WorldMapDialog.distanceMeters(Control.currentLatitude(),Control.currentLongitude(),p.latitude,p.longitude)));break;
                case 5:if(Control.locationKnown()&&Cooldown.hasHistory())visible.sort(java.util.Comparator.comparingLong(p->Cooldown.remainingAt(p.latitude,p.longitude)));break;
                default:break;
            }
            filter.setText(category[0]==null?"Filtrar: todos":category[0].label);
            if(visible.isEmpty()){TextView empty=new TextView(ui);empty.setText("Nenhum ponto corresponde aos filtros.");rows.addView(empty);}
            for(Hotspots.Spot spot:visible){
                LinearLayout card=new LinearLayout(ui);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(pad,pad,pad,pad);
                TextView label=new TextView(ui);label.setTextSize(15);labels.add(label);card.addView(label);
                LinearLayout tags=new LinearLayout(ui);tags.setGravity(android.view.Gravity.CENTER_VERTICAL);
                for(Hotspots.Category c:spot.categories){
                    if(c.icon.isEmpty())continue;
                    if(!icons.containsKey(c.icon))try(java.io.InputStream in=PgAssets.open(ui,"pgrazor/map-icons/"+c.icon)){icons.put(c.icon,android.graphics.BitmapFactory.decodeStream(in));}catch(Exception e){icons.put(c.icon,null);}
                    android.graphics.Bitmap bitmap=icons.get(c.icon);if(bitmap==null)continue;
                    android.widget.ImageView image=new android.widget.ImageView(ui);image.setImageBitmap(bitmap);image.setContentDescription(c.label);image.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);
                    tags.addView(image,new LinearLayout.LayoutParams(MapTheme.dp(ui,24),MapTheme.dp(ui,24)));
                }
                card.addView(tags);
                TextView coordinates=new TextView(ui);coordinates.setText(CoordinateClipboard.text(spot.latitude,spot.longitude));coordinates.setTextSize(13);coordinates.setTextColor(MapTheme.muted());
                card.addView(coordinates);card.setClickable(true);card.setFocusable(true);card.setOnClickListener(v->{dialog.dismiss();selection.select(spot);});rows.addView(card);
            }
            for(int j=0;j<labels.size();j++)labels.get(j).setText(pointLabel(visible.get(j),now,hours24));
        };
        filter.setOnClickListener(v->{Hotspots.Category[] values=Hotspots.Category.values();String[] options=new String[values.length+1];options[0]="Todos";for(int i=0;i<values.length;i++)options[i+1]=values[i].label;
            new AlertDialog.Builder(ui).setTitle("Mostrar categorias").setItems(options,(d,i)->{category[0]=i==0?null:values[i-1];render.run();}).setNegativeButton("Cancelar",null).show();});
        order.setOnClickListener(v->new AlertDialog.Builder(ui).setTitle("Ordenar por").setItems(new String[]{"Ordem do catálogo","País e nome","Fuso: mais adiantado","Fuso: mais atrasado","Distância","Cooldown estimado"},(d,i)->{sort[0]=i;render.run();}).setNegativeButton("Cancelar",null).show());
        search.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int count,int after){}public void onTextChanged(CharSequence s,int st,int before,int count){render.run();}public void afterTextChanged(android.text.Editable e){}});
        tick[0]=()->{long now=System.currentTimeMillis();for(int j=0;j<labels.size();j++)labels.get(j).setText(pointLabel(visible.get(j),now,hours24));if(dialog.isShowing())root.postDelayed(tick[0],30000L);};
        render.run();
        DialogLifecycle.bind(activity,dialog,()->root.removeCallbacks(tick[0]));
        dialog.setOnShowListener(d->{
            MapTheme.fullScreen(dialog);MapTheme.style(root);MapTheme.styleButtons(dialog);
            root.removeCallbacks(tick[0]);root.postDelayed(tick[0],30000L);
        });
        dialog.show();return dialog;
    }

    private static String pointLabel(Hotspots.Spot spot,long now,boolean hours24){
        String text=spot.name+"\n"+spot.city+", "+spot.country+(spot.countryCode.isEmpty()?"":" ("+spot.countryCode+")")+"\n"+spot.local(now,hours24)+"\n"+spot.categoryText();
        if(Control.locationKnown()){text+=String.format(Locale.forLanguageTag("pt-BR"),"\nDistância: %.1f km",WorldMapDialog.distanceMeters(Control.currentLatitude(),Control.currentLongitude(),spot.latitude,spot.longitude)/1000);
            if(Cooldown.hasHistory())text+=" · Cooldown est.: "+((Cooldown.remainingAt(spot.latitude,spot.longitude)+59999)/60000)+" min";else text+=" · Cooldown sem histórico";}
        return text;
    }

    public static void showQuick(Activity activity,Action teleport,Action walk) {
        show(activity,spot->{
            AlertDialog actions=new AlertDialog.Builder(MapTheme.wrap(activity)).setTitle(dev.pgrazor.PtBr.tr(spot.name))
                .setMessage(dev.pgrazor.PtBr.tr(spot.city+", "+spot.country+String.format(Locale.US,"\n%.6f, %.6f",spot.latitude,spot.longitude)))
                .setNegativeButton(dev.pgrazor.PtBr.tr("Cancel"),null).setNeutralButton(dev.pgrazor.PtBr.tr("Walk"),null).setPositiveButton(dev.pgrazor.PtBr.tr("Teleport"),null).create();
            DialogLifecycle.bind(activity,actions,null);
            actions.setOnShowListener(d->{
                MapTheme.styleButtons(actions);
                actions.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v->{if(walk.apply(spot.latitude,spot.longitude))actions.dismiss();});
                actions.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
                    actions.dismiss();TeleportDialog.confirm(activity,spot.latitude,spot.longitude,teleport::apply);
                });
            });actions.show();
        });
    }

}
