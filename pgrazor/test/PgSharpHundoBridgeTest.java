package dev.pgrazor;

import java.util.Arrays;
import java.util.Collections;
import static dev.pgrazor.PgSharpHundoBridge.*;

public final class PgSharpHundoBridgeTest {
    private static final long NOW = 1700000000000L;
    private static Row row(Object a, Object d, Object s, Object iv, Object dex, Object form,
            Object lat, Object lon, Object end, Object id, String cat, String precision) {
        return new Row(cat, precision, a, d, s, iv, dex, form, lat, lon, end, id);
    }
    private static Row good(Object id, double lat) {
        return row(15,15,15,100,25,0,lat,23.4567891,NOW+300000,id,"wild","exact");
    }
    private static Batch batch(Row... rows) {
        return new Batch(SCHEMA,"pgsharp","exact",NOW,Arrays.asList(rows));
    }
    private static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
    private static void invalid(Batch b) {
        try { validate(b,NOW); throw new AssertionError("accepted invalid batch"); }
        catch (IllegalArgumentException expected) { }
    }
    public static void main(String[] args) throws Exception {
        Report r=inspect(()->batch(good("18446744073709551615",12.3456789),good(null,0)),NOW);
        check(r.rows.size()==2 && r.rejected==0 && r.rows.get(0).lat==12.3456789
                && r.rows.get(1).encounterId==null,"preserves precise coordinates and real IDs");
        Row[] bad={row(14,15,15,100,25,0,12,23,NOW+300000,"1","wild","exact"),
            row(15,15,null,100,25,0,12,23,NOW+300000,"1","wild","exact"),
            row(15,15,15,99,25,0,12,23,NOW+300000,"1","wild","exact"),
            row(15,15,15,100,25,0,12,23,NOW+300000,"1","raid","exact"),
            row(15,15,15,100,25,0,12,23,NOW+300000,"1","wild","approximate"),
            row(15,15,15,100,25,0,0,0,NOW+300000,"1","wild","exact"),
            row(15,15,15,100,25,0,"12",23,NOW+300000,"1","wild","exact"),
            row(15,15,15,100,25,0,Double.NaN,23,NOW+300000,"1","wild","exact"),
            row(15,15,15,100,25,0,91,23,NOW+300000,"1","wild","exact"),
            row(15,15,15,100,25,0,12,181,NOW+300000,"1","wild","exact"),
            row(15,15,15,100,25,0,12,23,NOW-1,"1","wild","exact"),
            row(15,15,15,100,25,0,12,23,NOW+7200001,"1","wild","exact"),
            good("18446744073709551616",12),good("0",12),good(123,12),null};
        r=validate(batch(bad),NOW);
        check(r.rows.isEmpty() && r.rejected==bad.length,"reject all invalid rows");
        r=validate(batch(good("1",12),good("1",12),good("1",13),good("1",14),good(null,12),good(null,12)),NOW);
        check(r.rows.size()==1 && r.duplicates==2 && r.rejected==3,"dedup and conflict rejection");
        invalid(new Batch(SCHEMA,"other","exact",NOW,Collections.emptyList()));
        invalid(new Batch(SCHEMA,"pgsharp","approximate",NOW,Collections.emptyList()));
        invalid(new Batch("unknown","pgsharp","exact",NOW,Collections.emptyList()));
        invalid(new Batch(SCHEMA,"pgsharp","exact",NOW-600001,Collections.emptyList()));
        invalid(new Batch(SCHEMA,"pgsharp","exact",NOW+60001,Collections.emptyList()));
        invalid(new Batch(SCHEMA,"pgsharp","exact",NOW,Collections.nCopies(MAX_ROWS+1,good(null,12))));
        check(validate(batch(),NOW).rows.isEmpty(),"no simulated rows");
        try { inspect(null,NOW); throw new AssertionError("accepted absent module"); }
        catch (IllegalArgumentException expected) { }
        System.out.println("PgSharpHundoBridgeTest passed");
    }
}
