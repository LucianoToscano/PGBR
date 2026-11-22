package dev.pgrazor;

/** Tests entitlement and fail-closed transport gating without an Android device. */
public final class PgSharpFeedPolicyTest {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    public static void main(String[] args) {
        PgSharpFeedPolicy.Tier free = PgSharpFeedPolicy.Tier.FREE_100_IV;
        PgSharpFeedPolicy.Tier standard = PgSharpFeedPolicy.Tier.STANDARD_FILTERS;
        PgSharpFeedPolicy.Connector absent = PgSharpFeedPolicy.Connector.UNVERIFIED;
        PgSharpFeedPolicy.Connector verified = PgSharpFeedPolicy.Connector.VERIFIED;
        PgSharpFeedPolicy.KeyStatus none = PgSharpFeedPolicy.KeyStatus.ABSENT;
        PgSharpFeedPolicy.KeyStatus stored = PgSharpFeedPolicy.KeyStatus.STORED_UNVERIFIED;
        PgSharpFeedPolicy.KeyStatus validated = PgSharpFeedPolicy.KeyStatus.VERIFIED_BY_PROVIDER;

        check(PgSharpFeedPolicy.currentConnector() == absent, "unverified in this build");
        check(!PgSharpFeedPolicy.canFetch(free, absent, none), "no free request without contract");
        check(!PgSharpFeedPolicy.canFetch(free, absent, validated), "key must not bypass missing contract");
        check(PgSharpFeedPolicy.canFetch(free, verified, none), "free does not require key");
        check(!PgSharpFeedPolicy.canFetch(standard, verified, none), "no paid without key");
        check(!PgSharpFeedPolicy.canFetch(standard, verified, stored), "locally stored key not verified");
        check(PgSharpFeedPolicy.canFetch(standard, verified, validated), "provider-verified key enables tier");
        check(!PgSharpFeedPolicy.canFetch(standard, absent, validated), "no unverified transport");
        check(!PgSharpFeedPolicy.canFetch(null, verified, validated), "null tier fails closed");
        check(PgSharpFeedPolicy.freeStateText().contains("gratuito"), "free tier visible");
        check(PgSharpFeedPolicy.paidStateText(stored).contains("não autenticada"),
                "saved is not authenticated");
        System.out.println("PgSharpFeedPolicyTest passed");
    }
}
