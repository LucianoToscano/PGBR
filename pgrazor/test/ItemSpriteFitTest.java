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
        try {ItemSpriteFit.centeredRect(0,100,30);throw new AssertionError("zero width");}
        catch(IllegalArgumentException expected){}
        System.out.println("PASS: ItemSpriteFit");
    }
}
