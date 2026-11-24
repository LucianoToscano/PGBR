import dev.pgrazor.GpxCodec;
import dev.pgrazor.GpxRoute;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/** Exercises the standard-SAX GPX codec: parsing shapes, security limits, and round-tripping. */
public final class GpxCodecTest {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
    private static List<GpxRoute> read(String xml) throws IOException {
        return GpxCodec.read(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)), "Fallback");
    }
    private static void rejects(String why, String xml) {
        try { read(xml); throw new AssertionError("Expected rejection: " + why); }
        catch (IOException expected) { }
    }
    private static boolean near(double a, double b) { return Math.abs(a - b) < 1e-9; }

    public static void main(String[] args) throws Exception {
        rejects("GPX root required","<html><wpt lat='1' lon='2'/></html>");
        String utf16="<?xml version='1.0' encoding='UTF-16'?><!DOCTYPE gpx [<!ENTITY x '1'>]><gpx><wpt lat='&x;' lon='2'/></gpx>";
        try { GpxCodec.read(new ByteArrayInputStream(utf16.getBytes(StandardCharsets.UTF_16)),"bad");throw new AssertionError("UTF16 DTD accepted"); }
        catch(IOException expected) {}

        // --- Waypoint fallback, non-namespaced, order preserved (shape of the user sample) ---
        List<GpxRoute> wpt = read("<gpx version=\"1.1\"><wpt lat=\"41.1\" lon=\"2.1\"/>"
            + "<wpt lat=\"41.2\" lon=\"2.2\"/><wpt lat=\"41.3\" lon=\"2.3\"/></gpx>");
        check(wpt.size() == 1, "Top-level waypoints collapse into one route");
        check(wpt.get(0).name.equals("Fallback"), "Unnamed waypoint route uses the fallback name");
        double[][] p = wpt.get(0).points();
        check(p.length == 3 && near(p[0][0], 41.1) && near(p[2][1], 2.3), "Waypoint order is preserved");

        // --- Default-namespaced track with a name ---
        List<GpxRoute> trk = read("<gpx xmlns=\"http://www.topografix.com/GPX/1/1\" version=\"1.1\">"
            + "<trk><name>City loop</name><trkseg>"
            + "<trkpt lat=\"41.0\" lon=\"2.0\"></trkpt><trkpt lat=\"41.5\" lon=\"2.5\"></trkpt>"
            + "</trkseg></trk></gpx>");
        check(trk.size() == 1 && trk.get(0).name.equals("City loop"), "Namespaced track name parsed");
        check(trk.get(0).size() == 2, "Namespaced track points parsed");

        // --- Prefixed namespace is handled the same way ---
        List<GpxRoute> prefixed = read("<gpx:gpx xmlns:gpx=\"http://www.topografix.com/GPX/1/1\" version=\"1.1\">"
            + "<gpx:trk><gpx:name>Prefixed</gpx:name><gpx:trkseg>"
            + "<gpx:trkpt lat=\"1\" lon=\"2\"/><gpx:trkpt lat=\"3\" lon=\"4\"/>"
            + "</gpx:trkseg></gpx:trk></gpx:gpx>");
        check(prefixed.size() == 1 && prefixed.get(0).name.equals("Prefixed") && prefixed.get(0).size() == 2, "Prefixed namespace parsed");

        // --- Multiple tracks become separate routes, in document order ---
        List<GpxRoute> many = read("<gpx version=\"1.1\">"
            + "<trk><name>First</name><trkseg><trkpt lat=\"1\" lon=\"1\"/></trkseg></trk>"
            + "<trk><name>Second</name><trkseg><trkpt lat=\"2\" lon=\"2\"/></trkseg></trk></gpx>");
        check(many.size() == 2 && many.get(0).name.equals("First") && many.get(1).name.equals("Second"), "Tracks map to ordered separate routes");

        // --- Multiple segments split one track into several routes ---
        List<GpxRoute> split = read("<gpx version=\"1.1\"><trk><name>Trail</name>"
            + "<trkseg><trkpt lat=\"1\" lon=\"1\"/></trkseg>"
            + "<trkseg><trkpt lat=\"2\" lon=\"2\"/></trkseg></trk></gpx>");
        check(split.size() == 2, "Segments split a track into separate routes");
        check(split.get(0).name.contains("segment 1") && split.get(1).name.contains("segment 2"), "Split routes are numbered by segment");

        // A full-length name must not hide segment identity or split a Unicode surrogate pair.
        String longName = "A".repeat(80);
        String longPrefix = "<gpx><trk><name>";
        String longTail = "</name><trkseg><trkpt lat='1' lon='1'/></trkseg>"
            + "<trkseg><trkpt lat='2' lon='2'/></trkseg></trk></gpx>";
        List<GpxRoute> longSplit = read(longPrefix + longName + longTail);
        check(longSplit.size() == 2 && !longSplit.get(0).name.equalsIgnoreCase(longSplit.get(1).name), "Long segment names remain distinct");
        check(longSplit.get(0).name.endsWith(" (segment 1)") && longSplit.get(1).name.endsWith(" (segment 2)"), "Segment suffix preserved");
        check(longSplit.get(0).name.length() <= GpxRoute.NAME_LIMIT && longSplit.get(1).name.length() <= GpxRoute.NAME_LIMIT, "Segment names remain bounded");
        String emojiName = "A".repeat(GpxRoute.NAME_LIMIT - " (segment 1)".length() - 1) + "\uD83D\uDE00" + "B".repeat(20);
        List<GpxRoute> emojiSplit = read(longPrefix + emojiName + longTail);
        check(emojiSplit.get(0).name.equals("A".repeat(GpxRoute.NAME_LIMIT - " (segment 1)".length() - 1) + " (segment 1)"), "No split surrogate at segment suffix boundary");
        ByteArrayOutputStream segmentOut = new ByteArrayOutputStream();
        GpxCodec.write(segmentOut, emojiSplit.get(0));
        check(GpxCodec.read(new ByteArrayInputStream(segmentOut.toByteArray()), "ignored").get(0).name.equals(emojiSplit.get(0).name), "Long segment name round-trips");

        // --- A real route is preferred over unrelated waypoints in the same file ---
        List<GpxRoute> mixed = read("<gpx version=\"1.1\">"
            + "<wpt lat=\"10\" lon=\"10\"/><wpt lat=\"11\" lon=\"11\"/>"
            + "<trk><name>Real</name><trkseg><trkpt lat=\"1\" lon=\"1\"/></trkseg></trk></gpx>");
        check(mixed.size() == 1 && mixed.get(0).name.equals("Real"), "Tracks win over unrelated waypoints");

        // --- Route (rte/rtept) parsing ---
        List<GpxRoute> rte = read("<gpx version=\"1.1\"><rte><name>Plan</name>"
            + "<rtept lat=\"5\" lon=\"6\"/><rtept lat=\"7\" lon=\"8\"/></rte></gpx>");
        check(rte.size() == 1 && rte.get(0).name.equals("Plan") && rte.get(0).size() == 2, "Route elements parsed");

        // --- Security: DOCTYPE / external entity (XXE) is rejected ---
        rejects("external-entity XXE", "<?xml version=\"1.0\"?><!DOCTYPE gpx [<!ENTITY xxe SYSTEM \"file:///etc/passwd\">]>"
            + "<gpx version=\"1.1\"><trk><name>&xxe;</name><trkseg><trkpt lat=\"1\" lon=\"1\"/></trkseg></trk></gpx>");
        // --- Security: entity-expansion (billion laughs) is rejected along with the DOCTYPE ---
        rejects("entity expansion", "<?xml version=\"1.0\"?><!DOCTYPE gpx [<!ENTITY a \"aaaaaaaaaa\">"
            + "<!ENTITY b \"&a;&a;&a;&a;&a;&a;&a;&a;&a;&a;\">]>"
            + "<gpx version=\"1.1\"><wpt lat=\"1\" lon=\"&b;\"/></gpx>");

        // --- Invalid or missing coordinates are rejected (never silently dropped) ---
        rejects("non-numeric latitude", "<gpx version=\"1.1\"><wpt lat=\"abc\" lon=\"2\"/></gpx>");
        rejects("latitude out of range", "<gpx version=\"1.1\"><wpt lat=\"200\" lon=\"2\"/></gpx>");
        rejects("infinite coordinate", "<gpx version=\"1.1\"><wpt lat=\"1e999\" lon=\"2\"/></gpx>");
        rejects("missing longitude", "<gpx version=\"1.1\"><wpt lat=\"1\"/></gpx>");
        rejects("malformed xml", "<gpx version=\"1.1\"><trk><trkseg><trkpt lat=\"1\" lon=\"1\">");

        // --- Limit: a route over MAX_POINTS is rejected, not truncated ---
        StringBuilder big = new StringBuilder("<gpx version=\"1.1\"><trk><trkseg>");
        for (int i = 0; i <= GpxCodec.MAX_POINTS; i++) big.append("<trkpt lat=\"1\" lon=\"1\"/>");
        big.append("</trkseg></trk></gpx>");
        rejects("over the point cap", big.toString());

        // Aggregate bounds: many tiny routes cannot overwhelm the chooser.
        StringBuilder manyRoutes=new StringBuilder("<gpx>");
        for(int i=0;i<=GpxCodec.MAX_ROUTES;i++)manyRoutes.append("<rte><rtept lat='0' lon='0'/></rte>");
        rejects("too many routes",manyRoutes.append("</gpx>").toString());
        StringBuilder manySegments=new StringBuilder("<gpx><trk>");
        for(int i=0;i<=GpxCodec.MAX_ROUTES;i++)manySegments.append("<trkseg><trkpt lat='0' lon='0'/></trkseg>");
        rejects("too many segments",manySegments.append("</trk></gpx>").toString());
        StringBuilder tooMany=new StringBuilder("<gpx>");
        for(int i=0;i<=GpxCodec.MAX_TOTAL_POINTS;i++)tooMany.append("<wpt lat='0' lon='0'/>");
        rejects("aggregate point limit",tooMany.append("</gpx>").toString());
        rejects("foreign root namespace","<gpx xmlns='urn:unrelated'><wpt lat='0' lon='0'/></gpx>");
        check(read("<gpx><rte><name>"+" ".repeat(150)+"Trilha</name><rtept lat='0' lon='0'/></rte></gpx>").get(0).name.equals("Trilha"),"Leading whitespace does not consume name budget");
        // --- Limit: input larger than MAX_BYTES is rejected before parsing ---
        try {
            GpxCodec.read(new ByteArrayInputStream(new byte[GpxCodec.MAX_BYTES + 1]), "Fallback");
            throw new AssertionError("Expected rejection: oversized input");
        } catch (IOException expected) { }

        // XML-safe names, including supplementary characters and malformed fallback names.
        for (String badName : new String[]{"X\uD800", "X\uDC00", "X\uFFFE", "X\uFFFF"}) {
            try {
                new GpxRoute(badName, new double[][]{{0,0}});
                throw new AssertionError("Invalid XML name accepted");
            } catch (IllegalArgumentException expected) { }
        }
        GpxRoute sanitized = GpxCodec.read(new ByteArrayInputStream("<gpx><wpt lat='0' lon='0'/></gpx>".getBytes(StandardCharsets.UTF_8)), "A\uD800B\uFFFE").get(0);
        check(sanitized.name.equals("A B"), "Invalid fallback characters sanitized");
        ByteArrayOutputStream unicode = new ByteArrayOutputStream();
        GpxCodec.write(unicode, new GpxRoute("Trilha \uD83D\uDE00", new double[][]{{0,0}}));
        check(GpxCodec.read(new ByteArrayInputStream(unicode.toByteArray()), "fallback").get(0).name.equals("Trilha \uD83D\uDE00"), "Supplementary character round-trips");

        // A mathematically valid coordinate can be very long in xsd:decimal form.
        // Oversized exports must be rejected before even one byte reaches the destination.
        double[][] extreme = new double[GpxCodec.MAX_POINTS][2];
        for (int i=0;i<extreme.length;i++) { extreme[i][0]=Double.MIN_VALUE; extreme[i][1]=-Double.MIN_VALUE; }
        ByteArrayOutputStream untouched = new ByteArrayOutputStream();
        untouched.write(7);
        try {
            GpxCodec.write(untouched, new GpxRoute("Extremo", extreme));
            throw new AssertionError("Oversized GPX export accepted");
        } catch (IOException expected) { }
        check(untouched.size()==1 && untouched.toByteArray()[0]==7, "Oversized export leaves destination untouched");

        // --- Round trip: write then read reproduces name and ordered points exactly ---
        double[][] points = {{41.384210, 2.179990}, {-41.5, -2.25}, {0.0, 123.456789}};
        GpxRoute original = new GpxRoute("Trip & <friends>", points);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        GpxCodec.write(out, original);
        List<GpxRoute> back = GpxCodec.read(new ByteArrayInputStream(out.toByteArray()), "ignored");
        check(back.size() == 1, "Written GPX reads back as one route");
        check(back.get(0).name.equals("Trip & <friends>"), "Name escaping round-trips");
        double[][] r = back.get(0).points();
        check(r.length == points.length, "Round-trip point count matches");
        for (int i = 0; i < points.length; i++)
            check(r[i][0] == points[i][0] && r[i][1] == points[i][1], "Round-trip point " + i + " matches exactly");

        System.out.println("PASS: GpxCodec parsing, security limits and round-trip");

        // --- Optional: the user's real sample at the repository root ---
        if (args.length > 0 && args[0] != null && !args[0].isEmpty()) {
            Path sample = Paths.get(args[0]);
            if (Files.isRegularFile(sample)) {
                try (InputStream in = Files.newInputStream(sample)) {
                    List<GpxRoute> routes = GpxCodec.read(in, "Barcelona");
                    check(routes.size() == 1, "Sample waypoints collapse into a single route");
                    GpxRoute route = routes.get(0);
                    check(route.size() > 100 && route.size() <= GpxCodec.MAX_POINTS, "Sample point count is bounded");
                    double[][] s = route.points();
                    check(near(s[0][0], 41.384210) && near(s[0][1], 2.179990), "Sample first point preserved in order");
                    check(near(s[s.length - 1][0], 41.377620) && near(s[s.length - 1][1], 2.184080), "Sample last point preserved in order");
                    for (double[] q : s) check(Math.abs(q[0]) <= 90 && Math.abs(q[1]) <= 180, "Every sample coordinate is in range");
                }
                System.out.println("PASS: GpxCodec parses the Barcelona sample (" + Paths.get(args[0]) + ")");
            } else {
                System.out.println("SKIP: sample not found at " + sample);
            }
        }
    }
}
