package dev.pgrazor;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;

/** Layered lightning in the transparent space around the themed controls. */
final class EternatusStorm {
    private final Paint light = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path fork = new Path();

    EternatusStorm() {
        light.setStyle(Paint.Style.STROKE);
        light.setStrokeJoin(Paint.Join.ROUND);
        light.setStrokeCap(Paint.Cap.ROUND);
    }

    void draw(Canvas canvas, float cx, float cy, float radius, long now, boolean joystick) {
        final int count = joystick ? 8 : 5;
        int saved = canvas.save();
        canvas.translate(cx, cy);
        canvas.scale(radius, radius);
        for (int i = 0; i < count; i++) {
            long age = now + 431L * i;
            long cycle = age / 1700L;
            float phase = age % 1700L / 1700f;
            float glow = (float) Math.sin(Math.PI * phase);
            int rotated = canvas.save();
            canvas.rotate((i * 360f / count + (cycle % 31) * 23f) % 360f);
            boolean inward = joystick && i % 2 == 0;
            float start = inward ? .42f : .78f;
            float reach = inward ? .40f : .20f;
            float side = (i % 3 == 0 ? -1 : 1) * (inward ? .135f : .17f);
            fork.rewind();
            fork.moveTo(start, 0);
            fork.lineTo(start + reach * .32f, side * .52f);
            fork.lineTo(start + reach * .43f, side * .28f);
            fork.lineTo(start + reach * .68f, side * .77f);
            fork.lineTo(start + reach, side * .58f);
            fork.moveTo(start + reach * .43f, side * .28f);
            fork.lineTo(start + reach * .62f, -side * .16f);
            fork.lineTo(start + reach * .77f, -side * .09f);
            int red = i % 2 == 0 ? 45 : 244;
            int green = i % 2 == 0 ? 215 : 53;
            light.setColor(Color.argb(Math.round(64 * glow), red, green, 255));
            light.setStrokeWidth(.048f);
            canvas.drawPath(fork, light);
            light.setColor(Color.argb(Math.round(215 * glow), red, green, 255));
            light.setStrokeWidth(.022f);
            canvas.drawPath(fork, light);
            light.setColor(Color.argb(Math.round(235 * glow), 242, 246, 255));
            light.setStrokeWidth(.008f);
            canvas.drawPath(fork, light);
            canvas.restoreToCount(rotated);
        }
        canvas.restoreToCount(saved);
    }
}
