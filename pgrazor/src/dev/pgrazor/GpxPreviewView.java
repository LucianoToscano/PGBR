package dev.pgrazor;
import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.Locale;
/** Local GPX drawing; no location permission or movement. */
final class GpxPreviewView extends View {
 private final GpxPreviewGeometry geo;private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);private final Path path=new Path();
 GpxPreviewView(Activity a,GpxRoute r){super(a);geo=new GpxPreviewGeometry(r);setContentDescription("Trajeto GPX, início e fim");}
 @Override protected void onDraw(Canvas c){
  super.onDraw(c);float pad=MapTheme.dp(getContext(),24);
  double w=Math.max(1,getWidth()-2*pad),h=Math.max(1,getHeight()-2*pad),scale=Math.min(w/Math.max(geo.width,1e-10),h/Math.max(geo.height,1e-10));
  float left=(float)((getWidth()-geo.width*scale)/2),top=(float)((getHeight()-geo.height*scale)/2);path.reset();
  for(int i=0;i<geo.x.length;i++){float x=left+(float)(geo.x[i]*scale),y=top+(float)(geo.y[i]*scale);if(i==0)path.moveTo(x,y);else path.lineTo(x,y);}
  p.setColor(0xff1879ba);p.setStyle(Paint.Style.STROKE);p.setStrokeJoin(Paint.Join.ROUND);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeWidth(MapTheme.dp(getContext(),4));c.drawPath(path,p);
  p.setStyle(Paint.Style.FILL);float r=MapTheme.dp(getContext(),7);int last=geo.x.length-1;
  p.setColor(0xff167749);c.drawCircle(left+(float)(geo.x[0]*scale),top+(float)(geo.y[0]*scale),r,c);
  p.setColor(0xffb14826);c.drawCircle(left+(float)(geo.x[last]*scale),top+(float)(geo.y[last]*scale),r/2,c);
 }
 static void show(Activity a,GpxRoute route){
  if(a.isFinishing()||a.isDestroyed()||route==null)return;
  LinearLayout box=new LinearLayout(MapTheme.wrap(a));box.setOrientation(LinearLayout.VERTICAL);
  GpxPreviewView v=new GpxPreviewView(a,route);box.addView(v,new LinearLayout.LayoutParams(-1,MapTheme.dp(a,250)));
  TextView summary=new TextView(MapTheme.wrap(a));summary.setText(String.format(Locale.forLanguageTag("pt-BR"),"%d pontos • %.2f km\nVerde: início • Laranja: fim\nPrévia esquemática, sem movimentação.",route.size(),v.geo.meters/1000));box.addView(summary);
  AlertDialog d=new AlertDialog.Builder(MapTheme.wrap(a)).setTitle("Prévia GPX — "+route.name).setView(box).setPositiveButton("Fechar",null).create();
  DialogLifecycle.bind(a,d,null);d.setOnShowListener(x->MapTheme.styleButtons(d));d.show();
 }
}
