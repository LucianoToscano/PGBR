package dev.pgrazor;

/** Local image-only landmark matching. Never reads gameplay state or generates input. */
public final class RouteScreenTracker {
    private static final int PATCH=4, SOURCE_MARGIN=8, SEARCH=18, SIDE=PATCH*2+1;
    private static final int COUNT=SIDE*SIDE;
    private static final double[] ANGLES={-16,-8,0,8,16}, SCALES={.9,1,1.1};
    private final int width,height;
    private final double[][] refs;
    private final int[][][] templates=new int[6][][];
    private final double[][] means=new double[6][],energies=new double[6][];
    private final int[] xs=new int[6],ys=new int[6];

    /** Grayscale image (0..255) from the same explicitly calibrated frame. */
    public RouteScreenTracker(int[] image,int width,int height,double[][] refs) {
        if(width<40||height<40||width>768||height>768||image==null||image.length!=width*height)
            throw new IllegalArgumentException("Invalid image");
        new RouteGroundProjection(refs,width,height);
        image=smooth(image,width,height);
        this.width=width;this.height=height;this.refs=new double[6][];
        for(int i=0;i<6;i++){
            this.refs[i]=refs[i].clone();
            xs[i]=(int)Math.round(refs[i][2]);ys[i]=(int)Math.round(refs[i][3]);
            if(xs[i]<SOURCE_MARGIN||ys[i]<SOURCE_MARGIN||xs[i]>=width-SOURCE_MARGIN||ys[i]>=height-SOURCE_MARGIN)
                throw new IllegalArgumentException("Landmark at border");
            int n=ANGLES.length*SCALES.length,k=0;
            templates[i]=new int[n][];means[i]=new double[n];energies[i]=new double[n];
            for(double angle:ANGLES)for(double scale:SCALES){
                int[] t=patch(image,xs[i],ys[i],angle,scale);double sum=0,sq=0;
                for(int v:t){sum+=v;sq+=(double)v*v;}
                templates[i][k]=t;means[i][k]=sum/COUNT;energies[i][k]=sq-sum*sum/COUNT;
                if(angle==0&&scale==1&&energies[i][k]<90*COUNT)throw new IllegalArgumentException("Landmark lacks contrast");
                k++;
            }
        }
    }
    private static int[] smooth(int[] image,int width,int height){
        int[] out=image.clone();
        for(int y=1;y<height-1;y++)for(int x=1;x<width-1;x++){
            int i=y*width+x;
            out[i]=(image[i-width-1]+2*image[i-width]+image[i-width+1]+2*image[i-1]+4*image[i]+2*image[i+1]+image[i+width-1]+2*image[i+width]+image[i+width+1]+8)/16;
        }
        return out;
    }
    private int sample(int[] image,double x,double y){
        int ix=(int)Math.floor(x),iy=(int)Math.floor(y);double fx=x-ix,fy=y-iy;
        return (int)Math.round((1-fy)*((1-fx)*image[iy*width+ix]+fx*image[iy*width+ix+1])+
            fy*((1-fx)*image[(iy+1)*width+ix]+fx*image[(iy+1)*width+ix+1]));
    }
    private int[] patch(int[] image,int x,int y,double angle,double scale){
        int[] out=new int[COUNT];int k=0;double a=Math.toRadians(angle),c=Math.cos(a)/scale,s=Math.sin(a)/scale;
        for(int dy=-PATCH;dy<=PATCH;dy++)for(int dx=-PATCH;dx<=PATCH;dx++)
            out[k++]=sample(image,x+c*dx+s*dy,y-s*dx+c*dy);
        return out;
    }
    private double error(int[] image,int x,int y,int landmark,int[] candidate){
        int k=0,sum=0,sq=0;
        for(int dy=-PATCH;dy<=PATCH;dy++)for(int dx=-PATCH;dx<=PATCH;dx++){
            int a=image[(y+dy)*width+x+dx];candidate[k++]=a;sum+=a;sq+=a*a;
        }
        double av=sq-(double)sum*sum/COUNT;if(av<90*COUNT)return 2;
        double best=2;
        for(int j=0;j<templates[landmark].length;j++){
            double bv=energies[landmark][j];if(bv<90*COUNT)continue;
            int product=0;int[] t=templates[landmark][j];
            for(int i=0;i<COUNT;i++)product+=candidate[i]*t[i];
            double e=1-(product-sum*means[landmark][j])/Math.sqrt(av*bv);
            if(e<best)best=e;
        }
        return best;
    }
    /** Bounded translation, rotation and scale search. All six references must remain consistent.
     * A failed frame leaves the last verified positions untouched, allowing a later frame to recover. */
    public RouteGroundProjection update(int[] image){
        if(image==null||image.length!=width*height)return null;
        image=smooth(image,width,height);
        int[] nx=new int[6],ny=new int[6],patch=new int[COUNT];
        double[][] candidate=new double[6][];
        for(int i=0;i<6;i++){
            double best=2,second=2;int bx=0,by=0;
            int minX=Math.max(PATCH,xs[i]-SEARCH),maxX=Math.min(width-PATCH-1,xs[i]+SEARCH);
            int minY=Math.max(PATCH,ys[i]-SEARCH),maxY=Math.min(height-PATCH-1,ys[i]+SEARCH);
            int cols=maxX-minX+1;double[] scores=new double[cols*(maxY-minY+1)];
            for(int y=minY;y<=maxY;y++)for(int x=minX;x<=maxX;x++){
                double e=error(image,x,y,i,patch);scores[(y-minY)*cols+x-minX]=e;
                if(e<best){best=e;bx=x;by=y;}
            }
            for(int y=minY;y<=maxY;y++)for(int x=minX;x<=maxX;x++){
                if(Math.abs(x-bx)<=2&&Math.abs(y-by)<=2)continue;
                second=Math.min(second,scores[(y-minY)*cols+x-minX]);
            }
            if(best>.16||second-best<.10)return null;
            nx[i]=bx;ny[i]=by;candidate[i]=new double[]{refs[i][0],refs[i][1],bx,by};
        }
        try{
            RouteGroundProjection projection=new RouteGroundProjection(candidate,width,height);
            for(int i=0;i<6;i++){xs[i]=nx[i];ys[i]=ny[i];}
            return projection;
        }catch(IllegalArgumentException invalid){return null;}
    }
}
