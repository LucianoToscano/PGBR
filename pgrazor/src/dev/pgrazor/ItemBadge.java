package dev.pgrazor;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.View;

/** Compact item illustration for inventory rows; decorative only, never changes rules. */
final class ItemBadge extends View {
    private final int itemId;
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private final Bitmap itemSprite;
    // Bound retained memory; evicted bitmaps may still be displayed by existing Views.
    // Never recycle them on eviction. Unknown IDs must not grow a permanent cache.
    private static final int MAX_CACHE_BYTES=8*1024*1024,MAX_NEGATIVE_IDS=512;
    private static final android.util.LruCache<Integer,Bitmap> CACHE=new android.util.LruCache<Integer,Bitmap>(MAX_CACHE_BYTES){
        @Override protected int sizeOf(Integer id,Bitmap bitmap){return bitmap.getAllocationByteCount();}
    };
    private static final android.util.LruCache<Integer,Boolean> MISSED=new android.util.LruCache<>(MAX_NEGATIVE_IDS);
    private static final android.util.LruCache<Integer,Boolean> WARNED=new android.util.LruCache<>(MAX_NEGATIVE_IDS);

    ItemBadge(Context context,int itemId){
        super(context);
        this.itemId=itemId;
        itemSprite=load(context,itemId);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }
    boolean hasDecodedSprite(){return itemSprite!=null;}
    private static synchronized Bitmap load(Context context,int id){
        if(id<=0||id>999999)return null;
        Bitmap cached=CACHE.get(id);
        if(cached!=null)return cached;
        if(Boolean.TRUE.equals(MISSED.get(id)))return null;
        // Prefer an ID-exact inventory sprite; old map berries are a compatibility fallback.
        String[] choices=id>=701&&id<=707?
            new String[]{"pgrazor/item-icons/item-"+id+".png","pgrazor/map-icons/berry-"+id+".png"}:
            new String[]{"pgrazor/item-icons/item-"+id+".png"};
        boolean found=false, failed=false;
        for(String file:choices){
            try(java.io.InputStream probe=PgAssets.open(context,file)){
                found=true;
                BitmapFactory.Options bounds=new BitmapFactory.Options();
                bounds.inJustDecodeBounds=true;
                BitmapFactory.decodeStream(probe,null,bounds);
                if(bounds.outWidth<=0||bounds.outHeight<=0)
                    throw new java.io.IOException("Invalid image dimensions");
                BitmapFactory.Options options=new BitmapFactory.Options();
                // Bound decoded dimensions even for very tall or wide source images.
                options.inSampleSize=ItemSpriteFit.sampleSize(bounds.outWidth,bounds.outHeight);
                try(java.io.InputStream stream=PgAssets.open(context,file)){
                    Bitmap bitmap=BitmapFactory.decodeStream(stream,null,options);
                    if(bitmap==null)throw new java.io.IOException("Image decoder returned null");
                    if(bitmap.getWidth()>256||bitmap.getHeight()>256){bitmap.recycle();throw new java.io.IOException("Decoded image exceeds icon limit");}
                    CACHE.put(id,bitmap);
                    WARNED.remove(id);
                    return bitmap;
                }
            }catch(java.io.FileNotFoundException missing){
                // Only an actually absent file is eligible for negative caching.
            }catch(Exception error){
                failed=true;
                if(!Boolean.TRUE.equals(WARNED.get(id))){
                    Control.event("item-icon-error","id="+id+" asset="+file+" cause="+error.getClass().getSimpleName());
                    WARNED.put(id,true);
                }
            }
        }
        if(!found&&!failed){
            MISSED.put(id,true);
            // Capture exact missing catalog IDs so an asset can be added without guessing.
            Control.event("item-icon-missing","id="+id);
        }
        return null;
    }
    private void fill(int color){paint.setColor(color);paint.setStyle(Paint.Style.FILL);paint.setStrokeWidth(1);}
    private void stroke(int color,float width){paint.setColor(color);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(width);}
    protected void onDraw(Canvas c){
        super.onDraw(c);
        if(getWidth()==0||getHeight()==0)return;
        c.save();c.translate(getWidth()/2f,getHeight()/2f);
        float scale=Math.min(getWidth(),getHeight())/32f;
        c.scale(scale,scale);
        if(itemSprite!=null){
            fill(Color.WHITE);
            float[] fit=ItemSpriteFit.centeredRect(itemSprite.getWidth(),itemSprite.getHeight(),30f);
            c.drawBitmap(itemSprite,null,new RectF(fit[0],fit[1],fit[2],fit[3]),paint);
        }else if(itemId>=1&&itemId<=3){
            drawBall(c);
        }else if(itemId>=101&&itemId<=104){
            drawPotion(c);
        }else if(itemId==201||itemId==202){
            drawRevive(c);
        }else if(itemId==301){
            drawLuckyEgg(c);
        }else{
            // Do not draw a wrong Potion/Revive/Egg for a completely different
            // unknown item merely because its numeric ID falls in that range.
            // Exact ID artwork always takes priority; missing art shows its ID.
            drawGeneric(c);
        }
        c.restore();
    }
    private void drawBall(Canvas c){
        int top=itemId==1?0xffdf403b:itemId==2?0xff2874ca:0xffd99b29;
        fill(0xfff9f9f9);c.drawCircle(0,0,13,paint);
        c.save();c.clipRect(-14,-14,14,0);
        fill(top);c.drawCircle(0,0,13,paint);c.restore();
        stroke(0xff17222b,2.1f);c.drawCircle(0,0,13,paint);c.drawLine(-12.5f,0,12.5f,0,paint);
        fill(0xfffdfdfd);c.drawCircle(0,0,4.3f,paint);
        stroke(0xff17222b,1.6f);c.drawCircle(0,0,4.3f,paint);
    }
    private void drawPotion(Canvas c){
        // Approximate fallback colors checked against the existing item PNGs and Drive references.
        int color=itemId==101?0xffc449d9:itemId==102?0xffffa520:itemId==103?0xfff4a9c1:0xff268cde;
        fill(0xff768a94);c.drawRoundRect(-5,-13,5,-8,1,1,paint);
        fill(0xffe1eef5);c.drawRoundRect(-9,-9,9,13,4,4,paint);
        fill(color);c.drawRoundRect(-7,-6,7,11,3,3,paint);
        fill(0xffffffff);c.drawRoundRect(-1.5f,-3.5f,1.5f,8,1,1,paint);
        c.drawRoundRect(-5.5f,0.5f,5.5f,3.5f,1,1,paint);
    }
    private void drawRevive(Canvas c){
        // The original Max Revive is a golden star, not a purple diamond.
        fill(0xfff4c64f);
        Path gem=new Path();
        if(itemId==202){
            for(int i=0;i<10;i++){
                double angle=-Math.PI/2+i*Math.PI/5;
                double radius=(i%2==0)?14:6;
                float x=(float)(Math.cos(angle)*radius),y=(float)(Math.sin(angle)*radius);
                if(i==0)gem.moveTo(x,y);else gem.lineTo(x,y);
            }
        }else{
            gem.moveTo(4,-14);gem.lineTo(9,-7);gem.lineTo(-4,14);gem.lineTo(-9,7);
        }
        gem.close();c.drawPath(gem,paint);
        stroke(0xff9b7529,1.1f);c.drawPath(gem,paint);
    }
    private void drawLuckyEgg(Canvas c){
        // ID 301 is a Lucky Egg, not a distance-specific hatchable egg.
        fill(0xff6daee0);c.drawOval(-10,-14,10,14,paint);
        fill(0xffe8f7ff);c.drawCircle(-4,-6,2.5f,paint);c.drawCircle(4,2,1.7f,paint);
        c.drawCircle(-3,7,1.3f,paint);c.drawCircle(4,-7,1f,paint);
        stroke(0xff3f75a1,1.4f);c.drawOval(-10,-14,10,14,paint);
    }
    private void drawGeneric(Canvas c){
        // Neutral missing-art indicator: never impersonate the wrong Pok\u00e9mon GO item.
        fill(0xffe7edf0);c.drawRoundRect(-14,-12,14,12,5,5,paint);
        stroke(0xff81919b,1.2f);c.drawRoundRect(-14,-12,14,12,5,5,paint);
        paint.setStyle(Paint.Style.FILL);paint.setColor(0xff415765);
        paint.setTypeface(android.graphics.Typeface.create("sans-serif-medium",android.graphics.Typeface.NORMAL));
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(itemId<10000?8f:6f);
        c.drawText(String.valueOf(itemId),0,3f,paint);
    }
}
