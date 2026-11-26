"""Real SavedPlaces code with shared in-memory preferences; no device or network."""
from pathlib import Path
import os
import subprocess
import tempfile
import unittest
from test_control_java import java_tools
from org_json_support import sdk_json_sources, STUB_JSON

ROOT=Path(__file__).resolve().parents[1]
STUBS={
"android/content/Context.java": """package android.content;
public class Context { public Context getApplicationContext(){return this;} }""",
"android/content/SharedPreferences.java": """package android.content;
public interface SharedPreferences {
 String getString(String k,String fallback);
 Editor edit();
 interface Editor { Editor putString(String k,String v); boolean commit(); }
}""",
"dev/pgrazor/AccountProfiles.java": """package dev.pgrazor;
import android.content.*;
import java.util.*;
final class AccountProfiles {
 private static final Memory STORE=new Memory();
 static SharedPreferences preferences(Context c,String scope){return STORE;}
 static final class Memory implements SharedPreferences {
  private final Map<String,String> data=new HashMap<>();
  public synchronized String getString(String k,String fallback){return data.getOrDefault(k,fallback);}
  public Editor edit(){return new Editor(){
   final Map<String,String> next=new HashMap<>();
   public Editor putString(String k,String v){next.put(k,v);return this;}
   public boolean commit(){
    synchronized(Memory.this){data.putAll(next);}return true;
   }
  };}
 }
}""",
"dev/pgrazor/SavedPlacesRaceHarness.java": """package dev.pgrazor;
import android.content.Context;
import java.util.concurrent.*;
public class SavedPlacesRaceHarness {
 public static void main(String[] args)throws Exception {
  Context ctx=new Context();
  SavedPlaces a=new SavedPlaces(ctx), b=new SavedPlaces(ctx);
  ExecutorService pool=Executors.newFixedThreadPool(2);
  try {
   for(int i=0;i<50;i++){
    final int id=i;
    CyclicBarrier gate=new CyclicBarrier(2);
    Future<?> x=pool.submit(()->{gate.await();a.add(new SavedPlaces.Entry("A"+id,0,0));return null;});
    Future<?> y=pool.submit(()->{gate.await();b.add(new SavedPlaces.Entry("B"+id,0,0));return null;});
    x.get(10,TimeUnit.SECONDS); y.get(10,TimeUnit.SECONDS);
    SavedPlaces.Snapshot snap=a.read();
    if(snap.damaged||snap.entries.size()!=2*(i+1))throw new AssertionError("lost concurrent write: "+snap.entries.size());
   }
   SavedPlaces.Snapshot snapshot=a.read();
   String one=snapshot.entries.get(0).id,two=snapshot.entries.get(1).id;
   CyclicBarrier gate=new CyclicBarrier(2);
   Future<Boolean> x=pool.submit(()->{gate.await();return a.remove(one);});
   Future<Boolean> y=pool.submit(()->{gate.await();return b.remove(two);});
   if(!x.get(10,TimeUnit.SECONDS)||!y.get(10,TimeUnit.SECONDS)||a.read().entries.size()!=98)
    throw new AssertionError("lost concurrent removal");
  } finally {pool.shutdownNow();}
  android.content.SharedPreferences prefs=AccountProfiles.preferences(ctx,"pgrazor-saved-places-v1");
  String oversized="x".repeat(65537);
  prefs.edit().putString("places",oversized).commit();
  if(!a.read().damaged)throw new AssertionError("Oversized source accepted");
  try {a.add(new SavedPlaces.Entry("New",0,0));throw new AssertionError("Overwrote unbackable source");}
  catch(java.io.IOException expected){}
  if(!oversized.equals(prefs.getString("places",""))||!prefs.getString("recovered-from","").isEmpty())
    throw new AssertionError("Original was not preserved");
  String damaged="not JSON";
  prefs.edit().putString("places",damaged).commit();
  a.add(new SavedPlaces.Entry("Recovered",0,0));
  if(!damaged.equals(prefs.getString("recovered-from",""))||a.read().damaged||a.read().entries.size()!=1)
    throw new AssertionError("Bounded recovery lost its backup");
  a.add(new SavedPlaces.Entry("Café",1,2));
  try {b.add(new SavedPlaces.Entry("CAFE\\u0301",3,4));throw new AssertionError("Equivalent name accepted");}
  catch(IllegalArgumentException expected){}
  if(b.merge(java.util.Arrays.asList(new SavedPlaces.Entry("cafe\\u0301",4,5),new SavedPlaces.Entry("Outro",1,2)))!=1)
    throw new AssertionError("Merge accepted duplicate name or dropped distinct place");
  SavedPlaces.Snapshot normalized=a.read();
  if(normalized.damaged||normalized.entries.size()!=3||!normalized.entries.get(1).name.equals("Café"))
    throw new AssertionError("Original spelling or entries lost");
 }
}"""
}
class SavedPlacesConcurrencyTests(unittest.TestCase):
    def test_shared_store_concurrent_mutations(self):
        compiler,runtime=java_tools()
        with tempfile.TemporaryDirectory() as tmp:
            work=Path(tmp)
            sources=[]
            for relative,content in STUBS.items():
                target=work/relative
                target.parent.mkdir(parents=True,exist_ok=True)
                target.write_text(content,encoding="utf-8")
                sources.append(target)
            json=sdk_json_sources(work) or sorted(STUB_JSON.glob("*.java"))
            self.assertTrue(json,"org.json sources required")
            subprocess.run([str(compiler),"-encoding","UTF-8","-d",tmp,
                *map(str,sources+json+[ROOT/"src/dev/pgrazor/PtBr.java",ROOT/"src/dev/pgrazor/SavedPlaces.java"])],check=True,timeout=45)
            subprocess.run([str(runtime),"-cp",tmp,"dev.pgrazor.SavedPlacesRaceHarness"],check=True,timeout=60)
if __name__=="__main__":
    unittest.main()
