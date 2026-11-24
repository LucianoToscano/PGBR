package dev.pgrazor;

public final class UiViewportPolicyTest {
    public static void main(String[] args){
        testLauncherAndJoystick();
        testVgpCornerAlignment();
        near(UiViewportPolicy.clamp(Float.NaN,20,300,40),20);
        near(UiViewportPolicy.clamp(Float.POSITIVE_INFINITY,20,300,40),20);
        near(UiViewportPolicy.resizePosition(Float.NaN,20,300,40,30,400,60),30);
        int cases=0;
        for(int widthDp:new int[]{240,280,320,360,412,600,840})
        for(int heightDp:new int[]{240,320,480,640,960})
        for(float density:new float[]{.75f,1f,1.5f,2f,3f,4f})
        for(float font:new float[]{1f,1.3f,2f,3f})
        for(float requested:new float[]{.5f,1f,1.5f,2f}){
            int left=Math.round(24*density),top=Math.round(32*density);
            int right=Math.round(widthDp*density)-left,bottom=Math.round(heightDp*density)-Math.round(48*density);
            int aw=right-left,ah=bottom-top;
            int vw=Math.round(210*density),vh=Math.round(112*density*font);
            float scale=UiViewportPolicy.fit(requested,vw,vh,aw,ah);
            int fw=Math.round(vw*scale),fh=Math.round(vh*scale);
            if(fw>aw||fh>ah||scale>requested||scale<=0||!Float.isFinite(scale))throw new AssertionError("overlay cannot fit");
            for(float position:new float[]{-1000,0,left,(left+right)/2f,right,100000}){
                float x=UiViewportPolicy.clamp(position,left,right,fw),y=UiViewportPolicy.clamp(position,top,bottom,fh);
                if(x<left||x+fw>right||y<top||y+fh>bottom)throw new AssertionError("cutout/bars clamp");
            }
            int mw=UiViewportPolicy.menuExtent(Math.round(190*density),aw,requested);
            int mh=UiViewportPolicy.menuExtent(Math.round(12*48*density*font),ah,requested);
            if(Math.round(mw*requested)>aw||Math.round(mh*requested)>ah)throw new AssertionError("menu viewport overflow");
            // Restoring a normalized drag position after rotation keeps the control reachable.
            for(float fraction:new float[]{0,.25f,.5f,1}){
                int rotatedWidth=ah,rotatedHeight=aw;
                float rs=UiViewportPolicy.fit(requested,vw,vh,rotatedWidth,rotatedHeight);
                int rw=Math.round(vw*rs),rh=Math.round(vh*rs);
                float x=fraction*Math.max(0,rotatedWidth-rw),y=fraction*Math.max(0,rotatedHeight-rh);
                if(x<0||x+rw>rotatedWidth||y<0||y+rh>rotatedHeight)throw new AssertionError("rotation restore");
            }
            cases++;
        }
        System.out.println("PASS UiViewportPolicy: "+cases+" combinations; 240-840dp, portrait/landscape, density .75-4, font scale 1-3, UI scale .5-2, cutout/system bars, scrolling viewport and rotation");
    }
    private static void testVgpCornerAlignment(){
        // Hit target is minimum 48dp, sprite can be 28-32dp. At any of
        // the four corners the actual artwork, not only the invisible touch
        // box, must touch the safe edge. Same formula works in landscape.
        for(int[] bounds:new int[][]{{0,400,24,900},{31,380,48,700},{0,800,0,400}}){
            int left=bounds[0],right=bounds[1],top=bounds[2],bottom=bounds[3];
            for(int hit:new int[]{48,64,72}){
                for(int sprite:new int[]{28,32,48}){
                    if(sprite>hit)continue;
                    float startX=UiViewportPolicy.edgeAlignedCenter(left,left,right,hit,sprite);
                    float endX=UiViewportPolicy.edgeAlignedCenter(right-hit,left,right,hit,sprite);
                    float startY=UiViewportPolicy.edgeAlignedCenter(top,top,bottom,hit,sprite);
                    float endY=UiViewportPolicy.edgeAlignedCenter(bottom-hit,top,bottom,hit,sprite);
                    near(left+startX-sprite/2f,left);
                    near(right-hit+endX+sprite/2f,right);
                    near(top+startY-sprite/2f,top);
                    near(bottom-hit+endY+sprite/2f,bottom);
                    for(int x=left;x<=right-hit;x+=5){
                        float center=UiViewportPolicy.edgeAlignedCenter(x,left,right,hit,sprite);
                        if(x+center-sprite/2f<left-.001f||
                           x+center+sprite/2f>right+.001f)
                            throw new AssertionError("VGP sprite escaped horizontal bound");
                    }
                    for(int y=top;y<=bottom-hit;y+=5){
                        float center=UiViewportPolicy.edgeAlignedCenter(y,top,bottom,hit,sprite);
                        if(y+center-sprite/2f<top-.001f||
                           y+center+sprite/2f>bottom+.001f)
                            throw new AssertionError("VGP sprite escaped vertical bound");
                    }
                    near(UiViewportPolicy.edgeAlignedCenter(
                         left+(right-left-hit)/2f,left,right,hit,sprite),hit/2f);
                }
            }
        }
    }
    private static void testLauncherAndJoystick(){
        // Nonzero left/top insets, asymmetric bars, and both independent Pad controls.
        for(int[] size:new int[][]{{48,48},{136,164}}){
            int left=31,top=47,right=389,bottom=731;
            int w=size[0],h=size[1];
            for(float fraction:new float[]{0f,.25f,.5f,1f}){
                float x=left+fraction*(right-left-w),y=top+fraction*(bottom-top-h);
                int nw=w*2,nh=h*2;
                // UI scale changes must retain an offset from the safe origin.
                float sx=UiViewportPolicy.resizePosition(x,left,right,w,left,right,nw);
                float sy=UiViewportPolicy.resizePosition(y,top,bottom,h,top,bottom,nh);
                near(sx,left+fraction*(right-left-nw));near(sy,top+fraction*(bottom-top-nh));
                // Rotation/inset changes preserve the old safe-track fraction.
                sx=UiViewportPolicy.resizePosition(x,left,right,w,59,743,nw);
                sy=UiViewportPolicy.resizePosition(y,top,bottom,h,23,381,nh);
                near(sx,59+fraction*(743-59-nw));near(sy,23+fraction*(381-23-nh));
            }
            // Drag beyond each edge cannot enter a system bar or cutout.
            near(UiViewportPolicy.clamp(-1000,left,right,w),left);
            near(UiViewportPolicy.clamp(10000,left,right,w),right-w);
            near(UiViewportPolicy.clamp(-1000,top,bottom,h),top);
            near(UiViewportPolicy.clamp(10000,top,bottom,h),bottom-h);
            // An old control larger than its viewport has no usable track.
            near(UiViewportPolicy.resizePosition(99,31,51,136,59,743,w),59);
        }
    }
    private static void near(float actual,float expected){
        if(!Float.isFinite(actual)||!Float.isFinite(expected)||Math.abs(actual-expected)>.001f)throw new AssertionError("safe control position: "+actual+" != "+expected);
    }
}
