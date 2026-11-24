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
  public final List<Row> rows;public final List<Integer> rejectedLines;public final int protectedCount,uncertainCount,matchCount;
  Report(List<Row> rows,List<Integer> rejected){this.rows=Collections.unmodifiableList(new ArrayList<>(rows));rejectedLines=Collections.unmodifiableList(new ArrayList<>(rejected));int p=0,u=0,k=0;for(Row r:rows){switch(r.decision.status){case PROTECTED:p++;break;case UNCERTAIN:u++;break;case MATCH:k++;break;}}protectedCount=p;uncertainCount=u;matchCount=k;}
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
 public static Report parse(String text,int min,Set<LocalAuditPolicy.Flag> enabled){return parse(text,min,enabled,Collections.emptySet());}
 public static Report parse(String text,int min,Set<LocalAuditPolicy.Flag> enabled,Set<String> names){
  if(text==null||text.length()>MAX_CHARS||min<0||min>101||enabled==null||names==null)throw new IllegalArgumentException("Dados acima do limite ou configuração inválida.");
  List<Row> rows=new ArrayList<>();List<Integer> bad=new ArrayList<>();
  String[] lines=text.split("\\r\\n|\\n|\\r",-1);int count=0;
  for(int i=0;i<lines.length;i++){
   String line=lines[i].trim();if(line.isEmpty())continue;if(++count>MAX_ROWS)throw new IllegalArgumentException("Máximo de 500 registros.");
   try{
    String[] fields=line.split(";",-1);if(fields.length!=6)throw new IllegalArgumentException();
    String name=fields[0].trim();if(name.isEmpty()||name.length()>80)throw new IllegalArgumentException();
    LocalAuditPolicy.speciesKey(name);
    int cp=Integer.parseInt(fields[1].trim()),a=iv(fields[2]),d=iv(fields[3]),s=iv(fields[4]);if(cp<0||cp>100000)throw new IllegalArgumentException();
    EnumMap<LocalAuditPolicy.Flag,Boolean> observed=new EnumMap<>(LocalAuditPolicy.Flag.class);
    if(!fields[5].trim().isEmpty())for(String fact:fields[5].split(",",-1)){
      String[] parts=fact.trim().split("=",-1);if(parts.length!=2)throw new IllegalArgumentException();
      LocalAuditPolicy.Flag f=LocalAuditPolicy.Flag.valueOf(parts[0].trim().toUpperCase(Locale.ROOT));
      String val=parts[1].trim();if(observed.containsKey(f)||(!val.equals("1")&&!val.equals("0")&&!val.equals("?")))throw new IllegalArgumentException();
      observed.put(f,val.equals("?")?null:val.equals("1"));
    }
    rows.add(new Row(i+1,name,cp,a,d,s,LocalAuditPolicy.withSpeciesProtection(LocalAuditPolicy.preview(a,d,s,min,enabled,observed),name,names)));
   }catch(IllegalArgumentException invalid){bad.add(i+1);}
  }
  return new Report(rows,bad);
 }
 private static int iv(String value){String v=value.trim();if(v.equals("?"))return -1;int n=Integer.parseInt(v);if(n< -1||n>15)throw new IllegalArgumentException();return n;}
}
