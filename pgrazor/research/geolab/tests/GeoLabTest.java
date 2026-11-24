package dev.pgbr.geolab;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
public final class GeoLabTest {
    static void check(boolean b){if(!b)throw new AssertionError();}
    static byte[] bytes(String s){return s.getBytes(StandardCharsets.UTF_8);}
    static void bad(String s){try{JsonParser.entities(bytes(s));throw new AssertionError("accepted invalid JSON");}catch(IllegalArgumentException ok){}}
    public static void main(String[] args) throws Exception {
        byte[] b=new byte[1024*1024];new Random(7).nextBytes(b);check(Arrays.equals(b,NativePipe.transfer(b)));check(NativePipe.transfer(new byte[0]).length==0);
        try{NativePipe.transfer(new byte[b.length+1]);throw new AssertionError();}catch(Exception expected){check(expected instanceof java.io.IOException);}
        Entity e=new Entity("fixture_0",0,0,"READY",95);check(JsonParser.entities(NativePipe.transfer(bytes("["+e.json()+"]"))).size()==1);
        bad("[{}]");bad("[] trailing");bad("[{\"id\":\"x\",\"id\":\"y\"}]");bad("[{\"id\":\"x\",\"lat\":91,\"lon\":0,\"status\":\"READY\",\"value\":5}]");bad("[{\"id\":\"x\",\"lat\":0,\"lon\":0,\"status\":\"READY\",\"value\":1e999}]");
        FilterEngine.Rule r=JsonParser.rule(bytes("{\"status\":\"READY\",\"minValue\":80}"));
        FilterEngine.Rule strict=JsonParser.rule(bytes("{\"status\":\"READY\",\"minValue\":95,\"comparison\":\"GT\",\"junction\":\"AND\"}"));
        check(!strict.matches(e));
        FilterEngine.Rule any=JsonParser.rule(bytes("{\"status\":\"IDLE\",\"minValue\":95,\"comparison\":\"EQ\",\"junction\":\"OR\"}"));
        check(any.matches(e)); check(any.matches(new Entity("fixture_4",0,0,"IDLE",1)));
        for(FilterEngine.Rule.Comparison op:FilterEngine.Rule.Comparison.values()){
            boolean expected=op==FilterEngine.Rule.Comparison.GE||op==FilterEngine.Rule.Comparison.LE||op==FilterEngine.Rule.Comparison.EQ;
            check(new FilterEngine.Rule("READY",95,op,FilterEngine.Rule.Junction.AND).matches(e)==expected);
        }
        for(String invalid:List.of("{\"status\":\"READY\",\"minValue\":80,\"comparison\":\"EVAL\"}","{\"status\":\"READY\",\"minValue\":80,\"command\":\"x\"}")){
            try{JsonParser.rule(bytes(invalid));throw new AssertionError("invalid rule accepted");}catch(IllegalArgumentException ok){}
        }
        AtomicInteger calls=new AtomicInteger(); long t=System.nanoTime();
        try(FilterEngine f=new FilterEngine(r,2,4)){
            Future<Integer> a=f.submit(new MeshGrid(100),x->calls.incrementAndGet()), c=f.submit(new MeshGrid(100),x->calls.incrementAndGet());
            check(a.get()==1000);check(c.get()==1000);check(calls.get()==2000);
            f.update(new FilterEngine.Rule("IDLE",99));check(f.submit(new MeshGrid(10),x->{}).get()==1);
        }
        double sec=(System.nanoTime()-t)/1e9;System.out.printf(Locale.ROOT,"Filter + synthetic generation: 20000 entities / %.4f s (%.0f entries/s)%n",sec,20000/sec);
        StringBuilder json=new StringBuilder("[");
        for(Entity x:new MeshGrid(100)){if(json.length()>1)json.append(',');json.append(x.json());}json.append(']');
        byte[] batch=bytes(json.toString());
        long fullStart=System.nanoTime();
        try(FilterEngine f=new FilterEngine(r,2,4)){
            check(f.submit(JsonParser.entities(NativePipe.transfer(batch)),x->{}).get()==1000);
        }
        double fullSec=(System.nanoTime()-fullStart)/1e9;
        System.out.printf(Locale.ROOT,"JNI + parsing + filter: 10000 entities / %.4f s (%.0f entries/s), %d bytes%n",fullSec,10000/fullSec,batch.length);
        try(LocalSimulator sim=new LocalSimulator()){
            ActionDispatcher d=new ActionDispatcher(sim.port(),2);check(d.dispatch(e));check(!d.dispatch(e));check(d.dispatch(new Entity("fixture_1",0,0,"READY",90)));check(!d.dispatch(new Entity("fixture_2",0,0,"READY",90)));check(sim.accepted()==2);
            try{d.dispatch(new Entity("outside",1,1,"READY",99));throw new AssertionError();}catch(IllegalArgumentException ok){}
        }
        System.out.println("PASS: JNI roundtrip, bounds, JSON, grid, predicates, callbacks, loopback POST, deduplication and limits");
    }
}
