package dev.pgrazor;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Opt-in client for the separately licensed PokeList Partner API (schema 2026-11-22).
 * Never reuses the legacy Moonani api.v2.php contract or any provider credential.
 * No public base URL or secret is embedded in PGBR; missing feed/licence fail closed.
 */
final class PokeListPartnerClient {
    static final String SCHEMA = "2026-11-22";
    static final int MAX_PAGES = 3, PAGE_LIMIT = 100, MAX_RESPONSE_BYTES = 2 * 1024 * 1024;
    private final String baseUrl, key;
    private volatile boolean cancelled;
    private volatile HttpURLConnection active;
    private volatile SpawnSearch.Coverage coverage = SpawnSearch.Coverage.NONE;

    PokeListPartnerClient(String endpoint, String bearer) {
        baseUrl = endpoint == null ? "" : endpoint.trim();
        key = bearer == null ? "" : bearer.trim();
    }

    SpawnSearch.Coverage coverage() { return coverage; }

    void cancel() {
        cancelled = true;
        HttpURLConnection connection = active;
        if (connection != null) connection.disconnect();
    }

    static String safeEndpoint(String endpoint) throws LiveFeedClient.FeedException {
        try {
            URI uri = new URI(endpoint == null ? "" : endpoint.trim());
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null
                    || uri.getHost().isEmpty() || uri.getRawUserInfo() != null
                    || uri.getRawQuery() != null || uri.getRawFragment() != null
                    || uri.getPort() == 0)
                throw new Exception("invalid");
            String path = uri.getPath() == null ? "" : uri.getPath();
            if (!path.isEmpty() && !"/".equals(path))
                throw new Exception("base url must be the origin");
            return uri.getScheme().toLowerCase() + "://" + uri.getRawAuthority();
        } catch (Exception invalid) {
            throw new LiveFeedClient.FeedException(
                    "Configure apenas a origem HTTPS da API do parceiro (sem caminho, senha ou parâmetros).");
        }
    }

    List<SpawnContract.Spawn> fetch(SpawnSearch.Request request) throws Exception {
        coverage = SpawnSearch.Coverage.NONE;
        cancelled = false;
        if (request == null || request.raidsOnly)
            throw new LiveFeedClient.FeedException("Esta origem recebe somente buscas de Pokémon.");
        if (!request.global && !request.hasOrigin())
            throw new LiveFeedClient.FeedException("Selecione uma localização válida para a busca.");
        final String origin = safeEndpoint(baseUrl);
        if (!key.matches("plp_[A-Za-z0-9_-]{1,96}\\.[A-Za-z0-9_-]{8,256}"))
            throw new LiveFeedClient.FeedException("Configure a chave de parceiro PokeList no painel.");
        JSONObject capabilities = send(origin, "/v1/capabilities", null);
        ensureLiveAndLicensed(capabilities);
        JSONObject plan = capabilities.optJSONObject("plan");
        double maximumKm = plan == null ? 50.0 : plan.optDouble("max_radius_km", 50.0);
        int maxLimit = plan == null ? PAGE_LIMIT : plan.optInt("max_limit", PAGE_LIMIT);
        if (!request.global && (!Double.isFinite(request.radiusKm)
                || request.radiusKm <= 0 || request.radiusKm > maximumKm))
            throw new LiveFeedClient.FeedException(
                    "Raio não permitido por este plano da PokeList. Máximo: " + maximumKm + " km.");
        if (maxLimit < 1)
            throw new LiveFeedClient.FeedException("O plano do parceiro não permite buscas.");
        int pageLimit = Math.min(PAGE_LIMIT, maxLimit);
        List<SpawnContract.Spawn> out = new ArrayList<>();
        String cursor = null;
        boolean more = false;
        for (int page = 0; page < MAX_PAGES; page++) {
            if (cancelled) throw new LiveFeedClient.FeedException("Busca cancelada.");
            JSONObject query = new JSONObject();
            if (!request.global) {
                query.put("center", new JSONObject().put("lat", request.latitude)
                        .put("lng", request.longitude));
                query.put("radius_km", request.radiusKm);
            }
            if (request.species > 0) query.put("pokemon_ids", new JSONArray().put(request.species));
            query.put("active_only", true);
            query.put("sort", "expires_at");
            query.put("limit", pageLimit);
            applyRemoteFilters(query, request.filters, capabilities);
            if (cursor != null) query.put("cursor", cursor);
            Page result = parsePage(send(origin, "/v1/search/pokemon", query),
                    System.currentTimeMillis());
            out.addAll(result.rows);
            more = result.hasMore;
            if (!more) break;
            if (result.nextCursor == null || result.nextCursor.isEmpty())
                throw new LiveFeedClient.FeedException("A API informou paginação sem cursor válido.");
            if (result.nextCursor.equals(cursor))
                throw new LiveFeedClient.FeedException("A API repetiu um cursor de paginação.");
            cursor = result.nextCursor;
        }
        coverage = more ? SpawnSearch.Coverage.PARTIAL_PAGES
                : request.global ? SpawnSearch.Coverage.FULL_RESPONSE : SpawnSearch.Coverage.AREA;
        // FULL_RESPONSE means all ingested rows for this query, NOT worldwide scanning coverage.
        return out;
    }

    /**
     * Forward only filters that /v1/capabilities explicitly advertises as server-side.
     * Everything else remains a strict local filter in the shared Sniper engine.
     */
    static void applyRemoteFilters(JSONObject query, SpawnFilters filters, JSONObject capabilities)
            throws JSONException {
        if (filters == null) return;
        JSONObject local = filters.toJson();
        String[][] mapping = {
            {"min_iv", "iv_min"}, {"max_iv", "iv_max"},
            {"min_attack", "attack_min"}, {"max_attack", "attack_max"},
            {"min_defense", "defense_min"}, {"max_defense", "defense_max"},
            {"min_stamina", "stamina_min"}, {"max_stamina", "stamina_max"},
            {"min_cp", "cp_min"}, {"max_cp", "cp_max"},
            {"min_level", "level_min"}, {"max_level", "level_max"}
        };
        for (String[] pair : mapping) {
            if (local.has(pair[0]) && supportsField(capabilities, pair[1]))
                query.put(pair[1], local.opt(pair[0]));
        }
        if (local.optBoolean("perfect_only", false)
                && supportsField(capabilities, "iv_min")) query.put("iv_min", 100);
        if (local.optBoolean("zero_only", false)) {
            for (String key : new String[]{"attack_max", "defense_max", "stamina_max"})
                if (supportsField(capabilities, key)) query.put(key, 0);
        }
        int gender = local.optInt("gender", SpawnFilters.GENDER_ANY);
        if (gender >= 1 && gender <= 3 && supportsField(capabilities, "gender"))
            query.put("gender", gender);
        int size = local.optInt("size", SpawnFilters.SIZE_ANY);
        if (supportsField(capabilities, "size")) {
            if (size == SpawnFilters.SIZE_XXS) query.put("size", "xxs");
            if (size == SpawnFilters.SIZE_XXL) query.put("size", "xxl");
        }
        if (local.optBoolean("shiny_possible_only", false)
                && supportsField(capabilities, "shiny_status"))
            query.put("shiny_status", "eligible"); // eligibility != individual shiny
        int league = local.optInt("league", SpawnFilters.LEAGUE_ANY);
        if (supportsField(capabilities, "pvp_league")) {
            if (league == SpawnFilters.LEAGUE_GREAT) query.put("pvp_league", "great");
            if (league == SpawnFilters.LEAGUE_ULTRA) query.put("pvp_league", "ultra");
            int maxRank = local.optInt("max_rank", SpawnFilters.UNSET);
            if (maxRank > 0 && (league == SpawnFilters.LEAGUE_GREAT
                    || league == SpawnFilters.LEAGUE_ULTRA)
                    && supportsField(capabilities, "pvp_rank_max"))
                query.put("pvp_rank_max", maxRank);
        }
        // Numeric form ids cannot be mapped to the contract's string form without a catalogue.
        // shiny_confirmed_only cannot be forwarded: verified_for_account is not supported.
    }

    private static boolean supportsField(JSONObject capabilities, String field) {
        JSONArray flags = capabilities == null ? null : capabilities.optJSONArray("filters");
        if (flags == null) return false;
        String family = field.replaceAll("_(min|max)$", "_*");
        for (int i = 0; i < flags.length(); i++) {
            JSONObject f = flags.optJSONObject(i);
            if (f == null || !"server_side".equals(f.optString("position", ""))) continue;
            String label = f.optString("filter", "");
            if (label.contains(field) || label.contains(family)) return true;
        }
        return false;
    }

    /** No response from the reference backend is promoted to a production feed by UI guesses. */
    static void ensureLiveAndLicensed(JSONObject capabilities) throws LiveFeedClient.FeedException {
        JSONObject licensing = capabilities == null ? null : capabilities.optJSONObject("licensing");
        String status = licensing == null ? "" : licensing.optString("status", "");
        if (!"approved".equals(status) && !"active".equals(status)
                && !"authorized".equals(status)) {
            throw new LiveFeedClient.FeedException(
                    "A licença da PokeList Partner não está aprovada para este parceiro ("
                            + (status.isEmpty() ? "não declarada" : status) + ").", 403, 0);
        }
        JSONArray layers = capabilities.optJSONArray("data_layers");
        boolean live = false;
        if (layers != null) for (int i = 0; i < layers.length(); i++) {
            JSONObject layer = layers.optJSONObject(i);
            if (layer != null && "live_provider_feed".equals(layer.optString("layer", ""))) {
                String state = layer.optString("status", "");
                live = "configured".equals(state) || "active".equals(state);
                break;
            }
        }
        if (!live) throw new LiveFeedClient.FeedException(
                "A PokeList ainda não disponibiliza feed de produção conectado.", 503, 0);
    }

    static final class Page {
        final List<SpawnContract.Spawn> rows;
        final boolean hasMore;
        final String nextCursor;
        Page(List<SpawnContract.Spawn> rows, boolean hasMore, String nextCursor) {
            this.rows = rows; this.hasMore = hasMore; this.nextCursor = nextCursor;
        }
    }

    /** Pure parser; testable with the partner OpenAPI fixture, without network/Android. */
    static Page parsePage(JSONObject body, long now) throws JSONException {
        if (body == null || !"pokemon".equals(body.optString("kind", "")))
            throw new JSONException("PokeList Partner retornou tipo inesperado.");
        JSONArray items = body.optJSONArray("items");
        if (items == null || items.length() > 200)
            throw new JSONException("PokeList Partner retornou lista inválida.");
        List<SpawnContract.Spawn> rows = new ArrayList<>();
        for (int i = 0; i < items.length(); i++) {
            JSONObject item = items.optJSONObject(i);
            if (item == null || !"pokemon".equals(item.optString("kind", ""))) continue;
            String provenance = item.optString("source", "")
                    .toLowerCase(java.util.Locale.ROOT);
            if (item.optBoolean("demo", false) || provenance.contains("fixture")
                    || provenance.contains("synthetic") || provenance.contains("sintet")
                    || provenance.contains("demo")) continue;
            int dex = bounded(item, "pokemon_id", 1, 1025);
            long expires = timestamp(item.optString("expires_at", ""));
            long observed = timestamp(item.optString("observed_at", ""));
            if (dex < 1 || expires <= now || observed <= 0 || observed > now + 300_000L)
                continue;
            // Records labelled estimated/synthetic must never be shown as measured 100% IV.
            String quality = item.optString("quality", "");
            boolean measured = "observed".equals(quality) || "verified".equals(quality);
            SpawnContract.Builder b = new SpawnContract.Builder(
                    SpawnSourceRegistry.ID_POKELIST_PARTNER,
                    SpawnContract.Capabilities.POKELIST_PARTNER)
                    .encounter(item.optString("event_id", ""))
                    .species(dex, item.optString("name", "#" + dex))
                    .times(observed, expires)
                    .cp(bounded(item, "cp", 10, 10000))
                    .level(number(item, "level", 1.0, 51.0))
                    .gender(bounded(item, "gender", 1, 3))
                    .size("xxs".equals(item.optString("size", "")) ? SpawnFilters.SIZE_XXS
                            : "xxl".equals(item.optString("size", "")) ? SpawnFilters.SIZE_XXL
                            : SpawnContract.UNKNOWN);
            // A textual form name cannot be mapped to PGBR's numeric form id without a catalogue.
            int form = bounded(item, "form", 0, 100000);
            if (form >= 0) b.form(form);
            if (measured) {
                b.ivs(bounded(item, "attack_iv", 0, 15),
                        bounded(item, "defense_iv", 0, 15),
                        bounded(item, "stamina_iv", 0, 15))
                        .ivPercent(bounded(item, "iv_percent", 0, 100));
            }
            int great = bounded(item, "pvp_rank_great", 1, 4096);
            int ultra = bounded(item, "pvp_rank_ultra", 1, 4096);
            if (great > 0 && ultra < 0) b.pvp(SpawnFilters.LEAGUE_GREAT, great, false);
            if (ultra > 0 && great < 0) b.pvp(SpawnFilters.LEAGUE_ULTRA, ultra, false);
            b.shiny(false, "eligible".equals(item.optString("shiny_status", "")));
            if (measured && "exact".equals(item.optString("coordinate_precision", ""))) {
                b.location(number(item, "latitude", -90, 90),
                        number(item, "longitude", -180, 180));
            } else {
                b.coordinatesBlocked();
            }
            rows.add(b.build());
        }
        return new Page(rows, body.optBoolean("has_more", false),
                body.optString("next_cursor", ""));
    }

    private static int bounded(JSONObject o, String name, int min, int max) {
        if (!o.has(name) || o.isNull(name)) return SpawnContract.UNKNOWN;
        Object value = o.opt(name);
        if (!(value instanceof Number)) return SpawnContract.UNKNOWN;
        double n = ((Number)value).doubleValue();
        if (!Double.isFinite(n) || n < min || n > max || Math.floor(n) != n)
            return SpawnContract.UNKNOWN;
        return (int)n;
    }

    private static double number(JSONObject o, String name, double min, double max) {
        if (!o.has(name) || o.isNull(name)) return Double.NaN;
        Object value = o.opt(name);
        if (!(value instanceof Number)) return Double.NaN;
        double n = ((Number)value).doubleValue();
        return Double.isFinite(n) && n >= min && n <= max ? n : Double.NaN;
    }

    private static long timestamp(String iso) {
        try { return OffsetDateTime.parse(iso).toInstant().toEpochMilli(); }
        catch (Exception invalid) { return 0L; }
    }

    private JSONObject send(String origin, String path, JSONObject body) throws Exception {
        if (cancelled) throw new LiveFeedClient.FeedException("Busca cancelada.");
        URL url = new URL(origin + path);
        HttpURLConnection http = (HttpURLConnection)url.openConnection();
        active = http;
        try {
            http.setInstanceFollowRedirects(false);
            http.setConnectTimeout(10000);
            http.setReadTimeout(15000);
            http.setRequestProperty("Authorization", "Bearer " + key);
            http.setRequestProperty("Accept", "application/json");
            http.setRequestProperty("User-Agent", "PGBR-PokeListPartner/1");
            if (body != null) {
                http.setRequestMethod("POST");
                http.setDoOutput(true);
                http.setRequestProperty("Content-Type", "application/json");
                byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8);
                try (OutputStream out = http.getOutputStream()) { out.write(bytes); }
            }
            int status = http.getResponseCode();
            if (status == 401 || status == 403)
                throw new LiveFeedClient.FeedException("Acesso ao parceiro recusado (HTTP "
                        + status + ").", status, 0);
            if (status == 429)
                throw new LiveFeedClient.FeedException("Limite de buscas do parceiro atingido.", 429,
                        retryAfter(http));
            if (status < 200 || status >= 300)
                throw new LiveFeedClient.FeedException("PokeList Partner indisponível (HTTP "
                        + status + ").", status, 0);
            String schema = http.getHeaderField("X-Schema-Version");
            if (schema != null && !SCHEMA.equals(schema))
                throw new LiveFeedClient.FeedException("Schema PokeList Partner não compatível.");
            ByteArrayOutputStream result = new ByteArrayOutputStream();
            try (InputStream in = http.getInputStream()) {
                byte[] buffer = new byte[4096];
                int n;
                while ((n = in.read(buffer)) != -1) {
                    if (cancelled || Thread.currentThread().isInterrupted())
                        throw new LiveFeedClient.FeedException("Busca cancelada.");
                    if (result.size() + n > MAX_RESPONSE_BYTES)
                        throw new LiveFeedClient.FeedException("Resposta excede o limite de 2 MB.");
                    result.write(buffer, 0, n);
                }
            }
            return new JSONObject(new String(result.toByteArray(), StandardCharsets.UTF_8));
        } finally {
            http.disconnect();
            if (active == http) active = null;
        }
    }

    private static long retryAfter(HttpURLConnection http) {
        try {
            long seconds = Long.parseLong(http.getHeaderField("Retry-After"));
            return Math.min(3_600_000L, Math.max(60_000L, seconds * 1000L));
        } catch (Exception ignored) { return 60_000L; }
    }
}
