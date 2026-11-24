package dev.pgrazor;

import java.util.Arrays;

/**
 * Pure, read-only parser and local fit for the optional native anchor snapshot (feature 72).
 * No Android, game, camera or location access. Invalid, stale or poorly conditioned samples are refused:
 * a RouteGroundProjection is returned only when six independent references agree within the existing
 * projection tolerance. This class never draws anything by itself.
 */
final class RouteAnchorSnapshot {
    static final int MAX = 128;
    private final double[][] refs; // lat, lon, normalized x, normalized y (y down)

    private RouteAnchorSnapshot(double[][] refs) { this.refs = refs; }

    int size() { return refs.length; }

    /** Strict parser for the native JSON schema; returns null for anything unexpected. */
    static RouteAnchorSnapshot parse(String json) {
        if (json == null || json.length() < 8 || json.length() > 64000) return null;
        Parser p = new Parser(json);
        try {
            p.objectStart();
            boolean countSeen = false, pointsSeen = false;
            int count = -1;
            double[][] refs = null;
            while (p.more()) {
                String key = p.string();
                p.colon();
                if ("count".equals(key)) {
                    if (countSeen) return null;
                    count = p.integer();
                    countSeen = true;
                } else if ("points".equals(key)) {
                    if (pointsSeen) return null;
                    pointsSeen = true;
                    refs = p.points();
                } else return null;
                if (p.commaOrObjectEnd()) break;
            }
            p.end();
            if (!countSeen || !pointsSeen || count < 0 || count > MAX ||
                    refs == null || refs.length != count) return null;
            return new RouteAnchorSnapshot(refs);
        } catch (RuntimeException invalid) {
            return null;
        }
    }

    /**
     * Fit from six of the newest visible anchors. Prefers the widest well-conditioned set; an unavailable,
     * degenerate or inconsistent set returns null (fail closed). The caller must still check freshness.
     */
    RouteGroundProjection fit(int width, int height) {
        if (width < 40 || height < 40) return null;
        int[] candidates = new int[MAX];
        int n = 0;
        for (int i = 0; i < refs.length && n < 12; i++) {
            double[] r = refs[i];
            if (!(Math.abs(r[0]) <= 75.0) || !(Math.abs(r[1]) <= 180.0)) continue;
            if (!Double.isFinite(r[2]) || !Double.isFinite(r[3]) || r[2] < 0 || r[2] > 1 ||
                    r[3] < 0 || r[3] > 1) continue;
            candidates[n++] = i;
        }
        if (n < 6) return null;
        final int[] pick = new int[6];
        RouteGroundProjection best = null;
        double bestSpan = -1;
        for (int a = 0; a < n - 5; a++) for (int b = a + 1; b < n - 4; b++)
            for (int c = b + 1; c < n - 3; c++) for (int d = c + 1; d < n - 2; d++)
                for (int e = d + 1; e < n - 1; e++) for (int f = e + 1; f < n; f++) {
                    pick[0] = candidates[a]; pick[1] = candidates[b]; pick[2] = candidates[c];
                    pick[3] = candidates[d]; pick[4] = candidates[e]; pick[5] = candidates[f];
                    double[][] six = new double[6][4];
                    for (int i = 0; i < 6; i++) {
                        double[] r = refs[pick[i]];
                        six[i] = new double[]{r[0], r[1], r[2] * width, r[3] * height};
                    }
                    try {
                        RouteGroundProjection projection = new RouteGroundProjection(six, width, height);
                        double span = spread(six);
                        if (span > bestSpan) { bestSpan = span; best = projection; }
                    } catch (IllegalArgumentException invalid) {
                        // One bad combination never weakens the others: residual checks stay strict.
                    }
                }
        return best;
    }

    private static double spread(double[][] refs) {
        double span = 0;
        for (int i = 0; i < refs.length; i++) for (int j = i + 1; j < refs.length; j++) {
            double dx = (refs[i][1] - refs[j][1]) * 111320.0 * Math.cos(Math.toRadians(refs[i][0]));
            double dy = (refs[i][0] - refs[j][0]) * 110540.0;
            span = Math.max(span, Math.hypot(dx, dy));
        }
        return span;
    }

    /** Minimal strict JSON reader covering exactly the bounded native snapshot schema. */
    private static final class Parser {
        private final String s;
        private int at;
        Parser(String value) { s = value; }
        private void ws() { while (at < s.length() && (s.charAt(at) == ' ' || s.charAt(at) == '\n' ||
                s.charAt(at) == '\r' || s.charAt(at) == '\t')) at++; }
        boolean more() { ws(); return at < s.length() && s.charAt(at) == '"'; }
        void objectStart() { ws(); expect('{'); }
        private void expect(char c) { ws(); if (at >= s.length() || s.charAt(at) != c) throw new IllegalArgumentException(); at++; }
        void colon() { expect(':'); }
        void end() { ws(); if (at != s.length()) throw new IllegalArgumentException(); }
        boolean commaOrObjectEnd() {
            ws();
            if (at < s.length() && s.charAt(at) == ',') { at++; return false; }
            expect('}');
            return true;
        }
        String string() {
            ws();
            if (at >= s.length() || s.charAt(at) != '"') throw new IllegalArgumentException();
            at++;
            StringBuilder out = new StringBuilder();
            while (at < s.length()) {
                char c = s.charAt(at++);
                if (c == '"') return out.toString();
                if (c == '\\') {
                    if (at >= s.length()) throw new IllegalArgumentException();
                    char e = s.charAt(at++);
                    if (e == 'u') {
                        if (at + 4 > s.length()) throw new IllegalArgumentException();
                        out.append((char) Integer.parseInt(s.substring(at, at + 4), 16));
                        at += 4;
                    } else if (e == '"' || e == '\\' || e == '/' || e == 'b' || e == 'f' || e == 'n' || e == 'r' || e == 't') {
                        out.append(e == 'b' ? '\b' : e == 'f' ? '\f' : e == 'n' ? '\n' : e == 'r' ? '\r' : e == 't' ? '\t' : e);
                    } else throw new IllegalArgumentException();
                } else if (c < 0x20) throw new IllegalArgumentException();
                else out.append(c);
            }
            throw new IllegalArgumentException();
        }
        int integer() {
            ws();
            int start = at;
            if (at < s.length() && s.charAt(at) == '-') at++;
            while (at < s.length() && s.charAt(at) >= '0' && s.charAt(at) <= '9') at++;
            if (start == at) throw new IllegalArgumentException();
            return Integer.parseInt(s.substring(start, at));
        }
        private double number() {
            ws();
            int start = at;
            if (at < s.length() && (s.charAt(at) == '-' || s.charAt(at) == '+')) at++;
            boolean digits = false;
            while (at < s.length() && s.charAt(at) >= '0' && s.charAt(at) <= '9') { at++; digits = true; }
            if (at < s.length() && s.charAt(at) == '.') {
                at++;
                while (at < s.length() && s.charAt(at) >= '0' && s.charAt(at) <= '9') { at++; digits = true; }
            }
            if (at < s.length() && (s.charAt(at) == 'e' || s.charAt(at) == 'E')) {
                at++;
                if (at < s.length() && (s.charAt(at) == '-' || s.charAt(at) == '+')) at++;
                while (at < s.length() && s.charAt(at) >= '0' && s.charAt(at) <= '9') at++;
            }
            if (!digits) throw new IllegalArgumentException();
            return Double.parseDouble(s.substring(start, at));
        }
        private boolean bool() {
            ws();
            if (s.startsWith("true", at)) { at += 4; return true; }
            if (s.startsWith("false", at)) { at += 5; return false; }
            throw new IllegalArgumentException();
        }
        double[][] points() {
            expect('[');
            java.util.ArrayList<double[]> out = new java.util.ArrayList<double[]>();
            ws();
            if (at < s.length() && s.charAt(at) == ']') { at++; return new double[0][]; }
            while (at < s.length()) {
                expect('{');
                String id = null, kind = null;
                double lat = Double.NaN, lon = Double.NaN, x = Double.NaN, y = Double.NaN;
                boolean visible = false;
                int fields = 0;
                while (true) {
                    String key = string();
                    colon();
                    if ("id".equals(key)) id = string();
                    else if ("kind".equals(key)) kind = string();
                    else if ("lat".equals(key)) lat = number();
                    else if ("lon".equals(key)) lon = number();
                    else if ("x".equals(key)) x = number();
                    else if ("y".equals(key)) y = number();
                    else if ("visible".equals(key)) visible = bool();
                    else throw new IllegalArgumentException();
                    fields++;
                    if (!commaOrObjectEnd()) break;
                }
                if (id == null || id.length() == 0 || id.length() > 64 || kind == null ||
                        !( "stop".equals(kind) || "gym".equals(kind)) || fields != 7 ||
                        !Double.isFinite(lat) || Math.abs(lat) > 90 || !Double.isFinite(lon) ||
                        Math.abs(lon) > 180 || !Double.isFinite(x) || !Double.isFinite(y)) return null;
                if (visible && Math.abs(x) <= 1 && Math.abs(y) <= 1) out.add(new double[]{lat, lon, x, y});
                ws();
                if (at < s.length() && s.charAt(at) == ',') { at++; continue; }
                expect(']');
                return out.toArray(new double[out.size()][]);
            }
            throw new IllegalArgumentException();
        }
    }

    /** Used by tests and diagnostics only; never exposed to the drawing layer. */
    double[] reference(int i) { return Arrays.copyOf(refs[i], refs[i].length); }
}
