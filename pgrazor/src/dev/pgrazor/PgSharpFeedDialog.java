package dev.pgrazor;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.text.InputType;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * PGSharp is a separate provider choice. The free tier needs no key; the
 * Standard key can be saved with explicit consent but remains UNVERIFIED.
 * No guessed HTTP request or auth endpoint is called.
 */
public final class PgSharpFeedDialog {
    private PgSharpFeedDialog() {}

    public static void show(Activity activity) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;
        Context ui = MapTheme.wrap(activity);
        LinearLayout root = new LinearLayout(ui);
        root.setOrientation(LinearLayout.VERTICAL);
        int p = MapTheme.dp(ui, 14);
        root.setPadding(p, p, p, p);
        AlertDialog dialog = new AlertDialog.Builder(ui)
                .setTitle("PGSharp · Fontes e licença")
                .setView(root)
                .setNegativeButton("Fechar", null).create();
        Panel panel = new Panel(activity, ui, dialog, root);
        DialogLifecycle.bind(activity, dialog, panel::close);
        dialog.setOnShowListener(d -> {
            MapTheme.style(root);
            MapTheme.styleButtons(dialog);
        });
        dialog.show();
    }

    private static final class Panel {
        private final Activity activity;
        private final Context ui;
        private final AlertDialog dialog;
        private final ExecutorService worker = Executors.newSingleThreadExecutor(job -> {
            Thread t = new Thread(job, "pgbr-pgsharp-key-vault");
            t.setDaemon(true);
            return t;
        });
        private final PgSharpLicenseVault vault;
        private final TextView licenseStatus;
        private volatile boolean closed;

        Panel(Activity activity, Context ui, AlertDialog dialog, LinearLayout root) {
            this.activity = activity;
            this.ui = ui;
            this.dialog = dialog;
            PgSharpLicenseVault temporary;
            try { temporary = new PgSharpLicenseVault(activity); }
            catch (IOException e) { temporary = null; }
            vault = temporary;

            root.addView(text("100% IV grátis", 17));
            root.addView(text(PgSharpFeedPolicy.freeStateText(), 13));
            TextView warning = text("A integração direta com f.pgsharp.com "
                    + "ainda não tem resposta/assinatura de requisição verificadas. "
                    + "O PGBR não mostrará Pokémon de outra origem com o nome PGSharp.", 12);
            warning.setTextColor(MapTheme.muted());
            root.addView(warning);

            Button fallback = new Button(ui);
            fallback.setText("Abrir Sniper 100% IV do PGBR");
            fallback.setOnClickListener(v -> {
                if (closed) return;
                dialog.dismiss();
                HundoFeedDialog.show(activity);
            });
            root.addView(fallback, new LinearLayout.LayoutParams(-1, MapTheme.dp(ui, 48)));

            root.addView(text("Filtros avançados · PGSharp Standard", 17));
            root.addView(text("Quando houver conector verificado, os filtros adicionais "
                    + "dependerão das permissões da licença. A key cadastrada não "
                    + "ativa acesso nem autentica a conta por si só.", 13));

            licenseStatus = text("", 13);
            root.addView(licenseStatus);
            Button storeKey = new Button(ui);
            storeKey.setText("Guardar minha PGSharp key (somente no aparelho)");
            storeKey.setEnabled(vault != null);
            storeKey.setOnClickListener(v -> showSavePrompt());
            root.addView(storeKey, new LinearLayout.LayoutParams(-1, MapTheme.dp(ui, 48)));

            Button forgetKey = new Button(ui);
            forgetKey.setText("Apagar minha key deste aparelho");
            forgetKey.setEnabled(vault != null);
            forgetKey.setOnClickListener(v -> removeKey());
            root.addView(forgetKey, new LinearLayout.LayoutParams(-1, MapTheme.dp(ui, 48)));

            TextView note = text("Privacidade: chave protegida pelo Android Keystore e "
                    + "armazenada fora do backup. Não é transmitida, nem exibida em logs. "
                    + "O PGBR não solicita sua senha de Pokémon GO.", 12);
            note.setTextColor(MapTheme.muted());
            root.addView(note);
            updateStatus();
        }

        private TextView text(String value, int size) {
            TextView v = new TextView(ui);
            v.setText(value);
            v.setTextSize(size);
            v.setTextColor(MapTheme.ink());
            v.setPadding(0, MapTheme.dp(ui, 8), 0, MapTheme.dp(ui, 8));
            return v;
        }

        private void updateStatus() {
            if (closed) return;
            boolean present = vault != null && vault.hasSavedKey();
            PgSharpFeedPolicy.KeyStatus state = present
                    ? PgSharpFeedPolicy.KeyStatus.STORED_UNVERIFIED
                    : PgSharpFeedPolicy.KeyStatus.ABSENT;
            licenseStatus.setText(PgSharpFeedPolicy.paidStateText(state));
        }

        private void showSavePrompt() {
            if (closed || vault == null) return;
            EditText keyInput = new EditText(ui);
            keyInput.setSingleLine(true);
            keyInput.setHint("PGSharp key");
            keyInput.setInputType(InputType.TYPE_CLASS_TEXT
                    | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            int pad = MapTheme.dp(ui, 16);
            keyInput.setPadding(pad, 0, pad, 0);
            AlertDialog prompt = new AlertDialog.Builder(ui)
                    .setTitle("Guardar key localmente")
                    .setMessage("Esta versão não verifica nem envia sua key. "
                            + "Ela será armazenada apenas no aparelho, de forma criptografada. "
                            + "Filtros pagos continuarão bloqueados até uma conexão validada.")
                    .setView(keyInput)
                    .setNegativeButton("Cancelar", (d, which) -> keyInput.setText(""))
                    .setPositiveButton("Guardar", (d, which) -> {
                        CharSequence entered = keyInput.getText();
                        char[] chars = new char[entered.length()];
                        for (int i = 0; i < chars.length; i++) chars[i] = entered.charAt(i);
                        keyInput.setText("");
                        if (closed) {
                            Arrays.fill(chars, '\0');
                            return;
                        }
                        try { worker.execute(() -> {
                            boolean saved = false;
                            try { vault.save(chars); saved = true; }
                            catch (IOException e) { /* no key content in exception/report */ }
                            finally { Arrays.fill(chars, '\0'); }
                            final boolean ok = saved;
                            activity.runOnUiThread(() -> {
                                if (closed || activity.isFinishing() || activity.isDestroyed()) return;
                                updateStatus();
                                Toast.makeText(activity, ok
                                        ? "Key protegida localmente; ainda não autenticada."
                                        : "Não foi possível guardar a key.", Toast.LENGTH_SHORT).show();
                            });
                        }); }
                        catch (java.util.concurrent.RejectedExecutionException ignore) {
                            Arrays.fill(chars, '\0');
                        }
                    }).create();
            DialogLifecycle.bind(activity, prompt, () -> keyInput.setText(""));
            prompt.setOnShowListener(d -> {
                if (prompt.getWindow() != null)
                    prompt.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
                MapTheme.styleButtons(prompt);
            });
            prompt.show();
        }

        private void removeKey() {
            if (closed || vault == null) return;
            new AlertDialog.Builder(ui)
                    .setTitle("Apagar PGSharp key?")
                    .setMessage("Apaga apenas a key opcional salva no PGBR. "
                            + "Não afeta sua licença nem sua conta oficial do PGSharp.")
                    .setNegativeButton("Cancelar", null)
                    .setPositiveButton("Apagar", (d, which) -> {
                        if (closed) return;
                        try { worker.execute(() -> {
                        boolean removed = false;
                        try { vault.remove(); removed = true; }
                        catch (IOException e) { /* no credential information */ }
                        final boolean ok = removed;
                        activity.runOnUiThread(() -> {
                            if (closed || activity.isFinishing() || activity.isDestroyed()) return;
                            updateStatus();
                            Toast.makeText(activity, ok ? "Key local apagada."
                                    : "Não foi possível apagar a key local.",
                                    Toast.LENGTH_SHORT).show();
                        });
                        }); } catch (java.util.concurrent.RejectedExecutionException ignore) { }
                    }).show();
        }

        private void close() {
            closed = true;
            worker.shutdown();
        }
    }
}
