package dev.pgrazor;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONException;

/**
 * Radar of the five integrations. The tabs, the list and the coordinate actions all read the same
 * normalised records, so the counts on screen are the counts in the list.
 *
 * <p>Every state is honest: a source that answered nothing says so, a refused source says it was
 * refused (and is never replaced by another provider's rows), an unsupported filter is named, and a
 * row without released coordinates cannot be copied or teleported.
 */
public final class SourceRadarDialog {
    private SourceRadarDialog() {}

    static final SourceEvent.Kind[] TABS = {
        SourceEvent.Kind.POKEMON, SourceEvent.Kind.RAID, SourceEvent.Kind.QUEST,
        SourceEvent.Kind.ROCKET, SourceEvent.Kind.POWERSPOT, SourceEvent.Kind.NEST,
        SourceEvent.Kind.ROUTE};

    static final String[] TAB_LABELS = {
        "Pokémon", "Reides", "Pesquisas", "Rocket", "Power Spots", "Ninhos", "Rotas"};

    static final String[] SOURCES = {
        "import", SourceRegistry.PGBR_HUNDO, SourceRegistry.POKELIST, SourceRegistry.IFLOWGO,
        SourceRegistry.POGO_COORDINATES, SourceRegistry.PGSHARP, SourceRegistry.IPOGO};

    static final String[] SOURCE_LABELS = {
        "Dados importados (local)", "Feed 100% IV do PGBR", "PokeList / Moonani", "iFlowGo",
        "PoGoCoordinates", "PGSharp (dados próprios)", "iPogo (dados próprios)"};

    public static void show(Activity activity) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;
        Context ui = MapTheme.wrap(activity);
        LinearLayout root = new LinearLayout(ui);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = MapTheme.dp(ui, 12);
        root.setPadding(pad, pad, pad, pad);
        AlertDialog dialog = new AlertDialog.Builder(ui)
                .setTitle("Radar PGBR · 5 fontes")
                .setView(root)
                .setNegativeButton("Fechar", null)
                .create();
        Screen screen = new Screen(activity, ui, dialog, root);
        DialogLifecycle.bind(activity, dialog, screen::close);
        dialog.setOnShowListener(ignored -> {
            MapTheme.style(root);
            MapTheme.styleButtons(dialog);
            screen.search(null);
        });
        dialog.show();
    }

    private static final class Screen {
        private final Activity activity;
        private final Context ui;
        private final AlertDialog dialog;
        private final Handler main = new Handler(Looper.getMainLooper());
        private final ExecutorService worker = Executors.newSingleThreadExecutor(task -> {
            Thread thread = new Thread(task, "pgbr-radar");
            thread.setDaemon(true);
            return thread;
        });
        private final LocalEventStore store;
        private final LiveFeedIcons icons;
        private final List<SniperSearch.Result> visible = new ArrayList<>();
        private final ArrayAdapter<SniperSearch.Result> adapter;
        private final TextView status;
        private final TextView coverage;
        private final TextView alerts;
        private final Button filterButton;
        private final Button importButton;
        private final Button pageButton;
        private final Spinner source;
        private final LinearLayout tabs;
        private SourceEvent.Kind tab = SourceEvent.Kind.POKEMON;
        private SourceQuery query = SourceQuery.builder().build();
        private SourceSearchResult lastResult;
        private String cursor;
        private volatile boolean closed;
        private boolean busy;
        private int selectedSource = 0;

        Screen(Activity activity, Context ui, AlertDialog dialog, LinearLayout root) {
            this.activity = activity;
            this.ui = ui;
            this.dialog = dialog;
            this.store = SourceDataStore.events(activity, System.currentTimeMillis());
            this.icons = new LiveFeedIcons(activity);

            tabs = new LinearLayout(ui);
            tabs.setOrientation(LinearLayout.HORIZONTAL);
            HorizontalScrollView tabScroll = new HorizontalScrollView(ui);
            tabScroll.setHorizontalScrollBarEnabled(false);
            tabScroll.addView(tabs);
            root.addView(tabScroll, new LinearLayout.LayoutParams(-1, -2));

            LinearLayout controls = new LinearLayout(ui);
            controls.setOrientation(LinearLayout.HORIZONTAL);
            controls.setGravity(Gravity.CENTER_VERTICAL);
            source = new Spinner(ui);
            ArrayAdapter<String> sourceAdapter = new ArrayAdapter<>(ui,
                    android.R.layout.simple_spinner_dropdown_item, SOURCE_LABELS);
            source.setAdapter(sourceAdapter);
            source.setSelection(0);
            source.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                @Override public void onItemSelected(android.widget.AdapterView<?> parent, View view,
                                                     int position, long id) {
                    if (position == selectedSource) return;
                    selectedSource = position;
                    cursor = null;
                    search(null);
                }
                @Override public void onNothingSelected(android.widget.AdapterView<?> parent) { }
            });
            controls.addView(source, new LinearLayout.LayoutParams(0, MapTheme.dp(ui, 46), 1));
            filterButton = new Button(ui);
            filterButton.setAllCaps(false);
            filterButton.setText("Filtros");
            filterButton.setOnClickListener(v -> openFilters());
            controls.addView(filterButton, new LinearLayout.LayoutParams(-2, MapTheme.dp(ui, 46)));
            root.addView(controls, new LinearLayout.LayoutParams(-1, -2));

            status = new TextView(ui);
            status.setTextSize(13);
            status.setTextColor(MapTheme.ink());
            status.setText("Nenhuma consulta realizada.");
            root.addView(status);

            coverage = new TextView(ui);
            coverage.setTextSize(12);
            coverage.setTextColor(MapTheme.muted());
            root.addView(coverage);

            alerts = new TextView(ui);
            alerts.setTextSize(12);
            alerts.setTextColor(MapTheme.muted());
            alerts.setText("Alertas locais: nenhum preset salvo ainda.");
            root.addView(alerts);

            LinearLayout actions = new LinearLayout(ui);
            actions.setOrientation(LinearLayout.HORIZONTAL);
            importButton = new Button(ui);
            importButton.setAllCaps(false);
            importButton.setText("Importar");
            importButton.setOnClickListener(v -> LocalImportDialog.show(activity, () -> {
                cursor = null;
                search(null);
            }));
            actions.addView(importButton, new LinearLayout.LayoutParams(0, -2, 1));
            pageButton = new Button(ui);
            pageButton.setAllCaps(false);
            pageButton.setText("Próxima página");
            pageButton.setEnabled(false);
            pageButton.setOnClickListener(v -> search(cursor));
            actions.addView(pageButton, new LinearLayout.LayoutParams(0, -2, 1));
            root.addView(actions, new LinearLayout.LayoutParams(-1, -2));

            adapter = new ArrayAdapter<SniperSearch.Result>(ui, android.R.layout.simple_list_item_1,
                    visible) {
                @Override public View getView(int position, View convertView, ViewGroup parent) {
                    return row(getItem(position));
                }
            };
            ListView list = new ListView(ui);
            list.setAdapter(adapter);
            list.setOnItemClickListener((parent, view, position, id) -> {
                if (position >= 0 && position < visible.size()) details(visible.get(position));
            });
            int height = Math.min(MapTheme.dp(ui, 340),
                    activity.getResources().getDisplayMetrics().heightPixels * 38 / 100);
            root.addView(list, new LinearLayout.LayoutParams(-1, height));

            Button export = new Button(ui);
            export.setAllCaps(false);
            export.setText("Exportar GPX dos resultados válidos");
            export.setOnClickListener(v -> exportGpx());
            root.addView(export, new LinearLayout.LayoutParams(-1, -2));

            renderTabs();
            updateStatus("Nenhuma consulta realizada. Toque em uma aba para buscar.");
        }

        private void renderTabs() {
            tabs.removeAllViews();
            for (int i = 0; i < TABS.length; i++) {
                final SourceEvent.Kind kind = TABS[i];
                Button button = new Button(ui);
                button.setAllCaps(false);
                button.setTextSize(13);
                button.setText(TAB_LABELS[i]);
                button.setOnClickListener(v -> {
                    tab = kind;
                    cursor = null;
                    renderTabs();
                    search(null);
                });
                button.setTextColor(kind == tab ? MapTheme.accent() : MapTheme.muted());
                tabs.addView(button, new LinearLayout.LayoutParams(-2, -2));
            }
        }

        private List<SourceAdapter> adapters() {
            List<SourceAdapter> list = new ArrayList<>();
            String selected = SOURCES[selectedSource];
            if ("import".equals(selected)) {
                list.add(new LocalStoreAdapter(store));
                return list;
            }
            if (SourceRegistry.PGBR_HUNDO.equals(selected)) list.add(new PGBRHundoSourceAdapter());
            else if (SourceRegistry.POKELIST.equals(selected)) list.add(new PokeListSourceAdapter());
            else if (SourceRegistry.IFLOWGO.equals(selected)) list.add(new IFlowGoSourceAdapter());
            else if (SourceRegistry.POGO_COORDINATES.equals(selected)) list.add(new PoGoCoordinatesSourceAdapter());
            else if (SourceRegistry.PGSHARP.equals(selected)) list.add(new LocalSourceAdapters.PGSharp(store));
            else if (SourceRegistry.IPOGO.equals(selected)) list.add(new LocalSourceAdapters.IPogo(store));
            return list;
        }

        /** One query, one list: the tab and the filters feed the same normalised search. */
        private void search(String pageCursor) {
            if (closed || busy) return;
            if (activity.isFinishing() || activity.isDestroyed()) return;
            busy = true;
            cursor = pageCursor;
            pageButton.setEnabled(false);
            updateStatus("Consultando…");
            // A remote source needs a bounded centre; local data must not be hidden by a radius the
            // user never asked for, so the centre is set without a radius for local sources.
            boolean remote = selectedSource > 0 && SourceRegistry.of(SOURCES[selectedSource]) != null
                    && SourceRegistry.of(SOURCES[selectedSource]).supportsRemoteSearch;
            SourceQuery.Builder builder = query.toBuilder().kind(tab);
            if (query.centerLat == null || query.centerLon == null) {
                double lat = Control.latitude(), lon = Control.longitude();
                if (SourceEvent.validCoordinates(lat, lon)) builder.center(lat, lon, remote ? 5.0 : null);
            }
            final SourceQuery effective = builder.build();
            final List<SourceAdapter> selected = adapters();
            final String requestedCursor = pageCursor;
            final long now = System.currentTimeMillis();
            worker.execute(() -> {
                SourceSearchResult result = null;
                String failure = null;
                try {
                    if (selected.isEmpty()) {
                        failure = "Nenhuma fonte com dados disponível.";
                    } else {
                        SourceAdapter adapter = selected.get(0);
                        result = adapter.search(effective, requestedCursor, now);
                        for (int i = 1; i < selected.size(); i++) {
                            SourceSearchResult extra = selected.get(i).search(effective, requestedCursor, now);
                            List<SourceEvent> merged = new ArrayList<>(result.items);
                            merged.addAll(extra.items);
                            result = new SourceSearchResult(result.sourceId, merged,
                                    result.hasMore || extra.hasMore, result.nextCursor,
                                    result.totalKnown, result.retrievedAt, result.coverage,
                                    result.filtersAppliedRemotely, result.filtersAppliedLocally,
                                    result.unsupportedFilters, result.note + " " + extra.note);
                        }
                    }
                } catch (SourceUnavailableException unavailable) {
                    failure = unavailable.getMessage();
                } catch (Exception error) {
                    String message = error.getMessage();
                    failure = message == null || message.trim().isEmpty()
                            ? "Falha de rede ou resposta inválida da fonte." : message;
                }
                final SourceSearchResult completed = result;
                final String error = failure;
                main.post(() -> {
                    if (closed || activity.isFinishing() || activity.isDestroyed()) return;
                    busy = false;
                    if (error != null) {
                        visible.clear();
                        adapter.notifyDataSetChanged();
                        lastResult = null;
                        pageButton.setEnabled(false);
                        updateStatus(error);
                        coverage.setText("Nenhum resultado foi confirmado. Nenhuma outra fonte foi "
                                + "consultada no lugar.");
                        return;
                    }
                    lastResult = completed;
                    List<SourceEvent> events = completed.items;
                    EventFilterEngine.Outcome outcome = EventFilterEngine.apply(effective, events, now);
                    double lat = Control.latitude(), lon = Control.longitude();
                    List<SniperSearch.Result> rows = new ArrayList<>();
                    for (SourceEvent event : outcome.kept) {
                        double distance = event.hasCoordinates()
                                && SourceEvent.validCoordinates(lat, lon)
                                ? event.distanceKm(lat, lon) : Double.NaN;
                        rows.add(new SniperSearch.Result(event, distance, event.remainingMs(now),
                                SniperSearch.actionFor(event, now, 0L)));
                    }
                    visible.clear();
                    visible.addAll(rows);
                    adapter.notifyDataSetChanged();
                    pageButton.setEnabled(completed.hasMore && completed.nextCursor != null);
                    updateStatus(outcome.summary() + (completed.hasMore
                            ? " · há mais páginas nesta fonte." : " · página única."));
                    StringBuilder text = new StringBuilder();
                    text.append("Fonte: ").append(SourceRegistry.label(completed.sourceId)).append('\n');
                    if (!completed.coverage.isEmpty()) text.append(completed.coverage).append('\n');
                    if (!completed.filtersAppliedRemotely.isEmpty())
                        text.append("Filtros aplicados pela fonte: ")
                            .append(join(completed.filtersAppliedRemotely)).append('\n');
                    if (!outcome.appliedLocally.isEmpty())
                        text.append("Filtros aplicados no aparelho: ").append(join(outcome.appliedLocally))
                            .append('\n');
                    if (!outcome.unsupported.isEmpty())
                        text.append("Não suportado por esta fonte: ").append(join(outcome.unsupported))
                            .append('\n');
                    if (!completed.note.isEmpty()) text.append(completed.note).append('\n');
                    text.append("Registros sem coordenada liberada não podem ser copiados ou usados "
                            + "em teleporte.");
                    coverage.setText(text.toString().trim());
                    evaluateAlerts(completed.items);
                });
            });
        }

        /**
         * Local alerts are evaluated only over rows the device actually received or imported; a preset
         * with no criteria never fires and a source that answered nothing produces no alert at all.
         */
        private void evaluateAlerts(List<SourceEvent> page) {
            List<PresetCodec.Preset> presets = SourceDataStore.presets(activity);
            if (presets.isEmpty()) {
                alerts.setText("Alertas locais: nenhum preset salvo. Crie um filtro em Sniper 2.");
                return;
            }
            List<SourceEvent> real = new ArrayList<>();
            for (SourceEvent event : store.all()) if (event != null) real.add(event);
            if (page != null) for (SourceEvent event : page) if (event != null) real.add(event);
            List<AlertEngine.Alert> found = AlertEngine.evaluate(presets, real, System.currentTimeMillis());
            if (found.isEmpty()) {
                alerts.setText("Alertas locais: " + presets.size() + " preset(s) avaliado(s) sobre "
                        + real.size() + " registro(s) do aparelho — nenhum casa os critérios.");
                return;
            }
            StringBuilder text = new StringBuilder("Alertas locais (").append(found.size()).append("):");
            for (AlertEngine.Alert alert : found) {
                text.append('\n').append("• ").append(alert.presetName).append(" — ")
                        .append(label(alert.event)).append(" · ").append(alert.reason);
            }
            alerts.setText(text.toString());
        }

        private void updateStatus(String text) {
            status.setText(text);
        }

        private View row(SniperSearch.Result result) {
            LinearLayout rowLayout = new LinearLayout(ui);
            rowLayout.setGravity(Gravity.CENTER_VERTICAL);
            rowLayout.setPadding(MapTheme.dp(ui, 4), MapTheme.dp(ui, 7),
                    MapTheme.dp(ui, 4), MapTheme.dp(ui, 7));
            ImageView sprite = new ImageView(ui);
            rowLayout.addView(sprite, new LinearLayout.LayoutParams(MapTheme.dp(ui, 40), MapTheme.dp(ui, 40)));
            TextView title = new TextView(ui);
            title.setTextColor(MapTheme.ink());
            title.setTextSize(14);
            title.setPadding(MapTheme.dp(ui, 9), 0, 0, 0);
            if (result == null) {
                title.setText("");
                rowLayout.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
                return rowLayout;
            }
            SourceEvent event = result.event;
            Bitmap bitmap = icons.iconFor(visual(event), System.currentTimeMillis());
            if (bitmap != null) sprite.setImageBitmap(bitmap);
            StringBuilder line = new StringBuilder();
            line.append(label(event));
            if (event.ivPercent != null)
                line.append(" · ").append(String.format(Locale.US, "%.1f%% IV", event.ivPercent));
            if (event.cp != null) line.append(" · PC ").append(event.cp);
            if (event.level != null) line.append(" · Nv ").append(event.level);
            if (event.pvpGreatRank != null) line.append(" · GL #").append(event.pvpGreatRank);
            if (event.pvpUltraRank != null) line.append(" · UL #").append(event.pvpUltraRank);
            line.append('\n').append(result.distanceText());
            if (event.expiresAt > 0)
                line.append(" · expira em ").append(Math.max(0, result.remainingMs / 1000)).append("s");
            line.append(" · ").append(event.sourceLabel());
            if (event.coordinatePrecision != SourceEvent.CoordinatePrecision.EXACT)
                line.append(" · localização não liberada");
            if (event.originKind == OriginKind.PERSONAL_IMPORT) line.append(" · importado");
            if (event.isDemo) line.append(" · demonstração");
            title.setText(line.toString());
            rowLayout.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
            return rowLayout;
        }

        private String label(SourceEvent event) {
            if (event.name != null && !event.name.isEmpty()) return event.name;
            if (event.pokemonId != null) return "#" + event.pokemonId;
            switch (event.kind) {
                case RAID: return "Reide";
                case QUEST: return "Pesquisa";
                case ROCKET: return "Invasão Rocket";
                case POWERSPOT: return "Power Spot";
                case NEST: return "Ninho";
                case ROUTE: return "Rota";
                default: return "Pokémon";
            }
        }

        private LiveFeedData.Entry visual(SourceEvent event) {
            LiveFeedData.Kind kind;
            switch (event.kind) {
                case RAID: kind = LiveFeedData.Kind.RAID; break;
                case POWERSPOT: kind = LiveFeedData.Kind.STATION; break;
                default: kind = LiveFeedData.Kind.STOP; break;
            }
            if (event.kind == SourceEvent.Kind.POKEMON) kind = LiveFeedData.Kind.POKEMON;
            return new LiveFeedData.Entry(event.eventId, kind, label(event), "",
                    event.lat == null ? 0 : event.lat, event.lon == null ? 0 : event.lon,
                    event.pokemonId == null ? -1 : event.pokemonId,
                    event.cp == null ? -1 : event.cp,
                    event.level == null ? -1 : event.level,
                    event.ivPercent == null ? -1 : event.ivPercent,
                    event.expiresAt, event.ivPercent != null);
        }

        private void details(SniperSearch.Result result) {
            if (result == null) return;
            SourceEvent event = result.event;
            long now = System.currentTimeMillis();
            SniperSearch.ActionTarget action = SniperSearch.actionFor(event, now, 0L);
            StringBuilder message = new StringBuilder();
            message.append(label(event)).append('\n');
            if (event.pokemonId != null) message.append("Pokédex: #").append(event.pokemonId).append('\n');
            if (event.ivPercent != null) message.append("IV: ")
                    .append(String.format(Locale.US, "%.1f%%", event.ivPercent)).append('\n');
            if (event.ivAtk != null && event.ivDef != null && event.ivSta != null)
                message.append("A/D/S: ").append(event.ivAtk).append('/').append(event.ivDef).append('/')
                        .append(event.ivSta).append('\n');
            if (event.cp != null) message.append("PC: ").append(event.cp).append('\n');
            if (event.level != null) message.append("Nível: ").append(event.level).append('\n');
            if (event.gender != null) message.append("Sexo: ").append(event.gender).append('\n');
            if (event.size != null) message.append("Tamanho: ").append(event.size).append('\n');
            if (event.weatherBoost != null) message.append("Clima: ")
                    .append(event.weatherBoost ? "com bônus" : "sem bônus").append('\n');
            if (event.expiresAt > 0) message.append("Expira em ")
                    .append(Math.max(0, event.remainingMs(now) / 1000)).append(" s\n");
            if (event.hasCoordinates())
                message.append("Coordenadas: ").append(CoordinateClipboard.text(event.lat, event.lon)).append('\n');
            else message.append("Coordenadas: não liberadas pela fonte (reveal_code não é coordenada)\n");
            message.append("Precisão: ").append(event.coordinatePrecision.wire).append('\n');
            message.append("Origem: ").append(event.sourceLabel()).append(" · ")
                    .append(event.originKind.labelPtBr).append('\n');
            if (!event.note.isEmpty()) message.append(event.note).append('\n');
            if (event.shinyObserved == Boolean.TRUE) message.append("Shiny observado pela fonte.\n");
            else if (event.shinyObserved == null)
                message.append("Shiny: não informado pela fonte.\n");
            message.append(!action.blockReason.isEmpty() ? action.blockReason : "");

            AlertDialog detail = new AlertDialog.Builder(ui)
                    .setTitle("Detalhes do resultado")
                    .setMessage(message.toString().trim())
                    .setNegativeButton("Fechar", null)
                    .setNeutralButton("Copiar coords", null)
                    .setPositiveButton("Teleporte", null)
                    .create();
            DialogLifecycle.bind(activity, detail, null);
            detail.setOnShowListener(ignored -> {
                MapTheme.styleButtons(detail);
                Button copy = detail.getButton(AlertDialog.BUTTON_NEUTRAL);
                Button teleport = detail.getButton(AlertDialog.BUTTON_POSITIVE);
                copy.setEnabled(action.canCopy);
                teleport.setEnabled(action.canTeleport);
                copy.setOnClickListener(v -> {
                    if (!action.canCopy) {
                        Toast.makeText(activity, "Sem coordenada liberada para copiar.",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    CoordinateClipboard.copy(activity, event.lat, event.lon, "Coordenadas copiadas");
                });
                teleport.setOnClickListener(v -> {
                    if (!action.canTeleport) {
                        Toast.makeText(activity, action.blockReason.isEmpty()
                                ? "Este resultado não pode ser usado em teleporte."
                                : action.blockReason, Toast.LENGTH_LONG).show();
                        return;
                    }
                    confirmTeleport(event);
                });
            });
            detail.show();
        }

        private void confirmTeleport(SourceEvent event) {
            long now = System.currentTimeMillis();
            if (!event.isActionable(now)) {
                Toast.makeText(activity, "Resultado expirado ou sem coordenada liberada.",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            final double lat = event.lat, lon = event.lon;
            TeleportDialog.confirm(activity, lat, lon, (latitude, longitude) -> {
                if (closed || activity.isFinishing() || activity.isDestroyed()
                        || !event.isActionable(System.currentTimeMillis()))
                    return false;
                if (!Control.canEnable()) {
                    Toast.makeText(activity, "Localização indisponível no PGBR.", Toast.LENGTH_SHORT).show();
                    return false;
                }
                Control.teleport(latitude, longitude);
                Toast.makeText(activity, "Teleporte solicitado; confirme a posição no jogo.",
                        Toast.LENGTH_SHORT).show();
                return true;
            });
        }

        private void exportGpx() {
            List<SourceEvent> events = new ArrayList<>();
            for (SniperSearch.Result result : visible)
                if (result.action.canCopy) events.add(result.event);
            if (events.isEmpty()) {
                Toast.makeText(activity, "Nenhum resultado com coordenada válida para exportar.",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            String gpx = LocalImportDialog.gpxOf(events);
            try {
                android.content.ClipboardManager clipboard = (android.content.ClipboardManager)
                        activity.getSystemService(Context.CLIPBOARD_SERVICE);
                android.content.ClipData clip = android.content.ClipData.newPlainText("PGBR GPX", gpx);
                if (clipboard != null) clipboard.setPrimaryClip(clip);
                Toast.makeText(activity, "GPX com " + events.size()
                        + " ponto(s) válido(s) copiado para a área de transferência.",
                        Toast.LENGTH_LONG).show();
            } catch (Exception error) {
                Toast.makeText(activity, "Não foi possível copiar o GPX.", Toast.LENGTH_SHORT).show();
            }
        }

        private void openFilters() {
            LinearLayout body = new LinearLayout(ui);
            body.setOrientation(LinearLayout.VERTICAL);
            int pad = MapTheme.dp(ui, 12);
            body.setPadding(pad, pad, pad, pad);
            ScrollView scroll = new ScrollView(ui);
            scroll.addView(body);

            final EditText ivMin = field(body, "IV mínimo (0–100)");
            final EditText ivMax = field(body, "IV máximo (0–100)");
            final EditText cpMin = field(body, "PC mínimo");
            final EditText cpMax = field(body, "PC máximo");
            final EditText levelMin = field(body, "Nível mínimo");
            final EditText levelMax = field(body, "Nível máximo");
            final EditText pvpRank = field(body, "Rank PvP máximo (1–5000)");
            final EditText ttl = field(body, "TTL mínimo em segundos");
            final EditText radius = field(body, "Raio em km (vazio = sem raio)");

            final TextView error = new TextView(ui);
            error.setTextColor(0xffb3261e);
            error.setTextSize(13);
            body.addView(error);

            AlertDialog filters = new AlertDialog.Builder(ui)
                    .setTitle("Filtros do radar")
                    .setView(scroll)
                    .setNegativeButton("Cancelar", null)
                    .setNeutralButton("Limpar filtros", null)
                    .setPositiveButton("Aplicar filtros", null)
                    .create();
            DialogLifecycle.bind(activity, filters, null);
            filters.setOnShowListener(ignored -> {
                MapTheme.style(body);
                MapTheme.styleButtons(filters);
                filters.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> {
                    query = SourceQuery.builder().build();
                    cursor = null;
                    filters.dismiss();
                    search(null);
                });
                filters.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                    try {
                        SourceQuery.Builder builder = SourceQuery.builder()
                                .kind(tab)
                                .iv(number(ivMin), number(ivMax))
                                .cp(integer(cpMin), integer(cpMax))
                                .level(integer(levelMin), integer(levelMax));
                        if (number(radius) != null)
                            builder.center(Control.latitude(), Control.longitude(), number(radius));
                        if (integer(pvpRank) != null) builder.pvp(SourceQuery.LEAGUE_GREAT, integer(pvpRank));
                        if (integer(ttl) != null) builder.minTtlSeconds(Long.valueOf(integer(ttl)));
                        query = builder.build();
                        query.validate();
                        error.setText("");
                        filters.dismiss();
                        cursor = null;
                        search(null);
                    } catch (JSONException invalid) {
                        error.setText("Filtro inválido: " + invalid.getMessage());
                    } catch (IllegalArgumentException invalid) {
                        error.setText("Filtro inválido: " + invalid.getMessage());
                    }
                });
            });
            filters.show();
        }

        private EditText field(LinearLayout body, String hint) {
            EditText editText = new EditText(ui);
            editText.setHint(hint);
            editText.setInputType(android.text.InputType.TYPE_CLASS_NUMBER
                    | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
                    | android.text.InputType.TYPE_NUMBER_FLAG_SIGNED);
            editText.setSingleLine(true);
            editText.setTextSize(14);
            body.addView(editText, new LinearLayout.LayoutParams(-1, MapTheme.dp(ui, 46)));
            return editText;
        }

        private static Double number(EditText editText) {
            String raw = editText.getText().toString().trim();
            if (raw.isEmpty()) return null;
            return Double.valueOf(Double.parseDouble(raw));
        }

        private static Integer integer(EditText editText) {
            String raw = editText.getText().toString().trim();
            if (raw.isEmpty()) return null;
            return Integer.valueOf(Integer.parseInt(raw));
        }

        private static String join(List<String> values) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < values.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(values.get(i));
            }
            return sb.toString();
        }

        private void close() {
            closed = true;
            worker.shutdownNow();
            main.removeCallbacksAndMessages(null);
            visible.clear();
        }
    }
}
