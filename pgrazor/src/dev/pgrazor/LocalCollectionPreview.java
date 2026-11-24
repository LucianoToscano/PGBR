package dev.pgrazor;
import java.util.*;
/** Offline preview of data supplied by the player; no collection access or destructive action. */
public final class LocalCollectionPreview {
 private LocalCollectionPreview(){}
 public static final int MAX_ROWS=500,MAX_CHARS=100000,PAGE_SIZE=25;
 public static final class Row {
  public final String species;public final int sourceLine,cp,attack,defense,stamina;
  public final LocalAuditPolicy.Decision decision;
  Row(int line,String name,int cp,int a,int d,int s,LocalAuditPolicy.Decision decision){sourceLine=line;species=name;this.cp=cp;attack=a;defense=d;stamina=s;this.decision=decision;}
  public double ivPercent(){return LocalAuditPolicy.validIv(attack)&&LocalAuditPolicy.validIv(defense)&&LocalAuditPolicy.validIv(stamina)?(attack+defense+stamina)*100.0/45:Double.NaN;}
 }
 public static final class Report {
  public final List<Row> rows;public final List<Integer> rejectedLines;public final Map<Integer,String> rejectedReasons;public final int protectedCount,uncertainCount,matchCount;
  Report(List<Row> rows,Map<Integer,String> rejected){this.rows=Collections.unmodifiableList(new ArrayList<>(rows));rejectedLines=Collections.unmodifiableList(new ArrayList<>(rejected.keySet()));rejectedReasons=Collections.unmodifiableMap(new LinkedHashMap<>(rejected));int p=0,u=0,k=0;for(Row r:rows){switch(r.decision.status){case PROTECTED:p++;break;case UNCERTAIN:u++;break;case MATCH:k++;break;}}protectedCount=p;uncertainCount=u;matchCount=k;}
 }
 /** A null filter represents all statuses; order and source-line identity are preserved. */
 public static List<Row> filtered(Report report,LocalAuditPolicy.Status filter){
  Objects.requireNonNull(report);List<Row> result=new ArrayList<>();
  for(Row row:report.rows)if(filter==null||filter==row.decision.status)result.add(row);
  return Collections.unmodifiableList(result);
 }
 public static int pageCount(Report report,LocalAuditPolicy.Status filter){return Math.max(1,(filtered(report,filter).size()+PAGE_SIZE-1)/PAGE_SIZE);}
 public static List<Row> page(Report report,LocalAuditPolicy.Status filter,int index){
  List<Row> all=filtered(report,filter);int pages=Math.max(1,(all.size()+PAGE_SIZE-1)/PAGE_SIZE);
  if(index<0||index>=pages)throw new IllegalArgumentException("Página inválida");
  int start=index*PAGE_SIZE;return Collections.unmodifiableList(new ArrayList<>(all.subList(start,Math.min(all.size(),start+PAGE_SIZE))));
 }
 private static void checkCanceled(){if(Thread.currentThread().isInterrupted())throw new java.util.concurrent.CancellationException();}
 public static Report parse(String text,int min,Set<LocalAuditPolicy.Flag> enabled){return parse(text,min,enabled,Collections.emptySet());}
 public static Report parse(String text,int min,Set<LocalAuditPolicy.Flag> enabled,Set<String> names){
  if(text==null||text.length()>MAX_CHARS||min<0||min>101||enabled==null||names==null)throw new IllegalArgumentException("Dados acima do limite ou configuração inválida.");
  checkCanceled();
  List<Row> rows=new ArrayList<>();Map<Integer,String> bad=new LinkedHashMap<>();
  int count=0;
  // Scan one physical line at a time. A paste containing only newlines must
  // not allocate a split array proportional to MAX_CHARS.
  for(int i=0,start=0;start<text.length();i++){
   checkCanceled();
   int end=start;
   while(end<text.length()&&text.charAt(end)!='\r'&&text.charAt(end)!='\n'){
    if((end&4095)==0)checkCanceled();
    end++;
   }
   String line=text.substring(start,end);
   start=end;
   if(start<text.length()){
    char separator=text.charAt(start++);
    if(separator=='\r'&&start<text.length()&&text.charAt(start)=='\n')start++;
   }
   if(i==0&&line.startsWith("\uFEFF"))line=line.substring(1);line=line.trim();if(line.isEmpty())continue;if(++count>MAX_ROWS)throw new IllegalArgumentException("Máximo de 500 registros.");
   String error="Formato: são necessários seis campos separados por ponto e vírgula.";
   try{
    String[] fields=line.split(";",7);if(fields.length!=6)throw new IllegalArgumentException();
    error="Nome de espécie ausente ou inválido.";
    String name=fields[0].trim();LocalAuditPolicy.speciesKey(name);
    error="PC inválido: informe um inteiro de 0 a 100000.";
    int cp=Integer.parseInt(fields[1].trim());if(cp<0||cp>100000)throw new IllegalArgumentException();
    error="IV inválido: use 0–15, -1 ou ?.";
    int a=iv(fields[2]),d=iv(fields[3]),s=iv(fields[4]);
    EnumMap<LocalAuditPolicy.Flag,Boolean> observed=new EnumMap<>(LocalAuditPolicy.Flag.class);
    if(!fields[5].trim().isEmpty())for(String fact:fields[5].split(",",LocalAuditPolicy.Flag.values().length+1)){
      error="Critério inválido: use NOME=1, NOME=0 ou NOME=?.";
      String[] parts=fact.trim().split("=",3);if(parts.length!=2)throw new IllegalArgumentException();
      error="Critério não reconhecido.";
      LocalAuditPolicy.Flag f=LocalAuditPolicy.Flag.valueOf(parts[0].trim().toUpperCase(Locale.ROOT));
      error="Critério repetido.";if(observed.containsKey(f))throw new IllegalArgumentException();
      error="Valor de critério inválido: use 1, 0 ou ?.";
      String val=parts[1].trim();if(!val.equals("1")&&!val.equals("0")&&!val.equals("?"))throw new IllegalArgumentException();
      observed.put(f,val.equals("?")?null:val.equals("1"));
    }
    error="Não foi possível avaliar o registro.";
    rows.add(new Row(i+1,name,cp,a,d,s,LocalAuditPolicy.withSpeciesProtection(LocalAuditPolicy.preview(a,d,s,min,enabled,observed),name,names)));
   }catch(IllegalArgumentException invalid){bad.put(i+1,error);}
  }
  checkCanceled();
  return new Report(rows,bad);
 }
 private static int iv(String value){String v=value.trim();if(v.equals("?"))return -1;int n=Integer.parseInt(v);if(n< -1||n>15)throw new IllegalArgumentException();return n;}
}
