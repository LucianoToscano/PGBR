package dev.pgrazor;
import android.app.Activity;
import android.app.AlertDialog;
import android.widget.*;
import java.util.*;
/** Ephemeral manual entry; no account reads or game actions. */
final class LocalAuditDialog {
 private LocalAuditDialog(){}
 static void show(Activity a){
  AlertDialog d=new AlertDialog.Builder(MapTheme.wrap(a)).setTitle("Análise local • sem ações no jogo")
   .setItems(new String[]{"Prévia de proteção da coleção","Simular ataques carregados","Interpretar código de diagnóstico","Avaliar lista de Pokémon","Planejar energia PvP","Interpretar diagnóstico PvP","Planejar alternativas de frutas","Preparar equipe Max (manual)","Resumir diagnóstico filtrado","Conferir etiquetas (manual)","Conferir ícones instalados","Planejar Ovo da Sorte e Pedaço de Estrela"},(x,i)->{if(i==0)collection(a);else if(i==1)combat(a);else if(i==2)reason(a);else if(i==3)batch(a);else if(i==4)energyPlan(a);else if(i==5)battleDiagnostic(a);else if(i==6)berryPlan(a);else if(i==7)maxPlan(a);else if(i==8)summarize(a);else if(i==9)tagReview(a);else if(i==10)itemGallery(a);else boostPlan(a);})
   .setNegativeButton("Fechar",null).create();present(a,d);
 }
 static void present(Activity a,AlertDialog d){DialogLifecycle.bind(a,d,null);d.setOnShowListener(x->MapTheme.styleButtons(d));d.show();}
 // Protect only the manual collection/diagnostic windows; the public icon gallery remains capturable.
 static void presentPrivate(Activity a,AlertDialog d){
  android.view.Window w=d.getWindow();if(w!=null)w.addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE);
  present(a,d);
 }
 static void privateInput(EditText e){
  e.setSaveEnabled(false);
  if(android.os.Build.VERSION.SDK_INT>=26){
   e.setImportantForAutofill(android.view.View.IMPORTANT_FOR_AUTOFILL_NO);
   e.setImeOptions(e.getImeOptions()|android.view.inputmethod.EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING);
  }
 }
 static String evaluationTime(){return new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss Z",Locale.forLanguageTag("pt-BR")).format(new java.util.Date());}
 static LinearLayout body(Activity a){LinearLayout b=new LinearLayout(MapTheme.wrap(a));b.setOrientation(LinearLayout.VERTICAL);int p=MapTheme.dp(a,16);b.setPadding(p,p,p,p);return b;}
 static ScrollView scroll(Activity a,LinearLayout b){ScrollView s=new ScrollView(a);s.addView(b);return s;}
 static TextView text(Activity a,LinearLayout b,String s){TextView t=new TextView(MapTheme.wrap(a));t.setText(s);b.addView(t);return t;}
 static EditText input(Activity a,LinearLayout b,String title,String initial){TextView label=text(a,b,title);EditText e=new EditText(MapTheme.wrap(a));e.setId(android.view.View.generateViewId());label.setLabelFor(e.getId());e.setSingleLine(true);e.setText(initial);privateInput(e);b.addView(e);return e;}
 static EditText numeric(Activity a,LinearLayout b,String title,String initial,boolean decimal,boolean signed){EditText e=input(a,b,title,initial);e.setInputType(android.text.InputType.TYPE_CLASS_NUMBER|(decimal?android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL:0)|(signed?android.text.InputType.TYPE_NUMBER_FLAG_SIGNED:0));e.setSingleLine(true);privateInput(e);return e;}
 static Spinner select(Activity a,LinearLayout b,String title,String[] values){TextView label=text(a,b,title);Spinner s=new Spinner(MapTheme.wrap(a));s.setId(android.view.View.generateViewId());label.setLabelFor(s.getId());ArrayAdapter<String> ad=new ArrayAdapter<>(MapTheme.wrap(a),android.R.layout.simple_spinner_item,values);ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);s.setAdapter(ad);b.addView(s);return s;}
 static Button action(Activity a,LinearLayout b,String title,Runnable f){Button v=new Button(MapTheme.wrap(a));v.setText(title);MapTheme.setButtonBackground(v);v.setAllCaps(false);v.setOnClickListener(x->f.run());b.addView(v);return v;}
 static void invalidateOnEdit(TextView result,EditText... fields){
  android.text.TextWatcher w=new android.text.TextWatcher(){
   public void beforeTextChanged(CharSequence s,int start,int count,int after){}
   public void onTextChanged(CharSequence s,int start,int before,int count){result.setText("Dados alterados. Calcule novamente.");}
   public void afterTextChanged(android.text.Editable e){}
  };
  for(EditText f:fields)f.addTextChangedListener(w);
 }
 static void invalidateOnSelection(TextView result,Spinner... fields){
  for(Spinner f:fields)f.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
   public void onItemSelected(AdapterView<?> p,android.view.View v,int position,long id){result.setText("Dados alterados. Calcule novamente.");}
   public void onNothingSelected(AdapterView<?> p){result.setText("Seleção não confirmada. Calcule novamente.");}
  });
 }
 static int integer(EditText e){return Integer.parseInt(e.getText().toString().trim());}
 static double decimal(EditText e){return Double.parseDouble(e.getText().toString().trim().replace(',','.'));}
 static void collection(Activity a){
  LinearLayout b=body(a);text(a,b,"Informe dados verificados. Desconhecido protege por precaução. Nenhuma transferência será executada. A captura desta janela é bloqueada pelo Android; não é proteção das telas do jogo.");
  EditText name=input(a,b,"Espécie",""),cp=numeric(a,b,"PC","",false,false),atk=numeric(a,b,"IV ataque (0–15 ou -1 desconhecido)","-1",false,true),def=numeric(a,b,"IV defesa","-1",false,true),sta=numeric(a,b,"IV PS","-1",false,true),limit=numeric(a,b,"IV mínimo para preservar (0–100; 101 desativa só o limite de IV)","99",false,false);
  EditText names=input(a,b,"Espécies adicionais protegidas (nomes exatos separados por vírgulas)","");
  text(a,b,"Lista adicional, com os mesmos critérios da prévia em lote. Formas diferentes precisam de nomes próprios.");
  LocalAuditPolicy.Flag[] flags=LocalAuditPolicy.Flag.values();Spinner[] observed=new Spinner[flags.length];CheckBox[] active=new CheckBox[flags.length];
  for(int i=0;i<flags.length;i++){CheckBox c=new CheckBox(MapTheme.wrap(a));c.setText("Proteger: "+flags[i].label);c.setChecked(true);b.addView(c);active[i]=c;observed[i]=select(a,b,"Dado observado",new String[]{"Desconhecido","Sim","Não"});}
  TextView result=text(a,b,"");invalidateOnEdit(result,name,cp,atk,def,sta,limit,names);invalidateOnSelection(result,observed);
  for(CheckBox c:active)c.setOnCheckedChangeListener((v,on)->result.setText("Critérios alterados. Avalie novamente."));
  action(a,b,"Avaliar sem transferir",()->{try{
   int pc=integer(cp),aa=integer(atk),dd=integer(def),ss=integer(sta);if(pc<0||pc>100000||aa< -1||aa>15||dd< -1||dd>15||ss< -1||ss>15||name.getText().toString().trim().isEmpty())throw new IllegalArgumentException();
   LocalAuditPolicy.speciesKey(name.getText().toString());
   EnumSet<LocalAuditPolicy.Flag> selected=EnumSet.noneOf(LocalAuditPolicy.Flag.class);EnumMap<LocalAuditPolicy.Flag,Boolean> values=new EnumMap<>(LocalAuditPolicy.Flag.class);
   for(int i=0;i<flags.length;i++){if(active[i].isChecked())selected.add(flags[i]);int x=observed[i].getSelectedItemPosition();if(x==1||x==2)values.put(flags[i],x==1);}
   LocalAuditPolicy.Decision decision=LocalAuditPolicy.withSpeciesProtection(LocalAuditPolicy.preview(aa,dd,ss,integer(limit),selected,values),name.getText().toString().trim(),LocalAuditPolicy.protectedNames(names.getText().toString()));
   String state=decision.status==LocalAuditPolicy.Status.PROTECTED?"Protegido":decision.status==LocalAuditPolicy.Status.UNCERTAIN?"Protegido por incerteza":"Corresponde ao filtro";
   String iv=LocalAuditPolicy.validIv(aa)&&LocalAuditPolicy.validIv(dd)&&LocalAuditPolicy.validIv(ss)?String.format(Locale.forLanguageTag("pt-BR"),"%.2f%%",(aa+dd+ss)*100.0/45):"Desconhecido";
   result.setText(name.getText()+" • PC "+pc+" • IV "+iv+"\n"+state+"\n"+String.join("\n",decision.reasons)+"\n"+evaluationTime()+" • PGBR "+BuildVersion.NAME+" • esquema "+LocalAuditPolicy.SCHEMA);
  }catch(RuntimeException invalid){result.setText("Confira os campos e os limites.");}});
  presentPrivate(a,new AlertDialog.Builder(MapTheme.wrap(a)).setTitle("Prévia local de filtros").setView(scroll(a,b)).setPositiveButton("Fechar",null).create());
 }
 static void batch(Activity a){
  LinearLayout b=body(a);
  text(a,b,"Cole até 500 registros de uma fonte autorizada, um por linha. Formato: espécie;PC;IV ataque;IV defesa;IV PS;critérios. Use ? para desconhecido; critérios no formato EVENT=1,SHINY=0,DYNAMAX=?,GIGANTAMAX=?. Ausência de informação não significa falso. Não lê a coleção nem transfere Pokémon.");
  EditText limit=numeric(a,b,"IV mínimo para preservar (0–100; 101 desativa só o limite de IV)","99",false,false);
  EditText protectedNames=input(a,b,"Espécies adicionais protegidas (nomes exatos separados por vírgulas)","");
  text(a,b,"A lista é adicional aos "+LocalAuditPolicy.Flag.values().length+" critérios; formas diferentes precisam de nomes próprios. Não usa dados do jogo.");
  EditText records=new EditText(MapTheme.wrap(a));records.setMinLines(4);records.setMaxLines(8);records.setHint("Rattata;478;4;4;5;EVENT=1,SHINY=0");privateInput(records);b.addView(records);
  LocalAuditPolicy.Flag[] flags=LocalAuditPolicy.Flag.values();CheckBox[] active=new CheckBox[flags.length];
  for(int i=0;i<flags.length;i++){CheckBox c=new CheckBox(MapTheme.wrap(a));c.setText("Proteger "+flags[i].label+" ("+flags[i].name()+")");c.setChecked(true);b.addView(c);active[i]=c;}
  Spinner filter=select(a,b,"Mostrar resultados",new String[]{"Todos","Protegidos","Protegidos por incerteza","Correspondem ao filtro"});
  TextView result=text(a,b,"");
  LocalCollectionPreview.Report[] report={null};int[] page={0};String[] evaluatedAt={""};final boolean[] oversized={false};
  final Thread[] worker={null};final long[] generation={0};
  records.setFilters(new android.text.InputFilter[]{(incoming,start,end,dest,ds,de)->{
   if((long)dest.length()-(de-ds)+(end-start)>LocalCollectionPreview.MAX_CHARS){oversized[0]=true;generation[0]++;report[0]=null;page[0]=0;result.setText("Colagem rejeitada; nenhuma avaliação atual disponível.");records.setError("Lista grande demais. Use lotes menores.");return dest.subSequence(ds,de);}
   if(ds==0&&de==dest.length()){oversized[0]=false;records.setError(null);}return null;
  }});
  java.util.function.Supplier<LocalAuditPolicy.Status> status=()->filter.getSelectedItemPosition()<=0?null:LocalAuditPolicy.Status.values()[filter.getSelectedItemPosition()-1];
  Runnable render=()->{
   if(worker[0]!=null)return;
   if(report[0]==null){result.setText(oversized[0]?"Colagem rejeitada. Substitua toda a lista.":"Avalie a lista para obter os resultados.");return;}
   LocalCollectionPreview.Report r=report[0];int pages=LocalCollectionPreview.pageCount(r,status.get());page[0]=Math.max(0,Math.min(page[0],pages-1));
   StringBuilder out=new StringBuilder("Protegidos: "+r.protectedCount+" | Incertos: "+r.uncertainCount+" | Correspondem: "+r.matchCount+" | Linhas inválidas: "+r.rejectedLines.size()+"\n");
   if(!r.rejectedLines.isEmpty())out.append("Avaliação parcial: há registros inválidos que não foram classificados. Corrija-os antes de usar esta prévia.\n");
   if(!r.rejectedLines.isEmpty())out.append("Linhas inválidas (não classificadas): ").append(r.rejectedLines).append('\n');
   out.append("Página ").append(page[0]+1).append('/').append(pages).append(" • ").append(evaluatedAt[0]).append(" • PGBR ").append(BuildVersion.NAME).append(" • esquema ").append(LocalAuditPolicy.SCHEMA).append("\n\n");
   List<LocalCollectionPreview.Row> rows=LocalCollectionPreview.page(r,status.get(),page[0]);
   if(rows.isEmpty())out.append("Nenhum registro nesta categoria.");
   for(LocalCollectionPreview.Row row:rows){
    double iv=row.ivPercent();
    out.append("Linha ").append(row.sourceLine).append(" • ").append(row.species).append(" • PC ").append(row.cp).append(" • IV ").append(Double.isFinite(iv)?String.format(Locale.forLanguageTag("pt-BR"),"%.2f%%",iv):"Desconhecido").append(" • ").append(row.decision.status==LocalAuditPolicy.Status.PROTECTED?"Protegido":row.decision.status==LocalAuditPolicy.Status.UNCERTAIN?"Protegido por incerteza":"Corresponde ao filtro").append('\n').append(String.join(", ",row.decision.reasons)).append("\n\n");
   }
   result.setText(out.toString());
  };
  Runnable invalidate=()->{generation[0]++;report[0]=null;page[0]=0;result.setText("Dados ou filtros alterados. Avalie novamente.");};
  android.text.TextWatcher watcher=new android.text.TextWatcher(){
   public void beforeTextChanged(CharSequence s,int start,int count,int after){}
   public void onTextChanged(CharSequence s,int start,int before,int count){invalidate.run();}
   public void afterTextChanged(android.text.Editable e){}
  };
  records.addTextChangedListener(watcher);limit.addTextChangedListener(watcher);protectedNames.addTextChangedListener(watcher);
  for(CheckBox c:active)c.setOnCheckedChangeListener((v,on)->invalidate.run());
  filter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
   public void onItemSelected(AdapterView<?> parent,android.view.View v,int position,long id){page[0]=0;render.run();}
   public void onNothingSelected(AdapterView<?> parent){}
  });
  Button evaluate=new Button(MapTheme.wrap(a));evaluate.setText("Avaliar lista sem transferir");evaluate.setAllCaps(false);MapTheme.setButtonBackground(evaluate);b.addView(evaluate);
  java.util.function.Consumer<Boolean> inputEnabled=enabled->{
   records.setEnabled(enabled);limit.setEnabled(enabled);protectedNames.setEnabled(enabled);
   for(CheckBox c:active)c.setEnabled(enabled);evaluate.setEnabled(enabled);
  };
  b.addOnAttachStateChangeListener(new android.view.View.OnAttachStateChangeListener(){
   public void onViewAttachedToWindow(android.view.View v){}
   public void onViewDetachedFromWindow(android.view.View v){generation[0]++;if(worker[0]!=null)worker[0].interrupt();worker[0]=null;report[0]=null;}
  });
  evaluate.setOnClickListener(v->{
   if(worker[0]!=null)return;
   report[0]=null;page[0]=0;
   if(oversized[0]||records.length()>LocalCollectionPreview.MAX_CHARS){result.setText("Colagem rejeitada por tamanho. Substitua todo o conteúdo por um lote completo menor; nenhum registro foi avaliado.");return;}
   final int threshold;
   final Set<String> names;
   try{threshold=integer(limit);names=LocalAuditPolicy.protectedNames(protectedNames.getText().toString());}
   catch(IllegalArgumentException invalid){result.setText("Confira os limites e o formato da lista.");return;}
   final EnumSet<LocalAuditPolicy.Flag> selected=EnumSet.noneOf(LocalAuditPolicy.Flag.class);for(int i=0;i<flags.length;i++)if(active[i].isChecked())selected.add(flags[i]);
   final String snapshot=records.getText().toString();final long revision=++generation[0];
   inputEnabled.accept(false);result.setText("Analisando coleção local…");
   Thread task=new Thread(()->{
    LocalCollectionPreview.Report parsed=null;
    try{parsed=LocalCollectionPreview.parse(snapshot,threshold,selected,names);}
    catch(java.util.concurrent.CancellationException canceled){return;}
    catch(RuntimeException invalid){/* Fixed error message, no supplied data. */}
    final LocalCollectionPreview.Report completed=parsed;final Thread current=Thread.currentThread();
    a.runOnUiThread(()->{
     if(worker[0]!=current)return;worker[0]=null;
     if(a.isDestroyed()||!b.isAttachedToWindow())return;
     inputEnabled.accept(true);
     if(revision!=generation[0]||oversized[0]||!snapshot.contentEquals(records.getText())){result.setText("Dados ou filtros alterados. Avalie novamente.");return;}
     if(completed==null){result.setText("Confira os limites e o formato da lista.");return;}
     report[0]=completed;evaluatedAt[0]=evaluationTime();render.run();
    });
   },"pgbr-local-collection");worker[0]=task;task.start();
  });
  action(a,b,"Consultar motivos das linhas inválidas",()->{
   if(worker[0]!=null)return;
   if(report[0]==null){result.setText("Avalie uma lista válida primeiro.");return;}
   StringBuilder errors=new StringBuilder();
   for(Map.Entry<Integer,String> entry:report[0].rejectedReasons.entrySet())errors.append("Linha ").append(entry.getKey()).append(": ").append(entry.getValue()).append('\n');
   if(errors.length()==0)errors.append("Nenhuma linha inválida.");
   LinearLayout detail=body(a);text(a,detail,errors.toString());
   presentPrivate(a,new AlertDialog.Builder(MapTheme.wrap(a)).setTitle("Diagnóstico da importação").setView(scroll(a,detail)).setPositiveButton("Fechar",null).create());
  });
  action(a,b,"Página anterior",()->{if(report[0]!=null&&page[0]>0){page[0]--;render.run();}});
  action(a,b,"Próxima página",()->{if(report[0]!=null&&page[0]+1<LocalCollectionPreview.pageCount(report[0],status.get())){page[0]++;render.run();}});
  presentPrivate(a,new AlertDialog.Builder(MapTheme.wrap(a)).setTitle("Prévia de coleção em lote").setView(scroll(a,b)).setPositiveButton("Fechar",null).create());
 }
 static void reason(Activity a){LinearLayout b=body(a);EditText code=numeric(a,b,"Código do diagnóstico (reasonCode)","12",false,false);TextView r=text(a,b,"");invalidateOnEdit(r,code);action(a,b,"Interpretar",()->{try{r.setText(LocalAuditPolicy.reason(integer(code)));}catch(RuntimeException e){r.setText("Código inválido.");}});present(a,new AlertDialog.Builder(MapTheme.wrap(a)).setTitle("Motivos de proteção").setView(scroll(a,b)).setPositiveButton("Fechar",null).create());}
 static final int[] GALLERY_IDS={1,2,3,4,5,6,7,101,102,103,104,201,202,301,302,303,304,305,401,405,406,407,501,502,503,504,505,506,701,702,703,704,705,706,707,708,709,801,802,803,901,902,903,904,1001,1003,1101,1102,1103,1104,1105,1106,1107,1150,1151,1152,1153,1154,1155,1201,1202,1203,1204,1301,1302,1350,1351,1352,1353,1401,1402,1403,1404,1405,1406,1408,1410,1411,1501,1502,1503,1504,1505,1506,1507,1600,1601,1602,1606,1607,1609};
 /**
  * IDs whose bundled sprite is known NOT to depict the item, or to depict nothing at all.
  *
  * <p>A PNG that decodes proves the file is a valid image and nothing more: every sprite here is
  * byte-identical to its Drive reference, yet byte identity says only that the copy is faithful to
  * the file it came from. {@code item-1609} is literally the letters "TMP" — a placeholder — and it
  * is byte-identical to the Drive reference too. 1350/1351 are two variants of the same artwork and
  * no catalogue confirms which is which, so they stay unnamed.
  */
 static boolean spriteIdentityUnconfirmed(int id){
  return id==1350||id==1351||id==1602||id==1606||id==1607||id==1609;
 }
 /** Shown under a gallery row: separates "the image loaded" from "the image is the right item". */
 static String identityNote(int id){
  return spriteIdentityUnconfirmed(id)?"imagem carregada; identidade visual não confirmada":"";
 }
 static String galleryLabel(int id){
  switch(id){
   case 1:return "Poké Bola";case 2:return "Grande Bola";case 3:return "Ultra Bola";
   case 101:return "Poção";case 102:return "Superpoção";case 103:return "Hiperpoção";case 104:return "Poção Máxima";
   case 201:return "Reviver";case 202:return "Reviver Máximo";case 301:return "Ovo da Sorte";
   case 4:return "Bola Mestra";
   case 5:return "Bola Premier";
   case 6:return "Bola Besta";
   case 7:return "Bola Safári";
   case 302:return "Cogumelo Max";
   case 303:return "Amuleto da Sorte";
   case 304:return "Tampinha prateada";
   case 305:return "Tampinha dourada";
   case 401:return "Incenso";
   case 405:return "Caixa Misteriosa";
   case 406:return "Incenso de Aventura Diário";
   case 407:return "Bolsa de Moedas";
   case 501:return "Módulo Atrair";
   case 502:return "Módulo Atrair Glacial";
   case 503:return "Módulo Atrair Musgoso";
   case 504:return "Módulo Atrair Magnético";
   case 505:return "Módulo Atrair Chuvoso";
   case 506:return "Módulo Atrair Dourado";
   case 701:return "Fruta Frambo";
   case 702:return "Fruta Bluk";
   case 703:return "Fruta Caxí";
   case 704:return "Fruta Wepear";
   case 705:return "Fruta Abacaxi";
   case 706:return "Fruta Frambo Dourada";
   case 707:return "Fruta Caxí Dourada";
   case 708:return "Fruta Abacaxi Prateada";
   case 709:return "Poffin";
   case 801:return "Câmera";
   case 802:return "Adesivos";
   case 803:return "Álbum de Cartões Postais";
   case 901:return "Incubadora ilimitada";
   case 902:return "Incubadora";
   case 903:return "Superincubadora";
   case 904:return "Incubadora temporária";
   case 1101:return "Pedra Solar";
   case 1102:return "Pedra do Rei";
   case 1103:return "Revestimento Metálico";
   case 1104:return "Escama de Dragão";
   case 1105:return "Melhora";
   case 1106:return "Pedra de Sinnoh";
   case 1107:return "Pedra de Unova";
   case 1302:return "Doce Raro GG";
   case 1401:return "Passe de Reide";
   case 1402:return "Passe de Batalha Premium";
   case 1403:return "Passe de Reide EX";
   case 1404:return "Pedaço de Estrela";
   case 1405:return "Presente";
   case 1406:return "Medalhão de Equipe";
   case 1408:return "Passe de Reide Remoto";
   case 1410:return "Cargas de Vínculo";
   case 1411:return "Porta-vínculo";
   case 1501:return "Componente Misterioso";
   case 1502:return "Radar Rocket";
   case 1503:return "Super-radar Rocket";
   case 1504:return "Fragmento Sombrio";
   case 1505:return "Joia Purificada";
   case 1506:return "Partículas Max";
   case 1507:return "Pacote de Partículas Max";
   case 1150:return "Meteorito";
   case 1151:return "Maçã evolutiva A";
   case 1152:return "Maçã evolutiva B";
   case 1155:return "Maçã evolutiva C";
   case 1153:return "Energia Espada Coroada";
   case 1154:return "Energia Escudo Coroado";
   case 1352:return "Energia de Fusão Volt";
   case 1353:return "Energia de Fusão Blaze";
   case 1201:return "MT de Ataque Rápido";
   case 1202:return "MT de Ataque Carregado";
   case 1203:return "MT de Ataque Rápido Élite";
   case 1204:return "MT de Ataque Carregado Élite";
   case 1301:return "Doce Raro";
   case 1001:return "Expansão de armazenamento Pokémon";
   case 1003:return "Expansão de cartões postais";
   default:return "Item de catálogo (nome não confirmado)";
  }
 }
 static void itemGallery(Activity a){
  LinearLayout b=body(a);text(a,b,"Conferência visual dos ícones já empacotados. Compare pelo ID com o catálogo autorizado. Imagem decodificada não comprova identidade visual. Nenhum inventário é lido.");
  EditText lookup=numeric(a,b,"Consultar ID de catálogo (1–999999)","",false,false);
  LinearLayout inspected=body(a);b.addView(inspected);
  lookup.addTextChangedListener(new android.text.TextWatcher(){
   public void beforeTextChanged(CharSequence s,int start,int count,int after){}
   public void onTextChanged(CharSequence s,int start,int before,int count){inspected.removeAllViews();}
   public void afterTextChanged(android.text.Editable e){}
  });
  action(a,b,"Conferir ID informado",()->{
   inspected.removeAllViews();
   try{
    int id=integer(lookup);if(id<1||id>999999)throw new IllegalArgumentException();
    ItemBadge badge=new ItemBadge(MapTheme.wrap(a),id);
    inspected.addView(badge,new LinearLayout.LayoutParams(MapTheme.dp(a,56),MapTheme.dp(a,56)));
    String identity=spriteIdentityUnconfirmed(id)?" • identidade visual não confirmada":"";text(a,inspected,galleryLabel(id)+" • ID "+id+" • "+(badge.hasDecodedSprite()?"Imagem decodificada; confira a identidade":badge.spriteStatus()+"; não atribua outro ID por semelhança")+identity);
   }catch(IllegalArgumentException invalid){text(a,inspected,"Informe um ID válido de catálogo.");}
  });
  LinearLayout gallery=body(a);b.addView(gallery);
  TextView pageLabel=text(a,b,"");int[] page={0};int pageSize=9;
  Button[] navigation=new Button[2];
  Runnable render=()->{
   gallery.removeAllViews();
   int pages=(GALLERY_IDS.length+pageSize-1)/pageSize;
   pageLabel.setText("Página "+(page[0]+1)+"/"+pages+" • "+GALLERY_IDS.length+" IDs");
   navigation[0].setEnabled(page[0]>0);navigation[1].setEnabled(page[0]+1<pages);
   for(int i=page[0]*pageSize;i<Math.min(GALLERY_IDS.length,(page[0]+1)*pageSize);i++){
    int id=GALLERY_IDS[i];LinearLayout row=new LinearLayout(MapTheme.wrap(a));row.setGravity(android.view.Gravity.CENTER_VERTICAL);
    ItemBadge badge=new ItemBadge(MapTheme.wrap(a),id);
    row.addView(badge,new LinearLayout.LayoutParams(MapTheme.dp(a,48),MapTheme.dp(a,48)));
    TextView label=new TextView(MapTheme.wrap(a));String note=identityNote(id);label.setText(galleryLabel(id)+" • ID "+id+"\n"+badge.spriteStatus()+(note.isEmpty()?"":" • "+note));
    row.addView(label,new LinearLayout.LayoutParams(0,-2,1));gallery.addView(row);
   }
  };
  navigation[0]=action(a,b,"Página anterior",()->{if(page[0]>0){page[0]--;render.run();}});
  navigation[1]=action(a,b,"Próxima página",()->{if((page[0]+1)*pageSize<GALLERY_IDS.length){page[0]++;render.run();}});
  render.run();
  present(a,new AlertDialog.Builder(MapTheme.wrap(a)).setTitle("Conferência visual de itens").setView(scroll(a,b)).setPositiveButton("Fechar",null).create());
 }
 static void tagReview(Activity a){
  LinearLayout b=body(a);text(a,b,"Informe os nomes das etiquetas, um por linha. Selecione as protegidas e as observadas no Pokémon. Não lê etiquetas do jogo, não altera a coleção e não executa ações.");
  EditText source=new EditText(MapTheme.wrap(a));source.setMinLines(3);source.setMaxLines(6);source.setHint("Uma etiqueta por linha");privateInput(source);b.addView(source);
  LinearLayout choices=body(a);b.addView(choices);
  CheckBox complete=new CheckBox(MapTheme.wrap(a));complete.setText("Confirmei todas as etiquetas deste Pokémon");b.addView(complete);
  TextView result=text(a,b,"");List<String> catalog=new ArrayList<>();List<CheckBox> selected=new ArrayList<>(),observed=new ArrayList<>();
  Runnable invalidate=()->result.setText("Dados alterados. Confira novamente.");
  source.addTextChangedListener(new android.text.TextWatcher(){
   public void beforeTextChanged(CharSequence s,int start,int count,int after){}
   public void onTextChanged(CharSequence s,int start,int before,int count){catalog.clear();selected.clear();observed.clear();choices.removeAllViews();complete.setChecked(false);result.setText("Catálogo alterado. Carregue novamente.");}
   public void afterTextChanged(android.text.Editable e){}
  });
  complete.setOnCheckedChangeListener((v,on)->invalidate.run());
  action(a,b,"Carregar seleção visual",()->{
   catalog.clear();selected.clear();observed.clear();choices.removeAllViews();complete.setChecked(false);
   try{
    catalog.addAll(LocalAuditPolicy.tagCatalog(source.getText().toString()));
    if(catalog.isEmpty())throw new IllegalArgumentException();
    for(String tag:catalog){
     text(a,choices,tag);
     CheckBox protect=new CheckBox(MapTheme.wrap(a)),seen=new CheckBox(MapTheme.wrap(a));
     protect.setText("Proteger esta etiqueta");seen.setText("Observada neste Pokémon");
     protect.setContentDescription("Proteger etiqueta "+tag);seen.setContentDescription("Etiqueta observada "+tag);
     choices.addView(protect);choices.addView(seen);selected.add(protect);observed.add(seen);
     protect.setOnCheckedChangeListener((v,on)->invalidate.run());seen.setOnCheckedChangeListener((v,on)->{complete.setChecked(false);invalidate.run();});
    }
    result.setText("Selecione as etiquetas e confira os dados.");
   }catch(IllegalArgumentException invalid){result.setText("Catálogo vazio ou inválido; informe até 200 nomes.");}
  });
  action(a,b,"Conferir etiquetas",()->{
   if(catalog.isEmpty()){result.setText("Catálogo não confirmado. Carregue os nomes primeiro.");return;}
   Set<String> protectedTags=new LinkedHashSet<>(),seenTags=new LinkedHashSet<>();
   for(int i=0;i<catalog.size();i++){if(selected.get(i).isChecked())protectedTags.add(catalog.get(i));if(observed.get(i).isChecked())seenTags.add(catalog.get(i));}
   LocalAuditPolicy.TagObservation state=LocalAuditPolicy.reviewTags(protectedTags,seenTags,complete.isChecked());
   result.setText(state==LocalAuditPolicy.TagObservation.MATCH?"TAG=1 — etiqueta protegida confirmada.":state==LocalAuditPolicy.TagObservation.NO_MATCH?"TAG=0 — nenhuma etiqueta protegida na lista que você confirmou completa.":"TAG=? — dados insuficientes ou nenhuma etiqueta de proteção selecionada.");
  });
  text(a,b,"Use o resultado TAG na prévia local da coleção, se corresponder aos dados que você verificou. Nenhum filtro do jogo foi modificado.");
  presentPrivate(a,new AlertDialog.Builder(MapTheme.wrap(a)).setTitle("Seleção visual de etiquetas").setView(scroll(a,b)).setPositiveButton("Fechar",null).create());
 }
 static void summarize(Activity a){
  LinearLayout b=body(a);text(a,b,"Abra o ZIP filtrado, extraia diagnostico.txt e cole seu conteúdo completo. Não cole o TXT bruto. A análise é local e não envia nem executa nada. A captura desta janela é bloqueada pelo Android; não é proteção das telas do jogo.");
  EditText source=new EditText(MapTheme.wrap(a));source.setMinLines(4);source.setMaxLines(8);
  source.setHint("Conteúdo de diagnostico.txt");privateInput(source);
  b.addView(source);TextView result=text(a,b,"");
  // Reject an oversized replacement completely, instead of silently truncating it.
  final boolean[] rejected={false};
  source.setFilters(new android.text.InputFilter[]{(incoming,start,end,dest,ds,de)->{
   if((long)dest.length()-(de-ds)+(end-start)>LocalDiagnosticSummary.MAX_CHARS){
    rejected[0]=true;result.setText("Colagem rejeitada; nenhum resumo atual disponível.");source.setError("Diagnóstico grande demais; use uma exportação menor.");return dest.subSequence(ds,de);
   }
   // Require a complete replacement/clear after a rejected paste, not an edit of stale contents.
   if(ds==0&&de==dest.length()){rejected[0]=false;source.setError(null);}
   return null;
  }});
  invalidateOnEdit(result,source);
  Button summarize=new Button(MapTheme.wrap(a));summarize.setText("Resumir registros");summarize.setAllCaps(false);summarize.setTextColor(MapTheme.ink());MapTheme.setButtonBackground(summarize);b.addView(summarize);
  final Thread[] worker={null};
  b.addOnAttachStateChangeListener(new android.view.View.OnAttachStateChangeListener(){
   public void onViewAttachedToWindow(android.view.View v){}
   public void onViewDetachedFromWindow(android.view.View v){if(worker[0]!=null)worker[0].interrupt();worker[0]=null;}
  });
  summarize.setOnClickListener(v->{
   if(worker[0]!=null)return;
   if(rejected[0]){result.setText("Substitua todo o conteúdo após a colagem rejeitada.");return;}
   final String snapshot=source.getText().toString();
   source.setEnabled(false);summarize.setEnabled(false);result.setText("Analisando diagnóstico local…");
   Thread task=new Thread(()->{
    String message;
    try{message=LocalDiagnosticSummary.parse(snapshot).describe();}
    catch(java.util.concurrent.CancellationException canceled){return;}
    catch(IllegalArgumentException invalid){message=invalid.getMessage();}
    catch(RuntimeException invalid){message="Não foi possível analisar o diagnóstico.";}
    final String completed=message;final Thread current=Thread.currentThread();
    a.runOnUiThread(()->{
     if(worker[0]!=current)return;worker[0]=null;
     if(a.isDestroyed()||!b.isAttachedToWindow())return;
     source.setEnabled(true);summarize.setEnabled(true);
     if(!rejected[0]&&snapshot.contentEquals(source.getText()))result.setText(completed);
     else result.setText("Dados alterados. Calcule novamente.");
    });
   },"pgbr-local-diagnostic");
   worker[0]=task;task.start();
  });
  presentPrivate(a,new AlertDialog.Builder(MapTheme.wrap(a)).setTitle("Resumo local de diagnóstico").setView(scroll(a,b)).setPositiveButton("Fechar",null).create());
 }
 static void maxPlan(Activity a){
  LinearLayout b=body(a);text(a,b,"Preparação manual. Informe os tipos da forma correta, o tipo do golpe Max e um tipo de ataque do chefe que você confirmou. Isto compara apenas multiplicadores de tipo: não calcula dano, nível, habilidades Max, efeitos de equipe ou resultado da batalha.");
  Spinner f1=select(a,b,"Tipo 1 do chefe",LocalAuditPolicy.TYPES),f2=select(a,b,"Tipo 2 do chefe",LocalAuditPolicy.TYPES),fm=select(a,b,"Tipo do ataque do chefe (Nenhum = desconhecido)",LocalAuditPolicy.TYPES);
  EditText stock=numeric(a,b,"Partículas disponíveis (-1 desconhecido)","-1",false,true),cost=numeric(a,b,"Custo informado pelo jogo (-1 desconhecido)","-1",false,true);
  EditText[] names=new EditText[3];Spinner[] own1=new Spinner[3],own2=new Spinner[3],moves=new Spinner[3],eligible=new Spinner[3];
  for(int i=0;i<3;i++){
   text(a,b,"Candidato "+(i+1));
   names[i]=input(a,b,"Nome/forma (vazio para omitir)","");
   own1[i]=select(a,b,"Tipo 1",LocalAuditPolicy.TYPES);own2[i]=select(a,b,"Tipo 2",LocalAuditPolicy.TYPES);moves[i]=select(a,b,"Tipo do golpe Max",LocalAuditPolicy.TYPES);
   eligible[i]=select(a,b,"Elegibilidade verificada para esta batalha",new String[]{"Desconhecida","Confirmada","Não elegível"});
  }
  TextView result=text(a,b,"");invalidateOnEdit(result,stock,cost);invalidateOnEdit(result,names);
  invalidateOnSelection(result,f1,f2,fm);invalidateOnSelection(result,own1);invalidateOnSelection(result,own2);invalidateOnSelection(result,moves);invalidateOnSelection(result,eligible);
  action(a,b,"Revisar preparação",()->{try{
   LocalAuditPolicy.BudgetState budget=LocalAuditPolicy.maxBudget(integer(stock),integer(cost));
   StringBuilder out=new StringBuilder("Partículas: "+(budget==LocalAuditPolicy.BudgetState.UNKNOWN?"dados insuficientes":budget==LocalAuditPolicy.BudgetState.INSUFFICIENT?"estoque informado insuficiente":"estoque informado suficiente")+"\n");
   int present=0;
   for(int i=0;i<3;i++){
    String name=names[i].getText().toString().trim();if(name.isEmpty())continue;
    LocalAuditPolicy.speciesKey(name);present++;
    int e=eligible[i].getSelectedItemPosition();
    out.append("\n").append(name).append(": ");
    if(e!=1){out.append(e==2?"não elegível conforme informado.":"elegibilidade não confirmada.");continue;}
    LocalAuditPolicy.Matchup match=LocalAuditPolicy.partialMatchup(own1[i].getSelectedItemPosition(),own2[i].getSelectedItemPosition(),moves[i].getSelectedItemPosition(),f1.getSelectedItemPosition(),f2.getSelectedItemPosition(),fm.getSelectedItemPosition());
    out.append("Ataque: ").append(Double.isFinite(match.outgoing)?String.format(Locale.forLanguageTag("pt-BR"),"×%.3f",match.outgoing):"não confirmado");
    out.append("; dano recebido por tipo: ").append(Double.isFinite(match.incoming)?String.format(Locale.forLanguageTag("pt-BR"),"×%.3f",match.incoming):"não confirmado");
   }
   if(present==0)out.append("\nNenhum candidato informado.");
   out.append("\n\nConfira regras e elegibilidade no jogo. Nenhuma equipe foi selecionada e nenhuma partícula foi gasta.");
   result.setText(out);
  }catch(IllegalArgumentException invalid){result.setText("Confira nomes, estoque e custo.");}});
  presentPrivate(a,new AlertDialog.Builder(MapTheme.wrap(a)).setTitle("Preparação manual Max").setView(scroll(a,b)).setPositiveButton("Fechar",null).create());
 }
 static void boostPlan(Activity a){
  LinearLayout b=body(a);
  text(a,b,"Planejamento manual: informe a duração real mostrada no jogo, inclusive bônus de evento. Este painel não lê o inventário nem usa itens.");
  EditText minutes=numeric(a,b,"Duração planejada (minutos)","60",false,false);
  EditText eggActive=numeric(a,b,"Ovo da Sorte: minutos restantes já ativos","0",false,false);
  EditText eggDuration=numeric(a,b,"Ovo da Sorte: duração por item (minutos)","30",false,false);
  EditText eggStock=numeric(a,b,"Ovos da Sorte disponíveis (-1 desconhecido)","-1",false,true);
  EditText starActive=numeric(a,b,"Pedaço de Estrela: minutos restantes já ativos","0",false,false);
  EditText starDuration=numeric(a,b,"Pedaço de Estrela: duração por item (minutos)","30",false,false);
  EditText starStock=numeric(a,b,"Pedaços de Estrela disponíveis (-1 desconhecido)","-1",false,true);
  TextView result=text(a,b,"");
  invalidateOnEdit(result,minutes,eggActive,eggDuration,eggStock,starActive,starDuration,starStock);
  action(a,b,"Calcular consumo estimado",()->{try{
   int period=integer(minutes);
   LocalAuditPolicy.BoostPlan egg=LocalAuditPolicy.planBoost(period,integer(eggActive),integer(eggDuration),integer(eggStock));
   LocalAuditPolicy.BoostPlan star=LocalAuditPolicy.planBoost(period,integer(starActive),integer(starDuration),integer(starStock));
   result.setText(boostLine("Ovo da Sorte",egg)+"\n"+boostLine("Pedaço de Estrela",star)+"\nEstimativa com os dados informados; nenhum item foi usado.");
  }catch(IllegalArgumentException invalid){result.setText("Confira as durações e o estoque informado.");}});
  present(a,new AlertDialog.Builder(MapTheme.wrap(a)).setTitle("Planejamento de bônus").setView(scroll(a,b)).setPositiveButton("Fechar",null).create());
 }
 private static String boostLine(String name,LocalAuditPolicy.BoostPlan plan){
  return name+": "+plan.needed+" item(ns) adicional(is)"+(plan.available<0?" • estoque desconhecido":" • faltam "+plan.shortfall);
 }
 static void berryPlan(Activity a){
  LinearLayout b=body(a);text(a,b,"Escolha até três frutas em ordem de preferência e informe o estoque que você verificou: -1 desconhecido, 0 indisponível. O painel não lê inventário nem usa frutas.");
  // Names come from the IDs, in the same order as the picker, so this dialog cannot drift from it.
  String[] names=new String[BerryPriorityPicker.IDS.length];
  for(int i=0;i<names.length;i++)names[i]=BerryPriorityPicker.display(i);
  Spinner[] order=new Spinner[3];EditText[] count=new EditText[3];
  for(int i=0;i<3;i++){order[i]=select(a,b,"Preferência "+(i+1),names);count[i]=numeric(a,b,"Estoque "+(i+1),"-1",false,true);}
  TextView result=text(a,b,"");invalidateOnEdit(result,count);invalidateOnSelection(result,order);
  action(a,b,"Consultar alternativas",()->{try{
   ArrayList<Integer> selected=new ArrayList<>();ArrayList<Integer> stock=new ArrayList<>();
   for(int i=0;i<3;i++){
    int id=order[i].getSelectedItemPosition();
    if(id<0)throw new IllegalArgumentException();
    if(id==0)continue;
    if(selected.contains(id))throw new IllegalArgumentException();
    selected.add(id);stock.add(integer(count[i]));
   }
   int[] values=new int[stock.size()];for(int i=0;i<values.length;i++)values[i]=stock.get(i);
   LocalAuditPolicy.StockAdvice advice=LocalAuditPolicy.reviewStock(values);
   String msg=advice.index<0?"Nenhuma alternativa com estoque confirmado.":"Alternativa disponível informada: "+names[selected.get(advice.index)];
   if(advice.earlierUnknown)msg+="\nHá estoque desconhecido em uma preferência anterior; confira antes de escolher.";
   result.setText(msg);
  }catch(IllegalArgumentException invalid){result.setText("Selecione frutas distintas e informe estoques válidos.");}});
  present(a,new AlertDialog.Builder(MapTheme.wrap(a)).setTitle("Planejamento manual de frutas").setView(scroll(a,b)).setPositiveButton("Fechar",null).create());
 }
 static void battleDiagnostic(Activity a){
  LinearLayout b=body(a);text(a,b,"Selecione o código do diagnóstico que você observou. A descrição é informativa e não executa ataques.");
  String[] codes={"actor-unknown","actor-refresh","stale-effect","unknown-effect","unverified-slots","unavailable-pair","only-one-ready","none-ready","effectiveness","known-super","off"};
  Spinner reason=select(a,b,"Motivo técnico",codes);TextView result=text(a,b,"");invalidateOnSelection(result,reason);
  action(a,b,"Interpretar",()->{int i=reason.getSelectedItemPosition();result.setText(i<0||i>=codes.length?"Selecione um motivo.":LocalAuditPolicy.battleReason(codes[i]));});
  present(a,new AlertDialog.Builder(MapTheme.wrap(a)).setTitle("Diagnóstico PvP — interpretação local").setView(scroll(a,b)).setPositiveButton("Fechar",null).create());
 }
 static void energyPlan(Activity a){
  LinearLayout b=body(a);text(a,b,"Planejamento manual, sem executar ataques. Informe o ganho do ágil e o custo do carregado de acordo com os dados PvP da forma correta. Energia residual é preservada.");
  EditText min=numeric(a,b,"Energia mínima estimada (0–100)","0",false,false),max=numeric(a,b,"Energia máxima estimada (0–100)","0",false,false),gain=numeric(a,b,"Energia por ataque ágil","",false,false),cost=numeric(a,b,"Custo do carregado","",false,false);
  TextView result=text(a,b,"");invalidateOnEdit(result,min,max,gain,cost);action(a,b,"Calcular",()->{try{
   LocalAuditPolicy.EnergyPlan p=LocalAuditPolicy.planEnergy(integer(min),integer(max),integer(gain),integer(cost),4);
   String ready=p.latest==0?"Pronto com certeza":p.earliest==0?"Pode estar pronto; energia incerta":"Ainda não pronto";
   result.setText(ready+"\nÁgeis restantes: "+p.earliest+(p.earliest==p.latest?"":" a "+p.latest)+(p.sequence.length==0?"\nSequência exata indisponível: energia inicial incerta.":"\nÁgeis para os próximos 4 carregados: "+Arrays.toString(p.sequence)));
  }catch(IllegalArgumentException invalid){result.setText("Confira os limites de energia e o ganho/custo.");}});
  present(a,new AlertDialog.Builder(MapTheme.wrap(a)).setTitle("Planejamento de energia PvP").setView(scroll(a,b)).setPositiveButton("Fechar",null).create());
 }
 static void combat(Activity a){
  LinearLayout b=body(a);text(a,b,"Simulação: use potência e custo da modalidade e forma corretas. Não considera escudos, bônus de estado, clima ou prioridade CMP. Nenhum ataque será executado.");
  Spinner mode=select(a,b,"Modalidade",new String[]{"PvP","PvE"});
  Spinner objective=select(a,b,"Objetivo da comparação",new String[]{"Eficiência (dano por energia)","Maior dano imediato","Dano por segundo (PvE)","Finalizar alvo sem escudo"});
  EditText targetHp=numeric(a,b,"PS restantes do alvo (somente finalizar)","",true,false);
  text(a,b,"O cálculo de finalização só vale sem escudo, com PS e atributos informados corretamente. A duração PvE é a do golpe, sem incluir o ciclo de geração de energia.");
  Spinner u1=select(a,b,"Seu tipo 1",LocalAuditPolicy.TYPES),u2=select(a,b,"Seu tipo 2",LocalAuditPolicy.TYPES),f1=select(a,b,"Tipo adversário 1",LocalAuditPolicy.TYPES),f2=select(a,b,"Tipo adversário 2",LocalAuditPolicy.TYPES);
  EditText atk=numeric(a,b,"Ataque efetivo","",true,false),def=numeric(a,b,"Defesa efetiva adversária","",true,false),energy=numeric(a,b,"Energia disponível (0–100)","",true,false);
  CheckBox single=new CheckBox(MapTheme.wrap(a));single.setText("Confirmo que existe somente um carregado");b.addView(single);
  Spinner[] type=new Spinner[2];EditText[] power=new EditText[2],cost=new EditText[2],duration=new EditText[2];
  for(int i=0;i<2;i++){type[i]=select(a,b,"Tipo carregado "+(i+1),LocalAuditPolicy.TYPES);power[i]=numeric(a,b,"Potência "+(i+1),"",true,false);cost[i]=numeric(a,b,"Energia exigida "+(i+1),"",true,false);duration[i]=numeric(a,b,"Duração do golpe em segundos (PvE) "+(i+1),"",true,false);}
  TextView result=text(a,b,"");invalidateOnEdit(result,atk,def,energy,targetHp);invalidateOnEdit(result,power);invalidateOnEdit(result,cost);invalidateOnEdit(result,duration);
  invalidateOnSelection(result,mode,objective,u1,u2,f1,f2);invalidateOnSelection(result,type);single.setOnCheckedChangeListener((v,on)->result.setText("Dados alterados. Compare novamente."));
  action(a,b,"Comparar",()->{try{
   LocalAuditPolicy.Estimate[] e=new LocalAuditPolicy.Estimate[2];StringBuilder out=new StringBuilder(mode.getSelectedItemPosition()==0?"PvP\n":"PvE\n");
   for(int i=0;i<(single.isChecked()?1:2);i++){e[i]=LocalAuditPolicy.estimate(type[i].getSelectedItemPosition(),decimal(power[i]),decimal(cost[i]),u1.getSelectedItemPosition(),u2.getSelectedItemPosition(),f1.getSelectedItemPosition(),f2.getSelectedItemPosition(),decimal(atk),decimal(def),decimal(energy));if(e[i]==null)throw new IllegalArgumentException();out.append(String.format(Locale.forLanguageTag("pt-BR"),"Carregado %d: ×%.3f • %s • dano estimado %.0f • DPE %.2f • %s%n",i+1,e[i].effectiveness,e[i].effectiveness>1.001?"Super eficaz":e[i].effectiveness<0.999?"Pouco eficaz":"Neutro",e[i].damage,e[i].perEnergy,e[i].ready?"Pronto":"Sem energia"));}
   LocalAuditPolicy.Objective goal=LocalAuditPolicy.Objective.values()[objective.getSelectedItemPosition()];
   if(goal==LocalAuditPolicy.Objective.DAMAGE_PER_SECOND&&mode.getSelectedItemPosition()!=1)throw new IllegalArgumentException();
   double secA=goal==LocalAuditPolicy.Objective.DAMAGE_PER_SECOND?decimal(duration[0]):1;
   double secB=goal==LocalAuditPolicy.Objective.DAMAGE_PER_SECOND&&!single.isChecked()?decimal(duration[1]):1;
   double hp=goal==LocalAuditPolicy.Objective.FINISH_TARGET?decimal(targetHp):1;
   if(goal==LocalAuditPolicy.Objective.FINISH_TARGET&&(!Double.isFinite(hp)||hp<=0))throw new IllegalArgumentException();
   if(goal==LocalAuditPolicy.Objective.DAMAGE_PER_SECOND){
    if(!Double.isFinite(LocalAuditPolicy.damagePerSecond(e[0],secA))||(!single.isChecked()&&!Double.isFinite(LocalAuditPolicy.damagePerSecond(e[1],secB))))throw new IllegalArgumentException();
    for(int i=0;i<(single.isChecked()?1:2);i++)out.append(String.format(Locale.forLanguageTag("pt-BR"),"Carregado %d: %.2f dano/s%n",i+1,LocalAuditPolicy.damagePerSecond(e[i],i==0?secA:secB)));
   }
   int best=single.isChecked()?LocalAuditPolicy.recommendSingle(e[0]):LocalAuditPolicy.compareContext(e[0],e[1],goal,secA,secB,hp);
   out.append(best<0?"Nenhum carregado pronto.":"Recomendação pelo objetivo selecionado: carregado "+(best+1));
   if(goal==LocalAuditPolicy.Objective.FINISH_TARGET&&best>=0)out.append(e[best].damage>=hp?" — dano estimado suficiente, sem escudo.":" — dano estimado insuficiente para finalizar.");
   result.setText(out);
  }catch(RuntimeException invalid){result.setText("Dados incompletos ou inválidos. Não há recomendação.");}});
  present(a,new AlertDialog.Builder(MapTheme.wrap(a)).setTitle("Simulador de eficácia").setView(scroll(a,b)).setPositiveButton("Fechar",null).create());
 }
}
