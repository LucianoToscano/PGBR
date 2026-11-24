package dev.pgrazor;

/** Immutable visual preferences, independent of location, Android and movement. */
public final class RouteTraceStyle {
    public static final int DEFAULT_WIDTH = 5, DEFAULT_OPACITY = 100;
    public static final int DEFAULT_REMAINING = 0xffeeeeee, DEFAULT_COMPLETED = 0xff168fea;
    public final int width, opacity, remaining, completed;
    public final boolean showProgress, showMarkers, showPreview, showSummary;

    public RouteTraceStyle(int width,int opacity,int remaining,int completed,
            boolean showProgress,boolean showMarkers,boolean showPreview,boolean showSummary) {
        this.width=Math.max(2,Math.min(12,width));
        this.opacity=Math.max(30,Math.min(100,opacity));
        this.remaining=remaining|0xff000000;this.completed=completed|0xff000000;
        this.showProgress=showProgress;this.showMarkers=showMarkers;
        this.showPreview=showPreview;this.showSummary=showSummary;
    }
    public int color(int rgb) { return (Math.round(255*opacity/100f)<<24)|(rgb&0xffffff); }
    public static RouteTraceStyle defaults() {
        return new RouteTraceStyle(DEFAULT_WIDTH,DEFAULT_OPACITY,DEFAULT_REMAINING,
                DEFAULT_COMPLETED,true,true,true,true);
    }
}
