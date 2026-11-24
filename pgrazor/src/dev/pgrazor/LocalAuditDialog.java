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
   .setItems(new String[]{"Prévia de proteção da coleção","Simular ataques carregados","Interpretar código de diagnóstico","Avaliar lista de Pokémon","Planejar energia PvP"},(x,i)->{if(i==0)collection(a);else if(i==1)combat(a);else if(i==2)reason(a);else if(i==3)batch(a);else energyPlan(a);})
   .setNegativeButton("Fechar",null).create();present(a,d);
 }
 static void present(Activity a,AlertDialog d){DialogLifecycle.bind(a,d,null);d.setOnShowListener(x->MapTheme.styleButtons(d));d.show();}
 static LinearLayout body(Activity a){LinearLayout b=new LinearLayout(MapTheme.wrap(a));b.setOrientation(LinearLayout.VERTICAL);int p=MapTheme.dp(a,16);b.setPadding(p,p,p,p);return b;}
 static ScrollView scroll(Activity a,LinearLayout b){ScrollView s=new ScrollView(a);s.addView(b);return s;}
 static TextView text(Activity a,LinearLayout b,String s){TextView t=new TextView(MapTheme.wrap(a));t.setText(s);b.addView(t);return t;}
 static EditText input(Activity a,LinearLayout b,String title,String initial){text(a,b,title);EditText e=new EditText(MapTheme.wrap(a));e.setSingleLine(true);e.setText(initial);b.addView(e);return e;}
 static Spinner select(Activity a,LinearLayout b,String title,String[] values){text(a,b,title);Spinner s=new Spinner(MapTheme.wrap(a));ArrayAdapter<String> ad=new ArrayAdapter<>(a,android.R.layout.simple_spinner_item,values);ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);s.setAdapter(ad);b.addView(s);return s;}
 static void action(Activity a,LinearLayout b,String title,Runnable f){Button v=new Button(MapTheme.wrap(a));v.setText(title);v.setOnClickListener(x->f.run());b.addView(v);}
 static int integer(EditText e){return Integer.parseInt(e.getText().toString().trim());}
 static double decimal(EditText e){return Double.parseDouble(e.getText().toString().trim().replace(',','.'));}
 static void collection(Activity a){
  LinearLayout b=body(a);text(a,b,"Informe dados verificados. Desconhecido protege por precaução. Nenhuma transferência será executada.");
  EditText name=input(a,b,"Espécie",""),cp=input(a,b,"PC",""),atk=input(a,b,"IV ataque (0–15 ou -1 desconhecido)","-1"),def=input(a,b,"IV defesa","-1"),sta=input(a,b,"IV PS","-1"),limit=input(a,b,"IV mínimo para preservar (%)","99");
  LocalAuditPolicy.Flag[] flags=LocalAuditPolicy.Flag.values();Spinner[] observed=new Spinner[flags.length];CheckBox[] active=new CheckBox[flags.length];
  for(int i=0;i<flags.length;i++){CheckBox c=new CheckBox(MapTheme.wrap(a));c.setText("Proteger: "+flags[i].label);c.setChecked(true);b.addView(c);active[i]=c;observed[i]=select(a,b,"Dado observado",new String[]{"Desconhecido","Sim","Não"});}
  TextView result=text(a,b,"");action(a,b,"Avaliar sem transferir",()->{try{
   int pc=integer(cp),aa=integer(atk),dd=integer(def),ss=integer(sta);if(pc<0||name.getText().toString().trim().isEmpty())throw new IllegalArgumentException();
   EnumSet<LocalAuditPolicy.Flag> selected=EnumSet.noneOf(LocalAuditPolicy.Flag.class);EnumMap<LocalAuditPolicy.Flag,Boolean> values=new EnumMap<>(LocalAuditPolicy.Flag.class);
   for(int i=0;i<flags.length;i++){if(active[i].isChecked())selected.add(flags[i]);int x=observed[i].getSelectedItemPosition();if(x!=0)values.put(flags[i],x==1);}
   LocalAuditPolicy.Decision decision=LocalAuditPolicy.preview(aa,dd,ss,integer(limit),selected,values);
   String state=decision.status==LocalAuditPolicy.Status.PROTECTED?"Protegido":decision.status==LocalAuditPolicy.Status.UNCERTAIN?"Protegido por incerteza":"Corresponde ao filtro";
   String iv=LocalAuditPolicy.validIv(aa)&&LocalAuditPolicy.validIv(dd)&&LocalAuditPolicy.validIv(ss)?String.format(Locale.forLanguageTag("pt-BR"),"%.2f%%",(aa+dd+ss)*100.0/45):"Desconhecido";
   result.setText(name.getText()+" • PC "+pc+" • IV "+iv+"\n"+state+"\n"+String.join("\n",decision.reasons)+"\n"+new java.util.Date()+" • PGBR "+BuildVersion.NAME+" • esquema "+LocalAuditPolicy.SCHEMA);
  }catch(RuntimeException invalid){result.setText("Confira os campos e os limites.");}});
  present(a,new AlertDialog.Builder(MapTheme.wrap(a)).setTitle("Prévia local de filtros").setView(scroll(a,b)).setPositiveButton("Fechar",null).create());
 }
 static void batch(Activity a){
  LinearLayout b=body(a);
  text(a,b,"Cole até 500 registros de uma fonte autorizada, um por linha. Formato: espécie;PC;IV ataque;IV defesa;IV PS;critérios. Use ? para desconhecido; critérios no formato EVENT=1,SHINY=0. Ausência de informação não significa falso. Não lê a coleção nem transfere Pokémon.");
  EditText limit=input(a,b,"IV mínimo para preservar (%)","99");
  EditText records=new EditText(MapTheme.wrap(a));records.setMinLines(4);records.setMaxLines(8);records.setHint("Rattata;478;4;4;5;EVENT=1,SHINY=0");records.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(LocalCollectionPreview.MAX_CHARS)});b.addView(records);
  LocalAuditPolicy.Flag[] flags=LocalAuditPolicy.Flag.values();CheckBox[] active=new CheckBox[flags.length];
  for(int i=0;i<flags.length;i++){CheckBox c=new CheckBox(MapTheme.wrap(a));c.setText("Proteger "+flags[i].label+" ("+flags[i].name()+")");c.setChecked(true);b.addView(c);active[i]=c;}
  TextView result=text(a,b,"");action(a,b,"Avaliar lista sem transferir",()->{try{
   EnumSet<LocalAuditPolicy.Flag> selected=EnumSet.noneOf(LocalAuditPolicy.Flag.class);for(int i=0;i<flags.length;i++)if(active[i].isChecked())selected.add(flags[i]);
   LocalCollectionPreview.Report report=LocalCollectionPreview.parse(records.getText().toString(),integer(limit),selected);
   StringBuilder out=new StringBuilder("Protegidos: "+report.protectedCount+" | Incertos: "+report.uncertainCount+" | Correspondem: "+report.matchCount+" | Linhas inválidas: "+report.rejectedLines.size()+"\n");
   if(!report.rejectedLines.isEmpty())out.append("Linhas ignoradas: ").append(report.rejectedLines).append('\n');
   int shown=0;for(LocalCollectionPreview.Row row:report.rows){
    if(shown++==50){out.append("Outros resultados contabilizados; visualização limitada a 50.\n");break;}
    out.append(row.species).append(" • PC ").append(row.cp).append(" • ").append(row.decision.status==LocalAuditPolicy.Status.PROTECTED?"Protegido":row.decision.status==LocalAuditPolicy.Status.UNCERTAIN?"Incerto":"Corresponde ao filtro").append('\n').append(String.join(", ",row.decision.reasons)).append("\n\n");
   }
   result.setText(out.toString());
  }catch(IllegalArgumentException invalid){result.setText("Confira os limites e o formato da lista.");}});
  present(a,new AlertDialog.Builder(MapTheme.wrap(a)).setTitle("Prévia de coleção em lote").setView(scroll(a,b)).setPositiveButton("Fechar",null).create());
 }
 static void reason(Activity a){LinearLayout b=body(a);EditText code=input(a,b,"reasonCode","12");TextView r=text(a,b,"");action(a,b,"Interpretar",()->{try{r.setText(LocalAuditPolicy.reason(integer(code)));}catch(RuntimeException e){r.setText("Código inválido.");}});present(a,new AlertDialog.Builder(MapTheme.wrap(a)).setTitle("Motivos de proteção").setView(b).setPositiveButton("Fechar",null).create());}
 static void energyPlan(Activity a){
  LinearLayout b=body(a);text(a,b,"Planejamento manual, sem executar ataques. Informe o ganho do ágil e o custo do carregado de acordo com os dados PvP da forma correta. Energia residual é preservada.");
  EditText min=input(a,b,"Energia mínima estimada (0–100)","0"),max=input(a,b,"Energia máxima estimada (0–100)","0"),gain=input(a,b,"Energia por ataque ágil",""),cost=input(a,b,"Custo do carregado","");
  TextView result=text(a,b,"");action(a,b,"Calcular",()->{try{
   LocalAuditPolicy.EnergyPlan p=LocalAuditPolicy.planEnergy(integer(min),integer(max),integer(gain),integer(cost),4);
   String ready=p.latest==0?"Pronto com certeza":p.earliest==0?"Pode estar pronto; energia incerta":"Ainda não pronto";
   result.setText(ready+"\nÁgeis restantes: "+p.earliest+(p.earliest==p.latest?"":" a "+p.latest)+(p.sequence.length==0?"\nSequência exata indisponível: energia inicial incerta.":"\nÁgeis para os próximos 4 carregados: "+Arrays.toString(p.sequence)));
  }catch(IllegalArgumentException invalid){result.setText("Confira os limites de energia e o ganho/custo.");}});
  present(a,new AlertDialog.Builder(MapTheme.wrap(a)).setTitle("Planejamento de energia PvP").setView(scroll(a,b)).setPositiveButton("Fechar",null).create());
 }
 static void combat(Activity a){
  LinearLayout b=body(a);text(a,b,"Simulação: use potência e custo da modalidade e forma corretas. Não considera escudos, bônus de estado, clima ou prioridade CMP. Nenhum ataque será executado.");
  Spinner mode=select(a,b,"Modalidade",new String[]{"PvP","PvE"});
  Spinner u1=select(a,b,"Seu tipo 1",LocalAuditPolicy.TYPES),u2=select(a,b,"Seu tipo 2",LocalAuditPolicy.TYPES),f1=select(a,b,"Tipo adversário 1",LocalAuditPolicy.TYPES),f2=select(a,b,"Tipo adversário 2",LocalAuditPolicy.TYPES);
  EditText atk=input(a,b,"Ataque efetivo",""),def=input(a,b,"Defesa efetiva adversária",""),energy=input(a,b,"Energia disponível (0–100)","");
  CheckBox single=new CheckBox(MapTheme.wrap(a));single.setText("Confirmo que existe somente um carregado");b.addView(single);
  Spinner[] type=new Spinner[2];EditText[] power=new EditText[2],cost=new EditText[2];
  for(int i=0;i<2;i++){type[i]=select(a,b,"Tipo carregado "+(i+1),LocalAuditPolicy.TYPES);power[i]=input(a,b,"Potência "+(i+1),"");cost[i]=input(a,b,"Energia exigida "+(i+1),"");}
  TextView result=text(a,b,"");action(a,b,"Comparar",()->{try{
   LocalAuditPolicy.Estimate[] e=new LocalAuditPolicy.Estimate[2];StringBuilder out=new StringBuilder(mode.getSelectedItemPosition()==0?"PvP\n":"PvE\n");
   for(int i=0;i<(single.isChecked()?1:2);i++){e[i]=LocalAuditPolicy.estimate(type[i].getSelectedItemPosition(),decimal(power[i]),decimal(cost[i]),u1.getSelectedItemPosition(),u2.getSelectedItemPosition(),f1.getSelectedItemPosition(),f2.getSelectedItemPosition(),decimal(atk),decimal(def),decimal(energy));if(e[i]==null)throw new IllegalArgumentException();out.append(String.format(Locale.forLanguageTag("pt-BR"),"Carregado %d: ×%.3f • %s • dano estimado %.0f • DPE %.2f • %s%n",i+1,e[i].effectiveness,e[i].effectiveness>1.001?"Super eficaz":e[i].effectiveness<0.999?"Pouco eficaz":"Neutro",e[i].damage,e[i].perEnergy,e[i].ready?"Pronto":"Sem energia"));}
   int best=single.isChecked()?LocalAuditPolicy.recommendSingle(e[0]):LocalAuditPolicy.recommend(e[0],e[1]);out.append(best<0?"Nenhum carregado pronto.":"Melhor DPE entre os prontos: carregado "+(best+1));result.setText(out);
  }catch(RuntimeException invalid){result.setText("Dados incompletos ou inválidos. Não há recomendação.");}});
  present(a,new AlertDialog.Builder(MapTheme.wrap(a)).setTitle("Simulador de eficácia").setView(scroll(a,b)).setPositiveButton("Fechar",null).create());
 }
}
