package dev.pgrazor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.json.JSONObject;

/**
 * Read-only iFlowGo nearby feed for Pokémon and raids.
 *
 * Uses the observed public `/iflowgopokecoords/api/v1/nearby` contract,
 * never a PGSharp private endpoint. No credentials, spoofed browser headers,
 * pagination sweeps, auth bypass, or requests outside the selected area.
 * Provider may restrict/disable access; that must be respected.
 */
final class IFlowGoFeedClient {
    private static final String ENDPOINT =
            "https://pokecoords.iflowgo.com/iflowgopokecoords/api/v1/nearby";
    private static final int MAX_BYTES = 1024 * 1024;
    private static final int TIMEOUT_MS = 15000;
    private volatile HttpURLConnection active;
    private volatile boolean cancelled;

    LiveFeedData.Snapshot fetch(double lat, double lon, double radiusKm, boolean raids) throws Exception {
        if (!Double.isFinite(lat) || !Double.isFinite(lon) || lat < -90 || lat > 90
                || lon < -180 || lon > 180)
            throw new LiveFeedClient.FeedException("Coordenadas inválidas para iFlowGo.");
        cancelled = false;
        double radius = Double.isInfinite(radiusKm) && radiusKm > 0
                ? 25.0 : Math.max(1.0, Math.min(radiusKm, 25.0));
        String query = "?lat=" + enc(String.format(Locale.US, "%.6f", lat))
                + "&lon=" + enc(String.format(Locale.US, "%.6f", lon))
                + "&radius_km=" + enc(String.format(Locale.US, "%.1f", radius))
                + "&layers=" + (raids ? "raids" : "spawns")
                + "&limit=200";
        return doFetch(ENDPOINT + query, raids, false);
    }

    /** One page of the observed world search endpoint; no automated page sweeps. */
    LiveFeedData.Snapshot fetchGlobal() throws Exception {
        cancelled = false;
        return doFetch("https://pokecoords.iflowgo.com/iflowgopokecoords/api/v1/pokemon-search?page=1",
                false, true);
    }

    private LiveFeedData.Snapshot doFetch(String target, boolean raids, boolean global) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(target).openConnection();
        active = conn;
        try {
            conn.setConnectTimeout(TIMEOUT_MS);
            conn.setReadTimeout(TIMEOUT_MS);
            conn.setInstanceFollowRedirects(false);
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "PGBR/0.00.25 (+https://github.com/LucianoToscano/PGBR)");
            conn.setRequestProperty("Accept", "application/json");
            int status = conn.getResponseCode();
            if (status == 401 || status == 403)
                throw new LiveFeedClient.FeedException(
                        "iFlowGo recusou acesso (HTTP " + status
                                + "). Necessária autorização do fornecedor.", status, 0);
            if (status == 429)
                throw new LiveFeedClient.FeedException(
                        "iFlowGo: limite de consultas. Respeitando intervalo informado.",
                        status, retryAfter(conn));
            if (status < 200 || status >= 300)
                throw new LiveFeedClient.FeedException(
                        "iFlowGo indisponível (HTTP " + status + ").", status, 0);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try (InputStream in = conn.getInputStream()) {
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) != -1) {
                    if (cancelled || Thread.currentThread().isInterrupted())
                        throw new LiveFeedClient.FeedException("Busca iFlowGo cancelada.");
                    if (out.size() + n > MAX_BYTES)
                        throw new LiveFeedClient.FeedException("iFlowGo: resposta excedeu o limite.");
                    out.write(buf, 0, n);
                }
            }
            JSONObject payload;
            try { payload = new JSONObject(new String(out.toByteArray(), StandardCharsets.UTF_8)); }
            catch (Exception e) { throw new LiveFeedClient.FeedException("iFlowGo retornou JSON inválido."); }
            try { return global
                    ? IFlowGoFeedData.parseGlobal(payload, System.currentTimeMillis())
                    : IFlowGoFeedData.parse(payload, System.currentTimeMillis(), raids); }
            catch (Exception e) {
                throw new LiveFeedClient.FeedException("iFlowGo retornou campos desconhecidos nesta versão.");
            }
        } catch (IOException e) {
            if (cancelled) throw new LiveFeedClient.FeedException("Busca iFlowGo cancelada.");
            throw new LiveFeedClient.FeedException("Não foi possível acessar iFlowGo. Verifique a rede.");
        } finally {
            conn.disconnect();
            if (active == conn) active = null;
        }
    }

    void cancel() {
        cancelled = true;
        HttpURLConnection conn = active;
        if (conn != null) conn.disconnect();
    }

    private static String enc(String s) throws Exception {
        return URLEncoder.encode(s, "UTF-8");
    }

    private static long retryAfter(HttpURLConnection conn) {
        try {
            long seconds = Long.parseLong(conn.getHeaderField("Retry-After").trim());
            return Math.max(60000L, Math.min(3600000L, seconds * 1000L));
        } catch (Exception ignored) { return 60000L; }
    }
}
