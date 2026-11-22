package dev.pgrazor;

public final class CooldownVisualTest {
    private static void check(boolean ok,String label){
        if(!ok)throw new AssertionError(label);
    }
    public static void main(String[] args){
        int orange=0xffff8500,red=0xffff3535;
        check(CooldownVisual.ready("0:00:00"),"zero should be ready");
        check(CooldownVisual.color("0:00:00",orange)==0xff00da69,"zero must become green");
        check(CooldownVisual.color("0:00:00",red)==0xff00da69,"configured red does not override ready green");
        check(!CooldownVisual.ready("0:00:01"),"one second still waiting");
        check(CooldownVisual.color("0:00:01",orange)==orange,"retain selected countdown color");
        check(!CooldownVisual.ready("—"),"unknown location/history is not zero");
        check(CooldownVisual.color("—",red)==red,"no false green on unavailable location");
        check(!CooldownVisual.ready(null),"null is not ready");
        check(!CooldownVisual.ready("0:01:00"),"one minute not ready");
        System.out.println("PASS cooldown zero color and unknown location protection");
    }
}
