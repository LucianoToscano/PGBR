package dev.pgrazor;

import java.security.GeneralSecurityException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/** Local credential format; no network, logging or game dependencies. */
final class LocalAiKeyEnvelope {
    enum Provider { GEMINI, GROQ }
    static final int MAX_KEY=512, IV=12, TAG=16;
    private static byte[] aad(Provider p){java.util.Objects.requireNonNull(p,"Provider required");return new byte[]{'P','A','I',1,(byte)(p==Provider.GEMINI?1:2)};}
    static byte[] encode(char[] chars){
        if(chars==null||chars.length<8||chars.length>MAX_KEY)throw new IllegalArgumentException("Chave inválida.");
        byte[] result=new byte[chars.length];
        for(int i=0;i<chars.length;i++){
            if(chars[i]<33||chars[i]>126){Arrays.fill(result,(byte)0);throw new IllegalArgumentException("Chave inválida.");}
            result[i]=(byte)chars[i];
        }
        return result;
    }
    static byte[] seal(Provider p,SecretKey key,byte[] plaintext)throws GeneralSecurityException{
        if(plaintext.length<8||plaintext.length>MAX_KEY)throw new GeneralSecurityException("Invalid length");
        Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,key);
        byte[] header=aad(p),iv=cipher.getIV();
        if(iv.length!=IV)throw new GeneralSecurityException("Invalid IV");
        cipher.updateAAD(header);
        byte[] encrypted=cipher.doFinal(plaintext);
        return ByteBuffer.allocate(header.length+IV+encrypted.length).put(header).put(iv).put(encrypted).array();
    }
    static byte[] open(Provider p,SecretKey key,byte[] envelope)throws GeneralSecurityException{
        byte[] header=aad(p);
        if(envelope.length<header.length+IV+TAG+8||envelope.length>header.length+IV+TAG+MAX_KEY)throw new GeneralSecurityException("Invalid length");
        for(int i=0;i<header.length;i++)if(envelope[i]!=header[i])throw new GeneralSecurityException("Invalid provider");
        Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE,key,new GCMParameterSpec(128,envelope,header.length,IV));cipher.updateAAD(header);
        return cipher.doFinal(envelope,header.length+IV,envelope.length-header.length-IV);
    }
}
