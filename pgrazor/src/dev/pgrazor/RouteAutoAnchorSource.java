package dev.pgrazor;

import android.os.SystemClock;

/**
 * Experimental prototype source for an automatically projected visual route. It consumes only the bounded,
 * read-only anchor snapshot published by the native companion reader while an own visual-route token is
 * active. It reuses the PR #176 frame builder, geometry, progress and fail-closed overlay; it installs no
 * view, reads no camera directly and never touches official Routes.
 */
final class RouteAutoAnchorSource implements RouteTraceOverlay.ProjectedFrameSource {
    private static final long MAX_ANCHOR_AGE_MS = 700;
    final int width, height;
    private String raw = "";
    private RouteGroundProjection projection;
    private double[][] points;
    private boolean loop;
    private long cachedToken, completedToken;
    private double completed;

    RouteAutoAnchorSource(int width, int height) {
        this.width = width;
        this.height = height;
    }

    @Override public RouteTraceFrame latest() {
        long token = Control.engine.activeVisualRouteToken();
        if (token <= 0 || width < 40 || height < 40) return null;
        String snapshot = Control.mapAnchorSnapshot();
        if (snapshot == null || snapshot.length() == 0) return null;
        long now = SystemClock.elapsedRealtime();
        long observedAt = Control.mapAnchorReceivedAt();
        // Control renews the timestamp on every native observation, even when the camera is stationary
        // and the JSON payload is byte-identical. No fresh observation -> no stale line.
        if (observedAt <= 0 || now < observedAt || now - observedAt > MAX_ANCHOR_AGE_MS) return null;
        if (!snapshot.equals(raw)) {
            raw = snapshot;
            RouteAnchorSnapshot parsed = RouteAnchorSnapshot.parse(snapshot);
            projection = parsed == null ? null : parsed.fit(width, height);
            completedToken = 0;
        }
        if (projection == null) return null;

        MotionEngine.RouteVisualSnapshot route = Control.engine.routeVisualSnapshot(cachedToken);
        if (route == null || route.token != token) return null;
        if (route.newPoints != null) {
            points = route.newPoints;
            loop = route.looping;
            cachedToken = token;
        }
        if (points == null || points.length < 2) return null;
        if (completedToken != token) {
            completed = route.completedMeters;
            completedToken = token;
        } else {
            completed = route.completedMeters;
        }
        // Anchors are already full-viewport pixels; scale 1 keeps the builder's existing clipping/progress.
        return RouteScreenFrameBuilder.build(projection, points, loop, completed, token, now,
                width, height, 1, 1);
    }
}
