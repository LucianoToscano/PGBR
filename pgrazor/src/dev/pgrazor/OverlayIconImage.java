package dev.pgrazor;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.AtomicFile;
import android.widget.Toast;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicInteger;

/** Locally selected overlays: never transmits images and never changes game account data. */
final class OverlayIconImage {
    private static final int MAX_BYTES=3*1024*1024;
    private static final AtomicInteger GENERATION=new AtomicInteger();
    private OverlayIconImage(){}
    static int generation(){return GENERATION.get();}
    private static String fileName(String slot){
        switch(slot){
            case "menu": return "overlay-icon-menu.png";
            case "gym": return "overlay-icon-gym.png";
            case "vgp": return "overlay-icon-vgp.png";
            default:throw new IllegalArgumentException("Unknown overlay image");
        }
    }
    static Bitmap load(Context context,String slot){
        try{File f=AccountProfiles.file(context,fileName(slot));
            if(!f.isFile())return null;
            BitmapFactory.Options opts=new BitmapFactory.Options();
            opts.inScaled=false;
            return BitmapFactory.decodeFile(f.getAbsolutePath(),opts);
        }catch(RuntimeException ignored){return null;}
    }
    static void choose(Activity activity,String slot,Runnable update){
        fileName(slot);
        GpxDocuments.openImage(activity,new GpxDocuments.Callback(){
            public void onPicked(Uri uri){
                new Thread(()->{
                    try{
                        File target=AccountProfiles.file(activity,fileName(slot));
                        ByteArrayOutputStream raw=new ByteArrayOutputStream();
                        try(InputStream in=activity.getContentResolver().openInputStream(uri)){
                            if(in==null)throw new java.io.IOException("Cannot open image");
                            byte[] buffer=new byte[8192];int count;
                            while((count=in.read(buffer))!=-1){
                                if(count+raw.size()>MAX_BYTES)throw new java.io.IOException("Too large");
                                raw.write(buffer,0,count);
                            }
                        }
                        byte[] data=raw.toByteArray();
                        BitmapFactory.Options probe=new BitmapFactory.Options();
                        probe.inJustDecodeBounds=true;BitmapFactory.decodeByteArray(data,0,data.length,probe);
                        if(probe.outWidth<8||probe.outHeight<8||probe.outWidth>4096||probe.outHeight>4096)
                            throw new java.io.IOException("Invalid image");
                        BitmapFactory.Options options=new BitmapFactory.Options();
                        options.inSampleSize=1;
                        while(Math.max(probe.outWidth,probe.outHeight)/options.inSampleSize>512)
                            options.inSampleSize*=2;
                        Bitmap bitmap=BitmapFactory.decodeByteArray(data,0,data.length,options);
                        if(bitmap==null)throw new java.io.IOException("Invalid bitmap");
                        File dir=target.getParentFile();
                        if(dir==null||(!dir.isDirectory()&&!dir.mkdirs()))throw new java.io.IOException("Directory unavailable");
                        AtomicFile atomic=new AtomicFile(target);
                        FileOutputStream out=null;
                        try{
                            out=atomic.startWrite();
                            if(!bitmap.compress(Bitmap.CompressFormat.PNG,100,out))
                                throw new java.io.IOException("Cannot encode bitmap");
                            atomic.finishWrite(out);out=null;
                        }finally{if(out!=null)atomic.failWrite(out);bitmap.recycle();}
                        GENERATION.incrementAndGet();
                        result(activity,slot+" personalizado: imagem salva.",update);
                    }catch(Exception error){
                        result(activity,"Não foi possível importar imagem PNG, WebP ou JPEG de até 3 MB.",null);
                    }
                },"pgbr-overlay-icon").start();
            }
            public void onCancelled(){}
        });
    }
    static void clear(Activity activity,String slot,Runnable update){
        try{
            File file=AccountProfiles.file(activity,fileName(slot));
            if(file.exists()&&!file.delete()){result(activity,"Não foi possível remover a imagem.",null);return;}
            GENERATION.incrementAndGet();result(activity,"Imagem original restaurada.",update);
        }catch(RuntimeException error){result(activity,"Não foi possível restaurar a imagem.",null);}
    }
    private static void result(Activity activity,String message,Runnable callback){
        activity.runOnUiThread(()->{
            if(activity.isFinishing()||activity.isDestroyed())return;
            if(callback!=null)callback.run();
            Toast.makeText(activity,message,Toast.LENGTH_SHORT).show();
        });
    }
}
