import com.android.apksig.ApkSigner;
import com.android.apksig.ApkVerifier;
import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;
import java.util.HexFormat;

/** Uses Google's Apache-licensed APK signing library; never handles a provider's key. */
public final class PrivateApkSign {
    static String verifiedSigner(File apk) throws Exception {
        ApkVerifier.Result result = new ApkVerifier.Builder(apk).build().verify();
        if (!result.isVerified() || result.getSignerCertificates().size() != 1)
            throw new IllegalArgumentException("APK signature verification failed: " + result.getErrors());
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(result.getSignerCertificates().get(0).getEncoded()));
    }
    public static void main(String[] args) throws Exception {
        if (args.length == 2 && args[0].equals("verify")) {
            System.out.println(verifiedSigner(new File(args[1])));
            return;
        }
        if (args.length != 5 || !args[0].equals("sign"))
            throw new IllegalArgumentException("verify APK | sign INPUT OUTPUT KEYSTORE ALIAS");
        File input = new File(args[1]), output = new File(args[2]), keystore = new File(args[3]);
        if (!input.isFile() || output.exists() || !keystore.isFile())
            throw new IllegalArgumentException("Expected existing input/key and new output");
        String password = System.getenv("PGBR_HYBRID_STORE_PASS");
        if (password == null || password.isEmpty())
            throw new IllegalArgumentException("Dedicated local test keystore password required");
        char[] pass = password.toCharArray();
        try {
            KeyStore ks = KeyStore.getInstance("PKCS12");
            try (FileInputStream stream = new FileInputStream(keystore)) { ks.load(stream, pass); }
            if (!ks.isKeyEntry(args[4])) throw new IllegalArgumentException("Unknown test alias");
            PrivateKey key = (PrivateKey)ks.getKey(args[4], pass);
            List<X509Certificate> chain = Arrays.stream(ks.getCertificateChain(args[4]))
                    .map(c -> (X509Certificate)c).toList();
            ApkSigner.SignerConfig config = new ApkSigner.SignerConfig.Builder(args[4], key, chain).build();
            new ApkSigner.Builder(List.of(config))
                    .setInputApk(input).setOutputApk(output)
                    .setV1SigningEnabled(false).setV2SigningEnabled(true).setV3SigningEnabled(true)
                    .setV4SigningEnabled(false).setAlignmentPreserved(false)
                    .setLibraryPageAlignmentBytes(16384).build().sign();
            System.out.println(verifiedSigner(output));
        } catch (Exception error) {
            Files.deleteIfExists(output.toPath());
            throw error;
        } finally { Arrays.fill(pass, '\0'); }
    }
}
