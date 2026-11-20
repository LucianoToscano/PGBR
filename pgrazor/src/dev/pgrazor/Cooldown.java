package dev.pgrazor;
import android.app.*;
import android.content.*;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.InsetDrawable;
import android.graphics.drawable.RippleDrawable;
import android.content.res.ColorStateList;
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
    private static Bitmap readIcon(Context ui, String name) {
        try (InputStream in = PgAssets.open(ui, "pgrazor/map-icons/" + name)) {
            return BitmapFactory.decodeStream(in);
        } catch (IOException missing) { return null; }
    }

    private static ImageButton iconButton(Context ui, Bitmap bitmap, int fallback, String description) {
        ImageButton button = new ImageButton(ui);
        if (bitmap != null) button.setImageBitmap(bitmap);
        else if (fallback != 0) button.setImageResource(fallback);
        else button.setImageDrawable(new Drawable() {
            private final Paint ink = new Paint(Paint.ANTI_ALIAS_FLAG);
            public void draw(Canvas canvas) {
                Rect b = getBounds();
                int saved = canvas.save();
                canvas.translate(b.left, b.top);
                canvas.scale(b.width() / 24f, b.height() / 24f);
                ink.setStyle(Paint.Style.STROKE);
                ink.setColor(0xff175448);
                ink.setStrokeWidth(1.8f);
                canvas.drawRoundRect(8, 3, 21, 17, 2, 2, ink);
                canvas.drawRoundRect(3, 8, 16, 21, 2, 2, ink);
                canvas.restoreToCount(saved);
            }
            public void setAlpha(int alpha) { ink.setAlpha(alpha); }
            public void setColorFilter(ColorFilter filter) { ink.setColorFilter(filter); }
            public int getOpacity() { return PixelFormat.TRANSLUCENT; }
        });
        int inset = MapTheme.dp(ui, 6);
        // Light action chips in cooldown history: retain the original glyph silhouettes,
        // recolor their non-transparent pixels for contrast against a white surface.
        Drawable disc = new InsetDrawable(SettingsUi.rounded(ui, 0xffffffff, 20), inset);
        button.setBackground(new RippleDrawable(ColorStateList.valueOf(0x33175448), disc, null));
        button.setColorFilter(0xff175448, android.graphics.PorterDuff.Mode.SRC_IN);
        int padding = MapTheme.dp(ui, 14);
        button.setPadding(padding, padding, padding, padding);
        button.setScaleType(ImageView.ScaleType.FIT_CENTER);
        button.setContentDescription(description);
        return button;
    }

    private static Button button(Context c,String text){Button b=new Button(c);b.setText(dev.pgrazor.PtBr.tr(text));b.setAllCaps(false);b.setTextSize(12);return b;}
    private static void country(TextView label,CooldownState.Entry e){String key=String.format(Locale.US,"%.2f,%.2f",e.lat,e.lon);String cached=countries.get(key);if(cached!=null){label.setText(dev.pgrazor.PtBr.tr(cached));return;}label.setText(dev.pgrazor.PtBr.tr("??"));geo.execute(()->{String value="??";try{if(Geocoder.isPresent()){List<Address> found=new Geocoder(context,Locale.US).getFromLocation(e.lat,e.lon,1);if(found!=null&&!found.isEmpty()){String code=found.get(0).getCountryCode();if(code!=null&&code.matches("[A-Z]{2}"))value=new String(Character.toChars(0x1f1e6+code.charAt(0)-'A'))+new String(Character.toChars(0x1f1e6+code.charAt(1)-'A'));}}}catch(Exception ignored){}countries.put(key,value);final String result=value;main.post(()->label.setText(dev.pgrazor.PtBr.tr(result)));});}
    public static void show(Activity a){
        final Context ui=MapTheme.wrap(a); // full-screen, matching the PGBR dark/light theme
        int unit = MapTheme.dp(ui, 48);
        int margin = MapTheme.dp(ui, 8);
        Bitmap mapIcon = readIcon(ui, "ic_map.png");
        Bitmap trashIcon = readIcon(ui, "ic_trashcan_green.png");
        LinearLayout root = new LinearLayout(ui);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(margin * 2, margin, margin * 2, margin);
        TextView note = new TextView(ui);
        note.setText("A espera é estimada pelas ações recentes e pela localização atual. Caminhar e se teletransportar mantêm a contagem.");
        note.setTextSize(13);
        root.addView(note);
        Button details = button(ui, "Como é calculada?");

        details.setOnClickListener(v -> new AlertDialog.Builder(ui)
            .setTitle("Como a estimativa funciona")
            .setMessage("Arremessos e frutas em encontros selvagens, giros e batalhas de Ginásio entram no histórico automaticamente. Use o registro manual para ações de acessórios, colocação de Pokémon, frutas em Ginásios ou ações feitas fora do PGBR. Capturas de pesquisa ficam de fora. Excluir um registro altera apenas a estimativa local.")
            .setPositiveButton("Entendi", null).show());
        TextView summary = new TextView(ui);
        summary.setTextSize(12);
        LinearLayout summaryRow = new LinearLayout(ui);
        summaryRow.setGravity(Gravity.CENTER_VERTICAL);
        summaryRow.addView(summary, new LinearLayout.LayoutParams(0, -2, 1));
        summaryRow.addView(details);
        root.addView(summaryRow);
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
                LinearLayout row = new LinearLayout(ui);
                row.setOrientation(LinearLayout.VERTICAL);
                row.setPadding(margin, margin, margin, 0);
                row.setBackground(SettingsUi.rounded(ui, MapTheme.dark() ? 0xff202b3b : 0xffedf2f5, 12));
                LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(-1, -2);
                rowParams.bottomMargin = margin;
                LinearLayout header = new LinearLayout(ui);
                header.setGravity(Gravity.CENTER_VERTICAL);
                row.addView(header);
                TextView flag = new TextView(ui);
                flag.setTextSize(16);
                header.addView(flag);
                country(flag, e);
                String action = PtBr.tr(e.action);
                TextView title = new TextView(ui);
                title.setText(action);
                title.setPadding(margin, 0, margin, 0);
                title.setTextSize(14);
                title.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
                header.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
                TextView time = new TextView(ui);
                SimpleDateFormat format = new SimpleDateFormat("dd/MM/yy HH:mm", Locale.US);
                format.setTimeZone(TimeZone.getTimeZone(TimeZoneLookup.zoneId(e.lat, e.lon)));
                time.setText(format.format(new Date(e.time)));
                time.setTextSize(12);
                header.addView(time);
                LinearLayout actions = new LinearLayout(ui);
                actions.setGravity(Gravity.CENTER_VERTICAL);
                row.addView(actions);
                TextView coords = new TextView(ui);
                coords.setText(String.format(Locale.US, "%.5f, %.5f", e.lat, e.lon));
                coords.setTextSize(13);
                coords.setFontFeatureSettings("tnum");
                actions.addView(coords, new LinearLayout.LayoutParams(0, -2, 1));
                ImageButton copy = iconButton(ui, null, 0, "Copiar coordenadas");
                ImageButton map = iconButton(ui, mapIcon, android.R.drawable.ic_menu_mapmode, "Abrir no mapa");
                ImageButton del = iconButton(ui, trashIcon, android.R.drawable.ic_menu_delete, "Excluir registro");
                for (ImageButton control : new ImageButton[]{copy, map, del})
                    actions.addView(control, new LinearLayout.LayoutParams(unit, unit));
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
                row.setContentDescription("Teletransportar para "+coords.getText()+", "+action+", "+time.getText());
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
        if(!Control.canEnable()){Toast.makeText(a,PtBr.tr("Location adapter is not ready"),Toast.LENGTH_SHORT).show();return false;}
        try{
            if(walk)Control.walkTo(lat,lon);else Control.teleport(lat,lon);
            Control.event(walk?"cooldown-map-walk":"cooldown-map-teleport",String.format(Locale.US,"lat=%.7f lon=%.7f",lat,lon));
            return true;
        }catch(IllegalArgumentException error){Toast.makeText(a,PtBr.tr("Select valid coordinates"),Toast.LENGTH_SHORT).show();return false;}
    }
}
