package dev.pgrazor;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Full-screen LivePokeMap feed. Shows a login form while the saved session is unsigned, then a
 * themed, filterable list of nearby live results fetched around the current {@link Control} engine
 * coordinates (never a remote account's view). Tapping a row opens a read-only detail child dialog
 * whose Map / Walk / Teleport actions are the only things that can move the location.
 *
 * <p>Networking (login/fetch) is delegated to {@link LiveFeedClient} and runs on a single serialized
 * background {@link ExecutorService}; parsing happens off the main thread inside the client. A
 * request generation counter rejects any response that arrives after the filters, a logout, or a
 * close changed what the UI expects, so stale data never lands. All timers, the connection and the
 * executor are torn down when the dialog is dismissed or the activity leaves the foreground via
 * {@link DialogLifecycle}.
 */
public final class LiveFeedDialog {
    private LiveFeedDialog() {}

    private static final String[] CATEGORIES = {"All", "Pokémon", "Stops", "Gyms", "Raids", "Power Spots"};
    private static final double[] RADII = {1, 3, 5, LiveFeedClient.GLOBAL};
    private static final String[] RADIUS_LABELS = {"1 km", "3 km", "5 km", "Global"};
    private static final int DEFAULT_REFRESH_SECONDS = 15;
    /** A 429 always backs off at least this long regardless of a shorter server Retry-After. */
    private static final long MIN_BACKOFF_MS = 30_000L;

    /** Entry point wired by the coordinator from the quick menu / {@link Pad}. */
    public static void show(Activity activity) { show(activity, false, false); }

    /** Pokémon feed search with species/IV/level and saved filter sets. */
    public static void showPokemonSearch(Activity activity) { show(activity, true, false); }

    /** Separate Raid feed search. Result data comes from LivePokeMap, NOT PGSharp. */
    public static void showRaidSearch(Activity activity) { show(activity, false, true); }

    private static void show(Activity activity, boolean pokemonSearch, boolean raidSearch) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;
        Context ui = MapTheme.wrap(activity);
        LinearLayout root = new LinearLayout(ui);
        root.setOrientation(LinearLayout.VERTICAL);
        AlertDialog dialog = new AlertDialog.Builder(ui)
            .setTitle(dev.pgrazor.PtBr.tr(raidSearch ? "Buscar Reides · fontes ao vivo"
                    : pokemonSearch ? "Buscar Pokémon · fontes ao vivo" : "LivePokeMap"))
            .setView(root)
            .setNegativeButton(dev.pgrazor.PtBr.tr("Close"), null)
            .create();
        Controller controller = new Controller(activity, ui, dialog, root, pokemonSearch, raidSearch);
        DialogLifecycle.bind(activity, dialog, controller::close, ()->controller.showingLogin,
            controller::pauseLogin,controller::resumeLogin);
        dialog.setOnShowListener(d -> controller.attach());
        dialog.show();
    }

    /** Owns all screen state, the single network worker, the poll/expiry clock and teardown. */
    private static final class Controller {
        private final Activity activity;
        private final Context ui;
        private final AlertDialog dialog;
        private final LinearLayout root;
        private final SharedPreferences prefs;
        private final boolean pokemonSearchMode;
        private final boolean raidSearchMode;
        private RaidFeedFilters raidFilters = RaidFeedFilters.defaults();
        private String pokemonQuery = "";
        private final Handler main = new Handler(Looper.getMainLooper());
        private final ExecutorService net = Executors.newSingleThreadExecutor(task -> {
            Thread t = new Thread(task, "pgrazor-livefeed"); t.setDaemon(true); return t;
        });
        private final LiveFeedClient client;
        private final PokeListFeedClient pokeList = new PokeListFeedClient();
        private final IFlowGoFeedClient iFlowGo = new IFlowGoFeedClient();
        private final RocketMapFeedClient rocketMap = new RocketMapFeedClient();
        /** 0=LivePokeMap, 1=PokeList/Moonani (Pokémon only), 2=iFlowGo, 3=RocketMap. */
        private int provider;
        private final LiveFeedIcons icons;
        private volatile org.json.JSONObject speciesNames;
        private final List<AlertDialog> children = new ArrayList<>();
        private final Runnable tick = this::tick;
        private final Runnable deferredFetch = this::requestFetch;

        // All of the following are touched on the main thread only.
        private final List<LiveFeedData.Entry> entries = new ArrayList<>();
        private final List<LiveFeedData.Entry> visible = new ArrayList<>();
        private HashMap<LiveFeedData.Entry, Double> distCache = new HashMap<>();
        private final FeedAdapter adapter = new FeedAdapter();
        private boolean closed;
        private boolean showingLogin;
        private boolean loginPaused, renderOnResume;
        private int generation;               // bumped on radius/min-IV change, logout and close
        private boolean fetchInFlight, loginInFlight;
        private long lastFetchStartedElapsed; // SystemClock.elapsedRealtime
        private long lastUpdated;             // System.currentTimeMillis of last applied snapshot
        private long backoffUntilElapsed;
        /** Access-denied is permanent until a deliberate retry, never polled. */
        private boolean pokeListAccessDenied;
        private boolean iFlowGoAccessDenied;
        private boolean rocketMapAccessDenied;
        /** Memory only: never persisted to SharedPreferences, logs or a URL. */
        private String rocketReadSecret = "";
        private int refreshSeconds = DEFAULT_REFRESH_SECONDS;
        private String note = "", lastError;
        private int categoryIndex, radiusIndex = 1;
        private LiveFeedFilters filters;      // immutable; replaced wholesale on Apply
        private Button[] catButtons, radiusButtons;
        private Button raidFilterButton;
        private TextView feedStatus;

        Controller(Activity activity, Context ui, AlertDialog dialog, LinearLayout root,
                   boolean pokemonSearchMode, boolean raidSearchMode) {
            this.activity = activity; this.ui = ui; this.dialog = dialog; this.root = root;
            this.pokemonSearchMode = pokemonSearchMode;
            this.raidSearchMode = raidSearchMode;
            prefs = AccountProfiles.preferences(activity,"pgrazor-livefeed");
            try {
                raidFilters = new RaidFeedFilters(
                    prefs.getInt("raid_search_boss_v1", 0),
                    prefs.getInt("raid_search_time_v1", 0),
                    prefs.getInt("raid_search_type_v1", 0),
                    prefs.getInt("raid_search_team_v1", 0),
                    prefs.getBoolean("raid_search_hatched_v1", false));
            } catch (RuntimeException ignored) { raidFilters = RaidFeedFilters.defaults(); }
            pokemonQuery = prefs.getString("pokemon_query_v1", "");
            if (pokemonQuery == null || pokemonQuery.length() > 80) pokemonQuery = "";
            provider = pokemonSearchMode ? clamp(prefs.getInt("pokemon_provider_v1", 2), 0, 3)
                    : raidSearchMode ? clamp(prefs.getInt("raid_provider_v1", 2), 0, 3) : 0;
            // iFlowGo offers a verified-in-network area feed without requiring a LivePokeMap login.
            // Existing explicit provider selections remain saved independently per search.
            categoryIndex = pokemonSearchMode ? 1 : raidSearchMode ? 4
                    : clamp(prefs.getInt("category", 0), 0, CATEGORIES.length - 1);
            radiusIndex = clamp(prefs.getInt(radiusPreferenceKey(), provider == 2 ? 3 : 1),
                     0, RADII.length - 1);
            filters = loadFilters();
            // The session constructor must not touch the keystore on the main thread; token use is
            // confined to login()/fetch() which run on the network worker.
            client = new LiveFeedClient(new LiveFeedSession(activity));
            icons = new LiveFeedIcons(activity);
            showingLogin = provider == 0 && !client.signedIn();
            net.execute(() -> {
                try (java.io.InputStream in = PgAssets.open(activity, "pgrazor/map-icons/pokemon-names.json")) {
                    java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
                    byte[] buffer = new byte[4096]; int count;
                    while ((count = in.read(buffer)) != -1) {
                        if (bytes.size() + count > 2 * 1024 * 1024) return;
                        bytes.write(buffer, 0, count);
                    }
                    speciesNames = new org.json.JSONObject(bytes.toString("UTF-8"));
                } catch (Exception ignored) { }
            });
        }

        void attach() {
            MapTheme.fullScreen(dialog);
            dialog.getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
            dialog.getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
            render();
            MapTheme.styleButtons(dialog);
            main.removeCallbacks(tick);
            main.postDelayed(tick, 1000);
        }

        private void pauseLogin(){loginPaused=true;main.removeCallbacks(tick);}
        private void resumeLogin(){
            if(closed||!loginPaused)return;
            loginPaused=false;
            if(renderOnResume){renderOnResume=false;render();}
            main.removeCallbacks(tick);main.postDelayed(tick,1000);
        }

        // ------------------------------------------------------------------ rendering

        private void render() {
            root.removeAllViews();
            feedStatus = null; catButtons = radiusButtons = null;
            if (showingLogin) buildLogin(); else buildFeed();
            MapTheme.style(root);
            if (!showingLogin) highlightAll();
        }

        private void buildLogin() {
            int pad = dp(18);
            root.setPadding(0, 0, 0, 0);
            android.widget.ScrollView scroll = new android.widget.ScrollView(ui);
            scroll.setFillViewport(true);
            LinearLayout form = new LinearLayout(ui);
            form.setOrientation(LinearLayout.VERTICAL);
            form.setPadding(pad, dp(8), pad, dp(24));
            form.setFocusableInTouchMode(true);
            scroll.addView(form, new android.widget.ScrollView.LayoutParams(-1, -2));
            root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
            if (pokemonSearchMode || raidSearchMode) {
                Button liveAlternative = button("Consultar iFlowGo sem login LivePokeMap");
                form.addView(liveAlternative, new LinearLayout.LayoutParams(-1, dp(52)));
                liveAlternative.setOnClickListener(v -> selectProvider(2));
            }
            if (pokemonSearchMode || raidSearchMode) {
                Button rocket = button("Usar servidor RocketMap HTTPS");
                form.addView(rocket, new LinearLayout.LayoutParams(-1, dp(52)));
                rocket.setOnClickListener(v -> selectProvider(3));
            }
            if (pokemonSearchMode) {
                Button back = button("Consultar PokeList / Moonani (experimental)");
                form.addView(back, new LinearLayout.LayoutParams(-1, dp(52)));
                back.setOnClickListener(v -> selectProvider(1));
            }
            TextView intro = label("Sign in with your LivePokeMap account.", 14);
            intro.setPadding(0, 0, 0, dp(20));
            form.addView(intro);
            final EditText username = new EditText(ui);
            username.setHint(dev.pgrazor.PtBr.tr("Username"));
            username.setSingleLine(true);
            username.setInputType(InputType.TYPE_CLASS_TEXT);
            username.setId(View.generateViewId());
            username.setAutofillHints(View.AUTOFILL_HINT_USERNAME);
            username.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_YES);
            username.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_NEXT);
            addLoginField(form, username);
            final EditText password = new EditText(ui);
            password.setHint(dev.pgrazor.PtBr.tr("Password"));
            password.setSingleLine(true);
            // Let the user's password manager fill credentials; the app stores only its session.
            password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            password.setTypeface(android.graphics.Typeface.DEFAULT);
            password.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_DONE);
            password.setId(View.generateViewId());
            password.setAutofillHints(View.AUTOFILL_HINT_PASSWORD);
            password.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_YES);
            addLoginField(form, password);
            final Button login = button("Log in");
            login.setPadding(0, 0, 0, 0);
            form.addView(login, new LinearLayout.LayoutParams(-1, dp(48)));
            final TextView status = label("", 13);
            status.setPadding(0, dp(10), 0, 0);
            form.addView(status);
            form.requestFocus();
            password.setOnEditorActionListener((v, action, event) -> {
                if (action != android.view.inputmethod.EditorInfo.IME_ACTION_DONE) return false;
                login.performClick(); return true;
            });
            login.setOnClickListener(v -> {
                if(loginInFlight||loginPaused)return;
                String u = username.getText().toString().trim();
                String p = password.getText().toString();
                // Clear the password field immediately; it stays only in the local that is handed to
                // the worker, never logged or saved by PGBR.
                password.setText(dev.pgrazor.PtBr.tr(""));
                if (u.isEmpty() || p.isEmpty()) { status.setText(dev.pgrazor.PtBr.tr("Enter your username and password.")); return; }
                if (loginInFlight) return;
                loginInFlight = true;
                login.setEnabled(false);
                status.setText(dev.pgrazor.PtBr.tr("Signing in…"));
                submitLogin(u, p, login, username, status);
            });
        }

        private void addLoginField(LinearLayout form, EditText field) {
            field.setTextSize(16);
            field.setGravity(Gravity.CENTER_VERTICAL);
            field.setPadding(dp(14), dp(12), dp(14), dp(12));
            field.setHintTextColor(MapTheme.muted());
            GradientDrawable background = new GradientDrawable();
            background.setColor(MapTheme.dark() ? 0xff202b39 : 0xffedf2f6);
            background.setCornerRadius(dp(9));
            background.setStroke(dp(1), MapTheme.muted());
            field.setBackground(background);
            field.setBackgroundTintList(null);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(56));
            lp.bottomMargin = dp(16);
            form.addView(field, lp);
        }

        private void submitLogin(String username, String password, Button login, EditText userField, TextView status) {
            final int g = generation;
            net.execute(() -> {
                try {
                    client.login(username, password); // saves the session on this worker; serialized with logout
                    main.post(() -> {
                        loginInFlight = false;
                        if (closed) return;
                        userField.setText(dev.pgrazor.PtBr.tr("")); // credentials cleared on completion
                        if (g != generation) return; // superseded by a logout or close
                        showingLogin = false;
                        lastError = null; note = ""; lastUpdated = 0; backoffUntilElapsed = 0;
                        entries.clear();
                        if(loginPaused)renderOnResume=true;
                        else render(); // buildFeed() kicks off the first fetch
                    });
                } catch (LiveFeedClient.FeedException fe) {
                    final String msg = loginError(fe);
                    main.post(() -> {
                        loginInFlight = false;
                        if (closed || g != generation) return; // superseded; old login views are gone
                        login.setEnabled(true); status.setText(dev.pgrazor.PtBr.tr(msg));
                    });
                } catch (Exception e) {
                    main.post(() -> {
                        loginInFlight = false;
                        if (closed || g != generation) { return; }
                        login.setEnabled(true);
                        status.setText(dev.pgrazor.PtBr.tr("Couldn't reach LivePokeMap. Check your connection and try again."));
                    });
                }
            });
        }

        private void buildFeed() {
            int pad = dp(14);
            root.setPadding(pad, dp(6), pad, 0);

            if (pokemonSearchMode || raidSearchMode) {
                LinearLayout sources = new LinearLayout(ui);
                sources.setOrientation(LinearLayout.HORIZONTAL);
                Button live = segButton("LivePokeMap");
                Button iflow = segButton("iFlowGo");
                live.setOnClickListener(v -> selectProvider(0));
                iflow.setOnClickListener(v -> selectProvider(2));
                live.setTypeface(null, provider == 0
                        ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
                iflow.setTypeface(null, provider == 2
                        ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
                sources.addView(live, new LinearLayout.LayoutParams(-2, dp(44)));
                if (pokemonSearchMode) {
                    Button list = segButton("PokeList");
                    list.setOnClickListener(v -> selectProvider(1));
                    list.setTypeface(null, provider == 1
                            ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
                    sources.addView(list, new LinearLayout.LayoutParams(-2, dp(44)));
                }
                sources.addView(iflow, new LinearLayout.LayoutParams(-2, dp(44)));
                Button rocket = segButton("RocketMap");
                rocket.setOnClickListener(v -> selectProvider(3));
                rocket.setTypeface(null, provider == 3
                        ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
                sources.addView(rocket, new LinearLayout.LayoutParams(-2, dp(44)));
                // Horizontal scrolling prevents four source labels from being
                // clipped on narrow Android screens.
                HorizontalScrollView scroll = new HorizontalScrollView(ui);
                scroll.setHorizontalScrollBarEnabled(false);
                scroll.addView(sources);
                root.addView(scroll);
                if (provider == 3) {
                    Button config = button("Configurar servidor RocketMap HTTPS");
                    config.setOnClickListener(v -> configureRocketMap());
                    root.addView(config, new LinearLayout.LayoutParams(-1, dp(44)));
                }
                Button more = button("Fontes, autorização e créditos");
                more.setOnClickListener(v -> new AlertDialog.Builder(ui)
                    .setTitle("Fontes e créditos")
                    .setMessage("Fontes desta busca:\n"
                        + "• iFlowGo: busca mundial Pokémon por página e radar de Reides por área (até 25 km), "
                        + "consultado via HTTPS. O acesso pode mudar e exige confirmação "
                        + "das condições de integração do fornecedor.\n"
                        + "• RocketMap: servidor próprio ou autorizado, com scanner conectado; nenhum servidor público é incluído.\n"
                        + "• LivePokeMap: exige conta própria e respeita suas permissões.\n"
                        + "• PokeList / Moonani: experimental, apenas Pokémon; "
                        + "a fonte pode negar acesso (HTTP 403).\n\n"
                        + "PGSharp, iPogo, Pokémod e PGTools: funcionalidades usadas como "
                        + "referência visual, sem conexão com os servidores privados.\n\n"
                        + "PoGoCoordinates permanece como opção independente no menu principal.")
                    .setPositiveButton("Entendi", null).show());
                root.addView(more, new LinearLayout.LayoutParams(-1, dp(44)));
            }

            LinearLayout toolbar = new LinearLayout(ui);
            toolbar.setOrientation(LinearLayout.HORIZONTAL);
            Button logout = button("Log out"), refresh = button("Refresh"), filterBtn = button("Filters");
            if (provider == 0)
                toolbar.addView(logout, new LinearLayout.LayoutParams(0, dp(44), 1));
            toolbar.addView(refresh, new LinearLayout.LayoutParams(0, dp(44), 1));
            toolbar.addView(filterBtn, new LinearLayout.LayoutParams(0, dp(44), 1));
            root.addView(toolbar);
            logout.setOnClickListener(v -> logout());
            refresh.setOnClickListener(v -> {
                // A manual retry is explicit. HTTP 401/403 must never cause
                // automatic retries against an endpoint that refused access.
                if (provider == 2 && iFlowGoAccessDenied) {
                    iFlowGoAccessDenied = false;
                    lastFetchStartedElapsed = 0;
                }
                if (provider == 3 && rocketMapAccessDenied) {
                    rocketMapAccessDenied = false;
                    lastFetchStartedElapsed = 0;
                }
                if (provider == 1 && pokeListAccessDenied) {
                    pokeListAccessDenied = false;
                    // Explicit retry after access denial. Do not reset the
                    // 429 Retry-After timer on ordinary refreshes.
                    lastFetchStartedElapsed = 0;
                }
                if (provider == 2 && pokemonSearchMode && fetchInFlight) {
                    // New explicit filter request supersedes an older in-flight page.
                    generation++;
                    iFlowGo.cancel();
                    fetchInFlight = false;
                }
                requestFetch();
            });
            filterBtn.setOnClickListener(v -> {
                if (raidSearchMode) openRaidFilters();
                else openFilters();
            });
            if (pokemonSearchMode) {
                // Visible recovery action for stale incompatible filter presets.
                // Does not bypass 401/403 or provider-imposed rate limits.
                Button clear = button("Limpar filtros e buscar novamente");
                root.addView(clear, new LinearLayout.LayoutParams(-1, dp(42)));
                clear.setOnClickListener(v -> {
                    filters = new LiveFeedFilters();
                    pokemonQuery = "";
                    prefs.edit().putString("pokemon_query_v1", "").apply();
                    saveFilters();
                    generation++;
                    client.cancel(); pokeList.cancel(); iFlowGo.cancel(); rocketMap.cancel();
                    fetchInFlight = false;
                    entries.clear(); visible.clear(); distCache.clear();
                    lastUpdated = 0; lastError = null;
                    main.removeCallbacks(deferredFetch);
                    // Keep provider refusals/backoffs intact. Render resets
                    // the visible search field to the blank query.
                    render();
                });
            }

            {
                TextView around = label("Around current location", 13);
                around.setPadding(0, dp(6), 0, dp(2));
                root.addView(around);

                // The direct Pokémon search stays focused on Pokémon. The full feed
                // retains its separate category controls unchanged.
                if (!pokemonSearchMode && !raidSearchMode) {
                // Categories: a compact horizontally scrollable segmented control.
                HorizontalScrollView catScroll = new HorizontalScrollView(ui);
                catScroll.setHorizontalScrollBarEnabled(false);
                LinearLayout catRow = new LinearLayout(ui);
                catRow.setOrientation(LinearLayout.HORIZONTAL);
                catButtons = new Button[CATEGORIES.length];
                for (int i = 0; i < CATEGORIES.length; i++) {
                    final int index = i;
                    Button b = segButton(CATEGORIES[i]);
                    b.setOnClickListener(v -> selectCategory(index));
                    catRow.addView(b, new LinearLayout.LayoutParams(-2, dp(40)));
                    catButtons[i] = b;
                }
                catScroll.addView(catRow);
                root.addView(catScroll);
                }

                LinearLayout radiusRow = new LinearLayout(ui);
                radiusRow.setOrientation(LinearLayout.HORIZONTAL);
                radiusRow.setGravity(Gravity.CENTER_VERTICAL);
                radiusRow.addView(label("Radius", 13), new LinearLayout.LayoutParams(dp(58), -2));
                radiusButtons = new Button[RADII.length];
                for (int i = 0; i < RADII.length; i++) {
                    final int index = i;
                    Button b = segButton((provider == 3 || provider == 2 && raidSearchMode) && i == 3 ? "25 km" : RADIUS_LABELS[i]);
                    b.setOnClickListener(v -> selectRadius(index));
                    radiusRow.addView(b, new LinearLayout.LayoutParams(0, dp(40), 1));
                    radiusButtons[i] = b;
                }
                root.addView(radiusRow);
            }

            if (raidSearchMode) {
                TextView attribution = label("Fonte: " + sourceName()
                        + ". Reides reais, se informadas pelo serviço. "
                        + "Aparições por área no iFlowGo (máx. 25 km), não feed global. "
                        + "Não utiliza servidores privados do PGSharp. "
                        + "Popularidade do lobby e clima só são exibidos se comprovados.", 12);
                attribution.setPadding(0, dp(8), 0, dp(8));
                root.addView(attribution);
                raidFilterButton = button(raidFilters.summary() + " · Editar filtro");
                raidFilterButton.setOnClickListener(v -> openRaidFilters());
                root.addView(raidFilterButton, new LinearLayout.LayoutParams(-1, dp(48)));
            }

            if (pokemonSearchMode) {
                TextView providerLabel = label(provider == 1
                        ? "Fonte: PokeList / Moonani (experimental). Global exige espécie e "
                          + "pode ser bloqueado pela fonte (HTTP 403)."
                        : provider == 2
                          ? "Fonte: iFlowGo. Raio local até 5 km ou Global (primeira página "
                            + "mundial). Uma espécie específica é consultada direto no servidor; "
                            + "IV e nível são filtrados no aparelho."
                          : "Fonte: LivePokeMap. Global pode retornar apenas Pokémon 100% IV.", 12);
                providerLabel.setPadding(0, dp(8), 0, dp(4));
                root.addView(providerLabel);
                EditText search = new EditText(ui);
                search.setSingleLine(true);
                search.setHint("Buscar espécie ou #Pokédex (ex.: Pikachu, #25)");
                search.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
                search.setFilters(new android.text.InputFilter[]{
                    new android.text.InputFilter.LengthFilter(80)});
                search.setText(pokemonQuery);
                search.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH);
                search.setOnEditorActionListener((v, action, event) -> {
                    if (action == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                        // Explicit search may change the server-side species query.
                        // Cancel a stale global page; the 5s request gate below
                        // still applies before the next network call.
                        if (provider == 2) {
                            generation++;
                            iFlowGo.cancel();
                            fetchInFlight = false;
                            entries.clear(); visible.clear(); distCache.clear();
                            lastUpdated = 0;
                            adapter.notifyDataSetChanged();
                        }
                        requestFetch(); return true;
                    }
                    return false;
                });
                search.addTextChangedListener(new android.text.TextWatcher() {
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                    public void onTextChanged(CharSequence value, int start, int before, int count) {
                        pokemonQuery = value.toString().trim();
                        prefs.edit().putString("pokemon_query_v1", pokemonQuery).apply();
                        // Global iFlowGo returns only page 1. Local text filtering
                        // cannot find a species absent from that page; request
                        // one bounded, server-side species page instead.
                        if (provider == 2 && Double.isInfinite(RADII[radiusIndex])
                                && !iFlowGoAccessDenied) {
                            generation++;
                            iFlowGo.cancel();
                            fetchInFlight = false;
                            main.removeCallbacks(deferredFetch);
                            entries.clear(); visible.clear(); distCache.clear();
                            lastUpdated = 0;
                            main.postDelayed(deferredFetch, 650);
                        }
                        rebuildVisible();
                        adapter.notifyDataSetChanged();
                        updateStatus();
                    }
                    public void afterTextChanged(android.text.Editable e) {}
                });
                root.addView(search, new LinearLayout.LayoutParams(-1, dp(48)));
                root.addView(label("Use Filtros para IV, Shiny, XXL e PvP. "
                        + "Para Global na PokeList, informe uma espécie (ou #Dex) e toque Atualizar. "
                        + "Toque no Pokémon encontrado para confirmar o teleporte.", 12));
            }

            feedStatus = label("", 12);
            feedStatus.setPadding(0, dp(2), 0, dp(4));
            root.addView(feedStatus);

            FrameLayout listHolder = new FrameLayout(ui);
            ListView list = new ListView(ui);
            list.setAdapter(adapter);
            list.setOnItemClickListener((parent, view, pos, id) -> {
                if (pos >= 0 && pos < visible.size()) {
                    if (pokemonSearchMode) confirmPokemonTeleport(visible.get(pos));
                    else if (raidSearchMode) confirmRaidTeleport(visible.get(pos));
                    else openDetails(visible.get(pos));
                }
            });
            TextView empty = label("Sem aparições confirmadas para esta busca.", 14);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, dp(24), 0, 0);
            listHolder.addView(list, new FrameLayout.LayoutParams(-1, -1));
            listHolder.addView(empty, new FrameLayout.LayoutParams(-1, -2, Gravity.CENTER_HORIZONTAL));
            list.setEmptyView(empty);
            root.addView(listHolder, new LinearLayout.LayoutParams(-1, 0, 1));

            rebuildVisible();
            adapter.notifyDataSetChanged();
            updateStatus();
            requestFetch(); // first load immediately on entering the feed
        }

        private void selectProvider(int selected) {
            if ((!pokemonSearchMode && !raidSearchMode) || selected == provider || closed) return;
            if (raidSearchMode && selected == 1) return;
            ++generation;
            client.cancel(); pokeList.cancel(); iFlowGo.cancel(); rocketMap.cancel();
            provider = selected;
            main.removeCallbacks(deferredFetch);
            radiusIndex = clamp(prefs.getInt(radiusPreferenceKey(), selected == 2 ? 3 : 1),
                    0, RADII.length - 1);
            prefs.edit().putInt(pokemonSearchMode ? "pokemon_provider_v1" : "raid_provider_v1", selected).apply();
            showingLogin = selected == 0 && !client.signedIn();
            fetchInFlight = false;
            backoffUntilElapsed = 0;
            pokeListAccessDenied = false;
            iFlowGoAccessDenied = false;
            rocketMapAccessDenied = false;
            lastFetchStartedElapsed = 0;
            lastUpdated = 0;
            lastError = null; note = "";
            entries.clear(); visible.clear(); distCache.clear();
            render();
        }

        // ------------------------------------------------------------------ filters

        private void selectCategory(int index) {
            if (index == categoryIndex) return;
            categoryIndex = index; prefs.edit().putInt("category", index).apply();
            highlightAll();
            rebuildVisible(); adapter.notifyDataSetChanged(); updateStatus();
            // Category is a pure display filter: no refetch, so in-flight data stays valid.
        }

        private void selectRadius(int index) {
            if (index == radiusIndex) return;
            main.removeCallbacks(deferredFetch);
            radiusIndex = index; prefs.edit().putInt(radiusPreferenceKey(), index).apply();
            if (provider == 3) {
                // Coordinates from the previous viewport must not remain
                // clickable while waiting for an authorised server refresh.
                entries.clear(); visible.clear(); distCache.clear();
                lastUpdated = 0;
                adapter.notifyDataSetChanged();
            }
            generation++;
            client.cancel(); pokeList.cancel(); iFlowGo.cancel(); rocketMap.cancel();
            fetchInFlight = false; // old generation cannot keep the new request blocked
            highlightAll();
            rebuildVisible(); adapter.notifyDataSetChanged();
            requestFetch();
        }

        /**
         * Separate, locally applied Raid feed filters. Only fields present in
         * LivePokeMap's raid records are offered. No PGSharp private service is used.
         */
        private void openRaidFilters() {
            if (closed || showingLogin || !raidSearchMode) return;
            LinearLayout body = new LinearLayout(ui);
            body.setOrientation(LinearLayout.VERTICAL);
            body.setPadding(dp(16), dp(8), dp(16), dp(8));
            body.addView(label("Selecione filtros de reides. Campos não informados pela fonte "
                    + "não são considerados correspondências. Tipo = código bruto retornado "
                    + "pelo servidor (não necessariamente estrelas).", 12));
            EditText boss = raidNumber(body, "Chefe por #Pokédex (0 = todos)", raidFilters.bossDex);
            EditText type = raidNumber(body, "Tipo de reide da fonte (0 = todos)", raidFilters.raidType);
            EditText remaining = raidNumber(body, "Minutos restantes (0 = todos)", raidFilters.minMinutes);
            EditText team = raidNumber(body,
                    "Equipe do ginásio (0 = todas; 1 = Místico, 2 = Valor, 3 = Instinto)",
                    raidFilters.team);
            CheckBox hatch = new CheckBox(ui);
            hatch.setText("Somente reides com chefe confirmado");
            hatch.setChecked(raidFilters.hatchedOnly);
            body.addView(hatch);
            body.addView(label("Popularidade de sala, clima favorável e elegibilidade EX "
                    + "não são filtrados sem campos verificados do provedor.", 12));
            android.widget.ScrollView scroll = new android.widget.ScrollView(ui);
            scroll.addView(body);
            AlertDialog modal = new AlertDialog.Builder(ui)
                .setTitle("Filtros de Reides · PGBR")
                .setView(scroll)
                .setNegativeButton("Cancelar",null)
                .setNeutralButton("Limpar",null)
                .setPositiveButton("Aplicar",null)
                .create();
            trackOwn(modal);
            modal.setOnShowListener(d -> {
                MapTheme.style(body);
                MapTheme.styleButtons(modal);
                modal.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> {
                    raidFilters = RaidFeedFilters.defaults();
                    persistRaidFilters();
                    rebuildVisible(); adapter.notifyDataSetChanged(); updateStatus();
                    if (raidFilterButton != null)
                        raidFilterButton.setText(raidFilters.summary() + " · Editar filtro");
                    modal.dismiss();
                });
                modal.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                    try {
                        raidFilters = new RaidFeedFilters(
                                readRaidNumber(boss, 1025), readRaidNumber(remaining, 180),
                                readRaidNumber(type, 20), readRaidNumber(team, 3),
                                hatch.isChecked());
                    } catch (IllegalArgumentException invalid) {
                        toast("Confira os valores permitidos de cada filtro.");
                        return;
                    }
                    persistRaidFilters();
                    rebuildVisible(); adapter.notifyDataSetChanged(); updateStatus();
                    if (raidFilterButton != null)
                        raidFilterButton.setText(raidFilters.summary() + " · Editar filtro");
                    modal.dismiss();
                });
            });
            modal.show();
        }

        private EditText raidNumber(LinearLayout body, String caption, int current) {
            body.addView(label(caption, 13));
            EditText field = new EditText(ui);
            field.setSingleLine(true);
            field.setInputType(InputType.TYPE_CLASS_NUMBER);
            field.setText(Integer.toString(current));
            body.addView(field, new LinearLayout.LayoutParams(-1, dp(48)));
            return field;
        }

        private int readRaidNumber(EditText field, int maximum) {
            String input = field.getText().toString().trim();
            try {
                int n = input.isEmpty() ? 0 : Integer.parseInt(input);
                if (n < 0 || n > maximum) throw new IllegalArgumentException();
                return n;
            } catch (NumberFormatException invalid) { throw new IllegalArgumentException(invalid); }
        }

        private void persistRaidFilters() {
            prefs.edit()
                .putInt("raid_search_boss_v1",raidFilters.bossDex)
                .putInt("raid_search_time_v1",raidFilters.minMinutes)
                .putInt("raid_search_type_v1",raidFilters.raidType)
                .putInt("raid_search_team_v1",raidFilters.team)
                .putBoolean("raid_search_hatched_v1",raidFilters.hatchedOnly)
                .apply();
        }

        private void openFilters() {
            if (closed || showingLogin) return;
            org.json.JSONObject working;
            try {
                working = filters.toStoredJson(); // a fresh, mutable copy the modal edits in place
            } catch (org.json.JSONException e) { toast("Couldn't open the filters."); return; }
            LiveFeedFilterDialog.show(activity, ui, working, speciesNames, prefs,
                this::trackOwn, this::applyFilters);
        }

        /** Called by the modal on Apply. Validates the edited JSON through the immutable model; a bad
         *  value is rejected so the modal can stay open, otherwise the new set is saved and refetched. */
        private boolean applyFilters(org.json.JSONObject edited) {
            LiveFeedFilters next;
            try {
                next = LiveFeedFilters.fromJson(edited);
            } catch (org.json.JSONException e) { return false; }
            filters = next;
            main.removeCallbacks(deferredFetch);
            entries.clear(); visible.clear(); distCache = new HashMap<>();
            saveFilters();
            generation++;
            client.cancel(); pokeList.cancel(); iFlowGo.cancel(); rocketMap.cancel();
            fetchInFlight = false; // previous filter request is obsolete
            rebuildVisible(); adapter.notifyDataSetChanged();
            requestFetch();
            return true;
        }

        private LiveFeedFilters loadFilters() {
            try {
                String stored = prefs.getString("filters_v1", null);
                if (stored != null) return LiveFeedFilters.fromJson(new org.json.JSONObject(stored));
                int legacy = clamp(prefs.getInt("minIv", 0), 0, 3);
                org.json.JSONObject initial = new LiveFeedFilters().toStoredJson();
                initial.put("min_iv", new int[]{0,80,90,100}[legacy]);
                return LiveFeedFilters.fromJson(initial);
            } catch (Exception ignored) { } // fall back to defaults on any corrupt/legacy value
            return new LiveFeedFilters();
        }

        private void saveFilters() {
            try {
                prefs.edit().putString("filters_v1", filters.toStoredJson().toString()).apply();
            } catch (org.json.JSONException ignored) { }
        }

        private void highlightAll() {
            highlight(catButtons, categoryIndex);
            highlight(radiusButtons, radiusIndex);
        }

        private void highlight(Button[] buttons, int selected) {
            if (buttons == null) return;
            for (int i = 0; i < buttons.length; i++) {
                boolean on = i == selected;
                buttons[i].setTextColor(on ? MapTheme.accent() : MapTheme.ink());
                buttons[i].setTypeface(null, on ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
            }
        }

        // ------------------------------------------------------------------ session

        private void logout() {
            generation++;                 // reject any in-flight login/fetch result
            backoffUntilElapsed = 0; lastError = null; note = ""; lastUpdated = 0;
            entries.clear(); visible.clear(); distCache = new HashMap<>();
            showingLogin = true;
            // Run the token clear on the same serialized worker so it can never race ahead of an
            // in-flight login save; a later login just queues behind it.
            client.logout(); // invalidate immediately, even if this dialog is closed next
            render();
        }

        // ------------------------------------------------------------------ fetching

        private int sourceDexFilter() { return sourceDexFilter(pokemonQuery); }

        private int sourceDexFilter(String requestedQuery) {
            if (!pokemonSearchMode) return -1;
            String q = requestedQuery == null ? "" : requestedQuery.trim();
            String number = q.startsWith("#") ? q.substring(1) : q;
            if (number.matches("[0-9]{1,4}")) {
                try {
                    int dex = Integer.parseInt(number);
                    return dex >= 1 && dex <= 1025 ? dex : -1;
                } catch (NumberFormatException ignored) {return -1;}
            }
            org.json.JSONObject names = speciesNames;
            if (names == null || q.isEmpty()) return -1;
            int found = -1;
            for (int dex=1; dex<=1025; dex++) {
                String name=names.optString(Integer.toString(dex),"");
                if (!name.isEmpty() && PokemonFeedSearch.matches(q,dex,name,name)) {
                    if (found >= 0) return -1; // not unique: never over-filter remotely
                    found = dex;
                }
            }
            return found;
        }

        private void requestFetch() {
            if (closed || showingLogin || fetchInFlight) return;
            if ((provider == 1 && pokeListAccessDenied)
                    || (provider == 2 && iFlowGoAccessDenied)
                    || (provider == 3 && rocketMapAccessDenied)) {
                updateStatus(); return;
            }
            // iFlowGo reports IV, CP, level, species and coordinates, but not
            // trainer-specific Shiny, size, backgrounds or PvP ranks. Never
            // silently show unfiltered results under one of those filter labels.
            if ((provider == 2 || provider == 3) && pokemonSearchMode) {
                try {
                    org.json.JSONObject selected = filters.toStoredJson();
                    boolean unsupported = selected.optBoolean("shiny_only", false)
                            || selected.optBoolean("background_only", false)
                            || selected.optBoolean("filter_xxs", false)
                            || selected.optBoolean("filter_xxl", false)
                            || filters.pvpOnlyActive()
                            // RocketMap's public /raw_data pokemon object has
                            // CP and individual IVs, but no confirmed level.
                            || provider == 3 && (selected.optInt("min_level", 0) > 0
                                    || selected.optInt("max_level", 50) < 50);
                    if (unsupported) {
                        entries.clear(); visible.clear(); distCache.clear();
                        lastUpdated = 0;
                        lastError = sourceName()
                                + " não confirma Shiny individual, fundos, tamanhos XXS/XXL "
                                + "nem rankings PvP; RocketMap também não informa nível. "
                                + "Desative esses filtros ou escolha outra fonte.";
                        adapter.notifyDataSetChanged();
                        updateStatus();
                        return;
                    }
                } catch (org.json.JSONException badFilters) {
                    lastError = "Os filtros estão inválidos. Redefina os filtros para continuar.";
                    updateStatus();
                    return;
                }
            }
            final String configuredRocket = prefs.getString("rocketmap_https_url_v1", "");
            final String secretAtRequest = rocketReadSecret;
            if (provider == 3 && (configuredRocket == null || configuredRocket.trim().isEmpty())) {
                lastError = "Configure a URL HTTPS de um servidor RocketMap "
                        + "próprio ou autorizado. Não há servidor padrão.";
                updateStatus();
                return;
            }
            final int selectedDex = sourceDexFilter();
            final String queryAtRequest = pokemonQuery;
            if (pokemonSearchMode && provider == 1 && radiusIndex == 3 && selectedDex < 1) {
                lastError = "Informe uma espécie específica ou #Dex antes de consultar Global na PokeList.";
                updateStatus(); return;
            }
            long nowE = SystemClock.elapsedRealtime();
            if (nowE < backoffUntilElapsed) { updateStatus(); return; }
            // PokeList lacks published rate-limit semantics. Avoid rapid manual
            // refreshes even when the server did not return Retry-After.
            if (provider == 1 && lastFetchStartedElapsed > 0
                    && nowE - lastFetchStartedElapsed < 60000) {
                updateStatus(); return;
            }
            if (provider == 3 && lastFetchStartedElapsed > 0
                    && nowE - lastFetchStartedElapsed < 60000) {
                long remaining = 60000L - (nowE - lastFetchStartedElapsed);
                lastError = "RocketMap: respeitando intervalo de consultas. "
                        + "Nova busca em " + ((remaining + 999) / 1000) + "s.";
                main.removeCallbacks(deferredFetch);
                main.postDelayed(deferredFetch, remaining + 40);
                updateStatus(); return;
            }
            // Auto-refresh stays at 60s, but explicit iFlowGo species/radius
            // searches may run every 5s. A single deferred callback avoids
            // making the search button silently do nothing under that gate.
            if (provider == 2 && lastFetchStartedElapsed > 0
                    && nowE - lastFetchStartedElapsed < 5000L) {
                long remaining = 5000L - (nowE - lastFetchStartedElapsed);
                lastError = "iFlowGo: nova consulta em " + ((remaining + 999) / 1000) + "s.";
                main.removeCallbacks(deferredFetch);
                main.postDelayed(deferredFetch, remaining + 40);
                updateStatus();
                return;
            }
            main.removeCallbacks(deferredFetch);
            lastError = null;
            fetchInFlight = true;
            lastFetchStartedElapsed = nowE;
            final int g = generation;
            final double lat = Control.engine.latitude, lon = Control.engine.longitude;
            final double radiusKm = RADII[radiusIndex];
            final LiveFeedFilters f = filters; // immutable snapshot; safe to hand to the worker
            final int selectedProvider = provider;
            updateStatus();
            net.execute(() -> {
                try {
                    // The species-name catalog is loaded on this same serialized
                    // worker before the first request. Resolve human names now,
                    // so an early Global search does not fall back to page 1
                    // merely because the catalog was still loading on the UI.
                    int remoteDex = selectedProvider == 2 && selectedDex < 1
                            ? sourceDexFilter(queryAtRequest) : selectedDex;
                    // The Filters modal has a separate species picker.
                    // Previously it filtered just the FIRST global page on
                    // device, producing 0 matches even if the chosen species
                    // exists elsewhere in the provider's covered world.
                    if (selectedProvider == 2 && pokemonSearchMode
                            && Double.isInfinite(radiusKm) && remoteDex < 1
                            && queryAtRequest.trim().isEmpty()) {
                        remoteDex = f.singleAllowedSpecies();
                    }
                    LiveFeedData.Snapshot snap = selectedProvider == 1
                            ? pokeList.fetch(lat, lon, radiusKm, selectedDex)
                            : selectedProvider == 2
                            ? pokemonSearchMode && Double.isInfinite(radiusKm)
                              ? iFlowGo.fetchGlobal(remoteDex)
                              : iFlowGo.fetch(lat, lon, radiusKm, raidSearchMode)
                            : selectedProvider == 3
                            ? rocketMap.fetch(configuredRocket, lat, lon, radiusKm, raidSearchMode,
                                    secretAtRequest)
                            : client.fetch(lat, lon, radiusKm, f);
                    if (selectedProvider == 0 && Double.isInfinite(radiusKm))
                        Control.event("feed-global", snap.entries.size() + " results");
                    main.post(() -> onSnapshot(g, snap));
                } catch (LiveFeedClient.FeedException fe) {
                    final int status = fe.status; final long retry = fe.retryAfterMs;
                    final String msg = safeMessage(fe, "Couldn't load live results right now.");
                    main.post(() -> onFetchError(g, status, retry, msg));
                } catch (Exception e) {
                    main.post(() -> onFetchError(g, 0, 0,
                            selectedProvider == 1
                            ? "Não foi possível consultar PokeList / Moonani. Fonte indisponível."
                            : selectedProvider == 2
                            ? "Não foi possível consultar iFlowGo. Verifique a rede."
                            : selectedProvider == 3
                            ? "Não foi possível consultar RocketMap. Confira URL, HTTPS e permissões."
                            : "Couldn't reach LivePokeMap. Check your connection."));
                }
            });
        }

        private void onSnapshot(int g, LiveFeedData.Snapshot snap) {
            // A response from a previously selected provider/filter/radius
            // must never clear the active request or mix source results.
            if (closed || showingLogin || g != generation) return;
            fetchInFlight = false;
            entries.clear();
            if (snap != null && snap.entries != null) entries.addAll(snap.entries);
            refreshSeconds = snap != null && snap.refreshSeconds > 0 ? snap.refreshSeconds : DEFAULT_REFRESH_SECONDS;
            note = snap != null && snap.note != null ? snap.note : "";
            lastUpdated = snap != null && snap.fetchedAt > 0 ? snap.fetchedAt : System.currentTimeMillis();
            lastError = null;
            rebuildVisible(); adapter.notifyDataSetChanged(); updateStatus();
        }

        private void onFetchError(int g, int status, long retryAfterMs, String message) {
            // Even a late 403 from a previous source must not block the
            // currently selected one or unlock its active fetch.
            if (closed || g != generation) return;
            fetchInFlight = false;
            if (status == 429) backoffUntilElapsed = SystemClock.elapsedRealtime()
                    + Math.max(retryAfterMs, MIN_BACKOFF_MS);
            if (provider == 3 && (status == 401 || status == 403)) {
                rocketMapAccessDenied = true;
                backoffUntilElapsed = 0;
            }
            if (provider == 2 && (status == 401 || status == 403)) {
                iFlowGoAccessDenied = true;
                backoffUntilElapsed = 0;
            }
            if (provider == 1 && (status == 401 || status == 403)) {
                // A 403 on a CI runner was observed on 2026-11-21. This is
                // an access refusal, NOT a rate limit: never poll repeatedly.
                pokeListAccessDenied = true;
                backoffUntilElapsed = 0;
            }
            if (provider == 0 && (status == 401 || !client.signedIn())) {
                // The client clears the session on a 401; drop back to the login form.
                showingLogin = true; lastError = null; entries.clear(); visible.clear();
                render();
                toast("Your LivePokeMap session ended. Please sign in again.");
                return;
            }
            if (provider == 2 || provider == 3 || (pokemonSearchMode && provider == 1)) {
                // External entries may lose freshness when refresh fails; old Moonani entries often have no verified endTime. A failed
                // refresh must never leave indefinite stale coordinates clickable.
                entries.clear(); visible.clear(); distCache.clear();
                lastUpdated = 0;
                adapter.notifyDataSetChanged();
            }
            lastError = message;
            updateStatus();
        }

        /** Repeats every second: drops expired rows against the wall clock, refreshes the relative
         *  "updated" time, and polls once a refresh interval has elapsed (never while in flight or
         *  backing off). Keeps the list current between network fetches with no extra requests. */
        private void tick() {
            if (closed || loginPaused) return;
            if (activity.isFinishing() || activity.isDestroyed()) {
                if (dialog.isShowing()) dialog.dismiss(); else close();
                return;
            }
            if (!showingLogin) {
                rebuildVisible(); adapter.notifyDataSetChanged(); updateStatus();
                long nowE = SystemClock.elapsedRealtime();
                long intervalMs = (provider == 1 || provider == 2 || provider == 3 ? Math.max(60, refreshSeconds) : refreshSeconds) * 1000L;
                if (!fetchInFlight && !(provider == 1 && pokeListAccessDenied)
                    && !(provider == 2 && iFlowGoAccessDenied)
                    && !(provider == 3 && rocketMapAccessDenied)
                    && nowE >= backoffUntilElapsed
                    && nowE - lastFetchStartedElapsed >= intervalMs) requestFetch();
            }
            main.postDelayed(tick, 1000);
        }

        private void updateStatus() {
            if (feedStatus == null) return;
            if (provider == 3 && rocketMapAccessDenied) {
                feedStatus.setText("Seu servidor RocketMap recusou acesso (401/403). "
                    + "Solicite permissão ao administrador; sem tentativas automáticas.");
                return;
            }
            if (provider == 2 && iFlowGoAccessDenied) {
                feedStatus.setText("iFlowGo recusou a consulta (HTTP 401/403). "
                    + "O PGBR não fará tentativas automáticas. "
                    + "A fonte precisa permitir o uso deste acesso.");
                return;
            }
            if (provider == 1 && pokeListAccessDenied) {
                feedStatus.setText("PokeList / Moonani recusou a consulta (HTTP 401/403). "
                    + "Não há resultados confirmados. O PGBR não tentará "
                    + "novamente de forma automática. Use Atualizar apenas "
                    + "após verificar a autorização com a fonte.");
                return;
            }
            long nowE = SystemClock.elapsedRealtime();
            if (nowE < backoffUntilElapsed) {
                long s = (backoffUntilElapsed - nowE + 999) / 1000;
                feedStatus.setText(dev.pgrazor.PtBr.tr(
                        "Limite de " + (sourceName())
                        + ": consulte novamente em " + s + "s."));
                return;
            }
            StringBuilder sb = new StringBuilder();
            if (lastError != null) sb.append(lastError).append('\n');
            if (lastUpdated == 0) {
                if (lastError == null) {
                    sb.append(fetchInFlight ? "Consultando fonte de aparições…" :
                        "Nenhum resultado carregado. Use Atualizar.");
                }
            } else {
                long age = Math.max(0, (System.currentTimeMillis() - lastUpdated) / 1000);
                sb.append("Exibidos: ").append(visible.size())
                  .append(" · recebidos: ").append(entries.size())
                  .append(" · atualizado há ").append(age).append("s");
                if (fetchInFlight) sb.append(" · atualizando…");
                if (visible.isEmpty()) {
                    sb.append('\n');
                    if (entries.isEmpty()) {
                        sb.append("A fonte respondeu com 0 aparições. ")
                          .append("Confira cobertura/raio ou selecione outra fonte autorizada. ")
                          .append("O filtro não consegue criar registros ausentes.");
                    } else {
                        sb.append("A fonte enviou registros, mas nenhum passou nos filtros ")
                          .append("locais de categoria, espécie, IV, distância ou expiração. ")
                          .append("Confira os filtros e tente ampliar o raio.");
                    }
                }
            }
            if (note != null && !note.isEmpty()) sb.append('\n').append(note);
            feedStatus.setText(dev.pgrazor.PtBr.tr(sb.toString()));
        }

        // ------------------------------------------------------------------ list model

        private void rebuildVisible() {
            long now = System.currentTimeMillis();
            final double elat = Control.engine.latitude, elon = Control.engine.longitude;
            final HashMap<LiveFeedData.Entry, Double> dist = new HashMap<>();
            List<LiveFeedData.Entry> list = new ArrayList<>();
            for (LiveFeedData.Entry e : entries) {
                if (e == null || e.expired(now)) continue;               // hide expired every pass
                if (!categoryMatches(e)) continue;
                if (raidSearchMode && (e.kind != LiveFeedData.Kind.RAID
                        || !raidFilters.matches(e.dexId, e.level, e.team,
                            e.raidHatchAt, e.expiresAt, e.raidBossLive, now)))
                    continue;
                if (pokemonSearchMode &&
                        (e.kind != LiveFeedData.Kind.POKEMON ||
                         !PokemonFeedSearch.matches(pokemonQuery, e.dexId, e.name, displayName(e))))
                    continue;
                // The server only partly applies the display filters (not the level range), so
                // re-check Pokémon the way the LivePokeMap website does.
                if (e.kind == LiveFeedData.Kind.POKEMON && !filters.matches(e.dexId, e.hasStats ? e.iv : -1,
                        e.level, e.attrs.size, e.attrs.pvpGreatRank, e.attrs.pvpUltraRank, e.attrs.pvpLittleRank))
                    continue;
                if (!Double.isFinite(e.lat) || !Double.isFinite(e.lon)) continue;
                double meters = distanceMeters(elat, elon, e.lat, e.lon);
                if (meters > RADII[radiusIndex] * 1000) continue;
                dist.put(e, meters);
                list.add(e);
            }
            // Stable (merge) sort keeps the backend's order among equal distances.
            Collections.sort(list, (a, b) -> Double.compare(dist.get(a), dist.get(b)));
            visible.clear(); visible.addAll(list);
            distCache = dist;
        }

        private boolean categoryMatches(LiveFeedData.Entry e) {
            switch (categoryIndex) {
                case 1: return e.kind == LiveFeedData.Kind.POKEMON;
                case 2: return e.kind == LiveFeedData.Kind.STOP;
                case 3: return e.kind == LiveFeedData.Kind.GYM;
                case 4: return e.kind == LiveFeedData.Kind.RAID;
                case 5: return e.kind == LiveFeedData.Kind.STATION;
                default: return true;
            }
        }

        private final class FeedAdapter extends BaseAdapter {
            public int getCount() { return visible.size(); }
            public Object getItem(int position) { return visible.get(position); }
            public long getItemId(int position) { return position; }
            public View getView(int position, View convertView, ViewGroup parent) {
                RowHolder holder;
                if (convertView == null) { holder = buildRow(parent.getContext()); }
                else holder = (RowHolder) convertView.getTag();
                LiveFeedData.Entry e = visible.get(position);
                long now = System.currentTimeMillis();
                Bitmap icon = icons.iconFor(e, now);
                if (icon != null) {
                    holder.icon.setImageBitmap(icon);
                    holder.icon.setVisibility(View.VISIBLE);
                    holder.fallback.setVisibility(View.GONE);
                } else {
                    holder.icon.setImageDrawable(null);
                    holder.icon.setVisibility(View.GONE);
                    holder.fallbackShape.setColor(kindColor(e.kind));
                    holder.fallback.setText(dev.pgrazor.PtBr.tr(glyphLetter(e.kind)));
                    holder.fallback.setVisibility(View.VISIBLE);
                }
                String description = iconDescription(e, now);
                holder.icon.setContentDescription(dev.pgrazor.PtBr.tr(description));
                holder.fallback.setContentDescription(dev.pgrazor.PtBr.tr(description));
                holder.title.setText(dev.pgrazor.PtBr.tr(displayName(e)));
                String sub = displaySubtitle(e, now);
                Double d = distCache.get(e);
                StringBuilder line = new StringBuilder();
                if (sub != null && !sub.isEmpty()) line.append(sub);
                if (d != null) { if (line.length() > 0) line.append("  ·  "); line.append(formatDistance(d)); }
                if (pokemonSearchMode || raidSearchMode) {
                    String credit = sourceName();
                    line.insert(0, credit + "  ·  ");
                }
                holder.subtitle.setText(dev.pgrazor.PtBr.tr(line.toString()));
                return holder.view;
            }
        }

        private RowHolder buildRow(Context context) {
            RowHolder h = new RowHolder();
            LinearLayout row = new LinearLayout(context);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            int pad = dp(10);
            row.setPadding(pad, pad, pad, pad);
            // A 44dp aspect-fit icon; a lettered oval sits behind it and only shows when the bundled
            // asset is missing, so a recycled row never leaks the previous entry's glyph or sprite.
            FrameLayout iconHolder = new FrameLayout(context);
            h.fallback = new TextView(context);
            h.fallback.setGravity(Gravity.CENTER);
            h.fallback.setTextColor(Color.WHITE);
            h.fallback.setTextSize(15);
            h.fallbackShape = new GradientDrawable();
            h.fallbackShape.setShape(GradientDrawable.OVAL);
            h.fallback.setBackground(h.fallbackShape);
            iconHolder.addView(h.fallback, new FrameLayout.LayoutParams(-1, -1));
            h.icon = new ImageView(context);
            h.icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
            h.icon.setAdjustViewBounds(true);
            iconHolder.addView(h.icon, new FrameLayout.LayoutParams(-1, -1));
            LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(dp(44), dp(44));
            iconLp.rightMargin = dp(12);
            row.addView(iconHolder, iconLp);
            LinearLayout text = new LinearLayout(context);
            text.setOrientation(LinearLayout.VERTICAL);
            h.title = new TextView(context); h.title.setTextSize(15.5f); h.title.setTextColor(MapTheme.ink());
            h.subtitle = new TextView(context); h.subtitle.setTextSize(12.5f); h.subtitle.setTextColor(MapTheme.muted());
            text.addView(h.title); text.addView(h.subtitle);
            row.addView(text, new LinearLayout.LayoutParams(0, -2, 1));
            row.setTag(h);
            h.view = row;
            return h;
        }

        private static final class RowHolder {
            View view; ImageView icon; TextView fallback, title, subtitle; GradientDrawable fallbackShape;
        }

        /** Meaningful, text-independent accessibility label for a row's leading icon. */
        private String iconDescription(LiveFeedData.Entry e, long now) {
            if (e == null || e.kind == null) return "Result";
            if (e.kind == LiveFeedData.Kind.RAID)
                return e.raidHatchAt > now ? "Raid egg" : "Raid";
            return kindLabel(e.kind) + " " + displayName(e);
        }

        // ------------------------------------------------------------------ detail + actions

        /** One-tap selection, but never teleport without the existing confirmation. */
        private void confirmPokemonTeleport(LiveFeedData.Entry e) {
            if (closed || e == null || e.kind != LiveFeedData.Kind.POKEMON) return;
            if (e.expired(System.currentTimeMillis())) {
                toast("Este Pokémon não está mais disponível.");
                rebuildVisible(); adapter.notifyDataSetChanged(); return;
            }
            if (!CooldownState.valid(e.lat, e.lon)) {
                toast("A fonte não informou coordenadas válidas."); return;
            }
            AlertDialog confirmation = TeleportDialog.confirm(activity, e.lat, e.lon, (lat, lon) -> {
                if (closed || e.expired(System.currentTimeMillis())
                        || activity.isFinishing() || activity.isDestroyed()) return false;
                if (!Control.canEnable()) {
                    toast("O mecanismo de localização não está pronto."); return false;
                }
                Control.teleport(lat, lon);
                toast("Teleporte solicitado: " + displayName(e));
                dialog.dismiss();
                return true;
            });
            if (confirmation != null && !children.contains(confirmation)) children.add(confirmation);
        }

        /** Same user-confirmed location action as Pokémon results, but for raids. */
        private void confirmRaidTeleport(LiveFeedData.Entry e) {
            if (closed || e == null || e.kind != LiveFeedData.Kind.RAID) return;
            if (e.expiresAt <= System.currentTimeMillis()) {
                toast("Esta reide expirou.");
                rebuildVisible(); adapter.notifyDataSetChanged(); return;
            }
            if (!CooldownState.valid(e.lat, e.lon)) {
                toast("A fonte não informou coordenadas válidas."); return;
            }
            AlertDialog confirmation = TeleportDialog.confirm(activity, e.lat, e.lon, (lat, lon) -> {
                if (closed || e.expiresAt <= System.currentTimeMillis()
                        || activity.isFinishing() || activity.isDestroyed()) return false;
                if (!Control.canEnable()) {
                    toast("O mecanismo de localização não está pronto."); return false;
                }
                Control.teleport(lat, lon);
                toast("Teleporte solicitado para a reide: " + displayName(e));
                dialog.dismiss();
                return true;
            });
            if (confirmation != null && !children.contains(confirmation)) children.add(confirmation);
        }

        private void openDetails(LiveFeedData.Entry e) {
            if (closed) return;
            long now = System.currentTimeMillis();
            if (e.expired(now)) { toast("That result just expired."); rebuildVisible(); adapter.notifyDataSetChanged(); return; }
            LinearLayout body = new LinearLayout(ui);
            body.setOrientation(LinearLayout.VERTICAL);
            int pad = dp(18);
            body.setPadding(pad, dp(8), pad, 0);
            TextView info = label(detailText(e, now), 14);
            body.addView(info);
            LinearLayout actions = new LinearLayout(ui);
            actions.setOrientation(LinearLayout.HORIZONTAL);
            actions.setPadding(0, dp(14), 0, 0);
            Button map = button("Map"), walk = button("Walk here"), teleport = button("Teleport");
            actions.addView(map, new LinearLayout.LayoutParams(0, dp(48), 1));
            actions.addView(walk, new LinearLayout.LayoutParams(0, dp(48), 1));
            actions.addView(teleport, new LinearLayout.LayoutParams(0, dp(48), 1));
            body.addView(actions);
            AlertDialog detail = new AlertDialog.Builder(ui)
                .setTitle(dev.pgrazor.PtBr.tr(displayName(e)))
                .setView(body)
                .setNegativeButton(dev.pgrazor.PtBr.tr("Close"), null)
                .create();
            trackOwn(detail);
            detail.setOnShowListener(d -> {
                MapTheme.style(body); MapTheme.styleButtons(detail);
                map.setOnClickListener(v -> openMap(e, detail));
                walk.setOnClickListener(v -> applyMove(e, false, detail));
                teleport.setOnClickListener(v -> {
                    if(closed)return;
                    if(e.expired(System.currentTimeMillis())){toast("That result just expired.");return;}
                    if(!CooldownState.valid(e.lat,e.lon)){toast("That result has no valid location.");return;}
                    AlertDialog confirmation=TeleportDialog.confirm(activity,e.lat,e.lon,(lat,lon)->{
                        applyMove(e,true,detail);return true;
                    });
                    if(confirmation!=null&&!children.contains(confirmation))children.add(confirmation);
                });
            });
            detail.show();
        }

        /** Walk/Teleport only after a fresh validity, readiness and foreground check. */
        private void applyMove(LiveFeedData.Entry e, boolean teleport, AlertDialog detail) {
            if (closed) return;
            long now = System.currentTimeMillis();
            if (e.expired(now)) { toast("That result just expired."); detail.dismiss(); rebuildVisible(); adapter.notifyDataSetChanged(); return; }
            if (!Control.canEnable()) { toast("Location adapter is not ready."); return; }
            // Avoid hasWindowFocus() here: with this detail child open the game activity does not hold
            // window focus, so rely on it still being a live, non-finishing foreground activity.
            if (activity.isFinishing() || activity.isDestroyed()) return;
            if (!Double.isFinite(e.lat) || !Double.isFinite(e.lon)) { toast("That result has no valid location."); return; }
            if (teleport) Control.teleport(e.lat, e.lon); else Control.walkTo(e.lat, e.lon);
            toast(teleport ? "Teleported to the selected result." : "Walking to the selected result.");
            detail.dismiss();   // child
            dialog.dismiss();   // parent, after a confirmed action
        }

        private void openMap(LiveFeedData.Entry e, AlertDialog detail) {
            if (!Double.isFinite(e.lat) || !Double.isFinite(e.lon)) { toast("That result has no valid location."); return; }
            try {
                // Centred on the selected result, with both Teleport and Walk callbacks. We never push
                // this live data into Control.mapPoiSnapshot; the map loads its own game POIs.
                AlertDialog map = WorldMapDialog.show(activity, e.lat, e.lon,
                    (lat, lon) -> { if (!Control.canEnable()) { toast("Location adapter is not ready."); return false; } Control.teleport(lat, lon); return true; },
                    (lat, lon) -> { if (!Control.canEnable()) { toast("Location adapter is not ready."); return false; } Control.walkTo(lat, lon); return true; });
                if (map != null) children.add(map); // dismissed with this dialog on close()
            } catch (RuntimeException ex) {
                toast("Couldn't open the map for that result.");
                return;
            }
            detail.dismiss();
        }

        private void configureRocketMap() {
            if (closed) return;
            EditText address = new EditText(ui);
            address.setSingleLine(true);
            address.setHint("https://seu-servidor.com");
            address.setInputType(InputType.TYPE_CLASS_TEXT
                    | InputType.TYPE_TEXT_VARIATION_URI);
            address.setText(prefs.getString("rocketmap_https_url_v1", ""));
            LinearLayout form = new LinearLayout(ui);
            form.setOrientation(LinearLayout.VERTICAL);
            form.setPadding(dp(16), dp(8), dp(16), dp(8));
            form.addView(label("Informe a URL base de um RocketMap seu "
                    + "ou autorizado. HTTPS obrigatório; não informe usuário "
                    + "ou senha de Pokémon GO.", 13));
            form.addView(address, new LinearLayout.LayoutParams(-1, dp(52)));
            EditText readToken = new EditText(ui);
            readToken.setSingleLine(true);
            readToken.setHint("Chave de leitura do feed (opcional; somente nesta sessão)");
            readToken.setInputType(InputType.TYPE_CLASS_TEXT
                    | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            readToken.setText(rocketReadSecret);
            form.addView(label("Para um gateway PokeSearch protegido, informe a chave "
                    + "X-Feed-Secret emitida pelo seu servidor. Não use senha do jogo. "
                    + "A chave não será salva no aparelho.",12));
            form.addView(readToken, new LinearLayout.LayoutParams(-1, dp(52)));
            AlertDialog prompt = new AlertDialog.Builder(ui)
                    .setTitle("Servidor RocketMap")
                    .setView(form)
                    .setNegativeButton("Cancelar", null)
                    .setPositiveButton("Salvar", null).create();
            trackOwn(prompt);
            prompt.setOnShowListener(d -> {
                MapTheme.style(form);
                MapTheme.styleButtons(prompt);
                prompt.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                    String validated;
                    try { validated = RocketMapFeedClient.validateBaseUrl(
                            address.getText().toString()); }
                    catch (IllegalArgumentException ex) {
                        address.setError(ex.getMessage()); return;
                    }
                    ++generation;
                    rocketMap.cancel();
                    fetchInFlight = false;
                    main.removeCallbacks(deferredFetch);
                    prefs.edit().putString("rocketmap_https_url_v1", validated).apply();
                    rocketReadSecret = readToken.getText().toString().trim();
                    // Never persist the read secret: the operator enters it
                    // again after this screen is closed.
                    readToken.setText("");
                    rocketMapAccessDenied = false;
                    lastFetchStartedElapsed = 0;
                    backoffUntilElapsed = 0;
                    lastUpdated = 0;
                    lastError = null;
                    entries.clear(); visible.clear(); distCache.clear();
                    adapter.notifyDataSetChanged();
                    prompt.dismiss();
                    if (provider == 3) requestFetch();
                });
            });
            prompt.show();
        }

        private String radiusPreferenceKey() {
            // Provider-specific radius prevents an old 1/3 km LivePokeMap
            // preference from hiding iFlowGo's worldwide Pokémon page.
            if (provider == 2) return raidSearchMode
                    ? "iflowgo_raid_radius_v1" : "iflowgo_pokemon_radius_v1";
            return "radius";
        }

        private String sourceName() {
            if (provider == 1) return "PokeList / Moonani";
            if (provider == 2) return "iFlowGo";
            if (provider == 3) return "RocketMap (meu servidor)";
            return "LivePokeMap";
        }

        private String detailText(LiveFeedData.Entry e, long now) {
            StringBuilder sb = new StringBuilder();
            sb.append(kindLabel(e.kind)).append('\n');
            if (pokemonSearchMode || raidSearchMode)
                sb.append("Fonte: ").append(sourceName()).append('\n');
            String sub = displaySubtitle(e, now);
            if (sub != null && !sub.isEmpty()) sb.append(sub).append('\n');
            Double d = distCache.get(e);
            if (d != null) sb.append("Distance: ").append(formatDistance(d)).append('\n');
            if (Double.isFinite(e.lat) && Double.isFinite(e.lon))
                sb.append(String.format(Locale.US, "%.6f, %.6f\n", e.lat, e.lon));
            if (e.expiresAt > 0) {
                long remaining = e.expiresAt - now;
                sb.append(remaining > 0 ? "Expires in " + countdown(remaining) : "Expired").append('\n');
            }
            return sb.toString().trim();
        }

        private String displayName(LiveFeedData.Entry e) {
            String name = e.name == null || e.name.isEmpty() ? kindLabel(e.kind) : e.name;
            if (name.equals("#" + e.dexId) && speciesNames != null)
                return speciesNames.optString(Integer.toString(e.dexId), name);
            return name;
        }

        private String displaySubtitle(LiveFeedData.Entry e, long now) {
            String text = e.subtitle(now);
            if (e.dexId > 0 && speciesNames != null && e.kind != LiveFeedData.Kind.POKEMON) {
                String name = speciesNames.optString(Integer.toString(e.dexId), "");
                if (!name.isEmpty()) text = text.replace("#" + e.dexId, name);
            }
            return text;
        }

        // ------------------------------------------------------------------ child dialog tracking + teardown

        private void trackOwn(AlertDialog d) {
            children.add(d);
            // Keep the child's own dismissal/lifecycle cleanup intact.
            d.getWindow().getDecorView().addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
                public void onViewAttachedToWindow(View v) {}
                public void onViewDetachedFromWindow(View v) {
                    children.remove(d); v.removeOnAttachStateChangeListener(this);
                }
            });
        }

        private void toast(String message) {
            if (!closed) Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
        }

        void close() {
            if (closed) return;
            closed = true;
            generation++;
            main.removeCallbacksAndMessages(null);
            for (AlertDialog d : new ArrayList<>(children)) { try { d.dismiss(); } catch (RuntimeException ignored) {} }
            children.clear();
            try { client.cancel(); } catch (RuntimeException ignored) {} // disconnect any live connection off the UI
            try { pokeList.cancel(); } catch (RuntimeException ignored) {}
            try { iFlowGo.cancel(); } catch (RuntimeException ignored) {}
            try { rocketMap.cancel(); } catch (RuntimeException ignored) {}
            rocketReadSecret = "";
            net.shutdownNow();
        }

        // ------------------------------------------------------------------ view helpers

        private int dp(float v) { return Math.round(v * activity.getResources().getDisplayMetrics().density); }
        private TextView label(String text, int size) { TextView v = new TextView(ui); v.setText(dev.pgrazor.PtBr.tr(text)); v.setTextSize(size); return v; }
        private Button button(String text) { Button b = new Button(ui); b.setText(dev.pgrazor.PtBr.tr(text)); b.setAllCaps(false); b.setTextSize(13.5f); return b; }
        private Button segButton(String text) {
            Button b = new Button(ui); b.setText(dev.pgrazor.PtBr.tr(text)); b.setAllCaps(false); b.setTextSize(13);
            b.setPadding(dp(10), 0, dp(10), 0); b.setMinWidth(dp(52)); return b;
        }
    }

    // ---------------------------------------------------------------------- stateless helpers

    private static int clamp(int v, int lo, int hi) { return v < lo ? lo : (v > hi ? hi : v); }

    private static double distanceMeters(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371000.0;
        double dLat = Math.toRadians(lat2 - lat1), dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
            + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 2 * R * Math.asin(Math.min(1.0, Math.sqrt(a)));
    }

    private static String formatDistance(double meters) {
        if (!Double.isFinite(meters)) return "";
        if (meters < 1000) return Math.round(meters) + " m";
        return String.format(Locale.US, "%.1f km", meters / 1000);
    }

    private static String countdown(long millis) {
        long total = Math.max(0, millis) / 1000;
        return String.format(Locale.US, "%d:%02d", total / 60, total % 60);
    }

    private static String glyphLetter(LiveFeedData.Kind kind) {
        if (kind == null) return "?";
        switch (kind) {
            case POKEMON: return "P";
            case STOP: return "S";
            case GYM: return "G";
            case RAID: return "R";
            case STATION: return "W";
            default: return "?";
        }
    }

    private static int kindColor(LiveFeedData.Kind kind) {
        if (kind == null) return Color.rgb(120, 120, 120);
        switch (kind) {
            case POKEMON: return Color.rgb(46, 160, 100);
            case STOP: return Color.rgb(30, 120, 210);
            case GYM: return Color.rgb(223, 124, 35);
            case RAID: return Color.rgb(151, 65, 205);
            case STATION: return Color.rgb(0, 150, 160);
            default: return Color.rgb(120, 120, 120);
        }
    }

    private static String kindLabel(LiveFeedData.Kind kind) {
        if (kind == null) return "Result";
        switch (kind) {
            case POKEMON: return "Pokémon";
            case STOP: return "PokéStop";
            case GYM: return "Gym";
            case RAID: return "Raid";
            case STATION: return "Power Spot";
            default: return "Result";
        }
    }

    private static String loginError(LiveFeedClient.FeedException fe) {
        if (fe.status == 401) return "Incorrect username or password.";
        if (fe.status == 429) {
            long s = Math.max(fe.retryAfterMs, MIN_BACKOFF_MS) / 1000;
            return "Too many attempts. Wait " + s + "s and try again.";
        }
        return safeMessage(fe, "Sign-in failed. Please try again.");
    }

    private static String safeMessage(LiveFeedClient.FeedException fe, String fallback) {
        String m = fe.getMessage();
        return m != null && !m.isEmpty() ? m : fallback;
    }
}
