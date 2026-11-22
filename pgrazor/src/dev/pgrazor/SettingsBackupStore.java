package dev.pgrazor;

import android.content.Context;
import android.content.SharedPreferences;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import org.json.JSONObject;

/** Production backup serialization and restoration, separate from Android picker UI. */
final class SettingsBackupStore {
    private static final String[] SCOPES={"pgrazor-map","pgrazor-overlays","pgrazor-saved-places-v1","pgrazor-livefeed"};
    private static final String[][] KEYS={{"Stops","Gyms","Raids"},{"cooldown.fx","cooldown.fy","vgp.fx","vgp.fy","transfer.fx","transfer.fy","gym-feed.fx","gym-feed.fy","local-time.fx","local-time.fy"},{"places"},{"category","radius","minIv","myShinies","filters_v1","filter_presets_v1"}};
    static synchronized JSONObject snapshot(Context context)throws Exception{
        JSONObject result=new JSONObject();result.put("format","PGBR settings");result.put("version",1);result.put("speedKmh",Control.engine.speedKmh);
        JSONObject settings=new JSONObject();Properties p=Control.exportSettings();
        for(String key:SettingsBackupRules.SETTINGS)if(p.containsKey(key))settings.put(key,p.getProperty(key));result.put("settings",settings);
        JSONObject prefs=new JSONObject();for(int i=0;i<SCOPES.length;i++){
            Map<String,?> all=AccountProfiles.preferences(context,SCOPES[i]).getAll();JSONObject group=new JSONObject();
            for(String key:KEYS[i])if(all.containsKey(key))group.put(key,all.get(key));prefs.put(SCOPES[i],group);
        }result.put("preferences",prefs);SettingsBackupRules.validate(result);return result;
    }
    private static void writeGroup(Context c,String scope,JSONObject data,String[] keys)throws Exception{
        SharedPreferences.Editor edit=AccountProfiles.preferences(c,scope).edit();for(String key:keys)edit.remove(key);
        for(String key:keys)if(data.has(key)){
            Object value=data.get(key);
            if(value instanceof Boolean)edit.putBoolean(key,(Boolean)value);
            else if(value instanceof Number){if(scope.equals("pgrazor-overlays"))edit.putFloat(key,((Number)value).floatValue());else edit.putInt(key,((Number)value).intValue());}
            else edit.putString(key,(String)value);
        }if(!edit.commit())throw new IOException("Could not save settings");
    }
    static synchronized void restore(Context context,JSONObject document)throws Exception{
        SettingsBackupRules.validate(document);JSONObject previous=snapshot(context),prefs=document.getJSONObject("preferences");
        Properties props=new Properties();JSONObject settings=document.getJSONObject("settings");for(String key:SettingsBackupRules.SETTINGS)if(settings.has(key))props.setProperty(key,settings.getString(key));
        try{
            for(int i=0;i<SCOPES.length;i++)if(prefs.has(SCOPES[i]))writeGroup(context,SCOPES[i],prefs.getJSONObject(SCOPES[i]),KEYS[i]);
            Control.importSettings((android.app.Application)context.getApplicationContext(),props,document.getDouble("speedKmh"));
        }catch(Exception error){
            JSONObject old=previous.getJSONObject("preferences");for(int i=0;i<SCOPES.length;i++)try{writeGroup(context,SCOPES[i],old.getJSONObject(SCOPES[i]),KEYS[i]);}catch(Exception rollback){error.addSuppressed(rollback);}
            throw error;
        }
    }
}
