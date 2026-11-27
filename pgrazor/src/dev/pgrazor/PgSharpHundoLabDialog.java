package dev.pgrazor;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.text.InputFilter;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import org.json.JSONArray;
import org.json.JSONObject;

/** Device-side preview of the experimental read-only module contract, not a remote connector. */
final class PgSharpHundoLabDialog {
    private PgSharpHundoLabDialog() {}

    static void show(Activity activity) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;
        Context ui = MapTheme.wrap(activity);
        LinearLayout root = new LinearLayout(ui);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = MapTheme.dp(ui, 14);
        root.setPadding(pad, pad, pad, pad);
        ScrollView scroll = new ScrollView(ui);
        scroll.addView(root);
        TextView hint = new TextView(ui);
        hint.setText("Laborat\u00f3rio: nenhum m\u00f3dulo PGSharp nativo conectado. Cole uma amostra "
                + "autorizada no formato pgs-authorized-hundo-export-v1. Pr\u00e9via local, sem "
                + "consulta externa, teleporte, credenciais ou armazenamento.");
        hint.setTextColor(MapTheme.muted());
        root.addView(hint);
        EditText input = new EditText(ui);
        input.setHint("JSON do m\u00f3dulo de leitura");
        input.setGravity(Gravity.TOP | Gravity.START);
        input.setMinLines(5);
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(PgSharpHundoBridge.MAX_INPUT_CHARS)});
        root.addView(input, new LinearLayout.LayoutParams(-1, MapTheme.dp(ui, 150)));
        TextView result = new TextView(ui);
        result.setText("Nenhuma amostra validada.");
        result.setTextColor(MapTheme.ink());
        root.addView(result);
        AlertDialog dialog = new AlertDialog.Builder(ui).setTitle("PGSharp 100% IV - laborat\u00f3rio")
                .setView(scroll).setNegativeButton("Fechar", null)
                .setPositiveButton("Validar amostra", null).create();
        AtomicBoolean closed = new AtomicBoolean(false);
        ExecutorService worker = Executors.newSingleThreadExecutor();
        DialogLifecycle.bind(activity, dialog, () -> {
            closed.set(true);
            worker.shutdownNow();
        });
        dialog.setOnShowListener(d -> {
            MapTheme.style(root);
            MapTheme.styleButtons(dialog);
            Button validate = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            validate.setOnClickListener(v -> {
                if (closed.get()) return;
                String raw = input.getText().toString();
                validate.setEnabled(false);
                result.setText("Validando amostra local...");
                worker.execute(() -> {
                    String message;
                    try {
                        PgSharpHundoBridge.Report report = PgSharpHundoBridge.inspect(
                                () -> decode(raw), System.currentTimeMillis());
                        StringBuilder b = new StringBuilder("Pr\u00e9via de dados informados pelo usu\u00e1rio (origem n\u00e3o autenticada)\n");
                        b.append("Aceitos: ").append(report.rows.size()).append(" | Rejeitados: ")
                                .append(report.rejected).append(" | Duplicatas: ").append(report.duplicates);
                        long displayNow = System.currentTimeMillis();
                        int shown = 0;
                        for (PgSharpHundoBridge.Hundo h : report.rows) {
                            if (h.expiresMs <= displayNow || shown >= 50) continue;
                            shown++;
                            b.append("\n\n#").append(h.dex).append(" | IV 15/15/15\n")
                                    .append(h.lat).append(", ").append(h.lon)
                                    .append("\nExpira: ").append(new Date(h.expiresMs));
                        }
                        b.append("\n\nExibidos: ").append(shown).append(" (at\u00e9 50). Nenhuma a\u00e7\u00e3o executada.");
                        message = b.toString();
                    } catch (Exception invalid) {
                        message = "Amostra inv\u00e1lida, expirada ou incompat\u00edvel. Nenhum resultado carregado.";
                    }
                    final String output = message;
                    activity.runOnUiThread(() -> {
                        if (closed.get() || activity.isFinishing() || activity.isDestroyed()) return;
                        validate.setEnabled(true);
                        result.setText(output);
                    });
                });
            });
        });
        dialog.show();
    }

    private static PgSharpHundoBridge.Batch decode(String raw) throws Exception {
        if (raw.length() > PgSharpHundoBridge.MAX_INPUT_CHARS
                || raw.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > PgSharpHundoBridge.MAX_INPUT_CHARS)
            throw new IllegalArgumentException("Input too large");
        JSONObject doc = new JSONObject(raw);
        if (doc.optBoolean("is_demo", false)) throw new IllegalArgumentException("Demo not accepted");
        JSONArray items = doc.getJSONArray("items");
        if (items.length() > PgSharpHundoBridge.MAX_ROWS) throw new IllegalArgumentException("Too many rows");
        List<PgSharpHundoBridge.Row> rows = new ArrayList<>();
        for (int i = 0; i < items.length(); i++) {
            JSONObject p = items.optJSONObject(i);
            if (p == null || p.optBoolean("is_demo", false)) { rows.add(null); continue; }
            rows.add(new PgSharpHundoBridge.Row(p.opt("category"), p.opt("coordinate_precision"),
                    p.opt("attack_iv"), p.opt("defense_iv"), p.opt("stamina_iv"), p.opt("iv_percent"),
                    p.opt("dex"), p.opt("form"), p.opt("lat"), p.opt("lon"), p.opt("expires_ms"),
                    p.opt("encounter_id")));
        }
        return new PgSharpHundoBridge.Batch(doc.opt("schema"), doc.opt("provider"),
                doc.opt("coordinate_precision"), doc.opt("fetched_ms"), rows);
    }
}
