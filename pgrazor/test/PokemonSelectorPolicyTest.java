package dev.pgrazor;

/** Standalone JVM regression test for the actual selector's no-fake-result policy. */
public final class PokemonSelectorPolicyTest {
    private static void ok(boolean value,String label) {
        if (!value) throw new AssertionError(label);
    }
    public static void main(String[] args) {
        long now=1800000000000L;
        ok(PokemonSelectorPolicy.matchesSpecies(56,"Mankey","mank"),"partial name");
        ok(PokemonSelectorPolicy.matchesSpecies(115,"Kangaskhan","#115"),"numeric dex");
        ok(PokemonSelectorPolicy.matchesSpecies(25,"Pikachu","PIKA"),"case insensitive");
        ok(PokemonSelectorPolicy.matchesSpecies(1,"Nidorán","nidoran"),"accent fold");
        ok(!PokemonSelectorPolicy.matchesSpecies(25,"Pikachu","eevee"),"different species");
        LiveFeedData.Entry alive=new LiveFeedData.Entry("real",LiveFeedData.Kind.POKEMON,
                "Mankey","PC 100",35.6,139.6,56,100,10,89,now+100000L,true);
        LiveFeedData.Entry expired=new LiveFeedData.Entry("old",LiveFeedData.Kind.POKEMON,
                "Mankey","",35.6,139.6,56,-1,-1,-1,now-1L,false);
        LiveFeedData.Entry wrong=new LiveFeedData.Entry("other",LiveFeedData.Kind.POKEMON,
                "Kangaskhan","",35.6,139.6,115,-1,-1,-1,now+100000L,false);
        LiveFeedData.Entry fake=new LiveFeedData.Entry("fake",LiveFeedData.Kind.POKEMON,
                "Mankey","",0,0,56,-1,-1,-1,now+100000L,false);
        LiveFeedData.Entry raid=new LiveFeedData.Entry("raid",LiveFeedData.Kind.RAID,
                "Mankey","",35.6,139.6,56,-1,-1,-1,now+100000L,false);
        ok(PokemonSelectorPolicy.validResult(alive,56,now),"real matching spawn");
        ok(!PokemonSelectorPolicy.validResult(expired,56,now),"stale position");
        ok(!PokemonSelectorPolicy.validResult(wrong,56,now),"wrong species");
        ok(!PokemonSelectorPolicy.validResult(fake,56,now),"missing coordinates");
        ok(!PokemonSelectorPolicy.validResult(raid,56,now),"not a spawn");
        ok(PokemonSelectorPolicy.timeRemaining(now+61000L,now).equals("1:01"),
                "time to despawn");
        System.out.println("PASS: PGBR selector species and teleport safety");
    }
}
