package dev.pgrazor;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.ColorMatrixColorFilter;
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
    // Palette conversion plus alpha gain: the real PokeMiners particles are intentionally
    // very faint on dark backgrounds, so a straight alpha copy looks invisible at 48dp.
    private final ColorMatrixColorFilter vortexTint = tint(248f, 45f, 230f, 2.7f);
    private final ColorMatrixColorFilter smokeTint = tint(151f, 80f, 255f, 2.15f);
    private final ColorMatrixColorFilter glowTint = tint(62f, 219f, 255f, 1.65f);

    private static ColorMatrixColorFilter tint(float red, float green, float blue, float alphaGain) {
        return new ColorMatrixColorFilter(new float[]{
            0f,0f,0f,0f,red,
            0f,0f,0f,0f,green,
            0f,0f,0f,0f,blue,
            0f,0f,0f,alphaGain,0f
        });
    }
    private final RectF sourceBounds = new RectF(-1f, -1f, 1f, 1f);
    private final RectF outerArc = new RectF(-.79f, -.79f, .79f, .79f);
    private final RectF innerArc = new RectF(-.52f, -.52f, .52f, .52f);
    private final RectF ballArc = new RectF(-.94f, -.94f, .94f, .94f);
    private final RectF ballInnerArc = new RectF(-.77f, -.77f, .77f, .77f);

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
     * Staggered clouds are born invisible, drift outward and dissipate before reset.
     * Each lane has a different lifetime and direction; there is no shared loop.
     * The destination stays in normalized view-space and in the existing clip.
     */
    private void driftingPlumes(Canvas canvas, Bitmap texture, ColorMatrixColorFilter tint,
                                long now, int speed, float energy,
                                float base, float reach, float opacity) {
        if (texture == null || energy <= 0f) return;
        art.setStyle(Paint.Style.FILL);
        art.setColor(Color.WHITE);
        art.setColorFilter(tint);
        for (int lane = 0; lane < 4; lane++) {
            float progress = EternatusFxTiming.flow(now, speed, lane);
            float alpha = EternatusFxTiming.flowOpacity(progress);
            if (alpha < .005f) continue;
            int saved = canvas.save();
            float heading = lane * 137.50776f
                + .31f * EternatusFxTiming.rotation(now, speed, (lane & 1) != 0);
            canvas.rotate(heading);
            canvas.translate(base + reach * progress, 0f);
            canvas.rotate((lane & 1) == 0 ? -32f - progress * 20f : 41f + progress * 24f);
            float size = (.36f + .21f * progress) * ((lane & 1) == 0 ? 1f : .83f);
            canvas.scale(size, size * (.73f + .25f * progress));
            art.setAlpha(Math.min(255, Math.round(opacity * energy * alpha)));
            canvas.drawBitmap(texture, null, sourceBounds, art);
            canvas.restoreToCount(saved);
        }
        art.setAlpha(255);
        art.setColorFilter(null);
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
        setRing(.41f, .98f);
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
        // The diffuse cloud expands toward the outer rim rather than returning
        // through the same frames. The vortex and solid thumb remain independent.
        driftingPlumes(canvas, smoke != null ? smoke : wispy, smokeTint,
            now, speed, energy, .49f, .30f, 170f);
        driftingPlumes(canvas, wispy != null ? wispy : smoke, glowTint,
            now + 2819L, speed, energy, .54f, .24f, 112f);

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
    void ballBehind(Canvas canvas, Bitmap vortex, Bitmap aura, float cx, float cy, float radius,
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
        if (vortex != null) {
            int vortexRotation = canvas.save();
            canvas.rotate(EternatusFxTiming.rotation(now, speed, false));
            art.setColor(Color.WHITE);
            art.setColorFilter(vortexTint);
            art.setAlpha(Math.min(255, Math.round(235f * energy)));
            canvas.drawBitmap(vortex, null, sourceBounds, art);
            art.setColorFilter(null);
            art.setAlpha(255);
            canvas.restoreToCount(vortexRotation);
        }
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
        // Independent smoke tongues travel along the transparent outer halo.
        driftingPlumes(canvas, aura != null ? aura : vortex, smokeTint,
            now + 1171L, speed, energy, .74f, .15f, 148f);
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
        // At 48dp the LT011 halo was under 1px and nearly invisible; two stronger
        // overlapping graded rings now occupy the border reserved around the ball.
        plasma.setStrokeWidth(.11f);
        plasma.setAlpha(Math.min(255, Math.round(245f * energy)));
        canvas.drawArc(ballArc, 0f, 350f, false, plasma);
        plasma.setStrokeWidth(.048f);
        plasma.setAlpha(Math.min(255, Math.round(235f * energy)));
        canvas.drawArc(ballInnerArc, 65f, 245f, false, plasma);
        canvas.restoreToCount(orbit);

        float flash = EternatusFxTiming.flash(now, intensity);
        if (flash > .001f) {
            plasma.setShader(ballHaze);
            plasma.setStyle(Paint.Style.FILL);
            plasma.setAlpha(Math.min(220, Math.round(250f * flash)));
            canvas.drawCircle(0f, 0f, .57f, plasma);
        }
        plasma.setAlpha(255);
        plasma.setShader(null);
        plasma.setStyle(Paint.Style.FILL);
        canvas.restoreToCount(saved);
    }
}
