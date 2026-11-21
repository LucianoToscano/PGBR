package dev.pgrazor;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView;
import android.view.View;

/** Collection-search helper: generates copyable strings, no game input automation. */
final class PokemonInventorySearchDialog {
    private PokemonInventorySearchDialog() {}

    static void show(Activity activity) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;
        ScrollView scroll = new ScrollView(activity);
        LinearLayout body = new LinearLayout(activity);
        body.setOrientation(LinearLayout.VERTICAL);
        float density = activity.getResources().getDisplayMetrics().density;
        int pad = Math.round(density * 16);
        body.setPadding(pad, pad, pad, pad);
        scroll.addView(body);

        TextView intro = text(activity, "Crie uma busca para colar na coleção do Pokémon GO. "
                + "O PGBR não altera Pokémon nem acessa sua conta.", 14);
        body.addView(intro);

        body.addView(text(activity, "Avaliação / IV", 15));
        Spinner base = new Spinner(activity);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(activity,
                android.R.layout.simple_spinner_item, PokemonInventorySearchBuilder.BASE_LABELS);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        base.setAdapter(adapter);
        body.addView(base);

        CheckBox shiny = check(activity, body, "Shiny");
        CheckBox shadow = check(activity, body, "Sombrio");
        CheckBox xxl = check(activity, body, "XXL");
        CheckBox background = check(activity, body, "Fundo especial");
        CheckBox legacy = check(activity, body, "Ataque especial (@special)");

        body.addView(text(activity, "Pokédex (opcional, número 1–1025)", 14));
        EditText dex = new EditText(activity);
        dex.setSingleLine(true);
        dex.setInputType(InputType.TYPE_CLASS_NUMBER);
        dex.setFilters(new InputFilter[]{new InputFilter.LengthFilter(4)});
        dex.setHint("Ex.: 150");
        body.addView(dex);

        TextView result = text(activity, "", 15);
        result.setTextIsSelectable(true);
        result.setPadding(0, pad, 0, pad);
        result.setGravity(Gravity.START);
        body.addView(result);
        body.addView(text(activity, "Os termos em inglês (shiny, shadow, xxl, background e @special) "
                + "podem precisar de tradução conforme o idioma definido no Pokémon GO. "
                + "0/15/15 não garante Rank 1 PvP; depende da espécie e da liga.", 12));

        Runnable refresh = () -> {
            try {
                String generated = PokemonInventorySearchBuilder.build(base.getSelectedItemPosition(),
                        shiny.isChecked(), shadow.isChecked(), xxl.isChecked(),
                        background.isChecked(), legacy.isChecked(), dex.getText().toString());
                result.setText(generated.isEmpty()
                        ? "Selecione pelo menos um filtro para gerar a busca." : generated);
            } catch (IllegalArgumentException bad) {
                result.setText(bad.getMessage());
            }
        };

        for (CheckBox cb : new CheckBox[]{shiny, shadow, xxl, background, legacy})
            cb.setOnCheckedChangeListener((button, checked) -> refresh.run());
        base.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) { refresh.run(); }
            public void onNothingSelected(AdapterView<?> parent) { refresh.run(); }
        });
        dex.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            public void onTextChanged(CharSequence s, int start, int before, int count) { refresh.run(); }
            public void afterTextChanged(Editable s) { }
        });
        refresh.run();

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle("Busca inteligente · Coleção")
                .setView(scroll)
                .setNegativeButton("Fechar", null)
                .setPositiveButton("Copiar busca", null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(view -> {
                    String query;
                    try {
                        query = PokemonInventorySearchBuilder.build(base.getSelectedItemPosition(),
                                shiny.isChecked(), shadow.isChecked(), xxl.isChecked(),
                                background.isChecked(), legacy.isChecked(), dex.getText().toString());
                    } catch (IllegalArgumentException invalid) {
                        Toast.makeText(activity, invalid.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (query.isEmpty()) {
                        Toast.makeText(activity, "Escolha ao menos um critério.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    ClipboardManager clipboard = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
                    if (clipboard != null) {
                        clipboard.setPrimaryClip(ClipData.newPlainText("Busca Pokémon GO", query));
                        Toast.makeText(activity, "Busca copiada. Cole na coleção do jogo.", Toast.LENGTH_LONG).show();
                    }
                }));
        dialog.show();
    }

    private static CheckBox check(Activity activity, LinearLayout parent, String label) {
        CheckBox box = new CheckBox(activity);
        box.setText(label);
        box.setTextSize(14);
        box.setMinHeight(Math.round(activity.getResources().getDisplayMetrics().density * 44));
        parent.addView(box);
        return box;
    }

    private static TextView text(Activity activity, String label, int sp) {
        TextView t = new TextView(activity);
        t.setText(label);
        t.setTextSize(sp);
        return t;
    }
}
