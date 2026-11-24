package dev.pgrazor;
/** Local, non-consuming UI gesture recognition. No movement or game actions. */
final class OverlayGesturePolicy {
 private final long[] times={-1,-1,-1};private final float[] xs=new float[3],ys=new float[3];
 private int next;private long last=-1;
 void clear(){java.util.Arrays.fill(times,-1);next=0;}
 boolean down(long time,float x,float y,float apart){
  if(time<0||!Float.isFinite(x)||!Float.isFinite(y)||!Float.isFinite(apart)||apart<=0){clear();return false;}
  if(last>=0&&time<last){clear();last=-1;}
  if(last>=0&&time-last<1000)return false;
  times[next]=time;xs[next]=x;ys[next]=y;next=(next+1)%3;
  for(int i=0;i<3;i++){
   if(times[i]<0||time<times[i]||time-times[i]>250)return false;
   for(int j=i+1;j<3;j++)if(Math.hypot(xs[i]-xs[j],ys[i]-ys[j])<apart)return false;
  }
  return accept(time);
 }
 boolean accept(long time){
  if(time<0||(last>=0&&time>=last&&time-last<1000))return false;
  last=time;clear();return true;
 }
}
