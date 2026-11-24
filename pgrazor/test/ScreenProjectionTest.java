import dev.pgrazor.*;
import java.util.*;
public class ScreenProjectionTest {
  static void check(boolean ok,String msg){if(!ok)throw new AssertionError(msg);}
  static void near(double a,double b,double tol){check(Math.abs(a-b)<=tol,a+" != "+b);}
  static final int W=320,H=240;
  static double[] ref(double x,double y){return new double[]{y*.001,x*.001,45+200*x,40+150*y};}
  static int[] image(){int[] p=new int[W*H];Random r=new Random(17);for(int i=0;i<p.length;i++)p[i]=r.nextInt(256);return p;}
  static int[] shift(int[] a,int dx,int dy){int[] b=new int[a.length];for(int y=0;y<H;y++)for(int x=0;x<W;x++){int xx=x-dx,yy=y-dy;if(xx>=0&&xx<W&&yy>=0&&yy<H)b[y*W+x]=a[yy*W+xx];}return b;}
  static int[] warp(int[] a,double angle,double scale){
    int[] b=new int[a.length];double rad=Math.toRadians(angle),c=Math.cos(rad)/scale,s=Math.sin(rad)/scale;
    for(int y=0;y<H;y++)for(int x=0;x<W;x++){
      double dx=x-W/2.0,dy=y-H/2.0,xx=W/2.0+c*dx+s*dy,yy=H/2.0-s*dx+c*dy;
      int ix=(int)Math.floor(xx),iy=(int)Math.floor(yy);double fx=xx-ix,fy=yy-iy;
      if(ix>=0&&iy>=0&&ix+1<W&&iy+1<H)b[y*W+x]=(int)Math.round((1-fy)*((1-fx)*a[iy*W+ix]+fx*a[iy*W+ix+1])+fy*((1-fx)*a[(iy+1)*W+ix]+fx*a[(iy+1)*W+ix+1]));
    }return b;
  }
  static void transformed(int[] image,double[][] refs,double angle,double scale){
    RouteGroundProjection p=new RouteScreenTracker(image,W,H,refs).update(warp(image,angle,scale));
    check(p!=null,"rotation/scale "+angle+"/"+scale);
    double[] q=p.project(.0005,.0005);double dx=145-W/2.0,dy=115-H/2.0,a=Math.toRadians(angle);
    near(q[0],W/2.0+scale*(Math.cos(a)*dx-Math.sin(a)*dy),2);
    near(q[1],H/2.0+scale*(Math.sin(a)*dx+Math.cos(a)*dy),2);
  }
  static double[] perspectiveRef(double x,double y){double w=1+.2*x+.3*y;return new double[]{y*.001,x*.001,400*(.2+.5*x+.1*y)/w,800*(.3-.1*x+.4*y)/w};}
  static void perspectiveProgress(){
    RouteGroundProjection p=new RouteGroundProjection(new double[][]{perspectiveRef(0,0),perspectiveRef(1,0),perspectiveRef(0,1),perspectiveRef(1,1),perspectiveRef(.3,.4),perspectiveRef(.8,.6)},400,800);
    double[][] route={{.0005,0},{.0005,.001}};
    RouteTraceFrame base=RouteScreenFrameBuilder.build(p,route,false,0,1,0,400,800,1,1);
    check(base!=null,"perspective base");
    RouteTraceFrame middle=RouteScreenFrameBuilder.build(p,route,false,base.geometry.totalMeters()/2,1,0,400,800,1,1);
    check(middle!=null&&middle.geometry.size()==3,"perspective split point");
    double[] expected=p.project(.0005,.0005);int[] done={0};
    middle.geometry.drawSegments(middle.completedMeters,(x1,y1,x2,y2,completed)->{if(completed){near(x2,expected[0],1e-5);near(y2,expected[1],1e-5);done[0]++;}});
    check(done[0]==1,"one completed segment");
  }
  public static void main(String[] args){
    perspectiveProgress();
    double[][] refs={ref(0,0),ref(1,0),ref(0,1),ref(1,1),ref(.3,.4),ref(.8,.6)};
    int[] a=image();RouteScreenTracker tracker=new RouteScreenTracker(a,W,H,refs);
    RouteGroundProjection p=tracker.update(shift(a,5,7));check(p!=null,"translation");
    double[] q=p.project(.0005,.0005);near(q[0],150,1e-5);near(q[1],122,1e-5);
    double[] c=p.clipSegment(.0005,-.0005,.0005,.0015);check(c!=null,"clip through hull");near(c[0],.25,1e-8);near(c[1],.75,1e-8);near(c[2],50,1e-5);near(c[4],250,1e-5);
    check(p.clipSegment(.002,0,.002,.001)==null,"outside hull");
    RouteTraceFrame frame=RouteScreenFrameBuilder.build(p,new double[][]{{.0005,-.0005},{.0005,.0015}},false,111.195,1,1000,640,480,2,2);
    check(frame!=null,"build");check(!frame.geometry.startMarker&&!frame.geometry.endMarker,"clipped markers");
    ArrayList<Boolean> progress=new ArrayList<>();frame.geometry.drawSegments(frame.completedMeters,(x1,y1,x2,y2,done)->progress.add(done));check(progress.equals(Arrays.asList(true,false)),"progress at clipped leg");
    RouteTraceFrame pieces=RouteScreenFrameBuilder.build(p,new double[][]{{.0002,.0002},{.0002,.0008},{.003,.0008},{.003,.0002},{.0008,.0002},{.0008,.0008}},false,0,1,1000,W,H,1,1);
    check(pieces!=null,"disconnected pieces");int[] n={0};pieces.geometry.drawSegments(0,(x1,y1,x2,y2,done)->n[0]++);check(n[0]==4,"no bridge: "+n[0]);
    double[][] longRoute=new double[5000][2];for(int i=0;i<5000;i++){longRoute[i][0]=.0005;longRoute[i][1]=.0001+.0008*i/4999.0;}
    RouteTraceFrame longFrame=RouteScreenFrameBuilder.build(p,longRoute,true,0,1,1000,W,H,1,1);check(longFrame!=null&&longFrame.geometry.size()==10000,"5000 point loop");
    transformed(a,refs,8,1);transformed(a,refs,0,1.1);
    check(tracker.update(new int[W*H])==null,"blank screen rejected");check(tracker.update(a)==null,"loss latched");
    System.out.println("PASS: translation, rotation, zoom, perspective progress, clipping, disconnected segments, progress, 5000-point loop, lost landmarks");
  }
}
