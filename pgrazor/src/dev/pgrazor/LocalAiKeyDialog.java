package dev.pgrazor;

import android.app.Activity;
import android.app.AlertDialog;
import android.widget.*;
import java.util.Arrays;

/** Explicit local save/remove; no provider requests or secret display. */
final class LocalAiKeyDialog {
    static void show(Activity a){
        if(!LocalAiKeyVault.allowed(a))return;
        LinearLayout b=LocalAuditDialog.body(a);
        LocalAuditDialog.text(a,b,"Cofre local da prévia independente. Não envia chaves nem consulta IA. A conexão e cobrança do provedor ainda não foram verificadas. Não use uma chave em um cliente modificado.");
        for(LocalAiKeyEnvelope.Provider provider:LocalAiKeyEnvelope.Provider.values()){
            LocalAuditDialog.text(a,b,provider==LocalAiKeyEnvelope.Provider.GEMINI?"Gemini":"Groq");
            EditText field=LocalAuditDialog.input(a,b,"Sua chave de API","");
            field.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
            final boolean[] rejected={false};
            field.setFilters(new android.text.InputFilter[]{(src,start,end,dst,ds,de)->{
                if((long)dst.length()-(de-ds)+(end-start)>LocalAiKeyEnvelope.MAX_KEY){
                    rejected[0]=true;field.setError("Chave longa demais. Substitua todo o conteúdo.");return dst.subSequence(ds,de);
                }
                if(ds==0&&de==dst.length()){rejected[0]=false;field.setError(null);}
                return null;
            }});
            LocalAuditDialog.privateInput(field);
            TextView status=LocalAuditDialog.text(a,b,"Verificando armazenamento local…");
            Button save=new Button(MapTheme.wrap(a)),remove=new Button(MapTheme.wrap(a));
            save.setText("Salvar chave localmente");remove.setText("Remover chave");
            MapTheme.setButtonBackground(save);MapTheme.setButtonBackground(remove);
            b.addView(save);b.addView(remove);save.setEnabled(false);remove.setEnabled(false);
            android.content.Context app=a.getApplicationContext();
            Runnable refresh=()->{
                String message;
                try{message=LocalAiKeyVault.configured(app,provider)?"Chave armazenada e legível • não validada pelo provedor":"Nenhuma chave armazenada";}
                catch(Exception error){message="Cofre indisponível ou inválido.";}
                final String result=message;
                a.runOnUiThread(()->{if(!a.isDestroyed()&&b.isAttachedToWindow()){status.setText(result);save.setEnabled(true);remove.setEnabled(true);}});
            };
            save.setOnClickListener(v->{
                if(rejected[0]){field.setError("Colagem rejeitada. Substitua toda a chave antes de salvar.");return;}
                android.text.Editable value=field.getText();char[] secret=new char[value.length()];value.getChars(0,value.length(),secret,0);value.clear();
                save.setEnabled(false);remove.setEnabled(false);
                new Thread(()->{
                    try{LocalAiKeyVault.save(app,provider,secret);refresh.run();}
                    catch(Exception e){a.runOnUiThread(()->{if(b.isAttachedToWindow()){status.setText("Não foi possível salvar a chave.");save.setEnabled(true);remove.setEnabled(true);}});}
                    finally{Arrays.fill(secret,'\0');}
                },"pgbr-ai-local-save").start();
            });
            remove.setOnClickListener(v->new AlertDialog.Builder(MapTheme.wrap(a)).setTitle("Remover chave local?")
                .setMessage("A chave deixará de estar disponível neste cofre. Isso não a revoga no provedor.")
                .setNegativeButton("Cancelar",null).setPositiveButton("Remover",(dialog,which)->{
                    field.getText().clear();save.setEnabled(false);remove.setEnabled(false);
                    new Thread(()->{try{LocalAiKeyVault.remove(app,provider);refresh.run();}catch(Exception e){a.runOnUiThread(()->{if(b.isAttachedToWindow()){status.setText("Não foi possível remover a chave.");save.setEnabled(true);remove.setEnabled(true);}});}},"pgbr-ai-local-remove").start();
                }).show());
            b.addOnAttachStateChangeListener(new android.view.View.OnAttachStateChangeListener(){
                public void onViewAttachedToWindow(android.view.View view){new Thread(refresh,"pgbr-ai-local-status").start();}
                public void onViewDetachedFromWindow(android.view.View view){field.getText().clear();}
            });
        }
        LocalAuditDialog.presentPrivate(a,new AlertDialog.Builder(MapTheme.wrap(a)).setTitle("Chaves individuais de IA").setView(LocalAuditDialog.scroll(a,b)).setPositiveButton("Fechar",null).create());
    }
}
