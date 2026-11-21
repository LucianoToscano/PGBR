package dev.pgrazor;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONObject;

/**
 * PGBR Selector: species picker -> real, unexpired coordinates -> user-confirmed teleport.
 * Uses the already-integrated iFlowGo source. This is NOT the PGSharp or Pokemod private
 * service: the vendor feed contracts have not been furnished or verified.
 * No fake coordinates, speculative Shiny, automatic global page sweeps or provider bypass.
 */
public final class PokemonSelectorDialog {
    private PokemonSelectorDialog() {}
    public static void show(Activity activity) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;
        new Screen(activity).show();
    }

    private static final class Species {
        final int dex;
        final String name;
        final String normalized;
        Species(int dex, String name) {
            this.dex = dex; this.name = name;
            this.normalized = PokemonSelectorPolicy.normalize(name);
        }
    }

    private static final class Screen {
        private final Activity activity;
        private final Context context;
        private final Handler main = new Handler(Looper.getMainLooper());
        private final ExecutorService network = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "pgbr-selector"); t.setDaemon(true); return t;
        });
        private final IFlowGoFeedClient provider = new IFlowGoFeedClient();
        private final LiveFeedIcons icons;
        private final List<Species> catalog = new ArrayList<>();
        private final List<Species> suggestions = new ArrayList<>();
        private final List<LiveFeedData.Entry> results = new ArrayList<>();
        private final BaseAdapter adapter = new SelectorAdapter();
        private AlertDialog dialog;
        private EditText search;
        private TextView status;
        private Button back;
        private ListView list;
        private boolean showResults;
        private boolean fetching;
        private boolean closed;
        private boolean accessDenied;
        private int selectedDex = -1;
        private String selectedName = "";
        private int requestGeneration;
        private long lastRequestElapsed;
        private long retryAfterElapsed;
        private final Runnable tick = new Runnable() {
            @Override public void run() {
                if (closed) return;
                if (showResults) {
                    pruneExpired();
                    adapter.notifyDataSetChanged();
                    if (selectedDex > 0 && !fetching && !accessDenied && !results.isEmpty())
                        status.setText("Fonte: iFlowGo · " + results.size()
                                + " coordenadas válidas · primeira página mundial");
                }
                main.postDelayed(this, 1000L);
            }
        };

        Screen(Activity activity) {
            this.activity = activity;
            context = MapTheme.wrap(activity);
            icons = new LiveFeedIcons(activity);
            loadCatalog();
        }

        private void loadCatalog() {
            try (InputStream stream = PgAssets.open(activity,
                    "pgrazor/map-icons/pokemon-names.json")) {
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                byte[] buffer = new byte[4096];
                int n;
                while ((n = stream.read(buffer)) != -1) {
                    if (bytes.size() + n > 128 * 1024) break;
                    bytes.write(buffer, 0, n);
                }
                JSONObject names = new JSONObject(
                        new String(bytes.toByteArray(), StandardCharsets.UTF_8));
                for (int dex=1; dex<=1025; dex++) {
                    String name = names.optString(Integer.toString(dex), "").trim();
                    if (!name.isEmpty()) catalog.add(new Species(dex,name));
                }
            } catch (Exception ignored) { }
            updateSuggestions("");
        }

        private void show() {
            LinearLayout root = new LinearLayout(context);
            root.setOrientation(LinearLayout.VERTICAL);
            int pad = dp(16);
            root.setPadding(pad, dp(10), pad, dp(12));

            TextView help = label("Escolha um Pokémon, consulte aparições verificadas "
                    + "e toque na coordenada para teleportar.", 13);
            help.setPadding(0, 0, 0, dp(8));
            root.addView(help);

            LinearLayout searchLine = new LinearLayout(context);
            searchLine.setOrientation(LinearLayout.HORIZONTAL);
            searchLine.setGravity(Gravity.CENTER_VERTICAL);
            back = new Button(context); back.setText("‹ Voltar"); back.setAllCaps(false);
            back.setVisibility(View.GONE);
            back.setOnClickListener(v -> returnToSpecies());
            searchLine.addView(back, new LinearLayout.LayoutParams(dp(90),dp(48)));
            search = new EditText(context);
            search.setSingleLine(true);
            search.setHint("Nome ou #Pokédex");
            search.setFilters(new InputFilter[]{new InputFilter.LengthFilter(60)});
            search.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
            search.addTextChangedListener(new TextWatcher() {
                public void beforeTextChanged(CharSequence s, int st, int count, int after) {}
                public void onTextChanged(CharSequence s, int st, int before, int count) {
                    if (!showResults) {
                        updateSuggestions(s.toString());
                        adapter.notifyDataSetChanged();
                        updateSpeciesStatus();
                    }
                }
                public void afterTextChanged(Editable e) {}
            });
            search.setOnEditorActionListener((v, action, event) -> {
                if (action == EditorInfo.IME_ACTION_SEARCH && !showResults) {
                    int chosen = findUniqueExactDex(search.getText().toString());
                    if (chosen < 1 && suggestions.size() == 1)
                        chosen = suggestions.get(0).dex;
                    if (chosen > 0) selectSpecies(chosen);
                    return true;
                }
                return false;
            });
            searchLine.addView(search,new LinearLayout.LayoutParams(0,dp(50),1));
            root.addView(searchLine);
            status = label("", 12);
            status.setPadding(0, dp(6), 0, dp(8));
            root.addView(status);

            list = new ListView(context);
            list.setDividerHeight(dp(1));
            list.setAdapter(adapter);
            list.setOnItemClickListener((parent,view,position,id)-> {
                if (showResults) {
                    if (position>=0 && position<results.size())
                        confirmTeleport(results.get(position));
                } else if (position>=0 && position<suggestions.size()) {
                    selectSpecies(suggestions.get(position).dex);
                }
            });
            root.addView(list,new LinearLayout.LayoutParams(-1,0,1));

            TextView attribution = label("Resultados atuais: iFlowGo. "
                    + "A API direta de PGSharp/Pokémod ainda não está conectada. "
                    + "A cobertura mundial depende da fonte e pode ser limitada.", 11);
            attribution.setPadding(0,dp(8),0,0);
            root.addView(attribution);
            dialog = new AlertDialog.Builder(context)
                    .setTitle("PGBR · Selector Pokémon")
                    .setView(root)
                    .setNegativeButton("Fechar", null)
                    .setNeutralButton("Filtros avançados", (d,w)->LiveFeedDialog.showPokemonSearch(activity))
                    .create();
            DialogLifecycle.bind(activity,dialog,this::close);
            dialog.setOnShowListener(d -> {
                MapTheme.style(root);
                MapTheme.styleButtons(dialog);
                dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
                        (int)(activity.getResources().getDisplayMetrics().heightPixels * 0.78f));
            });
            dialog.show();
            updateSpeciesStatus();
            main.postDelayed(tick,1000L);
        }

        private void updateSpeciesStatus() {
            if (status != null && !showResults)
                status.setText("Pokédex: " + catalog.size() + " espécies · "
                        + suggestions.size() + " sugestões");
        }

        private void updateSuggestions(String query) {
            suggestions.clear();
            for (Species species:catalog) {
                if (PokemonSelectorPolicy.matchesSpecies(species.dex,species.name,query)) {
                    suggestions.add(species);
                    if (suggestions.size() >= 80) break;
                }
            }
        }

        private int findUniqueExactDex(String input) {
            String q = PokemonSelectorPolicy.normalize(input);
            String numeric = q.startsWith("#") ? q.substring(1) : q;
            try {
                int number = Integer.parseInt(numeric);
                if (number>=1 && number<=1025) return number;
            } catch (NumberFormatException ignored) {}
            for (Species species:catalog)
                if (species.normalized.equals(q)) return species.dex;
            return -1;
        }

        private void selectSpecies(int dex) {
            if (closed) return;
            Species selected = null;
            for(Species species:catalog) if(species.dex==dex){selected=species;break;}
            if (selected == null) return;
            selectedDex = dex;
            selectedName = selected.name;
            showResults = true;
            back.setVisibility(View.VISIBLE);
            search.setText(selectedName);
            search.setEnabled(false);
            results.clear();
            adapter.notifyDataSetChanged();
            querySelectedSpecies();
        }

        private void returnToSpecies() {
            if (closed) return;
            ++requestGeneration;
            provider.cancel();
            fetching = false;
            selectedDex = -1;
            showResults = false;
            results.clear();
            back.setVisibility(View.GONE);
            search.setEnabled(true);
            search.setText("");
            search.requestFocus();
            updateSuggestions("");
            adapter.notifyDataSetChanged();
            updateSpeciesStatus();
        }

        private void querySelectedSpecies() {
            if (closed || !showResults || selectedDex<1 || fetching) return;
            if (accessDenied) {
                status.setText("Fonte recusou acesso (401/403). "
                        + "Não será consultada novamente nesta sessão.");
                return;
            }
            long now = SystemClock.elapsedRealtime();
            if (now<retryAfterElapsed) {
                long seconds=(retryAfterElapsed-now+999L)/1000L;
                status.setText("Limite de consultas da fonte. Nova tentativa após "
                        + seconds + "s.");
                return;
            }
            if (lastRequestElapsed>0 && now-lastRequestElapsed<5000L) {
                long remaining=5000L-(now-lastRequestElapsed);
                status.setText("Consultando nova espécie em " + ((remaining+999L)/1000L) + "s.");
                int g=requestGeneration;
                main.postDelayed(()-> {
                    if (!closed && g==requestGeneration && !fetching) querySelectedSpecies();
                },remaining+40L);
                return;
            }
            fetching=true;
            lastRequestElapsed=now;
            int dex=selectedDex;
            int g=++requestGeneration;
            results.clear();
            adapter.notifyDataSetChanged();
            status.setText("Buscando aparições de " + selectedName + "...");
            network.execute(() -> {
                try {
                    LiveFeedData.Snapshot snapshot=provider.fetchGlobal(dex);
                    main.post(() -> acceptResults(g,dex,snapshot));
                } catch (LiveFeedClient.FeedException e) {
                    final int code=e.status;
                    final long retry=e.retryAfterMs;
                    final String message=e.getMessage();
                    main.post(() -> showError(g,code,retry,message));
                } catch(Exception e) {
                    main.post(() -> showError(g,0,0L,"Não foi possível consultar o radar."));
                }
            });
        }

        private void acceptResults(int generation,int dex,LiveFeedData.Snapshot snapshot) {
            if (closed || generation!=requestGeneration || dex!=selectedDex || !showResults)
                return;
            fetching=false;
            long now=System.currentTimeMillis();
            if(snapshot!=null && snapshot.entries!=null) for(LiveFeedData.Entry entry:snapshot.entries)
                if(PokemonSelectorPolicy.validResult(entry,dex,now)) results.add(entry);
            Collections.sort(results,Comparator.comparingLong(e->e.expiresAt));
            adapter.notifyDataSetChanged();
            status.setText(results.isEmpty()
                    ? "Nenhuma aparição confirmada nesta página para " + selectedName + "."
                    : results.size() + " aparições confirmadas de " + selectedName
                    + " · toque em uma para teleportar.");
        }

        private void showError(int generation,int code,long retry,String message) {
            if (closed || generation!=requestGeneration) return;
            fetching=false;
            results.clear();
            adapter.notifyDataSetChanged();
            if (code==401||code==403) accessDenied=true;
            if (code==429) retryAfterElapsed=SystemClock.elapsedRealtime()
                    + Math.max(60000L,retry);
            status.setText(message!=null && !message.isEmpty()
                    ? message:"Falha ao consultar a fonte.");
        }

        private void pruneExpired() {
            long now=System.currentTimeMillis();
            for (int i=results.size()-1;i>=0;i--)
                if (!PokemonSelectorPolicy.validResult(results.get(i),selectedDex,now)) results.remove(i);
        }

        private void confirmTeleport(LiveFeedData.Entry entry) {
            if (closed || !PokemonSelectorPolicy.validResult(entry,selectedDex,System.currentTimeMillis())) {
                Toast.makeText(activity,"A aparição expirou. Escolha outra.",Toast.LENGTH_SHORT).show();
                pruneExpired();adapter.notifyDataSetChanged();return;
            }
            TeleportDialog.confirm(activity,entry.lat,entry.lon,(lat,lon)-> {
                if (closed || !PokemonSelectorPolicy.validResult(entry,selectedDex,System.currentTimeMillis())
                        || activity.isFinishing() || activity.isDestroyed()) return false;
                if (!Control.canEnable()) {
                    Toast.makeText(activity,
                            "O mecanismo de localização ainda não está pronto.",
                            Toast.LENGTH_SHORT).show();
                    return false;
                }
                Control.teleport(lat,lon);
                Toast.makeText(activity,"Teleporte solicitado: " + selectedName,
                        Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                return true;
            });
        }

        private TextView label(String title,int size) {
            TextView t=new TextView(context);
            t.setText(title);t.setTextSize(size);t.setTextColor(MapTheme.ink());
            return t;
        }
        private int dp(float px) {
            return MapTheme.dp(context,px);
        }
        private void close() {
            if(closed)return;
            closed=true;
            ++requestGeneration;
            main.removeCallbacksAndMessages(null);
            provider.cancel();
            network.shutdownNow();
            results.clear();
        }

        private final class SelectorAdapter extends BaseAdapter {
            public int getCount(){return showResults ? results.size() : suggestions.size();}
            public Object getItem(int i){return showResults?results.get(i):suggestions.get(i);}
            public long getItemId(int i){return i;}
            public View getView(int position,View recycled,ViewGroup parent) {
                LinearLayout row=new LinearLayout(context);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setPadding(dp(8),dp(7),dp(8),dp(7));
                ImageView sprite=new ImageView(context);
                sprite.setScaleType(ImageView.ScaleType.FIT_CENTER);
                sprite.setContentDescription("Sprite Pokémon");
                row.addView(sprite,new LinearLayout.LayoutParams(dp(46),dp(46)));
                LinearLayout words=new LinearLayout(context);
                words.setPadding(dp(12),0,0,0);
                words.setOrientation(LinearLayout.VERTICAL);
                TextView title=label("",15);
                TextView secondary=label("",12);
                secondary.setTextColor(MapTheme.muted());
                words.addView(title);
                words.addView(secondary);
                row.addView(words,new LinearLayout.LayoutParams(0,-2,1));
                if (showResults) {
                    LiveFeedData.Entry entry=results.get(position);
                    title.setText(selectedName+" · "+PokemonSelectorPolicy.timeRemaining(entry.expiresAt,System.currentTimeMillis()));
                    StringBuilder line=new StringBuilder();
                    if (entry.cp>=0) line.append("PC ").append(entry.cp).append(" · ");
                    if (entry.iv>=0) line.append(String.format(Locale.US,"IV %.1f%% · ",entry.iv));
                    line.append(String.format(Locale.US,"%.5f, %.5f",entry.lat,entry.lon));
                    secondary.setText(line.toString());
                    Bitmap bitmap=icons.iconFor(entry,System.currentTimeMillis());
                    if (bitmap!=null) sprite.setImageBitmap(bitmap);
                    else sprite.setImageDrawable(null);
                } else {
                    Species species=suggestions.get(position);
                    title.setText(species.name);
                    secondary.setText("#"+species.dex+"  ·  Buscar aparições");
                    LiveFeedData.Entry placeholder=new LiveFeedData.Entry(
                            "species:"+species.dex,LiveFeedData.Kind.POKEMON,
                            species.name,"",0,0,species.dex,-1,-1,-1,0,false);
                    Bitmap bitmap=icons.iconFor(placeholder,System.currentTimeMillis());
                    if(bitmap!=null)sprite.setImageBitmap(bitmap);
                    else sprite.setImageDrawable(null);
                }
                return row;
            }
        }
    }
}
