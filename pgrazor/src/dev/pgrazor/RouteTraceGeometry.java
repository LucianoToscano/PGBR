package dev.pgrazor;

/**
 * Independent, read-only route drawing model for a PGBR-owned map surface.
 * Inputs are already projected by that surface, not geographic degrees.
 * This class never reads GPS, modifies movement or connects to the game.
 */
public final class RouteTraceGeometry {
    public static final int MAX_POINTS = 5000;
    private final double[] x, y, meters;

    public RouteTraceGeometry(double[] x, double[] y, double[] meters) {
        if (x == null || y == null || meters == null || x.length != y.length ||
            x.length != meters.length || x.length < 2 || x.length > MAX_POINTS)
            throw new IllegalArgumentException("Invalid route geometry");
        this.x = x.clone(); this.y = y.clone(); this.meters = meters.clone();
        if (this.meters[0] != 0) throw new IllegalArgumentException("Invalid start distance");
        for (int i = 0; i < this.x.length; i++) {
            if (!Double.isFinite(this.x[i]) || !Double.isFinite(this.y[i]) ||
                !Double.isFinite(this.meters[i]) || this.meters[i] < 0 ||
                (i > 0 && this.meters[i] < this.meters[i-1]))
                throw new IllegalArgumentException("Invalid route point");
        }
    }

    public int size() { return x.length; }
    public double x(int i) { return x[i]; }
    public double y(int i) { return y[i]; }
    public double totalMeters() { return meters[meters.length-1]; }

    public interface SegmentSink {
        void segment(double x1, double y1, double x2, double y2, boolean completed);
    }

    /** Stream segments in route order; split the current leg at the supplied local distance. */
    public void drawSegments(double completedMeters, SegmentSink sink) {
        if (sink == null || !Double.isFinite(completedMeters) ||
            completedMeters < 0 || completedMeters > totalMeters())
            throw new IllegalArgumentException("Invalid route progress");
        for (int i = 1; i < x.length; i++) {
            double a = meters[i-1], b = meters[i];
            if (b == a) continue;
            if (completedMeters <= a) {
                sink.segment(x[i-1], y[i-1], x[i], y[i], false);
            } else if (completedMeters >= b) {
                sink.segment(x[i-1], y[i-1], x[i], y[i], true);
            } else {
                double f = (completedMeters-a)/(b-a);
                double sx = x[i-1]*(1-f)+x[i]*f, sy = y[i-1]*(1-f)+y[i]*f;
                sink.segment(x[i-1], y[i-1], sx, sy, true);
                sink.segment(sx, sy, x[i], y[i], false);
            }
        }
    }
}
