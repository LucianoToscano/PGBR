package dev.pgrazor;

public final class DemoLiveFeedDataTest {
    static void ok(boolean valid,String why) {if(!valid)throw new AssertionError(why);}
    public static void main(String[] args) {
        long now=1800000000000L;
        LiveFeedData.Snapshot pokemon=DemoLiveFeedData.sample(-23.55,-46.63,now,false);
        ok(pokemon.entries.size()==8,"eight synthetic Pokémon");
        ok(pokemon.note.contains("FICTÍCIOS"),"the source explicitly labels all results");
        LiveFeedData.Entry p=pokemon.entries.get(0);
        ok(p.id.startsWith("demo:")&&p.dexId==25&&p.iv==100,"known hundo example");
        ok(p.details.contains("FICTÍCIOS"),"each item is clearly synthetic");
        ok(p.expiresAt>now && p.expiresAt<now+3600000L,"expirations are bounded");
        ok(pokemon.entries.get(3).iv<0 && !pokemon.entries.get(3).hasStats,
                "unknown IV stays unknown in demo");
        ok(pokemon.entries.get(4).attrs.size==5,"XXL filter has an example");
        ok(pokemon.entries.get(1).attrs.pvpGreatRank==1,"PvP rank example exists");
        for(LiveFeedData.Entry row:pokemon.entries){
            ok(row.id.startsWith("demo:")&&row.details.contains("FICTÍCIOS"),
                "every row is a fictional example");
            ok(row.lat>-90&&row.lat<90&&row.lon>-180&&row.lon<180,
                "coordinates valid for local UI only");
        }
        LiveFeedData.Snapshot raids=DemoLiveFeedData.sample(-23.55,-46.63,now,true);
        ok(raids.entries.size()==4,"four synthetic raids");
        ok(raids.entries.get(0).raidBossLive,"example hatched raid");
        ok(!raids.entries.get(3).raidBossLive,"example egg has no guaranteed boss");
        ok(raids.entries.get(3).dexId==-1,"unhatched example hides boss");
        LiveFeedData.Snapshot fallback=DemoLiveFeedData.sample(
            Double.NaN, Double.POSITIVE_INFINITY,now,false);
        ok(fallback.entries.size()==8,"invalid current game position uses only safe demo center");
        // Radius-aware demo: the radius control must be exercised, not ignored.
        LiveFeedData.Snapshot wide=DemoLiveFeedData.sample(-23.55,-46.63,now,false,25);
        ok(wide.entries.size()==8,"25 km keeps every example (fixture is ~200 m wide)");
        ok(wide.note.contains("8 de 8"),"note states shown/total");
        LiveFeedData.Snapshot none=DemoLiveFeedData.sample(-23.55,-46.63,now,false,0.001);
        ok(none.entries.size()<8,"very small radius excludes distant examples");
        LiveFeedData.Snapshot global=DemoLiveFeedData.sample(-23.55,-46.63,now,false,
                Double.POSITIVE_INFINITY);
        ok(global.entries.size()==8,"global radius returns the whole fixture");
        // The fixture is generated around the player, never around real spawns: a far centre must
        // produce examples near THAT centre (and still fictional), not leak any real coordinate.
        LiveFeedData.Snapshot far=DemoLiveFeedData.sample(51.5,-0.12,now,false,1);
        ok(far.entries.size()==8,"fixture follows the player position");
        for (LiveFeedData.Entry e:far.entries)
            ok(DemoLiveFeedData.distanceKm(51.5,-0.12,e.lat,e.lon)<1,"every example is near the centre");
        ok(DemoLiveFeedData.distanceKm(-23.55,-46.63,-23.55,-46.63)==0,"distance of a point to itself");
        System.out.println("PASS: demo entries clearly fictitious and bounded; Raid/IV/PvP examples; "
                + "radius filter honoured");
    }
}
