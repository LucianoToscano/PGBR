package dev.pgrazor;
final class IFlowGoFeedClient {
    LiveFeedData.Snapshot fetch(double lat, double lon, double radiusKm, boolean raids) throws Exception { return null; }
    static final int GLOBAL_PAGES = 2;
    LiveFeedData.Snapshot fetchGlobal(int dexId) throws Exception { return null; }
    LiveFeedData.Snapshot fetchGlobal(int dexId, int maxPages) throws Exception { return null; }
    void cancel() { }
}
