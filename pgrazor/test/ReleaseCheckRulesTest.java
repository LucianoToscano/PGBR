package dev.pgrazor;

public final class ReleaseCheckRulesTest {
    private static void check(boolean ok) { if (!ok) throw new AssertionError(); }
    public static void main(String[] args) {
        check(ReleaseCheckRules.newer("v0.00.49-LT", "0.00.48-LT"));
        check(!ReleaseCheckRules.newer("v0.00.48-LT", "0.00.48-LT"));
        check(!ReleaseCheckRules.newer("v0.3.7", "0.00.48-LT"));
        check(!ReleaseCheckRules.newer("v0.00.49-LT-beta", "0.00.48-LT"));
        check(!ReleaseCheckRules.newer("v0.00.49-LT", "unknown"));
        String tag="v0.00.49-LT";
        check("PGBR-v0.00.49-LT.apk".equals(ReleaseCheckRules.asset(tag,false)));
        check("PGBR-v0.00.49-LT-Root.apk".equals(ReleaseCheckRules.asset(tag,true)));
        String name=ReleaseCheckRules.asset(tag,false);
        check(ReleaseCheckRules.trustedAsset(tag,name,ReleaseCheckRules.REPOSITORY+"/download/"+tag+"/"+name));
        check(!ReleaseCheckRules.trustedAsset(tag,name,"https://github.com/jpitty03/PGRazor/releases/download/"+tag+"/"+name));
        check(!ReleaseCheckRules.trustedAsset(tag,name,ReleaseCheckRules.REPOSITORY+"/download/"+tag+"/"+name+"?x=1"));
        check(!ReleaseCheckRules.trustedAsset(tag,"unrelated.apk",ReleaseCheckRules.REPOSITORY+"/download/"+tag+"/unrelated.apk"));
    }
}
