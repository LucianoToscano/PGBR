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
 private float left,top,cursorX,cursorY;private double scale,distance;
 private boolean progressDirty=true;
 GpxPreviewView(Activity a,GpxRoute r){super(a);geo=new GpxPreviewGeometry(r);setContentDescription("Trajeto GPX, início e fim");}
 @Override protected void onSizeChanged(int w,int h,int oldW,int oldH){
  super.onSizeChanged(w,h,oldW,oldH);float pad=MapTheme.dp(getContext(),24);
  scale=Math.min(Math.max(1,w-2*pad)/Math.max(geo.width,1e-10),Math.max(1,h-2*pad)/Math.max(geo.height,1e-10));
  left=(float)((w-geo.width*scale)/2);top=(float)((h-geo.height*scale)/2);
  path.reset();for(int i=0;i<geo.x.length;i++){float x=x(geo.x[i]),y=y(geo.y[i]);if(i==0)path.moveTo(x,y);else path.lineTo(x,y);}progressDirty=true;
 }
 private float x(double n){return left+(float)(n*scale);}
 private float y(double n){return top+(float)(n*scale);}
 private void rebuildCompleted(){
  complete.reset();complete.moveTo(x(geo.x[0]),y(geo.y[0]));
  for(int i=1;i<geo.x.length&&geo.distanceAt(i)<=distance;i++)complete.lineTo(x(geo.x[i]),y(geo.y[i]));
  double[] point=geo.positionAt(distance);cursorX=x(point[0]);cursorY=y(point[1]);complete.lineTo(cursorX,cursorY);progressDirty=false;
 }
 // Coalesce multiple slider updates into one rebuild on the next rendered frame.
 private void setDistance(double meters){if(distance==meters)return;distance=meters;progressDirty=true;postInvalidateOnAnimation();}
 @Override protected void onDraw(Canvas c){
  super.onDraw(c);if(progressDirty)rebuildCompleted();p.setStyle(Paint.Style.STROKE);p.setStrokeJoin(Paint.Join.ROUND);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeWidth(MapTheme.dp(getContext(),4));
  p.setColor(0xff7c8991);c.drawPath(path,p);p.setColor(0xff1879ba);c.drawPath(complete,p);
  p.setStyle(Paint.Style.FILL);float r=MapTheme.dp(getContext(),7);int last=geo.x.length-1;
  p.setColor(0xff167749);c.drawCircle(x(geo.x[0]),y(geo.y[0]),r,p);
  p.setColor(0xffb14826);c.drawCircle(x(geo.x[last]),y(geo.y[last]),r/2,p);
  if(distance>0&&distance<geo.meters){p.setColor(0xff1879ba);c.drawCircle(cursorX,cursorY,r,p);}
 }
 static void show(Activity a,GpxRoute route){
  if(a==null||a.isFinishing()||a.isDestroyed()||route==null)return;
  LinearLayout box=new LinearLayout(MapTheme.wrap(a));box.setOrientation(LinearLayout.VERTICAL);
  GpxPreviewView v=new GpxPreviewView(a,route);box.addView(v,new LinearLayout.LayoutParams(-1,MapTheme.dp(a,250)));
  TextView summary=new TextView(MapTheme.wrap(a));box.addView(summary);
  SeekBar seek=new SeekBar(MapTheme.wrap(a));seek.setMax(100);seek.setEnabled(v.geo.meters>0);seek.setContentDescription("Simular progresso do percurso");box.addView(seek);
  TextView hint=new TextView(MapTheme.wrap(a));hint.setText("Distância percorrida informada manualmente (km)");box.addView(hint);
  android.widget.EditText km=new android.widget.EditText(MapTheme.wrap(a));km.setId(android.view.View.generateViewId());hint.setLabelFor(km.getId());km.setSingleLine(true);km.setInputType(android.text.InputType.TYPE_CLASS_NUMBER|android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);LocalAuditDialog.privateInput(km);box.addView(km);
  android.widget.Button apply=new android.widget.Button(MapTheme.wrap(a));apply.setText("Exibir distância");apply.setAllCaps(false);apply.setTextColor(MapTheme.ink());MapTheme.setButtonBackground(apply);box.addView(apply);
  android.widget.Button finish=new android.widget.Button(MapTheme.wrap(a));finish.setText("Exibir percurso completo");finish.setAllCaps(false);finish.setTextColor(MapTheme.ink());MapTheme.setButtonBackground(finish);box.addView(finish);
  final boolean[] syncing={false};
  final String[] displayedDistance={""};
  java.util.function.DoubleConsumer update=distance->{
   v.setDistance(distance);syncing[0]=true;
   try{seek.setProgress(v.geo.percentForDistance(distance));}finally{syncing[0]=false;}
   // Round down to the displayed precision so reapplying the field never exceeds route length.
   displayedDistance[0]=java.math.BigDecimal.valueOf(distance/1000).setScale(6,java.math.RoundingMode.DOWN).toPlainString().replace('.',',');
   if(!displayedDistance[0].contentEquals(km.getText()))km.setText(displayedDistance[0]);
   km.setError(null);
   String progress=v.geo.meters==0?"Sem extensão mensurável":String.format(Locale.forLanguageTag("pt-BR"),"%.2f%%",100*(distance/v.geo.meters));
   summary.setText(String.format(Locale.forLanguageTag("pt-BR"),"%d pontos • %.3f km\nPrévia: %s • %.3f km • restam %.3f km\nVerde: início • Laranja: fim • Azul: trecho percorrido\nProgresso ilustrativo, sem movimentação.",route.size(),v.geo.meters/1000,progress,distance/1000,(v.geo.meters-distance)/1000));
  };
  seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar s,int p,boolean user){if(user&&!syncing[0])update.accept(v.geo.distanceForPercent(p));}public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}});
  apply.setOnClickListener(x->{try{String entered=km.getText().toString().trim();update.accept(entered.equals(displayedDistance[0])?v.distance:v.geo.distanceFromKm(Double.parseDouble(entered.replace(',','.'))));}catch(IllegalArgumentException invalid){km.setError("Informe uma distância válida dentro da rota.");}});
  finish.setOnClickListener(x->update.accept(v.geo.meters));
  update.accept(0);
  android.widget.ScrollView scroll=new android.widget.ScrollView(MapTheme.wrap(a));scroll.addView(box);
  AlertDialog d=new AlertDialog.Builder(MapTheme.wrap(a)).setTitle("Prévia GPX — "+route.name).setView(scroll).setPositiveButton("Fechar",null).create();
  DialogLifecycle.bind(a,d,null);d.setOnShowListener(x->MapTheme.styleButtons(d));d.show();
 }
}
