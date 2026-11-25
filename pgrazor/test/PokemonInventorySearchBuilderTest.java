package dev.pgrazor;

public final class PokemonInventorySearchBuilderTest {
    private static void check(boolean ok, String why) {
        if (!ok) throw new AssertionError(why);
    }
    private static String build(int iv, boolean shiny, boolean shadow, boolean xxl,
                                boolean background, boolean legacy, String dex) {
        return PokemonInventorySearchBuilder.build(iv, shiny, shadow, xxl, background, legacy, dex);
    }
    private static void bad(int iv, String dex) {
        try { build(iv, false, false, false, false, false, dex); }
        catch (IllegalArgumentException expected) { return; }
        throw new AssertionError("Expected invalid preset/dex");
    }
    public static void main(String[] args) {
        check("".equals(build(0,false,false,false,false,false,"")), "empty defaults");
        check("4*".equals(build(1,false,false,false,false,false,"")), "hundo");
        check("0attack&0defense&0hp".equals(build(2,false,false,false,false,false,"")), "nundo");
        check("0attack&4defense&4hp".equals(build(3,false,false,false,false,false,"")), "PvP candidate");
        check("4*&shiny&shadow&xxl&background&@special&150".equals(
                    build(1,true,true,true,true,true,"0150")), "deterministic combined search");
        check("25".equals(build(0,false,false,false,false,false,"25")), "dex only");
        check("3*".equals(build(4,false,false,false,false,false,"")), "3 star");
        check("4*&xxs&background&150".equals(PokemonInventorySearchBuilder.build(
                1,false,false,PokemonInventorySearchBuilder.SIZE_XXS,true,false,"150")), "XXS with other criteria");
        for(int iv=0;iv<=4;iv++)for(int flags=0;flags<16;flags++){
            boolean shiny=(flags&1)!=0, shadow=(flags&2)!=0, background=(flags&4)!=0, legacy=(flags&8)!=0;
            for(int size=0;size<=2;size++){
                String result=PokemonInventorySearchBuilder.build(iv,shiny,shadow,size,background,legacy,"25");
                check(result.contains("xxl")== (size==1) && result.contains("xxs")== (size==2), "exclusive size");
                if(size<=1)check(result.equals(build(iv,shiny,shadow,size==1,background,legacy,"25")), "legacy overload unchanged");
            }
        }
        for(int invalid:new int[]{-1,3,Integer.MAX_VALUE})try {
            PokemonInventorySearchBuilder.build(0,false,false,invalid,false,false,"");
            throw new AssertionError("invalid size accepted");
        }catch(IllegalArgumentException expected){}
        bad(-1, ""); bad(5, ""); bad(0, "0"); bad(0, "1026"); bad(0, "25&shiny");
        bad(0, "Pikachu"); bad(0, "99999");
        System.out.println("PASS: independent and safe inventory query builder");
    }
}
