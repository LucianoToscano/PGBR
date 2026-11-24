package dev.pgrazor;
import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.SeekBar;
import java.util.Locale;
/** Local GPX drawing and user-selected progress preview. No location permission or movement. */
final class GpxPreviewView extends View {
 private final GpxPreviewGeometry geo;private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
 private final Path path=new Path(),complete=new Path();
 private float left,top;private double scale,distance;
 GpxPreviewView(Activity a,GpxRoute r){super(a);geo=new GpxPreviewGeometry(r);setContentDescription("Trajeto GPX, início e fim");}
 @Override protected void onSizeChanged(int w,int h,int oldW,int oldH){
  super.onSizeChanged(w,h,oldW,oldH);float pad=MapTheme.dp(getContext(),24);
  scale=Math.min(Math.max(1,w-2*pad)/Math.max(geo.width,1e-10),Math.max(1,h-2*pad)/Math.max(geo.height,1e-10));
  left=(float)((w-geo.width*scale)/2);top=(float)((h-geo.height*scale)/2);
  path.reset();for(int i=0;i<geo.x.length;i++){float x=x(geo.x[i]),y=y(geo.y[i]);if(i==0)path.moveTo(x,y);else path.lineTo(x,y);}rebuildCompleted();
 }
 private float x(double n){return left+(float)(n*scale);}
 private float y(double n){return top+(float)(n*scale);}
 private void rebuildCompleted(){
  complete.reset();complete.moveTo(x(geo.x[0]),y(geo.y[0]));
  for(int i=1;i<geo.x.length&&geo.distanceAt(i)<=distance;i++)complete.lineTo(x(geo.x[i]),y(geo.y[i]));
  double[] point=geo.positionAt(distance);complete.lineTo(x(point[0]),y(point[1]));
 }
 private void setDistance(double meters){distance=meters;rebuildCompleted();invalidate();}
 @Override protected void onDraw(Canvas c){
  super.onDraw(c);p.setStyle(Paint.Style.STROKE);p.setStrokeJoin(Paint.Join.ROUND);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeWidth(MapTheme.dp(getContext(),4));
  p.setColor(0xff7c8991);c.drawPath(path,p);p.setColor(0xff1879ba);c.drawPath(complete,p);
  p.setStyle(Paint.Style.FILL);float r=MapTheme.dp(getContext(),7);int last=geo.x.length-1;
  p.setColor(0xff167749);c.drawCircle(x(geo.x[0]),y(geo.y[0]),r,p);
  p.setColor(0xffb14826);c.drawCircle(x(geo.x[last]),y(geo.y[last]),r/2,p);
  if(distance>0&&distance<geo.meters){double[] point=geo.positionAt(distance);p.setColor(0xff1879ba);c.drawCircle(x(point[0]),y(point[1]),r,p);}
 }
 static void show(Activity a,GpxRoute route){
  if(a==null||a.isFinishing()||a.isDestroyed()||route==null)return;
  LinearLayout box=new LinearLayout(MapTheme.wrap(a));box.setOrientation(LinearLayout.VERTICAL);
  GpxPreviewView v=new GpxPreviewView(a,route);box.addView(v,new LinearLayout.LayoutParams(-1,MapTheme.dp(a,250)));
  TextView summary=new TextView(MapTheme.wrap(a));box.addView(summary);
  SeekBar seek=new SeekBar(MapTheme.wrap(a));seek.setMax(100);seek.setEnabled(v.geo.meters>0);seek.setContentDescription("Simular progresso do percurso");box.addView(seek);
  java.util.function.IntConsumer update=percent->{double d=v.geo.meters*percent/100.0;v.setDistance(d);
   summary.setText(String.format(Locale.forLanguageTag("pt-BR"),"%d pontos • %.2f km\nPrévia: %d%% • %.2f km • restam %.2f km\nVerde: início • Laranja: fim\nProgresso ilustrativo, sem movimentação.",route.size(),v.geo.meters/1000,percent,d/1000,(v.geo.meters-d)/1000));};
  seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar s,int p,boolean user){update.accept(p);}public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}});
  update.accept(0);
  AlertDialog d=new AlertDialog.Builder(MapTheme.wrap(a)).setTitle("Prévia GPX — "+route.name).setView(box).setPositiveButton("Fechar",null).create();
  DialogLifecycle.bind(a,d,null);d.setOnShowListener(x->MapTheme.styleButtons(d));d.show();
 }
}
