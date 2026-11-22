package dev.pgrazor;

import android.content.Context;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * An independent encrypted, no-backup storage slot for a user-provided PGSharp license key.
 * The key is NOT verified or transmitted by this class and does not unlock any feed.
 * Never include its contents in logs, crash reports, UI labels or source control.
 *
 * This slot is deliberately separate from the Pokémon GO account/session vault.
 * Android 9+ (API 28), consistent with the currently targeted PGSharp runtime.
 */
final class PgSharpLicenseVault {
    private static final String ALIAS = "pgbr-pgsharp-optional-license-v1";
    private static final String DIR = "pgbr-pgsharp-license-v1";
    private static final String FILE_NAME = "license.enc";
    private static final byte[] HEADER = {'P', 'G', 'K', 1};
    private static final int IV_LENGTH = 12;
    private static final int GCM_TAG_BITS = 128;
    private static final int MAX_UTF8 = 1024;

    private final File file;

    PgSharpLicenseVault(Context context) throws IOException {
        if (context == null) throw new IOException("Armazenamento indisponível.");
        File noBackup = context.getApplicationContext().getNoBackupFilesDir();
        if (noBackup == null) throw new IOException("Armazenamento indisponível.");
        File dir = new File(noBackup.getCanonicalFile(), DIR);
        if (!dir.isDirectory() && !dir.mkdirs())
            throw new IOException("Armazenamento indisponível.");
        file = new File(dir.getCanonicalFile(), FILE_NAME);
        if (!file.getCanonicalFile().equals(file.getAbsoluteFile()))
            throw new IOException("Caminho de armazenamento inválido.");
    }

    synchronized boolean hasSavedKey() { return file.isFile(); }

    /** Encrypts a key only after the user explicitly taps Save. Run on a worker. */
    synchronized void save(char[] characters) throws IOException {
        if (characters == null || characters.length < 4 || characters.length > 256)
            throw new IOException("Key inválida ou muito longa.");
        ByteBuffer encoded = StandardCharsets.UTF_8.encode(CharBuffer.wrap(characters));
        byte[] clear = new byte[encoded.remaining()];
        encoded.get(clear);
        if (clear.length < 4 || clear.length > MAX_UTF8) {
            Arrays.fill(clear, (byte) 0);
            throw new IOException("Key inválida ou muito longa.");
        }
        byte[] envelope;
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, loadOrCreateKey());
            byte[] iv = cipher.getIV();
            if (iv == null || iv.length != IV_LENGTH) throw new IOException("Falha de criptografia.");
            cipher.updateAAD(HEADER);
            byte[] data = cipher.doFinal(clear);
            envelope = ByteBuffer.allocate(HEADER.length + IV_LENGTH + data.length)
                    .put(HEADER).put(iv).put(data).array();
        } catch (GeneralSecurityException e) {
            throw new IOException("Falha de criptografia.");
        } finally {
            Arrays.fill(clear, (byte) 0);
            if (encoded.hasArray()) Arrays.fill(encoded.array(), (byte) 0);
        }
        Path parent = file.getParentFile().toPath();
        Path temp = Files.createTempFile(parent, "license-", ".tmp");
        boolean installed = false;
        try {
            try (FileChannel channel = FileChannel.open(temp, StandardOpenOption.WRITE,
                    StandardOpenOption.TRUNCATE_EXISTING)) {
                ByteBuffer bytes = ByteBuffer.wrap(envelope);
                while (bytes.hasRemaining()) channel.write(bytes);
                channel.force(true);
            }
            Files.move(temp, file.toPath(),
                    StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            installed = true;
        } finally {
            Arrays.fill(envelope, (byte) 0);
            if (!installed) Files.deleteIfExists(temp);
        }
    }

    /**
     * Reserved for a future verified provider-login connector. Never invoke from
     * an unverified transport; caller must zero the returned bytes promptly.
     */
    synchronized byte[] read() throws IOException {
        if (!file.isFile()) return null;
        byte[] contents = Files.readAllBytes(file.toPath());
        try {
            if (contents.length < HEADER.length + IV_LENGTH + 16
                    || contents.length > HEADER.length + IV_LENGTH + MAX_UTF8 + 16)
                throw new IOException("Key armazenada inválida.");
            for (int i = 0; i < HEADER.length; i++)
                if (contents[i] != HEADER[i]) throw new IOException("Key armazenada inválida.");
            byte[] iv = Arrays.copyOfRange(contents, HEADER.length, HEADER.length + IV_LENGTH);
            byte[] encrypted = Arrays.copyOfRange(contents, HEADER.length + IV_LENGTH, contents.length);
            try {
                Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
                cipher.init(Cipher.DECRYPT_MODE, loadExistingKey(),
                        new GCMParameterSpec(GCM_TAG_BITS, iv));
                cipher.updateAAD(HEADER);
                return cipher.doFinal(encrypted);
            } catch (GeneralSecurityException e) {
                throw new IOException("Não foi possível recuperar a key armazenada.");
            } finally {
                Arrays.fill(iv, (byte) 0);
                Arrays.fill(encrypted, (byte) 0);
            }
        } finally {
            Arrays.fill(contents, (byte) 0);
        }
    }

    synchronized void remove() throws IOException {
        Files.deleteIfExists(file.toPath());
    }

    private static SecretKey loadExistingKey() throws GeneralSecurityException, IOException {
        KeyStore store = KeyStore.getInstance("AndroidKeyStore");
        store.load(null);
        KeyStore.Entry entry = store.getEntry(ALIAS, null);
        if (!(entry instanceof KeyStore.SecretKeyEntry))
            throw new IOException("A chave de criptografia local não está disponível.");
        return ((KeyStore.SecretKeyEntry) entry).getSecretKey();
    }

    private static SecretKey loadOrCreateKey() throws GeneralSecurityException, IOException {
        KeyStore store = KeyStore.getInstance("AndroidKeyStore");
        store.load(null);
        if (store.containsAlias(ALIAS)) return loadExistingKey();
        KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,
                "AndroidKeyStore");
        generator.init(new KeyGenParameterSpec.Builder(ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256).build());
        return generator.generateKey();
    }
}
