import dev.pgrazor.*;
import java.util.*;
public final class LocalAuditTest {
 static void check(boolean b){if(!b)throw new AssertionError();}
 // Independent type-name fixture for the local, advisory-only calculator.
 // Each row: attack type, weaknesses, resistances, immunities (GO double resistance).
 // IDs are resolved by the public Portuguese names rather than copied matrix entries.
 private static void typeChartRegression(){
  String[][] cases={
   {"Normal","","Pedra,Aço","Fantasma"},
   {"Lutador","Normal,Pedra,Aço,Gelo,Sombrio","Voador,Venenoso,Inseto,Psíquico,Fada","Fantasma"},
   {"Voador","Lutador,Inseto,Planta","Pedra,Aço,Elétrico",""},
   {"Venenoso","Planta,Fada","Venenoso,Terrestre,Pedra,Fantasma","Aço"},
   {"Terrestre","Venenoso,Pedra,Aço,Fogo,Elétrico","Inseto,Planta","Voador"},
   {"Pedra","Voador,Inseto,Fogo,Gelo","Lutador,Terrestre,Aço",""},
   {"Inseto","Planta,Psíquico,Sombrio","Lutador,Voador,Venenoso,Fantasma,Aço,Fogo,Fada",""},
   {"Fantasma","Fantasma,Psíquico","Sombrio","Normal"},
   {"Aço","Pedra,Gelo,Fada","Aço,Fogo,Água,Elétrico",""},
   {"Fogo","Inseto,Aço,Planta,Gelo","Pedra,Fogo,Água,Dragão",""},
   {"Água","Terrestre,Pedra,Fogo","Água,Planta,Dragão",""},
   {"Planta","Terrestre,Pedra,Água","Voador,Venenoso,Inseto,Aço,Fogo,Planta,Dragão",""},
   {"Elétrico","Voador,Água","Planta,Elétrico,Dragão","Terrestre"},
   {"Psíquico","Lutador,Venenoso","Aço,Psíquico","Sombrio"},
   {"Gelo","Voador,Terrestre,Planta,Dragão","Aço,Fogo,Água,Gelo",""},
   {"Dragão","Dragão","Aço","Fada"},
   {"Sombrio","Fantasma,Psíquico","Lutador,Sombrio,Fada",""},
   {"Fada","Lutador,Dragão,Sombrio","Venenoso,Aço,Fogo",""}
  };
  String[] types=LocalAuditPolicy.TYPES;
  check(types.length==19&&cases.length==18);
  Set<String> attackers=new HashSet<>();
  for(String[] row:cases){
   int attack=Arrays.asList(types).indexOf(row[0]);
   check(attack>0&&attackers.add(row[0]));
   for(int a=1;a<types.length;a++){
    double first=expectedType(row,types[a]);
    assertType(first,LocalAuditPolicy.effectiveness(attack,a,0),row[0]+" / "+types[a]);
    for(int b=a+1;b<types.length;b++){
     double expected=first*expectedType(row,types[b]);
     assertType(expected,LocalAuditPolicy.effectiveness(attack,a,b),row[0]+" / "+types[a]+"+"+types[b]);
     assertType(expected,LocalAuditPolicy.effectiveness(attack,b,a),row[0]+" / "+types[b]+"+"+types[a]);
    }
    check(Double.isNaN(LocalAuditPolicy.effectiveness(attack,a,a)));
   }
   check(Double.isNaN(LocalAuditPolicy.effectiveness(attack,0,0)));
  }
  check(Double.isNaN(LocalAuditPolicy.effectiveness(0,1,0)));
  check(Double.isNaN(LocalAuditPolicy.effectiveness(19,1,0)));
  check(Double.isNaN(LocalAuditPolicy.effectiveness(1,1,19)));
 }
 private static double expectedType(String[] row,String defender){
  if(Arrays.asList(row[1].split(",")).contains(defender))return 1.6;
  if(Arrays.asList(row[2].split(",")).contains(defender))return 0.625;
  if(Arrays.asList(row[3].split(",")).contains(defender))return 0.390625;
  return 1.0;
 }
 private static void assertType(double expected,double actual,String label){
  if(!Double.isFinite(actual)||Math.abs(expected-actual)>1e-9)
   throw new AssertionError(label+" expected "+expected+", got "+actual);
 }
 public static void main(String[] args){
 typeChartRegression();
 var flags=EnumSet.of(LocalAuditPolicy.Flag.EVENT,LocalAuditPolicy.Flag.SHINY);
 var data=new EnumMap<LocalAuditPolicy.Flag,Boolean>(LocalAuditPolicy.Flag.class);
 data.put(LocalAuditPolicy.Flag.EVENT,true);data.put(LocalAuditPolicy.Flag.SHINY,false);
 check(LocalAuditPolicy.reason(12).equals("Pokémon de evento"));
 check(LocalAuditPolicy.battleReason("actor-unknown").contains("não identificado"));
 check(LocalAuditPolicy.battleReason("unavailable-pair").contains("Não prova"));
 check(LocalAuditPolicy.battleReason("secret-input").equals("Motivo não reconhecido; preserve o código técnico para análise."));
 check(LocalAuditPolicy.preview(4,4,5,99,flags,data).status==LocalAuditPolicy.Status.PROTECTED);
 data.put(LocalAuditPolicy.Flag.EVENT,false);
 check(LocalAuditPolicy.preview(4,4,5,99,flags,data).status==LocalAuditPolicy.Status.MATCH);
 data.remove(LocalAuditPolicy.Flag.EVENT);
 check(LocalAuditPolicy.preview(4,4,5,99,flags,data).status==LocalAuditPolicy.Status.UNCERTAIN);
 check(LocalAuditPolicy.preview(15,15,15,99,flags,data).status==LocalAuditPolicy.Status.PROTECTED);
 check(LocalAuditPolicy.preview(15,15,14,99,EnumSet.noneOf(LocalAuditPolicy.Flag.class),data).status==LocalAuditPolicy.Status.MATCH);
 check(LocalAuditPolicy.preview(-1,15,15,99,flags,data).status==LocalAuditPolicy.Status.UNCERTAIN);
 check(LocalAuditPolicy.preview(0,0,0,99,EnumSet.of(LocalAuditPolicy.Flag.NUNDO),data).status==LocalAuditPolicy.Status.PROTECTED);
 // With partial data, the lower bound alone can prove a protection threshold.
 var none=EnumSet.noneOf(LocalAuditPolicy.Flag.class);
 for(int known=0;known<=15;known++)for(int other=0;other<=15;other++){
  for(int min=0;min<=101;min++){
   var expected=(known+other)*100>=min*45&&min<=100?LocalAuditPolicy.Status.PROTECTED:LocalAuditPolicy.Status.UNCERTAIN;
   for(int[] partialIv:new int[][]{{-1,known,other},{known,-1,other},{known,other,-1}}){
    var decision=LocalAuditPolicy.preview(partialIv[0],partialIv[1],partialIv[2],min,none,Collections.emptyMap());
    check(decision.status==expected&&decision.reasons.contains("IV não confirmado"));
   }
  }
 }
 check(LocalAuditPolicy.preview(15,-1,-1,33,none,Collections.emptyMap()).status==LocalAuditPolicy.Status.PROTECTED);
 check(LocalAuditPolicy.preview(15,-1,-1,34,none,Collections.emptyMap()).status==LocalAuditPolicy.Status.UNCERTAIN);
 check(LocalAuditPolicy.preview(-1,-1,-1,101,none,Collections.emptyMap()).status==LocalAuditPolicy.Status.UNCERTAIN);
 // Every optional protection must preserve a positive observation, and absent facts remain uncertain.
 for(LocalAuditPolicy.Flag f:LocalAuditPolicy.Flag.values()){
  var facts=new EnumMap<LocalAuditPolicy.Flag,Boolean>(LocalAuditPolicy.Flag.class);facts.put(f,true);
  int v=f==LocalAuditPolicy.Flag.HUNDO?15:f==LocalAuditPolicy.Flag.NUNDO?0:4;
  check(LocalAuditPolicy.preview(v,v,v,99,EnumSet.of(f),facts).status==LocalAuditPolicy.Status.PROTECTED);
  check(LocalAuditPolicy.preview(-1,-1,-1,99,EnumSet.of(f),Collections.emptyMap()).status==LocalAuditPolicy.Status.UNCERTAIN);
  if(f!=LocalAuditPolicy.Flag.HUNDO&&f!=LocalAuditPolicy.Flag.NUNDO){
   facts.put(f,false);
   check(LocalAuditPolicy.preview(4,4,4,101,EnumSet.of(f),facts).status==LocalAuditPolicy.Status.MATCH);
   facts.put(f,null);
   check(LocalAuditPolicy.preview(4,4,4,101,EnumSet.of(f),facts).status==LocalAuditPolicy.Status.UNCERTAIN);
  }
 }
 var contradictory=new EnumMap<LocalAuditPolicy.Flag,Boolean>(LocalAuditPolicy.Flag.class);
 contradictory.put(LocalAuditPolicy.Flag.HUNDO,true);
 check(LocalAuditPolicy.preview(4,4,5,99,EnumSet.noneOf(LocalAuditPolicy.Flag.class),contradictory).status==LocalAuditPolicy.Status.UNCERTAIN);
 check(LocalAuditPolicy.preview(-1,14,15,101,EnumSet.noneOf(LocalAuditPolicy.Flag.class),contradictory).reasons.contains("Marcação IV 100% contradiz o IV parcial informado"));
 contradictory.clear();contradictory.put(LocalAuditPolicy.Flag.NUNDO,true);
 check(LocalAuditPolicy.preview(1,-1,0,101,EnumSet.noneOf(LocalAuditPolicy.Flag.class),contradictory).reasons.contains("Marcação IV 0% contradiz o IV parcial informado"));
 // Positive extreme-IV flags remain protective in the local preview, while
 // contradictory observations are still explained and absent data stays unknown.
 for(LocalAuditPolicy.Flag extreme:EnumSet.of(LocalAuditPolicy.Flag.HUNDO,LocalAuditPolicy.Flag.NUNDO)){
  int extremeIv=extreme==LocalAuditPolicy.Flag.HUNDO?15:0;
  int conflictingIv=extreme==LocalAuditPolicy.Flag.HUNDO?14:1;
  var extremeFacts=new EnumMap<LocalAuditPolicy.Flag,Boolean>(LocalAuditPolicy.Flag.class);
  extremeFacts.put(extreme,true);
  var partialPositive=LocalAuditPolicy.preview(-1,extremeIv,extremeIv,101,EnumSet.of(extreme),extremeFacts);
  check(partialPositive.status==LocalAuditPolicy.Status.PROTECTED&&partialPositive.reasons.contains("IV não confirmado")&&partialPositive.reasons.contains(extreme.label));
  var inconsistent=LocalAuditPolicy.preview(conflictingIv,extremeIv,extremeIv,101,EnumSet.of(extreme),extremeFacts);
  check(inconsistent.status==LocalAuditPolicy.Status.PROTECTED&&inconsistent.reasons.stream().anyMatch(reason->reason.contains("contradiz")));
  extremeFacts.put(extreme,false);
  check(LocalAuditPolicy.preview(-1,extremeIv,extremeIv,101,EnumSet.of(extreme),extremeFacts).status==LocalAuditPolicy.Status.UNCERTAIN);
  check(LocalAuditPolicy.preview(extremeIv,extremeIv,extremeIv,101,EnumSet.of(extreme),extremeFacts).status==LocalAuditPolicy.Status.PROTECTED);
 }
 contradictory.clear();contradictory.put(LocalAuditPolicy.Flag.XXL,true);contradictory.put(LocalAuditPolicy.Flag.XXS,true);
 check(LocalAuditPolicy.preview(4,4,5,99,EnumSet.noneOf(LocalAuditPolicy.Flag.class),contradictory).status==LocalAuditPolicy.Status.UNCERTAIN);
 check(LocalAuditPolicy.preview(15,15,15,99,EnumSet.noneOf(LocalAuditPolicy.Flag.class),contradictory).status==LocalAuditPolicy.Status.PROTECTED);
 check(Math.abs(LocalAuditPolicy.effectiveness(11,5,6)-2.56)<1e-8);
 check(Math.abs(LocalAuditPolicy.effectiveness(11,10,12)-1)<1e-8);
 check(Double.isNaN(LocalAuditPolicy.effectiveness(11,0,0)));
 var matchup=LocalAuditPolicy.matchup(11,0,11,10,6,10);
 check(matchup!=null&&Math.abs(matchup.outgoing-2.56)<1e-8&&Math.abs(matchup.incoming-0.625)<1e-8);
 check(LocalAuditPolicy.matchup(11,11,11,10,6,10)==null);
 check(LocalAuditPolicy.matchup(11,0,11,10,6,0)==null);
 var partial=LocalAuditPolicy.partialMatchup(11,0,11,10,6,0);
 check(Math.abs(partial.outgoing-2.56)<1e-8&&Double.isNaN(partial.incoming));
 var reverse=LocalAuditPolicy.partialMatchup(11,0,0,10,6,10);
 check(Double.isNaN(reverse.outgoing)&&Math.abs(reverse.incoming-0.625)<1e-8);
 var invalid=LocalAuditPolicy.partialMatchup(11,11,11,10,10,10);
 check(Double.isNaN(invalid.outgoing)&&Double.isNaN(invalid.incoming));
 check(LocalAuditPolicy.maxBudget(-1,250)==LocalAuditPolicy.BudgetState.UNKNOWN);
 check(LocalAuditPolicy.maxBudget(249,250)==LocalAuditPolicy.BudgetState.INSUFFICIENT);
 check(LocalAuditPolicy.maxBudget(250,250)==LocalAuditPolicy.BudgetState.SUFFICIENT);
 try{LocalAuditPolicy.maxBudget(-2,250);throw new AssertionError();}catch(IllegalArgumentException expected){}
 var a=LocalAuditPolicy.estimate(11,40,70,11,0,10,0,100,100,100);
 var b=LocalAuditPolicy.estimate(1,100,35,11,0,10,0,100,100,100);
 check(a.effectiveness>1&&LocalAuditPolicy.recommend(a,b)==1);
 var strong=LocalAuditPolicy.estimate(11,100,80,11,0,10,0,100,100,100);
 var cheap=LocalAuditPolicy.estimate(1,80,35,11,0,10,0,100,100,100);
 check(strong.damage>cheap.damage);
 check(LocalAuditPolicy.compareContext(strong,cheap,LocalAuditPolicy.Objective.IMMEDIATE_DAMAGE,1,1,1)==0);
 check(LocalAuditPolicy.compareContext(strong,cheap,LocalAuditPolicy.Objective.DAMAGE_PER_SECOND,5,1,1)==1);
 check(LocalAuditPolicy.compareContext(strong,cheap,LocalAuditPolicy.Objective.FINISH_TARGET,1,1,30)==1);
 check(LocalAuditPolicy.compareContext(strong,cheap,LocalAuditPolicy.Objective.FINISH_TARGET,1,1,70)==0);
 check(LocalAuditPolicy.compareContext(strong,cheap,LocalAuditPolicy.Objective.FINISH_TARGET,1,1,0)==-1);
 check(LocalAuditPolicy.compareContext(strong,cheap,LocalAuditPolicy.Objective.DAMAGE_PER_SECOND,0,1,1)==-1);
 check(LocalAuditPolicy.recommend(a,null)==-1);
 check(LocalAuditPolicy.recommendSingle(a)==0&&LocalAuditPolicy.recommendSingle(null)==-1);
 var c=LocalAuditPolicy.estimate(1,100,35,11,0,10,0,100,100,0);check(LocalAuditPolicy.recommend(a,c)==0&&LocalAuditPolicy.recommendSingle(c)==-1);
 var g=new GpxPreviewGeometry(new GpxRoute("teste",new double[][]{{0,179.9},{0,-179.9}}));
 check(g.meters>22000&&g.meters<22500&&g.width<1);
 check(Math.abs(g.positionAt(g.meters/2)[0]-g.width/2)<1e-8);
 check(g.positionAt(0)[0]==g.x[0]&&g.positionAt(g.meters)[0]==g.x[1]);
 check(g.distanceForPercent(100)==g.meters&&g.distanceForPercent(0)==0);
 check(g.percentForDistance(g.meters)==100&&g.percentForDistance(g.meters*0.999)==99);
 // Every slider threshold must round-trip exactly, without rounding up a value just below it.
 for(GpxPreviewGeometry geometry:Arrays.asList(g,new GpxPreviewGeometry(new GpxRoute("curto",new double[][]{{0,0},{0,0.000001}})),
   new GpxPreviewGeometry(new GpxRoute("longo",new double[][]{{-80,0},{0,90},{80,180}})))){
  double limit=geometry.meters/1000.0;
  check(geometry.distanceFromKm(limit)==geometry.meters);
  check(geometry.distanceFromKm(Math.nextDown(limit))<=geometry.meters);
  try{geometry.distanceFromKm(Math.nextUp(limit));throw new AssertionError("Distance beyond exact route endpoint accepted");}catch(IllegalArgumentException expected){}
  for(int percent=0;percent<=100;percent++){
   double threshold=geometry.distanceForPercent(percent);
   check(geometry.percentForDistance(threshold)==percent);
   if(percent>0)check(geometry.percentForDistance(Math.nextDown(threshold))==percent-1);
  }
 }

 check(g.distanceFromKm(1.234)==1234.0);
 try{g.distanceFromKm(Double.POSITIVE_INFINITY);throw new AssertionError();}catch(IllegalArgumentException expected){}
 try{g.distanceFromKm(g.meters/1000+1);throw new AssertionError();}catch(IllegalArgumentException expected){}
 try{g.distanceForPercent(101);throw new AssertionError();}catch(IllegalArgumentException expected){}
 var repeated=new GpxPreviewGeometry(new GpxRoute("repetido",new double[][]{{0,0},{0,0},{0,1},{0,1}}));
 check(Math.abs(repeated.positionAt(repeated.meters/2)[0]-0.5)<1e-8);
 try{g.positionAt(Double.NaN);throw new AssertionError();}catch(IllegalArgumentException expected){}
 var single=new GpxPreviewGeometry(new GpxRoute("um ponto",new double[][]{{0,0}}));check(single.meters==0&&single.x.length==1&&single.percentForDistance(0)==0);
 check(single.distanceFromKm(0)==0);
 try{single.distanceFromKm(Math.nextUp(0.0));throw new AssertionError("Positive distance on zero-length route accepted");}catch(IllegalArgumentException expected){}
 // Invisible whitespace must never become a classified species or tag.
 for(String blank:new String[]{"\u00a0","\u2003","\u202f"," \u00a0\u2003 "}) {
  try{LocalAuditPolicy.tagCatalog(blank);throw new AssertionError("Blank Unicode species");}
  catch(IllegalArgumentException expected){}
  check(LocalCollectionPreview.parse(blank+";1;0;0;0;",99,flags).rejectedLines.equals(Arrays.asList(1)));
 }
 var batch=LocalCollectionPreview.parse("Rattata;478;4;4;5;EVENT=1,SHINY=0\nRhyhorn;547;6;6;7;EVENT=0,SHINY=0\nMunna;14;?;6;7;EVENT=?,SHINY=0\ninvalid\nPikachu;606;15;10;10;EVENT=0,EVENT=1",99,flags);
 check(batch.protectedCount==1&&batch.uncertainCount==1&&batch.matchCount==1&&batch.rejectedLines.equals(Arrays.asList(4,5)));
 check(batch.rejectedReasons.get(4).contains("seis campos")&&batch.rejectedReasons.get(5).equals("Critério repetido."));
 var malformed=LocalCollectionPreview.parse("\uFEFFX;1;0;0;0;EVENT=0,SHINY=0\nX;bad;0;0;0;\nX;1;16;0;0;\nX;1;0;0;0;UNKNOWN=1\nX;1;0;0;0;EVENT=2",99,flags);
 check(malformed.matchCount==1&&malformed.rejectedLines.equals(Arrays.asList(2,3,4,5)));
 check(malformed.rejectedReasons.get(2).startsWith("PC inválido")&&malformed.rejectedReasons.get(3).startsWith("IV inválido")&&malformed.rejectedReasons.get(4).equals("Critério não reconhecido.")&&malformed.rejectedReasons.get(5).startsWith("Valor de critério inválido"));
 try{malformed.rejectedReasons.clear();throw new AssertionError();}catch(UnsupportedOperationException expected){}
 try{LocalCollectionPreview.parse(" ".repeat(LocalCollectionPreview.MAX_CHARS+1),99,flags);throw new AssertionError();}catch(IllegalArgumentException expected){}
 Thread.currentThread().interrupt();
 try{LocalCollectionPreview.parse("X;1;0;0;0;",99,flags);throw new AssertionError("Canceled collection preview");}
 catch(java.util.concurrent.CancellationException expected){}finally{Thread.interrupted();}
 var sparse=LocalCollectionPreview.parse("\r\n".repeat(30000)+"X;1;0;0;0;EVENT=0,SHINY=0\r\n\rY;1;0;0;0;EVENT=0,SHINY=0\n",99,flags);
 check(sparse.matchCount==2&&sparse.rows.get(0).sourceLine==30001&&sparse.rows.get(1).sourceLine==30003);
 check(LocalCollectionPreview.parse("\n".repeat(LocalCollectionPreview.MAX_CHARS),99,flags).rows.isEmpty());
 check(LocalCollectionPreview.parse("X;1;0;0;0;EVENT=0,SHINY=0;extra",99,flags).rejectedLines.equals(Arrays.asList(1)));
 check(LocalCollectionPreview.parse("X;1;0;0;0;EVENT=0=1",99,flags).rejectedLines.equals(Arrays.asList(1)));
 check(batch.rows.get(1).sourceLine==2&&Double.isNaN(batch.rows.get(2).ivPercent()));
 check(Math.abs(batch.rows.get(0).ivPercent()-13*100.0/45)<1e-9);
 check(LocalCollectionPreview.page(batch,LocalAuditPolicy.Status.MATCH,0).get(0).sourceLine==2);
 var many=LocalCollectionPreview.parse(String.join("\n",Collections.nCopies(51,"X;1;0;0;0;EVENT=0,SHINY=0")),99,flags);
 check(LocalCollectionPreview.pageCount(many,null)==3&&LocalCollectionPreview.page(many,null,2).size()==1&&LocalCollectionPreview.page(many,null,2).get(0).sourceLine==51);
 check(LocalCollectionPreview.page(many,LocalAuditPolicy.Status.PROTECTED,0).isEmpty());
 try{LocalCollectionPreview.page(many,null,3);throw new AssertionError();}catch(IllegalArgumentException expected){}
 check(LocalCollectionPreview.parse("X;1;0;0;0;EVENT=0",99,flags).uncertainCount==1);
 var protectedNames=LocalAuditPolicy.protectedNames("Mew; Pikachu, MEW");
 check(protectedNames.size()==2);
 check(LocalAuditPolicy.protectedNames("Pok\u00E9mon,Poke\u0301mon").size()==1);
 try{LocalAuditPolicy.protectedNames("Mew\uDB40\uDC01");throw new AssertionError();}catch(IllegalArgumentException expected){}
 try{LocalAuditPolicy.tagCatalog("Mew\uD800");throw new AssertionError();}catch(IllegalArgumentException expected){}
 check(LocalAuditPolicy.tagCatalog("Mew\uD83D\uDE00").size()==1);
 var custom=LocalCollectionPreview.parse("mew;1;0;0;0;SPECIES=0\nMewtwo;1;0;0;0;SPECIES=0\nPikachu;1;?;0;0;",101,EnumSet.noneOf(LocalAuditPolicy.Flag.class),protectedNames);
 check(custom.protectedCount==2&&custom.matchCount==1&&custom.rows.get(2).decision.reasons.contains("IV não confirmado"));
 check(LocalAuditPolicy.preview(15,15,15,101,EnumSet.of(LocalAuditPolicy.Flag.HUNDO),Collections.emptyMap()).status==LocalAuditPolicy.Status.PROTECTED);
 check(LocalAuditPolicy.preview(15,15,15,101,EnumSet.noneOf(LocalAuditPolicy.Flag.class),Collections.emptyMap()).status==LocalAuditPolicy.Status.MATCH);
 check(LocalAuditPolicy.tagCatalog("PvP\r\nRaro\npvp\nA, B").equals(Arrays.asList("PvP","Raro","A, B")));
 check(LocalAuditPolicy.reviewTags(Collections.singleton("PvP"),Collections.singleton("pvp"),false)==LocalAuditPolicy.TagObservation.MATCH);
 check(LocalAuditPolicy.reviewTags(Collections.singleton("PvP"),Collections.emptySet(),false)==LocalAuditPolicy.TagObservation.UNKNOWN);
 check(LocalAuditPolicy.reviewTags(Collections.singleton("PvP"),Collections.emptySet(),true)==LocalAuditPolicy.TagObservation.NO_MATCH);
 check(LocalAuditPolicy.reviewTags(Collections.emptySet(),Collections.emptySet(),true)==LocalAuditPolicy.TagObservation.UNKNOWN);
 check(LocalAuditPolicy.reviewTags(Collections.singleton("A, B"),Collections.singleton("A"),true)==LocalAuditPolicy.TagObservation.NO_MATCH);
 try{LocalAuditPolicy.tagCatalog(String.join("\n",Collections.nCopies(201,"X")));throw new AssertionError();}catch(IllegalArgumentException expected){}
 check(LocalAuditPolicy.reviewStock(new int[]{0,2,3}).index==1);
 check(LocalAuditPolicy.reviewStock(new int[]{-1,2}).earlierUnknown);
 check(LocalAuditPolicy.reviewStock(new int[]{0,-1}).index==-1&&LocalAuditPolicy.reviewStock(new int[]{0,-1}).earlierUnknown);
 check(!LocalAuditPolicy.reviewStock(new int[]{3,-1}).earlierUnknown);
 try{LocalAuditPolicy.reviewStock(new int[]{2,-2});throw new AssertionError();}catch(IllegalArgumentException expected){}

 check(LocalCollectionPreview.parse("X;1;0;0;0;EVENT=0,SHINY=0\rY;1;0;0;0;EVENT=0,SHINY=0",99,flags).matchCount==2);
 try{LocalCollectionPreview.parse(String.join("\n",Collections.nCopies(501,"X;1;0;0;0;")),99,flags);throw new AssertionError();}catch(IllegalArgumentException expected){}
 var plan=LocalAuditPolicy.planEnergy(24,24,8,40,4);check(Arrays.equals(plan.sequence,new int[]{2,5,5,5})&&plan.earliest==2&&plan.latest==2);
 var residual=LocalAuditPolicy.planEnergy(0,0,9,35,4);check(Arrays.equals(residual.sequence,new int[]{4,4,4,4}));
 var range=LocalAuditPolicy.planEnergy(24,64,8,40,4);check(range.earliest==0&&range.latest==2&&range.sequence.length==0);
 check(LocalAuditPolicy.planEnergy(100,100,8,40,2).sequence[1]==0);
 try{LocalAuditPolicy.planEnergy(0,100,0,40,4);throw new AssertionError();}catch(IllegalArgumentException expected){}
 System.out.println("PASS: local audit");
 }
}
