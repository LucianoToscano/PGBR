package dev.pgrazor;

import java.util.ArrayList;
import java.util.List;

/**
 * PoGoCoordinates adapter. The site's search can answer with a {@code reveal_code} instead of
 * coordinates: such a row keeps {@code coordinate_precision=unavailable} and {@code lat/lon=null},
 * so the radar shows "Localização não liberada" and never offers teleport.
 *
 * <p>The remote call needs the site's own session, which lives in the UI layer. This adapter takes a
 * {@link PayloadSource} so the dialog can inject {@code PogoFeedClient}; without one it refuses
 * instead of inventing a response.
 */
final class PoGoCoordinatesSourceAdapter implements SourceAdapter {
    /** Seam to the UI-owned client (which holds the site session). Returns raw JSON or a JSON array. */
    interface PayloadSource {
        Object fetch(SourceQuery query, String cursor) throws Exception;
    }

    private final PayloadSource payload;
    private volatile SourceCapabilities capabilities = SourceRegistry.of(SourceRegistry.POGO_COORDINATES);

    PoGoCoordinatesSourceAdapter() { this(null); }

    PoGoCoordinatesSourceAdapter(PayloadSource payload) { this.payload = payload; }

    @Override public String sourceId() { return SourceRegistry.POGO_COORDINATES; }

    @Override public SourceCapabilities capabilities() { return capabilities; }

    @Override public SourceCapabilities status() { return capabilities; }

    @Override public String attribution() { return capabilities.attribution; }

    @Override public void cancel() { /* the UI-owned client cancels its own request */ }

    /** True when at least one row of this result carries a reveal code instead of coordinates. */
    static int lockedRows(List<SourceEvent> events) {
        int locked = 0;
        for (SourceEvent event : events)
            if (event.coordinatePrecision == SourceEvent.CoordinatePrecision.UNAVAILABLE) locked++;
        return locked;
    }

    @Override public SourceSearchResult search(SourceQuery query, String cursor, long now) throws Exception {
        if (payload == null)
            throw new SourceUnavailableException(
                    "Conector do PoGoCoordinates não configurado nesta sessão. A fonte pode devolver apenas "
                            + "reveal_code; o PGBR não converte reveal em coordenada nem automatiza desbloqueio.");
        Object raw = payload.fetch(query == null ? SourceQuery.builder().build() : query, cursor);
        PogoFeedData.Snapshot snapshot = PogoFeedData.parse(raw, now);
        List<SourceEvent> events = PogoFeedData.toSourceEvents(snapshot, now);
        EventFilterEngine.Outcome outcome = EventFilterEngine.apply(
                query == null ? SourceQuery.builder().build() : query, events, now);
        int locked = lockedRows(events);
        return new SourceSearchResult(sourceId(), outcome.kept, false, null,
                Integer.valueOf(events.size()), now, capabilities.coverage,
                new ArrayList<String>(), outcome.appliedLocally, outcome.unsupported,
                snapshot.note + (locked > 0 ? " " + locked
                        + " resultado(s) com localização não liberada (reveal_code)." : ""));
    }
}
