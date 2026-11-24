package dev.pgrazor;

/** Local image-only landmark matching. Never reads gameplay state or generates input. */
public final class RouteScreenTracker {
    private static final int PATCH=4, SEARCH=18, SIDE=PATCH*2+1;
    private static final int COUNT=SIDE*SIDE;
    private final int width,height;
    private final double[][] refs;
    private final int[][] templates=new int[6][];
    private final int[] xs=new int[6],ys=new int[6];
    private boolean lost;

    /** Grayscale image (0..255) from the same explicitly calibrated frame. */
    public RouteScreenTracker(int[] image,int width,int height,double[][] refs) {
        if(width<40||height<40||width>768||height>768||image==null||image.length!=width*height)
            throw new IllegalArgumentException("Invalid image");
        new RouteGroundProjection(refs,width,height);
        this.width=width;this.height=height;this.refs=new double[6][];
        for(int i=0;i<6;i++){
            this.refs[i]=refs[i].clone();
            xs[i]=(int)Math.round(refs[i][2]);ys[i]=(int)Math.round(refs[i][3]);
            if(!inside(xs[i],ys[i]))throw new IllegalArgumentException("Landmark at border");
            templates[i]=patch(image,xs[i],ys[i]);
            if(variance(templates[i])<90)throw new IllegalArgumentException("Landmark lacks contrast");
        }
    }
    private boolean inside(int x,int y){return x>=PATCH&&y>=PATCH&&x<width-PATCH&&y<height-PATCH;}
    private int[] patch(int[] image,int x,int y){
        int[] out=new int[COUNT];int k=0;
        for(int dy=-PATCH;dy<=PATCH;dy++)for(int dx=-PATCH;dx<=PATCH;dx++)
            out[k++]=image[(y+dy)*width+x+dx];
        return out;
    }
    private static double variance(int[] a){double sum=0,sq=0;for(int v:a){sum+=v;sq+=(double)v*v;}return sq/a.length-(sum/a.length)*(sum/a.length);}
    private double error(int[] image,int x,int y,int[] t){
        double sum=0,sq=0,ts=0,tq=0,product=0;int k=0;
        for(int dy=-PATCH;dy<=PATCH;dy++)for(int dx=-PATCH;dx<=PATCH;dx++){
            int a=image[(y+dy)*width+x+dx],b=t[k++];
            sum+=a;sq+=(double)a*a;ts+=b;tq+=(double)b*b;product+=(double)a*b;
        }
        double av=sq-sum*sum/COUNT,bv=tq-ts*ts/COUNT;
        if(av<90*COUNT||bv<90*COUNT)return 2;
        return 1-(product-sum*ts/COUNT)/Math.sqrt(av*bv);
    }
    /** Strictly bounded search; all six landmarks must be distinctive and independently consistent. */
    public RouteGroundProjection update(int[] image){
        if(lost||image==null||image.length!=width*height)return null;
        int[] nx=new int[6],ny=new int[6];
        double[][] candidate=new double[6][];
        for(int i=0;i<6;i++){
            double best=2,second=2;int bx=0,by=0;
            int minX=Math.max(PATCH,xs[i]-SEARCH),maxX=Math.min(width-PATCH-1,xs[i]+SEARCH);
            int minY=Math.max(PATCH,ys[i]-SEARCH),maxY=Math.min(height-PATCH-1,ys[i]+SEARCH);
            for(int y=minY;y<=maxY;y++)for(int x=minX;x<=maxX;x++){
                double e=error(image,x,y,templates[i]);if(e<best){best=e;bx=x;by=y;}
            }
            for(int y=minY;y<=maxY;y++)for(int x=minX;x<=maxX;x++){
                if(Math.abs(x-bx)<=2&&Math.abs(y-by)<=2)continue;
                second=Math.min(second,error(image,x,y,templates[i]));
            }
            if(best>.12||second-best<.07){lost=true;return null;}
            nx[i]=bx;ny[i]=by;candidate[i]=new double[]{refs[i][0],refs[i][1],bx,by};
        }
        try{
            RouteGroundProjection projection=new RouteGroundProjection(candidate,width,height);
            for(int i=0;i<6;i++){xs[i]=nx[i];ys[i]=ny[i];}
            return projection;
        }catch(IllegalArgumentException invalid){lost=true;return null;}
    }
}
