package dev.pgrazor;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;

/** Optional drawing layer for a PGBR-owned surface; no input or movement ownership. */
final class RouteTraceView extends View {
    private final Path remaining = new Path(), completed = new Path();
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private RouteTraceGeometry geometry;
    private RouteTraceStyle style=RouteTraceStyle.defaults();
    private final float density;

    RouteTraceView(Context context) {
        super(context);
        density = getResources().getDisplayMetrics().density;
        setClickable(false);
        setFocusable(false);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
    }

    /** UI-thread only. Caller supplies verified surface-local pixel positions and progress. */
    void setTrace(RouteTraceGeometry next, double meters) {
        clearTrace();
        if (next == null) return;
        // Reject an unsuitable projection rather than presenting a misleading line.
        for (int i = 0; i < next.size(); i++) {
            if (Math.abs(next.x(i)) > 1e7 || Math.abs(next.y(i)) > 1e7) return;
        }
        try {
            next.drawSegments(meters, (x1,y1,x2,y2,done) -> {
                Path path = done ? completed : remaining;
                path.moveTo((float)x1,(float)y1);
                path.lineTo((float)x2,(float)y2);
            });
            geometry = next;
            invalidate();
        } catch (IllegalArgumentException invalid) {
            clearTrace();
        }
    }

    void clearTrace() {
        geometry = null;
        remaining.reset();
        completed.reset();
        invalidate();
    }

    void setStyle(RouteTraceStyle value) {
        if(value!=null&&style!=value){style=value;invalidate();}
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (geometry == null) return;
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth((style.width+2)*density);
        paint.setColor(style.color(0xff303945));
        canvas.drawPath(remaining,paint);
        canvas.drawPath(completed,paint);
        paint.setStrokeWidth(style.width*density);
        paint.setColor(style.color(style.remaining));
        canvas.drawPath(remaining,paint);
        paint.setColor(style.color(style.showProgress?style.completed:style.remaining));
        canvas.drawPath(completed,paint);
        if(style.showMarkers){if(geometry.startMarker)marker(canvas,0,0xff168fea);if(geometry.endMarker)marker(canvas,geometry.size()-1,0xffe2574f);}
    }

    private void marker(Canvas canvas,int index,int color) {
        float x=(float)geometry.x(index), y=(float)geometry.y(index);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xffffffff);
        canvas.drawCircle(x,y,6*density,paint);
        paint.setColor(color);
        canvas.drawCircle(x,y,4*density,paint);
    }

    @Override public boolean onTouchEvent(android.view.MotionEvent event){return false;}

    @Override protected void onDetachedFromWindow() {
        clearTrace();
        super.onDetachedFromWindow();
    }
}
