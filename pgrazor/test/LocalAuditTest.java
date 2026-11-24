import dev.pgrazor.*;
import java.util.*;
public final class LocalAuditTest {
 static void check(boolean b){if(!b)throw new AssertionError();}
 public static void main(String[] args){
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
 // Every optional protection must preserve a positive observation, and absent facts remain uncertain.
 for(LocalAuditPolicy.Flag f:LocalAuditPolicy.Flag.values()){
  var facts=new EnumMap<LocalAuditPolicy.Flag,Boolean>(LocalAuditPolicy.Flag.class);facts.put(f,true);
  int v=f==LocalAuditPolicy.Flag.HUNDO?15:f==LocalAuditPolicy.Flag.NUNDO?0:4;
  check(LocalAuditPolicy.preview(v,v,v,99,EnumSet.of(f),facts).status==LocalAuditPolicy.Status.PROTECTED);
  check(LocalAuditPolicy.preview(-1,-1,-1,99,EnumSet.of(f),Collections.emptyMap()).status==LocalAuditPolicy.Status.UNCERTAIN);
 }
 var contradictory=new EnumMap<LocalAuditPolicy.Flag,Boolean>(LocalAuditPolicy.Flag.class);
 contradictory.put(LocalAuditPolicy.Flag.HUNDO,true);
 check(LocalAuditPolicy.preview(4,4,5,99,EnumSet.noneOf(LocalAuditPolicy.Flag.class),contradictory).status==LocalAuditPolicy.Status.UNCERTAIN);
 contradictory.clear();contradictory.put(LocalAuditPolicy.Flag.XXL,true);contradictory.put(LocalAuditPolicy.Flag.XXS,true);
 check(LocalAuditPolicy.preview(4,4,5,99,EnumSet.noneOf(LocalAuditPolicy.Flag.class),contradictory).status==LocalAuditPolicy.Status.UNCERTAIN);
 check(LocalAuditPolicy.preview(15,15,15,99,EnumSet.noneOf(LocalAuditPolicy.Flag.class),contradictory).status==LocalAuditPolicy.Status.PROTECTED);
 check(Math.abs(LocalAuditPolicy.effectiveness(11,5,6)-2.56)<1e-8);
 check(Math.abs(LocalAuditPolicy.effectiveness(11,10,12)-1)<1e-8);
 check(Double.isNaN(LocalAuditPolicy.effectiveness(11,0,0)));
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
 var repeated=new GpxPreviewGeometry(new GpxRoute("repetido",new double[][]{{0,0},{0,0},{0,1},{0,1}}));
 check(Math.abs(repeated.positionAt(repeated.meters/2)[0]-0.5)<1e-8);
 try{g.positionAt(Double.NaN);throw new AssertionError();}catch(IllegalArgumentException expected){}
 var single=new GpxPreviewGeometry(new GpxRoute("um ponto",new double[][]{{0,0}}));check(single.meters==0&&single.x.length==1);
 var batch=LocalCollectionPreview.parse("Rattata;478;4;4;5;EVENT=1,SHINY=0\nRhyhorn;547;6;6;7;EVENT=0,SHINY=0\nMunna;14;?;6;7;EVENT=?,SHINY=0\ninvalid\nPikachu;606;15;10;10;EVENT=0,EVENT=1",99,flags);
 check(batch.protectedCount==1&&batch.uncertainCount==1&&batch.matchCount==1&&batch.rejectedLines.equals(Arrays.asList(4,5)));
 check(batch.rows.get(1).sourceLine==2&&Double.isNaN(batch.rows.get(2).ivPercent()));
 check(Math.abs(batch.rows.get(0).ivPercent()-13*100.0/45)<1e-9);
 check(LocalCollectionPreview.page(batch,LocalAuditPolicy.Status.MATCH,0).get(0).sourceLine==2);
 var many=LocalCollectionPreview.parse(String.join("\n",Collections.nCopies(51,"X;1;0;0;0;EVENT=0,SHINY=0")),99,flags);
 check(LocalCollectionPreview.pageCount(many,null)==3&&LocalCollectionPreview.page(many,null,2).size()==1&&LocalCollectionPreview.page(many,null,2).get(0).sourceLine==51);
 check(LocalCollectionPreview.page(many,LocalAuditPolicy.Status.PROTECTED,0).isEmpty());
 try{LocalCollectionPreview.page(many,null,3);throw new AssertionError();}catch(IllegalArgumentException expected){}
 check(LocalCollectionPreview.parse("X;1;0;0;0;EVENT=0",99,flags).uncertainCount==1);
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
