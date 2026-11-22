package dev.pgrazor;

import java.util.Collections;
import java.util.List;

/**
 * Concrete {@link SpawnSearch.Source} adapters over the provider clients that already exist in PGBR.
 *
 * <p>Each adapter is thin on purpose: it forwards the validated request, records the real coverage of
 * the call, and hands the raw entries to {@link SpawnSourceRegistry#normalize}. It never fabricates a
 * row, never retries in a loop, and never widens the request to "make something appear". Cancellation
 * is delegated to the client, which aborts the socket.
 *
 * <p>Not one of these classes reveals coordinates for a provider that withholds them: PoGoCoordinates
 * rows stay {@link SpawnContract.Coordinates#BLOCKED} and are handled by its own reveal panel.
 */
final class SpawnConnectors {
    private SpawnConnectors() {}

    /** Fetches once, reporting the coverage that actually happened. */
    abstract static class Connector implements SpawnSearch.Source {
        private volatile SpawnSearch.Coverage coverage = SpawnSearch.Coverage.NONE;
        private volatile boolean requested;

        @Override public SpawnSearch.Coverage coverage() { return coverage; }

        void markCoverage(SpawnSearch.Coverage value) { this.coverage = value; }

        /** Records that this request never reached a transport (used for pre-flight rejections). */
        void markNone() { this.coverage = SpawnSearch.Coverage.NONE; }

        boolean requested() { return requested; }

        void markRequested() { this.requested = true; }

        /** Normalises the snapshot for this source, or an empty list when it carries no rows. */
        static List<SpawnContract.Spawn> rows(LiveFeedData.Snapshot snapshot, String sourceId,
                                              SpawnContract.Capabilities capabilities) {
            if (snapshot == null) return Collections.emptyList();
            return SpawnSourceRegistry.normalize(snapshot.entries, sourceId, capabilities);
        }
    }

    /**
     * iFlowGo. {@code global} uses the paged search endpoint, which PGBR reads a single page of — hence
     * {@link SpawnSearch.Coverage#FIRST_PAGE}. A nearby query is an area, never "the world".
     */
    static final class IFlowGo extends Connector {
        private final IFlowGoFeedClient client = new IFlowGoFeedClient();

        @Override public String id() { return SpawnSourceRegistry.ID_IFLOWGO; }
        @Override public SpawnContract.Capabilities capabilities() {
            return SpawnContract.Capabilities.IFLOWGO;
        }

        @Override public List<SpawnContract.Spawn> fetch(SpawnSearch.Request request) throws Exception {
            markRequested();
            markNone();
            if (request == null) throw new LiveFeedClient.FeedException("Consulta inválida para iFlowGo.");
            LiveFeedData.Snapshot snapshot;
            if (request.global) {
                if (request.raidsOnly)
                    throw new LiveFeedClient.FeedException(
                            "iFlowGo: a busca global cobre apenas aparições, não raids.");
                // Same bounded pagination the radar uses (GLOBAL_PAGES), never a world sweep. A species
                // that is not on the first page is reachable here, which is the whole point of paging.
                snapshot = client.fetchGlobal(request.species, IFlowGoFeedClient.GLOBAL_PAGES);
                markCoverage(coverageFor(snapshot));
            } else {
                if (!request.hasOrigin())
                    throw new LiveFeedClient.FeedException(
                            "iFlowGo precisa da sua localização para buscar em um raio.");
                snapshot = client.fetch(request.latitude, request.longitude, request.radiusKm,
                        request.raidsOnly);
                markCoverage(SpawnSearch.Coverage.AREA);
            }
            return rows(snapshot, id(), capabilities());
        }

        /**
         * Coverage read from the note the client itself produced, so the label cannot contradict the
         * data. "páginas N de M" with N < M is partial; otherwise the pages read were the whole answer.
         */
        private static SpawnSearch.Coverage coverageFor(LiveFeedData.Snapshot snapshot) {
            String note = snapshot == null || snapshot.note == null ? "" : snapshot.note;
            java.util.regex.Matcher pages = java.util.regex.Pattern
                    .compile("p(?:á|a)ginas (\\d+) de (\\d+)").matcher(note);
            if (pages.find()) {
                int shown = Integer.parseInt(pages.group(1));
                int available = Integer.parseInt(pages.group(2));
                if (available > shown) return SpawnSearch.Coverage.PARTIAL_PAGES;
                return SpawnSearch.Coverage.FULL_RESPONSE;
            }
            // Sem total informado pelo servidor: nunca afirmar cobertura completa.
            return SpawnSearch.Coverage.FIRST_PAGE;
        }

        @Override public void cancel() { client.cancel(); }
    }

    /** RocketMap/PokeSearch gateway: operator URL required; viewport query only. */
    static final class RocketMap extends Connector {
        private final RocketMapFeedClient client = new RocketMapFeedClient();
        private final String baseUrl;

        RocketMap(String baseUrl) { this.baseUrl = baseUrl; }

        @Override public String id() { return SpawnSourceRegistry.ID_ROCKETMAP; }
        @Override public SpawnContract.Capabilities capabilities() {
            return SpawnContract.Capabilities.ROCKETMAP;
        }

        @Override public List<SpawnContract.Spawn> fetch(SpawnSearch.Request request) throws Exception {
            markRequested();
            markNone();
            if (baseUrl == null || baseUrl.trim().isEmpty())
                throw new LiveFeedClient.FeedException(
                        "Configure o endereço HTTPS do seu servidor RocketMap antes de buscar.");
            if (request == null)
                throw new LiveFeedClient.FeedException("Consulta inválida para o RocketMap.");
            if (request.global)
                throw new LiveFeedClient.FeedException(
                        "RocketMap cobre apenas a área do servidor configurado; busca mundial não existe.");
            if (!request.hasOrigin())
                throw new LiveFeedClient.FeedException(
                        "O RocketMap precisa da sua localização para consultar a área.");
            LiveFeedData.Snapshot snapshot = client.fetch(baseUrl, request.latitude, request.longitude,
                    request.radiusKm, request.raidsOnly);
            markCoverage(SpawnSearch.Coverage.AREA);
            return rows(snapshot, id(), capabilities());
        }

        @Override public void cancel() { client.cancel(); }
    }

    /** PokeList/Moonani: global requests require exactly one species. */
    static final class PokeList extends Connector {
        private final PokeListFeedClient client = new PokeListFeedClient();

        @Override public String id() { return SpawnSourceRegistry.ID_POKELIST; }
        @Override public SpawnContract.Capabilities capabilities() {
            return SpawnContract.Capabilities.POKELIST;
        }

        @Override public List<SpawnContract.Spawn> fetch(SpawnSearch.Request request) throws Exception {
            markRequested();
            markNone();
            if (request == null) throw new LiveFeedClient.FeedException("Consulta inválida.");
            double radius = request.global ? Double.POSITIVE_INFINITY : request.radiusKm;
            if (!request.global && !request.hasOrigin())
                throw new LiveFeedClient.FeedException(
                        "A PokeList precisa da sua localização para buscar em um raio.");
            LiveFeedData.Snapshot snapshot = client.fetch(request.latitude, request.longitude, radius,
                    request.species);
            markCoverage(request.global ? SpawnSearch.Coverage.FULL_RESPONSE
                    : SpawnSearch.Coverage.AREA);
            return rows(snapshot, id(), capabilities());
        }

        @Override public void cancel() { client.cancel(); }
    }

    /**
     * PGBR's own 100% IV feed. It is the one source that is always available inside the app, so the
     * connector fetches it directly (same URL, limits and no-redirect rule as the existing
     * {@code HundoFeedDialog}/{@code SnipeSlotsDialog}) and keeps the snapshot so a second
     * "Atualizar" inside the cache window does not hammer the feed.
     */
    static final class Hundo extends Connector {
        /** A snapshot younger than this is reused; matches the age the existing dialogs display. */
        static final long CACHE_MS = 30_000L;
        static final int MAX_BYTES = 8 * 1024 * 1024;

        private volatile HundoFeedData.Snapshot cached;
        private volatile java.net.HttpURLConnection active;
        private volatile boolean cancelled;

        @Override public String id() { return SpawnSourceRegistry.ID_PGBR_HUNDO; }
        @Override public SpawnContract.Capabilities capabilities() {
            return SpawnContract.Capabilities.PGBR_HUNDO;
        }

        @Override public List<SpawnContract.Spawn> fetch(SpawnSearch.Request request) throws Exception {
            markRequested();
            markNone();
            long now = System.currentTimeMillis();
            HundoFeedData.Snapshot snapshot = cached;
            if (snapshot == null || now - snapshot.fetchedMs > CACHE_MS) {
                snapshot = download(now);
                cached = snapshot;
            }
            markCoverage(SpawnSearch.Coverage.FULL_RESPONSE);
            List<LiveFeedData.Entry> entries = new java.util.ArrayList<>();
            for (HundoFeedData.Entry row : snapshot.entries) {
                entries.add(new LiveFeedData.Entry("hundo:" + row.encounterId, LiveFeedData.Kind.POKEMON,
                        "#" + row.dex, "", row.lat, row.lon, row.dex, row.cp, row.level,
                        HundoFeedData.FEED_CLAIMED_IV, row.despawnMs, true));
            }
            // The feed states 100% for every row; that claim is carried as the percentage, never as
            // proven 15/15/15 components (the feed does not send them).
            return rows(new LiveFeedData.Snapshot(entries, snapshot.fetchedMs,
                    LiveFeedData.DEFAULT_REFRESH_SECONDS, "feed 100% IV PGBR"), id(), capabilities());
        }

        private HundoFeedData.Snapshot download(long now) throws Exception {
            cancelled = false;
            java.net.HttpURLConnection http =
                    (java.net.HttpURLConnection) new java.net.URL(ShundoSniper.FEED_URL).openConnection();
            active = http;
            try {
                http.setInstanceFollowRedirects(false);
                http.setConnectTimeout(10_000);
                http.setReadTimeout(12_000);
                http.setRequestProperty("Accept", "application/json");
                http.setRequestProperty("User-Agent", "PGBR-Sniper");
                if (cancelled) throw new LiveFeedClient.FeedException("Busca cancelada.");
                int code = http.getResponseCode();
                if (code == 401 || code == 403)
                    throw new LiveFeedClient.FeedException(
                            "O feed 100% IV recusou o acesso (HTTP " + code + ").", code, 0);
                if (code == 429)
                    throw new LiveFeedClient.FeedException("O feed 100% IV limitou as consultas.", 429,
                            retryAfter(http));
                if (code != 200)
                    throw new LiveFeedClient.FeedException("O feed 100% IV respondeu HTTP " + code + ".", code, 0);
                java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
                try (java.io.InputStream in = http.getInputStream()) {
                    byte[] buffer = new byte[8192];
                    int read;
                    while ((read = in.read(buffer)) != -1) {
                        if (cancelled) throw new LiveFeedClient.FeedException("Busca cancelada.");
                        if (bytes.size() + read > MAX_BYTES)
                            throw new LiveFeedClient.FeedException(
                                    "Resposta do feed 100% IV excedeu o limite.");
                        bytes.write(buffer, 0, read);
                    }
                }
                return HundoFeedData.parse(new org.json.JSONObject(
                        new String(bytes.toByteArray(), java.nio.charset.StandardCharsets.UTF_8)), now);
            } catch (java.io.IOException network) {
                if (cancelled) throw new LiveFeedClient.FeedException("Busca cancelada.");
                throw new LiveFeedClient.FeedException("Não foi possível acessar o feed 100% IV. "
                        + "Verifique a rede.");
            } finally {
                http.disconnect();
                if (active == http) active = null;
            }
        }

        private static long retryAfter(java.net.HttpURLConnection http) {
            try {
                long seconds = Long.parseLong(String.valueOf(http.getHeaderField("Retry-After")).trim());
                return Math.max(60_000L, Math.min(3_600_000L, seconds * 1000L));
            } catch (RuntimeException absent) {
                return 60_000L;
            }
        }

        @Override public void cancel() {
            cancelled = true;
            java.net.HttpURLConnection http = active;
            if (http != null) http.disconnect();
        }
    }

    /** Builds the connector for a source id, or null when the source cannot be queried yet. */
    static SpawnSearch.Source create(String sourceId, String rocketMapBaseUrl) {
        if (SpawnSourceRegistry.ID_IFLOWGO.equals(sourceId)) return new IFlowGo();
        if (SpawnSourceRegistry.ID_ROCKETMAP.equals(sourceId)) return new RocketMap(rocketMapBaseUrl);
        if (SpawnSourceRegistry.ID_POKELIST.equals(sourceId)) return new PokeList();
        if (SpawnSourceRegistry.ID_PGBR_HUNDO.equals(sourceId)) return new Hundo();
        return null;
    }
}
