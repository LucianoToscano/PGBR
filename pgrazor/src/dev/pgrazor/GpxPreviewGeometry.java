package dev.pgrazor;
/** Read-only route geometry. */
public final class GpxPreviewGeometry {
 public final double[] x,y;public final double meters,width,height;
 public GpxPreviewGeometry(GpxRoute route){
 if(route==null)throw new IllegalArgumentException("Rota ausente");
 double[][] pts=route.points();x=new double[pts.length];y=new double[pts.length];
 double minX=0,maxX=0,minY=0,maxY=0,total=0,lon=pts[0][1],ref=pts[0][0],cos=Math.max(1e-6,Math.cos(Math.toRadians(ref)));
 for(int i=1;i<pts.length;i++){
 double delta=pts[i][1]-pts[i-1][1];while(delta>180)delta-=360;while(delta< -180)delta+=360;
 lon+=delta;x[i]=(lon-pts[0][1])*cos;y[i]=ref-pts[i][0];
 minX=Math.min(minX,x[i]);maxX=Math.max(maxX,x[i]);minY=Math.min(minY,y[i]);maxY=Math.max(maxY,y[i]);
 double a=Math.toRadians(pts[i-1][0]),b=Math.toRadians(pts[i][0]),dl=Math.toRadians(delta);
 double h=Math.pow(Math.sin((b-a)/2),2)+Math.cos(a)*Math.cos(b)*Math.pow(Math.sin(dl/2),2);
 total+=12742000*Math.asin(Math.sqrt(Math.max(0,Math.min(1,h))));
 }
 for(int i=0;i<x.length;i++){x[i]-=minX;y[i]-=minY;}
 width=maxX-minX;height=maxY-minY;meters=total;
 }
}
