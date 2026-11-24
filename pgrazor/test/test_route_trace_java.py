"""Host-side tests for the independent, read-only route trace model."""
from pathlib import Path
import subprocess
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[1]
HARNESS = r"""
import dev.pgrazor.RouteTraceGeometry;
import java.util.ArrayList;
public class RouteTraceHarness {
  static void check(boolean ok) { if (!ok) throw new AssertionError(); }
  static void reject(Runnable r) {
    try { r.run(); throw new AssertionError("accepted invalid input"); }
    catch (IllegalArgumentException expected) {}
  }
  public static void main(String[] args) {
    double[] x={0,10,0,0}, y={0,0,0,0}, d={0,10,20,20};
    RouteTraceGeometry g=new RouteTraceGeometry(x,y,d);
    x[1]=999; d[1]=999;
    ArrayList<double[]> out=new ArrayList<>();
    RouteTraceGeometry.SegmentSink sink=(a,b,c,e,done)->out.add(new double[]{a,b,c,e,done?1:0});
    g.drawSegments(15,sink);
    check(out.size()==3 && out.get(0)[4]==1 && out.get(1)[4]==1 && out.get(2)[4]==0);
    check(out.get(1)[2]==5 && out.get(2)[0]==5 && out.get(2)[2]==0);
    out.clear(); g.drawSegments(0,sink);
    check(out.size()==2 && out.get(0)[4]==0 && out.get(1)[4]==0);
    out.clear(); g.drawSegments(20,sink);
    check(out.size()==2 && out.get(0)[4]==1 && out.get(1)[4]==1);
    reject(()->g.drawSegments(-1,sink)); reject(()->g.drawSegments(21,sink));
    reject(()->g.drawSegments(Double.NaN,sink)); reject(()->g.drawSegments(0,null));
    reject(()->new RouteTraceGeometry(new double[]{0,1},new double[]{0,1},new double[]{1,0}));
    reject(()->new RouteTraceGeometry(new double[]{0,Double.NaN},new double[]{0,1},new double[]{0,1}));
    reject(()->new RouteTraceGeometry(new double[5001],new double[5001],new double[5001]));
    RouteTraceGeometry longRoute=new RouteTraceGeometry(new double[5000],new double[5000],new double[5000]);
    check(longRoute.size()==5000); out.clear(); longRoute.drawSegments(0,sink); check(out.isEmpty());
  }
}
"""
class RouteTraceTest(unittest.TestCase):
    def test_geometry(self):
        with tempfile.TemporaryDirectory() as tmp:
            harness = Path(tmp) / "RouteTraceHarness.java"
            harness.write_text(HARNESS, encoding="utf-8")
            subprocess.run(["javac", "-encoding", "UTF-8", "-d", tmp,
                            str(ROOT / "src/dev/pgrazor/RouteTraceGeometry.java"),
                            str(harness)], check=True)
            subprocess.run(["java", "-cp", tmp, "RouteTraceHarness"], check=True)

if __name__ == "__main__":
    unittest.main()
