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
    // Decode each catalog icon at most once; a live catalog may render dozens of rows.
    private static final android.util.SparseArray<Bitmap> CACHE=new android.util.SparseArray<>();
    private static final android.util.SparseBooleanArray MISSED=new android.util.SparseBooleanArray();

    ItemBadge(Context context,int itemId){
        super(context);
        this.itemId=itemId;
        itemSprite=load(context,itemId);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }
    private static synchronized Bitmap load(Context context,int id){
        Bitmap cached=CACHE.get(id);
        if(cached!=null)return cached;
        if(MISSED.get(id))return null;
        if(id<=0||id>999999){MISSED.put(id,true);return null;}
        // Prefer an ID-exact inventory sprite; old map berries are a compatibility fallback.
        String[] choices=id>=701&&id<=707?
            new String[]{"pgrazor/item-icons/item-"+id+".png","pgrazor/map-icons/berry-"+id+".png"}:
            new String[]{"pgrazor/item-icons/item-"+id+".png"};
        for(String file:choices){
            try(java.io.InputStream stream=PgAssets.open(context,file)){
                BitmapFactory.Options options=new BitmapFactory.Options();
                options.inSampleSize=2; // Crisp at 26dp without full-resolution per-row allocations.
                Bitmap bitmap=BitmapFactory.decodeStream(stream,null,options);
                if(bitmap!=null){CACHE.put(id,bitmap);return bitmap;}
            }catch(Exception ignored){}
        }
        MISSED.put(id,true);return null;
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
            c.drawBitmap(itemSprite,null,new RectF(-15,-15,15,15),paint);
        }else if(itemId>=1&&itemId<=3){
            drawBall(c);
        }else if(itemId>=100&&itemId<200){
            drawPotion(c);
        }else if(itemId>=200&&itemId<300){
            drawRevive(c);
        }else if(itemId>=300&&itemId<400){
            drawEgg(c);
        }else{
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
        int color=itemId==101?0xff7fa8e5:itemId==102?0xff6a8ce8:itemId==103?0xffa85acf:0xffe16ab4;
        fill(0xff768a94);c.drawRoundRect(-5,-13,5,-8,1,1,paint);
        fill(0xffe1eef5);c.drawRoundRect(-9,-9,9,13,4,4,paint);
        fill(color);c.drawRoundRect(-7,-6,7,11,3,3,paint);
        fill(0xffffffff);c.drawRoundRect(-1.5f,-3.5f,1.5f,8,1,1,paint);
        c.drawRoundRect(-5.5f,0.5f,5.5f,3.5f,1,1,paint);
    }
    private void drawRevive(Canvas c){
        fill(itemId==201?0xfff4b642:0xffd78be9);
        Path diamond=new Path();diamond.moveTo(0,-14);diamond.lineTo(13,0);diamond.lineTo(0,14);diamond.lineTo(-13,0);diamond.close();c.drawPath(diamond,paint);
        stroke(0xffffffff,2);c.drawLine(0,-7,0,7,paint);c.drawLine(-7,0,7,0,paint);
    }
    private void drawEgg(Canvas c){
        fill(0xfff4e8a6);c.drawOval(-10,-14,10,14,paint);
        fill(0xffffffff);c.drawCircle(-4,-4,3,paint);c.drawCircle(5,5,2,paint);
        stroke(0xffb8ac6a,1.4f);c.drawOval(-10,-14,10,14,paint);
    }
    private void drawGeneric(Canvas c){
        fill(0xff6ab5bb);c.drawRoundRect(-11,-11,11,11,4,4,paint);
        stroke(Color.WHITE,2.2f);c.drawCircle(0,0,5,paint);
        c.drawLine(-8,0,-5,0,paint);c.drawLine(5,0,8,0,paint);
    }
}
