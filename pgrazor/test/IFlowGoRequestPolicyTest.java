package dev.pgrazor;

/** Offline, network-free regression coverage of global/nearby request bounds. */
public final class IFlowGoRequestPolicyTest {
    private static void equal(double a, double b) { if (a != b) throw new AssertionError(a + " != " + b); }
    private static void equal(String a, String b) { if (!a.equals(b)) throw new AssertionError(a + " != " + b); }
    private static void invalid(Runnable r) {
        try { r.run(); } catch (IllegalArgumentException expected) { return; }
        throw new AssertionError("Expected explicit rejection instead of silent broadening");
    }
    public static void main(String[] args) {
        equal(IFlowGoRequestPolicy.nearbyRadiusKm(1.0), 1.0);
        equal(IFlowGoRequestPolicy.nearbyRadiusKm(3.0), 3.0);
        equal(IFlowGoRequestPolicy.nearbyRadiusKm(5.0), 5.0);
        equal(IFlowGoRequestPolicy.nearbyRadiusKm(250.0), 25.0);
        equal(IFlowGoRequestPolicy.nearbyRadiusKm(Double.POSITIVE_INFINITY), 25.0);
        invalid(() -> IFlowGoRequestPolicy.nearbyRadiusKm(Double.NaN));
        invalid(() -> IFlowGoRequestPolicy.nearbyRadiusKm(0.0));
        invalid(() -> IFlowGoRequestPolicy.nearbyRadiusKm(-1.0));
        invalid(() -> IFlowGoRequestPolicy.nearbyRadiusKm(Double.NEGATIVE_INFINITY));
        equal(IFlowGoRequestPolicy.globalSpeciesQuery(-1), "");
        equal(IFlowGoRequestPolicy.globalSpeciesQuery(1), "&pokemon_id=1");
        equal(IFlowGoRequestPolicy.globalSpeciesQuery(1025), "&pokemon_id=1025");
        invalid(() -> IFlowGoRequestPolicy.globalSpeciesQuery(0));
        invalid(() -> IFlowGoRequestPolicy.globalSpeciesQuery(1026));
        invalid(() -> IFlowGoRequestPolicy.globalSpeciesQuery(-2));
        System.out.println("IFlowGoRequestPolicy OK: 15 boundary assertions");
    }
}
