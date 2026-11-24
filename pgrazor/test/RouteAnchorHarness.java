package dev.pgrazor;

/** Standalone JVM regression harness. No Android classes, no device claims. */
public class RouteAnchorHarness {
  static void check(boolean ok) { if (!ok) throw new AssertionError(); }

  static double[] ref(double x, double y) {
    double w = 1 + .2 * x + .3 * y;
    return new double[]{y * .001, x * .001, 400 * (.2 + .5 * x + .1 * y) / w, 800 * (.3 - .1 * x + .4 * y) / w};
  }

  static String json(double[][] refs, int declared) { return json(refs, declared, null); }

  static String json(double[][] refs, int declared, boolean[] visibility) {
    StringBuilder b = new StringBuilder("{\"at\":" + System.nanoTime() / 1000000L +
        ",\"count\":" + declared + ",\"points\":[");
    for (int i = 0; i < refs.length; i++) {
      double[] r = refs[i];
      if (i > 0) b.append(',');
      boolean visible = visibility == null || visibility[i];
      b.append("{\"id\":\"p").append(i).append("\",\"kind\":\"").append(i % 2 == 0 ? "stop" : "gym")
       .append("\",\"lat\":").append(r[0]).append(",\"lon\":").append(r[1])
       .append(",\"x\":").append(r[2] / 400.0).append(",\"y\":").append(r[3] / 800.0)
       .append(",\"z\":5,\"visible\":").append(visible).append('}');
    }
    return b.append("]}").toString();
  }

  public static void main(String[] args) {
    int width = 400, height = 800;
    double[][] refs = {ref(0, 0), ref(1, 0), ref(0, 1), ref(1, 1), ref(.3, .4), ref(.8, .6)};

    RouteAnchorSnapshot a = RouteAnchorSnapshot.parse(json(refs, 6));
    check(a != null && a.size() == 6 && a.total() == 6 && a.capturedAtMs() > 0);
    RouteGroundProjection p = a.fit(width, height);
    check(p != null);
    double[] q = ref(.6, .7), s = p.project(q[0], q[1]);
    check(Math.hypot(q[2] - s[0], q[3] - s[1]) < 2);

    // Total count includes offscreen records; only the six visible ones are retained and fitted.
    double[][] mixed = new double[8][];
    for (int i = 0; i < 6; i++) mixed[i] = refs[i];
    mixed[6] = new double[]{refs[0][0], refs[0][1], 500, 200, 5, 0};
    mixed[7] = new double[]{refs[1][0], refs[1][1], -120, 900, 5, 0};
    boolean[] flags = {true, true, true, true, true, true, false, false};
    RouteAnchorSnapshot m = RouteAnchorSnapshot.parse(json(mixed, 8, flags));
    check(m != null && m.total() == 8 && m.size() == 6 && m.fit(width, height) != null);

    // More than six independent references: an outlier must not become a fit anchor.
    double[][] eight = new double[8][];
    for (int i = 0; i < 6; i++) eight[i] = refs[i];
    eight[6] = ref(.2, .9);
    eight[7] = ref(.4, .2);
    eight[7][2] += 12;
    RouteAnchorSnapshot h = RouteAnchorSnapshot.parse(json(eight, 8));
    check(h != null && h.size() == 8);
    RouteGroundProjection hp = h.fit(width, height);
    check(hp != null);
    double[] q2 = ref(.5, .8), s2 = hp.project(q2[0], q2[1]);
    check(Math.hypot(q2[2] - s2[0], q2[3] - s2[1]) < 2);

    // Strict contract: count, fields, duplicates, values and truncation.
    check(RouteAnchorSnapshot.parse(json(refs, 7)) == null);
    check(RouteAnchorSnapshot.parse(json(refs, 5)) == null);
    check(RouteAnchorSnapshot.parse(json(refs, 6).replace("\"kind\"", "\"bad\"")) == null);
    check(RouteAnchorSnapshot.parse(json(refs, 6).replace("\"id\":\"p1\"", "\"id\":\"p0\"")) == null);
    check(RouteAnchorSnapshot.parse(json(refs, 6).replace("\"z\":5,", "\"z\":5,\"z\":5,")) == null);
    check(RouteAnchorSnapshot.parse(json(refs, 6).replace("\"z\":5", "\"z\":0")) == null);
    String full = json(refs, 6);
    check(RouteAnchorSnapshot.parse(full.substring(0, full.length() - 3)) == null);

    double[][] broken = new double[6][];
    for (int i = 0; i < 6; i++) broken[i] = refs[i].clone();
    broken[5][2] += 10;
    RouteAnchorSnapshot bad = RouteAnchorSnapshot.parse(json(broken, 6));
    check(bad != null && bad.fit(width, height) == null);

    String offscreen = json(refs, 6, new boolean[]{false, false, false, false, false, false});
    RouteAnchorSnapshot none = RouteAnchorSnapshot.parse(offscreen);
    check(none != null && none.total() == 6 && none.size() == 0 && none.fit(width, height) == null);

    System.out.println("PASS auto anchor snapshot");
  }
}
