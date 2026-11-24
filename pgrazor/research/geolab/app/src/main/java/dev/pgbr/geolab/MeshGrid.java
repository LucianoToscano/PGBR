package dev.pgbr.geolab;
import java.util.Iterator;
import java.util.NoSuchElementException;
// Offline synthetic points only. No network calls or device location changes.
public final class MeshGrid implements Iterable<Entity> {
    private final int side;
    public MeshGrid(int side) {
        if(side<1 || side>100) throw new IllegalArgumentException("side 1..100");
        this.side=side;
    }
    @Override public Iterator<Entity> iterator() {
        return new Iterator<Entity>() {
            private int i;
            public boolean hasNext(){return i<side*side;}
            public Entity next(){
                if(!hasNext()) throw new NoSuchElementException();
                int n=i++;
                return new Entity("fixture_"+n, (n/side)*0.001, (n%side)*0.001, n%2==0?"READY":"IDLE", n%100);
            }
        };
    }
}
