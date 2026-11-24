package dev.pgrazor;

/** Direct JVM regression for Events (512), Selector (8192) and persisted masks. */
public final class QuickMenuVisibilityPolicyTest {
    private static void ok(boolean yes, String msg) {
        if (!yes) throw new AssertionError(msg);
    }
    public static void main(String[] args) {
        int events=QuickMenuVisibilityPolicy.EVENTS_BIT;
        int selector=QuickMenuVisibilityPolicy.SELECTOR_BIT;
        ok(events==512&&selector==8192,"stable mapping of persisted bits");
        ok(QuickMenuVisibilityPolicy.MASK==16383,"14 independent menu entries");
        ok(QuickMenuVisibilityPolicy.visible(0,0),"settings always visible");
        ok(!QuickMenuVisibilityPolicy.validBit(0),"settings cannot be disabled");
        ok(!QuickMenuVisibilityPolicy.validBit(3),"non-power-of-two rejected");
        ok(!QuickMenuVisibilityPolicy.validBit(16384),"outside mask rejected");
        int hidden=0;
        hidden=QuickMenuVisibilityPolicy.withVisibility(hidden,events,false);
        ok(!QuickMenuVisibilityPolicy.visible(hidden,events),"Events hidden");
        ok(QuickMenuVisibilityPolicy.visible(hidden,selector),"Selector unaffected");
        hidden=QuickMenuVisibilityPolicy.withVisibility(hidden,selector,false);
        ok(hidden==8704,"Events and Selector both persist");
        ok(!QuickMenuVisibilityPolicy.visible(hidden,selector),"Selector hidden");
        hidden=QuickMenuVisibilityPolicy.withVisibility(hidden,events,true);
        ok(hidden==selector,"Events restored without changing Selector");
        ok(QuickMenuVisibilityPolicy.visible(hidden,events),"Events restored");
        ok(!QuickMenuVisibilityPolicy.visible(hidden,selector),"Selector preserved");
        hidden=QuickMenuVisibilityPolicy.withVisibility(hidden,selector,true);
        ok(hidden==0,"restore all");
        ok(QuickMenuVisibilityPolicy.sanitize(-1)==0,"negative stored mask fails closed");
        ok(QuickMenuVisibilityPolicy.sanitize(32768)==0,"unsupported bits ignored");
        ok(QuickMenuVisibilityPolicy.withVisibility(512,3,false)==512,"invalid mask unchanged");
        ok(QuickMenuVisibilityPolicy.visible(8192,0),"settings not hidden by Selector");
        int locations=QuickMenuVisibilityPolicy.LOCATIONS_BIT;
        ok(QuickMenuVisibilityPolicy.visible(32,locations)&&QuickMenuVisibilityPolicy.visible(64,locations),"either legacy shortcut remains available");
        ok(!QuickMenuVisibilityPolicy.visible(96,locations),"both formerly hidden");
        ok(QuickMenuVisibilityPolicy.withVisibility(512,locations,false)==608,"hide both legacy bits, preserve events");
        ok(QuickMenuVisibilityPolicy.withVisibility(608,locations,true)==512,"restore Locais only");
        System.out.println("PASS QuickMenuVisibilityPolicy: Events, Selector, persisted masks");
    }
}
