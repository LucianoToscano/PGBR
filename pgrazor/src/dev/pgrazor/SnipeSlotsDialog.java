package dev.pgrazor;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONObject;

/**
 * Sniper 1 and Sniper 2 side by side, each with its OWN filter set and its OWN result list.
 *
 * <p>Both slots read the same PGBR source that the manual 100% IV reader already uses
 * ({@link ShundoSniper#FEED_URL}); no PGSharp endpoint, login, key or protocol is contacted. One
 * request feeds both slots, and every row is then filtered independently by
 * {@link SnipeFilters#matches}. Nothing is captured or teleported automatically, and there is no
 * polling: exactly one request is made when the panel opens, plus one per Atualizar press paced at a
 * 30-second minimum. The 10-second countdown tick is local and never touches the network.
 *
 * <p>"100% IV" is the provider's claim inherited from the feed schema; it is not an IV read from the
 * game. Rows are only selectable while the snapshot is fresh and the row is unexpired, and a teleport
 * always needs the existing explicit confirmation.
 */
public final class SnipeSlotsDialog {
    private SnipeSlotsDialog() {}

    public static void show(Activity activity) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;
        new Screen(activity).open();
    }

    private static final class Screen {
        private static final long REQUEST_GAP_MS = 30_000L;
        private static final long BLOCK_MS = 5 * 60_000L;

        private final Activity activity;
        private final Context ui;
        private final Handler main = new Handler(Looper.getMainLooper());
        private final ExecutorService worker = Executors.newSingleThreadExecutor(task -> {
            Thread t = new Thread(task, "pgbr-snipe-slots");
            t.setDaemon(true);
            return t;
        });

        private AlertDialog dialog;
        private final SlotView first;
        private final SlotView second;
        private final TextView status;
        private final Button update;
        private final android.content.SharedPreferences prefs;

        private HundoFeedData.Snapshot snapshot;
        private volatile boolean closed;
        private boolean fetching;
        private long lastAttemptElapsed, blockedUntilElapsed;

        private final Runnable expiryTick = new Runnable() {
            @Override public void run() {
                if (closed) return;
                render();                        // Local countdown only; never another request.
                main.postDelayed(this, 10_000L);
            }
        };

        Screen(Activity activity) {
            this.activity = activity;
            this.ui = MapTheme.wrap(activity);
            this.prefs = SnipeFilterDialog.preferences(activity);
            this.first = new SlotView(SnipeFilters.SLOT_ONE, "Sniper 1");
            this.second = new SlotView(SnipeFilters.SLOT_TWO, "Sniper 2");
            this.status = new TextView(ui);
            this.update = new Button(ui);
        }

        void open() {
            final LinearLayout page = new LinearLayout(ui);
            page.setOrientation(LinearLayout.VERTICAL);
            int pad = MapTheme.dp(ui, 12);
            page.setPadding(pad, pad, pad, pad);

            // Small screens scroll instead of clipping the second slot; weight 1 keeps the button
            // and status row pinned at the bottom.
            android.widget.ScrollView scroll = new android.widget.ScrollView(ui);
            scroll.setFillViewport(true);
            LinearLayout root = new LinearLayout(ui);
            root.setOrientation(LinearLayout.VERTICAL);
            scroll.addView(root, new android.widget.ScrollView.LayoutParams(-1, -2));
            page.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));

            TextView hint = new TextView(ui);
            hint.setTextSize(13);
            hint.setTextColor(MapTheme.muted());
            hint.setText("Dois filtros separados sobre a mesma fonte 100% IV já usada pelo PGBR. "
                    + "100% IV é a declaração da fonte, não uma leitura do jogo. "
                    + "Toque em um resultado para conferir e teleportar com confirmação.");
            root.addView(hint);

            root.addView(first.card(), new LinearLayout.LayoutParams(-1, -2));
            root.addView(second.card(), new LinearLayout.LayoutParams(-1, -2));

            update.setText("Atualizar fonte 100% IV");
            update.setOnClickListener(v -> refresh());
            page.addView(update, new LinearLayout.LayoutParams(-1, MapTheme.dp(ui, 48)));

            status.setTextSize(13);
            status.setTextColor(MapTheme.muted());
            status.setText("Nenhuma consulta realizada.");
            page.addView(status);

            dialog = new AlertDialog.Builder(ui)
                    .setTitle("Sniper 1 e 2 · filtros separados")
                    .setView(page)
                    .setNegativeButton("Fechar", null)
                    .create();
            DialogLifecycle.bind(activity, dialog, this::close);
            dialog.setOnShowListener(d -> {
                MapTheme.fullScreen(dialog);   // two lists plus the button need the height.
                MapTheme.style(root);
                MapTheme.styleButtons(dialog);
                render();
                refresh(true);                   // The one request this panel makes on its own.
                main.postDelayed(expiryTick, 10_000L);
            });
            dialog.show();
        }

        /** Re-reads both slots from their own keys and re-renders each list independently. */
        private void render() {
            if (closed) return;
            long now = System.currentTimeMillis();
            first.applyFilters(SnipeFilterDialog.load(prefs, SnipeFilters.SLOT_ONE));
            second.applyFilters(SnipeFilterDialog.load(prefs, SnipeFilters.SLOT_TWO));
            boolean stale = snapshot == null || now - snapshot.fetchedMs > HundoFeedData.MAX_SNAPSHOT_AGE_MS;
            first.render(stale ? null : snapshot, now);
            second.render(stale ? null : snapshot, now);
            if (stale && snapshot != null)
                status.setText("Resultados antigos: toque Atualizar para consultar novamente.");
        }

        // ------------------------------------------------------------------ one request, both slots

        private void refresh() { refresh(false); }

        /** {@code automatic} is true only for the single request made when the panel opens. */
        private void refresh(boolean automatic) {
            if (closed || fetching || activity.isFinishing() || activity.isDestroyed()) return;
            long elapsed = SystemClock.elapsedRealtime();
            if (elapsed < blockedUntilElapsed) {
                status.setText("Fonte indisponível ou com limite de consultas. Tente mais tarde.");
                return;
            }
            // The 30 s gap paces the user's button. The single automatic request on open bypasses it,
            // so a panel that was left and reopened always shows a fresh snapshot or an error.
            if (!automatic && lastAttemptElapsed > 0 && elapsed - lastAttemptElapsed < REQUEST_GAP_MS) {
                status.setText("Aguarde 30 segundos entre consultas manuais.");
                return;
            }
            fetching = true;
            lastAttemptElapsed = elapsed;
            update.setEnabled(false);
            status.setText("Consultando fonte 100% IV…");
            worker.execute(() -> {
                HundoFeedData.Snapshot result = null;
                String failure = null;
                try {
                    result = fetchOnce();
                } catch (Exception exception) {
                    String raw = exception.getMessage();
                    failure = raw != null && raw.matches("HTTP [0-9]{3}") ? raw
                            : "Falha de rede ou resposta inválida da fonte.";
                }
                final HundoFeedData.Snapshot completed = result;
                final String error = failure;
                main.post(() -> {
                    if (closed || activity.isFinishing() || activity.isDestroyed()) return;
                    fetching = false;
                    update.setEnabled(true);
                    snapshot = completed;
                    if (error != null) {
                        first.clear();
                        second.clear();
                        if (error.equals("HTTP 401") || error.equals("HTTP 403")
                                || error.equals("HTTP 429"))
                            blockedUntilElapsed = SystemClock.elapsedRealtime() + BLOCK_MS;
                        status.setText("Não foi possível consultar o feed (" + error + "). "
                                + "Nenhuma aparição foi confirmada; sem tentativas automáticas.");
                        return;
                    }
                    render();
                    status.setText("Fonte PGBR · Sniper 1: " + first.size() + " · Sniper 2: "
                            + second.size() + " · atualizados há "
                            + Math.max(0, (System.currentTimeMillis() - completed.fetchedMs) / 1000)
                            + "s. Cada filtro vale só para o seu slot.");
                });
            });
        }

        /** Same transport contract as the manual 100% IV reader: one GET, no redirects, bounded body. */
        private static HundoFeedData.Snapshot fetchOnce() throws Exception {
            HttpURLConnection http = (HttpURLConnection) new URL(ShundoSniper.FEED_URL).openConnection();
            http.setInstanceFollowRedirects(false);
            http.setConnectTimeout(10_000);
            http.setReadTimeout(12_000);
            http.setRequestProperty("Accept", "application/json");
            http.setRequestProperty("User-Agent", "PGBR-Snipe-Slots");
            try {
                int code = http.getResponseCode();
                if (code != 200) throw new IOException("HTTP " + code);
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                try (InputStream in = http.getInputStream()) {
                    byte[] buf = new byte[8192];
                    int n;
                    while ((n = in.read(buf)) != -1) {
                        if (bytes.size() + n > 8 * 1024 * 1024)
                            throw new IOException("Resposta excedeu o limite de 8 MB.");
                        bytes.write(buf, 0, n);
                    }
                }
                return HundoFeedData.parse(
                        new JSONObject(new String(bytes.toByteArray(), StandardCharsets.UTF_8)),
                        System.currentTimeMillis());
            } finally {
                http.disconnect();
            }
        }

        private void close() {
            closed = true;
            worker.shutdownNow();
            main.removeCallbacksAndMessages(null);
            first.clear();
            second.clear();
        }

        // ------------------------------------------------------------------ one slot

        /** One independent slot: its own filters, its own filtered rows, its own list widget. */
        private final class SlotView {
            private final String slot, title;
            private SnipeFilters filters = SnipeFilters.defaults();
            private final List<HundoFeedData.Entry> visible = new ArrayList<>();
            private final TextView summary;
            private final Button edit;
            private final ArrayAdapter<HundoFeedData.Entry> adapter;

            SlotView(String slot, String title) {
                this.slot = slot;
                this.title = title;
                this.summary = new TextView(ui);
                this.edit = new Button(ui);
                this.adapter = new ArrayAdapter<HundoFeedData.Entry>(ui,
                        android.R.layout.simple_list_item_1, visible) {
                    @Override public View getView(int position, View convertView, ViewGroup parent) {
                        HundoFeedData.Entry entry = getItem(position);
                        TextView row = new TextView(ui);
                        row.setTextColor(MapTheme.ink());
                        row.setTextSize(14);
                        row.setPadding(MapTheme.dp(ui, 4), MapTheme.dp(ui, 7),
                                MapTheme.dp(ui, 4), MapTheme.dp(ui, 7));
                        row.setText(entry == null ? "" : rowLabel(entry, System.currentTimeMillis()));
                        return row;
                    }
                };
            }

            LinearLayout card() {
                LinearLayout card = new LinearLayout(ui);
                card.setOrientation(LinearLayout.VERTICAL);
                int pad = MapTheme.dp(ui, 6);
                card.setPadding(pad, pad, pad, pad);

                TextView heading = new TextView(ui);
                heading.setText(title);
                heading.setTextSize(16);
                heading.setTextColor(MapTheme.accent());
                heading.setGravity(Gravity.START);
                card.addView(heading);

                summary.setTextSize(12);
                summary.setTextColor(MapTheme.muted());
                card.addView(summary);

                edit.setText("Editar filtros de " + title);
                edit.setOnClickListener(v -> SnipeFilterDialog.show(activity, title, filters, parsed -> {
                    // Only this slot's key is written; the other slot keeps its own JSON untouched.
                    prefs.edit().putString(slot, parsed.toJson().toString()).apply();
                    filters = parsed;
                    render(snapshot != null
                            && System.currentTimeMillis() - snapshot.fetchedMs
                                    <= HundoFeedData.MAX_SNAPSHOT_AGE_MS ? snapshot : null,
                            System.currentTimeMillis());
                }));
                card.addView(edit, new LinearLayout.LayoutParams(-1, MapTheme.dp(ui, 44)));

                ListView list = new ListView(ui);
                list.setAdapter(adapter);
                list.setOnItemClickListener((parent, view, position, id) -> {
                    if (position >= 0 && position < visible.size()) details(visible.get(position));
                });
                card.addView(list, new LinearLayout.LayoutParams(-1,
                        Math.max(MapTheme.dp(ui, 96),
                                Math.min(MapTheme.dp(ui, 180),
                                        activity.getResources().getDisplayMetrics().heightPixels
                                                * 12 / 100))));
                return card;
            }

            void applyFilters(SnipeFilters loaded) {
                filters = loaded == null ? SnipeFilters.defaults() : loaded;
            }

            /** Filters the snapshot for this slot only. A stale or absent snapshot renders nothing. */
            void render(HundoFeedData.Snapshot fresh, long now) {
                visible.clear();
                if (fresh != null) visible.addAll(filters.select(fresh.entries, now));
                adapter.notifyDataSetChanged();
                summary.setText(filters.summary() + (fresh == null
                        ? " · sem consulta recente"
                        : " · " + visible.size() + (visible.size() == 1 ? " alvo" : " alvos")));
            }

            int size() { return visible.size(); }

            void clear() {
                visible.clear();
                adapter.notifyDataSetChanged();
            }

            private String rowLabel(HundoFeedData.Entry e, long now) {
                StringBuilder line = new StringBuilder("#").append(e.dex).append(" · 100% IV (fonte)");
                if (e.cp > 0) line.append(" · PC ").append(e.cp);
                if (Double.isFinite(e.level))
                    line.append(" · Nv ").append(String.format(Locale.US, "%.1f", e.level));
                line.append("\nDesaparece em ").append(Math.max(0, (e.despawnMs - now) / 1000)).append("s");
                return line.toString();
            }

            private void details(HundoFeedData.Entry entry) {
                long now = System.currentTimeMillis();
                if (closed || snapshot == null
                        || now - snapshot.fetchedMs > HundoFeedData.MAX_SNAPSHOT_AGE_MS
                        || entry.expired(now)) {
                    status.setText("Resultado expirado. Atualize antes de selecionar.");
                    render(null, now);
                    return;
                }
                String coords = CoordinateClipboard.text(entry.lat, entry.lon);
                AlertDialog detail = new AlertDialog.Builder(ui)
                        .setTitle("Pokémon #" + entry.dex + " · 100% IV (fonte)")
                        .setMessage(rowLabel(entry, now) + "\nCoordenadas: " + coords
                                + "\nSlot: " + title
                                + "\nOrigem: feed 100% IV PGBR. Confirme no jogo.")
                        .setNeutralButton("Copiar coords", (d, which) ->
                                CoordinateClipboard.copy(activity, entry.lat, entry.lon,
                                        "Coordenadas copiadas"))
                        .setPositiveButton("Teleportar", (d, which) -> confirmTeleport(entry))
                        .setNegativeButton("Cancelar", null).create();
                DialogLifecycle.bind(activity, detail, null);
                detail.setOnShowListener(d -> MapTheme.styleButtons(detail));
                detail.show();
            }

            private void confirmTeleport(final HundoFeedData.Entry entry) {
                if (closed || entry.expired(System.currentTimeMillis()) || snapshot == null
                        || System.currentTimeMillis() - snapshot.fetchedMs
                                > HundoFeedData.MAX_SNAPSHOT_AGE_MS)
                    return;
                TeleportDialog.confirm(activity, entry.lat, entry.lon, (lat, lon) -> {
                    if (closed || activity.isFinishing() || activity.isDestroyed()
                            || entry.expired(System.currentTimeMillis()) || snapshot == null
                            || System.currentTimeMillis() - snapshot.fetchedMs
                                    > HundoFeedData.MAX_SNAPSHOT_AGE_MS)
                        return false;
                    if (!Control.canEnable()) {
                        status.setText("Localização indisponível no PGBR.");
                        return false;
                    }
                    Control.teleport(lat, lon);
                    status.setText("Teleporte solicitado; confirme a posição no jogo.");
                    return true;
                });
            }
        }
    }
}
