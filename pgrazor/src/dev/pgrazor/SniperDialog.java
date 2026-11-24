package dev.pgrazor;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
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
 * Sniper 1 (quick manual search) and Sniper 2 (advanced multi-result search) over the same
 * normalised records. Every control changes the query that is really executed; every result shows
 * its origin, TTL and precision; copy, map link and Teleporte are gated per row and Teleporte keeps
 * the existing confirmation dialog and cooldown.
 */
public final class SniperDialog {
    private SniperDialog() {}

    /** Sniper 1: quick search — species, IV, PC, TTL and radius only. */
    public static void showQuick(Activity activity) { show(activity, false); }

    /** Sniper 2: advanced search — A/D/S, PvP, form, gender, size, weather, AND/OR, presets. */
    public static void showAdvanced(Activity activity) { show(activity, true); }

    static final String[] SOURCES = {
        "import", SourceRegistry.PGBR_HUNDO, SourceRegistry.POKELIST, SourceRegistry.IFLOWGO,
        SourceRegistry.POGO_COORDINATES, SourceRegistry.PGSHARP, SourceRegistry.IPOGO};

    static final String[] SOURCE_LABELS = {
        "Dados importados (local)", "Feed 100% IV do PGBR", "PokeList / Moonani", "iFlowGo",
        "PoGoCoordinates", "PGSharp (dados próprios)", "iPogo (dados próprios)"};

    private static final String[] IV_MODES = {"Qualquer IV", "IV 100%", "IV 0%", "Faixa de IV"};
    private static final String[] GENDERS = {"Qualquer sexo", "Masculino", "Feminino", "Sem gênero"};
    private static final String[] SIZES = {"Qualquer tamanho", "XXS", "XS", "S", "M", "L", "XL", "XXL"};
    private static final String[] LEAGUES = {"Sem PvP", "Great League (GL)", "Ultra League (UL)"};
    private static final String[] LOGIC = {"Todos os critérios (E)", "Qualquer critério (OU)"};

    private static void show(Activity activity, boolean advanced) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;
        Context ui = MapTheme.wrap(activity);
        LinearLayout root = new LinearLayout(ui);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = MapTheme.dp(ui, 12);
        root.setPadding(pad, pad, pad, pad);
        AlertDialog dialog = new AlertDialog.Builder(ui)
                .setTitle(advanced ? "Sniper 2 · busca avançada" : "Sniper 1 · busca rápida")
                .setView(root)
                .setNegativeButton("Fechar", null)
                .create();
        Screen screen = new Screen(activity, ui, dialog, root, advanced);
        DialogLifecycle.bind(activity, dialog, screen::close);
        dialog.setOnShowListener(ignored -> {
            MapTheme.style(root);
            MapTheme.styleButtons(dialog);
            screen.search();
        });
        dialog.show();
    }

    private static final class Screen {
        private final Activity activity;
        private final Context ui;
        private final AlertDialog dialog;
        private final boolean advanced;
        private final Handler main = new Handler(Looper.getMainLooper());
        private final ExecutorService worker = Executors.newSingleThreadExecutor(task -> {
            Thread thread = new Thread(task, "pgbr-sniper");
            thread.setDaemon(true);
            return thread;
        });
        private final LocalEventStore store;
        private final LiveFeedIcons icons;
        private final List<SniperSearch.Result> visible = new ArrayList<>();
        private final ArrayAdapter<SniperSearch.Result> adapter;
        private final TextView status;
        private final EditText species;
        private final EditText ivMin, ivMax;
        private final EditText cpMin, cpMax;
        private final EditText ttl;
        private final EditText radius;
        private final Spinner source;
        private final Spinner ivMode;
        private final Spinner logic;
        private EditText atkMin, atkMax, defMin, defMax, staMin, staMax;
        private EditText pvpRank;
        private Spinner gender, size, league;
        private volatile boolean closed;
        private boolean busy;
        private int selectedSource = 0;

        Screen(Activity activity, Context ui, AlertDialog dialog, LinearLayout root, boolean advanced) {
            this.activity = activity;
            this.ui = ui;
            this.dialog = dialog;
            this.advanced = advanced;
            this.store = SourceDataStore.events(activity, System.currentTimeMillis());
            this.icons = new LiveFeedIcons(activity);

            LinearLayout controls = new LinearLayout(ui);
            controls.setOrientation(LinearLayout.HORIZONTAL);
            source = new Spinner(ui);
            source.setAdapter(new ArrayAdapter<>(ui, android.R.layout.simple_spinner_dropdown_item,
                    SOURCE_LABELS));
            source.setSelection(0);
            source.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                @Override public void onItemSelected(android.widget.AdapterView<?> parent, View view,
                                                     int position, long id) {
                    selectedSource = position;
                }
                @Override public void onNothingSelected(android.widget.AdapterView<?> parent) { }
            });
            controls.addView(source, new LinearLayout.LayoutParams(0, MapTheme.dp(ui, 44), 1));
            ivMode = new Spinner(ui);
            ivMode.setAdapter(new ArrayAdapter<>(ui, android.R.layout.simple_spinner_dropdown_item,
                    IV_MODES));
            controls.addView(ivMode, new LinearLayout.LayoutParams(0, MapTheme.dp(ui, 44), 1));
            root.addView(controls, new LinearLayout.LayoutParams(-1, -2));

            LinearLayout line2 = new LinearLayout(ui);
            line2.setOrientation(LinearLayout.HORIZONTAL);
            species = new EditText(ui);
            species.setHint("#Dex ou nome (vazio = qualquer)");
            species.setSingleLine(true);
            species.setTextSize(14);
            species.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
            line2.addView(species, new LinearLayout.LayoutParams(0, MapTheme.dp(ui, 44), 2));
            ivMin = field("IV mín");
            line2.addView(ivMin, new LinearLayout.LayoutParams(0, MapTheme.dp(ui, 44), 1));
            ivMax = field("IV máx");
            line2.addView(ivMax, new LinearLayout.LayoutParams(0, MapTheme.dp(ui, 44), 1));
            root.addView(line2, new LinearLayout.LayoutParams(-1, -2));

            LinearLayout line3 = new LinearLayout(ui);
            line3.setOrientation(LinearLayout.HORIZONTAL);
            cpMin = field("PC mín");
            line3.addView(cpMin, new LinearLayout.LayoutParams(0, MapTheme.dp(ui, 44), 1));
            cpMax = field("PC máx");
            line3.addView(cpMax, new LinearLayout.LayoutParams(0, MapTheme.dp(ui, 44), 1));
            ttl = field("TTL mín (s)");
            line3.addView(ttl, new LinearLayout.LayoutParams(0, MapTheme.dp(ui, 44), 1));
            radius = field("Raio km");
            line3.addView(radius, new LinearLayout.LayoutParams(0, MapTheme.dp(ui, 44), 1));
            root.addView(line3, new LinearLayout.LayoutParams(-1, -2));

            if (advanced) {
                LinearLayout ads = new LinearLayout(ui);
                ads.setOrientation(LinearLayout.HORIZONTAL);
                atkMin = field("Atk mín"); atkMax = field("Atk máx");
                defMin = field("Def mín"); defMax = field("Def máx");
                staMin = field("Sta mín"); staMax = field("Sta máx");
                for (EditText field : new EditText[]{atkMin, atkMax, defMin, defMax, staMin, staMax})
                    ads.addView(field, new LinearLayout.LayoutParams(0, MapTheme.dp(ui, 44), 1));
                root.addView(ads, new LinearLayout.LayoutParams(-1, -2));

                LinearLayout pvpRow = new LinearLayout(ui);
                pvpRow.setOrientation(LinearLayout.HORIZONTAL);
                league = new Spinner(ui);
                league.setAdapter(new ArrayAdapter<>(ui, android.R.layout.simple_spinner_dropdown_item,
                        LEAGUES));
                pvpRow.addView(league, new LinearLayout.LayoutParams(0, MapTheme.dp(ui, 44), 2));
                pvpRank = field("Rank máx");
                pvpRow.addView(pvpRank, new LinearLayout.LayoutParams(0, MapTheme.dp(ui, 44), 1));
                gender = new Spinner(ui);
                gender.setAdapter(new ArrayAdapter<>(ui, android.R.layout.simple_spinner_dropdown_item,
                        GENDERS));
                pvpRow.addView(gender, new LinearLayout.LayoutParams(0, MapTheme.dp(ui, 44), 1));
                size = new Spinner(ui);
                size.setAdapter(new ArrayAdapter<>(ui, android.R.layout.simple_spinner_dropdown_item,
                        SIZES));
                pvpRow.addView(size, new LinearLayout.LayoutParams(0, MapTheme.dp(ui, 44), 1));
                root.addView(pvpRow, new LinearLayout.LayoutParams(-1, -2));

                LinearLayout logicRow = new LinearLayout(ui);
                logicRow.setOrientation(LinearLayout.HORIZONTAL);
                logic = new Spinner(ui);
                logic.setAdapter(new ArrayAdapter<>(ui, android.R.layout.simple_spinner_dropdown_item,
                        LOGIC));
                logicRow.addView(logic, new LinearLayout.LayoutParams(0, MapTheme.dp(ui, 44), 2));
                Button save = new Button(ui);
                save.setAllCaps(false);
                save.setText("Salvar como preset");
                save.setOnClickListener(v -> savePreset());
                logicRow.addView(save, new LinearLayout.LayoutParams(0, MapTheme.dp(ui, 44), 1));
                Button presets = new Button(ui);
                presets.setAllCaps(false);
                presets.setText("Presets");
                presets.setOnClickListener(v -> openPresets());
                logicRow.addView(presets, new LinearLayout.LayoutParams(0, MapTheme.dp(ui, 44), 1));
                root.addView(logicRow, new LinearLayout.LayoutParams(-1, -2));
            } else {
                logic = null;
            }

            status = new TextView(ui);
            status.setTextSize(13);
            status.setTextColor(MapTheme.ink());
            status.setText("Nenhuma busca realizada.");
            root.addView(status);

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
            int height = Math.min(MapTheme.dp(ui, 320),
                    activity.getResources().getDisplayMetrics().heightPixels * 36 / 100);
            root.addView(list, new LinearLayout.LayoutParams(-1, height));

            Button search = new Button(ui);
            search.setAllCaps(false);
            search.setText(advanced ? "Buscar (avançado)" : "Buscar (rápido)");
            search.setOnClickListener(v -> this.search());
            root.addView(search, new LinearLayout.LayoutParams(-1, -2));
        }

        private EditText field(String hint) {
            EditText editText = new EditText(ui);
            editText.setHint(hint);
            editText.setSingleLine(true);
            editText.setTextSize(14);
            editText.setInputType(android.text.InputType.TYPE_CLASS_NUMBER
                    | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
                    | android.text.InputType.TYPE_NUMBER_FLAG_SIGNED);
            return editText;
        }

        /** Builds the query exactly from what the controls hold; invalid ranges are reported. */
        private SourceQuery queryFromControls() throws JSONException {
            SourceQuery.Builder builder = SourceQuery.builder();
            String rawSpecies = species.getText().toString().trim();
            if (!rawSpecies.isEmpty()) {
                String digits = rawSpecies.startsWith("#") ? rawSpecies.substring(1) : rawSpecies;
                try {
                    int dex = Integer.parseInt(digits.trim());
                    builder.pokemon(dex);
                } catch (NumberFormatException notDex) {
                    builder.text(rawSpecies);
                }
            }
            String mode = IV_MODES[ivMode.getSelectedItemPosition()];
            if (mode.equals("IV 100%")) builder.iv(100.0, 100.0);
            else if (mode.equals("IV 0%")) builder.iv(0.0, 0.0);
            else if (mode.equals("Faixa de IV")) builder.iv(number(ivMin), number(ivMax));
            builder.cp(integer(cpMin), integer(cpMax));
            if (integer(ttl) != null) builder.minTtlSeconds(Long.valueOf(integer(ttl)));
            Double radiusKm = number(radius);
            double lat = Control.latitude(), lon = Control.longitude();
            if (SourceEvent.validCoordinates(lat, lon)) builder.center(lat, lon, radiusKm);
            if (advanced) {
                builder.attack(integer(atkMin), integer(atkMax));
                builder.defense(integer(defMin), integer(defMax));
                builder.stamina(integer(staMin), integer(staMax));
                int leagueIndex = league.getSelectedItemPosition();
                if (leagueIndex == 1) builder.pvp(SourceQuery.LEAGUE_GREAT, integer(pvpRank) == null
                        ? Integer.valueOf(1) : integer(pvpRank));
                else if (leagueIndex == 2) builder.pvp(SourceQuery.LEAGUE_ULTRA, integer(pvpRank) == null
                        ? Integer.valueOf(1) : integer(pvpRank));
                int genderIndex = gender.getSelectedItemPosition();
                if (genderIndex == 1) builder.gender("MALE");
                else if (genderIndex == 2) builder.gender("FEMALE");
                else if (genderIndex == 3) builder.gender("GENDERLESS");
                int sizeIndex = size.getSelectedItemPosition();
                if (sizeIndex > 0) builder.size(SIZES[sizeIndex]);
                if (logic != null && logic.getSelectedItemPosition() == 1) builder.andLogic(false);
            }
            SourceQuery query = builder.build();
            query.validate();
            return query;
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
            else if (SourceRegistry.POGO_COORDINATES.equals(selected))
                list.add(new PoGoCoordinatesSourceAdapter());
            else if (SourceRegistry.PGSHARP.equals(selected))
                list.add(new LocalSourceAdapters.PGSharp(store));
            else if (SourceRegistry.IPOGO.equals(selected)) list.add(new LocalSourceAdapters.IPogo(store));
            return list;
        }

        private void search() {
            if (closed || busy) return;
            final SourceQuery query;
            try {
                query = queryFromControls();
            } catch (JSONException invalid) {
                status.setText("Filtro inválido: " + invalid.getMessage());
                return;
            } catch (IllegalArgumentException invalid) {
                status.setText("Filtro inválido: " + invalid.getMessage());
                return;
            }
            busy = true;
            status.setText("Buscando…");
            final List<SourceAdapter> selected = adapters();
            final double lat = Control.latitude(), lon = Control.longitude();
            final long now = System.currentTimeMillis();
            worker.execute(() -> {
                SniperSearch.Outcome outcome = null;
                String failure = null;
                try {
                    outcome = advanced
                            ? SniperSearch.sniper2(query, selected, store, now, lat, lon, 0L)
                            : SniperSearch.sniper1(query, selected, store, now, lat, lon, 0L);
                } catch (Exception error) {
                    failure = error.getMessage() == null ? "Falha na busca." : error.getMessage();
                }
                final SniperSearch.Outcome completed = outcome;
                final String error = failure;
                main.post(() -> {
                    if (closed || activity.isFinishing() || activity.isDestroyed()) return;
                    busy = false;
                    if (error != null) {
                        visible.clear();
                        adapter.notifyDataSetChanged();
                        status.setText("Busca não concluída: " + error);
                        return;
                    }
                    visible.clear();
                    visible.addAll(completed.results);
                    adapter.notifyDataSetChanged();
                    status.setText(completed.summary());
                    if (completed.isEmpty() && !completed.anyUnavailable)
                        status.append("\nNenhum resultado confirmado com estes critérios. "
                                + "Nenhuma lista foi fabricada.");
                });
            });
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
                rowLayout.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
                return rowLayout;
            }
            SourceEvent event = result.event;
            if (event.kind == SourceEvent.Kind.POKEMON) {
                Bitmap bitmap = icons.iconFor(new LiveFeedData.Entry(event.eventId,
                        LiveFeedData.Kind.POKEMON, event.name, "",
                        event.lat == null ? 0 : event.lat, event.lon == null ? 0 : event.lon,
                        event.pokemonId == null ? -1 : event.pokemonId,
                        event.cp == null ? -1 : event.cp, event.level == null ? -1 : event.level,
                        event.ivPercent == null ? -1 : event.ivPercent, event.expiresAt,
                        event.ivPercent != null), System.currentTimeMillis());
                if (bitmap != null) sprite.setImageBitmap(bitmap);
            }
            StringBuilder line = new StringBuilder();
            line.append(event.name.isEmpty() ? "#" + event.pokemonId : event.name);
            if (event.ivPercent != null)
                line.append(" · ").append(String.format(Locale.US, "%.1f%%", event.ivPercent));
            if (event.cp != null) line.append(" · PC ").append(event.cp);
            if (event.pvpGreatRank != null) line.append(" · GL #").append(event.pvpGreatRank);
            line.append('\n').append(result.distanceText());
            if (event.expiresAt > 0)
                line.append(" · ").append(Math.max(0, result.remainingMs / 1000)).append("s");
            line.append(" · ").append(event.sourceLabel());
            if (!result.action.canTeleport) line.append(" · sem teleporte");
            title.setText(line.toString());
            rowLayout.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
            return rowLayout;
        }

        private void details(SniperSearch.Result result) {
            if (result == null) return;
            final SourceEvent event = result.event;
            final long now = System.currentTimeMillis();
            final SniperSearch.ActionTarget action = SniperSearch.actionFor(event, now, 0L);
            StringBuilder message = new StringBuilder();
            message.append(event.name.isEmpty() ? "#" + event.pokemonId : event.name).append('\n');
            if (event.ivPercent != null) message.append("IV: ")
                    .append(String.format(Locale.US, "%.1f%%", event.ivPercent)).append('\n');
            if (event.ivAtk != null && event.ivDef != null && event.ivSta != null)
                message.append("A/D/S: ").append(event.ivAtk).append('/').append(event.ivDef)
                        .append('/').append(event.ivSta).append('\n');
            if (event.cp != null) message.append("PC: ").append(event.cp).append('\n');
            if (event.level != null) message.append("Nível: ").append(event.level).append('\n');
            if (event.gender != null) message.append("Sexo: ").append(event.gender).append('\n');
            if (event.size != null) message.append("Tamanho: ").append(event.size).append('\n');
            if (event.expiresAt > 0)
                message.append("Expira em ").append(Math.max(0, event.remainingMs(now) / 1000))
                        .append(" s\n");
            if (event.hasCoordinates())
                message.append("Coordenadas: ").append(CoordinateClipboard.text(event.lat, event.lon))
                        .append('\n');
            else message.append("Coordenadas: não liberadas pela fonte.\n");
            message.append("Origem: ").append(event.sourceLabel()).append(" · ")
                    .append(event.originKind.labelPtBr).append('\n');
            message.append("Precisão: ").append(event.coordinatePrecision.wire).append('\n');
            if (!event.note.isEmpty()) message.append(event.note).append('\n');
            if (!action.blockReason.isEmpty()) message.append(action.blockReason);

            LinearLayout body = new LinearLayout(ui);
            body.setOrientation(LinearLayout.VERTICAL);
            int pad = MapTheme.dp(ui, 12);
            body.setPadding(pad, pad, pad, pad);
            TextView text = new TextView(ui);
            text.setText(message.toString().trim());
            text.setTextSize(14);
            body.addView(text);
            // The map link is a separate action so copy/map/teleporte stay individually gated by
            // the same ActionTarget instead of one button enabling all of them.
            if (action.canMap) {
                Button map = new Button(ui);
                map.setAllCaps(false);
                map.setText("Abrir no mapa");
                map.setOnClickListener(v -> {
                    if (!event.hasCoordinates()) return;
                    try {
                        String coords = String.format(Locale.US, "%.6f,%.6f", event.lat, event.lon);
                        Uri uri = Uri.parse("geo:" + coords + "?q=" + coords);
                        activity.startActivity(new Intent(Intent.ACTION_VIEW, uri));
                    } catch (Exception noMapApp) {
                        Toast.makeText(activity, "Nenhum aplicativo de mapa disponível.",
                                Toast.LENGTH_SHORT).show();
                    }
                });
                body.addView(map, new LinearLayout.LayoutParams(-1, -2));
            }
            ScrollView scroll = new ScrollView(ui);
            scroll.addView(body);

            AlertDialog detail = new AlertDialog.Builder(ui)
                    .setTitle("Resultado do Sniper")
                    .setView(scroll)
                    .setNegativeButton("Fechar", null)
                    .setNeutralButton("Copiar coords", null)
                    .setPositiveButton("Teleporte", null)
                    .create();
            DialogLifecycle.bind(activity, detail, null);
            detail.setOnShowListener(ignored -> {
                MapTheme.style(body);
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
            final double lat = event.lat, lon = event.lon;
            if (!event.isActionable(System.currentTimeMillis())) {
                Toast.makeText(activity, "Resultado expirado ou sem coordenada liberada.",
                        Toast.LENGTH_SHORT).show();
                return;
            }
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

        private void savePreset() {
            final SourceQuery query;
            try {
                query = queryFromControls();
            } catch (Exception invalid) {
                Toast.makeText(activity, "Corrija os filtros antes de salvar: " + invalid.getMessage(),
                        Toast.LENGTH_SHORT).show();
                return;
            }
            LinearLayout body = new LinearLayout(ui);
            body.setOrientation(LinearLayout.VERTICAL);
            int pad = MapTheme.dp(ui, 12);
            body.setPadding(pad, pad, pad, pad);
            EditText name = new EditText(ui);
            name.setHint("Nome do preset");
            name.setSingleLine(true);
            body.addView(name, new LinearLayout.LayoutParams(-1, MapTheme.dp(ui, 46)));
            CheckBox favorite = new CheckBox(ui);
            favorite.setText("Favorito");
            body.addView(favorite);
            AlertDialog save = new AlertDialog.Builder(ui)
                    .setTitle("Salvar preset")
                    .setView(body)
                    .setNegativeButton("Cancelar", null)
                    .setPositiveButton("Salvar", null)
                    .create();
            DialogLifecycle.bind(activity, save, null);
            save.setOnShowListener(ignored -> {
                MapTheme.style(body);
                MapTheme.styleButtons(save);
                save.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                    String label = name.getText().toString().trim();
                    if (label.isEmpty()) {
                        Toast.makeText(activity, "Dê um nome ao preset.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    try {
                        PresetCodec.Preset preset = PresetCodec.create(label, query,
                                System.currentTimeMillis());
                        preset = preset.withFavorite(favorite.isChecked(), System.currentTimeMillis());
                        SourceDataStore.upsertPreset(activity, preset);
                        Toast.makeText(activity, "Preset salvo: " + label, Toast.LENGTH_SHORT).show();
                        save.dismiss();
                    } catch (JSONException invalid) {
                        Toast.makeText(activity, "Preset inválido: " + invalid.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
            });
            save.show();
        }

        private void openPresets() {
            final List<PresetCodec.Preset> presets = SourceDataStore.presets(activity);
            if (presets.isEmpty()) {
                new AlertDialog.Builder(ui).setTitle("Presets")
                        .setMessage("Nenhum preset salvo. Use \"Salvar como preset\" para guardar a busca atual.")
                        .setPositiveButton("Fechar", null).show();
                return;
            }
            String[] labels = new String[presets.size()];
            for (int i = 0; i < presets.size(); i++)
                labels[i] = (presets.get(i).favorite ? "★ " : "") + presets.get(i).name;
            AlertDialog list = new AlertDialog.Builder(ui).setTitle("Presets salvos")
                    .setItems(labels, (d, which) -> applyPreset(presets.get(which)))
                    .setNegativeButton("Fechar", null)
                    .setNeutralButton("Excluir", null)
                    .setPositiveButton("Exportar", null)
                    .create();
            DialogLifecycle.bind(activity, list, null);
            list.setOnShowListener(ignored -> {
                MapTheme.styleButtons(list);
                list.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> {
                    list.dismiss();
                    chooseToDelete(presets);
                });
                list.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> exportPresets(presets));
            });
            list.show();
        }

        private void applyPreset(PresetCodec.Preset preset) {
            SourceQuery query = preset.query;
            species.setText(query.pokemonIds.isEmpty() ? "" : "#" + query.pokemonIds.iterator().next());
            if (query.ivMin != null && query.ivMax != null && query.ivMin == 100 && query.ivMax == 100)
                ivMode.setSelection(1);
            else if (query.ivMin != null && query.ivMax != null && query.ivMin == 0 && query.ivMax == 0)
                ivMode.setSelection(2);
            else {
                ivMode.setSelection(3);
                ivMin.setText(query.ivMin == null ? "" : trim(query.ivMin));
                ivMax.setText(query.ivMax == null ? "" : trim(query.ivMax));
            }
            cpMin.setText(query.cpMin == null ? "" : String.valueOf(query.cpMin));
            cpMax.setText(query.cpMax == null ? "" : String.valueOf(query.cpMax));
            ttl.setText(query.minTtlSeconds == null ? "" : String.valueOf(query.minTtlSeconds));
            radius.setText(query.radiusKm == null ? "" : trim(query.radiusKm));
            if (advanced) {
                atkMin.setText(query.atkMin == null ? "" : String.valueOf(query.atkMin));
                atkMax.setText(query.atkMax == null ? "" : String.valueOf(query.atkMax));
                defMin.setText(query.defMin == null ? "" : String.valueOf(query.defMin));
                defMax.setText(query.defMax == null ? "" : String.valueOf(query.defMax));
                staMin.setText(query.staMin == null ? "" : String.valueOf(query.staMin));
                staMax.setText(query.staMax == null ? "" : String.valueOf(query.staMax));
                pvpRank.setText(query.pvpMaxRank == null ? "" : String.valueOf(query.pvpMaxRank));
                league.setSelection(SourceQuery.LEAGUE_GREAT.equals(query.pvpLeague) ? 1
                        : SourceQuery.LEAGUE_ULTRA.equals(query.pvpLeague) ? 2 : 0);
                gender.setSelection("MALE".equals(query.gender) ? 1
                        : "FEMALE".equals(query.gender) ? 2
                        : "GENDERLESS".equals(query.gender) ? 3 : 0);
                int sizeIndex = 0;
                if (!query.sizes.isEmpty()) {
                    String wanted = query.sizes.iterator().next();
                    for (int i = 1; i < SIZES.length; i++) if (SIZES[i].equals(wanted)) sizeIndex = i;
                }
                size.setSelection(sizeIndex);
                logic.setSelection(query.andLogic ? 0 : 1);
            }
            status.setText("Preset aplicado: " + preset.name + ". Toque em Buscar.");
        }

        private void chooseToDelete(final List<PresetCodec.Preset> presets) {
            String[] labels = new String[presets.size()];
            for (int i = 0; i < presets.size(); i++) labels[i] = presets.get(i).name;
            AlertDialog delete = new AlertDialog.Builder(ui).setTitle("Excluir preset")
                    .setItems(labels, (d, which) -> {
                        SourceDataStore.deletePreset(activity, presets.get(which).id);
                        Toast.makeText(activity, "Preset excluído: " + presets.get(which).name,
                                Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancelar", null).create();
            DialogLifecycle.bind(activity, delete, null);
            delete.setOnShowListener(ignored -> MapTheme.styleButtons(delete));
            delete.show();
        }

        private void exportPresets(List<PresetCodec.Preset> presets) {
            try {
                String json = PresetCodec.export(presets);
                android.content.ClipboardManager clipboard = (android.content.ClipboardManager)
                        activity.getSystemService(Context.CLIPBOARD_SERVICE);
                if (clipboard != null)
                    clipboard.setPrimaryClip(android.content.ClipData.newPlainText("PGBR presets", json));
                Toast.makeText(activity, "Presets exportados para a área de transferência.",
                        Toast.LENGTH_SHORT).show();
            } catch (JSONException error) {
                Toast.makeText(activity, "Não foi possível exportar os presets.", Toast.LENGTH_SHORT).show();
            }
        }

        private static String trim(double value) {
            return value == Math.rint(value) ? Long.toString((long) value) : Double.toString(value);
        }

        private static Double number(EditText editText) {
            String raw = editText.getText().toString().trim();
            return raw.isEmpty() ? null : Double.valueOf(Double.parseDouble(raw));
        }

        private static Integer integer(EditText editText) {
            String raw = editText.getText().toString().trim();
            return raw.isEmpty() ? null : Integer.valueOf(Integer.parseInt(raw));
        }

        private void close() {
            closed = true;
            worker.shutdownNow();
            main.removeCallbacksAndMessages(null);
            visible.clear();
        }
    }
}
