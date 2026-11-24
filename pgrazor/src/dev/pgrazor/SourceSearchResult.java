package dev.pgrazor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * One page of results from a source, plus the provenance the UI must show: which filters the
 * origin applied, which PGBR applied, whether another page exists and what coverage was promised.
 *
 * <p>{@code hasMore} is a claim from the origin, not from the app: when a source does not expose a
 * cursor the adapter sets it to {@code false} and the UI says "página única / cobertura limitada".
 */
public final class SourceSearchResult {
    public final String sourceId;
    public final List<SourceEvent> items;
    public final boolean hasMore;
    public final String nextCursor;
    public final Integer totalKnown;
    public final long retrievedAt;
    public final String coverage;
    public final List<String> filtersAppliedRemotely;
    public final List<String> filtersAppliedLocally;
    public final List<String> unsupportedFilters;
    public final String note;

    public SourceSearchResult(String sourceId, List<SourceEvent> items, boolean hasMore,
                              String nextCursor, Integer totalKnown, long retrievedAt, String coverage,
                              List<String> filtersAppliedRemotely, List<String> filtersAppliedLocally,
                              List<String> unsupportedFilters, String note) {
        this.sourceId = sourceId == null ? "" : sourceId;
        this.items = Collections.unmodifiableList(new ArrayList<>(items == null
                ? Collections.<SourceEvent>emptyList() : items));
        this.hasMore = hasMore;
        this.nextCursor = nextCursor;
        this.totalKnown = totalKnown;
        this.retrievedAt = retrievedAt;
        this.coverage = coverage == null ? "" : coverage;
        this.filtersAppliedRemotely = Collections.unmodifiableList(new ArrayList<>(
                filtersAppliedRemotely == null ? Collections.<String>emptyList() : filtersAppliedRemotely));
        this.filtersAppliedLocally = Collections.unmodifiableList(new ArrayList<>(
                filtersAppliedLocally == null ? Collections.<String>emptyList() : filtersAppliedLocally));
        this.unsupportedFilters = Collections.unmodifiableList(new ArrayList<>(
                unsupportedFilters == null ? Collections.<String>emptyList() : unsupportedFilters));
        this.note = note == null ? "" : note;
    }

    public static SourceSearchResult empty(String sourceId, long now, String note) {
        return new SourceSearchResult(sourceId, Collections.<SourceEvent>emptyList(), false, null,
                0, now, "", Collections.<String>emptyList(), Collections.<String>emptyList(),
                Collections.<String>emptyList(), note);
    }

    public boolean isEmpty() { return items.isEmpty(); }
}
