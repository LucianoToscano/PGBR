package dev.pgrazor;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Bitmap;
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
import android.widget.TextView;
import android.widget.Toast;
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
 * Manual 100% IV list over the EXISTING optional PGBR hundo JSON source.
 * No login, hidden credentials, periodic polling, auto-teleport or auto-catch.
 * One request is made only when the user opens the view or presses Atualizar.
 * All observed IVs are source claims; only the game can confirm an encounter.
 */
public final class HundoFeedDialog {
    private HundoFeedDialog() {}

    public static void show(Activity activity) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;
        Context ui = MapTheme.wrap(activity);
        LinearLayout root = new LinearLayout(ui);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = MapTheme.dp(ui, 12);
        root.setPadding(pad, pad, pad, pad);
        AlertDialog dialog = new AlertDialog.Builder(ui)
                .setTitle("Sniper 100% IV · PGBR")
                .setView(root)
                .setNegativeButton("Fechar", null).create();
        Screen screen = new Screen(activity, ui, dialog, root);
        DialogLifecycle.bind(activity, dialog, screen::close);
        dialog.setOnShowListener(d -> {
            MapTheme.style(root);
            MapTheme.styleButtons(dialog);
            screen.refresh();
        });
        dialog.show();
    }

    private static final class Screen {
        private static final long REQUEST_GAP_MS = 30_000L;
        private final Activity activity;
        private final Context ui;
        private final AlertDialog dialog;
        private final TextView status;
        private final EditText search;
        private final Button update;
        private final List<HundoFeedData.Entry> visible = new ArrayList<>();
        private final LiveFeedIcons icons;
        private final ArrayAdapter<HundoFeedData.Entry> adapter;
        private final Handler main = new Handler(Looper.getMainLooper());
        private final ExecutorService worker = Executors.newSingleThreadExecutor(task -> {
            Thread t = new Thread(task, "pgbr-hundo-manual");
            t.setDaemon(true);
            return t;
        });
        private HundoFeedData.Snapshot snapshot;
        private volatile boolean closed;
        private boolean fetching;
        private long lastAttemptElapsed, blockedUntilElapsed;

        Screen(Activity activity, Context ui, AlertDialog dialog, LinearLayout root) {
            this.activity = activity;
            this.ui = ui;
            this.dialog = dialog;
            this.icons = new LiveFeedIcons(activity);
            TextView hint = new TextView(ui);
            hint.setText("100% IV conforme a fonte · resultados não garantidos no jogo. "
                    + "Toque para detalhes e teleporte com confirmação.");
            hint.setTextSize(13);
            hint.setTextColor(MapTheme.muted());
            hint.setPadding(0, 0, 0, MapTheme.dp(ui, 8));
            root.addView(hint);

            search = new EditText(ui);
            search.setSingleLine(true);
            search.setHint("Filtrar por número da Pokédex (#001)");
            search.setTextSize(15);
            root.addView(search, new LinearLayout.LayoutParams(-1, MapTheme.dp(ui, 48)));

            status = new TextView(ui);
            status.setTextSize(13);
            status.setTextColor(MapTheme.muted());
            status.setText("Nenhuma consulta realizada.");
            root.addView(status);

            update = new Button(ui);
            update.setText("Atualizar resultados 100% IV");
            update.setOnClickListener(v -> refresh());
            root.addView(update, new LinearLayout.LayoutParams(-1, MapTheme.dp(ui, 48)));

            adapter = new ArrayAdapter<HundoFeedData.Entry>(ui, android.R.layout.simple_list_item_1, visible) {
                @Override public View getView(int position, View convertView, ViewGroup parent) {
                    HundoFeedData.Entry entry = getItem(position);
                    LinearLayout row = new LinearLayout(ui);
                    row.setGravity(Gravity.CENTER_VERTICAL);
                    row.setPadding(MapTheme.dp(ui, 4), MapTheme.dp(ui, 7),
                            MapTheme.dp(ui, 4), MapTheme.dp(ui, 7));
                    ImageView sprite = new ImageView(ui);
                    row.addView(sprite, new LinearLayout.LayoutParams(MapTheme.dp(ui, 44), MapTheme.dp(ui, 44)));
                    if (entry != null) {
                        LiveFeedData.Entry visual = new LiveFeedData.Entry(
                                "hundo:" + entry.encounterId, LiveFeedData.Kind.POKEMON,
                                "#" + entry.dex, "", entry.lat, entry.lon,
                                entry.dex, entry.cp, entry.level, -1, entry.despawnMs, false);
                        Bitmap bitmap = icons.iconFor(visual, System.currentTimeMillis());
                        if (bitmap != null) sprite.setImageBitmap(bitmap);
                    }
                    TextView title = new TextView(ui);
                    title.setTextColor(MapTheme.ink());
                    title.setTextSize(14);
                    title.setPadding(MapTheme.dp(ui, 9), 0, 0, 0);
                    title.setText(entry == null ? "" : rowLabel(entry, System.currentTimeMillis()));
                    row.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
                    return row;
                }
            };
            ListView list = new ListView(ui);
            list.setAdapter(adapter);
            list.setOnItemClickListener((parent, view, position, id) -> {
                if (position >= 0 && position < visible.size()) details(visible.get(position));
            });
            root.addView(list, new LinearLayout.LayoutParams(-1, MapTheme.dp(ui, 360)));
            search.addTextChangedListener(new TextWatcher() {
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                public void onTextChanged(CharSequence s, int start, int before, int count) { filter(); }
                public void afterTextChanged(Editable s) {}
            });
        }

        private String rowLabel(HundoFeedData.Entry e, long now) {
            StringBuilder line = new StringBuilder("#").append(e.dex).append(" · 100% IV (fonte)");
            if (e.cp > 0) line.append(" · PC ").append(e.cp);
            if (Double.isFinite(e.level)) line.append(" · Nv ").append(String.format(Locale.US, "%.1f", e.level));
            line.append("\nDesaparece em ").append(Math.max(0, (e.despawnMs - now) / 1000)).append("s");
            return line.toString();
        }

        private void filter() {
            visible.clear();
            if (snapshot == null || closed) { adapter.notifyDataSetChanged(); return; }
            String query = search.getText().toString().trim();
            if (query.startsWith("#")) query = query.substring(1).trim();
            long now = System.currentTimeMillis();
            if (now - snapshot.fetchedMs > HundoFeedData.MAX_SNAPSHOT_AGE_MS) {
                status.setText("Resultados antigos: toque Atualizar para consultar novamente.");
                adapter.notifyDataSetChanged(); return;
            }
            for (HundoFeedData.Entry entry : snapshot.entries) {
                if (entry.expired(now)) continue;
                if (!query.isEmpty() && !Integer.toString(entry.dex).contains(query)) continue;
                visible.add(entry);
            }
            adapter.notifyDataSetChanged();
        }

        private void refresh() {
            if (closed || fetching || activity.isFinishing() || activity.isDestroyed()) return;
            long elapsed = SystemClock.elapsedRealtime();
            if (elapsed < blockedUntilElapsed) {
                status.setText("Fonte indisponível ou com limite de consultas. Tente mais tarde.");
                return;
            }
            if (lastAttemptElapsed > 0 && elapsed - lastAttemptElapsed < REQUEST_GAP_MS) {
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
                try { result = fetchOnce(); }
                catch (Exception exception) {
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
                        visible.clear();
                        adapter.notifyDataSetChanged();
                        if (error.equals("HTTP 401") || error.equals("HTTP 403")
                                || error.equals("HTTP 429"))
                            blockedUntilElapsed = SystemClock.elapsedRealtime() + 5 * 60_000L;
                        status.setText("Não foi possível consultar o feed (" + error + "). "
                                + "Nenhuma aparição foi confirmada; sem tentativas automáticas.");
                    } else {
                        filter();
                        status.setText("Fonte PGBR · " + visible.size() + " resultados 100% IV declarados "
                                + "· atualizados há "
                                + Math.max(0, (System.currentTimeMillis() - completed.fetchedMs) / 1000)
                                + "s. Clique em um resultado para conferir.");
                    }
                });
            });
        }

        private static HundoFeedData.Snapshot fetchOnce() throws Exception {
            HttpURLConnection http = (HttpURLConnection) new URL(ShundoSniper.FEED_URL).openConnection();
            http.setInstanceFollowRedirects(false);
            http.setConnectTimeout(10_000);
            http.setReadTimeout(12_000);
            http.setRequestProperty("Accept", "application/json");
            http.setRequestProperty("User-Agent", "PGBR-Hundo-Manual");
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
            } finally { http.disconnect(); }
        }

        private void details(HundoFeedData.Entry entry) {
            long now = System.currentTimeMillis();
            if (closed || snapshot == null || now - snapshot.fetchedMs > HundoFeedData.MAX_SNAPSHOT_AGE_MS
                    || entry.expired(now)) {
                status.setText("Resultado expirado. Atualize antes de selecionar.");
                filter(); return;
            }
            String coords = CoordinateClipboard.text(entry.lat, entry.lon);
            AlertDialog detail = new AlertDialog.Builder(ui)
                    .setTitle("Pokémon #" + entry.dex + " · 100% IV (fonte)")
                    .setMessage(rowLabel(entry, now) + "\nCoordenadas: " + coords
                            + "\nOrigem: feed 100% IV PGBR. Confirme no jogo.")
                    .setNeutralButton("Copiar coords", (d, which) ->
                            CoordinateClipboard.copy(activity, entry.lat, entry.lon, "Coordenadas copiadas"))
                    .setPositiveButton("Teleportar", (d, which) -> confirmTeleport(entry))
                    .setNegativeButton("Cancelar", null).create();
            DialogLifecycle.bind(activity, detail, null);
            detail.setOnShowListener(d -> MapTheme.styleButtons(detail));
            detail.show();
        }

        private void confirmTeleport(HundoFeedData.Entry entry) {
            if (closed || entry.expired(System.currentTimeMillis()) || snapshot == null
                    || System.currentTimeMillis() - snapshot.fetchedMs > HundoFeedData.MAX_SNAPSHOT_AGE_MS)
                return;
            TeleportDialog.confirm(activity, entry.lat, entry.lon, (lat, lon) -> {
                if (closed || activity.isFinishing() || activity.isDestroyed()
                        || entry.expired(System.currentTimeMillis()) || snapshot == null
                        || System.currentTimeMillis() - snapshot.fetchedMs > HundoFeedData.MAX_SNAPSHOT_AGE_MS)
                    return false;
                if (!Control.canEnable()) {
                    Toast.makeText(activity, "Localização indisponível no PGBR.", Toast.LENGTH_SHORT).show();
                    return false;
                }
                Control.teleport(lat, lon);
                Toast.makeText(activity, "Teleporte solicitado; confirme a posição no jogo.",
                        Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                return true;
            });
        }

        private void close() {
            closed = true;
            worker.shutdownNow();
            main.removeCallbacksAndMessages(null);
            visible.clear();
        }
    }
}
