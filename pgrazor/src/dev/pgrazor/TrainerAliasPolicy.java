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
    /** Custom alias used only in PGBR's own labels; never changes PlayerService or account IDs. */
    public static boolean validCustom(String value) {
        if (value == null || !value.equals(value.trim()) || value.length() < 2 || value.length() > 24) return false;
        boolean letterOrNumber = false;
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (Character.isISOControl(ch) || Character.isSurrogate(ch)) return false;
            if (Character.isLetterOrDigit(ch)) { letterOrNumber = true; continue; }
            if (ch != ' ' && ch != '_' && ch != '-' && ch != '.') return false;
        }
        return letterOrNumber && !value.contains("  ");
    }

    public static boolean valid(String value){
        if(value==null||value.length()!=PREFIX.length()+4||!value.startsWith(PREFIX))return false;
        for(int i=PREFIX.length();i<value.length();i++)if(value.charAt(i)<'0'||value.charAt(i)>'9')return false;
        return true;
    }
}
