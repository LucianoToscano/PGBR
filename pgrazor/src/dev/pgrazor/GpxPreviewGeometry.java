package dev.pgrazor;
/** Read-only route geometry. */
public final class GpxPreviewGeometry {
 public final double[] x,y;public final double meters,width,height;
 private final double[] cumulative;
 public GpxPreviewGeometry(GpxRoute route){
 if(route==null)throw new IllegalArgumentException("Rota ausente");
 double[][] pts=route.points();x=new double[pts.length];y=new double[pts.length];cumulative=new double[pts.length];
 double minX=0,maxX=0,minY=0,maxY=0,total=0,lon=pts[0][1],ref=pts[0][0],cos=Math.max(1e-6,Math.cos(Math.toRadians(ref)));
 for(int i=1;i<pts.length;i++){
 double delta=pts[i][1]-pts[i-1][1];while(delta>180)delta-=360;while(delta< -180)delta+=360;
 lon+=delta;x[i]=(lon-pts[0][1])*cos;y[i]=ref-pts[i][0];
 minX=Math.min(minX,x[i]);maxX=Math.max(maxX,x[i]);minY=Math.min(minY,y[i]);maxY=Math.max(maxY,y[i]);
 double a=Math.toRadians(pts[i-1][0]),b=Math.toRadians(pts[i][0]),dl=Math.toRadians(delta);
 double h=Math.pow(Math.sin((b-a)/2),2)+Math.cos(a)*Math.cos(b)*Math.pow(Math.sin(dl/2),2);
 total+=12742000*Math.asin(Math.sqrt(Math.max(0,Math.min(1,h))));cumulative[i]=total;
 }
 for(int i=0;i<x.length;i++){x[i]-=minX;y[i]-=minY;}
 width=maxX-minX;height=maxY-minY;meters=total;
 }
 /** Distance-based preview only. Does not read or change device location. */
 public double distanceAt(int index){return cumulative[index];}
 public double distanceForPercent(int percent){
  if(percent<0||percent>100)throw new IllegalArgumentException("Percentual inválido");
  return meters*(percent/100.0);
 }
 public int percentForDistance(double distance){
  if(!Double.isFinite(distance)||distance<0||distance>meters)throw new IllegalArgumentException("Distância inválida");
  if(meters==0)return 0;
  if(distance==meters)return 100;
  // Compare against the same distance thresholds used by the slider. A division
  // round-off at an exact threshold must not send the thumb one step backwards.
  int lo=0,hi=100;
  while(lo+1<hi){
   int mid=(lo+hi)>>>1;
   if(distanceForPercent(mid)<=distance)lo=mid;else hi=mid;
  }
  return lo;
 }
 public double distanceFromKm(double km){
  if(!Double.isFinite(km)||km<0)throw new IllegalArgumentException("Distância inválida");
  double limit=meters/1000.0;
  if(km>limit)throw new IllegalArgumentException("Distância além do percurso");
  // The exact endpoint in km must round-trip to the route's exact length.
  if(km==limit)return meters;
  return Math.min(meters,km*1000.0);
 }
 public double[] positionAt(double distance){
  if(!Double.isFinite(distance)||distance<0||distance>meters)throw new IllegalArgumentException("Distância inválida");
  if(distance==0)return new double[]{x[0],y[0]};
  if(distance==meters)return new double[]{x[x.length-1],y[y.length-1]};
  int lo=0,hi=cumulative.length-1;
  while(lo+1<hi){int mid=(lo+hi)>>>1;if(cumulative[mid]<=distance)lo=mid;else hi=mid;}
  double span=cumulative[hi]-cumulative[lo],f=span>0?(distance-cumulative[lo])/span:0;
  return new double[]{x[lo]+(x[hi]-x[lo])*f,y[lo]+(y[hi]-y[lo])*f};
 }
}
