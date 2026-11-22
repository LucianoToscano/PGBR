package dev.pgrazor;

/** Numeric release ordering: never offer equal builds or a downgrade. */
public final class UpdateVersion {
    private UpdateVersion() {}
    private static int[] parse(String raw) {
        if(raw==null)return null;
        java.util.regex.Matcher m=java.util.regex.Pattern
            .compile("^v?(\\d{1,6})\\.(\\d{1,6})\\.(\\d{1,6})(?:[-+][A-Za-z0-9.-]+)?$")
            .matcher(raw.trim());
        if(!m.matches())return null;
        try{return new int[]{Integer.parseInt(m.group(1)),Integer.parseInt(m.group(2)),Integer.parseInt(m.group(3))};}
        catch(NumberFormatException ignored){return null;}
    }
    public static boolean valid(String tag){return parse(tag)!=null;}
    public static boolean isNewer(String published,String installed){
        int[] next=parse(published),current=parse(installed);
        if(next==null||current==null)return false;
        for(int i=0;i<3;i++){if(next[i]>current[i])return true;if(next[i]<current[i])return false;}
        return false;
    }
}
