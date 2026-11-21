package dev.pgrazor;

/** Tests real PGBR raid rules; no network or game dependencies. */
public final class RaidFeedFiltersTest {
    static void yes(boolean condition, String detail) {
        if(!condition) throw new AssertionError(detail);
    }
    public static void main(String[] args) {
        final long now=1_800_000_000_000L;
        final long end=now+60L*60_000L;
        final RaidFeedFilters all=RaidFeedFilters.defaults();
        yes(all.matches(-1,5,0,now+20_000L,end,false,now),"eggs included by default");
        yes(all.matches(150,5,1,now-10_000L,end,true,now),"hatched raid included");
        yes(!all.matches(150,5,1,now-10_000L,now-1,true,now),"expired raid excluded");
        yes(!all.matches(150,5,1,now-10_000L,0,true,now),"unknown end cannot be live");
        final RaidFeedFilters narrowed=new RaidFeedFilters(150,30,5,2,true);
        yes(narrowed.matches(150,5,2,now-5000L,end,true,now),"all fields correct");
        yes(!narrowed.matches(150,5,2,now-5000L,now+29L*60_000L,true,now),"remaining time");
        yes(!narrowed.matches(151,5,2,now-5000L,end,true,now),"wrong boss");
        yes(!narrowed.matches(150,3,2,now-5000L,end,true,now),"wrong provider raid type");
        yes(!narrowed.matches(150,5,3,now-5000L,end,true,now),"wrong team");
        yes(!narrowed.matches(150,5,2,now+1000L,end,false,now),"egg not boss");
        yes(!narrowed.matches(150,-1,2,now-5000L,end,true,now),"unknown type not a match");
        yes(narrowed.summary().contains("#150"),"summary includes selected boss");
        for(int bad:new int[]{-1,1026}) {
            try {new RaidFeedFilters(bad,0,0,0,false);throw new AssertionError("bad species accepted");}
            catch(IllegalArgumentException expected) {}
        }
        try {new RaidFeedFilters(0,181,0,0,false);throw new AssertionError("invalid time accepted");}
        catch(IllegalArgumentException expected) {}
        try {new RaidFeedFilters(0,0,21,0,false);throw new AssertionError("invalid type accepted");}
        catch(IllegalArgumentException expected) {}
        try {new RaidFeedFilters(0,0,0,4,false);throw new AssertionError("invalid team accepted");}
        catch(IllegalArgumentException expected) {}
        System.out.println("PASS: raid search filters — confirmed inputs only");
    }
}
