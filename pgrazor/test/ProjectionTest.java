import dev.pgrazor.RouteGroundProjection;
public class ProjectionTest {
  static void check(boolean b){if(!b)throw new AssertionError();}
  static void rejects(Runnable r){try{r.run();throw new AssertionError();}catch(IllegalArgumentException ok){}}
  static double[] ref(double x,double y){double w=1+.2*x+.3*y;return new double[]{y*.001,x*.001,400*(.2+.5*x+.1*y)/w,800*(.3-.1*x+.4*y)/w};}
  public static void main(String[] args){
    double[][] refs={ref(0,0),ref(1,0),ref(0,1),ref(1,1),ref(.3,.4),ref(.8,.6)};
    RouteGroundProjection p=new RouteGroundProjection(refs,400,800);
    double[] q=ref(.6,.7),s=p.project(q[0],q[1]);check(Math.hypot(q[2]-s[0],q[3]-s[1])<1e-6);
    rejects(()->p.project(1,1));rejects(()->p.project(.0012,.0005));refs[5][2]+=10;rejects(()->new RouteGroundProjection(refs,400,800));
    refs[5]=ref(.8,.6);refs[3]=ref(0,1);rejects(()->new RouteGroundProjection(refs,400,800));
    System.out.println("PASS projection synthetic references");
  }
}
