"""Host-side tests for the independent, read-only route trace model."""
from pathlib import Path
import subprocess
import tempfile
import unittest
from test_control_java import java_tools

ROOT = Path(__file__).resolve().parents[1]
HARNESS = r"""
import dev.pgrazor.RouteTraceGeometry;
import dev.pgrazor.MotionEngine;
import dev.pgrazor.RouteTraceStyle;
import dev.pgrazor.RouteTraceFrame;
import java.util.ArrayList;
public class RouteTraceHarness {
  static void check(boolean ok) { if (!ok) throw new AssertionError(); }
  static void reject(Runnable r) {
    try { r.run(); throw new AssertionError("accepted invalid input"); }
    catch (IllegalArgumentException expected) {}
  }
  static void testSnapshot() {
    MotionEngine e=new MotionEngine(); e.position(0,0); e.speed(3.6);
    check(e.routeVisualSnapshot(0)==null);
    e.walkVisualRoute(new double[][]{{0,0},{0,.0001},{0,.0002}},false);
    MotionEngine.RouteVisualSnapshot a=e.routeVisualSnapshot(0);
    check(a!=null && a.visited==1 && a.total==3 && a.newPoints.length==3 && Math.abs(a.legFraction)<1e-8);
    a.newPoints[1][1]=999;
    for(int i=0;i<20;i++) e.step(.25);
    MotionEngine.RouteVisualSnapshot c=e.routeVisualSnapshot(a.token);
    check(c.newPoints==null && c.legFraction>.4 && c.legFraction<.5 && c.remainingMeters<a.remainingMeters);
    check(Math.abs(c.completedMeters-5)<1e-4 && Math.abs(c.routeMeters-c.completedMeters-c.remainingMeters)<1e-4);
    check(e.routeVisualSnapshot(0).newPoints[1][1]==.0001);
    check(e.setVisualRoutePaused(c.token,true)); double before=e.longitude; e.step(.25); check(e.longitude==before&&e.metersPerSecond()==0&&e.routeVisualSnapshot(c.token).paused&&e.speedKmh==3.6);check(!e.setVisualRoutePaused(c.token+1,false));check(e.setVisualRoutePaused(c.token,false));e.step(.25);check(e.longitude>before);
    e.speed(0); check(e.routeVisualSnapshot(c.token).paused);
    e.stop(); check(e.routeVisualSnapshot(c.token)==null);
    e.speed(3.6); e.position(0,0); e.walkVisualRoute(new double[][]{{0,0},{0,.000001}},true);
    MotionEngine.RouteVisualSnapshot loop=e.routeVisualSnapshot(c.token);
    check(loop!=null && loop.looping && loop.newPoints!=null && loop.token!=c.token);
    e.step(.25); check(e.routeVisualSnapshot(loop.token)!=null);
    e.stop(); e.position(0,0); e.walkVisualRoute(new double[][]{{0,0},{0,.00001}},true);
    MotionEngine.RouteVisualSnapshot start=e.routeVisualSnapshot(0);
    check(!start.returning && start.routeMeters>2.2 && start.routeMeters<2.3);
    for(int i=0;i<5;i++) e.step(.25);
    MotionEngine.RouteVisualSnapshot back=e.routeVisualSnapshot(start.token);
    check(back.returning && back.visited==2 && back.legFraction>0 && back.legFraction<1);
    check(Math.abs(back.completedMeters-1.25)<1e-4);
    for(int i=0;i<4;i++)e.step(.25);
    MotionEngine.RouteVisualSnapshot lap=e.routeVisualSnapshot(start.token);
    check(!lap.returning && lap.completedMeters<.1 && lap.visited==1);
    e.stop(); e.position(0,0); e.walkTo(0,.0001); check(e.routeVisualSnapshot(0)==null);
    e.walkRoute(new double[][]{{0,0},{0,.0001}},false);
    long ordinary=e.activeRouteToken();check(ordinary!=0&&e.activeVisualRouteToken()==0&&e.routeVisualSnapshot(0)==null);
    check(!e.stopVisualRoute(ordinary)&&!e.setVisualRoutePaused(ordinary,true)&&e.walking);
    e.stop();e.position(0,0);e.walkVisualTo(0,.0001);
    MotionEngine.RouteVisualSnapshot direct=e.routeVisualSnapshot(0);
    check(direct!=null&&direct.total==2&&direct.visited==1&&direct.newPoints.length==2&&e.activeRouteToken()==0);
    check(direct.newPoints[0][1]==0&&direct.newPoints[1][1]==.0001);
    for(int i=0;i<20;i++)e.step(.25);
    MotionEngine.RouteVisualSnapshot progressed=e.routeVisualSnapshot(direct.token);
    check(progressed.newPoints==null&&Math.abs(progressed.completedMeters-5)<1e-4&&progressed.legFraction>.4&&progressed.legFraction<.5);
    e.walkVisualRoute(new double[][]{{0,.001}},false);
    check(e.routeVisualSnapshot(0).newPoints.length==2&&e.activeRouteToken()!=0);
    check(!e.stopVisualRoute(direct.token)&&e.walking);
    check(e.stopVisualRoute(e.activeVisualRouteToken())&&!e.walking&&e.routeVisualSnapshot(0)==null);
    e.walkVisualTo(0,.0001);e.walkRoute(new double[][]{{0,.002}},false);
    check(e.routeVisualSnapshot(0)==null&&e.activeRouteToken()!=0);
    e.stop(); e.position(0,0); e.walkVisualRoute(new double[][]{{0,0},{0,.000001}},false);
    e.step(.25); check(e.routeVisualSnapshot(0)==null);
  }
  public static void main(String[] args) {
    testSnapshot();
    RouteTraceGeometry projected=new RouteTraceGeometry(new double[]{20,30},new double[]{40,50},new double[]{0,10});
    RouteTraceFrame frame=new RouteTraceFrame(42,1000,360,800,true,projected,5);
    check(frame.usable(1250,42,360,800,true));
    check(!frame.usable(1251,42,360,800,true));
    check(!frame.usable(999,42,360,800,true));
    check(!frame.usable(1000,43,360,800,true));
    check(!frame.usable(1000,42,800,360,true));
    check(!frame.usable(1000,42,360,800,false));
    check(!new RouteTraceFrame(42,1000,360,800,false,projected,5).usable(1000,42,360,800,true));
    reject(()->new RouteTraceFrame(42,1000,360,800,true,projected,11));
    reject(()->new RouteTraceFrame(42,1000,360,800,true,new RouteTraceGeometry(new double[]{0,2000},new double[]{0,5},new double[]{0,10}),0));

    RouteTraceStyle style=RouteTraceStyle.defaults();
    check(style.width==5 && style.opacity==100 && style.color(style.remaining)==0xffeeeeee);
    style=new RouteTraceStyle(-1,10,0,0x123456,false,false,false,false);
    check(style.width==2 && style.opacity==30 && style.completed==0xff123456);
    check((style.color(style.completed)>>>24)==77 && !style.showMarkers && !style.showPreview);
    style=new RouteTraceStyle(100,1000,0,0,true,true,true,true);
    check(style.width==12 && style.opacity==100);
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
        compiler, runtime = java_tools()
        with tempfile.TemporaryDirectory() as tmp:
            harness = Path(tmp) / "RouteTraceHarness.java"
            harness.write_text(HARNESS, encoding="utf-8")
            subprocess.run([str(compiler), "-encoding", "UTF-8", "-d", tmp,
                            str(ROOT / "src/dev/pgrazor/RouteTraceGeometry.java"),
                            str(ROOT / "src/dev/pgrazor/MotionEngine.java"),
                            str(ROOT / "src/dev/pgrazor/RouteTraceStyle.java"),
                            str(ROOT / "src/dev/pgrazor/RouteTraceFrame.java"),
                            str(harness)], check=True, timeout=30)
            subprocess.run([str(runtime), "-cp", tmp, "RouteTraceHarness"], check=True, timeout=30)

    def test_ground_projection(self):
        compiler, runtime = java_tools()
        with tempfile.TemporaryDirectory() as tmp:
            subprocess.run([str(compiler), "-encoding", "UTF-8", "-d", tmp,
                            str(ROOT / "src/dev/pgrazor/RouteGroundProjection.java"),
                            str(ROOT / "test/ProjectionTest.java")], check=True, timeout=30)
            subprocess.run([str(runtime), "-cp", tmp, "ProjectionTest"], check=True, timeout=30)

    def test_screen_projection(self):
        compiler, runtime = java_tools()
        with tempfile.TemporaryDirectory() as tmp:
            files=["RouteGroundProjection.java","RouteScreenTracker.java","RouteScreenFrameBuilder.java","RouteTraceGeometry.java","RouteTraceFrame.java"]
            subprocess.run([str(compiler), "-encoding", "UTF-8", "-d", tmp,
                            *[str(ROOT / "src/dev/pgrazor" / name) for name in files],
                            str(ROOT / "test/ScreenProjectionTest.java")], check=True, timeout=30)
            subprocess.run([str(runtime), "-cp", tmp, "ScreenProjectionTest"], check=True, timeout=30)

    def test_overlay_boundaries(self):
        overlay=(ROOT / "src/dev/pgrazor/RouteTraceOverlay.java").read_text(encoding="utf-8")
        view=(ROOT / "src/dev/pgrazor/RouteTraceView.java").read_text(encoding="utf-8")
        for token in ["source==null", "parent.hasWindowFocus()", "frame.usable(", "view.postDelayed(expire", "view.clearTrace()", "parent.removeView(view)"]:
            self.assertIn(token, overlay)
        self.assertIn("onTouchEvent(android.view.MotionEvent event){return false;}", view)
        pad=(ROOT / "src/dev/pgrazor/Pad.java").read_text(encoding="utf-8")
        for token in ["void attachVisualRouteSource(", "if(source!=null) visualRouteOverlay=", "if(visualRouteOverlay==null)return", "!Control.catchScreenVisible()&&!blackShown", "visualRouteOverlay.close()"]:
            self.assertIn(token, pad)
        self.assertEqual(pad.count("attachVisualRouteSource("),2)
        self.assertIn("source.calibrate(()->", pad)
        self.assertIn("Control::vgpMapVisible", pad)
        self.assertIn("screenRouteSource.close()", pad)
        self.assertIn("Control.engine.activeVisualRouteToken()",pad)
        self.assertIn("allowed&&!Control.gameRoutesRunning()",pad)
        self.assertIn("Control.walkVisualTo(lat, lon)",pad)
        self.assertIn("Control.walkVisualRoute(waypoints, loop)",pad)
        official=(ROOT / "src/dev/pgrazor/GameRoutesController.java").read_text(encoding="utf-8")
        self.assertNotIn("walkVisual",official)
        self.assertIn("engine.walkRoute(pts, false)",official)
        dialog=(ROOT / "src/dev/pgrazor/GameRoutesDialog.java").read_text(encoding="utf-8")
        self.assertIn("setEnabled(running||visualToken!=0)",dialog)
        self.assertIn("Control.stopVisualWalk(token)",dialog)
        screen=(ROOT / "src/dev/pgrazor/RouteScreenProjectionSource.java").read_text(encoding="utf-8")
        for token in ["PixelCopy.request(surface", "matches.size()!=1", "mapVisible.getAsBoolean()", "if(n[0]!=6)", "RouteScreenTracker", "RouteScreenFrameBuilder.build", "bitmap.recycle()", "worker.shutdown()", "routeVisualSnapshot(knownToken)", "trackingLost", "ready=false"]:
            self.assertIn(token, screen)
        for forbidden in ["NativeCore", "Runtime.getRuntime", "java.net.", "FileOutputStream", ".walkRoute(", ".teleport(", ".input("]:
            self.assertNotIn(forbidden, screen)
        settings=(ROOT / "src/dev/pgrazor/SettingsPanel.java").read_text(encoding="utf-8")
        self.assertIn("host::calibrateVisualRoute", settings)
        self.assertIn("host::disableVisualRoute", settings)

        for forbidden in ["Control.", "MotionEngine", "NativeCore", "System.load", "inject"]:
            self.assertNotIn(forbidden, overlay)

    def test_map_intelligent_controls(self):
        source=(ROOT/"src/dev/pgrazor/WorldMapDialog.java").read_text(encoding="utf-8")
        for token in ["map.zoom(1)","map.zoom(-1)","map.centerCurrentLocation()","map.poiLayer.openNearbyList()","Cooldown.remainingAt(latitude,longitude)","Collections.reverse(draft)","SavedPlaces.CATEGORIES","layout.addView(selected)"]:
            self.assertIn(token,source)
        poi=(ROOT/"src/dev/pgrazor/MapPoiLayer.java").read_text(encoding="utf-8")
        self.assertIn("((layers & 8) != 0 && rocketName(point,now)!=null)",poi)
        self.assertIn("void openNearbyList()",poi)
        for token in ["mapFavorites","mapImportedPower","mapOfficialRoutes","event.isActionable(now)","drawOfficialRoutes(canvas)","Control.setMapRoutesOpen(false)","gameRoutesMapSnapshot()","setVisualRoutePaused(token"]:
            self.assertIn(token,source)
        controller=(ROOT/"src/dev/pgrazor/GameRoutesController.java").read_text(encoding="utf-8")
        self.assertIn("ready&&freshSnapshot()?rawSnapshot",controller)

    def test_map_library_and_coordinates_layout(self):
        source=(ROOT/"src/dev/pgrazor/WorldMapDialog.java").read_text(encoding="utf-8")
        self.assertEqual(source.count("layout.addView(selectionRow)"),1)
        self.assertNotIn("layout.addView(libraryTabs)",source)
        self.assertNotIn("Button savePin=",source)
        self.assertIn('Button saved=button(ui,"Locais")',source)
        self.assertNotIn("selectionRow.addView(savedRoutes",source)
        self.assertIn('new String[]{"Salvos","Pontos","Rotas GPX","Missões / Eventos"}',source)
        quick=(ROOT/"src/dev/pgrazor/QuickMenu.java").read_text(encoding="utf-8")
        self.assertIn('row(MAP, "Locais", true, listener::onLocations',quick)
        self.assertNotIn('row(FAVORITES, "Favorites"',quick)
        self.assertNotIn('row(MAP, "Hotspots"',quick)
        self.assertIn("Button gpx = route == null ? null : savedRoutes",source)
        self.assertIn("layout.addView(status);\n        layout.addView(selected);\n        layout.addView(copyRow);",source)
        self.assertIn('info+="\\n"+CoordinateClipboard.text(latitude,longitude)',source)
        self.assertIn("detail,CoordinateClipboard.text(entry.latitude,entry.longitude)",source)
        points=(ROOT/"src/dev/pgrazor/HotspotsDialog.java").read_text(encoding="utf-8")
        self.assertIn("coordinates.setText(CoordinateClipboard.text(spot.latitude,spot.longitude))",points)
        self.assertLess(points.index("card.addView(tags)"),points.index("card.addView(coordinates)"))

    def test_map_integration(self):
        source = (ROOT / "src/dev/pgrazor/WorldMapDialog.java").read_text(encoding="utf-8")
        for token in ["routeVisualSnapshot(traceToken)", "routeTraceEnabled", "activeTrace=Collections.emptyList()", "drawTraceSegment(canvas", "traceState.legFraction", "traceState.completedMeters", "Prévia: caminho planejado"]:
            self.assertIn(token, source)
        self.assertIn("preferences.getBoolean(\"routeTraceEnabled\",false)", source)
        self.assertIn("removeCallbacks(locationRefresh)", source)
        self.assertIn("traceSummary=null", source)
        for token in ["RouteTraceSettings.show(", "traceStyle.showProgress", "traceStyle.showMarkers", "traceStyle.showPreview", "traceStyle.showSummary", "traceStyle.color(traceStyle.completed)", "traceState.returning", "map.setDraftLoop(on)"]:
            self.assertIn(token, source)
        settings=(ROOT / "src/dev/pgrazor/RouteTraceSettings.java").read_text(encoding="utf-8")
        self.assertIn("setNeutralButton(\"Restaurar padrão\"", settings)
        self.assertNotIn("Control.", settings)
        self.assertNotIn("MotionEngine", settings)

if __name__ == "__main__":
    unittest.main()
