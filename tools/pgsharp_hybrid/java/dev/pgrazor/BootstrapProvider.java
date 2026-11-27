package dev.pgrazor;

import android.app.Activity;
import android.app.Application;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import java.util.WeakHashMap;

/** Minimal alternate entry compiled ONLY by the private coexistence builder. */
public final class BootstrapProvider extends ContentProvider {
    private static final String HOST = "com.nianticproject.holoholo.libholoholo.unity.UnityMainActivity";
    @Override public boolean onCreate() {
        if (getContext()==null || !(getContext().getApplicationContext() instanceof Application)) return false;
        Application app=(Application)getContext().getApplicationContext();
        if (!Application.getProcessName().equals(app.getApplicationInfo().processName)) return true;
        app.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            private final WeakHashMap<Activity,Button> buttons=new WeakHashMap<>();
            @Override public void onActivityResumed(Activity activity) {
                if (!HOST.equals(activity.getClass().getName()) || buttons.containsKey(activity)) return;
                View content=activity.findViewById(android.R.id.content);
                if (!(content instanceof FrameLayout)) return;
                FrameLayout root=(FrameLayout)content;
                Button button=new Button(activity);
                button.setText("PGBR QA");
                button.setContentDescription("Abrir painel PGBR de leitura");
                FrameLayout.LayoutParams params=new FrameLayout.LayoutParams(-2,-2,Gravity.BOTTOM|Gravity.END);
                int margin=(int)(16*activity.getResources().getDisplayMetrics().density+0.5f);
                params.setMargins(margin,margin,margin,margin);
                button.setOnClickListener(v -> activity.startActivity(new Intent(activity,LogActivity.class)));
                root.addView(button,params);buttons.put(activity,button);
            }
            private void remove(Activity activity) {
                Button b=buttons.remove(activity);
                if (b!=null && b.getParent() instanceof ViewGroup) ((ViewGroup)b.getParent()).removeView(b);
            }
            @Override public void onActivityPaused(Activity a){remove(a);}
            @Override public void onActivityDestroyed(Activity a){remove(a);}
            @Override public void onActivityCreated(Activity a,Bundle b){}
            @Override public void onActivityStarted(Activity a){}
            @Override public void onActivityStopped(Activity a){}
            @Override public void onActivitySaveInstanceState(Activity a,Bundle b){}
        });
        return true;
    }
    @Override public Cursor query(Uri u,String[] p,String s,String[] a,String o){return null;}
    @Override public String getType(Uri u){return null;}
    @Override public Uri insert(Uri u,ContentValues v){return null;}
    @Override public int delete(Uri u,String s,String[] a){return 0;}
    @Override public int update(Uri u,ContentValues v,String s,String[] a){return 0;}
}
