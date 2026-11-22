package dev.pgrazor;

/**
 * Provider capabilities, deliberately separated from the already-working PGBR hundo source.
 *
 * The public PGSharp product page lists a free 100-IV feed and a Standard all-coordinates
 * feature. Those are product entitlements, NOT a documented third-party HTTP API.
 * DEX inspection found a feed URL but not a validated request/response/auth contract.
 */
public final class PgSharpFeedPolicy {
    private PgSharpFeedPolicy() {}

    public enum Tier { FREE_100_IV, STANDARD_FILTERS }
    public enum Connector { UNVERIFIED, VERIFIED }
    public enum KeyStatus { ABSENT, STORED_UNVERIFIED, VERIFIED_BY_PROVIDER }

    /** Only a validated connector can retrieve results; a product-plan page is not an API. */
    public static boolean canFetch(Tier tier, Connector connector, KeyStatus key) {
        if (tier == null || connector != Connector.VERIFIED) return false;
        if (tier == Tier.FREE_100_IV) return true; // free: does not require a key
        return key == KeyStatus.VERIFIED_BY_PROVIDER; // NEVER trust an entered key alone
    }

    /** This build deliberately has no enabled PGSharp transport or license verifier. */
    public static Connector currentConnector() { return Connector.UNVERIFIED; }

    public static String freeStateText() {
        return "O feed 100% IV é gratuito no PGSharp. "
                + "A consulta direta pelo PGBR aguarda validação do contrato de dados; "
                + "nenhuma aparição PGSharp foi carregada.";
    }

    public static String paidStateText(KeyStatus key) {
        if (key == KeyStatus.STORED_UNVERIFIED) {
            return "Key guardada neste aparelho, ainda não autenticada. "
                    + "Os filtros Standard permanecem indisponíveis até "
                    + "a verificação do provedor estar operacional.";
        }
        return "Filtros adicionais exigem o plano correspondente e "
                + "verificação de licença pelo serviço PGSharp. "
                + "Nenhuma key é necessária para o feed gratuito.";
    }
}
