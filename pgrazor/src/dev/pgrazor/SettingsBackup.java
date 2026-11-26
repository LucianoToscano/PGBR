package dev.pgrazor;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import org.json.*;

/** User-selected, bounded backups. Never enumerates account or game storage. */
final class SettingsBackup {
    private static final int EXPORT=71, IMPORT=72, PGSHARP=73, LIMIT=512*1024;
    private final Activity activity;
    private TextView status;
    private Button export,restore,back,pgsharpImport,pgsharpView;
    private boolean busy;
    private android.app.AlertDialog dialog;
    SettingsBackup(Activity activity){this.activity=activity;}
    void show(){
        LinearLayout root=new LinearLayout(activity);root.setOrientation(LinearLayout.VERTICAL);
        int p=MapTheme.dp(activity,20);root.setPadding(p,p*3,p,p*2);
        TextView title=new TextView(activity);title.setText(dev.pgrazor.PtBr.tr("Settings backup"));title.setTextSize(22);root.addView(title);
        TextView detail=new TextView(activity);detail.setText(dev.pgrazor.PtBr.tr("Save a backup outside the app to keep settings through reinstalls. Includes favorites and their coordinates. Login tokens, current location, cooldown history and active routes are excluded.\n\nImport replaces the included settings and favorites. VGP stays off."));detail.append("\n\nO arquivo pode conter nomes, apelido, favoritos, coordenadas e dados importados. Guarde-o em local privado; o modo de privacidade não anonimiza o backup completo.");detail.setPadding(0,p,0,p);root.addView(detail);
        export=new Button(activity);export.setText(dev.pgrazor.PtBr.tr("Export settings"));root.addView(export);
        restore=new Button(activity);restore.setText(dev.pgrazor.PtBr.tr("Import settings"));root.addView(restore);
        pgsharpImport=new Button(activity);pgsharpImport.setText("Importar dados PGSharp (JSON convertido)");root.addView(pgsharpImport);
        pgsharpView=new Button(activity);pgsharpView.setText("Ver dados PGSharp importados");root.addView(pgsharpView);
        status=new TextView(activity);status.setPadding(0,p,0,p);root.addView(status);
        back=new Button(activity);back.setText(dev.pgrazor.PtBr.tr("Back to game"));back.setOnClickListener(v->{if(dialog!=null)dialog.dismiss();else activity.finish();});root.addView(back);
        export.setOnClickListener(v->pick(EXPORT));restore.setOnClickListener(v->pick(IMPORT));
        pgsharpImport.setOnClickListener(v->pick(PGSHARP));
        pgsharpView.setOnClickListener(v->new android.app.AlertDialog.Builder(MapTheme.wrap(activity))
            .setTitle("Dados PGSharp neste perfil")
            .setMessage(PgSharpDataImport.storedSummary(activity))
            .setNeutralButton("Explorar dados",(d,w)->showImportedCategories())
            .setPositiveButton("OK",null).show());
        ScrollView scroll=new ScrollView(activity);scroll.addView(root);MapTheme.style(scroll);
        if(PgAssets.rooted()){
            detail.append("\n\nA versão Root usa arquivos na pasta de arquivos externos do jogo. Backup comum:\n"+rootedFile()+"\nImportação PGSharp (JSON convertido):\n"+rootedPgSharpFile());
            dialog=new android.app.AlertDialog.Builder(MapTheme.wrap(activity)).setView(scroll).create();
            dialog.setCancelable(false);dialog.show();
        }else activity.setContentView(scroll);
    }
    private void showImportedCategories() {
        JSONObject imported=PgSharpDataImport.stored(activity);
        if(imported==null) {
            Toast.makeText(activity,"Nenhuma importação PGSharp para esta conta.",Toast.LENGTH_LONG).show();
            return;
        }
        final String[] keys={"favorites","routes","pokemonFilters","questFilters","raidFilters"};
        final String[] labels={"Favoritos","Rotas","Filtros Pokémon","Filtros de pesquisas","Filtros de reides"};
        new android.app.AlertDialog.Builder(MapTheme.wrap(activity))
            .setTitle("Categorias PGSharp importadas")
            .setItems(labels,(dialog,selected)->showImportedList(imported,keys[selected],labels[selected]))
            .setNegativeButton("Fechar",null).show();
    }

    private void showImportedList(JSONObject imported,String key,String title) {
        JSONArray rows=imported.optJSONArray(key);
        if(rows==null)return;
        String[] names=new String[rows.length()];
        for(int i=0;i<rows.length();i++){
            JSONObject item=rows.optJSONObject(i);
            names[i]=(i+1)+". "+(item==null?"Registro inválido":item.optString("name","Sem nome"));
        }
        new android.app.AlertDialog.Builder(MapTheme.wrap(activity))
            .setTitle(title+" ("+names.length+")")
            .setItems(names,(d,index)->{
                JSONObject item=rows.optJSONObject(index);
                if(item==null)return;
                new android.app.AlertDialog.Builder(MapTheme.wrap(activity))
                    .setTitle(item.optString("name","Dados originais"))
                    .setMessage(item.toString())
                    .setPositiveButton("Fechar",null).show();
            }).setNegativeButton("Voltar",(d,w)->showImportedCategories()).show();
    }

    private File rootedFile(){File dir=activity.getExternalFilesDir(null);return dir==null?null:new File(dir,"PGBR-settings.json");}
    private File rootedPgSharpFile(){File dir=activity.getExternalFilesDir(null);return dir==null?null:new File(dir,"PGBR_PGSharp_Importacao.json");}
    private void pick(int code){
        if(PgAssets.rooted()){
            File file=code==PGSHARP?rootedPgSharpFile():rootedFile();
            if(file==null){status.setText(dev.pgrazor.PtBr.tr("External storage is unavailable."));return;}
            new android.app.AlertDialog.Builder(MapTheme.wrap(activity))
                .setMessage(dev.pgrazor.PtBr.tr(code==EXPORT?"Replace the settings backup at "+file+"?":code==PGSHARP?"Importar e unir os dados PGSharp de "+file+"? Os filtros serão apenas preservados.":"Replace settings with the backup at "+file+"? VGP will stay off."))
                .setNegativeButton(dev.pgrazor.PtBr.tr("Cancel"),null).setPositiveButton(dev.pgrazor.PtBr.tr("Continue"),(d,w)->result(code,Activity.RESULT_OK,new Intent().setData(Uri.fromFile(file)))).show();
            return;
        }
        Intent i=new Intent(code==EXPORT?Intent.ACTION_CREATE_DOCUMENT:Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("application/json");
        if(code==EXPORT)i.putExtra(Intent.EXTRA_TITLE,"PGBR-settings.json");
        try{activity.startActivityForResult(i,code);}catch(android.content.ActivityNotFoundException e){status.setText(dev.pgrazor.PtBr.tr("No file picker is available."));}
    }
    void result(int request,int result,Intent data){
        if((request!=EXPORT&&request!=IMPORT&&request!=PGSHARP)||result!=Activity.RESULT_OK||data==null||data.getData()==null)return;
        if(busy)return;busy=true;
        Uri uri=data.getData();export.setEnabled(false);restore.setEnabled(false);pgsharpImport.setEnabled(false);pgsharpView.setEnabled(false);back.setEnabled(false);status.setText(request==PGSHARP?"Importando dados PGSharp...":dev.pgrazor.PtBr.tr(request==EXPORT?"Saving backup...":"Restoring backup..."));
        new Thread(()->{
            String message;
            try{
                if(request==EXPORT){
                    byte[] bytes=SettingsBackupStore.snapshot(activity).toString(2).getBytes(StandardCharsets.UTF_8);
                    if(bytes.length>LIMIT)throw new IOException("Backup is too large");
                    try(OutputStream out=activity.getContentResolver().openOutputStream(uri,"wt")){if(out==null)throw new IOException("Cannot open destination");out.write(bytes);}
                    message="Settings exported.";
                }else{
                    JSONObject document;
                    try(InputStream in=activity.getContentResolver().openInputStream(uri)){
                        if(in==null)throw new IOException("Cannot open backup");
                        document=new JSONObject(BackupInput.readJson(in,LIMIT));
                    }
                    if(request==IMPORT){
                        SettingsBackupStore.restore(activity,document);message="Settings restored. Return to the game to use them. VGP is off.";
                    } else {
                        PgSharpDataImport.Report report=PgSharpDataImport.importData(activity,document);
                        message=report.summary();
                    }
                }
            }catch(Exception e){
                message=SettingsBackupStore.rollbackFailed(e)
                    ?"Backup failed and rollback was incomplete. Check your settings and storage before retrying."
                    :BackupInput.safeFailure(e);
            }
            final String text=message;activity.runOnUiThread(()->{busy=false;if(activity.isFinishing()||activity.isDestroyed())return;status.setText(dev.pgrazor.PtBr.tr(text));export.setEnabled(true);restore.setEnabled(true);pgsharpImport.setEnabled(true);pgsharpView.setEnabled(true);back.setEnabled(true);});
        },"pgrazor-settings-backup").start();
    }
    boolean isBusy(){return busy;}
}
