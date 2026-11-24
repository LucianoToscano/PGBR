package dev.pgrazor;

import android.content.Context;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.channels.FileChannel;
import java.nio.ByteBuffer;
import java.security.KeyStore;
import java.util.Arrays;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

/** Independent preview-only vault; never store API keys in a game process. */
final class LocalAiKeyVault {
    static final String OWN_PACKAGE="dev.pgrazor.preview";
    static boolean allowed(Context c){return c!=null&&OWN_PACKAGE.equals(c.getPackageName());}
    private static String alias(LocalAiKeyEnvelope.Provider p){return "pgbr-ai-local-v1-"+p.name();}
    private static File file(Context c,LocalAiKeyEnvelope.Provider p)throws IOException{
        if(!allowed(c))throw new IOException("Ambiente não permitido.");
        File root=c.getApplicationContext().getNoBackupFilesDir().getCanonicalFile();
        File dir=new File(root,"pgbr-ai-local-v1");
        if(!dir.getCanonicalFile().equals(dir)||(!dir.isDirectory()&&!dir.mkdirs()))throw new IOException("Armazenamento indisponível.");
        File result=new File(dir,p.name()+".enc");
        if(!result.getCanonicalFile().equals(result))throw new IOException("Caminho inválido.");
        return result;
    }
    private static SecretKey key(LocalAiKeyEnvelope.Provider p,boolean create)throws Exception{
        KeyStore ks=KeyStore.getInstance("AndroidKeyStore");ks.load(null);
        if(ks.containsAlias(alias(p))){
            KeyStore.Entry entry=ks.getEntry(alias(p),null);
            if(!(entry instanceof KeyStore.SecretKeyEntry))throw new IOException("Cofre indisponível.");
            return ((KeyStore.SecretKeyEntry)entry).getSecretKey();
        }
        if(!create)throw new IOException("Cofre indisponível.");
        KeyGenerator generator=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");
        generator.init(new KeyGenParameterSpec.Builder(alias(p),KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).setKeySize(256).build());
        return generator.generateKey();
    }
    static synchronized void save(Context c,LocalAiKeyEnvelope.Provider p,char[] chars)throws Exception{
        File dest=file(c,p);
        byte[] plaintext=LocalAiKeyEnvelope.encode(chars),sealed=null,verified=null;
        java.nio.file.Path temp=null;
        try{
            SecretKey key=key(p,true);
            sealed=LocalAiKeyEnvelope.seal(p,key,plaintext);
            verified=LocalAiKeyEnvelope.open(p,key,sealed);
            if(!java.security.MessageDigest.isEqual(plaintext,verified))throw new IOException("Verificação local falhou.");
            temp=Files.createTempFile(dest.getParentFile().toPath(),"key-",".tmp");
            try(FileChannel channel=FileChannel.open(temp,StandardOpenOption.WRITE,StandardOpenOption.TRUNCATE_EXISTING)){
                ByteBuffer buf=ByteBuffer.wrap(sealed);while(buf.hasRemaining())channel.write(buf);channel.force(true);
            }
            Files.move(temp,dest.toPath(),StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
        }finally{
            Arrays.fill(plaintext,(byte)0);if(sealed!=null)Arrays.fill(sealed,(byte)0);if(verified!=null)Arrays.fill(verified,(byte)0);
            if(temp!=null)Files.deleteIfExists(temp);
        }
    }
    static synchronized boolean configured(Context c,LocalAiKeyEnvelope.Provider p)throws Exception{
        File f=file(c,p);if(!f.exists())return false;
        if(!f.isFile()||f.length()>LocalAiKeyEnvelope.MAX_KEY+64)throw new IOException("Cofre inválido.");
        byte[] encrypted=Files.readAllBytes(f.toPath()),plain=null;
        try{plain=LocalAiKeyEnvelope.open(p,key(p,false),encrypted);return true;}
        finally{Arrays.fill(encrypted,(byte)0);if(plain!=null)Arrays.fill(plain,(byte)0);}
    }
    static synchronized void remove(Context c,LocalAiKeyEnvelope.Provider p)throws Exception{
        File f=file(c,p);Files.deleteIfExists(f.toPath());
        KeyStore ks=KeyStore.getInstance("AndroidKeyStore");ks.load(null);ks.deleteEntry(alias(p));
    }
}
