package dev.pgrazor;

/** Ordered, unique catch-berry choices; no inventory data is persisted in the policy. */
public final class BerryPriority {
    private BerryPriority() {}
    public static boolean validId(int id) { return id==701||id==703||id==705||id==706||id==707; }
    /**
     * Portuguese names for the five berry IDs the app accepts, kept beside the IDs they name.
     *
     * <p>The pairing is authoritative, not positional: {@code ITEM_RAZZ_BERRY=701},
     * {@code ITEM_NANAB_BERRY=703}, {@code ITEM_PINAP_BERRY=705},
     * {@code ITEM_GOLDEN_RAZZ_BERRY=706}. Nanab is 703 — the banana — and Pinap is 705, the
     * pineapple; a label list written in another order silently swaps the two.
     *
     * <p>Screens used to pass their English label through {@code PtBr.tr}. That only matches on an
     * exact key, so the five plain labels translated and every composed line — {@code "Razz Berry
     * (12)"}, {@code "Razz Berry, priority 1"} — fell through to the prefix rules and reached the
     * screen half translated. Deriving the text from the ID closes that gap.
     */
    public static String displayName(int id) {
        switch (id) {
            case 701: return "Fruta Frambo";
            case 703: return "Fruta Caxí";
            case 705: return "Fruta Abacaxi";
            case 706: return "Fruta Frambo Dourada";
            case 707: return "Fruta Abacaxi Prateada";
            default: return "";
        }
    }
    public static int[] parse(String value) {
        if(value==null||value.length()>11)throw new IllegalArgumentException("Invalid berry priorities");
        if(value.isEmpty())return new int[0];
        String[] parts=value.split(",",-1);
        if(parts.length>3)throw new IllegalArgumentException("Too many berry priorities");
        int[] ids=new int[parts.length];
        for(int i=0;i<parts.length;i++){
            if(!parts[i].matches("[0-9]{3}"))throw new IllegalArgumentException("Invalid berry");
            ids[i]=Integer.parseInt(parts[i]);
            if(!validId(ids[i]))throw new IllegalArgumentException("Invalid berry");
            for(int j=0;j<i;j++)if(ids[j]==ids[i])throw new IllegalArgumentException("Duplicate berry");
        }
        return ids;
    }
    public static String select(String current,int id) {
        if(id==0)return "";
        if(!validId(id))throw new IllegalArgumentException("Invalid berry");
        int[] ids=parse(current);
        if(ids.length==3)return String.valueOf(id);
        for(int chosen:ids)if(chosen==id)return current;
        return current.isEmpty()?String.valueOf(id):current+","+id;
    }
}
