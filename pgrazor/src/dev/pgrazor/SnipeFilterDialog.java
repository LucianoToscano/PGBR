package dev.pgrazor;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Editor for one Snipe slot. It never talks to the network: it only turns user text into a validated
 * {@link SnipeFilters} and hands it to the caller, which persists it and re-renders that slot alone.
 *
 * <p>Validation failures keep the dialog open and show the exact reason. Empty numeric fields mean
 * "no bound" rather than zero, so a blank level field never silently hides every result.
 */
public final class SnipeFilterDialog {
    private SnipeFilterDialog() {}

    /** Receives the accepted filters for one slot; the caller persists and re-renders. */
    public interface ApplyHandler { void apply(SnipeFilters filters); }

    public static void show(final Activity activity, final String slotTitle, SnipeFilters current,
                            final ApplyHandler onApply) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed() || onApply == null) return;
        final Context ui = MapTheme.wrap(activity);
        final SnipeFilters base = current == null ? SnipeFilters.defaults() : current;
        int pad = MapTheme.dp(ui, 18);
        LinearLayout root = new LinearLayout(ui);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, MapTheme.dp(ui, 8), pad, MapTheme.dp(ui, 8));

        final CheckBox enabled = new CheckBox(ui);
        enabled.setText("Ligar " + slotTitle);
        enabled.setTextColor(MapTheme.ink());
        enabled.setChecked(base.enabled());
        root.addView(enabled);

        final TextView hint = new TextView(ui);
        hint.setTextColor(MapTheme.muted());
        hint.setTextSize(12);
        hint.setText("Filtros aplicados somente ao que a fonte já declara. Campo vazio = sem limite; "
                + "valor que a fonte não informa (IV, nível, forma) nunca entra no resultado.");
        hint.setPadding(0, 0, 0, MapTheme.dp(ui, 8));
        root.addView(hint);

        final EditText species = field(ui, root, "Espécies (nº da Pokédex, vazio = todas)",
                joinSpecies(base.species()));
        final EditText minIv = field(ui, root, "IV mínimo (0–100)", bounded(base.ivBounded(), base.minIv()));
        final EditText maxIv = field(ui, root, "IV máximo (0–100)", bounded(base.ivBounded(), base.maxIv()));
        final EditText minLevel = field(ui, root, "Nível mínimo (1–50)",
                bounded(base.levelBounded(), base.minLevel()));
        final EditText maxLevel = field(ui, root, "Nível máximo (1–50)",
                bounded(base.levelBounded(), base.maxLevel()));
        final EditText form = field(ui, root, "Forma exata (vazio = qualquer)",
                base.form() == SnipeFilters.FORM_ANY ? "" : Integer.toString(base.form()));
        final EditText remaining = field(ui, root, "Tempo mínimo restante em segundos (vazio = 0)",
                base.minRemainingSeconds() == 0 ? "" : Integer.toString(base.minRemainingSeconds()));

        final AlertDialog dialog = new AlertDialog.Builder(ui)
                .setTitle("Filtros · " + slotTitle)
                .setView(root)
                .setPositiveButton("Aplicar", null)   // overridden: invalid text must not dismiss.
                .setNegativeButton("Cancelar", null)
                .create();
        dialog.setOnShowListener(d -> {
            MapTheme.style(root);
            MapTheme.styleButtons(dialog);
            Button apply = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            apply.setOnClickListener(v -> {
                try {
                    SnipeFilters parsed = SnipeFilters.fromEditor(
                            enabled.isChecked(), species.getText().toString(),
                            minIv.getText().toString(), maxIv.getText().toString(),
                            minLevel.getText().toString(), maxLevel.getText().toString(),
                            form.getText().toString(), remaining.getText().toString());
                    onApply.apply(parsed);
                    dialog.dismiss();
                } catch (JSONException invalid) {
                    String message = invalid.getMessage();
                    hint.setText(message == null || message.trim().isEmpty()
                            ? "Valor inválido. Confira os campos." : message);
                    hint.setTextColor(0xFFD24B4B);
                }
            });
        });
        DialogLifecycle.bind(activity, dialog, null);
        dialog.show();
    }

    /** Appends one labelled, single-line numeric field to {@code root}. */
    private static EditText field(Context ui, LinearLayout root, String label, String value) {
        TextView caption = new TextView(ui);
        caption.setText(label);
        caption.setTextSize(12);
        caption.setTextColor(MapTheme.muted());
        caption.setPadding(0, MapTheme.dp(ui, 6), 0, 0);
        root.addView(caption);
        EditText input = new EditText(ui);
        input.setSingleLine(true);
        input.setTextSize(15);
        input.setText(value);
        input.setTextColor(MapTheme.ink());
        input.setGravity(Gravity.START);
        root.addView(input, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                MapTheme.dp(ui, 44)));
        return input;
    }

    private static String bounded(boolean bounded, int value) {
        return bounded ? Integer.toString(value) : "";
    }

    private static String joinSpecies(java.util.List<Integer> ids) {
        if (ids == null || ids.isEmpty()) return "";
        StringBuilder s = new StringBuilder();
        for (Integer id : ids) {
            if (s.length() > 0) s.append(", ");
            s.append(id.intValue());
        }
        return s.toString();
    }

    // ------------------------------------------------------------------ persistence

    /**
     * Preferences file owned by the snipe slots; no other feature reads or writes it. Each slot owns
     * one key ({@code sniper1}/{@code sniper2}), written by that slot's editor only, so a corrupt or
     * absent entry for one slot can never overwrite the other slot's saved filters.
     */
    static SharedPreferences preferences(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences("pgrazor-snipe-filters", Context.MODE_PRIVATE);
    }

    /**
     * Reads one slot. A missing, corrupt or unparsable entry falls back to {@link SnipeFilters#defaults()}
     * for that slot only; it never borrows the other slot's values and never throws into the UI.
     */
    static SnipeFilters load(SharedPreferences prefs, String slot) {
        if (prefs == null) return SnipeFilters.defaults();
        try {
            String stored = prefs.getString(slot, null);
            if (stored == null || stored.trim().isEmpty()) return SnipeFilters.defaults();
            JSONObject json = new JSONObject(stored);
            return SnipeFilters.containsAnyKey(json) ? SnipeFilters.fromJson(json)
                    : SnipeFilters.defaults();
        } catch (JSONException corrupt) {
            return SnipeFilters.defaults();
        }
    }
}
