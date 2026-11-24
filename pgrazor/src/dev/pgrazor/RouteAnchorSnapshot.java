package dev.pgrazor;

import java.util.Arrays;
import java.util.HashSet;

/**
 * Pure, read-only parser and local fit for the optional native anchor snapshot (feature 72).
 * No Android, game, camera or location access. Invalid, stale or poorly conditioned samples are refused:
 * a RouteGroundProjection is returned only when six independent references agree within the existing
 * projection tolerance. This class never draws anything by itself.
 *
 * The JSON declares the TOTAL number of scanned records; only the visible subset is retained for fitting.
 * Records outside the viewport are still parsed and validated, never silently discarded as malformed.
 */
final class RouteAnchorSnapshot {
    static final int MAX = 128;
    private static final double MAX_JSON_BYTES = 64000;
    private final double[][] refs; // lat, lon, normalized x, normalized y (y down)
    private final int total;       // records received, visible or not
    private final long capturedAtMs; // native monotonic clock at scan time

    private RouteAnchorSnapshot(double[][] refs, int total, long capturedAtMs) {
        this.refs = refs;
        this.total = total;
        this.capturedAtMs = capturedAtMs;
    }

    /** Visible references kept for the local fit. */
    int size() { return refs.length; }

    /** Total records reported by the native producer, including offscreen ones. */
    int total() { return total; }

    /** Native monotonic capture time; freshness against the Java clock is checked by the consumer. */
    long capturedAtMs() { return capturedAtMs; }

    /** Strict parser for the native JSON schema; returns null for anything unexpected. */
    static RouteAnchorSnapshot parse(String json) {
        if (json == null || json.length() < 8 || json.length() > MAX_JSON_BYTES) return null;
        Parser p = new Parser(json);
        try {
            p.objectStart();
            boolean countSeen = false, pointsSeen = false, timeSeen = false;
            int count = -1;
            long capturedAt = -1;
            int[] parsed = new int[1];
            double[][] refs = null;
            while (p.more()) {
                String key = p.string();
                p.colon();
                if ("count".equals(key)) {
                    if (countSeen) return null;
                    count = p.integer();
                    countSeen = true;
                } else if ("at".equals(key)) {
                    if (timeSeen) return null;
                    capturedAt = p.whole();
                    timeSeen = true;
                } else if ("points".equals(key)) {
                    if (pointsSeen) return null;
                    pointsSeen = true;
                    refs = p.points(parsed);
                } else return null;
                if (p.commaOrObjectEnd()) break;
            }
            p.end();
            if (!countSeen || !pointsSeen || !timeSeen || count < 0 || count > MAX ||
                    capturedAt <= 0 || capturedAt > 1L << 50 ||
                    refs == null || parsed[0] != count) return null;
            return new RouteAnchorSnapshot(refs, parsed[0], capturedAt);
        } catch (RuntimeException invalid) {
            return null;
        }
    }

    /**
     * Fit from six of the newest visible anchors. Prefers the widest well-conditioned set; an unavailable,
     * degenerate or inconsistent set returns null (fail closed). When more than six references are
     * available, every candidate must still agree with at least one reference outside the selected set.
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
        double bestScore = -1;
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
                    double span = spread(six);
                    if (span < 10) continue; // reject near-collinear or clustered control sets
                    try {
                        RouteGroundProjection projection = new RouteGroundProjection(six, width, height);
                        int holdouts = 0, inliers = 0;
                        for (int i = 0; i < n; i++) {
                            if (selected(pick, candidates[i])) continue;
                            holdouts++;
                            double[] r = refs[candidates[i]];
                            try {
                                double[] s = projection.project(r[0], r[1]);
                                if (Math.hypot(s[0] - r[2] * width, s[1] - r[3] * height) <= 2.0) inliers++;
                            } catch (IllegalArgumentException outsideHull) {
                                // An unused anchor outside the calibrated hull is not evidence against the fit.
                            }
                        }
                        if (holdouts > 0 && inliers == 0) continue; // no independent agreement
                        double score = inliers * 10000.0 + span;
                        if (score > bestScore) { bestScore = score; best = projection; }
                    } catch (IllegalArgumentException invalid) {
                        // One bad combination never weakens the others: residual checks stay strict.
                    }
                }
        return best;
    }

    private static boolean selected(int[] pick, int index) {
        for (int i = 0; i < pick.length; i++) if (pick[i] == index) return true;
        return false;
    }

    /** Maximum pair distance in metres, wrapping longitude so antimeridian anchors stay local. */
    private static double spread(double[][] refs) {
        double span = 0;
        for (int i = 0; i < refs.length; i++) for (int j = i + 1; j < refs.length; j++) {
            double dl = refs[i][1] - refs[j][1];
            if (dl > 180) dl -= 360; else if (dl < -180) dl += 360;
            double dx = dl * 111320.0 * Math.cos(Math.toRadians((refs[i][0] + refs[j][0]) / 2));
            double dy = (refs[i][0] - refs[j][0]) * 110540.0;
            span = Math.max(span, Math.hypot(dx, dy));
        }
        return span <= 1000 ? span : -1; // RouteGroundProjection refuses larger local planes
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
            long value = whole();
            if (value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) throw new IllegalArgumentException();
            return (int) value;
        }
        long whole() {
            ws();
            int start = at;
            if (at < s.length() && s.charAt(at) == '-') at++;
            while (at < s.length() && s.charAt(at) >= '0' && s.charAt(at) <= '9') at++;
            if (start == at) throw new IllegalArgumentException();
            return Long.parseLong(s.substring(start, at));
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
        double[][] points(int[] totalOut) {
            expect('[');
            java.util.ArrayList<double[]> out = new java.util.ArrayList<double[]>();
            HashSet<String> ids = new HashSet<String>();
            int total = 0;
            ws();
            if (at < s.length() && s.charAt(at) == ']') { at++; totalOut[0] = 0; return new double[0][]; }
            while (at < s.length()) {
                expect('{');
                String id = null, kind = null;
                double lat = Double.NaN, lon = Double.NaN, x = Double.NaN, y = Double.NaN, z = Double.NaN;
                boolean visible = false;
                int mask = 0;
                while (true) {
                    String key = string();
                    colon();
                    int bit;
                    if ("id".equals(key)) { bit = 1; id = string(); }
                    else if ("kind".equals(key)) { bit = 2; kind = string(); }
                    else if ("lat".equals(key)) { bit = 4; lat = number(); }
                    else if ("lon".equals(key)) { bit = 8; lon = number(); }
                    else if ("x".equals(key)) { bit = 16; x = number(); }
                    else if ("y".equals(key)) { bit = 32; y = number(); }
                    else if ("z".equals(key)) { bit = 64; z = number(); }
                    else if ("visible".equals(key)) { bit = 128; visible = bool(); }
                    else throw new IllegalArgumentException();
                    if ((mask & bit) != 0) return null; // repeated field
                    mask |= bit;
                    if (commaOrObjectEnd()) break;
                }
                if (mask != 255 || id == null || id.length() == 0 || id.length() > 64 || kind == null ||
                        !( "stop".equals(kind) || "gym".equals(kind)) ||
                        !Double.isFinite(lat) || Math.abs(lat) > 90 ||
                        !Double.isFinite(lon) || Math.abs(lon) > 180 ||
                        !Double.isFinite(x) || Math.abs(x) > 64 ||
                        !Double.isFinite(y) || Math.abs(y) > 64 ||
                        !Double.isFinite(z) || z <= 0 || z > 1e8) return null;
                if (!ids.add(id)) return null; // duplicate record
                if (++total > MAX) return null;
                if (visible) {
                    if (x < 0 || x > 1 || y < 0 || y > 1) return null;
                    out.add(new double[]{lat, lon, x, y});
                }
                ws();
                if (at < s.length() && s.charAt(at) == ',') { at++; continue; }
                expect(']');
                totalOut[0] = total;
                return out.toArray(new double[out.size()][]);
            }
            throw new IllegalArgumentException();
        }
    }

    /** Used by tests and diagnostics only; never exposed to the drawing layer. */
    double[] reference(int i) { return Arrays.copyOf(refs[i], refs[i].length); }
}
