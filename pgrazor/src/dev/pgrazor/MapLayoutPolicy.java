package dev.pgrazor;

/** Pure layout arithmetic in pixels. Overflow scrolls rather than hiding the map or controls. */
final class MapLayoutPolicy {
    private MapLayoutPolicy() {}
    static int height(int viewport,int reserved,int minimum,int preferred,int topOverlay,int bottomOverlay,int gap) {
        long floor=Math.max(Math.max(0,minimum),(long)Math.max(0,topOverlay)+Math.max(0,bottomOverlay)+Math.max(0,gap));
        long remaining=viewport<0?Math.max(0,preferred):Math.max(0L,(long)viewport-Math.max(0,reserved));
        return (int)Math.min(Integer.MAX_VALUE,Math.max(floor,remaining));
    }
}
