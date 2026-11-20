package dev.pgrazor;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Checks stable public PGBR releases only. Never changes game identity, installs silently
 * or exposes the private upstream input APK.
 */
final class UpdateChecker {
    private static final String API="https://api.github.com/repos/LucianoToscano/PGBR/releases/latest";
    private static final String PREFS="pgbr-update-notifications";
    private static final long CHECK_INTERVAL=6L*60*60*1000;
    private static final long PROMPT_INTERVAL=24L*60*60*1000;
    private static final Handler MAIN=new Handler(Looper.getMainLooper());
    private static final AtomicBoolean RUNNING=new AtomicBoolean();
    private UpdateChecker(){}

    private static SharedPreferences prefs(Context c){
        return c.getApplicationContext().getSharedPreferences(PREFS,Context.MODE_PRIVATE);
    }
    static boolean automaticEnabled(Context c){return prefs(c).getBoolean("automatic",true);}
    static void setAutomaticEnabled(Context c,boolean on){prefs(c).edit().putBoolean("automatic",on).apply();}

    /** Runs after the real game activity has been attached, not during process initialization. */
    static void checkAutomatically(Activity activity){
        if(!automaticEnabled(activity))return;
        MAIN.postDelayed(()->{
            if(!visible(activity))return;
            SharedPreferences p=prefs(activity);
            long now=System.currentTimeMillis(),last=p.getLong("last-check",0);
            if(last>0&&now>=last&&now-last<CHECK_INTERVAL)return;
            if(!RUNNING.compareAndSet(false,true))return;
            p.edit().putLong("last-check",now).apply();
            query(activity,true);
        },2500);
    }

    /** Explicit action is never blocked by the periodic-check interval. */
    static void check(Activity activity){
        if(!RUNNING.compareAndSet(false,true)){
            Toast.makeText(activity,"Verificação de atualização em andamento.",Toast.LENGTH_SHORT).show();return;
        }
        Toast.makeText(activity,"Verificando atualizações do PGBR...",Toast.LENGTH_SHORT).show();
        query(activity,false);
    }

    private static boolean visible(Activity activity){
        return !activity.isFinishing()&&!activity.isDestroyed()&&Control.accountActivity()==activity;
    }
    private static void query(Activity activity,boolean automatic){
        new Thread(()->{
            Release release=null;String failure=null;
            try{release=fetch();}
            catch(Exception problem){failure="Não foi possível consultar atualizações: "+problem.getMessage();}
            final Release found=release;final String error=failure;
            RUNNING.set(false);
            MAIN.post(()->{
                if(!visible(activity))return;
                if(error!=null){if(!automatic)message(activity,error);return;}
                if(!UpdateVersion.isNewer(found.version,BuildVersion.NAME)){
                    if(!automatic)message(activity,"Você já está na versão mais recente publicada.\n\nInstalada: "+BuildVersion.NAME);
                    return;
                }
                if(found.url==null){
                    if(!automatic)message(activity,"Existe uma versão nova, mas o APK sem Root ainda não foi publicado.");
                    return;
                }
                SharedPreferences p=prefs(activity);
                long now=System.currentTimeMillis();
                if(automatic&&found.version.equals(p.getString("prompt-version",""))
                    &&now>=p.getLong("prompt-time",0)
                    &&now-p.getLong("prompt-time",0)<PROMPT_INTERVAL)return;
                showWhenReady(activity,found,automatic,0);
            });
        },"pgbr-updates").start();
    }
    private static void showWhenReady(Activity activity,Release release,boolean automatic,int retry){
        if(!visible(activity))return;
        // Avoid covering other modals during startup. Try again briefly.
        if(automatic&&!activity.hasWindowFocus()){
            if(retry<10)MAIN.postDelayed(()->showWhenReady(activity,release,true,retry+1),1500);
            return;
        }
        if(automatic)prefs(activity).edit().putString("prompt-version",release.version)
            .putLong("prompt-time",System.currentTimeMillis()).apply();
        String notes=release.notes.length()>1400?release.notes.substring(0,1400)+"…":release.notes;
        String text="Instalada: "+BuildVersion.NAME+"\nNova versão: "+release.version+
            "\n\n"+(notes.isEmpty()?"Confira as novidades.":notes)+
            "\n\nO Android solicitará sua confirmação para instalar a atualização.";
        new AlertDialog.Builder(MapTheme.wrap(activity))
            .setTitle(dev.pgrazor.PtBr.tr("Nova atualização do PGBR"))
            .setMessage(dev.pgrazor.PtBr.tr(text))
            .setNegativeButton(dev.pgrazor.PtBr.tr("Depois"),null)
            .setPositiveButton(dev.pgrazor.PtBr.tr("Baixar atualização"),(dialog,which)->{
                try{activity.startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(release.url)));}
                catch(Exception problem){Toast.makeText(activity,"Não foi possível abrir o download.",Toast.LENGTH_LONG).show();}
            }).show();
    }
    private static void message(Activity a,String text){
        new AlertDialog.Builder(MapTheme.wrap(a)).setTitle(dev.pgrazor.PtBr.tr("Atualizações do PGBR"))
            .setMessage(dev.pgrazor.PtBr.tr(text)).setPositiveButton(dev.pgrazor.PtBr.tr("Entendi"),null).show();
    }
    private static Release fetch() throws Exception{
        HttpURLConnection c=null;
        try{
            c=(HttpURLConnection)new URL(API).openConnection();
            c.setConnectTimeout(8000);c.setReadTimeout(8000);
            c.setRequestProperty("Accept","application/vnd.github+json");
            c.setRequestProperty("User-Agent","PGBR-Android-UpdateChecker");
            int status=c.getResponseCode();
            if(status==404)throw new IOException("Nenhuma versão pública disponível no GitHub Releases.");
            if(status!=200)throw new IOException("Erro HTTP "+status);
            ByteArrayOutputStream out=new ByteArrayOutputStream();
            try(InputStream in=c.getInputStream()){
                byte[] buffer=new byte[4096];int n;
                while((n=in.read(buffer))!=-1){
                    if(out.size()+n>128*1024)throw new IOException("Resposta muito grande");
                    out.write(buffer,0,n);
                }
            }
            JSONObject root=new JSONObject(out.toString("UTF-8"));
            if(root.optBoolean("draft")||root.optBoolean("prerelease"))
                throw new IOException("Nenhuma atualização estável publicada.");
            String version=root.optString("tag_name","");
            if(!UpdateVersion.valid(version))throw new IOException("Versão pública inválida.");
            String notes=root.optString("body","");
            JSONArray assets=root.optJSONArray("assets");
            String url=null;
            if(assets!=null)for(int i=0;i<assets.length();i++){
                JSONObject asset=assets.optJSONObject(i);if(asset==null)continue;
                String name=asset.optString("name",""),candidate=asset.optString("browser_download_url","");
                if(name.startsWith("PGBR-")&&name.endsWith(".apk")
                    &&!name.toLowerCase(Locale.ROOT).contains("root")
                    &&candidate.startsWith("https://github.com/LucianoToscano/PGBR/releases/download/")){
                    url=candidate;break;
                }
            }
            return new Release(version,notes,url);
        }finally{if(c!=null)c.disconnect();}
    }
    private static final class Release{
        final String version,notes,url;
        Release(String version,String notes,String url){this.version=version;this.notes=notes;this.url=url;}
    }
}
