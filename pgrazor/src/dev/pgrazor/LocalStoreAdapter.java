package dev.pgrazor;

import java.util.ArrayList;
import java.util.List;

/**
 * Local adapter over the imported store: the only adapter that always works offline. It applies the
 * query locally, reports every filter as local, and never claims pagination (there is no cursor).
 *
 * <p>This is what powers the "dados importados" mode, the PGSharp and iPogo local adapters (by
 * source id) and the offline Sniper fallback.
 */
final class LocalStoreAdapter implements SourceAdapter {
    private final LocalEventStore store;
    private final String sourceId;
    private final SourceCapabilities capabilities;

    /** Adapter over every imported record regardless of origin. */
    LocalStoreAdapter(LocalEventStore store) {
        this(store, "import");
    }

    LocalStoreAdapter(LocalEventStore store, String sourceId) {
        this.store = store == null ? new LocalEventStore() : store;
        this.sourceId = sourceId == null ? "import" : sourceId;
        SourceCapabilities declared = SourceRegistry.of(this.sourceId);
        this.capabilities = declared == null
                ? SourceCapabilities.builder(this.sourceId)
                        .status(SourceCapabilities.Status.LOCAL_ONLY)
                        .originKind(OriginKind.PERSONAL_IMPORT)
                        .coverage("Somente dados importados neste aparelho.")
                        .note("Nenhuma chamada remota: a origem é o arquivo do próprio usuário.")
                        .supports(SourceCapabilities.SPECIES, SourceCapabilities.Support.LOCAL)
                        .supports(SourceCapabilities.IV_PERCENT, SourceCapabilities.Support.LOCAL)
                        .supports(SourceCapabilities.IV_ADS, SourceCapabilities.Support.LOCAL)
                        .supports(SourceCapabilities.CP, SourceCapabilities.Support.LOCAL)
                        .supports(SourceCapabilities.LEVEL, SourceCapabilities.Support.LOCAL)
                        .supports(SourceCapabilities.GENDER, SourceCapabilities.Support.LOCAL)
                        .supports(SourceCapabilities.SIZE, SourceCapabilities.Support.LOCAL)
                        .supports(SourceCapabilities.WEATHER, SourceCapabilities.Support.LOCAL)
                        .supports(SourceCapabilities.PVP, SourceCapabilities.Support.LOCAL)
                        .supports(SourceCapabilities.TTL, SourceCapabilities.Support.LOCAL)
                        .build()
                : declared;
    }

    @Override public String sourceId() { return sourceId; }

    @Override public SourceCapabilities capabilities() { return capabilities; }

    @Override public SourceCapabilities status() { return capabilities; }

    @Override public String attribution() {
        return capabilities.attribution == null || capabilities.attribution.isEmpty()
                ? "Dados importados pelo usuário" : capabilities.attribution;
    }

    @Override public SourceSearchResult search(SourceQuery query, String cursor, long now) {
        List<SourceEvent> rows = "import".equals(sourceId) ? store.all() : store.bySource(sourceId);
        EventFilterEngine.Outcome outcome = EventFilterEngine.apply(query, rows, now);
        List<String> local = new ArrayList<>(outcome.appliedLocally);
        return new SourceSearchResult(sourceId, outcome.kept, false, null,
                Integer.valueOf(rows.size()), now,
                capabilities.coverage.isEmpty() ? "Somente dados importados neste aparelho."
                        : capabilities.coverage,
                new ArrayList<String>(), local, outcome.unsupported,
                "Nenhuma chamada remota: estes são os dados importados por você ("
                        + rows.size() + " registro(s) guardados).");
    }

    @Override public void cancel() { /* nothing in flight: local data only */ }
}
