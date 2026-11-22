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

/** Image override for the VGP, main launcher, gym and berry. Files are account-private. */
final class OverlayArtwork {
    private static final int MAX_BYTES=2*1024*1024;
    private static final AtomicInteger VERSION=new AtomicInteger();
    private OverlayArtwork(){}
    static int version(){return VERSION.get();}
    private static String file(String kind){
        if("vgp".equals(kind)||"launcher".equals(kind)||"gym".equals(kind)||"berry".equals(kind))
            return "overlay-"+kind+"-image.png";
        throw new IllegalArgumentException("Unknown overlay image kind");
    }
    static Bitmap load(Context context,String kind){
        try{
            File image=AccountProfiles.file(context,file(kind));
            return image.isFile()?BitmapFactory.decodeFile(image.getAbsolutePath()):null;
        }catch(RuntimeException invalid){return null;}
    }
    static void select(Activity activity,String kind,Runnable changed){
        final String filename=file(kind);
        GpxDocuments.openImage(activity,new GpxDocuments.Callback(){
            public void onCancelled(){}
            public void onPicked(Uri uri){
                final File destination;
                try{destination=AccountProfiles.file(activity,filename);}
                catch(RuntimeException error){notifyUser(activity,"Não foi possível acessar a imagem desta conta.");return;}
                new Thread(()->{
                    try{
                        ByteArrayOutputStream raw=new ByteArrayOutputStream();
                        try(InputStream in=activity.getContentResolver().openInputStream(uri)){
                            if(in==null)throw new java.io.IOException("Unavailable image");
                            byte[] chunk=new byte[8192];int count;
                            while((count=in.read(chunk))!=-1){
                                if(raw.size()+count>MAX_BYTES)throw new java.io.IOException("Image exceeds 2 MB");
                                raw.write(chunk,0,count);
                            }
                        }
                        byte[] bytes=raw.toByteArray();
                        BitmapFactory.Options probe=new BitmapFactory.Options();
                        probe.inJustDecodeBounds=true;BitmapFactory.decodeByteArray(bytes,0,bytes.length,probe);
                        if(probe.outWidth<1||probe.outHeight<1||probe.outWidth>4096||probe.outHeight>4096)
                            throw new java.io.IOException("Unsupported image dimensions");
                        BitmapFactory.Options opts=new BitmapFactory.Options();
                        for(opts.inSampleSize=1;Math.max(probe.outWidth,probe.outHeight)/opts.inSampleSize>512;opts.inSampleSize*=2){}
                        Bitmap bitmap=BitmapFactory.decodeByteArray(bytes,0,bytes.length,opts);
                        if(bitmap==null)throw new java.io.IOException("Unsupported image");
                        File parent=destination.getParentFile();
                        if(parent==null||(!parent.isDirectory()&&!parent.mkdirs()))
                            throw new java.io.IOException("No image directory");
                        AtomicFile target=new AtomicFile(destination);FileOutputStream stream=null;
                        try{
                            stream=target.startWrite();
                            if(!bitmap.compress(Bitmap.CompressFormat.PNG,100,stream))throw new java.io.IOException("PNG conversion failed");
                            target.finishWrite(stream);stream=null;
                        }finally{if(stream!=null)target.failWrite(stream);bitmap.recycle();}
                        VERSION.incrementAndGet();
                        activity.runOnUiThread(()->{
                            if(activity.isFinishing()||activity.isDestroyed())return;
                            if(changed!=null)changed.run();
                            notifyUser(activity,"Ícone atualizado para esta conta.");
                        });
                    }catch(Exception failure){notifyUser(activity,"Escolha PNG, WebP ou JPEG, até 2 MB e 4096 px.");}
                },"pgbr-art-import").start();
            }
        });
    }
    static void reset(Activity activity,String kind,Runnable changed){
        try{
            File file=AccountProfiles.file(activity,file(kind));
            if(file.isFile()&&!file.delete()){notifyUser(activity,"Não foi possível restaurar a imagem.");return;}
            VERSION.incrementAndGet();if(changed!=null)changed.run();
            notifyUser(activity,"Imagem original restaurada.");
        }catch(RuntimeException error){notifyUser(activity,"Não foi possível restaurar a imagem.");}
    }
    private static void notifyUser(Activity activity,String message){
        activity.runOnUiThread(()->{
            if(!activity.isFinishing()&&!activity.isDestroyed())
                Toast.makeText(activity,message,Toast.LENGTH_SHORT).show();
        });
    }
}
