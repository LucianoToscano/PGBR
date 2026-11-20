package dev.pgrazor;

import java.util.Random;

/** A local visual alias. Never substitutes game/network account identity. */
public final class TrainerAliasPolicy {
    private TrainerAliasPolicy() {}
    private static final String PREFIX="PGBR_Treinador";
    public static String generate(Random random) {
        if(random==null)throw new IllegalArgumentException("random");
        return PREFIX+Integer.toString(10000+random.nextInt(10000)).substring(1);
    }
    public static boolean valid(String value){
        if(value==null||value.length()!=PREFIX.length()+4||!value.startsWith(PREFIX))return false;
        for(int i=PREFIX.length();i<value.length();i++)if(value.charAt(i)<'0'||value.charAt(i)>'9')return false;
        return true;
    }
}
