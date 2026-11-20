package dev.pgrazor;
import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.location.*;
import android.os.*;
import android.util.AtomicFile;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;
public final class Cooldown {
    private static final CooldownState state=new CooldownState();
    private static final ExecutorService io=Executors.newSingleThreadExecutor();
    private static final ExecutorService geo=Executors.newSingleThreadExecutor();
    private static final Handler main=new Handler(Looper.getMainLooper());
    private static final Map<String,String> countries=new ConcurrentHashMap<>();
    private static AtomicFile file; private static long nextId; private static Context context;
    private static volatile IOException writeFailure;
    public static synchronized void init(Context app){context=app.getApplicationContext();file=new AtomicFile(AccountProfiles.file(app,"cooldown-history.txt"));try{state.decode(new String(file.readFully(),StandardCharsets.UTF_8));for(CooldownState.Entry e:state.rows)nextId=Math.max(nextId,e.id);}catch(IOException ignored){}}
    static void flush() throws IOException {
        try {io.submit(()->{}).get(2500,TimeUnit.MILLISECONDS);if(writeFailure!=null)throw writeFailure;}
        catch(Exception error){throw new IOException("Cooldown save did not finish",error);}
    }
    private static void save(){final String data=state.encode();final AtomicFile f=file;if(f==null)return;io.execute(()->{FileOutputStream out=null;try{out=f.startWrite();out.write(data.getBytes(StandardCharsets.UTF_8));f.finishWrite(out);writeFailure=null;}catch(IOException e){writeFailure=e;if(out!=null)f.failWrite(out);Control.event("cooldown-save-error",e.toString());}});}
    public static synchronized void record(String action){double lat=Control.currentLatitude(),lon=Control.currentLongitude();if(!CooldownState.valid(lat,lon)){Control.event("cooldown-action-skipped","action="+action+" reason=location-unavailable");return;}long now=System.currentTimeMillis();if(!state.rows.isEmpty()){CooldownState.Entry last=state.rows.get(0);if(action.equals("Gym battle")&&last.action.equals(action)&&CooldownState.distance(lat,lon,last.lat,last.lon)<0.01&&now-last.time<60000){state.rows.set(0,new CooldownState.Entry(last.id,now,lat,lon,action));if(now/5000!=last.time/5000)save();return;}if(last.action.equals(action)&&now-last.time<1500&&CooldownState.distance(lat,lon,last.lat,last.lon)<0.01)return;}state.add(new CooldownState.Entry(++nextId,now,lat,lon,action));save();Control.event("cooldown-action-recorded","action="+action);}
    public static synchronized long remaining(){if(!Control.locationKnown())return 0;return state.remaining(System.currentTimeMillis(),Control.currentLatitude(),Control.currentLongitude());}
    /** Read-only destination estimate: never records teleporting as a cooldown action. */
    public static synchronized long remainingAt(double lat,double lon){return state.remaining(System.currentTimeMillis(),lat,lon);}
    public static synchronized boolean hasHistory(){return !state.rows.isEmpty();}
    public static synchronized String label(){if(state.rows.isEmpty())return "Sem ações registradas";if(!Control.locationKnown())return "Aguardando localização";long seconds=(remaining()+999)/1000;if(seconds==0)return "Sem espera estimada";return String.format(Locale.US,"Espera est. %d:%02d:%02d",seconds/3600,(seconds/60)%60,seconds%60);}
    /** Numeric-only display; full explanation remains in the history and accessibility label. */
    public static synchronized String compactLabel(){
        if(state.rows.isEmpty())return "—";
        if(!Control.locationKnown())return "—";
        long seconds=(remaining()+999)/1000;
        return String.format(Locale.US,"%d:%02d:%02d",seconds/3600,(seconds/60)%60,seconds%60);
    }
    private static synchronized ArrayList<CooldownState.Entry> entries(){return new ArrayList<>(state.rows);}
    private static synchronized void remove(long id){state.rows.removeIf(e->e.id==id);save();}
    private static void renderCurrent(Runnable[] callbacks){
        if(callbacks!=null&&callbacks.length>0&&callbacks[0]!=null)callbacks[0].run();
    }
    private static android.widget.ImageButton iconButton(Context ui,int icon,String description){
        android.widget.ImageButton button=new android.widget.ImageButton(ui);
        button.setImageResource(icon);button.setContentDescription(description);
        button.setScaleType(android.widget.ImageView.ScaleType.CENTER_INSIDE);
        button.setPadding(8,8,8,8);
        button.setColorFilter(MapTheme.dark()?0xff30dfb5:0xff087767);
        button.setBackground(SettingsUi.rounded(ui,MapTheme.dark()?0xff26384b:0xffdce8f0,9));
        return button;
    }
    private static Button button(Context c,String text){Button b=new Button(c);b.setText(dev.pgrazor.PtBr.tr(text));b.setAllCaps(false);b.setTextSize(12);return b;}
    private static void country(TextView label,CooldownState.Entry e){String key=String.format(Locale.US,"%.2f,%.2f",e.lat,e.lon);String cached=countries.get(key);if(cached!=null){label.setText(dev.pgrazor.PtBr.tr(cached));return;}label.setText(dev.pgrazor.PtBr.tr("??"));geo.execute(()->{String value="??";try{if(Geocoder.isPresent()){List<Address> found=new Geocoder(context,Locale.US).getFromLocation(e.lat,e.lon,1);if(found!=null&&!found.isEmpty()){String code=found.get(0).getCountryCode();if(code!=null&&code.matches("[A-Z]{2}"))value=new String(Character.toChars(0x1f1e6+code.charAt(0)-'A'))+new String(Character.toChars(0x1f1e6+code.charAt(1)-'A'));}}}catch(Exception ignored){}countries.put(key,value);final String result=value;main.post(()->label.setText(dev.pgrazor.PtBr.tr(result)));});}
    public static void show(Activity a){
        final Context ui=MapTheme.wrap(a); // full-screen, matching the PGBR dark/light theme
        int unit=(int)(48*a.getResources().getDisplayMetrics().density);
        LinearLayout root=new LinearLayout(ui);root.setOrientation(1);root.setPadding(16,12,16,12);
        TextView note=new TextView(ui);note.setText(dev.pgrazor.PtBr.tr("Estimated from recorded actions and current location. Teleporting and walking do not reset it. Automatic: wild encounter throws, encounter berries, spins and gym attacks. Use Add action for gym placement/feeding, accessories or an action performed outside this app. Quest catches are excluded. Deleting history changes only this estimate."));note.setTextSize(12);root.addView(note);
        TextView summary=new TextView(ui);summary.setTextSize(12);root.addView(summary);
        Button add=button(ui,"Add action at current location");root.addView(add);
        final int[] page={0}; final Runnable[] repaint={null};
        LinearLayout paging=new LinearLayout(ui); paging.setGravity(Gravity.CENTER_VERTICAL);
        Button previous=button(ui,"Previous"),next=button(ui,"Next"); TextView pageLabel=new TextView(ui);pageLabel.setGravity(Gravity.CENTER);
        paging.addView(previous,new LinearLayout.LayoutParams(-2,-2));paging.addView(pageLabel,new LinearLayout.LayoutParams(0,-2,1));paging.addView(next,new LinearLayout.LayoutParams(-2,-2));root.addView(paging);
        ScrollView scroll=new ScrollView(ui);LinearLayout list=new LinearLayout(ui);list.setOrientation(1);scroll.addView(list);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        final AlertDialog[] historyDialog={null};
        Runnable render=()->{
            list.removeAllViews();
            ArrayList<CooldownState.Entry> rows=entries();
            summary.setText(rows.isEmpty()?"Nenhuma ação registrada.":"Ações registradas: "+rows.size());
            final int perPage=20, pages=Math.max(1,(rows.size()+perPage-1)/perPage);
            page[0]=Math.min(page[0],pages-1);
            previous.setEnabled(page[0]>0);next.setEnabled(page[0]+1<pages);
            pageLabel.setText((page[0]+1)+" / "+pages);
            if(rows.isEmpty()){
                TextView empty=new TextView(ui);empty.setText("Nenhuma ação registrada ainda.");
                list.addView(empty);
            }
            for(CooldownState.Entry e:rows.subList(page[0]*perPage,Math.min(rows.size(),(page[0]+1)*perPage))){
                LinearLayout row=new LinearLayout(ui);
                row.setGravity(Gravity.CENTER_VERTICAL);
                int margin=(int)(6*a.getResources().getDisplayMetrics().density);
                row.setPadding(margin,margin,margin,margin);
                row.setBackground(SettingsUi.rounded(ui,MapTheme.dark()?0xff202b3b:0xffeaf0f5,12));
                LinearLayout.LayoutParams rowParams=new LinearLayout.LayoutParams(-1,-2);
                rowParams.bottomMargin=margin;
                TextView flag=new TextView(ui);flag.setTextSize(19);
                row.addView(flag,new LinearLayout.LayoutParams(unit/2,-2));country(flag,e);
                TextView info=new TextView(ui);
                String timestamp=new SimpleDateFormat("dd/MM/yy HH:mm",new Locale("pt","BR")).format(new Date(e.time));
                info.setText(timestamp+"  •  "+dev.pgrazor.PtBr.tr(e.action)
                    +String.format(Locale.US,"\n%.5f, %.5f",e.lat,e.lon));
                info.setTextColor(MapTheme.ink());info.setTextSize(12);
                info.setPadding(margin/2,0,margin/2,0);
                row.addView(info,new LinearLayout.LayoutParams(0,-2,1));
                android.widget.ImageButton copy=iconButton(ui,android.R.drawable.ic_menu_copy,"Copiar coordenadas");
                android.widget.ImageButton map=iconButton(ui,android.R.drawable.ic_menu_mapmode,"Abrir no mapa");
                android.widget.ImageButton del=iconButton(ui,android.R.drawable.ic_menu_delete,"Excluir registro");
                row.addView(copy,new LinearLayout.LayoutParams(unit*4/5,unit));
                row.addView(map,new LinearLayout.LayoutParams(unit*4/5,unit));
                row.addView(del,new LinearLayout.LayoutParams(unit*4/5,unit));
                copy.setOnClickListener(v->{
                    ((ClipboardManager)a.getSystemService(Context.CLIPBOARD_SERVICE))
                        .setPrimaryClip(ClipData.newPlainText("Coordenadas",
                            String.format(Locale.US,"%.7f, %.7f",e.lat,e.lon)));
                    Toast.makeText(a,"Coordenadas copiadas",Toast.LENGTH_SHORT).show();
                });
                map.setOnClickListener(v->WorldMapDialog.show(a,e.lat,e.lon,
                    (lat,lon)->mapApply(a,lat,lon,false),(lat,lon)->mapApply(a,lat,lon,true)));
                del.setOnClickListener(v->new AlertDialog.Builder(ui)
                    .setTitle("Excluir registro?")
                    .setMessage("Esta ação remove apenas o registro local do histórico.")
                    .setNegativeButton("Cancelar",null)
                    .setPositiveButton("Excluir",(d,which)->{remove(e.id);renderCurrent(repaint);})
                    .show());
                row.setClickable(true);row.setFocusable(true);
                row.setContentDescription("Teletransportar para "+info.getText());
                row.setOnClickListener(v->TeleportDialog.confirm(a,e.lat,e.lon,(lat,lon)->{
                    boolean ok=mapApply(a,lat,lon,false);
                    if(ok&&historyDialog[0]!=null)historyDialog[0].dismiss();
                    return ok;
                }));
                list.addView(row,rowParams);
            }
            MapTheme.style(list);
        };
        repaint[0]=render;
        previous.setOnClickListener(v->{if(page[0]>0){page[0]--;repaint[0].run();scroll.scrollTo(0,0);}});
        next.setOnClickListener(v->{page[0]++;repaint[0].run();scroll.scrollTo(0,0);});
        add.setOnClickListener(v->{String[] kinds={"Catch / ball throw","Berry feeding","Pok\u00e9Stop spin","Gym placement / feeding","Gym battle","Accessory action"};
            String[] labels=new String[kinds.length];for(int j=0;j<kinds.length;j++)labels[j]=dev.pgrazor.PtBr.tr(kinds[j]);
            AlertDialog pick=ChoiceList.attach(new AlertDialog.Builder(ui).setTitle(dev.pgrazor.PtBr.tr("Record action here")).setNegativeButton(dev.pgrazor.PtBr.tr("Cancel"),null).create(),labels,i->{record(kinds[i]);render.run();});
            pick.setOnShowListener(d->MapTheme.styleButtons(pick));pick.show();});render.run();
        AlertDialog dialog=new AlertDialog.Builder(ui).setTitle(dev.pgrazor.PtBr.tr("Cooldown history")).setView(root).setPositiveButton(dev.pgrazor.PtBr.tr("Done"),null).create();
        dialog.setOnShowListener(d->{MapTheme.fullScreen(dialog);MapTheme.style(root);MapTheme.styleButtons(dialog);});
        historyDialog[0]=dialog;
        DialogLifecycle.bind(a,dialog,null);
        dialog.show();
    }
    /** Applies a destination chosen from the history map, validating adapter readiness first (never a dummy no-op). */
    private static boolean mapApply(Activity a,double lat,double lon,boolean walk){
        if(!Control.canEnable()){Toast.makeText(a,"Location adapter is not ready",Toast.LENGTH_SHORT).show();return false;}
        try{
            if(walk)Control.walkTo(lat,lon);else Control.teleport(lat,lon);
            Control.event(walk?"cooldown-map-walk":"cooldown-map-teleport",String.format(Locale.US,"lat=%.7f lon=%.7f",lat,lon));
            return true;
        }catch(IllegalArgumentException error){Toast.makeText(a,"Select valid coordinates",Toast.LENGTH_SHORT).show();return false;}
    }
}
