package dev.pgrazor;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.json.JSONObject;

/**
 * Opt-in, read-only client for an HTTPS RocketMap server controlled or
 * explicitly authorised by its operator. The URL alone cannot create
 * scanner coverage. No credentials or host bypass included.
 */
final class RocketMapFeedClient {
    private volatile HttpURLConnection active;
    private volatile boolean cancelled;
    private static final int MAX_BYTES = 2 * 1024 * 1024;

    static String validateBaseUrl(String input) {
        if (input == null) throw new IllegalArgumentException("Informe o endereço do servidor.");
        String value = input.trim();
        if (value.isEmpty() || value.length() > 300)
            throw new IllegalArgumentException("Endereço do servidor inválido.");
        try {
            URI parsed = URI.create(value);
            if (!"https".equalsIgnoreCase(parsed.getScheme()) || parsed.getHost() == null
                    || parsed.getHost().isEmpty() || parsed.getRawUserInfo() != null
                    || parsed.getRawQuery() != null || parsed.getRawFragment() != null)
                throw new IllegalArgumentException("Use o endereço HTTPS, sem login, query ou fragmento.");
            String path = parsed.getRawPath();
            if (path != null && (path.contains("..") || path.contains("\\")))
                throw new IllegalArgumentException("Caminho de servidor inválido.");
            while (value.endsWith("/")) value = value.substring(0,value.length()-1);
            return value;
        } catch (IllegalArgumentException e) {
            if (e.getMessage() != null && (e.getMessage().startsWith("Use o endereço")
                    || e.getMessage().startsWith("Caminho de servidor"))) throw e;
            throw new IllegalArgumentException("Informe URL HTTPS válida do seu RocketMap.");
        }
    }

    LiveFeedData.Snapshot fetch(String baseUrl, double lat, double lon,
                               double radiusKm, boolean raidsOnly) throws Exception {
        return fetch(baseUrl,lat,lon,radiusKm,raidsOnly,null);
    }

    /** Optional per-session read token for an operator-controlled HTTPS gateway. */
    LiveFeedData.Snapshot fetch(String baseUrl, double lat, double lon,
                               double radiusKm, boolean raidsOnly, String readSecret) throws Exception {
        String base = validateBaseUrl(baseUrl);
        if (readSecret != null && !readSecret.isEmpty()
                && (readSecret.length() < 24 || readSecret.length() > 256
                    || !readSecret.matches("[\\x21-\\x7E]+")))
            throw new LiveFeedClient.FeedException("Token de leitura inválido.");
        if (!Double.isFinite(lat) || !Double.isFinite(lon) || Math.abs(lat) > 90
                || Math.abs(lon) > 180)
            throw new LiveFeedClient.FeedException("Localização inválida.");
        cancelled = false;
        // RocketMap returns entries within a viewport (not a global search).
        double km = Double.isFinite(radiusKm) ? Math.max(0.5, Math.min(25, radiusKm)) : 25;
        double dLat = km / 111.32;
        double dLon = km / Math.max(1.0, 111.32 * Math.abs(Math.cos(Math.toRadians(lat))));
        double swLat = Math.max(-90, lat-dLat), neLat = Math.min(90,lat+dLat);
        double swLon = Math.max(-180, lon-dLon), neLon = Math.min(180, lon+dLon);
        String qs = String.format(Locale.US,
                "?swLat=%.6f&swLng=%.6f&neLat=%.6f&neLng=%.6f"
                + "&pokemon=%s&gyms=%s&pokestops=false&scanned=false&spawnpoints=false",
                swLat,swLon,neLat,neLon,raidsOnly?"false":"true",raidsOnly?"true":"false");
        HttpURLConnection conn = (HttpURLConnection)new URL(base+"/raw_data"+qs).openConnection();
        active = conn;
        try {
            conn.setInstanceFollowRedirects(false);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(15000);
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept","application/json");
            if (readSecret != null && !readSecret.isEmpty())
                conn.setRequestProperty("X-Feed-Secret", readSecret);
            conn.setRequestProperty("User-Agent","PGBR/0.00.26");
            int status = conn.getResponseCode();
            if (status == 401 || status == 403)
                throw new LiveFeedClient.FeedException(
                        "RocketMap recusou acesso (HTTP " + status
                                + "). Solicite permissão ao administrador.", status, 0);
            if (status == 429)
                throw new LiveFeedClient.FeedException(
                        "RocketMap limitou consultas. Aguarde.", status, 60000);
            if (status < 200 || status >= 300)
                throw new LiveFeedClient.FeedException("RocketMap HTTP " + status, status, 0);
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try(InputStream in=conn.getInputStream()) {
                byte[] buf = new byte[8192];
                int n;
                while ((n=in.read(buf))>=0) {
                    if (cancelled || Thread.currentThread().isInterrupted())
                        throw new LiveFeedClient.FeedException("Consulta cancelada.");
                    if (bytes.size()+n > MAX_BYTES)
                        throw new LiveFeedClient.FeedException("Resposta grande demais para o PGBR.");
                    bytes.write(buf,0,n);
                }
            }
            JSONObject parsed;
            try {parsed = new JSONObject(new String(bytes.toByteArray(),StandardCharsets.UTF_8));}
            catch(Exception ex) {throw new LiveFeedClient.FeedException("RocketMap retornou JSON inválido.");}
            try {return RocketMapFeedData.parse(parsed,System.currentTimeMillis(),raidsOnly);}
            catch(Exception ex) {throw new LiveFeedClient.FeedException(
                    "Formato RocketMap incompatível; confira a versão do seu servidor.");}
        } finally {
            conn.disconnect();
            if (active == conn) active = null;
        }
    }

    void cancel() {
        cancelled = true;
        HttpURLConnection running = active;
        if (running != null) running.disconnect();
    }
}
