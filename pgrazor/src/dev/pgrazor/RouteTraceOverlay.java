package dev.pgrazor;

import android.content.Context;
import android.os.SystemClock;
import android.view.View;
import android.widget.FrameLayout;

/**
 * Optional transparent visual layer. The host may provide authorized projected frames.
 * Without a provider it attaches no view, makes no requests and never displays a guessed route.
 * All calls are on the activity UI thread.
 */
final class RouteTraceOverlay {
    interface ProjectedFrameSource {
        RouteTraceFrame latest();
    }
    private final Context context;
    private final FrameLayout parent;
    private final ProjectedFrameSource source;
    private RouteTraceView view;
    private RouteTraceFrame displayed;
    private boolean closed;
    private final Runnable expire=()->hide();

    RouteTraceOverlay(Context context,FrameLayout parent,ProjectedFrameSource source) {
        this.context=context;this.parent=parent;this.source=source;
    }

    void refresh(boolean allowed,long token,RouteTraceStyle style) {
        if(closed||!allowed||token<=0||source==null||style==null||
                !parent.isAttachedToWindow()||!parent.hasWindowFocus()) { hide();return; }
        RouteTraceFrame frame;
        try { frame=source.latest(); }
        catch(RuntimeException failure) { hide();return; }
        long now=SystemClock.elapsedRealtime();
        if(frame==null||!frame.usable(now,token,parent.getWidth(),parent.getHeight(),allowed)){
            hide();return;
        }
        if(view==null) {
            view=new RouteTraceView(context);
            parent.addView(view,0,new FrameLayout.LayoutParams(-1,-1));
        }
        view.setStyle(style);
        if(displayed!=frame) {
            displayed=frame;
            view.setTrace(frame.geometry,frame.completedMeters);
        }
        view.setVisibility(View.VISIBLE);
        view.removeCallbacks(expire);
        view.postDelayed(expire,Math.max(1,RouteTraceFrame.MAX_AGE_MS-(now-frame.capturedAtMs)+1));
    }

    void hide() {
        displayed=null;
        if(view!=null) {
            view.removeCallbacks(expire);
            view.clearTrace();
            view.setVisibility(View.GONE);
        }
    }

    void close() {
        closed=true;hide();
        if(view!=null){parent.removeView(view);view=null;}
    }
}
