package dev.pgrazor;

/**
 * Presentation contract for a host that is authorized to project a route.
 * Pixel coordinates must already be projected and clipped for the exact viewport.
 * This model does not discover a camera, inspect a game or control movement.
 */
public final class RouteTraceFrame {
    public static final long MAX_AGE_MS = 250;
    public final long routeToken, capturedAtMs;
    public final int viewportWidth, viewportHeight;
    public final boolean mapVisible;
    public final RouteTraceGeometry geometry;
    public final double completedMeters;

    public RouteTraceFrame(long routeToken,long capturedAtMs,int viewportWidth,int viewportHeight,
                           boolean mapVisible,RouteTraceGeometry geometry,double completedMeters) {
        if(routeToken<=0||capturedAtMs<0||viewportWidth<=0||viewportHeight<=0||geometry==null||
                !Double.isFinite(completedMeters)||completedMeters<0||completedMeters>geometry.totalMeters())
            throw new IllegalArgumentException("Invalid projected route frame");
        for(int i=0;i<geometry.size();i++){
            if(Math.abs(geometry.x(i))>4.0*viewportWidth||Math.abs(geometry.y(i))>4.0*viewportHeight)
                throw new IllegalArgumentException("Projection outside bounded viewport");
        }
        this.routeToken=routeToken;this.capturedAtMs=capturedAtMs;
        this.viewportWidth=viewportWidth;this.viewportHeight=viewportHeight;this.mapVisible=mapVisible;
        this.geometry=geometry;this.completedMeters=completedMeters;
    }

    /** Fail closed if the supplied projection is stale, off-map, resized or for a different route. */
    public boolean usable(long nowMs,long activeToken,int width,int height,boolean hostAllowsDrawing) {
        return hostAllowsDrawing&&mapVisible&&activeToken>0&&routeToken==activeToken&&
            width==viewportWidth&&height==viewportHeight&&nowMs>=capturedAtMs&&
            nowMs-capturedAtMs<=MAX_AGE_MS;
    }
}
