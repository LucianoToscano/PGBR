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
            save.setText("Salvar chave localmente");remove.setText("Remover chave");save.setAllCaps(false);remove.setAllCaps(false);
            MapTheme.setButtonBackground(save);MapTheme.setButtonBackground(remove);
            b.addView(save);b.addView(remove);
            final boolean[] busy={false};
            java.util.function.Consumer<Boolean> enabled=on->{field.setEnabled(on);save.setEnabled(on);remove.setEnabled(on);};
            enabled.accept(false);
            android.content.Context app=a.getApplicationContext();
            java.util.function.Supplier<String> statusText=()->{
                try{return LocalAiKeyVault.configured(app,provider)?"Chave armazenada e legível • não validada pelo provedor":"Nenhuma chave armazenada";}
                catch(Exception error){return "Cofre indisponível ou inválido.";}
            };
            java.util.function.Consumer<String> complete=message->a.runOnUiThread(()->{
                busy[0]=false;
                if(!a.isDestroyed()&&!a.isFinishing()&&b.isAttachedToWindow()){
                    status.setText(message);enabled.accept(true);
                }
            });
            Runnable refresh=()->{
                if(busy[0])return;
                busy[0]=true;enabled.accept(false);
                new Thread(()->complete.accept(statusText.get()),"pgbr-ai-local-status").start();
            };
            save.setOnClickListener(v->{
                if(busy[0])return;
                if(rejected[0]){field.setError("Colagem rejeitada. Substitua toda a chave antes de salvar.");return;}
                android.text.Editable value=field.getText();char[] secret=new char[value.length()];value.getChars(0,value.length(),secret,0);value.clear();
                busy[0]=true;enabled.accept(false);status.setText("Salvando no cofre local…");
                new Thread(()->{
                    String message;
                    try{LocalAiKeyVault.save(app,provider,secret);message=statusText.get();}
                    catch(Exception e){message="Não foi possível salvar a chave.";}
                    finally{Arrays.fill(secret,'\0');}
                    complete.accept(message);
                },"pgbr-ai-local-save").start();
            });
            remove.setOnClickListener(v->{
                if(busy[0])return;
                AlertDialog confirm=new AlertDialog.Builder(MapTheme.wrap(a)).setTitle("Remover chave local?")
                    .setMessage("A chave deixará de estar disponível neste cofre. Isso não a revoga no provedor.")
                    .setNegativeButton("Cancelar",null).setPositiveButton("Remover",(dialog,which)->{
                        if(busy[0]||a.isDestroyed()||a.isFinishing()||!b.isAttachedToWindow())return;
                        field.getText().clear();busy[0]=true;enabled.accept(false);status.setText("Removendo do cofre local…");
                        new Thread(()->{
                            String message;
                            try{LocalAiKeyVault.remove(app,provider);message=statusText.get();}
                            catch(Exception e){message="Não foi possível remover a chave.";}
                            complete.accept(message);
                        },"pgbr-ai-local-remove").start();
                    }).create();
                LocalAuditDialog.presentPrivate(a,confirm);
            });
            b.addOnAttachStateChangeListener(new android.view.View.OnAttachStateChangeListener(){
                public void onViewAttachedToWindow(android.view.View view){refresh.run();}
                public void onViewDetachedFromWindow(android.view.View view){field.getText().clear();}
            });
        }
        LocalAuditDialog.presentPrivate(a,new AlertDialog.Builder(MapTheme.wrap(a)).setTitle("Chaves individuais de IA").setView(LocalAuditDialog.scroll(a,b)).setPositiveButton("Fechar",null).create());
    }
}
