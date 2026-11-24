import dev.pgrazor.*;
import java.util.*;
public final class LocalAuditTest {
 static void check(boolean b){if(!b)throw new AssertionError();}
 public static void main(String[] args){
 var flags=EnumSet.of(LocalAuditPolicy.Flag.EVENT,LocalAuditPolicy.Flag.SHINY);
 var data=new EnumMap<LocalAuditPolicy.Flag,Boolean>(LocalAuditPolicy.Flag.class);
 data.put(LocalAuditPolicy.Flag.EVENT,true);data.put(LocalAuditPolicy.Flag.SHINY,false);
 check(LocalAuditPolicy.reason(12).equals("Pokémon de evento"));
 check(LocalAuditPolicy.preview(4,4,5,99,flags,data).status==LocalAuditPolicy.Status.PROTECTED);
 data.put(LocalAuditPolicy.Flag.EVENT,false);
 check(LocalAuditPolicy.preview(4,4,5,99,flags,data).status==LocalAuditPolicy.Status.MATCH);
 data.remove(LocalAuditPolicy.Flag.EVENT);
 check(LocalAuditPolicy.preview(4,4,5,99,flags,data).status==LocalAuditPolicy.Status.UNCERTAIN);
 check(LocalAuditPolicy.preview(15,15,15,99,flags,data).status==LocalAuditPolicy.Status.PROTECTED);
 check(LocalAuditPolicy.preview(15,15,14,99,EnumSet.noneOf(LocalAuditPolicy.Flag.class),data).status==LocalAuditPolicy.Status.MATCH);
 check(LocalAuditPolicy.preview(-1,15,15,99,flags,data).status==LocalAuditPolicy.Status.UNCERTAIN);
 check(LocalAuditPolicy.preview(0,0,0,99,EnumSet.of(LocalAuditPolicy.Flag.NUNDO),data).status==LocalAuditPolicy.Status.PROTECTED);
 check(Math.abs(LocalAuditPolicy.effectiveness(11,5,6)-2.56)<1e-8);
 check(Math.abs(LocalAuditPolicy.effectiveness(11,10,12)-1)<1e-8);
 check(Double.isNaN(LocalAuditPolicy.effectiveness(11,0,0)));
 var a=LocalAuditPolicy.estimate(11,40,70,11,0,10,0,100,100,100);
 var b=LocalAuditPolicy.estimate(1,100,35,11,0,10,0,100,100,100);
 check(a.effectiveness>1&&LocalAuditPolicy.recommend(a,b)==1);
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
 System.out.println("PASS: local audit");
 }
}
