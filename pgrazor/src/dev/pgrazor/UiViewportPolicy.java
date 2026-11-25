package dev.pgrazor;

/** Pure layout policy, also exercised on the host JVM without Android stubs. */
final class UiViewportPolicy {
    private UiViewportPolicy() {}
    static float clamp(float value,int start,int end,int extent){
        return Math.max(start,Math.min(Math.max(start,end-Math.max(0,extent)),Float.isFinite(value)?value:start));
    }
    /** Preserve the fraction of the usable track when size or safe insets change. */
    static float resizePosition(float position,int oldStart,int oldEnd,int oldExtent,
                                int start,int end,int extent){
        int oldTrack=Math.max(0,oldEnd-oldStart-Math.max(0,oldExtent));
        float fraction=oldTrack>0&&Float.isFinite(position)?Math.max(0f,Math.min(1f,(position-oldStart)/oldTrack)):0f;
        return clamp(start+fraction*Math.max(0,end-start-Math.max(0,extent)),start,end,extent);
    }
    /**
     * Align the *visible sprite*, not just its larger tappable hitbox, to an edge.
     * When a 32dp sprite lives in a minimum 48dp hit target, centering alone
     * leaves an 8dp blank strip even after dragging the hitbox to the corner.
     * This shifts the image smoothly over that strip as the hitbox approaches
     * a safe edge, retaining the full 48dp touch target.
     */
    static float edgeAlignedCenter(float position,int edgeStart,int edgeEnd,
                                   int hitExtent,float spriteExtent) {
        float halfPadding=Math.max(0f,(hitExtent-spriteExtent)*.5f);
        float fromStart=Math.max(0f,position-edgeStart);
        float fromEnd=Math.max(0f,edgeEnd-(position+hitExtent));
        float leftShift=Math.max(0f,halfPadding-fromStart);
        float rightShift=Math.max(0f,halfPadding-fromEnd);
        return hitExtent*.5f-leftShift+rightShift;
    }
    // Corrupt or unavailable UI-scale values must not produce NaN geometry
    // or collapse the menu to a single pixel. Keep the default at 100%.
    private static float validScale(float scale){
        return Float.isFinite(scale)&&scale>0f?scale:1f;
    }
    static float fit(float requested,int width,int height,int availableWidth,int availableHeight){
        float scale=validScale(requested);
        if(width>0)scale=Math.min(scale,(float)Math.max(1,availableWidth)/width);
        if(height>0)scale=Math.min(scale,(float)Math.max(1,availableHeight)/height);
        return Math.max(0.01f,scale);
    }
    static int menuExtent(int desired,int available,float scale){
        return Math.max(1,Math.min(desired,(int)Math.floor(Math.max(1,available)/Math.max(.01f,validScale(scale)))));
    }
}
