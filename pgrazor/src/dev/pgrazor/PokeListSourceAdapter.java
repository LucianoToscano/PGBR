package dev.pgrazor;

import java.util.ArrayList;
import java.util.List;

/**
 * PokeList adapter: one bounded request per category, no pagination sweep, no retry after a refusal.
 * The query's centre and radius come from the app; the species allowlist is forwarded because it is
 * the only server-side bound that keeps a category request finite.
 */
final class PokeListSourceAdapter implements SourceAdapter {
    private final PokeListFeedClient client;
    private volatile SourceCapabilities capabilities = SourceRegistry.of(SourceRegistry.POKELIST);

    PokeListSourceAdapter() { this(new PokeListFeedClient()); }

    PokeListSourceAdapter(PokeListFeedClient client) { this.client = client; }

    @Override public String sourceId() { return SourceRegistry.POKELIST; }

    @Override public SourceCapabilities capabilities() { return capabilities; }

    @Override public SourceCapabilities status() { return capabilities; }

    @Override public String attribution() { return capabilities.attribution; }

    @Override public void cancel() { client.cancel(); }

    @Override public SourceSearchResult search(SourceQuery query, String cursor, long now) throws Exception {
        SourceQuery q = query == null ? SourceQuery.builder().build() : query;
        if (cursor != null && !cursor.isEmpty())
            throw new SourceUnavailableException(
                    "PokeList não informa paginação: a consulta é uma página única por área/espécie.");
        if (q.centerLat == null || q.centerLon == null)
            throw new SourceUnavailableException(
                    "Selecione um centro de busca antes de consultar a PokeList.");
        if (!capabilities.supportsRemoteSearch)
            throw new SourceUnavailableException("O conector da PokeList está desativado: "
                    + capabilities.note);
        int distance = q.radiusKm == null ? 0 : Math.max(0, (int) Math.ceil(q.radiusKm));
        int dex = q.pokemonIds.isEmpty() ? -1 : q.pokemonIds.iterator().next();
        if (distance == 0 && dex < 1)
            throw new SourceUnavailableException(
                    "Sem raio, a PokeList exige uma espécie selecionada para limitar a resposta.");
        PokeListFeedData.Category category = SourceAdapterSupport.categoryFor(q);
        try {
            PokeListFeedData.EventPage page = client.fetchEvents(q.centerLat, q.centerLon, distance, category, dex);
            capabilities = SourceCapabilities.withStatus(capabilities, SourceCapabilities.Status.UNVERIFIED);
            EventFilterEngine.Outcome outcome = EventFilterEngine.apply(q, page.events, now);
            List<String> remote = new ArrayList<>();
            if (dex >= 1) remote.add("espécies");
            if (distance > 0) remote.add("raio");
            remote.add("tipo de evento");
            return new SourceSearchResult(sourceId(), outcome.kept, false, null,
                    Integer.valueOf(page.received), now, capabilities.coverage, remote,
                    outcome.appliedLocally, outcome.unsupported, page.note);
        } catch (LiveFeedClient.FeedException error) {
            throw SourceAdapterSupport.refusal("PokeList", error.status, error.retryAfterMs);
        }
    }
}
