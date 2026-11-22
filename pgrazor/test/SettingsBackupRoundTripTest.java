package dev.pgrazor;

import android.app.Application;
import java.io.File;
import java.lang.reflect.Method;
import org.json.JSONException;
import org.json.JSONObject;

/** Exercises the same JSON snapshot and restore used by the Android picker, on a JVM. */
public final class SettingsBackupRoundTripTest {
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private static void reload(Application app)throws Exception{
        Control.flushSettings(5000);
        Method load=Control.class.getDeclaredMethod("loadSettings",Application.class);load.setAccessible(true);load.invoke(null,app);
    }
    private static JSONObject copy(JSONObject document)throws Exception{return new JSONObject(document.toString());}
    private static void verify(boolean enabled,int shield,int swap,String label){
        check(Control.smartAttack()==enabled && Control.smartShieldTiming()==shield && Control.smartFaintedSwapTiming()==swap,label);
    }
    public static void main(String[] args)throws Exception{
        File root=new File(args[0],"backup-round-trip");check(root.mkdirs(),"test directory");
        Application app=new Application(){public File getFilesDir(){return root;}};reload(app);Control.engine.speed(5.0);
        Control.setSmartAttack(true);Control.setSmartShieldTiming(2);Control.setSmartFaintedSwapTiming(0);
        Control.setQuickMenuVisible(QuickMenuVisibilityPolicy.SELECTOR_BIT,false);
        AccountProfiles.preferences(app,"pgrazor-map").edit().putBoolean("Stops",true).commit();
        Control.flushSettings(5000);
        JSONObject backup=new JSONObject(SettingsBackupStore.snapshot(app).toString(2));
        SettingsBackupRules.validate(backup);
        JSONObject settings=backup.getJSONObject("settings");
        check("true".equals(settings.getString("smartAttack")) && "2".equals(settings.getString("smartShieldTiming")) && "0".equals(settings.getString("smartFaintedSwapTiming")),"exported choices");
        check((Integer.parseInt(settings.getString("quickMenuHidden")) & QuickMenuVisibilityPolicy.SELECTOR_BIT)!=0,"selector exported");
        Control.setSmartAttack(false);Control.setSmartShieldTiming(0);Control.setSmartFaintedSwapTiming(2);
        Control.setQuickMenuVisible(QuickMenuVisibilityPolicy.SELECTOR_BIT,true);
        AccountProfiles.preferences(app,"pgrazor-map").edit().putBoolean("Stops",false).commit();
        SettingsBackupStore.restore(app,backup);reload(app);verify(true,2,0,"restore and cold reload");
        check(!Control.quickMenuVisible(QuickMenuVisibilityPolicy.SELECTOR_BIT),"selector round trip");
        check(Boolean.TRUE.equals(AccountProfiles.preferences(app,"pgrazor-map").getAll().get("Stops")),"preference group restored");
        JSONObject old=copy(backup);old.put("format","PGRazor settings");
        old.getJSONObject("settings").remove("smartAttack");old.getJSONObject("settings").remove("smartShieldTiming");old.getJSONObject("settings").remove("smartFaintedSwapTiming");
        Control.setSmartAttack(false);Control.setSmartShieldTiming(1);Control.setSmartFaintedSwapTiming(2);
        SettingsBackupStore.restore(app,new JSONObject(old.toString()));reload(app);verify(false,1,2,"old file preserves absent keys");
        JSONObject partial=copy(old);partial.getJSONObject("settings").put("smartAttack","true");
        SettingsBackupStore.restore(app,new JSONObject(partial.toString()));reload(app);verify(true,1,2,"partial file preserves absent timing keys");
        JSONObject bad=copy(backup);bad.getJSONObject("settings").put("smartShieldTiming","3");
        bad.getJSONObject("preferences").getJSONObject("pgrazor-map").put("Stops",false);
        try{SettingsBackupStore.restore(app,bad);throw new AssertionError("invalid file accepted");}catch(JSONException expected){}
        reload(app);verify(true,1,2,"invalid file modified settings");
        check(Boolean.TRUE.equals(AccountProfiles.preferences(app,"pgrazor-map").getAll().get("Stops")),"invalid file modified preference group");
        JSONObject again=new JSONObject(SettingsBackupStore.snapshot(app).toString());SettingsBackupRules.validate(again);
        SettingsBackupStore.restore(app,again);reload(app);verify(true,1,2,"re-export and second restore");
        // Installed legacy properties (not old backup JSON, which never contained pvpBattleOptions).
        java.util.Properties legacy=Control.exportSettings();legacy.remove("smartAttack");
        legacy.setProperty("pvpBattleOptions","13");legacy.remove("rocketOptions");
        legacy.setProperty("smartShieldTiming","3");legacy.setProperty("smartFaintedSwapTiming","3");
        try(java.io.FileOutputStream out=new java.io.FileOutputStream(new File(root,"pgrazor-settings-v1.properties"))){legacy.store(out,null);}
        reload(app);check(Control.smartAttack() && Control.rocketOptions()==5 && (Control.pvpBattleOptions() & 7)==5,"legacy PvP bit migration");
        check(Control.smartShieldTiming()==1 && Control.smartFaintedSwapTiming()==1,"LT040 fallback for retired LT039 timing index");
        Control.flushSettings(5000);legacy=Control.exportSettings();legacy.setProperty("smartAttack","false");legacy.setProperty("pvpBattleOptions","13");
        try(java.io.FileOutputStream out=new java.io.FileOutputStream(new File(root,"pgrazor-settings-v1.properties"))){legacy.store(out,null);}
        reload(app);check(!Control.smartAttack() && (Control.pvpBattleOptions() & 8)==0 && Control.rocketOptions()==5,"explicit new choice wins over legacy bit");
        System.out.println("PASS: real backup JSON snapshot, validation, old and partial restore, invalid file and reload");
    }
}
