package dev.pgrazor;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.List;
import org.json.JSONObject;

/**
 * Local entry/import: paste clipboard contents, type coordinates or paste a JSON/CSV/GPX block.
 * The report names every rejected row and its reason, refuses duplicates, keeps the original text in
 * the report and never turns an imported file into a live feed.
 */
final class LocalImportDialog {
    private LocalImportDialog() {}

    static void show(Activity activity, Runnable onImported) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;
        Context ui = MapTheme.wrap(activity);
        LinearLayout root = new LinearLayout(ui);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = MapTheme.dp(ui, 12);
        root.setPadding(pad, pad, pad, pad);

        TextView hint = new TextView(ui);
        hint.setText("Cole aqui coordenadas (lat, lon), JSON, CSV, GPX ou a configuração "
                + "exportada do PGSharp. Os dados ficam no aparelho e são marcados como importação "
                + "pessoal — não viram feed ao vivo.");
        hint.setTextSize(13);
        hint.setTextColor(MapTheme.muted());
        root.addView(hint);

        EditText input = new EditText(ui);
        input.setHint("-23.550520, -46.633310");
        input.setTextSize(14);
        input.setGravity(Gravity.TOP | Gravity.START);
        input.setMinLines(6);
        root.addView(input, new LinearLayout.LayoutParams(-1, MapTheme.dp(ui, 180)));

        final TextView report = new TextView(ui);
        report.setTextSize(13);
        report.setTextColor(MapTheme.muted());
        report.setText("Nenhum dado importado ainda.");
        ScrollView reportScroll = new ScrollView(ui);
        reportScroll.addView(report);
        root.addView(reportScroll, new LinearLayout.LayoutParams(-1, MapTheme.dp(ui, 140)));

        AlertDialog dialog = new AlertDialog.Builder(ui)
                .setTitle("Importar dados locais")
                .setView(root)
                .setNegativeButton("Fechar", null)
                .setNeutralButton("Colar", null)
                .setPositiveButton("Importar", null)
                .create();
        DialogLifecycle.bind(activity, dialog, null);
        dialog.setOnShowListener(ignored -> {
            MapTheme.style(root);
            MapTheme.styleButtons(dialog);
            Button paste = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
            Button confirm = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            confirm.setOnClickListener(v -> {
                String text = input.getText().toString();
                if (text.trim().isEmpty()) {
                    report.setText("Nada para importar: cole ou digite os dados primeiro.");
                    return;
                }
                confirm.setEnabled(false);
                final Handler main = new Handler(Looper.getMainLooper());
                new Thread(() -> {
                    // A PGSharp export is a filters document, not an events document: it becomes local
                    // presets instead of rows, and no feed is enabled by it.
                    final PgSharpFilterMapping.MappingReport mapped = mapPgSharpConfig(text);
                    if (mapped != null) {
                        int saved = 0;
                        for (PresetCodec.Preset preset : mapped.presets)
                            if (SourceDataStore.upsertPreset(activity, preset) != null) saved++;
                        final int stored = saved;
                        main.post(() -> {
                            if (activity.isFinishing() || activity.isDestroyed()) return;
                            confirm.setEnabled(true);
                            report.setText(mapped.summary() + "\nPresets guardados no aparelho: " + stored
                                    + ". Nenhum teleporte foi feito.");
                            if (stored > 0) {
                                Toast.makeText(activity, "Filtros do PGSharp convertidos: " + stored
                                        + " preset(s) local(is).", Toast.LENGTH_SHORT).show();
                                if (onImported != null) onImported.run();
                            }
                        });
                        return;
                    }
                    final LocalEventImport.Report result = LocalEventImport.parse(text,
                            System.currentTimeMillis(), LocalEventImport.IMPORT_SOURCE);
                    final LocalEventStore store = SourceDataStore.events(activity, System.currentTimeMillis());
                    final int added = store.addAll(result.events);
                    SourceDataStore.saveEvents(activity, store);
                    main.post(() -> {
                        if (activity.isFinishing() || activity.isDestroyed()) return;
                        confirm.setEnabled(true);
                        report.setText(result.summary() + "\nRegistros guardados: " + added
                                + " (total no aparelho: " + store.size() + ").");
                        if (result.imported > 0) {
                            Toast.makeText(activity, "Importação concluída: "
                                    + result.imported + " registro(s).", Toast.LENGTH_SHORT).show();
                            if (onImported != null) onImported.run();
                        }
                    });
                }, "pgbr-local-import").start();
            });
            paste.setOnClickListener(v -> {
                try {
                    android.content.ClipboardManager clipboard = (android.content.ClipboardManager)
                            activity.getSystemService(Context.CLIPBOARD_SERVICE);
                    android.content.ClipData clip = clipboard == null ? null : clipboard.getPrimaryClip();
                    if (clip == null || clip.getItemCount() == 0) {
                        Toast.makeText(activity, "Área de transferência vazia.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    CharSequence text = clip.getItemAt(0).getText();
                    if (text != null) input.setText(text);
                } catch (Exception error) {
                    Toast.makeText(activity, "Não foi possível ler a área de transferência.",
                            Toast.LENGTH_SHORT).show();
                }
            });
        });
        dialog.show();
    }

    /**
     * Returns the mapping when {@code text} is a PGSharp filters document, or {@code null} when it is
     * anything else (rows, coordinates, GPX, invalid JSON). Only the filter families this version
     * understands are converted; every other key stays in {@code raw_source} and is reported.
     */
    private static PgSharpFilterMapping.MappingReport mapPgSharpConfig(String text) {
        if (text == null) return null;
        String trimmed = text.trim();
        if (!trimmed.startsWith("{")) return null;
        JSONObject document;
        try {
            document = new JSONObject(trimmed);
        } catch (Exception notJson) {
            return null;
        }
        if (!document.has("pokemonFilters") && !document.has("questFilters")
                && !document.has("raidFilters")) return null;
        return PgSharpFilterMapping.map(document);
    }

    /** Text used by the export-selection action: only rows with valid coordinates. */
    static String gpxOf(List<SourceEvent> events) {
        return LocalEventImport.toGpx(events);
    }
}
