
package dev.pgrazor;
public class RouteAnchorHarness {
  static void check(boolean ok) { if (!ok) throw new AssertionError(); }
  static double[] ref(double x,double y) { double w=1+.2*x+.3*y; return new double[]{y*.001,x*.001,400*(.2+.5*x+.1*y)/w,800*(.3-.1*x+.4*y)/w}; }
  static String json(double[][] refs,int declared) {
    StringBuilder b=new StringBuilder("{\"count\":"+declared+",\"points\":[");
    for(int i=0;i<refs.length;i++) {
      double[] r=refs[i];
      if(i>0)b.append(',');
      b.append("{\"id\":\"p").append(i).append("\",\"kind\":\"").append(i%2==0?"stop":"gym")
       .append("\",\"lat\":").append(r[0]).append(",\"lon\":").append(r[1])
       .append(",\"x\":").append(r[2]/400.0).append(",\"y\":").append(r[3]/800.0)
       .append(",\"z\":5,\"visible\":true}");
    }
    return b.append("]}").toString();
  }
  public static void main(String[] args) {
    int width=400,height=800;
    double[][] refs={ref(0,0),ref(1,0),ref(0,1),ref(1,1),ref(.3,.4),ref(.8,.6)};
    double[][] normal=new double[6][4];
    for(int i=0;i<6;i++)normal[i]=new double[]{refs[i][0],refs[i][1],refs[i][2]/width,refs[i][3]/height};
    RouteAnchorSnapshot a=RouteAnchorSnapshot.parse(json(normal,6));
    check(a!=null&&a.size()==6);
    RouteGroundProjection p=a.fit(width,height);
    check(p!=null);
    double[] q=ref(.6,.7),s=p.project(q[0],q[1]);
    check(Math.hypot(q[2]-s[0],q[3]-s[1])<2);
    check(RouteAnchorSnapshot.parse(json(normal,7))==null);
    check(RouteAnchorSnapshot.parse(json(normal,6).replace("\"kind\"","\"bad\""))==null);
    double[][] broken=normal.clone();
    for(int i=0;i<broken.length;i++)broken[i]=normal[i].clone();
    broken[5][2]+=10.0/width;
    RouteAnchorSnapshot bad=RouteAnchorSnapshot.parse(json(broken,6));
    check(bad!=null&&bad.fit(width,height)==null);
    String offscreen=json(normal,6).replace("\"visible\":true","\"visible\":false");
    RouteAnchorSnapshot none=RouteAnchorSnapshot.parse(offscreen);
    check(none!=null&&none.size()==0);
    System.out.println("PASS auto anchor snapshot");
  }
}
