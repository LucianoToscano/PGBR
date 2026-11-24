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
   .setItems(new String[]{"Prévia de proteção da coleção","Simular ataques carregados","Interpretar código de diagnóstico","Avaliar lista de Pokémon","Planejar energia PvP","Interpretar diagnóstico PvP","Planejar alternativas de frutas","Preparar equipe Max (manual)","Resumir diagnóstico filtrado"},(x,i)->{if(i==0)collection(a);else if(i==1)combat(a);else if(i==2)reason(a);else if(i==3)batch(a);else if(i==4)energyPlan(a);else if(i==5)battleDiagnostic(a);else if(i==6)berryPlan(a);else if(i==7)maxPlan(a);else summarize(a);})
   .setNegativeButton("Fechar",null).create();present(a,d);
 }
 static void present(Activity a,AlertDialog d){DialogLifecycle.bind(a,d,null);d.setOnShowListener(x->MapTheme.styleButtons(d));d.show();}
 static LinearLayout body(Activity a){LinearLayout b=new LinearLayout(MapTheme.wrap(a));b.setOrientation(LinearLayout.VERTICAL);int p=MapTheme.dp(a,16);b.setPadding(p,p,p,p);return b;}
 static ScrollView scroll(Activity a,LinearLayout b){ScrollView s=new ScrollView(a);s.addView(b);return s;}
 static TextView text(Activity a,LinearLayout b,String s){TextView t=new TextView(MapTheme.wrap(a));t.setText(s);b.addView(t);return t;}
 static EditText input(Activity a,LinearLayout b,String title,String initial){text(a,b,title);EditText e=new EditText(MapTheme.wrap(a));e.setSingleLine(true);e.setText(initial);b.addView(e);return e;}
 static Spinner select(Activity a,LinearLayout b,String title,String[] values){text(a,b,title);Spinner s=new Spinner(MapTheme.wrap(a));ArrayAdapter<String> ad=new ArrayAdapter<>(a,android.R.layout.simple_spinner_item,values);ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);s.setAdapter(ad);b.addView(s);return s;}
 static void action(Activity a,LinearLayout b,String title,Runnable f){Button v=new Button(MapTheme.wrap(a));v.setText(title);v.setTextColor(MapTheme.ink());MapTheme.setButtonBackground(v);v.setAllCaps(false);v.setOnClickListener(x->f.run());b.addView(v);}
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
  LinearLayout b=body(a);text(a,b,"Informe dados verificados. Desconhecido protege por precaução. Nenhuma transferência será executada.");
  EditText name=input(a,b,"Espécie",""),cp=input(a,b,"PC",""),atk=input(a,b,"IV ataque (0–15 ou -1 desconhecido)","-1"),def=input(a,b,"IV defesa","-1"),sta=input(a,b,"IV PS","-1"),limit=input(a,b,"IV mínimo para preservar (0–100; 101 desativa só o limite de IV)","99");
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
   result.setText(name.getText()+" • PC "+pc+" • IV "+iv+"\n"+state+"\n"+String.join("\n",decision.reasons)+"\n"+new java.util.Date()+" • PGBR "+BuildVersion.NAME+" • esquema "+LocalAuditPolicy.SCHEMA);
  }catch(RuntimeException invalid){result.setText("Confira os campos e os limites.");}});
  present(a,new AlertDialog.Builder(MapTheme.wrap(a)).setTitle("Prévia local de filtros").setView(scroll(a,b)).setPositiveButton("Fechar",null).create());
 }
 static void batch(Activity a){
  LinearLayout b=body(a);
  text(a,b,"Cole até 500 registros de uma fonte autorizada, um por linha. Formato: espécie;PC;IV ataque;IV defesa;IV PS;critérios. Use ? para desconhecido; critérios no formato EVENT=1,SHINY=0. Ausência de informação não significa falso. Não lê a coleção nem transfere Pokémon.");
  EditText limit=input(a,b,"IV mínimo para preservar (0–100; 101 desativa só o limite de IV)","99");
  EditText protectedNames=input(a,b,"Espécies adicionais protegidas (nomes exatos separados por vírgulas)","");
  text(a,b,"A lista é adicional aos 20 critérios; formas diferentes precisam de nomes próprios. Não usa dados do jogo.");
  EditText records=new EditText(MapTheme.wrap(a));records.setMinLines(4);records.setMaxLines(8);records.setHint("Rattata;478;4;4;5;EVENT=1,SHINY=0");b.addView(records);
  LocalAuditPolicy.Flag[] flags=LocalAuditPolicy.Flag.values();CheckBox[] active=new CheckBox[flags.length];
  for(int i=0;i<flags.length;i++){CheckBox c=new CheckBox(MapTheme.wrap(a));c.setText("Proteger "+flags[i].label+" ("+flags[i].name()+")");c.setChecked(true);b.addView(c);active[i]=c;}
  Spinner filter=select(a,b,"Mostrar resultados",new String[]{"Todos","Protegidos","Protegidos por incerteza","Correspondem ao filtro"});
  TextView result=text(a,b,"");
  LocalCollectionPreview.Report[] report={null};int[] page={0};String[] evaluatedAt={""};final boolean[] oversized={false};
  records.setFilters(new android.text.InputFilter[]{(incoming,start,end,dest,ds,de)->{
   if((long)dest.length()-(de-ds)+(end-start)>LocalCollectionPreview.MAX_CHARS){oversized[0]=true;report[0]=null;page[0]=0;result.setText("Colagem rejeitada; nenhuma avaliação atual disponível.");records.setError("Lista grande demais. Use lotes menores.");return dest.subSequence(ds,de);}
   if(ds==0&&de==dest.length()){oversized[0]=false;records.setError(null);}return null;
  }});
  java.util.function.Supplier<LocalAuditPolicy.Status> status=()->filter.getSelectedItemPosition()<=0?null:LocalAuditPolicy.Status.values()[filter.getSelectedItemPosition()-1];
  Runnable render=()->{
   if(report[0]==null){result.setText("Avalie a lista para obter os resultados.");return;}
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
  Runnable invalidate=()->{report[0]=null;page[0]=0;result.setText("Dados ou filtros alterados. Avalie novamente.");};
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
  action(a,b,"Avaliar lista sem transferir",()->{
   report[0]=null;page[0]=0;
   if(oversized[0]||records.length()>LocalCollectionPreview.MAX_CHARS){result.setText("Colagem rejeitada por tamanho. Substitua todo o conteúdo por um lote completo menor; nenhum registro foi avaliado.");return;}
   try{
    EnumSet<LocalAuditPolicy.Flag> selected=EnumSet.noneOf(LocalAuditPolicy.Flag.class);for(int i=0;i<flags.length;i++)if(active[i].isChecked())selected.add(flags[i]);
    report[0]=LocalCollectionPreview.parse(records.getText().toString(),integer(limit),selected,LocalAuditPolicy.protectedNames(protectedNames.getText().toString()));evaluatedAt[0]=new java.util.Date().toString();render.run();
   }catch(IllegalArgumentException invalid){result.setText("Confira os limites e o formato da lista.");}
  });
  action(a,b,"Consultar motivos das linhas inválidas",()->{
   if(report[0]==null){result.setText("Avalie uma lista válida primeiro.");return;}
   StringBuilder errors=new StringBuilder();
   for(Map.Entry<Integer,String> entry:report[0].rejectedReasons.entrySet())errors.append("Linha ").append(entry.getKey()).append(": ").append(entry.getValue()).append('\n');
   if(errors.length()==0)errors.append("Nenhuma linha inválida.");
   LinearLayout detail=body(a);text(a,detail,errors.toString());
   present(a,new AlertDialog.Builder(MapTheme.wrap(a)).setTitle("Diagnóstico da importação").setView(scroll(a,detail)).setPositiveButton("Fechar",null).create());
  });
  action(a,b,"Página anterior",()->{if(report[0]!=null&&page[0]>0){page[0]--;render.run();}});
  action(a,b,"Próxima página",()->{if(report[0]!=null&&page[0]+1<LocalCollectionPreview.pageCount(report[0],status.get())){page[0]++;render.run();}});
  present(a,new AlertDialog.Builder(MapTheme.wrap(a)).setTitle("Prévia de coleção em lote").setView(scroll(a,b)).setPositiveButton("Fechar",null).create());
 }
 static void reason(Activity a){LinearLayout b=body(a);EditText code=input(a,b,"reasonCode","12");TextView r=text(a,b,"");action(a,b,"Interpretar",()->{try{r.setText(LocalAuditPolicy.reason(integer(code)));}catch(RuntimeException e){r.setText("Código inválido.");}});present(a,new AlertDialog.Builder(MapTheme.wrap(a)).setTitle("Motivos de proteção").setView(b).setPositiveButton("Fechar",null).create());}
 static void summarize(Activity a){
  LinearLayout b=body(a);text(a,b,"Abra o ZIP filtrado, extraia diagnostico.txt e cole seu conteúdo completo. Não cole o TXT bruto. A análise é local e não envia nem executa nada.");
  EditText source=new EditText(MapTheme.wrap(a));source.setMinLines(4);source.setMaxLines(8);
  source.setHint("Conteúdo de diagnostico.txt");
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
  action(a,b,"Resumir registros",()->{try{
    if(rejected[0])throw new IllegalArgumentException("Substitua todo o conteúdo após a colagem rejeitada.");
    result.setText(LocalDiagnosticSummary.parse(source.getText().toString()).describe());
   }catch(IllegalArgumentException invalid){result.setText(invalid.getMessage());}});
  present(a,new AlertDialog.Builder(MapTheme.wrap(a)).setTitle("Resumo local de diagnóstico").setView(scroll(a,b)).setPositiveButton("Fechar",null).create());
 }
 static void maxPlan(Activity a){
  LinearLayout b=body(a);text(a,b,"Preparação manual. Informe os tipos da forma correta, o tipo do golpe Max e um tipo de ataque do chefe que você confirmou. Isto compara apenas multiplicadores de tipo: não calcula dano, nível, habilidades Max, efeitos de equipe ou resultado da batalha.");
  Spinner f1=select(a,b,"Tipo 1 do chefe",LocalAuditPolicy.TYPES),f2=select(a,b,"Tipo 2 do chefe",LocalAuditPolicy.TYPES),fm=select(a,b,"Tipo do ataque do chefe",LocalAuditPolicy.TYPES);
  EditText stock=input(a,b,"Partículas disponíveis (-1 desconhecido)","-1"),cost=input(a,b,"Custo informado pelo jogo (-1 desconhecido)","-1");
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
    LocalAuditPolicy.Matchup match=LocalAuditPolicy.matchup(own1[i].getSelectedItemPosition(),own2[i].getSelectedItemPosition(),moves[i].getSelectedItemPosition(),f1.getSelectedItemPosition(),f2.getSelectedItemPosition(),fm.getSelectedItemPosition());
    if(match==null){out.append("tipagem incompleta ou inválida.");continue;}
    out.append(String.format(Locale.forLanguageTag("pt-BR"),"ataque ×%.3f; defesa contra golpe informado ×%.3f",match.outgoing,match.incoming));
   }
   if(present==0)out.append("\nNenhum candidato informado.");
   out.append("\n\nConfira regras e elegibilidade no jogo. Nenhuma equipe foi selecionada e nenhuma partícula foi gasta.");
   result.setText(out);
  }catch(IllegalArgumentException invalid){result.setText("Confira nomes, estoque e custo.");}});
  present(a,new AlertDialog.Builder(MapTheme.wrap(a)).setTitle("Preparação manual Max").setView(scroll(a,b)).setPositiveButton("Fechar",null).create());
 }
 static void berryPlan(Activity a){
  LinearLayout b=body(a);text(a,b,"Escolha até três frutas em ordem de preferência e informe o estoque que você verificou: -1 desconhecido, 0 indisponível. O painel não lê inventário nem usa frutas.");
  String[] names={"Nenhuma","Fruta Frambo","Fruta Anaba","Fruta Caxi","Fruta Frambo Dourada","Fruta Caxi Prateada"};
  Spinner[] order=new Spinner[3];EditText[] count=new EditText[3];
  for(int i=0;i<3;i++){order[i]=select(a,b,"Preferência "+(i+1),names);count[i]=input(a,b,"Estoque "+(i+1),"-1");}
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
  String[] codes={"actor-unknown","actor-refresh","stale-effect","unknown-effect","unverified-slots","unavailable-pair","only-one-ready","effectiveness","known-super","off"};
  Spinner reason=select(a,b,"Motivo técnico",codes);TextView result=text(a,b,"");invalidateOnSelection(result,reason);
  action(a,b,"Interpretar",()->{int i=reason.getSelectedItemPosition();result.setText(i<0||i>=codes.length?"Selecione um motivo.":LocalAuditPolicy.battleReason(codes[i]));});
  present(a,new AlertDialog.Builder(MapTheme.wrap(a)).setTitle("Diagnóstico PvP — interpretação local").setView(b).setPositiveButton("Fechar",null).create());
 }
 static void energyPlan(Activity a){
  LinearLayout b=body(a);text(a,b,"Planejamento manual, sem executar ataques. Informe o ganho do ágil e o custo do carregado de acordo com os dados PvP da forma correta. Energia residual é preservada.");
  EditText min=input(a,b,"Energia mínima estimada (0–100)","0"),max=input(a,b,"Energia máxima estimada (0–100)","0"),gain=input(a,b,"Energia por ataque ágil",""),cost=input(a,b,"Custo do carregado","");
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
  EditText targetHp=input(a,b,"PS restantes do alvo (somente finalizar)","");
  text(a,b,"O cálculo de finalização só vale sem escudo, com PS e atributos informados corretamente. A duração PvE é a do golpe, sem incluir o ciclo de geração de energia.");
  Spinner u1=select(a,b,"Seu tipo 1",LocalAuditPolicy.TYPES),u2=select(a,b,"Seu tipo 2",LocalAuditPolicy.TYPES),f1=select(a,b,"Tipo adversário 1",LocalAuditPolicy.TYPES),f2=select(a,b,"Tipo adversário 2",LocalAuditPolicy.TYPES);
  EditText atk=input(a,b,"Ataque efetivo",""),def=input(a,b,"Defesa efetiva adversária",""),energy=input(a,b,"Energia disponível (0–100)","");
  CheckBox single=new CheckBox(MapTheme.wrap(a));single.setText("Confirmo que existe somente um carregado");b.addView(single);
  Spinner[] type=new Spinner[2];EditText[] power=new EditText[2],cost=new EditText[2],duration=new EditText[2];
  for(int i=0;i<2;i++){type[i]=select(a,b,"Tipo carregado "+(i+1),LocalAuditPolicy.TYPES);power[i]=input(a,b,"Potência "+(i+1),"");cost[i]=input(a,b,"Energia exigida "+(i+1),"");duration[i]=input(a,b,"Duração do golpe em segundos (PvE) "+(i+1),"");}
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
