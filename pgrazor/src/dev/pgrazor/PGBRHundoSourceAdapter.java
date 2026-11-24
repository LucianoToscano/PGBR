package dev.pgrazor;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONObject;

/**
 * Adapter for the PGBR 100% IV feed collected by {@code tools/hundo-feed}. It is the project's own
 * service, but "100% IV" is still the collector's claim: the app does not re-check the encounter,
 * and the row is not proof that the spawn is still on the map.
 *
 * <p>{@link Fetcher} exists so the device-free suite can drive the normalisation without network.
 */
final class PGBRHundoSourceAdapter implements SourceAdapter {
    interface Fetcher {
        JSONObject fetch() throws Exception;
    }

    /**
     * Same URL the existing Shundo Sniper reads; kept here so this adapter stays free of
     * Android types and can be unit-tested offline. tools/check.py asserts both constants match.
     */
    static final String FEED_URL = "https://feed.faustusfoundry.com/pgrazor/hundos.json";

    private final Fetcher fetcher;
    private volatile HttpURLConnection active;
    private volatile SourceCapabilities capabilities = SourceRegistry.of(SourceRegistry.PGBR_HUNDO);

    PGBRHundoSourceAdapter() { this(null); }

    PGBRHundoSourceAdapter(Fetcher fetcher) { this.fetcher = fetcher; }

    @Override public String sourceId() { return SourceRegistry.PGBR_HUNDO; }

    @Override public SourceCapabilities capabilities() { return capabilities; }

    @Override public SourceCapabilities status() { return capabilities; }

    @Override public String attribution() { return capabilities.attribution; }

    @Override public void cancel() {
        HttpURLConnection connection = active;
        if (connection != null) try { connection.disconnect(); } catch (Exception ignored) { }
    }

    @Override public SourceSearchResult search(SourceQuery query, String cursor, long now) throws Exception {
        if (cursor != null && !cursor.isEmpty())
            throw new SourceUnavailableException("O feed 100% IV do PGBR é um arquivo único, sem paginação.");
        JSONObject body = fetcher != null ? fetcher.fetch() : download();
        if (body == null) throw new SourceUnavailableException("Feed 100% IV do PGBR sem conteúdo.");
        HundoFeedData.Snapshot snapshot;
        try {
            snapshot = HundoFeedData.parse(body, now);
        } catch (Exception stale) {
            throw new SourceUnavailableException(
                    "Feed 100% IV do PGBR sem atualização recente verificável: " + stale.getMessage());
        }
        List<SourceEvent> events = new ArrayList<>();
        for (HundoFeedData.Entry entry : snapshot.entries) {
            events.add(SourceEvent.builder(sourceId(), "hundo:" + entry.encounterId, SourceEvent.Kind.POKEMON)
                    .originKind(OriginKind.LIVE_EXTERNAL)
                    .pokemonId(entry.dex).formId(entry.form < 0 ? null : Integer.valueOf(entry.form))
                    .coordinates(entry.lat, entry.lon)
                    // The feed's own contract is hundo-only; the value is the provider's claim.
                    .ivPercent(Double.valueOf(100.0))
                    .ivAtk(Integer.valueOf(15)).ivDef(Integer.valueOf(15)).ivSta(Integer.valueOf(15))
                    .cp(entry.cp <= 0 ? null : Integer.valueOf(entry.cp))
                    .level(Double.isNaN(entry.level) ? null : Integer.valueOf((int) Math.rint(entry.level)))
                    .observedAt(snapshot.fetchedMs).retrievedAt(snapshot.fetchedMs)
                    .expiresAt(entry.despawnMs)
                    .dataQuality(SourceEvent.QUALITY_PROVIDER_CLAIMED)
                    .coverage(capabilities.coverage)
                    .note("100% IV declarado pelo feed do PGBR; não é confirmação de encontro.")
                    .build());
        }
        EventFilterEngine.Outcome outcome = EventFilterEngine.apply(
                query == null ? SourceQuery.builder().build() : query, events, now);
        capabilities = SourceCapabilities.withLastVerified(capabilities, snapshot.fetchedMs);
        List<String> remote = new ArrayList<>();
        remote.add("IV % (somente 100%)");
        return new SourceSearchResult(sourceId(), outcome.kept, false, null,
                Integer.valueOf(events.size()), snapshot.fetchedMs, capabilities.coverage, remote,
                outcome.appliedLocally, outcome.unsupported,
                "Feed 100% IV do PGBR · atualizado em " + snapshot.fetchedMs + " · "
                        + snapshot.entries.size() + " linha(s) na fonte.");
    }

    private JSONObject download() throws Exception {
        URL url = new URL(FEED_URL);
        if (!"https".equals(url.getProtocol()))
            throw new SourceUnavailableException("HTTPS obrigatório para o feed 100% IV.");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        active = connection;
        try {
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(15000);
            connection.setInstanceFollowRedirects(false);
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("User-Agent", "PGBR/0.00.46 (+https://github.com/LucianoToscano/PGBR)");
            int status = connection.getResponseCode();
            if (status == 401 || status == 403 || status == 429)
                throw new SourceUnavailableException(
                        "Feed 100% IV do PGBR recusou a consulta (HTTP " + status + ").",
                        status, 0L, true);
            if (status < 200 || status >= 300)
                throw new SourceUnavailableException(
                        "Feed 100% IV do PGBR indisponível (HTTP " + status + ").", status, 0L, false);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try (InputStream in = connection.getInputStream()) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    if (out.size() + read > 4 * 1024 * 1024)
                        throw new SourceUnavailableException("Feed 100% IV excedeu o limite de tamanho.");
                    out.write(buffer, 0, read);
                }
            }
            return new JSONObject(new String(out.toByteArray(), StandardCharsets.UTF_8));
        } finally {
            try { connection.disconnect(); } catch (Exception ignored) { }
            if (active == connection) active = null;
        }
    }
}
