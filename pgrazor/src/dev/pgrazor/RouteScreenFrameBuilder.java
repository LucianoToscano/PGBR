package dev.pgrazor;

import java.util.Arrays;

/** Pure adapter: clip a route to the calibrated ground region and retain original progress distances. */
public final class RouteScreenFrameBuilder {
    private static final double R=6371008.8;
    private RouteScreenFrameBuilder(){}
    private static double distance(double[] a,double[] b){
        double dlat=Math.toRadians(b[0]-a[0]),dlon=Math.toRadians(b[1]-a[1]);
        double h=Math.sin(dlat/2)*Math.sin(dlat/2)+Math.cos(Math.toRadians(a[0]))*
            Math.cos(Math.toRadians(b[0]))*Math.sin(dlon/2)*Math.sin(dlon/2);
        return 2*R*Math.asin(Math.sqrt(Math.max(0,Math.min(1,h))));
    }
    public static RouteTraceFrame build(RouteGroundProjection projection,double[][] points,boolean loop,
            double completedMeters,long token,long capturedAt,int width,int height,double sx,double sy) {
        if(projection==null||points==null||points.length<2||points.length>RouteTraceGeometry.MAX_POINTS||
                !Double.isFinite(completedMeters)||completedMeters<0||!Double.isFinite(sx)||!Double.isFinite(sy)||sx<=0||sy<=0)
            return null;
        for(double[] p:points)if(p==null||p.length!=2||!Double.isFinite(p[0])||!Double.isFinite(p[1])||
                Math.abs(p[0])>90||Math.abs(p[1])>180)return null;
        int legs=points.length-1+(loop?1:0),n=0;
        double[] x=new double[legs*2+1],y=new double[legs*2+1],m=new double[legs*2+1];
        boolean[] connect=new boolean[legs*2+1];
        double total=0;boolean first=false,last=false;
        for(int i=0;i<legs;i++){
            double[] a=points[i],b=points[(i+1)%points.length];
            double d=distance(a,b);
            if(d<=1e-8)continue;
            double[] c=projection.clipSegment(a[0],a[1],b[0],b[1]);
            if(c!=null){
                if(n==0)first=i==0&&c[0]<1e-8;
                x[n]=c[2]*sx;y[n]=c[3]*sy;m[n]=total+d*c[0];n++;
                double end=total+d*c[1];
                // Split before projection, not by linear screen distance: perspective changes scale along a leg.
                if(completedMeters>m[n-1]&&completedMeters<end){
                    double t=(completedMeters-total)/d,dl=b[1]-a[1];
                    if(dl>180)dl-=360;else if(dl< -180)dl+=360;
                    double lon=a[1]+dl*t;if(lon>180)lon-=360;else if(lon< -180)lon+=360;
                    try{
                        double[] split=projection.project(a[0]+(b[0]-a[0])*t,lon);
                        x[n]=split[0]*sx;y[n]=split[1]*sy;m[n]=completedMeters;connect[n]=true;n++;
                    }catch(IllegalArgumentException invalid){return null;}
                }
                x[n]=c[4]*sx;y[n]=c[5]*sy;m[n]=end;connect[n]=true;n++;
                last=i==legs-1&&c[1]>1-1e-8;
            }
            total+=d;
        }
        if(n<2)return null;
        try{
            RouteTraceGeometry g=RouteTraceGeometry.clipped(Arrays.copyOf(x,n),Arrays.copyOf(y,n),Arrays.copyOf(m,n),
                Arrays.copyOf(connect,n),total,first,last);
            return new RouteTraceFrame(token,capturedAt,width,height,true,g,Math.min(total,completedMeters));
        }catch(IllegalArgumentException invalid){return null;}
    }
}
