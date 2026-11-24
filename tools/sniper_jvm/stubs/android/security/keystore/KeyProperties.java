package android.security.keystore;

/** Espelho de android.security.keystore.KeyProperties. */
public final class KeyProperties {
    private KeyProperties() {}
    public static final int PURPOSE_ENCRYPT = 1;
    public static final int PURPOSE_DECRYPT = 2;
    public static final String KEY_ALGORITHM_AES = "AES";
    public static final String BLOCK_MODE_GCM = "GCM";
    public static final String ENCRYPTION_PADDING_NONE = "NoPadding";
}
