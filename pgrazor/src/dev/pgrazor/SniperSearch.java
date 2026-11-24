package dev.pgrazor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Shared engine for Sniper 1 (quick manual search) and Sniper 2 (advanced multi-result search).
 * Both consume the same normalised records — never raw provider payloads — and both gate the
 * coordinate actions behind {@link SourceEvent#isActionable(long)} plus the app's cooldown.
 *
 * <p>Nothing here teleports. The engine only decides whether a row <em>may</em> be offered to the
 * existing teleport action, and the UI still asks for confirmation and applies the cooldown.
 */
public final class SniperSearch {
    private SniperSearch() {}

    /** Default minimum remaining life for a snipe target: no point travelling to a despawned spawn. */
    public static final long DEFAULT_MIN_TTL_SECONDS = 60L;
    /** Sniper 2 returns more rows, but never an unbounded list. */
    public static final int SNIPER2_LIMIT = 100;

    public static final class ActionTarget {
        public final boolean canCopy, canMap, canTeleport;
        public final String blockReason;

        ActionTarget(boolean canCopy, boolean canMap, boolean canTeleport, String blockReason) {
            this.canCopy = canCopy;
            this.canMap = canMap;
            this.canTeleport = canTeleport;
            this.blockReason = blockReason == null ? "" : blockReason;
        }

        static ActionTarget none(String reason) { return new ActionTarget(false, false, false, reason); }
    }

    public static final class Result {
        public final SourceEvent event;
        public final String sourceLabel;
        public final double distanceKm;
        public final long remainingMs;
        public final ActionTarget action;

        Result(SourceEvent event, double distanceKm, long remainingMs, ActionTarget action) {
            this.event = event;
            this.sourceLabel = event.sourceLabel();
            this.distanceKm = distanceKm;
            this.remainingMs = remainingMs;
            this.action = action;
        }

        /** True when this row can actually be used as a teleport destination. */
        public boolean actionable() { return action != null && action.canTeleport; }

        public String distanceText() {
            return Double.isNaN(distanceKm) ? "distância desconhecida"
                    : String.format(Locale.US, distanceKm < 1 ? "%.0f m" : "%.1f km",
                            distanceKm < 1 ? distanceKm * 1000 : distanceKm);
        }
    }

    public static final class Contribution {
        public final String sourceId;
        public final int received, shown;
        /** {@code ok}, {@code empty}, {@code unavailable} or {@code blocked}. */
        public final String status;
        public final String detail;

        Contribution(String sourceId, int received, int shown, String status, String detail) {
            this.sourceId = sourceId;
            this.received = received;
            this.shown = shown;
            this.status = status;
            this.detail = detail == null ? "" : detail;
        }
    }

    public static final class Outcome {
        public final List<Result> results;
        public final List<Contribution> contributions;
        public final int received, shown;
        public final boolean anyUnavailable;

        Outcome(List<Result> results, List<Contribution> contributions, int received, int shown,
                boolean anyUnavailable) {
            this.results = Collections.unmodifiableList(new ArrayList<>(results));
            this.contributions = Collections.unmodifiableList(new ArrayList<>(contributions));
            this.received = received;
            this.shown = shown;
            this.anyUnavailable = anyUnavailable;
        }

        public boolean isEmpty() { return results.isEmpty(); }

        /** Honest summary: names the sources that answered and the ones that did not. */
        public String summary() {
            StringBuilder sb = new StringBuilder();
            sb.append("Recebidos ").append(received).append(" · exibidos ").append(shown);
            for (Contribution contribution : contributions) {
                if ("ok".equals(contribution.status) || "empty".equals(contribution.status)) continue;
                sb.append(" · ").append(SourceRegistry.label(contribution.sourceId))
                  .append(": ").append(contribution.detail);
            }
            return sb.toString();
        }
    }

    /** Sniper 1: quick manual search, one page, ordered by distance, minimum TTL enforced. */
    public static Outcome sniper1(SourceQuery query, List<SourceAdapter> adapters,
                                  LocalEventStore local, long now,
                                  double centerLat, double centerLon, long cooldownUntilMs) throws Exception {
        SourceQuery.Builder base = query != null ? query.toBuilder() : SourceQuery.builder();
        SourceQuery effective = base
                .minTtlSeconds(query != null && query.minTtlSeconds != null
                        ? query.minTtlSeconds : Long.valueOf(DEFAULT_MIN_TTL_SECONDS))
                .includeExpired(false).includeDemo(false)
                .limit(Math.min(query != null && query.limit > 0 ? query.limit : 25, 25))
                .sort(SourceQuery.Sort.DISTANCE)
                .build();
        return run(effective, adapters, local, now, centerLat, centerLon, cooldownUntilMs, true);
    }

    /** Sniper 2: advanced search with presets, AND/OR criteria, PvP and a larger result list. */
    public static Outcome sniper2(SourceQuery query, List<SourceAdapter> adapters,
                                  LocalEventStore local, long now,
                                  double centerLat, double centerLon, long cooldownUntilMs) throws Exception {
        SourceQuery.Builder base = query != null ? query.toBuilder() : SourceQuery.builder();
        SourceQuery effective = base
                .includeExpired(false).includeDemo(false)
                .limit(Math.min(query != null && query.limit > 0 ? query.limit : SNIPER2_LIMIT, SNIPER2_LIMIT))
                .sort(query != null && query.sort != null ? query.sort : SourceQuery.Sort.DISTANCE)
                .build();
        return run(effective, adapters, local, now, centerLat, centerLon, cooldownUntilMs, false);
    }

    private static Outcome run(SourceQuery query, List<SourceAdapter> adapters, LocalEventStore local,
                               long now, double centerLat, double centerLon, long cooldownUntilMs,
                               boolean requireActionable) throws Exception {
        List<Result> results = new ArrayList<>();
        List<Contribution> contributions = new ArrayList<>();
        int received = 0;
        boolean anyUnavailable = false;

        if (local != null && local.size() > 0) {
            LocalStoreAdapter storeAdapter = new LocalStoreAdapter(local);
            SourceSearchResult page = storeAdapter.search(query, null, now);
            received += page.items.size();
            contributions.add(new Contribution(storeAdapter.sourceId(), page.items.size(),
                    page.items.size(), page.items.isEmpty() ? "empty" : "ok", page.note));
            results.addAll(collect(query, page.items, now, centerLat, centerLon, cooldownUntilMs,
                    requireActionable));
        }

        if (adapters != null) for (SourceAdapter adapter : adapters) {
            if (adapter == null) continue;
            try {
                SourceSearchResult page = adapter.search(query, null, now);
                received += page.items.size();
                contributions.add(new Contribution(adapter.sourceId(), page.items.size(),
                        page.items.size(), page.items.isEmpty() ? "empty" : "ok", page.note));
                results.addAll(collect(query, page.items, now, centerLat, centerLon, cooldownUntilMs,
                        requireActionable));
            } catch (SourceUnavailableException unavailable) {
                anyUnavailable = true;
                contributions.add(new Contribution(adapter.sourceId(), 0, 0,
                        unavailable.refused ? "blocked" : "unavailable", unavailable.getMessage()));
            }
        }

        if (query.hasCenter()) {
            results.sort((a, b) -> Double.compare(
                    Double.isNaN(a.distanceKm) ? Double.MAX_VALUE : a.distanceKm,
                    Double.isNaN(b.distanceKm) ? Double.MAX_VALUE : b.distanceKm));
        }
        if (results.size() > query.limit) results = new ArrayList<>(results.subList(0, query.limit));
        return new Outcome(results, contributions, received, results.size(), anyUnavailable);
    }

    private static List<Result> collect(SourceQuery query, List<SourceEvent> events, long now,
                                        double centerLat, double centerLon, long cooldownUntilMs,
                                        boolean requireActionable) {
        List<Result> out = new ArrayList<>();
        for (SourceEvent event : events) {
            if (event == null) continue;
            if (requireActionable && !event.hasCoordinates()) continue;
            double distance = event.hasCoordinates() && query.hasCenter()
                    ? event.distanceKm(query.centerLat, query.centerLon)
                    : Double.NaN;
            out.add(new Result(event, distance, event.remainingMs(now),
                    actionFor(event, now, cooldownUntilMs)));
        }
        return out;
    }

    /** The single place that decides whether copy/map/teleport is allowed for a row. */
    public static ActionTarget actionFor(SourceEvent event, long now, long cooldownUntilMs) {
        if (event == null) return ActionTarget.none("Resultado inválido.");
        if (event.isDemo) return ActionTarget.none("Resultado de demonstração: sem ação de coordenada.");
        if (!event.hasCoordinates())
            return ActionTarget.none("Sem coordenada liberada pela fonte (reveal_code não é coordenada).");
        if (event.coordinatePrecision != SourceEvent.CoordinatePrecision.EXACT)
            return ActionTarget.none("Coordenada aproximada: não pode ser usada como destino.");
        if (event.expired(now)) return ActionTarget.none("Resultado expirado.");
        if (cooldownUntilMs > now)
            return new ActionTarget(true, true, false,
                    "Teleporte em cooldown por mais " + ((cooldownUntilMs - now) / 1000) + " s.");
        return new ActionTarget(true, true, true, "");
    }
}
