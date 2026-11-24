package dev.pgrazor;
public final class OverlayGestureTest {
 static void check(boolean b){if(!b)throw new AssertionError();}
 public static void main(String[] args){
  OverlayGesturePolicy p=new OverlayGesturePolicy();
  check(!p.down(100,0,0,24));check(!p.down(110,40,0,24));check(p.down(120,80,0,24));
  check(!p.accept(121));check(!p.down(200,0,0,24));check(p.accept(1200));
  p.clear();check(!p.down(2300,0,0,24));check(!p.down(2310,1,0,24));check(!p.down(2320,2,0,24));
  p.clear();check(!p.down(3000,0,0,24));check(!p.down(3300,40,0,24));check(!p.down(3310,80,0,24));
  check(!p.down(3320,Float.NaN,0,24));check(!p.down(3330,80,0,24));
  System.out.println("PASS: overlay gesture");
 }
}
