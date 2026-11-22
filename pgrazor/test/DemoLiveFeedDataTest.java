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
        System.out.println("PASS: demo entries clearly fictitious and bounded; Raid/IV/PvP examples");
    }
}
