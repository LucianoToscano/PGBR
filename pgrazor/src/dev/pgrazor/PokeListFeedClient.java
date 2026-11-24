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
    /** Longest radius the historical client used; anything larger is a UI sentinel, not coverage. */
    private static final int MAX_DISTANCE_KM = 5;
    private volatile HttpURLConnection active;
    private volatile boolean cancelled;

    LiveFeedData.Snapshot fetch(double lat,double lon,double radius,int dexId) throws Exception {
        validate(lat, lon);
        boolean global = Double.isInfinite(radius) && radius > 0;
        if (global && (dexId < 1 || dexId > 1025))
            throw new LiveFeedClient.FeedException(
                    "Para consultar a PokeList globalmente, selecione uma espécie ou número da Pokédex.");
        // The supplied APK's FilterActivity labels distance values as km and
        // explicitly pairs value 0 with "All". Global is allowed only with a
        // server-side species allowlist to bound the response.
        int distance = global ? 0 : Math.max(1, Math.min(MAX_DISTANCE_KM, (int)Math.round(radius)));
        JSONObject body = request(query(PokeListFeedData.Category.POKEMON, lat, lon, distance, dexId));
        try {return PokeListFeedData.parse(body,System.currentTimeMillis());}
        catch(Exception bad) {throw new LiveFeedClient.FeedException("Formato da PokeList não confirmado nesta versão.");}
    }

    /**
     * Category-aware request used by the radar tabs and the Snipers. {@code distanceKm = 0} keeps the
     * historical "All" value and is accepted only together with a species allowlist, so a category
     * sweep can never turn into an unbounded crawl.
     */
    PokeListFeedData.EventPage fetchEvents(double lat,double lon,int distanceKm,
                                           PokeListFeedData.Category category,int dexId) throws Exception {
        validate(lat, lon);
        PokeListFeedData.Category resolved = category == null
                ? PokeListFeedData.Category.POKEMON : category;
        int distance = Math.max(0, Math.min(MAX_DISTANCE_KM, distanceKm));
        if (distance == 0 && (dexId < 1 || dexId > 1025))
            throw new LiveFeedClient.FeedException(
                    "Para consultar a PokeList sem limite de distância, selecione uma espécie.");
        JSONObject body = request(query(resolved, lat, lon, distance, dexId));
        try {return PokeListFeedData.parseEvents(body, System.currentTimeMillis(), resolved);}
        catch(Exception bad) {
            throw new LiveFeedClient.FeedException("Formato da PokeList não confirmado nesta versão.");
        }
    }

    private static void validate(double lat, double lon) throws LiveFeedClient.FeedException {
        if (!Double.isFinite(lat) || !Double.isFinite(lon)
                || lat < -90 || lat > 90 || lon < -180 || lon > 180)
            throw new LiveFeedClient.FeedException("Coordenadas inválidas para a busca.");
    }

    private static String query(PokeListFeedData.Category category, double lat, double lon,
                                int distance, int dexId) throws Exception {
        return "?type=" + category.wire
                + "&mons=" + (dexId >= 1 && dexId <= 1025 ? dexId : "")
                + "&lat=" + enc(String.format(Locale.US,"%.6f",lat))
                + "&lng=" + enc(String.format(Locale.US,"%.6f",lon))
                + "&distance=" + distance
                + "&_time=" + System.currentTimeMillis(); // observed cache-buster in supplied APK
    }

    /** One HTTPS GET with bounded body, explicit refusals and no retry-on-refusal. */
    private JSONObject request(String query) throws Exception {
        URL url = new URL(BASE + query);
        if (!"https".equals(url.getProtocol()))
            throw new LiveFeedClient.FeedException("HTTPS obrigatório para o feed.");
        cancelled = false;
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
                        "PokeList / Moonani recusou a consulta (HTTP " + status
                        + "). Verifique com o fornecedor as condições de acesso."
                        + " O PGBR não tentará contornar essa restrição.",
                        status, 0);
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
            try {return new JSONObject(new String(out.toByteArray(),StandardCharsets.UTF_8));}
            catch(Exception bad) {throw new LiveFeedClient.FeedException("PokeList retornou dados em formato inesperado.");}
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
