package dev.pgrazor;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Experimental read-only boundary. No PGSharp SDK or native transport is linked. */
final class PgSharpHundoBridge {
    static final String SCHEMA = "pgs-authorized-hundo-export-v1";
    static final int MAX_ROWS = 2048;
    static final int MAX_INPUT_CHARS = 262144;
    private static final BigInteger MAX_ID = BigInteger.ONE.shiftLeft(64).subtract(BigInteger.ONE);
    private PgSharpHundoBridge() {}

    // A future approved SDK adapter must supply the same explicit contract.
    interface ReadOnlyModule { Batch readSnapshot() throws Exception; }

    static final class Batch {
        final Object schema, provider, precision, fetched;
        final List<Row> rows;
        Batch(Object schema, Object provider, Object precision, Object fetched, List<Row> rows) {
            this.schema = schema; this.provider = provider; this.precision = precision;
            this.fetched = fetched; this.rows = rows;
        }
    }

    static final class Row {
        final Object category, precision, attack, defense, stamina, iv, dex, form, lat, lon, expiry, id;
        Row(Object category, Object precision, Object attack, Object defense, Object stamina,
            Object iv, Object dex, Object form, Object lat, Object lon, Object expiry, Object id) {
            this.category = category; this.precision = precision; this.attack = attack;
            this.defense = defense; this.stamina = stamina; this.iv = iv; this.dex = dex;
            this.form = form; this.lat = lat; this.lon = lon; this.expiry = expiry; this.id = id;
        }
    }

    static final class Hundo {
        final String encounterId;
        final int dex, form;
        final double lat, lon;
        final long expiresMs;
        Hundo(String id, int dex, int form, double lat, double lon, long expiresMs) {
            this.encounterId = id; this.dex = dex; this.form = form;
            this.lat = lat; this.lon = lon; this.expiresMs = expiresMs;
        }
        String spotKey() { return dex + ":" + form + ":" + lat + ":" + lon + ":" + expiresMs; }
        String key() { return encounterId == null ? "spot:" + spotKey() : "encounter:" + encounterId; }
    }

    static final class Report {
        final List<Hundo> rows;
        final int rejected, duplicates;
        final long fetchedMs;
        Report(List<Hundo> rows, int rejected, int duplicates, long fetchedMs) {
            this.rows = Collections.unmodifiableList(new ArrayList<>(rows));
            this.rejected = rejected; this.duplicates = duplicates; this.fetchedMs = fetchedMs;
        }
    }

    static Report inspect(ReadOnlyModule module, long now) throws Exception {
        if (module == null) throw new IllegalArgumentException("No approved module connected");
        return validate(module.readSnapshot(), now);
    }

    static Report validate(Batch batch, long now) {
        if (now <= 0 || now > Long.MAX_VALUE - 7200000 || batch == null
                || !SCHEMA.equals(batch.schema) || !"pgsharp".equals(batch.provider)
                || !"exact".equals(batch.precision) || !integer(batch.fetched, now - 600000, now + 60000)
                || batch.rows == null || batch.rows.size() > MAX_ROWS) {
            throw new IllegalArgumentException("Invalid or stale export envelope");
        }
        Map<String,Hundo> accepted = new LinkedHashMap<>();
        Set<String> conflicts = new HashSet<>();
        int rejected = 0, duplicates = 0;
        for (Row r : batch.rows) {
            if (r == null || !"wild".equals(r.category) || !"exact".equals(r.precision)
                    || !integer(r.attack, 15, 15) || !integer(r.defense, 15, 15)
                    || !integer(r.stamina, 15, 15) || (r.iv != null && !integer(r.iv, 100, 100))
                    || !integer(r.dex, 1, 65535) || (r.form != null && !integer(r.form, 0, 65535))
                    || !finite(r.lat, -90, 90) || !finite(r.lon, -180, 180)
                    || (number(r.lat) == 0 && number(r.lon) == 0)
                    || !integer(r.expiry, now + 60000, now + 7200000)
                    || (r.id != null && !validId(r.id))) { rejected++; continue; }
            Hundo h = new Hundo((String)r.id, (int)number(r.dex), r.form == null ? 0 : (int)number(r.form),
                    number(r.lat), number(r.lon), ((Number)r.expiry).longValue());
            String key = h.key();
            if (conflicts.contains(key)) { rejected++; continue; }
            Hundo old = accepted.get(key);
            if (old == null) accepted.put(key, h);
            else if (old.spotKey().equals(h.spotKey())) duplicates++;
            else { accepted.remove(key); conflicts.add(key); rejected += 2; }
        }
        return new Report(new ArrayList<>(accepted.values()), rejected, duplicates,
                ((Number)batch.fetched).longValue());
    }

    private static boolean validId(Object v) {
        return v instanceof String && ((String)v).matches("[1-9][0-9]{0,19}")
                && new BigInteger((String)v).compareTo(MAX_ID) <= 0;
    }
    private static double number(Object v) { return ((Number)v).doubleValue(); }
    private static boolean finite(Object v, double min, double max) {
        return v instanceof Number && Double.isFinite(number(v)) && number(v) >= min && number(v) <= max;
    }
    private static boolean integer(Object v, long min, long max) {
        return finite(v, min, max) && number(v) == Math.rint(number(v));
    }
}
