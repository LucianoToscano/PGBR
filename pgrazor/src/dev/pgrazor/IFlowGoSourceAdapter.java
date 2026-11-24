package dev.pgrazor;

import java.util.ArrayList;
import java.util.List;

/**
 * iFlowGo adapter with two honest modes:
 * <ul>
 *   <li><b>nearby</b> — a centre and radius are required; the layer (spawns or raids) is chosen by
 *       the query's kind and never mixed.</li>
 *   <li><b>global</b> — a species is required, one page per request, and {@code hasMore} is copied
 *       from the response. A single 25-row page is never reported as a worldwide sweep.</li>
 * </ul>
 */
final class IFlowGoSourceAdapter implements SourceAdapter {
    private final IFlowGoFeedClient client;
    private volatile SourceCapabilities capabilities = SourceRegistry.of(SourceRegistry.IFLOWGO);

    IFlowGoSourceAdapter() { this(new IFlowGoFeedClient()); }

    IFlowGoSourceAdapter(IFlowGoFeedClient client) { this.client = client; }

    @Override public String sourceId() { return SourceRegistry.IFLOWGO; }

    @Override public SourceCapabilities capabilities() { return capabilities; }

    @Override public SourceCapabilities status() { return capabilities; }

    @Override public String attribution() { return capabilities.attribution; }

    @Override public void cancel() { client.cancel(); }

    @Override public SourceSearchResult search(SourceQuery query, String cursor, long now) throws Exception {
        SourceQuery q = query == null ? SourceQuery.builder().build() : query;
        boolean nearby = q.centerLat != null && q.centerLon != null;
        boolean raids = q.kinds.contains(SourceEvent.Kind.RAID);
        if (!nearby && q.pokemonIds.isEmpty())
            throw new SourceUnavailableException(
                    "A busca global do iFlowGo exige uma espécie selecionada (é assim que a consulta fica limitada).");
        int page = 1;
        if (cursor != null && !cursor.isEmpty()) {
            try { page = Integer.parseInt(cursor.trim()); }
            catch (NumberFormatException invalid) {
                throw new SourceUnavailableException("Cursor de página inválido para o iFlowGo.");
            }
        }
        try {
            IFlowGoFeedData.EventPage result;
            List<String> remote = new ArrayList<>();
            if (nearby) {
                double radius = q.radiusKm == null ? 25.0 : q.radiusKm.doubleValue();
                result = client.fetchEvents(q.centerLat, q.centerLon, radius, raids);
                remote.add("raio");
                remote.add("camada de reides");
            } else {
                int dex = q.pokemonIds.iterator().next();
                result = client.fetchGlobalPage(dex, page);
                remote.add("espécies");
                remote.add("página");
                if (q.ivMin != null || q.ivMax != null) remote.add("IV %");
            }
            EventFilterEngine.Outcome outcome = EventFilterEngine.apply(q, result.events, now);
            capabilities = SourceCapabilities.withStatus(capabilities, SourceCapabilities.Status.UNVERIFIED);
            return new SourceSearchResult(sourceId(), outcome.kept, result.hasMore,
                    result.hasMore ? String.valueOf(page + 1) : null, result.totalKnown, now,
                    capabilities.coverage, remote, outcome.appliedLocally, outcome.unsupported,
                    result.note + " " + outcome.summary());
        } catch (LiveFeedClient.FeedException error) {
            if (SourceAdapterSupport.isRefusal(error.status))
                capabilities = SourceCapabilities.withStatus(capabilities,
                        SourceCapabilities.Status.BLOCKED);
            throw SourceAdapterSupport.refusal("iFlowGo", error.status, error.retryAfterMs);
        }
    }
}
