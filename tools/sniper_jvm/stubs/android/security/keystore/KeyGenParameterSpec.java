package android.security.keystore;

import java.security.spec.AlgorithmParameterSpec;

/** Espelho de android.security.keystore.KeyGenParameterSpec (apenas o que o app usa). */
public final class KeyGenParameterSpec implements AlgorithmParameterSpec {
    private KeyGenParameterSpec() {}
    public static final class Builder {
        public Builder(String alias, int purposes) {}
        public Builder setBlockModes(String... modes) { return this; }
        public Builder setEncryptionPaddings(String... paddings) { return this; }
        public Builder setKeySize(int size) { return this; }
        public Builder setRandomizedEncryptionRequired(boolean required) { return this; }
        public KeyGenParameterSpec build() { return new KeyGenParameterSpec(); }
    }
}
