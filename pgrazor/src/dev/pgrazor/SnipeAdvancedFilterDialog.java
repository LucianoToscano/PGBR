package dev.pgrazor;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

/**
 * Full filter editor for one sniper slot: the complete {@link SpawnFilters} surface, with every empty
 * field meaning "no bound" and every invalid value rejected without dismissing the dialog.
 *
 * <p>Two honesty rules are visible in the UI rather than buried in code:
 * <ul>
 *   <li>Setting a numeric bound that the selected source does not deliver hides rows, so the source's
 *       capability line is printed above the fields.</li>
 *   <li>"Regionais", "raros" and "evento" are shown disabled: this build has no validated list for
 *       them, so offering a checkbox would silently filter on nothing.</li>
 * </ul>
 */
public final class SnipeAdvancedFilterDialog {
    private SnipeAdvancedFilterDialog() {}

    /** Receives a validated filter set; the caller persists it and re-runs the search. */
    public interface Handler { void apply(SpawnFilters filters); }

    public static void show(final Activity activity, String slotTitle, final SpawnFilters current,
                            final String sourceId, final Handler handler) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed() || handler == null) return;
        final Context ui = MapTheme.wrap(activity);
        final SpawnFilters base = current == null ? SpawnFilters.defaults() : current;
        int pad = MapTheme.dp(ui, 14);

        LinearLayout root = new LinearLayout(ui);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, MapTheme.dp(ui, 6), pad, MapTheme.dp(ui, 6));

        SpawnContract.Capabilities capabilities = capabilitiesFor(sourceId);
        TextView sourceNote = new TextView(ui);
        sourceNote.setTextSize(11);
        sourceNote.setTextColor(MapTheme.muted());
        sourceNote.setText("Fonte: " + capabilities.label + "\nEntrega: " + describe(capabilities)
                + "\nUm limite definido usa somente valores que a fonte declarou; valor não informado "
                + "nunca entra no resultado.");
        root.addView(sourceNote);

        final EditText species = field(ui, root, "Espécies (nº da Pokédex, separados por vírgula)",
                joinSpecies(base.species()));
        final CheckBox perfect = check(ui, root, "Somente 100% IV (15/15/15 ou 100% declarado)", base.perfectOnly());
        final CheckBox zero = check(ui, root, "Somente 0% IV (0/0/0)", base.zeroOnly());
        final CheckBox knownIv = check(ui, root, "Exigir IV conhecido", false);
        final CheckBox coordinatesOnly = check(ui, root, "Somente com coordenadas válidas", true);

        final EditText minIv = field(ui, root, "IV mínimo (0–100)", bounded(base.minIv()));
        final EditText maxIv = field(ui, root, "IV máximo (0–100)", bounded(base.maxIv()));
        final EditText minAtk = field(ui, root, "Ataque mínimo (0–15)", "");
        final EditText maxAtk = field(ui, root, "Ataque máximo (0–15)", "");
        final EditText minDef = field(ui, root, "Defesa mínima (0–15)", "");
        final EditText maxDef = field(ui, root, "Defesa máxima (0–15)", "");
        final EditText minSta = field(ui, root, "Stamina mínima (0–15)", "");
        final EditText maxSta = field(ui, root, "Stamina máxima (0–15)", "");
        final EditText minCp = field(ui, root, "PC mínimo", bounded(base.minCp()));
        final EditText maxCp = field(ui, root, "PC máximo", bounded(base.maxCp()));
        final EditText minLevel = field(ui, root, "Nível mínimo (1–50)", bounded(base.minLevel()));
        final EditText maxLevel = field(ui, root, "Nível máximo (1–50)", bounded(base.maxLevel()));

        final Picker gender = picker(ui, root, "Gênero",
                new String[]{"Qualquer", "Macho", "Fêmea", "Sem gênero"}, genderIndex(base.gender()));
        final Picker size = picker(ui, root, "Tamanho",
                new String[]{"Qualquer", "XXS", "XXL"}, sizeIndex(base.size()));
        final EditText form = field(ui, root, "Forma exata (id numérico; vazio = qualquer)",
                base.form() == SpawnFilters.UNSET ? "" : Integer.toString(base.form()));
        final Picker league = picker(ui, root, "Liga PvP",
                new String[]{"Qualquer", "Grande Liga", "Ultra Liga", "Pequena Copa"}, leagueIndex(base.league()));
        final EditText maxRank = field(ui, root, "Ranking PvP máximo (1–4096)", bounded(base.maxRank()));
        final EditText maxDistance = field(ui, root, "Distância máxima de teleporte em km (vazio = sem limite)",
                base.maxDistanceKm() == SpawnFilters.UNSET ? ""
                        : Integer.toString((int) base.maxDistanceKm()));

        final CheckBox shinyPossible = check(ui, root, "Shiny possível (a espécie pode ser shiny)", false);
        final CheckBox shinyConfirmed = check(ui, root, "Shiny confirmado pela fonte", base.shinyConfirmedOnly());
        final CheckBox legendary = check(ui, root, "Somente lendários", false);
        final CheckBox mythical = check(ui, root, "Somente míticos", false);

        CheckBox disabledRegional = check(ui, root, "Regionais (indisponível: sem lista validada)", false);
        disabledRegional.setEnabled(false);
        CheckBox disabledRare = check(ui, root, "Raros (indisponível: sem definição validada)", false);
        disabledRare.setEnabled(false);
        CheckBox disabledEvent = check(ui, root, "Evento (indisponível: sem definição validada)", false);
        disabledEvent.setEnabled(false);

        TextView note = new TextView(ui);
        note.setTextSize(11);
        note.setTextColor(MapTheme.muted());
        note.setPadding(0, MapTheme.dp(ui, 6), 0, 0);
        note.setText("Shiny possível não é shiny confirmado. O ranking PvP vem da fonte quando ela envia; "
                + "quando o PGBR calcular localmente, a linha é marcada como calculada.");
        root.addView(note);

        final AlertDialog dialog = new AlertDialog.Builder(ui)
                .setTitle("Filtros completos · " + slotTitle)
                .setView(new ScrollView(ui) {{ addView(root); }})
                .setPositiveButton("Aplicar", null)
                .setNegativeButton("Cancelar", null)
                .create();
        DialogLifecycle.bind(activity, dialog, null);
        dialog.setOnShowListener(d -> {
            MapTheme.style(root);
            MapTheme.styleButtons(dialog);
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                try {
                    SpawnFilters filters = new SpawnFilters.Builder()
                            .species(parseSpecies(species.getText().toString()))
                            .perfectOnly(perfect.isChecked())
                            .zeroOnly(zero.isChecked())
                            .requireKnownIv(knownIv.isChecked())
                            .coordinatesOnly(coordinatesOnly.isChecked())
                            .shinyPossibleOnly(shinyPossible.isChecked())
                            .shinyConfirmedOnly(shinyConfirmed.isChecked())
                            .groups(legendary.isChecked(), mythical.isChecked(), false, false, false)
                            .iv(number(minIv), number(maxIv))
                            .attack(number(minAtk), number(maxAtk))
                            .defense(number(minDef), number(maxDef))
                            .stamina(number(minSta), number(maxSta))
                            .cp(number(minCp), number(maxCp))
                            .level(number(minLevel), number(maxLevel))
                            .gender(genderValue(gender.index()))
                            .form(numberOrUnset(form))
                            .size(sizeValue(size.index()))
                            .league(leagueValue(league.index()), number(maxRank))
                            .distance(maxDistance.getText().toString().trim().isEmpty()
                                    ? SpawnFilters.UNSET
                                    : Double.parseDouble(maxDistance.getText().toString().trim()), 25)
                            .build();
                    handler.apply(filters);
                    dialog.dismiss();
                } catch (IllegalArgumentException invalid) {
                    // Keep the dialog open and name the exact problem; never accept a bad bound silently.
                    AlertDialog problem = new AlertDialog.Builder(ui).setTitle("Filtro inválido")
                            .setMessage(invalid.getMessage() == null ? "Valor inválido." : invalid.getMessage())
                            .setPositiveButton("Corrigir", null).create();
                    DialogLifecycle.bind(activity, problem, null);
                    problem.show();
                }
            });
        });
        dialog.show();
    }

    // -------------------------------------------------------------- helpers

    private static SpawnContract.Capabilities capabilitiesFor(String sourceId) {
        SpawnSourceRegistry.SourceInfo info = SpawnSourceRegistry.find(sourceId);
        return info != null && info.capabilities != null ? info.capabilities
                : SpawnContract.Capabilities.IFLOWGO;
    }

    private static String describe(SpawnContract.Capabilities c) {
        StringBuilder out = new StringBuilder("coordenadas, ");
        out.append(c.ivComponents ? "IV individual, " : "").append(c.ivPercent ? "IV %, " : "")
                .append(c.cp ? "PC, " : "").append(c.level ? "nível, " : "")
                .append(c.gender ? "gênero, " : "").append(c.form ? "forma, " : "")
                .append(c.size ? "tamanho, " : "").append(c.despawn ? "sumiço, " : "")
                .append(c.pvpRanks ? "rank PvP, " : "").append(c.serverSideSpecies ? "filtro de espécie na fonte" : "");
        String text = out.toString();
        while (text.endsWith(", ")) text = text.substring(0, text.length() - 2);
        return text;
    }

    private static String joinSpecies(java.util.List<Integer> ids) {
        StringBuilder out = new StringBuilder();
        for (Integer id : ids) {
            if (out.length() > 0) out.append(", ");
            out.append(id);
        }
        return out.toString();
    }

    private static java.util.List<Integer> parseSpecies(String text) {
        java.util.List<Integer> ids = new java.util.ArrayList<>();
        for (String token : text.split("[,;#\\s]+")) {
            String value = token.trim();
            if (value.isEmpty()) continue;
            int id;
            try {
                id = Integer.parseInt(value);
            } catch (NumberFormatException invalid) {
                throw new IllegalArgumentException("Espécie inválida: " + value + ".");
            }
            if (id < 1 || id > 1025) throw new IllegalArgumentException("Espécie fora da Pokédex: " + id + ".");
            if (!ids.contains(id)) ids.add(id);
        }
        return ids;
    }

    /** Empty text means "no bound", never zero. */
    static int number(EditText field) {
        return numberOrUnset(field);
    }

    private static int numberOrUnset(EditText field) {
        String text = field.getText().toString().trim();
        if (text.isEmpty()) return SpawnFilters.UNSET;
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException invalid) {
            throw new IllegalArgumentException("Valor numérico inválido: " + text + ".");
        }
    }

    private static String bounded(int value) {
        return value == SpawnFilters.UNSET ? "" : Integer.toString(value);
    }

    private static EditText field(Context ui, LinearLayout parent, String hint, String value) {
        EditText field = new EditText(ui);
        field.setHint(hint);
        field.setText(value);
        field.setTextSize(14);
        field.setSingleLine(true);
        parent.addView(field);
        return field;
    }

    private static CheckBox check(Context ui, LinearLayout parent, String label, boolean checked) {
        CheckBox box = new CheckBox(ui);
        box.setText(label);
        box.setTextSize(13);
        box.setTextColor(MapTheme.ink());
        box.setChecked(checked);
        parent.addView(box);
        return box;
    }

    /** A labelled picker; keeps the position of the user's choice as a plain int. */
    private static final class Picker {
        private final Spinner view;
        private final int last;

        Picker(Spinner view, int last) { this.view = view; this.last = last; }

        /** Current choice, clamped so a spinner without an adapter still answers a valid index. */
        int index() {
            int position = view.getSelectedItemPosition();
            return position < 0 ? Math.max(0, Math.min(last, position + 1)) : Math.min(last, position);
        }
    }

    private static Picker picker(Context ui, LinearLayout parent, String label, String[] options,
                                 int selected) {
        TextView caption = new TextView(ui);
        caption.setText(label);
        caption.setTextSize(12);
        caption.setTextColor(MapTheme.muted());
        caption.setPadding(0, MapTheme.dp(ui, 6), 0, 0);
        parent.addView(caption);
        Spinner spinner = new Spinner(ui);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(ui, android.R.layout.simple_spinner_item,
                new java.util.ArrayList<>(java.util.Arrays.asList(options)));
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(Math.max(0, Math.min(options.length - 1, selected)));
        parent.addView(spinner);
        return new Picker(spinner, options.length - 1);
    }

    private static int genderIndex(int value) {
        if (value == SpawnFilters.GENDER_MALE) return 1;
        if (value == SpawnFilters.GENDER_FEMALE) return 2;
        if (value == SpawnFilters.GENDER_GENDERLESS) return 3;
        return 0;
    }

    private static int genderValue(int index) {
        if (index == 1) return SpawnFilters.GENDER_MALE;
        if (index == 2) return SpawnFilters.GENDER_FEMALE;
        if (index == 3) return SpawnFilters.GENDER_GENDERLESS;
        return SpawnFilters.GENDER_ANY;
    }

    private static int sizeIndex(int value) {
        if (value == SpawnFilters.SIZE_XXS) return 1;
        if (value == SpawnFilters.SIZE_XXL) return 2;
        return 0;
    }

    private static int sizeValue(int index) {
        if (index == 1) return SpawnFilters.SIZE_XXS;
        if (index == 2) return SpawnFilters.SIZE_XXL;
        return SpawnFilters.SIZE_ANY;
    }

    private static int leagueIndex(int value) {
        if (value == SpawnFilters.LEAGUE_GREAT) return 1;
        if (value == SpawnFilters.LEAGUE_ULTRA) return 2;
        if (value == SpawnFilters.LEAGUE_LITTLE) return 3;
        return 0;
    }

    private static int leagueValue(int index) {
        if (index == 1) return SpawnFilters.LEAGUE_GREAT;
        if (index == 2) return SpawnFilters.LEAGUE_ULTRA;
        if (index == 3) return SpawnFilters.LEAGUE_LITTLE;
        return SpawnFilters.LEAGUE_ANY;
    }
}
