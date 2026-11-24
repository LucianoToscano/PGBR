package dev.pgrazor;
import java.util.*;
/** Offline preview of data supplied by the player; no collection access or destructive action. */
public final class LocalCollectionPreview {
 private LocalCollectionPreview(){}
 public static final int MAX_ROWS=500,MAX_CHARS=100000;
 public static final class Row {
  public final String species;public final int cp,attack,defense,stamina;
  public final LocalAuditPolicy.Decision decision;
  Row(String name,int cp,int a,int d,int s,LocalAuditPolicy.Decision decision){species=name;this.cp=cp;attack=a;defense=d;stamina=s;this.decision=decision;}
 }
 public static final class Report {
  public final List<Row> rows;public final List<Integer> rejectedLines;public final int protectedCount,uncertainCount,matchCount;
  Report(List<Row> rows,List<Integer> rejected){this.rows=Collections.unmodifiableList(new ArrayList<>(rows));rejectedLines=Collections.unmodifiableList(new ArrayList<>(rejected));int p=0,u=0,k=0;for(Row r:rows){switch(r.decision.status){case PROTECTED:p++;break;case UNCERTAIN:u++;break;case MATCH:k++;break;}}protectedCount=p;uncertainCount=u;matchCount=k;}
 }
 public static Report parse(String text,int min,Set<LocalAuditPolicy.Flag> enabled){
  if(text==null||text.length()>MAX_CHARS||min<0||min>100||enabled==null)throw new IllegalArgumentException("Dados acima do limite ou configuração inválida.");
  List<Row> rows=new ArrayList<>();List<Integer> bad=new ArrayList<>();
  String[] lines=text.split("\\r?\\n",-1);int count=0;
  for(int i=0;i<lines.length;i++){
   String line=lines[i].trim();if(line.isEmpty())continue;if(++count>MAX_ROWS)throw new IllegalArgumentException("Máximo de 500 registros.");
   try{
    String[] fields=line.split(";",-1);if(fields.length!=6)throw new IllegalArgumentException();
    String name=fields[0].trim();if(name.isEmpty()||name.length()>80)throw new IllegalArgumentException();
    for(int k=0;k<name.length();k++)if(Character.isISOControl(name.charAt(k)))throw new IllegalArgumentException();
    int cp=Integer.parseInt(fields[1].trim()),a=iv(fields[2]),d=iv(fields[3]),s=iv(fields[4]);if(cp<0||cp>100000)throw new IllegalArgumentException();
    EnumMap<LocalAuditPolicy.Flag,Boolean> observed=new EnumMap<>(LocalAuditPolicy.Flag.class);
    if(!fields[5].trim().isEmpty())for(String fact:fields[5].split(",",-1)){
      String[] parts=fact.trim().split("=",-1);if(parts.length!=2)throw new IllegalArgumentException();
      LocalAuditPolicy.Flag f=LocalAuditPolicy.Flag.valueOf(parts[0].trim().toUpperCase(Locale.ROOT));
      String val=parts[1].trim();if(observed.containsKey(f)||(!val.equals("1")&&!val.equals("0")&&!val.equals("?")))throw new IllegalArgumentException();
      observed.put(f,val.equals("?")?null:val.equals("1"));
    }
    rows.add(new Row(name,cp,a,d,s,LocalAuditPolicy.preview(a,d,s,min,enabled,observed)));
   }catch(IllegalArgumentException invalid){bad.add(i+1);}
  }
  return new Report(rows,bad);
 }
 private static int iv(String value){String v=value.trim();if(v.equals("?"))return -1;int n=Integer.parseInt(v);if(n< -1||n>15)throw new IllegalArgumentException();return n;}
}
