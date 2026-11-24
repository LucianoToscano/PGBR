package dev.pgrazor;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

/**
 * Makes an in-activity overlay draggable without losing its tap action.
 *
 * <p>The handle view is also the dragged target. A short movement threshold keeps a
 * tap from registering as a drag, so the handle's existing click listener still fires.
 * The resting place is stored as a fraction of the free track (parent size minus view
 * size) in an app-private {@code SharedPreferences}; fractions survive restarts and
 * keep the overlay on-screen after a rotation or any other relayout. An optional
 * follower (the VGP status pill) rides directly below the handle, clamped to the parent
 * and flipped above when there is no room, so the button and its pill stay coherent.
 */
public final class OverlayDrag implements View.OnTouchListener {
    private static final String PREFS = "pgrazor-overlays";
    private final ViewGroup parent;
    private final View view;
    private final View follower;
    private final SharedPreferences prefs;
    private final String key;
    private final int threshold, gap;
    private final View.OnLayoutChangeListener parentLayout, viewLayout, followerLayout;
    private float downX, downY, originX, originY;
    private boolean moved, dragging, resetPending;

    static OverlayDrag attach(ViewGroup parent, View view, String key) { return new OverlayDrag(parent, view, null, key); }
    static OverlayDrag attach(ViewGroup parent, View view, View follower, String key) { return new OverlayDrag(parent, view, follower, key); }

    private OverlayDrag(ViewGroup parent, View view, View follower, String key) {
        this.parent = parent; this.view = view; this.follower = follower; this.key = key;
        prefs = AccountProfiles.preferences(view.getContext().getApplicationContext(),PREFS);
        float density = view.getResources().getDisplayMetrics().density;
        threshold = android.view.ViewConfiguration.get(view.getContext()).getScaledTouchSlop(); gap = Math.round(6 * density);
        view.setOnTouchListener(this);
        view.setEnabled(true);
        view.setElevation(8 * density);
        view.post(() -> { int[] xy = new int[2]; view.getLocationOnScreen(xy); Control.event("overlay-layout", key + " x=" + xy[0] + " y=" + xy[1] + " w=" + view.getWidth() + " h=" + view.getHeight() + " enabled=" + view.isEnabled()); });
        // Restore when the handle is (re)laid out: first show, a visibility flip, or a rotation.
        viewLayout = (v, l, t, r, b, ol, ot, or, ob) -> { if(resetPending)applyDefault();else applySaved(); };
        view.addOnLayoutChangeListener(viewLayout);
        // A rotation or window resize relays out the parent; re-apply to the new bounds.
        parentLayout = (v, l, t, r, b, ol, ot, or, ob) -> { if (r - l != or - ol || b - t != ob - ot) applySaved(); };
        parent.addOnLayoutChangeListener(parentLayout);
        if (follower != null) { followerLayout = (v, l, t, r, b, ol, ot, or, ob) -> positionFollower(); follower.addOnLayoutChangeListener(followerLayout); }
        else followerLayout = null;
    }

    public boolean onTouch(View v, MotionEvent e) {
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                Control.event("overlay-touch", key + " down " + e.getRawX() + "," + e.getRawY());
                downX = e.getRawX(); downY = e.getRawY(); originX = view.getX(); originY = view.getY(); moved = false; dragging = true; view.setPressed(true); return true;
            case MotionEvent.ACTION_POINTER_DOWN:
                moved = true; view.setPressed(false); return true;
            case MotionEvent.ACTION_MOVE:
                float dx = e.getRawX() - downX, dy = e.getRawY() - downY;
                if (!moved && Math.hypot(dx, dy) > threshold) moved = true;
                if (moved) { view.setPressed(false); view.setX(clampX(originX + dx)); view.setY(clampY(originY + dy)); positionFollower(); }
                return true;
            case MotionEvent.ACTION_UP:
                Control.event("overlay-touch", key + " up moved=" + moved + " x=" + view.getX() + " y=" + view.getY());
                dragging = false; view.setPressed(false);
                if (moved) save(); else v.performClick(); // tap below the threshold still opens history / toggles VGP
                return true;
            case MotionEvent.ACTION_CANCEL:
                dragging = false; view.setPressed(false); if (moved) save(); return true;
        }
        return true;
    }

    /** Pull the saved fraction back onto the current bounds; a no-op until the user first drags. */
    private void applySaved() {
        if (resetPending){applyDefault();return;}
        if (dragging || !prefs.contains(key + ".fx")) return;
        int w = visualWidth(view), h = visualHeight(view);
        if (w == 0 || h == 0 || parent.getWidth() == 0 || parent.getHeight() == 0) return;
        android.graphics.Rect safe=dragBounds();
        view.setX(clampX(safe.left+Math.max(0,safe.width()-w)*savedFraction(key+".fx")));
        view.setY(clampY(safe.top+Math.max(0,safe.height()-h)*savedFraction(key+".fy")));
        positionFollower();
    }

    private float savedFraction(String name){
        try{return clampUnit(prefs.getFloat(name,0f));}catch(ClassCastException invalid){return 0f;}
    }
    /** Reset only this overlay's position, without changing its feature or other preferences. */
    void resetPosition(){
        dragging=false;moved=false;view.setPressed(false);
        prefs.edit().remove(key+".fx").remove(key+".fy").apply();
        resetPending=true;view.setTranslationX(0);view.setTranslationY(0);
        applyDefault();
    }
    private void applyDefault(){
        if(view.getVisibility()!=View.VISIBLE||view.getWidth()==0||view.getHeight()==0||parent.getWidth()==0||parent.getHeight()==0)return;
        resetPending=false;view.setX(clampX(view.getLeft()));view.setY(clampY(view.getTop()));positionFollower();
    }
    private void save() {
        android.graphics.Rect safe=dragBounds();
        int trackW=safe.width()-visualWidth(view),trackH=safe.height()-visualHeight(view);
        float fx=trackW>0?clampUnit((view.getX()-safe.left)/trackW):0f;
        float fy=trackH>0?clampUnit((view.getY()-safe.top)/trackH):0f;
        prefs.edit().putFloat(key + ".fx", fx).putFloat(key + ".fy", fy).apply();
    }

    /** Keep the status pill directly below the handle, clamped on-screen, flipped above when there is no room. */
    private void positionFollower() {
        if (follower == null) return;
        int fw = visualWidth(follower), fh = visualHeight(follower);
        if (fw == 0 || fh == 0) return; // hidden or not yet measured; its own layout listener will catch up
        float x = view.getX(), y = view.getY() + visualHeight(view) + gap * view.getScaleY();
        android.graphics.Rect safe=UiViewport.bounds(parent);
        if(y+fh>safe.bottom)y=view.getY()-gap-fh;
        follower.setX(UiViewportPolicy.clamp(x,safe.left,safe.right,fw));
        follower.setY(UiViewportPolicy.clamp(y,safe.top,safe.bottom,fh));
    }

    /**
     * The VGP may rest flush against the LEFT and RIGHT screen edges on portrait
     * game layouts. Vertical limits still avoid system bars/gesture navigation,
     * so all four corners of the usable gameplay area stay accessible.
     *
     * The exact same rectangle must be used for dragging, saving, restoring and
     * rendering — otherwise a saved right-edge position can jump on relayout.
     */
    private android.graphics.Rect dragBounds(){
        return "vgp".equals(key)?UiViewport.vgpBounds(parent):UiViewport.bounds(parent);
    }
    private float clampX(float x){
        android.graphics.Rect safe=dragBounds();
        return UiViewportPolicy.clamp(x,safe.left,safe.right,visualWidth(view));
    }
    private float clampY(float y){
        android.graphics.Rect safe=dragBounds();
        return UiViewportPolicy.clamp(y,safe.top,safe.bottom,visualHeight(view));
    }
    static int visualWidth(View view){return Math.round(view.getWidth()*view.getScaleX());}
    static int visualHeight(View view){return Math.round(view.getHeight()*view.getScaleY());}
    void refreshBounds(){applySaved();view.setX(clampX(view.getX()));view.setY(clampY(view.getY()));positionFollower();}
    private static float clampUnit(float f) { return Float.isFinite(f) ? Math.max(0f, Math.min(1f, f)) : 0f; }

    /** Drop the hierarchy listeners when the pad is torn down so the decor view keeps no stale references. */
    void detach() {
        parent.removeOnLayoutChangeListener(parentLayout);
        view.removeOnLayoutChangeListener(viewLayout);
        view.setOnTouchListener(null);
        if (follower != null) follower.removeOnLayoutChangeListener(followerLayout);
    }
}
