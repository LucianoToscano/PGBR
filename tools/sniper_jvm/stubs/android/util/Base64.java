package android.util;

/**
 * Espelho de android.util.Base64 para compilar/rodar sem android.jar.
 *
 * <p>A implementação delega ao java.util.Base64 (mesmo alfabeto e mesmas regras de NO_WRAP), então o
 * teste da sessão real exercita de fato a lógica de criptografia, não um retorno fixo.
 */
public final class Base64 {
    private Base64() {}
    public static final int DEFAULT = 0;
    public static final int NO_WRAP = 2;
    public static final int NO_PADDING = 1;

    public static byte[] decode(String input, int flags) {
        if (input == null) return null;
        try { return java.util.Base64.getDecoder().decode(input); }
        catch (IllegalArgumentException e) { return null; }
    }

    public static byte[] decode(byte[] input, int flags) {
        if (input == null) return null;
        try { return java.util.Base64.getDecoder().decode(input); }
        catch (IllegalArgumentException e) { return null; }
    }

    public static String encodeToString(byte[] input, int flags) {
        if (input == null) return null;
        return java.util.Base64.getEncoder().encodeToString(input);
    }

    public static byte[] encode(byte[] input, int flags) {
        if (input == null) return null;
        return java.util.Base64.getEncoder().encode(input);
    }
}
