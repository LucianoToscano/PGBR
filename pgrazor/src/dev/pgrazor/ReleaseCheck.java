package dev.pgrazor;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.widget.Button;
import android.widget.Toast;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import org.json.JSONArray;
import org.json.JSONObject;

/** Explicit user-initiated check. No installation, storage or new permissions. */
final class ReleaseCheck {
    private ReleaseCheck() {}

    static void check(Activity activity, Button button) {
        if (!button.isEnabled()) return;
        button.setEnabled(false);
        button.setText("Verificando atualização…");
        new Thread(() -> {
            String message, page = ReleaseCheckRules.REPOSITORY;
            boolean available = false;
            try {
                JSONObject release = readRelease();
                String tag = release.optString("tag_name", "");
                String wanted = ReleaseCheckRules.asset(tag, PgAssets.rooted());
                if (ReleaseCheckRules.version(tag) < 0 || ReleaseCheckRules.version(BuildVersion.NAME) < 0 ||
                    release.optBoolean("draft", true) || release.optBoolean("prerelease", true)) {
                    message = "Não foi encontrada uma versão estável compatível.";
                } else if (!ReleaseCheckRules.newer(tag, BuildVersion.NAME)) {
                    message = "Nenhuma versão estável mais recente foi encontrada.";
                } else {
                    JSONArray assets = release.optJSONArray("assets");
                    int apk = 0, checksums = 0;
                    for (int i = 0; assets != null && i < assets.length(); i++) {
                        JSONObject item = assets.optJSONObject(i);
                        if (item == null) continue;
                        String name = item.optString("name", "");
                        if (!ReleaseCheckRules.trustedAsset(tag, name, item.optString("browser_download_url", ""))) continue;
                        if (item.optLong("size", 0) <= 0) continue;
                        if (name.equals(wanted)) apk++;
                        if (name.equals("SHA256SUMS.txt")) checksums++;
                    }
                    available = apk == 1 && checksums == 1;
                    if (available) {
                        page = ReleaseCheckRules.releasePage(tag);
                        message = "Nova versão estável: " + tag + "\nPacote: " + wanted +
                            "\nConfira o SHA-256 publicado e a assinatura antes de instalar. A instalação permanece manual.";
                    } else message = "A versão encontrada não possui os arquivos esperados para sua variante.";
                }
            } catch (Exception failure) {
                message = "Não foi possível consultar as versões oficiais agora.";
            }
            final String detail = message, target = page;
            final boolean found = available;
            activity.runOnUiThread(() -> {
                button.setEnabled(true);
                button.setText("Verificar atualização do PGBR");
                if (activity.isFinishing() || activity.isDestroyed()) return;
                new AlertDialog.Builder(activity).setTitle(found ? "Atualização disponível" : "Atualizações do PGBR")
                    .setMessage(detail).setNegativeButton("Fechar", null)
                    .setPositiveButton("Ver versões oficiais", (dialog, which) -> {
                        try { activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(target))); }
                        catch (android.content.ActivityNotFoundException e) {
                            Toast.makeText(activity, "Nenhum navegador disponível.", Toast.LENGTH_SHORT).show();
                        }
                    }).show();
            });
        }, "pgbr-release-check").start();
    }

    private static JSONObject readRelease() throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(ReleaseCheckRules.API).openConnection();
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(10000);
        connection.setInstanceFollowRedirects(false);
        connection.setRequestProperty("Accept", "application/vnd.github+json");
        try {
            if (connection.getResponseCode() != 200) throw new java.io.IOException("HTTP");
            try (InputStream input = connection.getInputStream(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[4096];
                for (int count; (count = input.read(buffer)) != -1;) {
                    if (output.size() + count > 512 * 1024) throw new java.io.IOException("Response too large");
                    output.write(buffer, 0, count);
                }
                return new JSONObject(output.toString("UTF-8"));
            }
        } finally { connection.disconnect(); }
    }
}
