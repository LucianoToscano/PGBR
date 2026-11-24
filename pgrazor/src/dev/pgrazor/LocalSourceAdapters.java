package dev.pgrazor;

/**
 * PGSharp and iPogo adapters: local-only by design. Both products are closed-source clients whose
 * data feeds are features of their own apps, not public APIs; the only legitimate data is what the
 * user exports and imports on this device. The adapters read that store and never call a remote
 * service, so they can never label another provider's rows as PGSharp or iPogo data.
 */
final class LocalSourceAdapters {
    private LocalSourceAdapters() {}

    /** Reads the user's converted PGSharp data from the local store. */
    static final class PGSharp implements SourceAdapter {
        private final LocalStoreAdapter delegate;

        PGSharp(LocalEventStore store) { delegate = new LocalStoreAdapter(store, SourceRegistry.PGSHARP); }

        @Override public String sourceId() { return SourceRegistry.PGSHARP; }
        @Override public SourceCapabilities capabilities() { return delegate.capabilities(); }
        @Override public SourceCapabilities status() { return delegate.status(); }
        @Override public String attribution() { return delegate.attribution(); }
        @Override public SourceSearchResult search(SourceQuery query, String cursor, long now) throws Exception {
            if (cursor != null && !cursor.isEmpty())
                throw new SourceUnavailableException("Os dados PGSharp locais não têm paginação.");
            return delegate.search(query, null, now);
        }
        @Override public void cancel() { delegate.cancel(); }
    }

    /** Reads the user's own iPogo export from the local store. */
    static final class IPogo implements SourceAdapter {
        private final LocalStoreAdapter delegate;

        IPogo(LocalEventStore store) { delegate = new LocalStoreAdapter(store, SourceRegistry.IPOGO); }

        @Override public String sourceId() { return SourceRegistry.IPOGO; }
        @Override public SourceCapabilities capabilities() { return delegate.capabilities(); }
        @Override public SourceCapabilities status() { return delegate.status(); }
        @Override public String attribution() { return delegate.attribution(); }
        @Override public SourceSearchResult search(SourceQuery query, String cursor, long now) throws Exception {
            if (cursor != null && !cursor.isEmpty())
                throw new SourceUnavailableException("Os dados iPogo locais não têm paginação.");
            return delegate.search(query, null, now);
        }
        @Override public void cancel() { delegate.cancel(); }
    }
}
