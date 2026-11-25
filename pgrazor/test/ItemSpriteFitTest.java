package dev.pgrazor;

final class ItemSpriteFitTest {
    private static void near(float actual,float expected){
        if(Math.abs(actual-expected)>0.02f)
            throw new AssertionError(actual+" != "+expected);
    }
    private static void check(int w,int h){
        float[] r=ItemSpriteFit.centeredRect(w,h,30);
        float drawnW=r[2]-r[0],drawnH=r[3]-r[1];
        near((drawnW/drawnH),(float)w/h);
        near((r[0]+r[2]),0);
        near((r[1]+r[3]),0);
        if(drawnW>30.01f||drawnH>30.01f)
            throw new AssertionError("Sprite overflows frame");
    }
    public static void main(String[] args){
        check(203,79);
        check(238,161);
        check(172,196);
        check(256,256);
        check(1,8192);
        if(ItemSpriteFit.sampleSize(104,104)!=1||ItemSpriteFit.sampleSize(4096,64)!=16||ItemSpriteFit.sampleSize(257,256)!=2||ItemSpriteFit.sampleSize(8192,1)!=32)throw new AssertionError("Icon decode bounds");
        try {ItemSpriteFit.sampleSize(8193,1);throw new AssertionError("Oversized icon");}catch(IllegalArgumentException expected){}
        try {ItemSpriteFit.centeredRect(0,100,30);throw new AssertionError("zero width");}
        catch(IllegalArgumentException expected){}
        for(float available:new float[]{1e-30f,30f,Float.MAX_VALUE}) {
            for(int[] size:new int[][]{{8192,1},{1,8192},{8192,8192},{203,79}}){
                float[] rect=ItemSpriteFit.centeredRect(size[0],size[1],available);
                for(float edge:rect)if(!Float.isFinite(edge)||Math.abs(edge)>available/2)
                    throw new AssertionError("Non-finite or overflowing sprite edge");
                if(rect[2]<=0||rect[3]<=0||rect[0]!=-rect[2]||rect[1]!=-rect[3])
                    throw new AssertionError("Degenerate or uncentered sprite");
            }
        }
        System.out.println("PASS: ItemSpriteFit");
    }
}
