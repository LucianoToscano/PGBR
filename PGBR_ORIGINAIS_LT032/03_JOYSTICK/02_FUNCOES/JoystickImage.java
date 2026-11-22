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

/** User-selected joystick image. Bounded, app-private and isolated by account profile. */
final class JoystickImage {
    private static final int MAX_BYTES=2*1024*1024;
    private static final AtomicInteger GENERATION=new AtomicInteger();
    private static final String FILE="joystick-custom.png";
    private JoystickImage(){}

    static int generation(){return GENERATION.get();}

    static Bitmap load(Context context){
        try{
            File file=AccountProfiles.file(context,FILE);
            if(!file.isFile())return null;
            return BitmapFactory.decodeFile(file.getAbsolutePath());
        }catch(RuntimeException ignored){return null;}
    }

    static void select(Activity activity){
        GpxDocuments.openImage(activity,new GpxDocuments.Callback(){
            public void onPicked(Uri uri){
                File target;
                try{target=AccountProfiles.file(activity,FILE);}
                catch(RuntimeException error){notifyUser(activity,"Imagem indisponível para esta conta.");return;}
                new Thread(()->{
                    try{
                        ByteArrayOutputStream raw=new ByteArrayOutputStream();
                        try(InputStream in=activity.getContentResolver().openInputStream(uri)){
                            if(in==null)throw new java.io.IOException("Cannot open image");
                            byte[] buffer=new byte[8192];int count;
                            while((count=in.read(buffer))!=-1){
                                if(raw.size()+count>MAX_BYTES)throw new java.io.IOException("Image too large");
                                raw.write(buffer,0,count);
                            }
                        }
                        byte[] bytes=raw.toByteArray();
                        BitmapFactory.Options bounds=new BitmapFactory.Options();
                        bounds.inJustDecodeBounds=true;
                        BitmapFactory.decodeByteArray(bytes,0,bytes.length,bounds);
                        if(bounds.outWidth<=0||bounds.outHeight<=0||bounds.outWidth>4096||
                            bounds.outHeight>4096)throw new java.io.IOException("Invalid dimensions");
                        int sample=1;
                        while(Math.max(bounds.outWidth,bounds.outHeight)/sample>512)sample*=2;
                        BitmapFactory.Options decoded=new BitmapFactory.Options();decoded.inSampleSize=sample;
                        Bitmap bitmap=BitmapFactory.decodeByteArray(bytes,0,bytes.length,decoded);
                        if(bitmap==null)throw new java.io.IOException("Invalid bitmap");
                        File directory=target.getParentFile();
                        if(directory==null||(!directory.isDirectory()&&!directory.mkdirs()))
                            throw new java.io.IOException("Cannot create image directory");
                        AtomicFile atomic=new AtomicFile(target);FileOutputStream output=null;
                        try{
                            output=atomic.startWrite();
                            if(!bitmap.compress(Bitmap.CompressFormat.PNG,100,output))
                                throw new java.io.IOException("Cannot encode image");
                            atomic.finishWrite(output);output=null;
                        }finally{if(output!=null)atomic.failWrite(output);}
                        bitmap.recycle();
                        GENERATION.incrementAndGet();
                        activity.runOnUiThread(()->{
                            if(!activity.isFinishing()&&!activity.isDestroyed())Control.setJoystickStyle(3);
                        });
                        notifyUser(activity,"Imagem do joystick atualizada.");
                    }catch(Exception error){notifyUser(activity,"Não foi possível importar a imagem. Escolha PNG, WebP ou JPEG de até 2 MB.");}
                },"pgbr-joystick-image").start();
            }
            public void onCancelled(){}
        });
    }

    static void reset(Activity activity){
        try{
            File target=AccountProfiles.file(activity,FILE);
            if(target.exists()&&!target.delete()){
                notifyUser(activity,"Não foi possível remover a imagem personalizada.");return;
            }
            GENERATION.incrementAndGet();
            if(Control.joystickStyle()==3)Control.setJoystickStyle(1); // Return to Eternatus default after deleting custom art.
            notifyUser(activity,"Joystick original restaurado.");
        }catch(RuntimeException error){notifyUser(activity,"Não foi possível restaurar a imagem.");}
    }

    private static void notifyUser(Activity activity,String message){
        activity.runOnUiThread(()->{
            if(!activity.isFinishing()&&!activity.isDestroyed())
                Toast.makeText(activity,message,Toast.LENGTH_SHORT).show();
        });
    }
}
