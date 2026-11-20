package dev.pgrazor;

/** Computes a centered fit-center rectangle without stretching the source sprite. */
final class ItemSpriteFit {
    private ItemSpriteFit() {}

    /** {left, top, right, bottom}, in canvas coordinates centered at zero. */
    static float[] centeredRect(int width, int height, float available) {
        if (width <= 0 || height <= 0 || available <= 0 || !Float.isFinite(available))
            throw new IllegalArgumentException("Invalid item sprite dimensions");
        float scale = Math.min(available / width, available / height);
        float halfWidth = width * scale / 2f;
        float halfHeight = height * scale / 2f;
        return new float[]{-halfWidth, -halfHeight, halfWidth, halfHeight};
    }
}
