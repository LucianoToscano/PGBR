package dev.pgrazor;

/**
 * An immutable, validated walking route: an ordered list of [latitude, longitude] points.
 *
 * <p>This type is plain Java with no Android dependencies so it can be exercised off-device.
 * The constructor validates defensively and copies its input, and {@link #points()} hands back a
 * deep copy, so a {@code GpxRoute} never aliases a caller's array and cannot be mutated after
 * construction. Point order is preserved exactly as supplied.
 */
public final class GpxRoute {
    /** Longest accepted route name, in characters, after trimming. */
    public static final int NAME_LIMIT = 80;

    /** Display name for the route; never null, trimmed, non-empty, free of control characters. */
    public final String name;

    // Each row is a fresh {lat, lon} pair owned solely by this instance.
    private final double[][] points;

    public GpxRoute(String name, double[][] points) {
        if (name == null) throw new IllegalArgumentException("A rota precisa de um nome.");
        name = name.trim();
        if (!hasVisibleName(name)) throw new IllegalArgumentException("Informe um nome para a rota.");
        if (name.length() > NAME_LIMIT) throw new IllegalArgumentException("O nome da rota pode conter no máximo " + NAME_LIMIT + " caracteres.");
        for (int i = 0; i < name.length();) {
            int cp = name.codePointAt(i);
            if (!validNameCodePoint(cp)) throw new IllegalArgumentException("Use um nome sem caracteres de controle ou inválidos para XML.");
            i += Character.charCount(cp);
        }
        if (points == null) throw new IllegalArgumentException("A rota precisa de pontos.");
        if (points.length == 0) throw new IllegalArgumentException("A rota precisa de pelo menos um ponto.");
        if (points.length > GpxCodec.MAX_POINTS) throw new IllegalArgumentException("Uma rota pode conter no máximo " + GpxCodec.MAX_POINTS + " pontos.");
        double[][] copy = new double[points.length][];
        for (int i = 0; i < points.length; i++) {
            double[] point = points[i];
            if (point == null || point.length != 2) throw new IllegalArgumentException("Cada ponto deve conter um par [lat, lon].");
            double lat = point[0], lon = point[1];
            if (!Double.isFinite(lat) || !Double.isFinite(lon) || Math.abs(lat) > 90 || Math.abs(lon) > 180)
                throw new IllegalArgumentException("O ponto " + (i + 1) + " está fora do intervalo válido de coordenadas.");
            copy[i] = new double[]{lat, lon};
        }
        this.name = name;
        this.points = copy;
    }

    static boolean hasVisibleName(String name) {
        return name != null && name.codePoints().anyMatch(cp -> !Character.isWhitespace(cp) && !Character.isSpaceChar(cp));
    }

    /** XML 1.0-safe route name characters, excluding all ISO controls. */
    static boolean validNameCodePoint(int cp) {
        return !Character.isISOControl(cp) &&
            ((cp >= 0x20 && cp <= 0xD7FF) ||
             (cp >= 0xE000 && cp <= 0xFFFD) ||
             (cp >= 0x10000 && cp <= 0x10FFFF));
    }

    /** Number of points in the route. */
    public int size() { return points.length; }

    /** Read one coordinate without copying thousands of unrelated GPX points on every tick. */
    public double[] point(int index) {
        if(index<0||index>=points.length)throw new IndexOutOfBoundsException("Route position "+(index+1));
        return new double[]{points[index][0],points[index][1]};
    }

    /** A deep copy of the ordered [lat, lon] points; mutating the result never affects this route. */
    public double[][] points() {
        double[][] copy = new double[points.length][];
        for (int i = 0; i < points.length; i++) copy[i] = new double[]{points[i][0], points[i][1]};
        return copy;
    }
}
