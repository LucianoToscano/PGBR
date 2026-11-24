package dev.pgrazor;
import java.util.*;
/** Local advice only. No action or network API. */
public final class LocalAuditPolicy {
 private LocalAuditPolicy(){}
 public static final int SCHEMA=1;
 public enum Flag {
 SHINY("shiny"),HUNDO("IV 100%"),NUNDO("IV 0%"),LEGENDARY("lendário"),MYTHICAL("mítico"),ULTRA("Ultra Criatura"),
 LUCKY("sortudo"),FAVORITE("favorito"),SHADOW("sombrio"),PURIFIED("purificado"),COSTUME("fantasia"),EVENT("evento"),BACKGROUND("fundo especial"),
 XXL("XXL"),XXS("XXS"),LEGACY("ataque exclusivo ou legado"),PVP("relevante para PvP"),TAG("etiqueta protegida"),RESEARCH("pesquisa especial"),SPECIES("espécie protegida");
 public final String label;Flag(String label){this.label=label;}
 }
 public enum Status {PROTECTED,UNCERTAIN,MATCH}
 public static final class Decision {
 public final Status status;public final List<String> reasons;
 Decision(Status s,List<String> r){status=s;reasons=Collections.unmodifiableList(new ArrayList<>(r));}
 }
 public static boolean validIv(int n){return n>=0&&n<=15;}
 public static Decision preview(int a,int d,int s,int min,Set<Flag> enabled,Map<Flag,Boolean> observed){
 if(min<0||min>100||enabled==null||observed==null)throw new IllegalArgumentException("Configuração inválida");
 List<String> reasons=new ArrayList<>(),unknown=new ArrayList<>();
 boolean iv=validIv(a)&&validIv(d)&&validIv(s);
 if(!iv)unknown.add("IV não confirmado");else if((a+d+s)*100>=min*45)reasons.add("IV acima ou igual ao limite");
 for(Flag f:enabled){
 Boolean v=observed.get(f);
 if(f==Flag.HUNDO)v=iv?Boolean.valueOf(a+d+s==45):null;
 if(f==Flag.NUNDO)v=iv?Boolean.valueOf(a+d+s==0):null;
 if(Boolean.TRUE.equals(v))reasons.add(f.label);else if(v==null)unknown.add(f.label+" não confirmado");
 }
 Status status=!reasons.isEmpty()?Status.PROTECTED:!unknown.isEmpty()?Status.UNCERTAIN:Status.MATCH;
 if(status==Status.MATCH)reasons.add("Corresponde ao filtro; nenhuma transferência executada.");
 reasons.addAll(unknown);return new Decision(status,reasons);
 }
 public static String reason(int code){
 switch(code){
 case 0:return "Corresponde ao filtro";case 1:return "Proteção geral ativa";case 2:return "Lista de espécies inválida";
 case 3:return "Espécie não confirmada";case 4:return "Espécie protegida";case 5:return "IV inválido ou ausente";
 case 6:case 7:return "Dados insuficientes para classificação";case 8:return "Favorito ou ovo";
 case 9:return "IV acima ou igual ao limite";case 10:return "Categoria rara";case 11:return "Shiny";case 12:return "Pokémon de evento";
 case 13:return "Fundo especial";case 14:return "XXS";case 15:return "XXL";case 16:return "Contrato inválido";case 17:return "Limite inválido";
 case 18:return "Proteção PvP";case 19:return "IV 0%";case 20:return "Fantasia";default:return "Motivo desconhecido ("+code+")";
 }
 }
 public static final String[] TYPES={"Nenhum","Normal","Lutador","Voador","Venenoso","Terrestre","Pedra","Inseto","Fantasma","Aço","Fogo","Água","Planta","Elétrico","Psíquico","Gelo","Dragão","Sombrio","Fada"};
 private static final int[][] CHART={{  0,  0,  0,  0,  0, -1,  0, -2, -1,  0,  0,  0,  0,  0,  0,  0,  0,  0},
{  1,  0, -1, -1,  0,  1, -1, -2,  1,  0,  0,  0,  0, -1,  1,  0,  1, -1},
{  0,  1,  0,  0,  0, -1,  1,  0, -1,  0,  0,  1, -1,  0,  0,  0,  0,  0},
{  0,  0,  0, -1, -1, -1,  0, -1, -2,  0,  0,  1,  0,  0,  0,  0,  0,  1},
{  0,  0, -2,  1,  0,  1, -1,  0,  1,  1,  0, -1,  1,  0,  0,  0,  0,  0},
{  0, -1,  1,  0, -1,  0,  1,  0, -1,  1,  0,  0,  0,  0,  1,  0,  0,  0},
{  0, -1, -1, -1,  0,  0,  0, -1, -1, -1,  0,  1,  0,  1,  0,  0,  1, -1},
{ -2,  0,  0,  0,  0,  0,  0,  1,  0,  0,  0,  0,  0,  1,  0,  0, -1,  0},
{  0,  0,  0,  0,  0,  1,  0,  0, -1, -1, -1,  0, -1,  0,  1,  0,  0,  1},
{  0,  0,  0,  0,  0, -1,  1,  0,  1, -1, -1,  1,  0,  0,  1, -1,  0,  0},
{  0,  0,  0,  0,  1,  1,  0,  0,  0,  1, -1, -1,  0,  0,  0, -1,  0,  0},
{  0,  0, -1, -1,  1,  1, -1,  0, -1, -1,  1, -1,  0,  0,  0, -1,  0,  0},
{  0,  0,  1,  0, -2,  0,  0,  0,  0,  0,  1, -1, -1,  0,  0, -1,  0,  0},
{  0,  1,  0,  1,  0,  0,  0,  0, -1,  0,  0,  0,  0, -1,  0,  0, -2,  0},
{  0,  0,  1,  0,  1,  0,  0,  0, -1, -1, -1,  1,  0,  0, -1,  1,  0,  0},
{  0,  0,  0,  0,  0,  0,  0,  0, -1,  0,  0,  0,  0,  0,  0,  1,  0, -2},
{  0, -1,  0,  0,  0,  0,  0,  1,  0,  0,  0,  0,  0,  1,  0,  0, -1, -1},
{  0,  1,  0, -1,  0,  0,  0,  0, -1, -1,  0,  0,  0,  0,  0,  1,  1,  0}};
 static boolean validTypes(int a,int b){return a>=1&&a<=18&&b>=0&&b<=18&&a!=b;}
 public static double effectiveness(int move,int a,int b){
 if(move<1||move>18||!validTypes(a,b))return Double.NaN;
 return Math.pow(1.6,CHART[move-1][a-1]+(b==0?0:CHART[move-1][b-1]));
 }
 public static final class Estimate {
 public final double effectiveness,damage,perEnergy;public final boolean ready;
 Estimate(double e,double d,double pe,boolean r){effectiveness=e;damage=d;perEnergy=pe;ready=r;}
 }
 /** Inputs are supplied by user for the chosen mode/form. Excludes shields, buffs, weather and CMP. */
 public static Estimate estimate(int move,double power,double cost,int own1,int own2,int foe1,int foe2,double attack,double defense,double energy){
 if(!validTypes(own1,own2)||!validTypes(foe1,foe2)||!Double.isFinite(attack)||attack<=0||!Double.isFinite(defense)||defense<=0||
 !Double.isFinite(power)||power<=0||!Double.isFinite(cost)||cost<=0||cost>100||!Double.isFinite(energy)||energy<0||energy>100)return null;
 double e=effectiveness(move,foe1,foe2);if(!Double.isFinite(e))return null;
 double d=Math.floor(0.5*power*attack/defense*(move==own1||move==own2?1.2:1)*e)+1;
 if(!Double.isFinite(d))return null;return new Estimate(e,d,d/cost,energy>=cost);
 }
 /** Offline planning with explicit energy bounds; no live battle input. */
 public static final class EnergyPlan {
  public final int earliest,latest;public final int[] sequence;
  EnergyPlan(int a,int b,int[] seq){earliest=a;latest=b;sequence=seq;}
 }
 public static EnergyPlan planEnergy(int minimum,int maximum,int gain,int cost,int steps){
  if(minimum<0||maximum>100||minimum>maximum||gain<1||gain>100||cost<1||cost>100||steps<1||steps>12)throw new IllegalArgumentException("Energia inválida");
  int earliest=fasts(maximum,gain,cost),latest=fasts(minimum,gain,cost);
  // Only an exact starting state yields an exact repeated sequence.
  int[] seq=minimum==maximum?new int[steps]:new int[0];
  int energy=minimum;for(int i=0;i<seq.length;i++){seq[i]=fasts(energy,gain,cost);energy=Math.min(100,energy+seq[i]*gain)-cost;}
  return new EnergyPlan(earliest,latest,seq);
 }
 private static int fasts(int energy,int gain,int cost){return (Math.max(0,cost-energy)+gain-1)/gain;}
 /** Explicit one-move configuration, never inferred from missing data. */
 public static int recommendSingle(Estimate a){return a!=null&&a.ready?0:-1;}
 public static int recommend(Estimate a,Estimate b){
 if(a==null||b==null)return -1;if(!a.ready&&!b.ready)return -1;if(!a.ready)return 1;if(!b.ready)return 0;
 return b.perEnergy>a.perEnergy?1:0;
 }
}
