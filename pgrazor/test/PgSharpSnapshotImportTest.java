package dev.pgrazor;

import java.util.Collections;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

public final class PgSharpSnapshotImportTest {
    private static final long NOW = 1800000000000L;

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static JSONObject row() throws Exception {
        return new JSONObject().put("category","wild").put("coordinate_precision","exact")
                .put("attack_iv",15).put("defense_iv",15).put("stamina_iv",15).put("iv_percent",100)
                .put("dex",25).put("lat",12.3456789).put("lon",23.4567891)
                .put("expires_ms",NOW+600000).put("encounter_id","18446744073709551615");
    }

    private static JSONObject batch(JSONObject... rows) throws Exception {
        JSONArray items = new JSONArray();
        for (JSONObject r : rows) items.put(r);
        return new JSONObject().put("schema",PgSharpHundoBridge.SCHEMA).put("provider","pgsharp")
                .put("coordinate_precision","exact").put("fetched_ms",NOW).put("items",items);
    }

    public static void main(String[] args) throws Exception {
        PgSharpSnapshotImport.Result result = PgSharpSnapshotImport.tryParse(
                batch(row(), row(), row().put("attack_iv",14),
                      row().put("expires_ms",NOW-1), row().put("is_demo",true),
                      row().put("encounter_id",123)).toString(),NOW);
        check(!result.invalid && result.events.size()==1 && result.duplicates==1 && result.rejected==4,
                "strict hundo validation");
        SourceEvent e=result.events.get(0);
        check(SourceRegistry.PGSHARP.equals(e.sourceId) && e.originKind==OriginKind.PERSONAL_IMPORT
                && SourceEvent.QUALITY_IMPORTED.equals(e.dataQuality) && e.expiresAt==NOW+600000
                && e.lat==12.3456789 && e.ivAtk==15 && e.ivDef==15 && e.ivSta==15,
                "canonical imported record retains exact coordinates and expiry, no authenticated origin claim");
        check(PgSharpSnapshotImport.tryParse(batch(row()).put("provider","other").toString(),NOW).invalid,
                "wrong source rejected");
        check(PgSharpSnapshotImport.tryParse(batch(row()).put("fetched_ms",NOW-600001).toString(),NOW).invalid,
                "stale snapshot rejected");
        check(PgSharpSnapshotImport.tryParse(batch(row()).put("is_demo",true).toString(),NOW).invalid,
                "demo snapshot rejected");
        check(PgSharpSnapshotImport.tryParse(batch(row(),row().put("lat",14.0)).toString(),NOW).events.isEmpty(),
                "conflicting encounters removed");
        check(PgSharpSnapshotImport.tryParse("{\"items\":[]}",NOW)==null,"ordinary import unchanged");
        LocalEventStore store = new LocalEventStore();
        check(store.addAll(result.events)==1,"store receives canonical PGSharp row");
        store.add(SourceEvent.builder("import","other",SourceEvent.Kind.POKEMON)
                .coordinates(12.3,23.4).pokemonId(25).ivPercent(100.0).expiresAt(NOW+600000).build());
        LocalEventStore restored = new LocalEventStore();
        restored.mergeJson(store.toJson(),NOW);
        List<SourceAdapter> scoped=Collections.singletonList(new LocalSourceAdapters.PGSharp(restored));
        SourceQuery q=SourceQuery.builder().pokemon(25).build();
        for (SniperSearch.Outcome outcome : new SniperSearch.Outcome[]{
                SniperSearch.sniper1(q,scoped,null,NOW,12.3,23.4,0),
                SniperSearch.sniper2(q,scoped,null,NOW,12.3,23.4,0)}) {
            check(outcome.results.size()==1 && SourceRegistry.PGSHARP.equals(outcome.results.get(0).event.sourceId),
                    "both Snipers receive only persisted source-specific imported data");
        }
        System.out.println("PgSharpSnapshotImportTest: OK");
    }
}
