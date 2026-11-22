package dev.pgrazor;

import android.content.Context;
import android.content.SharedPreferences;

/** Account-scoped, manually confirmed event progress; no guesses about rewards or server state. */
public final class EternatusProgress {
    private static final String PREFS="pgrazor-event-eternatus-v1";
    private static final String CLAIMED="confirmed_claims";
    private EternatusProgress() {}
    private static SharedPreferences preferences(Context context){return AccountProfiles.preferences(context.getApplicationContext(),PREFS);}
    public static int confirmed(Context context){return Math.max(0,Math.min(10000,preferences(context).getInt(CLAIMED,0)));}
    public static boolean confirm(Context context){
        SharedPreferences p=preferences(context);int value=confirmed(context);
        if(value>=10000)return false;
        return p.edit().putInt(CLAIMED,value+1).commit();
    }
    public static boolean clear(Context context){return preferences(context).edit().remove(CLAIMED).commit();}
}
