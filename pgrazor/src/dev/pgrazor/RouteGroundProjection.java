package dev.pgrazor;

/** Pure local planar perspective calibration; no camera, game or location access. */
public final class RouteGroundProjection {
    private static final double R=6371008.8, MAX_METERS=1000, MAX_ERROR_PX=2;
    private final double lat0,lon0,cos0;
    private final double[] h;
    private final double[][] anchors=new double[4][];
    private final int width,height;

    /** Six same-frame references [latitude,longitude,pixelX,pixelY]: four fit, two independent checks. */
    public RouteGroundProjection(double[][] refs,int width,int height) {
        if(refs==null||refs.length!=6||width<1||height<1)throw new IllegalArgumentException("Invalid calibration");
        for(double[] r:refs) {
            if(r==null||r.length!=4||!valid(r[0],r[1])||!Double.isFinite(r[2])||!Double.isFinite(r[3])||
                    r[2]<0||r[2]>width||r[3]<0||r[3]>height)throw new IllegalArgumentException("Invalid reference");
        }
        this.width=width;this.height=height;lat0=refs[0][0];lon0=refs[0][1];cos0=Math.cos(Math.toRadians(lat0));
        if(Math.abs(lat0)>75)throw new IllegalArgumentException("Local plane outside supported latitude");
        double[][] a=new double[8][9];
        for(int i=0;i<4;i++) {
            double[] q=local(refs[i][0],refs[i][1]);anchors[i]=q;double x=q[0],y=q[1],u=refs[i][2]/width,v=refs[i][3]/height;
            a[2*i]=new double[]{x,y,1,0,0,0,-u*x,-u*y,u};
            a[2*i+1]=new double[]{0,0,0,x,y,1,-v*x,-v*y,v};
        }
        h=solve(a);
        // Require independent, distinct checks, and reject coincident or implausible samples.
        for(int i=0;i<6;i++)for(int j=0;j<i;j++) {
            double[] p=local(refs[i][0],refs[i][1]),q=local(refs[j][0],refs[j][1]);
            if(Math.hypot(p[0]-q[0],p[1]-q[1])*MAX_METERS<1||
                    Math.hypot(refs[i][2]-refs[j][2],refs[i][3]-refs[j][3])<1)
                throw new IllegalArgumentException("Coincident references");
        }
        for(double[] ref:refs) {
            double[] p=project(ref[0],ref[1]);
            if(Math.hypot(p[0]-ref[2],p[1]-ref[3])>MAX_ERROR_PX)
                throw new IllegalArgumentException("Calibration check failed");
        }
    }
    private static boolean valid(double lat,double lon){return Double.isFinite(lat)&&Double.isFinite(lon)&&Math.abs(lat)<=90&&Math.abs(lon)<=180;}
    private double[] local(double lat,double lon) {
        if(!valid(lat,lon))throw new IllegalArgumentException("Invalid point");
        double dl=lon-lon0;if(dl>180)dl-=360;else if(dl< -180)dl+=360;
        double x=R*Math.toRadians(dl)*cos0/MAX_METERS,y=R*Math.toRadians(lat-lat0)/MAX_METERS;
        if(Math.hypot(x,y)>1)throw new IllegalArgumentException("Outside local calibration radius");
        return new double[]{x,y};
    }
    private static double[] solve(double[][] a) {
        for(int col=0;col<8;col++) {
            int pivot=col;for(int j=col+1;j<8;j++)if(Math.abs(a[j][col])>Math.abs(a[pivot][col]))pivot=j;
            if(Math.abs(a[pivot][col])<1e-9)throw new IllegalArgumentException("Degenerate calibration");
            double[] t=a[col];a[col]=a[pivot];a[pivot]=t;
            double n=a[col][col];for(int k=col;k<9;k++)a[col][k]/=n;
            for(int j=0;j<8;j++)if(j!=col){double f=a[j][col];for(int k=col;k<9;k++)a[j][k]-=f*a[col][k];}
        }
        double[] result=new double[8];for(int i=0;i<8;i++)result[i]=a[i][8];return result;
    }
    private static double cross(double[] a,double[] b,double[] c){
        return (b[0]-a[0])*(c[1]-a[1])-(b[1]-a[1])*(c[0]-a[0]);
    }
    private boolean inside(double[] p){
        for(int i=0;i<4;i++)for(int j=i+1;j<4;j++)for(int k=j+1;k<4;k++){
            double[] a=anchors[i],b=anchors[j],c=anchors[k];double area=cross(a,b,c);
            if(Math.abs(area)<1e-10)continue;
            double u=cross(a,b,p)/area,v=cross(b,c,p)/area,w=cross(c,a,p)/area;
            if(u>=-1e-9&&v>=-1e-9&&w>=-1e-9)return true;
        }
        return false;
    }
    public double[] project(double lat,double lon) {
        return projectLocal(local(lat,lon));
    }
    private double[] projectLocal(double[] p) {
        if(!inside(p))throw new IllegalArgumentException("Outside calibrated reference hull");
        double x=p[0],y=p[1],w=h[6]*x+h[7]*y+1;
        if(!(w>1e-6))throw new IllegalArgumentException("Behind projection plane");
        double u=width*(h[0]*x+h[1]*y+h[2])/w,v=height*(h[3]*x+h[4]*y+h[5])/w;
        if(!Double.isFinite(u)||!Double.isFinite(v)||Math.abs(u)>4.0*width||Math.abs(v)>4.0*height)
            throw new IllegalArgumentException("Outside bounded viewport");
        return new double[]{u,v};
    }
    /** Local planar clipping against the union of triangles covering the calibrated convex hull.
     * Long legs and ambiguous longitude wraps are refused instead of inventing an alignment.
     * Returns [fractionStart,fractionEnd,pixelXStart,pixelYStart,pixelXEnd,pixelYEnd].
     */
    public double[] clipSegment(double latA,double lonA,double latB,double lonB) {
        if(!valid(latA,lonA)||!valid(latB,lonB))return null;
        double[] a=unboundedLocal(latA,lonA),b=unboundedLocal(latB,lonB);
        if(Math.hypot(a[0]-b[0],a[1]-b[1])>20)return null;
        double low=2,high=-1;
        for(int i=0;i<4;i++)for(int j=i+1;j<4;j++)for(int k=j+1;k<4;k++){
            double[] u=anchors[i],v=anchors[j],w=anchors[k];double area=cross(u,v,w);
            if(Math.abs(area)<1e-10)continue;
            double lo=0,hi=1;double[][] tri={u,v,w};
            for(int side=0;side<3&&lo<=hi;side++){
                double[] q=tri[side],t=tri[(side+1)%3];
                double f=cross(q,t,a)/area,g=cross(q,t,b)/area;
                if(f<0&&g<0){lo=1;hi=0;break;}
                if(f<0)lo=Math.max(lo,f/(f-g));
                else if(g<0)hi=Math.min(hi,f/(f-g));
            }
            if(lo<=hi){low=Math.min(low,lo);high=Math.max(high,hi);}
        }
        if(high-low<=1e-10)return null;
        try{
            double[] p=projectLocal(new double[]{a[0]+(b[0]-a[0])*low,a[1]+(b[1]-a[1])*low});
            double[] q=projectLocal(new double[]{a[0]+(b[0]-a[0])*high,a[1]+(b[1]-a[1])*high});
            return new double[]{low,high,p[0],p[1],q[0],q[1]};
        }catch(IllegalArgumentException invalid){return null;}
    }
    private double[] unboundedLocal(double lat,double lon) {
        double dl=lon-lon0;if(dl>180)dl-=360;else if(dl< -180)dl+=360;
        return new double[]{R*Math.toRadians(dl)*cos0/MAX_METERS,R*Math.toRadians(lat-lat0)/MAX_METERS};
    }

}
