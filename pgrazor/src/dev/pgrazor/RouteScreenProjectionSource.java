package dev.pgrazor;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.PixelCopy;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BooleanSupplier;

/**
 * Explicit opt-in screen-based calibration for a visible, non-protected SurfaceView in this activity.
 * No hooks, native game calls, simulated touches, screenshot files or network.
 * Landmark loss hides the trace; later frames can recover near the last verified positions.
 */
final class RouteScreenProjectionSource implements RouteTraceOverlay.ProjectedFrameSource {
    private final Activity activity;
    private final ViewGroup viewport;
    private final MotionEngine motion;
    private final BooleanSupplier mapVisible;
    private final Handler main=new Handler(Looper.getMainLooper());
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private SurfaceView surface;
    private AlertDialog dialog;
    private RouteScreenTracker tracker;
    private RouteTraceFrame latest;
    private long knownToken,lastRequest;
    private double[][] points;
    private int captureWidth,captureHeight,viewportWidth,viewportHeight;
    private boolean busy,closed,ready;

    RouteScreenProjectionSource(Activity activity,ViewGroup viewport,MotionEngine motion,BooleanSupplier mapVisible){
        this.activity=activity;this.viewport=viewport;this.motion=motion;this.mapVisible=mapVisible;
    }
    private void toast(String text){Toast.makeText(activity,text,Toast.LENGTH_LONG).show();}
    private boolean exactSurface(SurfaceView v){
        if(v==null||!v.isShown()||!v.getHolder().getSurface().isValid()||v.getWidth()!=viewport.getWidth()||v.getHeight()!=viewport.getHeight())return false;
        int[] a=new int[2],b=new int[2];v.getLocationInWindow(a);viewport.getLocationInWindow(b);
        return a[0]==b[0]&&a[1]==b[1];
    }
    private void find(View v,ArrayList<SurfaceView> matches){
        if(v instanceof SurfaceView&&exactSurface((SurfaceView)v))matches.add((SurfaceView)v);
        if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++)find(g.getChildAt(i),matches);}
    }
    private boolean available(){
        return !closed&&!activity.isFinishing()&&!activity.isDestroyed()&&mapVisible.getAsBoolean()&&
            viewport.getWidth()==viewportWidth&&viewport.getHeight()==viewportHeight&&exactSurface(surface);
    }
    private static int[] gray(Bitmap b){
        int w=b.getWidth(),h=b.getHeight();int[] p=new int[w*h];
        b.getPixels(p,0,w,0,0,w,h);for(int i=0;i<p.length;i++){
            int c=p[i];p[i]=(77*((c>>16)&255)+150*((c>>8)&255)+29*(c&255))>>8;
        }return p;
    }
    void calibrate(Runnable onReady){
        if(closed||busy)return;
        if(Build.VERSION.SDK_INT<24||!mapVisible.getAsBoolean()){
            toast("Abra o mapa principal com a câmera disponível para calibrar.");return;
        }
        ArrayList<SurfaceView> matches=new ArrayList<>();find(activity.getWindow().getDecorView(),matches);
        if(matches.size()!=1){toast("Não foi encontrada uma superfície gráfica única e compatível.");return;}
        surface=matches.get(0);viewportWidth=viewport.getWidth();viewportHeight=viewport.getHeight();
        double scale=Math.min(1,768.0/Math.max(viewportWidth,viewportHeight));
        captureWidth=(int)Math.round(viewportWidth*scale);captureHeight=(int)Math.round(viewportHeight*scale);
        if(captureWidth<40||captureHeight<40){toast("Área de calibração insuficiente.");return;}
        busy=true;Bitmap image=Bitmap.createBitmap(captureWidth,captureHeight,Bitmap.Config.ARGB_8888);
        try{PixelCopy.request(surface,image,result->{
            busy=false;
            if(closed||result!=PixelCopy.SUCCESS||!available()){image.recycle();if(!closed)toast("Não foi possível capturar o mapa.");return;}
            showCalibration(image,onReady);
        },main);}catch(RuntimeException e){busy=false;image.recycle();toast("Captura gráfica indisponível.");}
    }
    private void showCalibration(Bitmap bitmap,Runnable onReady){
        android.content.Context c=MapTheme.wrap(activity);
        LinearLayout panel=new LinearLayout(c);panel.setOrientation(LinearLayout.VERTICAL);
        TextView hint=new TextView(c);hint.setText("Modo experimental. Escolha quatro pontos fixos no chão que envolvam o trajeto e duas referências internas. Toque na imagem e informe a latitude e longitude exatas de cada ponto. Não use Pokémon, ícones ou objetos animados. A captura fica apenas na memória.");panel.addView(hint);
        ImageView image=new ImageView(c);image.setImageBitmap(bitmap);image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        panel.addView(image,new LinearLayout.LayoutParams(-1,MapTheme.dp(c,360)));
        TextView status=new TextView(c);panel.addView(status);status.setText("Referências: 0/6");
        EditText lat=new EditText(c),lon=new EditText(c);lat.setHint("Latitude (graus decimais)");lon.setHint("Longitude (graus decimais)");
        for(EditText e:new EditText[]{lat,lon}){e.setSingleLine(true);e.setInputType(android.text.InputType.TYPE_CLASS_NUMBER|android.text.InputType.TYPE_NUMBER_FLAG_SIGNED|android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);LocalAuditDialog.privateInput(e);panel.addView(e);}
        Button add=new Button(c);add.setText("Adicionar referência");panel.addView(add);
        Button reset=new Button(c);reset.setText("Limpar referências");panel.addView(reset);
        double[][] refs=new double[6][];int[] n={0};double[] selected={Double.NaN,Double.NaN};
        image.setOnTouchListener((v,event)->{
            if(event.getActionMasked()!=MotionEvent.ACTION_UP)return true;
            double scale=Math.min(image.getWidth()/(double)captureWidth,image.getHeight()/(double)captureHeight);
            double x=(event.getX()-(image.getWidth()-captureWidth*scale)/2)/scale;
            double y=(event.getY()-(image.getHeight()-captureHeight*scale)/2)/scale;
            if(x>=0&&x<captureWidth&&y>=0&&y<captureHeight){selected[0]=x;selected[1]=y;status.setText("Referências: "+n[0]+"/6 • ponto selecionado");}
            return true;
        });
        add.setOnClickListener(v->{
            if(n[0]>=6||!Double.isFinite(selected[0])){status.setText("Toque na imagem ou conclua a calibração.");return;}
            try{
                double a=Double.parseDouble(lat.getText().toString().trim().replace(',','.')),b=Double.parseDouble(lon.getText().toString().trim().replace(',','.'));
                if(!Double.isFinite(a)||!Double.isFinite(b)||Math.abs(a)>90||Math.abs(b)>180)throw new IllegalArgumentException();
                refs[n[0]++]=new double[]{a,b,selected[0],selected[1]};
                selected[0]=selected[1]=Double.NaN;lat.setText("");lon.setText("");status.setText("Referências: "+n[0]+"/6");
            }catch(IllegalArgumentException e){status.setText("Coordenadas inválidas.");}
        });
        reset.setOnClickListener(v->{n[0]=0;selected[0]=selected[1]=Double.NaN;status.setText("Referências: 0/6");});
        ScrollView scroll=new ScrollView(c);scroll.addView(panel);
        final boolean[] accepted={false};
        AlertDialog d=new AlertDialog.Builder(c).setTitle("Calibrar Rota Visual — imagem do mapa")
            .setView(scroll).setNegativeButton("Cancelar",null).setPositiveButton("Calibrar",null).create();
        dialog=d;DialogLifecycle.bind(activity,d,()->{
            image.setImageDrawable(null);bitmap.recycle();dialog=null;
            if(accepted[0]&&!closed){ready=true;onReady.run();}
        });
        d.setOnShowListener(v->{MapTheme.styleButtons(d);d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(button->{
            if(n[0]!=6){status.setText("Selecione seis referências.");return;}
            try{tracker=new RouteScreenTracker(gray(bitmap),captureWidth,captureHeight,refs);}
            catch(IllegalArgumentException invalid){status.setText("Referências incompatíveis ou sem contraste. Limpe e tente novamente.");return;}
            accepted[0]=true;d.dismiss();
        });});d.show();
    }
    @Override public RouteTraceFrame latest(){
        if(!ready||!available()){latest=null;return null;}
        MotionEngine.RouteVisualSnapshot route=motion.routeVisualSnapshot(knownToken);
        if(route==null){knownToken=0;points=null;latest=null;return null;}
        if(route.newPoints!=null){points=route.newPoints;knownToken=route.token;latest=null;}
        long now=SystemClock.elapsedRealtime();
        if(!busy&&points!=null&&now-lastRequest>=100){
            lastRequest=now;busy=true;final double[][] routePoints=points;
            final RouteScreenTracker current=tracker;
            Bitmap bitmap=Bitmap.createBitmap(captureWidth,captureHeight,Bitmap.Config.ARGB_8888);
            try{PixelCopy.request(surface,bitmap,result->{
                if(closed||result!=PixelCopy.SUCCESS||!available()){bitmap.recycle();busy=false;latest=null;return;}
                try{worker.execute(()->{
                    RouteTraceFrame frame=null;boolean lost=false;
                    try{
                        if(!Thread.currentThread().isInterrupted()){
                            RouteGroundProjection p=current.update(gray(bitmap));
                            if(p==null)lost=true;
                            else frame=RouteScreenFrameBuilder.build(p,routePoints,route.looping,route.completedMeters,route.token,
                                now,viewportWidth,viewportHeight,viewportWidth/(double)captureWidth,viewportHeight/(double)captureHeight);
                        }
                    }catch(RuntimeException invalid){lost=true;}finally{bitmap.recycle();}
                    final RouteTraceFrame next=frame;final boolean trackingLost=lost;
                    main.post(()->{
                        busy=false;if(closed)return;
                        if(trackingLost){latest=null;return;}
                        if(available()&&knownToken==route.token)latest=next;else latest=null;
                    });
                });}catch(java.util.concurrent.RejectedExecutionException e){bitmap.recycle();busy=false;latest=null;}
            },main);}catch(RuntimeException e){bitmap.recycle();busy=false;latest=null;}
        }
        return latest;
    }
    void close(){
        if(closed)return;closed=true;ready=false;latest=null;points=null;tracker=null;
        if(dialog!=null)dialog.dismiss();worker.shutdown();
    }
}
