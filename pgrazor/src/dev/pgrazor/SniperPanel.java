package dev.pgrazor;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Native Sniper panel: Sniper 1 and Sniper 2 inside PGBR, sharing the search engine but never the
 * filters.
 *
 * <p>Everything the screen shows comes from {@link SpawnSearch.Result}: the row list, the counters
 * (received / removed by filter / shown) and a state message that names exactly what happened (source
 * empty, everything filtered locally, access denied, rate limited, coordinates withheld…). A result
 * list is never presented as "filtered" when no filter ran, and a single provider page is never called
 * a complete worldwide search.
 *
 * <p>Behaviour worth knowing before reading the code:
 * <ul>
 *   <li><b>One request at a time per slot.</b> A new search cancels the previous connector, and a late
 *       answer from a superseded request is discarded by generation counter (out-of-order safe).</li>
 *   <li><b>Debounce</b> on the text field only; no request is fired per keystroke.</li>
 *   <li><b>Filters persist</b> per slot; a refresh reuses the saved filters, and switching slots keeps
 *       each panel's own state.</li>
 *   <li><b>Teleport</b> always goes through {@link TeleportDialog#confirm} → {@link Control#teleport},
 *       after re-validating coordinates, freshness and expiry. Nothing is teleported automatically.</li>
 * </ul>
 */
public final class SniperPanel {
    private SniperPanel() {}

    /** Opens the panel on the requested slot. */
    public static void show(Activity activity, String slot) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;
        new Screen(activity, SpawnProfiles.SLOT_TWO.equals(slot) ? SpawnProfiles.SLOT_TWO
                : SpawnProfiles.SLOT_ONE).open();
    }

    private static final class Screen {
        private static final long MIN_REQUEST_GAP_MS = 15_000L;
        private static final long DEBOUNCE_MS = 400L;
        private static final long TICK_MS = 5_000L;
        /** A snapshot older than this is stale: coordinates are still shown, teleport is denied. */
        private static final long MAX_SNAPSHOT_AGE_MS = 10 * 60_000L;

        private final Activity activity;
        private final Context ui;
        private final String slot;
        private final Handler main = new Handler(Looper.getMainLooper());
        private final ExecutorService worker = Executors.newSingleThreadExecutor(task -> {
            Thread thread = new Thread(task, "pgbr-sniper-panel");
            thread.setDaemon(true);
            return thread;
        });
        private final SharedPreferences prefs;
        private final LiveFeedIcons icons;
        private final AtomicInteger generation = new AtomicInteger();

        private SpawnProfiles state;
        private SpawnSearch.Source source;
        private SpawnSearch.Result result;
        private final List<SpawnContract.Spawn> visible = new ArrayList<>();
        private final List<SpawnContract.Spawn> favourites = new ArrayList<>();

        private String search = "";
        private boolean fetching;
        private long lastRequestElapsed;
        private long blockedUntilElapsed;
        private String lastAttemptSource = "";
        private boolean closed;
        private int loadToken;
        private Runnable loadMore;

        private EditText searchField;
        private TextView status, counters, filterSummary, sourceLabel;
        private Button sniperOne, sniperTwo, refresh, filters, sourceButton, sortButton, profilesButton;
        private ArrayAdapter<SpawnContract.Spawn> adapter;
        private AlertDialog dialog;

        Screen(Activity activity, String slot) {
            this.activity = activity;
            this.ui = MapTheme.wrap(activity);
            this.slot = slot;
            this.prefs = AccountProfiles.preferences(activity, SpawnProfiles.PREF_FILE);
            this.icons = new LiveFeedIcons(activity);
            this.state = load();
        }

        // -------------------------------------------------------------- persistence

        private SpawnProfiles load() {
            try {
                String raw = prefs.getString(slot, null);
                if (raw == null || raw.trim().isEmpty()) return SpawnProfiles.defaults(slot);
                return SpawnProfiles.fromJson(slot, new JSONObject(raw));
            } catch (JSONException corrupt) {
                return SpawnProfiles.defaults(slot);
            }
        }

        private void save() {
            try {
                prefs.edit().putString(slot, state.toJson().toString()).apply();
            } catch (RuntimeException ignored) {
                // Persistence failing must never break the panel; the in-memory state stays valid.
            }
        }

        // -------------------------------------------------------------- layout

        void open() {
            LinearLayout root = new LinearLayout(ui);
            root.setOrientation(LinearLayout.VERTICAL);
            int pad = MapTheme.dp(ui, 14);
            root.setPadding(pad, MapTheme.dp(ui, 6), pad, MapTheme.dp(ui, 6));

            LinearLayout tabs = new LinearLayout(ui);
            tabs.setOrientation(LinearLayout.HORIZONTAL);
            sniperOne = tab("Sniper 1", SpawnProfiles.SLOT_ONE);
            sniperTwo = tab("Sniper 2", SpawnProfiles.SLOT_TWO);
            tabs.addView(sniperOne, new LinearLayout.LayoutParams(0, MapTheme.dp(ui, 44), 1));
            tabs.addView(sniperTwo, new LinearLayout.LayoutParams(0, MapTheme.dp(ui, 44), 1));
            root.addView(tabs);

            sourceLabel = new TextView(ui);
            sourceLabel.setTextSize(12);
            sourceLabel.setTextColor(MapTheme.muted());
            sourceLabel.setPadding(0, MapTheme.dp(ui, 6), 0, 0);
            root.addView(sourceLabel);

            searchField = new EditText(ui);
            searchField.setSingleLine(true);
            searchField.setTextSize(15);
            searchField.setHint("Nome ou nº da Pokédex (ex.: Pikachu, #25)");
            searchField.setText(search);
            searchField.addTextChangedListener(new TextWatcher() {
                public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
                public void onTextChanged(CharSequence s, int a, int b, int c) {}
                public void afterTextChanged(Editable s) {
                    search = s.toString().trim();
                    main.removeCallbacks(debounced);
                    main.postDelayed(debounced, DEBOUNCE_MS);
                }
            });
            root.addView(searchField, new LinearLayout.LayoutParams(-1, MapTheme.dp(ui, 46)));

            LinearLayout actions = new LinearLayout(ui);
            actions.setOrientation(LinearLayout.HORIZONTAL);
            filters = small("Filtros", v -> openFilters());
            refresh = small("Atualizar", v -> search(true));
            sourceButton = small("Fonte", v -> openSources());
            sortButton = small("Ordem", v -> openSort());
            actions.addView(filters, new LinearLayout.LayoutParams(0, MapTheme.dp(ui, 44), 1));
            actions.addView(refresh, new LinearLayout.LayoutParams(0, MapTheme.dp(ui, 44), 1));
            actions.addView(sourceButton, new LinearLayout.LayoutParams(0, MapTheme.dp(ui, 44), 1));
            actions.addView(sortButton, new LinearLayout.LayoutParams(0, MapTheme.dp(ui, 44), 1));
            root.addView(actions);

            filterSummary = new TextView(ui);
            filterSummary.setTextSize(12);
            filterSummary.setTextColor(MapTheme.muted());
            filterSummary.setPadding(0, MapTheme.dp(ui, 4), 0, 0);
            root.addView(filterSummary);

            counters = new TextView(ui);
            counters.setTextSize(12);
            counters.setTextColor(MapTheme.ink());
            counters.setTypeface(null, Typeface.BOLD);
            root.addView(counters);

            status = new TextView(ui);
            status.setTextSize(12);
            status.setTextColor(MapTheme.muted());
            status.setPadding(0, MapTheme.dp(ui, 2), 0, MapTheme.dp(ui, 4));
            root.addView(status);

            adapter = new ArrayAdapter<SpawnContract.Spawn>(ui, android.R.layout.simple_list_item_1, visible) {
                @Override public View getView(int position, View convertView, ViewGroup parent) {
                    return rowView(getItem(position));
                }
            };
            ListView list = new ListView(ui);
            list.setAdapter(adapter);
            list.setOnItemClickListener((parent, view, position, id) -> {
                if (position >= 0 && position < visible.size()) details(visible.get(position));
            });
            root.addView(list, new LinearLayout.LayoutParams(-1, 0, 1));

            profilesButton = small("Perfis e preferências", v -> openProfiles());
            root.addView(profilesButton, new LinearLayout.LayoutParams(-1, MapTheme.dp(ui, 44)));

            dialog = new AlertDialog.Builder(ui)
                    .setTitle("Busca de Pokémon · " + title())
                    .setView(new ScrollView(ui) {{ addView(root); }})
                    .setNegativeButton("Fechar", null)
                    .create();
            DialogLifecycle.bind(activity, dialog, this::close);
            dialog.setOnShowListener(d -> {
                MapTheme.style(root);
                MapTheme.styleButtons(dialog);
                MapTheme.fullScreen(dialog);
                render();
                main.postDelayed(tick, TICK_MS);
            });
            dialog.show();
        }

        private Button tab(String label, String target) {
            Button button = new Button(ui);
            button.setText(label);
            button.setTextSize(14);
            button.setOnClickListener(v -> {
                if (slot.equals(target)) return;
                save();
                dialog.dismiss();
                SniperPanel.show(activity, target);
            });
            return button;
        }

        private Button small(String label, View.OnClickListener click) {
            Button button = new Button(ui);
            button.setText(label);
            button.setTextSize(12);
            button.setAllCaps(false);
            button.setOnClickListener(click);
            return button;
        }

        private String title() {
            return SpawnProfiles.SLOT_ONE.equals(slot) ? "Sniper 1" : "Sniper 2";
        }

        // -------------------------------------------------------------- rendering

        private void render() {
            if (closed || dialog == null) return;
            sniperOne.setTypeface(null, SpawnProfiles.SLOT_ONE.equals(slot) ? Typeface.BOLD : Typeface.NORMAL);
            sniperTwo.setTypeface(null, SpawnProfiles.SLOT_TWO.equals(slot) ? Typeface.BOLD : Typeface.NORMAL);

            SpawnSourceRegistry.SourceInfo info = SpawnSourceRegistry.find(state.sourceId());
            sourceLabel.setText("Fonte: " + (info == null ? state.sourceId() : info.label)
                    + (info != null && info.status == SpawnSourceRegistry.Status.NEEDS_CONFIGURATION
                    ? " · requer configuração" : ""));
            filterSummary.setText("Filtros ativos: " + state.filters().summary());
            sourceButton.setText("Fonte");
            sortButton.setText("Ordem: " + orderLabel(state.order()));
            refresh.setText(fetching ? "Buscando…" : "Atualizar");

            List<SpawnContract.Spawn> matched = matches();
            visible.clear();
            visible.addAll(matched);
            adapter.notifyDataSetChanged();

            if (fetching) {
                counters.setText("Buscando na fonte…");
                status.setText("Consulta em andamento. Nada é exibido como resultado até a fonte responder.");
                return;
            }
            if (result == null) {
                counters.setText("Nenhuma consulta realizada");
                status.setText("Toque Atualizar para consultar a fonte selecionada. "
                        + "Nenhum dado é inventado quando não há consulta.");
                return;
            }
            counters.setText("Recebidos " + result.received + " · removidos por filtro "
                    + result.droppedByFilter + " · exibidos " + visible.size());
            StringBuilder line = new StringBuilder(result.message);
            if (visible.size() != result.count()) {
                line.append("\nBusca por nome/nº aplicada sobre os ")
                        .append(result.count()).append(" resultado(s) já filtrados.");
            }
            if (result.state == SpawnSearch.State.OK && result.partialCoverage()) {
                line.append("\nCobertura parcial: não é uma busca mundial completa.");
            }
            if (isStale()) line.append("\nDados antigos: atualize antes de teleportar.");
            status.setText(line.toString());
        }

        private boolean isStale() {
            return result != null && result.fetchedAt > 0
                    && System.currentTimeMillis() - result.fetchedAt > MAX_SNAPSHOT_AGE_MS;
        }

        private String orderLabel(SpawnContract.Order order) {
            switch (order) {
                case TIME_LEFT: return "tempo";
                case IV: return "IV";
                case SPECIES: return "espécie";
                case PVP_RANK: return "PvP";
                case DISTANCE:
                default: return "distância";
            }
        }

        /** Local text filter over the already-filtered result set (name or dex), never a new request. */
        private List<SpawnContract.Spawn> matches() {
            if (result == null) return Collections.emptyList();
            List<SpawnContract.Spawn> out = new ArrayList<>();
            for (SpawnContract.Spawn spawn : result.visible) {
                if (PokemonFeedSearch.matches(search, spawn.pokemonId, spawn.sourceId,
                        spawn.pokemonName)) out.add(spawn);
            }
            return out;
        }

        private View rowView(final SpawnContract.Spawn spawn) {
            LinearLayout row = new LinearLayout(ui);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(MapTheme.dp(ui, 2), MapTheme.dp(ui, 6), MapTheme.dp(ui, 2), MapTheme.dp(ui, 6));
            if (spawn == null) return row;

            ImageView sprite = new ImageView(ui);
            Bitmap bitmap = spawn.live == null ? null : icons.iconFor(spawn.live, System.currentTimeMillis());
            if (bitmap != null) sprite.setImageBitmap(bitmap);
            row.addView(sprite, new LinearLayout.LayoutParams(MapTheme.dp(ui, 42), MapTheme.dp(ui, 42)));

            TextView text = new TextView(ui);
            text.setTextColor(MapTheme.ink());
            text.setTextSize(13);
            text.setPadding(MapTheme.dp(ui, 8), 0, 0, 0);
            text.setText(label(spawn));
            row.addView(text, new LinearLayout.LayoutParams(0, -2, 1));

            TextView fav = new TextView(ui);
            fav.setText(isFavourite(spawn) ? "★" : "☆");
            fav.setTextSize(20);
            fav.setTextColor(MapTheme.accent());
            fav.setPadding(MapTheme.dp(ui, 6), 0, MapTheme.dp(ui, 6), 0);
            fav.setOnClickListener(v -> toggleFavourite(spawn));
            row.addView(fav);
            return row;
        }

        /** Multi-line row text: every value is labelled and unknown values are simply omitted. */
        private String label(SpawnContract.Spawn s) {
            long now = System.currentTimeMillis();
            StringBuilder out = new StringBuilder("#").append(s.pokemonId);
            if (!s.pokemonName.isEmpty()) out.append(" ").append(s.pokemonName);
            out.append("\n");
            boolean wrote = false;
            if (s.perfect()) {
                out.append("100% IV 15/15/15");
                wrote = true;
            } else if (s.hasIvComponents() || s.ivPercent != SpawnContract.UNKNOWN) {
                out.append("IV ").append(SpawnFilters.percentOf(s)).append('%');
                if (s.hasIvComponents())
                    out.append(" (").append(s.ivAttack).append('/').append(s.ivDefense)
                            .append('/').append(s.ivStamina).append(')');
                wrote = true;
            } else {
                out.append("IV não informado");
            }
            if (s.cp != SpawnContract.UNKNOWN) out.append(" · PC ").append(s.cp);
            if (Double.isFinite(s.level))
                out.append(" · Nv ").append(String.format(Locale.US, "%.1f", s.level));
            if (s.pvpRank > 0) out.append(" · PvP ").append(SpawnFilters.leagueName(s.pvpLeague))
                    .append(" #").append(s.pvpRank).append(s.pvpRankLocal ? " (calculado)" : "");
            if (s.shinyConfirmed) out.append(" · shiny confirmado");
            else if (s.shinyPossible) out.append(" · shiny possível");

            out.append("\n");
            double km = s.distanceKmFrom(Control.currentLatitude(), Control.currentLongitude());
            if (Double.isFinite(km)) out.append(km < 1 ? (Math.round(km * 1000) + " m")
                    : String.format(Locale.US, "%.1f km", km));
            else out.append("distância indisponível");
            long remaining = s.remainingMs(now);
            if (remaining != SpawnContract.UNKNOWN_TIME)
                out.append(" · ").append(Math.max(0, remaining / 1000)).append("s restantes");
            else out.append(" · sumiço não informado");
            out.append("\nFonte: ").append(sourceName(s.sourceId));
            if (wrote) out.append(" · dados da fonte");
            if (s.coordinatesStatus != SpawnContract.Coordinates.AVAILABLE)
                out.append("\nSem coordenadas reveladas");
            return out.toString();
        }

        private String sourceName(String id) {
            SpawnSourceRegistry.SourceInfo info = SpawnSourceRegistry.find(id);
            return info == null ? id : info.label;
        }

        private boolean isFavourite(SpawnContract.Spawn spawn) {
            for (SpawnContract.Spawn fav : favourites)
                if (fav.dedupeKey().equals(spawn.dedupeKey())) return true;
            return false;
        }

        private void toggleFavourite(SpawnContract.Spawn spawn) {
            for (SpawnContract.Spawn fav : new ArrayList<>(favourites)) {
                if (fav.dedupeKey().equals(spawn.dedupeKey())) {
                    favourites.remove(fav);
                    Toast.makeText(activity, "Removido dos favoritos.", Toast.LENGTH_SHORT).show();
                    render();
                    return;
                }
            }
            favourites.add(spawn);
            Toast.makeText(activity, "Adicionado aos favoritos desta sessão.", Toast.LENGTH_SHORT).show();
            render();
        }

        /** Local 5-second tick: drops rows that expired while the panel is open. No network. */
        private final Runnable tick = new Runnable() { public void run() { tickOnce(); } };

        private void tickOnce() {
            if (closed) return;
            if (result != null) {
                // Drop rows that expired while the panel is open; the counters always say so.
                boolean changed = false;
                long now = System.currentTimeMillis();
                for (SpawnContract.Spawn spawn : new ArrayList<>(visible)) {
                    if (spawn.expired(now)) {
                        visible.remove(spawn);
                        changed = true;
                    }
                }
                if (changed) {
                    adapter.notifyDataSetChanged();
                    counters.setText("Recebidos " + result.received + " · removidos por filtro "
                            + result.droppedByFilter + " · exibidos " + visible.size()
                            + " (linhas expiradas foram retiradas)");
                }
            }
            main.postDelayed(tick, TICK_MS);
        }

        /** Debounced once the user pauses typing; re-renders the local list only. */
        private final Runnable debounced = new Runnable() { public void run() { render(); } };


        // -------------------------------------------------------------- search

        private void search(boolean manual) {
            if (closed || fetching) return;
            long elapsed = SystemClock.elapsedRealtime();
            if (elapsed < blockedUntilElapsed && lastAttemptSource.equals(state.sourceId())) {
                status.setText("Esta fonte recusou ou limitou a última consulta. Aguarde antes de tentar de novo.");
                return;
            }
            // Guard against a double tap and against hammering the vendor.
            if (manual && lastRequestElapsed > 0 && elapsed - lastRequestElapsed < MIN_REQUEST_GAP_MS
                    && lastAttemptSource.equals(state.sourceId())) {
                status.setText("Aguarde " + ((MIN_REQUEST_GAP_MS - (elapsed - lastRequestElapsed)) / 1000)
                        + "s antes de consultar esta fonte de novo.");
                return;
            }

            SpawnSourceRegistry.SourceInfo info = SpawnSourceRegistry.find(state.sourceId());
            if (info == null || !info.selectable()) {
                status.setText("Esta fonte não está disponível para consulta direta: "
                        + (info == null ? "fonte desconhecida." : info.detail));
                return;
            }
            if (source != null) source.cancel();
            final int token = generation.incrementAndGet();
            final SpawnSearch.Source connector = SpawnConnectors.create(state.sourceId(), rocketBaseUrl());
            if (connector == null) {
                status.setText("Fonte sem conector disponível nesta versão: " + info.label + ".");
                return;
            }
            source = connector;
            fetching = true;
            loadToken = token;
            lastRequestElapsed = elapsed;
            lastAttemptSource = state.sourceId();
            final SpawnSearch.Request request = request();
            final SpawnFilters filters = state.filters();
            final SpawnContract.Order order = state.order();
            final double originLat = Control.currentLatitude();
            final double originLon = Control.currentLongitude();
            render();

            worker.execute(() -> {
                SpawnSearch.Result outcome;
                try {
                    outcome = SpawnSearch.run(connector, request,
                            (spawn, at) -> filters.accepts(spawn, at, originLat, originLon),
                            order, originLat, originLon, System.currentTimeMillis());
                } catch (Throwable unexpected) {
                    outcome = SpawnSearch.failure(connector, state.sourceId(), new Exception(unexpected));
                }
                final SpawnSearch.Result finalOutcome = outcome;
                main.post(() -> {
                    if (closed) return;
                    // Out-of-order guard: only the newest request may publish.
                    if (token != generation.get() || token != loadToken) return;
                    fetching = false;
                    result = finalOutcome;
                    if (finalOutcome.state == SpawnSearch.State.ACCESS_DENIED
                            || finalOutcome.state == SpawnSearch.State.RATE_LIMITED) {
                        long wait = finalOutcome.retryAfterMs > 0 ? finalOutcome.retryAfterMs
                                : (finalOutcome.state == SpawnSearch.State.RATE_LIMITED ? 60_000L : 30_000L);
                        blockedUntilElapsed = SystemClock.elapsedRealtime() + Math.max(1_000L, wait);
                    }
                    if (finalOutcome.state == SpawnSearch.State.ACCESS_DENIED) {
                        // Do not keep hammering an endpoint that refused us.
                        Toast.makeText(activity, "A fonte recusou o acesso.", Toast.LENGTH_SHORT).show();
                    }
                    render();
                });
            });
        }

        private String rocketBaseUrl() {
            try {
                SharedPreferences feed = AccountProfiles.preferences(activity, "pgrazor-livefeed");
                return feed.getString("rocketmap_https_url_v1", "");
            } catch (RuntimeException missing) {
                return "";
            }
        }

        /**
         * Builds the request from the slot's own saved state. Raids are not requested here: this engine
         * searches spawns, and {@code raidsOnly} stays false so a raid row can never be mistaken for a
         * Pokémon.
         */
        private SpawnSearch.Request request() {
            SpawnFilters filters = state.filters();
            return new SpawnSearch.Request(state.sourceId(), filters.remoteSpecies(), state.global(),
                    Control.currentLatitude(), Control.currentLongitude(), state.radiusKm(), false);
        }

        // -------------------------------------------------------------- dialogs

        private void openFilters() {
            SnipeAdvancedFilterDialog.show(activity, title(), state.filters(), state.sourceId(), updated -> {
                state = state.withFilters(updated);
                save();
                render();
                // Re-run with the new filters so the counters show the new split immediately.
                if (result != null) search(false);
            });
        }

        private void openSources() {
            List<SpawnSourceRegistry.SourceInfo> all = SpawnSourceRegistry.all();
            final List<SpawnSourceRegistry.SourceInfo> selectable = new ArrayList<>();
            List<String> labels = new ArrayList<>();
            for (SpawnSourceRegistry.SourceInfo info : all) {
                if (info.status == SpawnSourceRegistry.Status.NOT_INTEGRATED) continue;
                selectable.add(info);
                labels.add(info.label + (info.selectable() ? "" : " · indisponível"));
            }
            AlertDialog.Builder builder = new AlertDialog.Builder(ui).setTitle("Fonte de dados");
            builder.setItems(labels.toArray(new String[0]), (d, which) -> {
                SpawnSourceRegistry.SourceInfo info = selectable.get(which);
                if (!info.selectable()) {
                    AlertDialog explained = new AlertDialog.Builder(ui).setTitle(info.label)
                            .setMessage(info.detail).setPositiveButton("Entendi", null).create();
                    DialogLifecycle.bind(activity, explained, null);
                    explained.setOnShowListener(shown -> MapTheme.styleButtons(explained));
                    explained.show();
                    return;
                }
                state = state.withSource(info.id);
                save();
                if (info.status == SpawnSourceRegistry.Status.NEEDS_CONFIGURATION) {
                    Toast.makeText(activity, "Configure o endereço HTTPS em Filtro Pokémon.",
                            Toast.LENGTH_LONG).show();
                }
                render();
            });
            AlertDialog picker = builder.setNegativeButton("Cancelar", null).create();
            DialogLifecycle.bind(activity, picker, null);
            picker.setOnShowListener(d -> MapTheme.styleButtons(picker));
            picker.show();
        }

        private void openSort() {
            String[] labels = {"Distância", "Tempo restante", "IV", "Espécie", "Ranking PvP"};
            SpawnContract.Order[] orders = {SpawnContract.Order.DISTANCE, SpawnContract.Order.TIME_LEFT,
                    SpawnContract.Order.IV, SpawnContract.Order.SPECIES, SpawnContract.Order.PVP_RANK};
            AlertDialog sorter = new AlertDialog.Builder(ui).setTitle("Ordenar por")
                    .setItems(labels, (d, which) -> {
                        state = state.withOrder(orders[which]);
                        save();
                        render();
                    }).setNegativeButton("Cancelar", null).create();
            DialogLifecycle.bind(activity, sorter, null);
            sorter.setOnShowListener(d -> MapTheme.styleButtons(sorter));
            sorter.show();
        }

        private void openProfiles() {
            LinearLayout root = new LinearLayout(ui);
            root.setOrientation(LinearLayout.VERTICAL);
            int pad = MapTheme.dp(ui, 12);
            root.setPadding(pad, pad, pad, pad);

            TextView title = new TextView(ui);
            title.setTextColor(MapTheme.ink());
            title.setTextSize(13);
            title.setText("Perfis salvos do " + title() + " (independentes do outro Sniper)");
            root.addView(title);

            for (final String name : state.profileNames()) {
                LinearLayout line = new LinearLayout(ui);
                line.setOrientation(LinearLayout.HORIZONTAL);
                Button apply = small(name, v -> {
                    state = state.applyProfile(name);
                    save();
                    dialog.dismiss();
                    SniperPanel.show(activity, slot);
                });
                Button remove = small("Excluir", v -> {
                    state = state.deleteProfile(name);
                    save();
                    dialog.dismiss();
                    SniperPanel.show(activity, slot);
                });
                line.addView(apply, new LinearLayout.LayoutParams(0, MapTheme.dp(ui, 42), 2));
                line.addView(remove, new LinearLayout.LayoutParams(0, MapTheme.dp(ui, 42), 1));
                root.addView(line);
            }
            if (state.profileNames().isEmpty()) {
                TextView empty = new TextView(ui);
                empty.setTextColor(MapTheme.muted());
                empty.setTextSize(12);
                empty.setText("Nenhum perfil salvo ainda.");
                root.addView(empty);
            }

            final EditText name = new EditText(ui);
            name.setHint("Nome do novo perfil");
            name.setSingleLine(true);
            root.addView(name);

            Button saveProfile = small("Salvar filtros atuais como perfil", v -> {
                String clean = SpawnProfiles.cleanName(name.getText().toString());
                if (clean == null) {
                    Toast.makeText(activity, "Informe um nome de até "
                            + SpawnProfiles.MAX_NAME + " caracteres.", Toast.LENGTH_SHORT).show();
                    return;
                }
                state = state.saveProfile(clean, state.filters());
                save();
                dialog.dismiss();
                SniperPanel.show(activity, slot);
            });
            root.addView(saveProfile, new LinearLayout.LayoutParams(-1, MapTheme.dp(ui, 42)));

            Button clear = small("Limpar filtros", v -> {
                state = state.clearFilters();
                save();
                dialog.dismiss();
                SniperPanel.show(activity, slot);
            });
            root.addView(clear, new LinearLayout.LayoutParams(-1, MapTheme.dp(ui, 42)));

            LinearLayout prefsRow = new LinearLayout(ui);
            prefsRow.setOrientation(LinearLayout.HORIZONTAL);
            prefsRow.addView(small(state.global() ? "Escopo: mundial" : "Escopo: raio",
                    v -> {
                        state = state.withGlobal(!state.global());
                        save();
                        dialog.dismiss();
                        SniperPanel.show(activity, slot);
                    }), new LinearLayout.LayoutParams(0, MapTheme.dp(ui, 42), 1));
            prefsRow.addView(small("Raio " + (int) state.radiusKm() + " km", v -> pickRadius()),
                    new LinearLayout.LayoutParams(0, MapTheme.dp(ui, 42), 1));
            root.addView(prefsRow);

            Button auto = small("Atualização automática: " + autoLabel(), v -> pickAutoRefresh());
            root.addView(auto, new LinearLayout.LayoutParams(-1, MapTheme.dp(ui, 42)));

            TextView warning = new TextView(ui);
            warning.setTextSize(11);
            warning.setTextColor(MapTheme.muted());
            warning.setText("Teleporte e busca alteram sua localização no jogo; use por sua conta e risco "
                    + "e respeite os termos do fornecedor e as regras do jogo.");
            root.addView(warning);

            AlertDialog sheet = new AlertDialog.Builder(ui).setTitle("Perfis e preferências")
                    .setView(new ScrollView(ui) {{ addView(root); }})
                    .setNegativeButton("Fechar", null).create();
            DialogLifecycle.bind(activity, sheet, null);
            sheet.setOnShowListener(d -> {
                MapTheme.style(root);
                MapTheme.styleButtons(sheet);
            });
            sheet.show();
        }

        private String autoLabel() {
            int seconds = state.autoRefreshSeconds();
            return seconds == SpawnProfiles.AUTO_OFF ? "desligada" : (seconds + "s");
        }

        private void pickRadius() {
            final int[] choices = {1, 5, 10, 25, 50};
            String[] labels = new String[choices.length];
            for (int i = 0; i < choices.length; i++) labels[i] = choices[i] + " km";
            AlertDialog picker = new AlertDialog.Builder(ui).setTitle("Raio da busca")
                    .setItems(labels, (d, which) -> {
                        state = state.withRadius(choices[which]);
                        save();
                        dialog.dismiss();
                        SniperPanel.show(activity, slot);
                    }).setNegativeButton("Cancelar", null).create();
            DialogLifecycle.bind(activity, picker, null);
            picker.setOnShowListener(d -> MapTheme.styleButtons(picker));
            picker.show();
        }

        private void pickAutoRefresh() {
            String[] labels = new String[SpawnProfiles.AUTO_CHOICES.length];
            for (int i = 0; i < labels.length; i++) {
                int seconds = SpawnProfiles.AUTO_CHOICES[i];
                labels[i] = seconds == SpawnProfiles.AUTO_OFF ? "Desligada (só manual)"
                        : "A cada " + seconds + "s";
            }
            AlertDialog picker = new AlertDialog.Builder(ui).setTitle("Atualização automática")
                    .setItems(labels, (d, which) -> {
                        state = state.withAutoRefresh(SpawnProfiles.AUTO_CHOICES[which]);
                        save();
                        dialog.dismiss();
                        SniperPanel.show(activity, slot);
                    }).setNegativeButton("Cancelar", null).create();
            DialogLifecycle.bind(activity, picker, null);
            picker.setOnShowListener(d -> MapTheme.styleButtons(picker));
            picker.show();
        }

        /** Row detail: real values, provenance, freshness, copy, map and confirmed teleport. */
        private void details(final SpawnContract.Spawn spawn) {
            long now = System.currentTimeMillis();
            double km = spawn.distanceKmFrom(Control.currentLatitude(), Control.currentLongitude());
            long remaining = spawn.remainingMs(now);
            StringBuilder message = new StringBuilder();
            message.append("#").append(spawn.pokemonId);
            if (!spawn.pokemonName.isEmpty()) message.append(" ").append(spawn.pokemonName);
            message.append("\nFonte: ").append(sourceName(spawn.sourceId));
            message.append("\nIV: ");
            if (spawn.perfect()) message.append("100% comprovado (15/15/15)");
            else if (spawn.hasIvComponents()) message.append(spawn.ivAttack).append('/')
                    .append(spawn.ivDefense).append('/').append(spawn.ivStamina)
                    .append(" (").append(SpawnFilters.percentOf(spawn)).append("%)");
            else if (spawn.ivPercent != SpawnContract.UNKNOWN) message.append(spawn.ivPercent)
                    .append("% (informado pela fonte)");
            else message.append("não informado");
            if (spawn.cp != SpawnContract.UNKNOWN) message.append("\nPC: ").append(spawn.cp);
            if (Double.isFinite(spawn.level))
                message.append("\nNível: ").append(String.format(Locale.US, "%.1f", spawn.level));
            if (spawn.pvpRank > 0) message.append("\nPvP: ").append(SpawnFilters.leagueName(spawn.pvpLeague))
                    .append(" #").append(spawn.pvpRank)
                    .append(spawn.pvpRankLocal ? " (calculado localmente)" : " (informado pela fonte)");
            message.append(spawn.shinyConfirmed ? "\nShiny: confirmado pela fonte"
                    : spawn.shinyPossible ? "\nShiny: possível (não confirmado)" : "");
            message.append("\nCoordenadas: ").append(spawn.hasLocation()
                    ? String.format(Locale.US, "%.6f, %.6f", spawn.latitude, spawn.longitude)
                    : "não reveladas por esta fonte");
            if (Double.isFinite(km)) message.append("\nDistância: ")
                    .append(km < 1 ? Math.round(km * 1000) + " m" : String.format(Locale.US, "%.1f km", km));
            message.append("\nRestante: ").append(remaining == SpawnContract.UNKNOWN_TIME
                    ? "não informado" : (Math.max(0, remaining / 1000) + "s"));
            if (isStale()) message.append("\n\nDados antigos: atualize antes de usar.");
            message.append("\n\nTeleporte altera sua posição no jogo, respeita o cooldown e só ocorre "
                    + "após confirmação. O PGBR não contorna limites da fonte.");

            final boolean usable = spawn.hasLocation() && !spawn.expired(now) && !isStale();
            AlertDialog.Builder builder = new AlertDialog.Builder(ui)
                    .setTitle("Detalhes · " + sourceName(spawn.sourceId))
                    .setMessage(message.toString())
                    .setNeutralButton("Copiar coords", (d, w) -> {
                        if (!spawn.hasLocation()) {
                            Toast.makeText(activity, "Sem coordenadas para copiar.", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        CoordinateClipboard.copy(activity, spawn.latitude, spawn.longitude,
                                "Coordenadas copiadas");
                    })
                    .setNegativeButton("Mapa", (d, w) -> {
                        if (!spawn.hasLocation()) {
                            Toast.makeText(activity, "Sem coordenadas para abrir no mapa.",
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }
                        WorldMapDialog.show(activity, spawn.latitude, spawn.longitude,
                                (lat, lon) -> confirmTeleport(spawn), (lat, lon) -> { });
                    })
                    .setPositiveButton("Teleporte", (d, w) -> {
                        if (!usable) {
                            Toast.makeText(activity, !spawn.hasLocation()
                                    ? "Esta linha não tem coordenadas reveladas."
                                    : "Esta linha expirou ou os dados estão antigos.", Toast.LENGTH_LONG).show();
                            return;
                        }
                        confirmTeleport(spawn);
                    });
            AlertDialog detail = builder.create();
            DialogLifecycle.bind(activity, detail, null);
            detail.setOnShowListener(d -> MapTheme.styleButtons(detail));
            detail.show();
        }

        private void confirmTeleport(final SpawnContract.Spawn spawn) {
            if (!spawn.hasLocation() || !SpawnContract.validCoordinates(spawn.latitude, spawn.longitude)) {
                Toast.makeText(activity, "Coordenadas inválidas: teleporte bloqueado.", Toast.LENGTH_LONG).show();
                return;
            }
            if (spawn.expired(System.currentTimeMillis()) || isStale()) {
                Toast.makeText(activity, "Aparição expirada ou dados antigos: atualize antes.",
                        Toast.LENGTH_LONG).show();
                return;
            }
            TeleportDialog.confirm(activity, spawn.latitude, spawn.longitude, (lat, lon) -> {
                // Re-validate at the moment of the click: the dialog may have stayed open for a while.
                if (closed || activity.isFinishing() || activity.isDestroyed()
                        || spawn.expired(System.currentTimeMillis()) || isStale()
                        || !SpawnContract.validCoordinates(lat, lon)) return false;
                if (!Control.canEnable()) {
                    Toast.makeText(activity, "Localização indisponível no PGBR.", Toast.LENGTH_SHORT).show();
                    return false;
                }
                Control.teleport(lat, lon);
                Toast.makeText(activity, "Teleporte solicitado; confirme a posição no jogo.",
                        Toast.LENGTH_SHORT).show();
                render();
                return true;
            });
        }

        private void close() {
            closed = true;
            generation.incrementAndGet();
            if (source != null) source.cancel();
            worker.shutdownNow();
            main.removeCallbacksAndMessages(null);
        }
    }
}
