package dev.pgrazor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;

/**
 * The shared search engine behind Sniper 1 and Sniper 2.
 *
 * <p>Both panels use this one pipeline:
 * <pre>
 *   provider rows -> {@link SpawnContract} normalisation -> de-duplication -> expiry
 *   -> per-slot filters -> ordering -> display
 * </pre>
 *
 * <p>Its job is to make the difference between "the source has nothing" and "the PGBR hid
 * everything" impossible to confuse: {@link Result#received}, {@link Result#droppedByFilter} and
 * {@link Result#visible} are counted separately, and {@link Result#state} names exactly what
 * happened. A single page of a paged provider is never reported as a complete worldwide search:
 * {@link Result#coverage} says which part of the source was actually consulted.
 *
 * <p>This class performs no I/O. The panel supplies a {@link Source} implementation, so the whole
 * chain can be exercised with fixtures on a JVM (and with a real connector on a device).
 */
public final class SpawnSearch {
    private SpawnSearch() {}

    /** How far the consulted data can reach. Never upgraded by guesswork. */
    public enum Coverage {
        /** Every row the provider returned for the request, with no pagination involved. */
        FULL_RESPONSE,
        /** Only the first page of a paged endpoint was read. */
        FIRST_PAGE,
        /** Several consecutive pages were read, but the provider reported more results beyond them. */
        PARTIAL_PAGES,
        /** One area around a point (radius query). */
        AREA,
        /** Nothing was consulted (error, cancel, empty request). */
        NONE
    }

    /** What the caller may tell the user. Each value maps to a distinct message. */
    public enum State {
        OK,
        /** Provider replied, but with zero rows. */
        SOURCE_EMPTY,
        /** Provider replied rows, but the slot's filters removed all of them. */
        ALL_FILTERED_OUT,
        /** Provider refused access (401/403) — authorisation is required. */
        ACCESS_DENIED,
        /** Provider needs a sign-in or key that is missing/expired. */
        AUTH_REQUIRED,
        /** Provider rate-limited us (429). */
        RATE_LIMITED,
        /** Provider failed (5xx, malformed body, unexpected field). */
        SOURCE_ERROR,
        /** No network. */
        NETWORK_ERROR,
        /** The caller cancelled the request (a newer query replaced it, or the panel closed). */
        CANCELLED,
        /** Rows exist, but their coordinates are withheld behind an authorised reveal. */
        COORDINATES_BLOCKED,
        /** Snapshot older than the allowed age. */
        STALE,
        /** The request itself was invalid (missing species for a provider that requires one). */
        INVALID_REQUEST
    }

    /** A provider connector. Implementations own their transport and error mapping. */
    public interface Source {
        String id();
        SpawnContract.Capabilities capabilities();
        /**
         * Returns the rows for this request. Must throw {@link LiveFeedClient.FeedException} with the
         * provider's status/retry-after so {@link #classify} can distinguish access, auth and rate
         * limiting. Implementations must respect their own documented limits.
         */
        List<SpawnContract.Spawn> fetch(Request request) throws Exception;
        /** Consultant coverage of the last call, e.g. FIRST_PAGE for a paged endpoint. */
        Coverage coverage();
        /** Aborts the in-flight request; called when a newer request supersedes this one. */
        void cancel();
    }

    /** A validated search request. Built by the panel from its own slot state. */
    public static final class Request {
        public final String sourceId;
        /** Single species narrowing sent to providers that support it; -1 asks for everything. */
        public final int species;
        /** True only for a global (worldwide) request; false means "around the current position". */
        public final boolean global;
        public final double latitude, longitude, radiusKm;
        public final boolean raidsOnly;

        public Request(String sourceId, int species, boolean global, double latitude, double longitude,
                       double radiusKm, boolean raidsOnly) {
            this.sourceId = sourceId;
            this.species = species;
            this.global = global;
            this.latitude = latitude;
            this.longitude = longitude;
            this.radiusKm = radiusKm;
            this.raidsOnly = raidsOnly;
        }

        /** True when a radius query has usable coordinates. */
        public boolean hasOrigin() {
            return SpawnContract.validCoordinates(latitude, longitude);
        }
    }

    /** Outcome of one search, including everything the UI must be able to say honestly. */
    public static final class Result {
        public final State state;
        public final Coverage coverage;
        public final String sourceId;
        /** Rows the provider actually delivered after normalisation. */
        public final int received;
        /** Rows removed by the slot's filters (never "not received"). */
        public final int droppedByFilter;
        /** Rows removed as duplicates of another row in the same response (not a filter). */
        public final int duplicates;
        /** Rows removed because they had already despawned. */
        public final int expired;
        public final List<SpawnContract.Spawn> visible;
        /** Suggested wait from the provider (Retry-After) in ms; 0 when not applicable. */
        public final long retryAfterMs;
        /** When the provider answer was produced; 0 for results that never reached a source. */
        public final long fetchedAt;
        public final String message;
        /**
         * Rows the source delivered, after de-duplication and before filters. Never empty when
         * {@link #received} &gt; 0, and reused by {@link #refilter} so local filter changes do not
         * depend on a mutable copy held by the UI.
         */
        public final List<SpawnContract.Spawn> receivedRows;

        Result(State state, Coverage coverage, String sourceId, int received, int droppedByFilter,
               int duplicates, int expired, List<SpawnContract.Spawn> visible, long retryAfterMs,
               long fetchedAt, String message) {
            this(state, coverage, sourceId, received, droppedByFilter, duplicates, expired, visible,
                    retryAfterMs, fetchedAt, message, visible);
        }

        /** Full constructor, including the unfiltered rows a later {@link #refilter} needs. */
        Result(State state, Coverage coverage, String sourceId, int received, int droppedByFilter,
               int duplicates, int expired, List<SpawnContract.Spawn> visible, long retryAfterMs,
               long fetchedAt, String message, List<SpawnContract.Spawn> receivedRows) {
            this.state = state;
            this.coverage = coverage;
            this.sourceId = sourceId == null ? "" : sourceId;
            this.received = received;
            this.droppedByFilter = droppedByFilter;
            this.duplicates = duplicates;
            this.expired = expired;
            this.visible = Collections.unmodifiableList(new ArrayList<>(visible));
            this.retryAfterMs = retryAfterMs;
            this.fetchedAt = fetchedAt;
            this.message = message == null ? "" : message;
            this.receivedRows = Collections.unmodifiableList(new ArrayList<>(
                    receivedRows == null ? visible : receivedRows));
        }

        public boolean hasResults() {
            return !visible.isEmpty();
        }

        /** True when the provider was asked for less than the whole source. */
        public boolean partialCoverage() {
            return coverage == Coverage.FIRST_PAGE || coverage == Coverage.AREA
                    || coverage == Coverage.PARTIAL_PAGES;
        }

        /** Number of result rows shown, i.e. after every filter. */
        public int count() {
            return visible.size();
        }
    }

    /** The slot-specific filtering the engine delegates to; Sniper 1 and Sniper 2 each pass their own. */
    public interface Filter {
        boolean accepts(SpawnContract.Spawn spawn, long now);
    }

    /**
     * Runs the whole chain and never throws: every failure is classified into a {@link State} so the
     * UI can distinguish access denial, rate limiting, empty source and "all filtered locally".
     */
    /**
     * Re-applies filters, expiry and ordering to a list already received, WITHOUT touching the network.
     *
     * <p>This is what a local-only filter change must use: the snapshot is still valid, so calling the
     * provider again would burn quota and could even return less than the first answer. Coverage and the
     * received/duplicate counters are carried over from the original result, so the UI never claims a new
     * consultation happened.
     */
    public static Result refilter(Result previous, Filter filter, SpawnContract.Order order,
                                  double originLat, double originLon, long now) {
        if (previous == null)
            return empty(State.INVALID_REQUEST, Coverage.NONE, "", 0L,
                    "Nenhuma consulta anterior para refiltrar.");
        // Sempre a partir das linhas recebidas guardadas no resultado: refiltrar de novo (ou três
        // vezes) não acumula perdas como aconteceria sobre a lista já filtrada.
        List<SpawnContract.Spawn> rows = previous.receivedRows;
        int received = previous.received;
        List<SpawnContract.Spawn> unique = SpawnContract.dedupe(rows);
        int duplicates = previous.duplicates;
        List<SpawnContract.Spawn> alive = SpawnContract.alive(unique, now);
        int expired = unique.size() - alive.size();
        List<SpawnContract.Spawn> visible = new ArrayList<>();
        boolean blocked = false;
        for (SpawnContract.Spawn row : alive) {
            if (row.coordinatesStatus == SpawnContract.Coordinates.BLOCKED) blocked = true;
            if (filter == null || filter.accepts(row, now)) visible.add(row);
        }
        int dropped = alive.size() - visible.size();
        visible = new ArrayList<>(SpawnContract.sort(visible, order, originLat, originLon, now));
        State state;
        if (!visible.isEmpty()) state = State.OK;
        else if (received == 0) state = State.SOURCE_EMPTY;
        else if (dropped > 0 || expired > 0) state = State.ALL_FILTERED_OUT;
        else if (blocked) state = State.COORDINATES_BLOCKED;
        else state = State.SOURCE_EMPTY;
        String message = describe(state, received, dropped, previous.coverage)
                + " Filtros reaplicados sobre a consulta já recebida (sem nova requisição).";
        return new Result(state, previous.coverage, previous.sourceId, received, dropped, duplicates,
                expired, visible, previous.retryAfterMs, previous.fetchedAt, message, unique);
    }

    public static Result run(Source source, Request request, Filter filter,
                             SpawnContract.Order order, double originLat, double originLon, long now) {
        if (source == null || request == null)
            return empty(State.INVALID_REQUEST, Coverage.NONE, request == null ? "" : request.sourceId,
                    0L, "Consulta inválida.");
        String sourceId = request.sourceId == null ? source.id() : request.sourceId;
        List<SpawnContract.Spawn> rows;
        try {
            rows = source.fetch(request);
        } catch (Exception failure) {
            return failure(source, sourceId, failure);
        }
        Coverage coverage = source.coverage() == null ? Coverage.NONE : source.coverage();
        if (rows == null) rows = Collections.emptyList();

        int received = rows.size();
        List<SpawnContract.Spawn> unique = SpawnContract.dedupe(rows);
        int duplicates = received - unique.size();
        List<SpawnContract.Spawn> alive = SpawnContract.alive(unique, now);
        int expired = unique.size() - alive.size();

        List<SpawnContract.Spawn> visible = new ArrayList<>();
        boolean blocked = false;
        for (SpawnContract.Spawn row : alive) {
            if (row.coordinatesStatus == SpawnContract.Coordinates.BLOCKED) blocked = true;
            if (filter == null || filter.accepts(row, now)) visible.add(row);
        }
        int dropped = alive.size() - visible.size();
        visible = new ArrayList<>(SpawnContract.sort(visible, order, originLat, originLon, now));

        State state;
        if (!visible.isEmpty()) {
            state = State.OK;
        } else if (received == 0) {
            // The provider answered, with nothing. Distinct from "your filters hid everything".
            state = State.SOURCE_EMPTY;
        } else if (dropped > 0 || expired > 0) {
            state = State.ALL_FILTERED_OUT;
        } else if (blocked) {
            state = State.COORDINATES_BLOCKED;
        } else {
            // Rows were received, none of them was filtered, expired or blocked, yet nothing can be
            // shown: the honest answer is that the source gave nothing usable for this request.
            state = State.SOURCE_EMPTY;
        }
        return new Result(state, coverage, sourceId, received, dropped, duplicates, expired, visible,
                0L, now, describe(state, received, dropped, coverage), unique);
    }

    /** Maps a connector failure to a user-visible state without leaking raw provider content. */
    static Result failure(Source source, String sourceId, Exception failure) {
        if (failure == null)
            return empty(State.SOURCE_ERROR, coverageOf(source), sourceId, 0L, "Falha desconhecida.");
        // A cancellation is not a provider problem and must not be shown as an error.
        String message = failure.getMessage() == null ? "" : failure.getMessage().toLowerCase();
        if (message.contains("cancelad")) {
            return empty(State.CANCELLED, coverageOf(source), sourceId, 0L,
                    "Consulta cancelada; uma consulta mais nova assumiu o lugar.");
        }
        if (failure instanceof LiveFeedClient.FeedException) {
            LiveFeedClient.FeedException feed = (LiveFeedClient.FeedException) failure;
            State state;
            if (feed.status == 401 || feed.status == 403) state = State.ACCESS_DENIED;
            else if (feed.status == 429) state = State.RATE_LIMITED;
            else if (feed.status >= 500) state = State.SOURCE_ERROR;
            else if (feed.status == 0 && message.contains("rede")) state = State.NETWORK_ERROR;
            else if (feed.status == 0) state = State.SOURCE_ERROR;
            else state = State.SOURCE_ERROR;
            return empty(state, Coverage.NONE, sourceId, Math.max(0L, feed.retryAfterMs),
                    describe(state, 0, 0, Coverage.NONE));
        }
        if (failure instanceof java.net.UnknownHostException
                || failure instanceof java.net.ConnectException
                || failure instanceof java.net.SocketTimeoutException
                || failure instanceof java.io.IOException) {
            return empty(State.NETWORK_ERROR, Coverage.NONE, sourceId, 0L,
                    describe(State.NETWORK_ERROR, 0, 0, Coverage.NONE));
        }
        return empty(State.SOURCE_ERROR, Coverage.NONE, sourceId, 0L,
                describe(State.SOURCE_ERROR, 0, 0, Coverage.NONE));
    }

    private static Coverage coverageOf(Source source) {
        try {
            Coverage coverage = source == null ? null : source.coverage();
            return coverage == null ? Coverage.NONE : coverage;
        } catch (RuntimeException ignored) {
            return Coverage.NONE;
        }
    }

    /**
     * User-facing sentence for a state. Deliberately explicit about the difference between "the
     * source returned nothing" and "the source returned rows that your filters removed".
     */
    public static String describe(State state, int received, int dropped, Coverage coverage) {
        switch (state) {
            case OK:
                return received + " registro(s) recebido(s); " + dropped + " removido(s) por filtro; "
                        + coverageSuffix(coverage);
            case SOURCE_EMPTY:
                return "A fonte respondeu com zero registros para esta consulta" + coverageSuffix(coverage)
                        + ". Isso não significa ausência mundial do Pokémon.";
            case ALL_FILTERED_OUT:
                return "A fonte enviou " + received + " registro(s), mas nenhum passou nos filtros "
                        + "desta busca (espécie, IV, nível, PC, gênero, forma, distância, PvP ou "
                        + "validade). Ajuste ou limpe os filtros.";
            case ACCESS_DENIED:
                return "A fonte recusou o acesso (HTTP 401/403). É necessária autorização do "
                        + "fornecedor; o PGBR não contorna essa restrição.";
            case AUTH_REQUIRED:
                return "A fonte exige login ou chave que não está disponível nesta busca.";
            case RATE_LIMITED:
                return "A fonte limitou as consultas (HTTP 429). Aguardando o intervalo informado "
                        + "por ela antes de tentar de novo.";
            case SOURCE_ERROR:
                return "A fonte falhou ou respondeu em formato inesperado. Nenhum dado foi inventado.";
            case NETWORK_ERROR:
                return "Sem conexão com a fonte. Verifique a rede do aparelho.";
            case CANCELLED:
                return "Consulta cancelada; uma consulta mais nova assumiu o lugar.";
            case COORDINATES_BLOCKED:
                return "A fonte devolveu registros sem coordenadas reveladas. Use o fluxo autorizado "
                        + "do próprio fornecedor para revelar; o PGBR não contorna quotas.";
            case STALE:
                return "Dados antigos: atualize antes de usar um resultado.";
            case INVALID_REQUEST:
            default:
                return "Consulta inválida para esta fonte.";
        }
    }

    private static String coverageSuffix(Coverage coverage) {
        switch (coverage) {
            case FIRST_PAGE:
                return "cobertura: apenas a primeira página da fonte (não é uma busca mundial completa)";
            case PARTIAL_PAGES:
                return "cobertura: parcial — várias páginas consultadas, mas a fonte informa mais "
                        + "resultados além delas";
            case AREA:
                return "cobertura: área consultada ao redor do ponto escolhido";
            case FULL_RESPONSE:
                return "cobertura: todos os registros enviados pela fonte para esta consulta";
            case NONE:
            default:
                return "cobertura: nenhuma";
        }
    }

    private static Result empty(State state, Coverage coverage, String sourceId, long retryAfterMs,
                                String message) {
        return new Result(state, coverage, sourceId, 0, 0, 0, 0,
                Collections.<SpawnContract.Spawn>emptyList(), retryAfterMs, 0L, message);
    }

    /**
     * Species allowlist helper shared by both slots: a set of de-duplicated National Dex ids in
     * {@code 1..1025}. An empty set joins every species.
     */
    public static final class SpeciesSet {
        private final TreeSet<Integer> ids = new TreeSet<>();
        public static final int MAX_SPECIES_ID = 1025;

        public SpeciesSet add(int dex) {
            if (dex >= 1 && dex <= MAX_SPECIES_ID) ids.add(dex);
            return this;
        }

        public boolean isEmpty() { return ids.isEmpty(); }
        public int size() { return ids.size(); }
        public boolean contains(int dex) { return ids.contains(dex); }
        public List<Integer> list() { return Collections.unmodifiableList(new ArrayList<>(ids)); }
        /** The only species, or -1 when the set is empty or holds several (never invents a pick). */
        public int single() { return ids.size() == 1 ? ids.first() : -1; }

        /** Parses "25, #6; 150" into ids, ignoring blanks and out-of-range tokens. */
        public static SpeciesSet parse(String csv) {
            SpeciesSet set = new SpeciesSet();
            if (csv == null) return set;
            for (String token : csv.split("[,;#\\s]+")) {
                String value = token.trim();
                if (value.isEmpty()) continue;
                try {
                    set.add(Integer.parseInt(value));
                } catch (NumberFormatException ignored) {
                    // Non-numeric tokens are ignored here; strict validation belongs to the editor.
                }
            }
            return set;
        }
    }
}
