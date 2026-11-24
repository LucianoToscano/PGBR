package dev.pgbr.geolab;
import java.util.concurrent.*;
import java.util.function.*;
import java.util.Objects;
public final class FilterEngine implements AutoCloseable {
    public static final class Rule {
        public enum Comparison { GE, GT, LE, LT, EQ, NE }
        public enum Junction { AND, OR }
        public final String status; public final double minValue;
        public final Comparison comparison; public final Junction junction;
        public Rule(String status,double minValue){
            this(status,minValue,Comparison.GE,Junction.AND);
        }
        public Rule(String status,double minValue,Comparison comparison,Junction junction){
            if(status==null || !status.matches("[A-Za-z0-9_-]{1,32}") || !Double.isFinite(minValue)) throw new IllegalArgumentException("Invalid rule");
            this.status=status; this.minValue=minValue;
            this.comparison=Objects.requireNonNull(comparison); this.junction=Objects.requireNonNull(junction);
        }
        public boolean matches(Entity e){
            boolean statusMatch=status.equals(e.status);
            if(junction==Junction.AND && !statusMatch) return false;
            if(junction==Junction.OR && statusMatch) return true;
            switch(comparison){
                case GE: return e.value>=minValue;
                case GT: return e.value>minValue;
                case LE: return e.value<=minValue;
                case LT: return e.value<minValue;
                case EQ: return e.value==minValue;
                case NE: return e.value!=minValue;
                default: throw new AssertionError(comparison);
            }
        }
    }
    private final ThreadPoolExecutor executor;
    private volatile Rule rule;
    public FilterEngine(Rule initial,int workers,int queueLimit){
        rule=Objects.requireNonNull(initial);
        if(workers<1||workers>8||queueLimit<1||queueLimit>1024) throw new IllegalArgumentException("Invalid limits");
        executor=new ThreadPoolExecutor(workers,workers,0,TimeUnit.SECONDS,new ArrayBlockingQueue<>(queueLimit),new ThreadPoolExecutor.AbortPolicy());
    }
    public void update(Rule next){rule=Objects.requireNonNull(next);}
    public Future<Integer> submit(Iterable<Entity> batch, Consumer<Entity> callback){
        Objects.requireNonNull(batch); Objects.requireNonNull(callback);
        final Rule snapshot=rule;
        return executor.submit(()->{int matched=0; for(Entity e:batch){
            if(Thread.currentThread().isInterrupted()) throw new InterruptedException();
            if(snapshot.matches(e)){callback.accept(e); matched++;}
        } return matched;});
    }
    public void close(){executor.shutdown();}
}
