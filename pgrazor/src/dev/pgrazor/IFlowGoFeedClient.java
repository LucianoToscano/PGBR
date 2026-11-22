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

    /** Pages requested per search; each page is one HTTP request, never a background sweep. */
    static final int GLOBAL_PAGES = 2;

    /**
     * The global search for a client that asked for a single page (the species preview list). One HTTP
     * request. Server-side pokemon_id is an observed filter (a limited #25 query returned exclusively
     * Pikachu spawns), and no unknown query flags are invented.
     */
    LiveFeedData.Snapshot fetchGlobal(int dexId) throws Exception {
        return fetchGlobal(dexId, 1);
    }

    /**
     * Up to {@code maxPages} consecutive pages of the global search, stopping at the first empty or
     * failed page.
     *
     * <p>Paging stops rather than guesses: page N+1 is requested only when the server's own {@code total}
     * proves it exists. A failure after the first page keeps the rows already received and says that the
     * search is partial — it never discards a valid first response. Rate limits and refusals are rethrown so
     * the caller can keep its 401/403/429 handling.
     */
    LiveFeedData.Snapshot fetchGlobal(int dexId, int maxPages) throws Exception {
        cancelled = false;
        int pages = Math.max(1, Math.min(maxPages, GLOBAL_PAGES));
        java.util.List<LiveFeedData.Entry> entries = new java.util.ArrayList<>();
        java.util.Set<String> ids = new java.util.HashSet<>();
        int pagesShown = 0, pagesAvailable = 0;
        String tone = "";
        Exception pageFailure = null;
        for (int page = 1; page <= pages; page++) {
            String endpoint = "https://pokecoords.iflowgo.com/iflowgopokecoords/api/v1/pokemon-search?page=" + page;
            if (dexId >= 1 && dexId <= 1025) endpoint += "&pokemon_id=" + dexId;
            JSONObject payload;
            try {
                payload = getJson(endpoint);
            } catch (LiveFeedClient.FeedException fe) {
                if (fe.status == 401 || fe.status == 403 || fe.status == 429) throw fe;
                pageFailure = fe;
                break;                       // partial result is better than none
            } catch (IOException ioe) {
                if (cancelled) throw ioe;
                pageFailure = ioe;
                break;
            }
            pagesShown = page;
            for (LiveFeedData.Entry entry : IFlowGoFeedData.parseGlobal(payload, System.currentTimeMillis())
                    .entries) {
                if (ids.add(entry.id)) entries.add(entry);
            }
            // A missing/invalid total is not evidence of full coverage, even if no further
            // page can be requested. Always keep the note honest about unknown pagination.
            int reportedTotal = payload.optInt("total", -1);
            boolean totalKnown = payload.has("total") && !payload.isNull("total")
                    && reportedTotal >= 0;
            boolean morePages = IFlowGoFeedData.hasNextPage(payload, page, 200);
            if (!morePages) {
                pagesAvailable = totalKnown ? page : 0;
                break;
            }
            // Report a *known next page* even when our local safety cap stops here.
            // In particular, page 2 of 5 must never be presented as "2 de 2".
            pagesAvailable = page + 1;
        }
        if (pagesShown == 0) throw pageFailure != null ? pageFailure
                : new LiveFeedClient.FeedException("iFlowGo global não retornou nenhuma página.");
        String note = IFlowGoFeedData.parseGlobal(
                new JSONObject().put("items", new org.json.JSONArray()), System.currentTimeMillis(),
                pagesShown, pagesShown, pagesAvailable).note;
        if (pageFailure != null)
            note = note + " Busca parcial: a página seguinte falhou (" + pageFailure.getMessage() + ").";
        return new LiveFeedData.Snapshot(entries, System.currentTimeMillis(), 60, note);
    }

    private LiveFeedData.Snapshot doFetch(String target, boolean raids, boolean global) throws Exception {
        JSONObject payload = getJson(target);
        try {
            return global
                    ? IFlowGoFeedData.parseGlobal(payload, System.currentTimeMillis())
                    : IFlowGoFeedData.parse(payload, System.currentTimeMillis(), raids);
        } catch (Exception e) {
            throw new LiveFeedClient.FeedException("iFlowGo retornou campos desconhecidos nesta versão.");
        }
    }

    /**
     * One GET with the full safety contract: no redirects, bounded body, cancellation, explicit
     * 401/403/429 handling and a typed exception for every other status. Shared by the area and global
     * searches so a new call site cannot accidentally skip a check.
     */
    private JSONObject getJson(String target) throws Exception {
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
            try { return new JSONObject(new String(out.toByteArray(), StandardCharsets.UTF_8)); }
            catch (Exception e) { throw new LiveFeedClient.FeedException("iFlowGo retornou JSON inválido."); }
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
