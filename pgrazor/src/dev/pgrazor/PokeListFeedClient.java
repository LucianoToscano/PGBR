package dev.pgrazor;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.json.JSONObject;

/**
 * Optional PokeList/Moonani connector based on the supplied 1.3.18 APK schema.
 * Not a PGSharp, Pokémod, PGTools or iPogo backend.
 *
 * The vendor may change or restrict this endpoint. Caller presents errors,
 * respects rate limiting and never attempts to bypass authentication.
 */
final class PokeListFeedClient {
    private static final String BASE = "https://moonani.com/PokeList/api.v2.php";
    private static final int MAX_RESPONSE = 2 * 1024 * 1024;
    private volatile HttpURLConnection active;
    private volatile boolean cancelled;

    LiveFeedData.Snapshot fetch(double lat,double lon,double radius) throws Exception {
        if (!Double.isFinite(lat) || !Double.isFinite(lon)
                || lat < -90 || lat > 90 || lon < -180 || lon > 180)
            throw new LiveFeedClient.FeedException("Coordenadas inválidas para a busca.");
        cancelled = false;
        // The analysis did not establish the unit accepted by distance.
        // The bounded values used here are experimental and must be verified.
        int distance = Math.max(1, Math.min(5, (int)Math.round(radius)));
        String query = "?type=pokemon&lat=" + enc(String.format(Locale.US,"%.6f",lat))
                + "&lng=" + enc(String.format(Locale.US,"%.6f",lon))
                + "&distance=" + distance;
        URL url = new URL(BASE + query);
        if (!"https".equals(url.getProtocol()))
            throw new LiveFeedClient.FeedException("HTTPS obrigatório para o feed.");
        HttpURLConnection http = (HttpURLConnection)url.openConnection();
        active = http;
        try {
            http.setConnectTimeout(10000);
            http.setReadTimeout(15000);
            http.setInstanceFollowRedirects(false);
            http.setRequestMethod("GET");
            http.setRequestProperty("Accept", "application/json");
            http.setRequestProperty("User-Agent","PGBR/0.1");
            int status = http.getResponseCode();
            if (status == 401 || status == 403)
                throw new LiveFeedClient.FeedException(
                        "PokeList não autorizou o acesso a esta fonte.", status, 0);
            if (status == 429)
                throw new LiveFeedClient.FeedException(
                        "Limite de consultas da PokeList atingido.",status,
                        Math.max(60000L, retryAfter(http)));
            if (status < 200 || status >= 300)
                throw new LiveFeedClient.FeedException(
                        "PokeList indisponível (HTTP " + status + ").",status,0);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try (InputStream in = http.getInputStream()) {
                byte[] buf = new byte[4096];
                int n;
                while ((n=in.read(buf))!=-1) {
                    if (cancelled || Thread.currentThread().isInterrupted())
                        throw new LiveFeedClient.FeedException("Busca cancelada.");
                    if (out.size()+n>MAX_RESPONSE)
                        throw new LiveFeedClient.FeedException("Resposta da PokeList excedeu o limite permitido.");
                    out.write(buf,0,n);
                }
            }
            JSONObject body;
            try {body=new JSONObject(new String(out.toByteArray(),StandardCharsets.UTF_8));}
            catch(Exception bad) {throw new LiveFeedClient.FeedException("PokeList retornou dados em formato inesperado.");}
            try {return PokeListFeedData.parse(body,System.currentTimeMillis());}
            catch(Exception bad) {throw new LiveFeedClient.FeedException("Formato da PokeList não confirmado nesta versão.");}
        } finally {
            try { http.disconnect(); } catch (Exception ignored) {}
            if(active==http)active=null;
        }
    }
    void cancel() {
        cancelled=true;
        HttpURLConnection conn=active;
        if(conn!=null)try {conn.disconnect();}catch(Exception ignored){}
    }
    private static String enc(String x) throws Exception { return URLEncoder.encode(x,"UTF-8"); }
    private static long retryAfter(HttpURLConnection h) {
        try {
            String seconds=h.getHeaderField("Retry-After");
            if(seconds==null) return 60000L;
            return Math.min(3600000L,Math.max(60000L,Long.parseLong(seconds)*1000L));
        } catch(Exception ignored) {return 60000L;}
    }
}
