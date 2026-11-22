package dev.pgrazor;
final class IFlowGoFeedClient {
    LiveFeedData.Snapshot fetch(double lat, double lon, double radiusKm, boolean raids) throws Exception { return null; }
    LiveFeedData.Snapshot fetchGlobal(int dexId) throws Exception { return null; }
    void cancel() { }
}
