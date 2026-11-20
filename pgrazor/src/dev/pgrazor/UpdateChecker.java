package dev.pgrazor;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Read-only release discovery. Never installs an APK silently and never changes game data.
 * Only public, non-prerelease PGBR APK assets are offered; draft input assets are excluded.
 */
final class UpdateChecker {
    private static final String RELEASE_API =
        "https://api.github.com/repos/LucianoToscano/PGBR/releases/latest";
    private UpdateChecker() {}

    static void check(Activity activity) {
        Toast.makeText(activity, "Verificando atualizações do PGBR...", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            String message = null, version = null, notes = null, link = null;
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection)new URL(RELEASE_API).openConnection();
                connection.setConnectTimeout(8000);
                connection.setReadTimeout(8000);
                connection.setRequestProperty("Accept", "application/vnd.github+json");
                connection.setRequestProperty("User-Agent", "PGBR-Android-UpdateChecker");
                if(connection.getResponseCode()!=200)throw new java.io.IOException("Nenhuma versão pública disponível");
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                try(InputStream in=connection.getInputStream()){
                    byte[] buffer=new byte[4096];int n;
                    while((n=in.read(buffer))!=-1){
                        if(out.size()+n>128*1024)throw new java.io.IOException("Resposta muito grande");
                        out.write(buffer,0,n);
                    }
                }
                JSONObject release=new JSONObject(out.toString("UTF-8"));
                if(release.optBoolean("draft")||release.optBoolean("prerelease"))
                    throw new java.io.IOException("Nenhuma versão estável disponível");
                version=release.optString("tag_name","");
                notes=release.optString("body","");
                JSONArray assets=release.optJSONArray("assets");
                if(assets!=null)for(int i=0;i<assets.length();i++){
                    JSONObject asset=assets.optJSONObject(i);
                    if(asset==null)continue;
                    String name=asset.optString("name","");
                    if(name.startsWith("PGBR-")&&name.endsWith(".apk")&&!name.toLowerCase(java.util.Locale.ROOT).contains("root")){
                        link=asset.optString("browser_download_url","");
                        if(link.startsWith("https://github.com/"))break;
                        link=null;
                    }
                }
                if(link==null||link.isEmpty())message="A versão publicada ainda não possui um APK sem Root para atualização direta.";
            }catch(Exception e){message="Não foi possível consultar atualizações: "+e.getMessage();}
            finally{if(connection!=null)connection.disconnect();}
            final String status=message,tag=version,body=notes,url=link;
            new Handler(Looper.getMainLooper()).post(() -> {
                if(activity.isFinishing()||activity.isDestroyed())return;
                if(status!=null){
                    new AlertDialog.Builder(activity).setTitle("Atualizações do PGBR")
                        .setMessage(status).setPositiveButton("Entendi",null).show();
                    return;
                }
                String text="Instalado: "+BuildVersion.NAME+"\nDisponível: "+tag+
                    "\n\n"+(tag.equals(BuildVersion.NAME)?"Você já possui esta versão publicada.":"Verifique as novidades antes de atualizar.")+
                    (body==null||body.isEmpty()?"":"\n\n"+body.substring(0,Math.min(1800,body.length())))+
                    "\n\nA instalação exige confirmação do Android e a mesma assinatura digital.";
                new AlertDialog.Builder(activity).setTitle("Atualizações do PGBR")
                    .setMessage(text)
                    .setNegativeButton("Fechar",null)
                    .setPositiveButton("Abrir download", (d,w) -> {
                        try{activity.startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(url)));}
                        catch(Exception error){Toast.makeText(activity,"Não foi possível abrir o download",Toast.LENGTH_LONG).show();}
                    }).show();
            });
        },"pgbr-update-check").start();
    }
}
