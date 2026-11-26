package dev.pgrazor;
import java.util.*;
/** Local advice only. No action or network API. */
public final class LocalAuditPolicy {
 private LocalAuditPolicy(){}
 public static final int SCHEMA=3;
 public enum Flag {
 SHINY("brilhante (shiny)"),HUNDO("IV 100%"),NUNDO("IV 0%"),LEGENDARY("lendário"),MYTHICAL("mítico"),ULTRA("Ultra Criatura"),
 LUCKY("sortudo"),FAVORITE("favorito"),SHADOW("sombrio"),PURIFIED("purificado"),COSTUME("fantasia"),EVENT("evento"),BACKGROUND("fundo especial"),
 XXL("XXL"),XXS("XXS"),LEGACY("ataque exclusivo ou legado"),PVP("relevante para PvP"),TAG("etiqueta protegida"),RESEARCH("pesquisa especial"),SPECIES("espécie protegida"),DYNAMAX("Dynamax"),GIGANTAMAX("Gigantamax");
 public final String label;Flag(String label){this.label=label;}
 }
 public enum Status {PROTECTED,UNCERTAIN,MATCH}
 public static final class Decision {
 public final Status status;public final List<String> reasons;
 Decision(Status s,List<String> r){status=s;reasons=Collections.unmodifiableList(new ArrayList<>(r));}
 }
 public static boolean validIv(int n){return n>=0&&n<=15;}
 public static Decision preview(int a,int d,int s,int min,Set<Flag> enabled,Map<Flag,Boolean> observed){
 if(min<0||min>101||enabled==null||observed==null)throw new IllegalArgumentException("Configuração inválida");
 List<String> reasons=new ArrayList<>(),unknown=new ArrayList<>();
 boolean iv=validIv(a)&&validIv(d)&&validIv(s);
 if(!iv){
  unknown.add("IV não confirmado");
  // Known components establish a lower bound, not an estimate of the missing IV.
  int lower=(validIv(a)?a:0)+(validIv(d)?d:0)+(validIv(s)?s:0);
  if(min<=100&&lower*100>=min*45)reasons.add("IV mínimo confirmado acima ou igual ao limite");
 }else if(min<=100&&(a+d+s)*100>=min*45)reasons.add("IV acima ou igual ao limite");
 // Contradictions are uncertainty even if the optional protection is disabled.
 if(iv){
  if(observed.get(Flag.HUNDO)!=null&&observed.get(Flag.HUNDO)!=(a+d+s==45))unknown.add("Marcação IV 100% contradiz o IV informado");
  if(observed.get(Flag.NUNDO)!=null&&observed.get(Flag.NUNDO)!=(a+d+s==0))unknown.add("Marcação IV 0% contradiz o IV informado");
 }else{
  if(Boolean.TRUE.equals(observed.get(Flag.HUNDO))&&((validIv(a)&&a!=15)||(validIv(d)&&d!=15)||(validIv(s)&&s!=15)))unknown.add("Marcação IV 100% contradiz o IV parcial informado");
  if(Boolean.TRUE.equals(observed.get(Flag.NUNDO))&&((validIv(a)&&a!=0)||(validIv(d)&&d!=0)||(validIv(s)&&s!=0)))unknown.add("Marcação IV 0% contradiz o IV parcial informado");
 }
 if(Boolean.TRUE.equals(observed.get(Flag.HUNDO))&&Boolean.TRUE.equals(observed.get(Flag.NUNDO)))unknown.add("Marcações IV 100% e IV 0% incompatíveis");
 if(Boolean.TRUE.equals(observed.get(Flag.XXL))&&Boolean.TRUE.equals(observed.get(Flag.XXS)))unknown.add("Marcações XXL e XXS incompatíveis");
 if(Boolean.TRUE.equals(observed.get(Flag.SHADOW))&&Boolean.TRUE.equals(observed.get(Flag.PURIFIED)))unknown.add("Marcações sombrio e purificado incompatíveis");
 for(Flag f:enabled){
 Boolean v=observed.get(f);
 // An explicit positive observation is a reason to preserve, even if IVs
 // are incomplete or inconsistent. Contradictions remain visible above.
 // A negative observation alone cannot establish an unknown IV.
 if(f==Flag.HUNDO)v=Boolean.TRUE.equals(v)?Boolean.TRUE:iv?Boolean.valueOf(a+d+s==45):null;
 if(f==Flag.NUNDO)v=Boolean.TRUE.equals(v)?Boolean.TRUE:iv?Boolean.valueOf(a+d+s==0):null;
 if(Boolean.TRUE.equals(v))reasons.add(f.label);else if(v==null)unknown.add(f.label+" não confirmado");
 }
 Status status=!reasons.isEmpty()?Status.PROTECTED:!unknown.isEmpty()?Status.UNCERTAIN:Status.MATCH;
 if(status==Status.MATCH)reasons.add("Corresponde ao filtro; nenhuma transferência executada.");
 reasons.addAll(unknown);return new Decision(status,reasons);
 }
 /** A separately selected exact species-name list is an additional protection, not a replacement
  * for the imported SPECIES observation or any other criterion. No fuzzy/form matching. */
 public static Decision withSpeciesProtection(Decision base,String species,Set<String> names){
  Objects.requireNonNull(base);Objects.requireNonNull(names);
  if(!names.contains(speciesKey(species)))return base;
  List<String> reasons=new ArrayList<>();
  reasons.add("Espécie na lista personalizada de proteção");
  if(base.status!=Status.MATCH)reasons.addAll(base.reasons);
  return new Decision(Status.PROTECTED,reasons);
 }
 static String speciesKey(String name){
  if(name==null||name.trim().isEmpty()||name.length()>80
      ||name.codePoints().allMatch(cp->Character.isWhitespace(cp)||Character.isSpaceChar(cp)))
   throw new IllegalArgumentException("Nome de espécie inválido");
  for(int i=0;i<name.length();){
   int cp=name.codePointAt(i),type=Character.getType(cp);
   if(Character.isISOControl(cp)||type==Character.FORMAT||type==Character.SURROGATE)throw new IllegalArgumentException("Nome de espécie inválido");
   i+=Character.charCount(cp);
  }
  String normalized=java.text.Normalizer.normalize(name.trim(),java.text.Normalizer.Form.NFC).toLowerCase(Locale.ROOT);
  return java.text.Normalizer.normalize(normalized,java.text.Normalizer.Form.NFC);
 }
 public static Set<String> protectedNames(String text){
  if(text==null||text.length()>20000)throw new IllegalArgumentException("Lista de espécies inválida");
  Set<String> result=new LinkedHashSet<>();
  int count=0;
  for(String name:text.split("[,;\\r\\n]",-1)){
   if(name.trim().isEmpty())continue;
   if(++count>200)throw new IllegalArgumentException("Máximo de 200 espécies");
   result.add(speciesKey(name.trim()));
  }
  return Collections.unmodifiableSet(result);
 }
 /** Manual tag catalogue. Each line represents one exact name; no inference from missing data. */
 public static List<String> tagCatalog(String raw){
  if(raw==null||raw.length()>20000)throw new IllegalArgumentException("Catálogo de etiquetas inválido");
  LinkedHashMap<String,String> unique=new LinkedHashMap<>();int count=0;
  for(String line:raw.split("\\r\\n|\\n|\\r",-1)){
   String name=line.trim();if(name.isEmpty())continue;
   if(++count>200)throw new IllegalArgumentException("Máximo de 200 etiquetas");
   String key=speciesKey(name);if(!unique.containsKey(key))unique.put(key,name);
  }
  return Collections.unmodifiableList(new ArrayList<>(unique.values()));
 }
 public enum TagObservation { MATCH, NO_MATCH, UNKNOWN }
 public static TagObservation reviewTags(Set<String> protectedTags,Set<String> observedTags,boolean complete){
  Objects.requireNonNull(protectedTags);Objects.requireNonNull(observedTags);
  if(protectedTags.size()>200||observedTags.size()>200)throw new IllegalArgumentException("Limite de etiquetas");
  Set<String> selected=new HashSet<>();
  for(String tag:protectedTags)selected.add(speciesKey(tag));
  Set<String> observed=new HashSet<>();
  for(String tag:observedTags)observed.add(speciesKey(tag));
  if(selected.isEmpty())return TagObservation.UNKNOWN;
  for(String key:observed)if(selected.contains(key))return TagObservation.MATCH;
  return complete?TagObservation.NO_MATCH:TagObservation.UNKNOWN;
 }
 /** Manual stock review only: first confirmed available item in the supplied order.
  * -1 is unknown stock, 0 is unavailable, and a positive value is available.
  * An unknown earlier choice is explicitly reported; no action or inventory read. */
 public static final class StockAdvice {
  public final int index;public final boolean earlierUnknown;
  StockAdvice(int index,boolean earlierUnknown){this.index=index;this.earlierUnknown=earlierUnknown;}
 }
 public static StockAdvice reviewStock(int[] counts){
  if(counts==null||counts.length<1||counts.length>5)throw new IllegalArgumentException("Lista inválida");
  for(int count:counts)if(count< -1||count>100000)throw new IllegalArgumentException("Estoque inválido");
  boolean unknown=false;
  for(int i=0;i<counts.length;i++){
   if(counts[i]>0)return new StockAdvice(i,unknown);
   if(counts[i]<0)unknown=true;
  }
  return new StockAdvice(-1,unknown);
 }
 /** Manual planning, using only the duration and stock confirmed by the user. */
 public static final class BoostPlan {
  public final int needed,available,shortfall;
  BoostPlan(int n,int a){needed=n;available=a;shortfall=a<0?-1:Math.max(0,n-a);}
 }
 public static BoostPlan planBoost(int minutes,int activeMinutes,int duration,int stock){
  if(minutes<0||minutes>1440||activeMinutes<0||activeMinutes>1440||
     duration<1||duration>1440||stock< -1||stock>100000)throw new IllegalArgumentException("Limites inválidos");
  int remaining=Math.max(0,minutes-activeMinutes);
  return new BoostPlan((remaining+duration-1)/duration,stock);
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
 public static String battleReason(String code){
  if(code==null)return "Motivo não informado.";
  switch(code){
   case "actor-unknown":return "Adversário não identificado. A observação não permite confirmar a eficácia.";
   case "actor-refresh":return "Identidade do adversário mudou ou está sendo revalidada.";
   case "stale-effect":return "Classificação antiga ou fora da mesma janela de observação.";
   case "unknown-effect":return "Eficácia de pelo menos um ataque não confirmada.";
   case "unverified-slots":return "Associação entre os dois botões e suas classificações não confirmada.";
   case "unavailable-pair":return "Par de carregados não confirmado. Não prova que haja dois ataques.";
   case "only-one-ready":return "Um dos carregados não estava pronto; em registros antigos também pode significar nenhum pronto. Confira os indicadores da última leitura.";
   case "none-ready":return "Nenhum dos dois botões observados foi confirmado pronto na última leitura; não prova o estado atual nem a quantidade real de golpes.";
   case "effectiveness":return "Classificação por eficácia disponível; não prova execução ou dano.";
   case "known-super":return "Há classificação supereficaz confirmada; a outra pode ser desconhecida.";
   case "off":return "Preferência de Ataque Inteligente desligada.";
   default:return "Motivo não reconhecido; preserve o código técnico para análise.";
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
 /** Type-only preparation: no inference of Max eligibility, move power, or battle outcome. */
 public static final class Matchup {
  public final double outgoing,incoming;
  Matchup(double outgoing,double incoming){this.outgoing=outgoing;this.incoming=incoming;}
 }
 public static Matchup matchup(int own1,int own2,int move,int foe1,int foe2,int foeMove){
  if(!validTypes(own1,own2)||!validTypes(foe1,foe2))return null;
  double outgoing=effectiveness(move,foe1,foe2);
  double incoming=effectiveness(foeMove,own1,own2);
  if(!Double.isFinite(outgoing)||!Double.isFinite(incoming))return null;
  return new Matchup(outgoing,incoming);
 }
 /** Independent known components: missing boss move must not erase a valid offensive type comparison.
  * NaN means unknown/invalid; never infer a neutral multiplier from missing facts. */
 public static Matchup partialMatchup(int own1,int own2,int move,int foe1,int foe2,int foeMove){
  return new Matchup(effectiveness(move,foe1,foe2),effectiveness(foeMove,own1,own2));
 }
 public enum BudgetState { UNKNOWN, INSUFFICIENT, SUFFICIENT }
 public static BudgetState maxBudget(int available,int cost){
  if(available< -1||available>1000000||cost< -1||cost>1000000)throw new IllegalArgumentException("Partículas inválidas");
  if(available==-1||cost==-1)return BudgetState.UNKNOWN;
  return available>=cost?BudgetState.SUFFICIENT:BudgetState.INSUFFICIENT;
 }
 public static final class Estimate {
 public final double effectiveness,damage,perEnergy,energyCost;public final boolean ready;
 Estimate(double e,double d,double cost,boolean r){effectiveness=e;damage=d;perEnergy=d/cost;energyCost=cost;ready=r;}
 }
 /** Inputs are supplied by user for the chosen mode/form. Excludes shields, buffs, weather and CMP. */
 public static Estimate estimate(int move,double power,double cost,int own1,int own2,int foe1,int foe2,double attack,double defense,double energy){
 if(!validTypes(own1,own2)||!validTypes(foe1,foe2)||!Double.isFinite(attack)||attack<=0||!Double.isFinite(defense)||defense<=0||
 !Double.isFinite(power)||power<=0||!Double.isFinite(cost)||cost<=0||cost>100||!Double.isFinite(energy)||energy<0||energy>100)return null;
 double e=effectiveness(move,foe1,foe2);if(!Double.isFinite(e))return null;
 double d=Math.floor(0.5*power*attack/defense*(move==own1||move==own2?1.2:1)*e)+1;
 if(!Double.isFinite(d)||!Double.isFinite(d/cost))return null;return new Estimate(e,d,cost,energy>=cost);
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
 /** Contextual offline estimates; never represents shielded outcomes as guaranteed damage. */
 public enum Objective { EFFICIENCY, IMMEDIATE_DAMAGE, DAMAGE_PER_SECOND, FINISH_TARGET }
 public static double damagePerSecond(Estimate estimate,double seconds){
  return estimate!=null&&Double.isFinite(seconds)&&seconds>0&&Double.isFinite(estimate.damage/seconds)?estimate.damage/seconds:Double.NaN;
 }
 public static int compareContext(Estimate a,Estimate b,Objective objective,double secondsA,double secondsB,double targetHp){
  if(a==null||b==null||objective==null)return -1;
  if(objective==Objective.DAMAGE_PER_SECOND&&(!Double.isFinite(damagePerSecond(a,secondsA))||!Double.isFinite(damagePerSecond(b,secondsB))))return -1;
  if(objective==Objective.FINISH_TARGET&&(!Double.isFinite(targetHp)||targetHp<=0))return -1;
  if(!a.ready&&!b.ready)return -1;if(!a.ready)return 1;if(!b.ready)return 0;
  switch(objective){
   case IMMEDIATE_DAMAGE:return b.damage>a.damage?1:0;
   case DAMAGE_PER_SECOND:return damagePerSecond(b,secondsB)>damagePerSecond(a,secondsA)?1:0;
   case FINISH_TARGET:
    boolean finishA=a.damage>=targetHp,finishB=b.damage>=targetHp;
    if(finishA!=finishB)return finishB?1:0;
    if(finishA)return b.energyCost<a.energyCost?1:0;
    return b.damage>a.damage?1:0;
   default:return recommend(a,b);
  }
 }
 public static int recommend(Estimate a,Estimate b){
 if(a==null||b==null)return -1;if(!a.ready&&!b.ready)return -1;if(!a.ready)return 1;if(!b.ready)return 0;
 return b.perEnergy>a.perEnergy?1:0;
 }
}
