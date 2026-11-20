package dev.pgrazor;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.SweepGradient;

/**
 * Eternatus atmospheric renderer. Separate image layers create visible rotation;
 * crystalline rim, Poké Ball silhouette and live joystick thumb stay upright.
 *
 * No additional worker, thread, generated zigzag lightning or input interception.
 * All paths, gradients and paints are allocated once for each view.
 */
final class EternatusVisualEffects {
    private final Paint art = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Paint plasma = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path clip = new Path();
    private final PorterDuffColorFilter vortexTint =
        new PorterDuffColorFilter(0xFFF82DE6, PorterDuff.Mode.SRC_IN);
    private final PorterDuffColorFilter smokeTint =
        new PorterDuffColorFilter(0xFF8F5AFF, PorterDuff.Mode.SRC_IN);
    private final PorterDuffColorFilter glowTint =
        new PorterDuffColorFilter(0xFF4CE3FF, PorterDuff.Mode.SRC_IN);
    private final RectF sourceBounds = new RectF(-1f, -1f, 1f, 1f);
    private final RectF outerArc = new RectF(-.79f, -.79f, .79f, .79f);
    private final RectF innerArc = new RectF(-.52f, -.52f, .52f, .52f);
    private final RectF ballArc = new RectF(-.94f, -.94f, .94f, .94f);

    private final RadialGradient vortexHaze = new RadialGradient(0f, 0f, .85f,
        new int[]{Color.TRANSPARENT, 0x0C29CFFF, 0x5C7827E5, 0x66F91AC8, Color.TRANSPARENT},
        new float[]{0f, .35f, .53f, .76f, 1f}, Shader.TileMode.CLAMP);
    private final RadialGradient ballHaze = new RadialGradient(0f, 0f, .98f,
        new int[]{0x69D92AF7, 0x3B6F1DFB, 0x60E634E8, 0x36AA4BFF, Color.TRANSPARENT},
        new float[]{0f, .3f, .62f, .8f, 1f}, Shader.TileMode.CLAMP);
    private final SweepGradient stormArcs = new SweepGradient(0f, 0f,
        new int[]{Color.TRANSPARENT, 0x623FCAFF, 0xFFEC36E0, 0x5D6C41F5,
                  Color.TRANSPARENT, 0xB5EB26DF, 0xF568D9FF, Color.TRANSPARENT},
        new float[]{0f, .11f, .24f, .38f, .47f, .60f, .79f, 1f});

    private void setRing(float inner, float outer) {
        clip.rewind();
        clip.setFillType(Path.FillType.EVEN_ODD);
        clip.addCircle(0f, 0f, outer, Path.Direction.CW);
        clip.addCircle(0f, 0f, inner, Path.Direction.CW);
    }

    /**
     * Concentric counter-rotating art between the crystal ring and movable center.
     * Alpha goes to exactly zero at both clean ends of each morph cycle.
     */
    void joystick(Canvas canvas, Bitmap vortex, Bitmap smoke, Bitmap wispy, float cx, float cy,
                  float radius, long now, int intensity, int speed) {
        float energy = EternatusFxTiming.strength(now, intensity);
        if (energy < .001f || radius <= 0f) return;

        int saved = canvas.save();
        canvas.translate(cx, cy);
        canvas.scale(radius, radius);
        setRing(.41f, .82f);
        canvas.clipPath(clip);

        plasma.setStyle(Paint.Style.FILL);
        plasma.setShader(vortexHaze);
        plasma.setAlpha(Math.min(255, Math.round(208f * energy)));
        canvas.drawCircle(0f, 0f, .85f, plasma);
        plasma.setShader(null);

        // An image with a real vortex pattern rotates CW; its smoky layer rotates CCW.
        // Rendering both inside the clipped annulus creates continuous storm motion,
        // unlike LT010's almost invisible 42-alpha drift.
        art.setColor(Color.WHITE);
        art.setStyle(Paint.Style.FILL);
        if (vortex != null) {
            int clockwise = canvas.save();
            canvas.rotate(EternatusFxTiming.rotation(now, speed, false));
            art.setColorFilter(vortexTint);
            art.setAlpha(Math.min(255, Math.round(240f * energy)));
            canvas.drawBitmap(vortex, null, sourceBounds, art);
            art.setColorFilter(null);
            canvas.restoreToCount(clockwise);
        }
        if (smoke != null) {
            int counter = canvas.save();
            canvas.rotate(EternatusFxTiming.rotation(now, speed, true));
            art.setColorFilter(smokeTint);
            art.setAlpha(Math.min(255, Math.round(195f * energy)));
            canvas.drawBitmap(smoke, null, sourceBounds, art);
            art.setColorFilter(null);
            canvas.restoreToCount(counter);
        }
        if (wispy != null) {
            int wisps = canvas.save();
            canvas.rotate(EternatusFxTiming.rotation(now, speed, false) * .64f);
            art.setColorFilter(glowTint);
            art.setAlpha(Math.min(180, Math.round(115f * energy)));
            canvas.drawBitmap(wispy, null, sourceBounds, art);
            art.setColorFilter(null);
            canvas.restoreToCount(wisps);
        }
        art.setAlpha(255);

        // Curved plasma / illumination, no fake forked lightning paths.
        int glowRotation = canvas.save();
        canvas.rotate(EternatusFxTiming.rotation(now, speed, false));
        plasma.setStyle(Paint.Style.STROKE);
        plasma.setStrokeCap(Paint.Cap.ROUND);
        plasma.setShader(stormArcs);
        plasma.setStrokeWidth(.055f);
        plasma.setAlpha(Math.min(255, Math.round(237f * energy)));
        canvas.drawArc(outerArc, 0f, 310f, false, plasma);
        plasma.setStrokeWidth(.026f);
        plasma.setAlpha(Math.min(255, Math.round(185f * energy)));
        canvas.drawArc(innerArc, 40f, 280f, false, plasma);
        canvas.restoreToCount(glowRotation);
        plasma.setShader(null);
        plasma.setStyle(Paint.Style.FILL);

        // Diffuse storm illumination peaks briefly before fading with the vortex.
        float flash = EternatusFxTiming.flash(now, intensity);
        if (flash > 0f) {
            plasma.setShader(vortexHaze);
            plasma.setAlpha(Math.round(195f * flash));
            canvas.drawCircle(0f, 0f, .84f, plasma);
            plasma.setShader(null);
        }
        canvas.restoreToCount(saved);
    }

    /** Rotating charged atmosphere behind the Poké Ball, without rotating its face. */
    void ballBehind(Canvas canvas, Bitmap aura, float cx, float cy, float radius,
                    long now, int intensity, int speed) {
        float energy = EternatusFxTiming.strength(now, intensity);
        if (energy < .001f || radius <= 0f) return;
        int saved = canvas.save();
        canvas.translate(cx, cy);
        canvas.scale(radius, radius);
        setRing(.67f, .99f);
        canvas.clipPath(clip);

        plasma.setStyle(Paint.Style.FILL);
        plasma.setShader(ballHaze);
        plasma.setAlpha(Math.min(255, Math.round(242f * energy)));
        canvas.drawCircle(0f, 0f, .97f, plasma);
        plasma.setShader(null);
        if (aura != null) {
            int rotation = canvas.save();
            canvas.rotate(EternatusFxTiming.rotation(now, speed, true));
            art.setColor(Color.WHITE);
            art.setColorFilter(smokeTint);
            art.setAlpha(Math.min(255, Math.round(245f * energy)));
            canvas.drawBitmap(aura, null, sourceBounds, art);
            art.setColorFilter(null);
            art.setAlpha(255);
            canvas.restoreToCount(rotation);
        }
        canvas.restoreToCount(saved);
    }

    /** A pulsing, rotating corona + very brief diffuse illumination on the Ball itself. */
    void ballFront(Canvas canvas, float cx, float cy, float radius,
                   long now, int intensity, int speed) {
        float energy = EternatusFxTiming.strength(now, intensity);
        if (energy < .001f || radius <= 0f) return;
        int saved = canvas.save();
        canvas.translate(cx, cy);
        canvas.scale(radius, radius);

        int orbit = canvas.save();
        canvas.rotate(EternatusFxTiming.rotation(now, speed, false));
        plasma.setShader(stormArcs);
        plasma.setStyle(Paint.Style.STROKE);
        plasma.setStrokeCap(Paint.Cap.ROUND);
        plasma.setStrokeWidth(.046f);
        plasma.setAlpha(Math.min(255, Math.round(215f * energy)));
        canvas.drawArc(ballArc, 0f, 350f, false, plasma);
        canvas.restoreToCount(orbit);

        float flash = EternatusFxTiming.flash(now, intensity);
        if (flash > .001f) {
            plasma.setShader(ballHaze);
            plasma.setStyle(Paint.Style.FILL);
            plasma.setAlpha(Math.min(125, Math.round(190f * flash)));
            canvas.drawCircle(0f, 0f, .47f, plasma);
        }
        plasma.setAlpha(255);
        plasma.setShader(null);
        plasma.setStyle(Paint.Style.FILL);
        canvas.restoreToCount(saved);
    }
}
