package dev.pgrazor;

import android.app.Activity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

/** Opens the game's native TM flow only on a fresh, identified move row. */
final class TmShortcutOverlay {
    private final Activity activity;
    private final FrameLayout parent;
    private final Button[] buttons=new Button[3];
    private final long[] tokens=new long[3];
    private boolean unavailable,closed;
    TmShortcutOverlay(Activity activity,FrameLayout parent){
        this.activity=activity;this.parent=parent;
        for(int i=0;i<3;i++){
            final int row=i;
            Button v=new Button(MapTheme.wrap(activity));v.setAllCaps(false);v.setTextSize(10);
            v.setPadding(0,0,0,0);v.setVisibility(View.GONE);
            v.setContentDescription(i==0?"Abrir MT de ataque ágil":"Abrir MT de ataque carregado "+i);
            v.setOnClickListener(x->{
                long token=tokens[row];if(closed||!Control.itemAccessAllowed()||Control.catchScreenVisible()||token==0)return;
                tokens[row]=0;v.setEnabled(false);
                try{if(!NativeCore.tmUse(row,token))Toast.makeText(activity,"MT indisponível ou tela alterada.",Toast.LENGTH_SHORT).show();}
                catch(UnsatisfiedLinkError e){unavailable=true;}
            });
            parent.addView(v,new FrameLayout.LayoutParams(dp(68),dp(30)));buttons[i]=v;
        }
    }
    private int dp(float n){return MapTheme.dp(activity,n);}
    void refresh(boolean allowed){
        if(closed)return;
        long[] s=null;
        if(!unavailable)try{
            NativeCore.tmAllowed(allowed);
            if(allowed)s=NativeCore.tmState();
        }catch(UnsatisfiedLinkError e){unavailable=true;}
        for(int i=0;i<3;i++){
            Button v=buttons[i];int index=1+i*4;
            boolean show=allowed&&s!=null&&s.length==13&&s[0]>0&&s[index]==1&&parent.getWidth()>0&&parent.getHeight()>0;
            tokens[i]=show?s[0]:0;
            v.setVisibility(show?View.VISIBLE:View.GONE);
            if(!show){v.setEnabled(false);continue;}
            long count=s[index+3];
            v.setText((i==0?"MT Ágil ":"MT C. ")+Math.max(0,count));
            v.setEnabled(count>0);v.setAlpha(count>0?1f:0.45f);
            float x=(float)(s[index+1]/1000000.0*parent.getWidth())-dp(100);
            float y=(float)(s[index+2]/1000000.0*parent.getHeight())-dp(15);
            v.setX(Math.max(0,Math.min(parent.getWidth()-dp(68),x)));
            v.setY(Math.max(0,Math.min(parent.getHeight()-dp(30),y)));
        }
    }
    void close(){
        if(closed)return;closed=true;
        if(!unavailable)try{NativeCore.tmAllowed(false);}catch(UnsatisfiedLinkError ignored){}
        for(Button v:buttons){v.setOnClickListener(null);parent.removeView(v);}
    }
}
