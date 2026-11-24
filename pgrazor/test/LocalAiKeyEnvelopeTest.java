package dev.pgrazor;
import java.util.Arrays;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
public class LocalAiKeyEnvelopeTest {
    public static void main(String[] args)throws Exception{
        KeyGenerator gen=KeyGenerator.getInstance("AES");gen.init(256);SecretKey key=gen.generateKey();
        byte[] input=LocalAiKeyEnvelope.encode("synthetic-test-only".toCharArray());
        var p=LocalAiKeyEnvelope.Provider.GEMINI;
        byte[] first=LocalAiKeyEnvelope.seal(p,key,input),second=LocalAiKeyEnvelope.seal(p,key,input);
        if(Arrays.equals(first,second)||!Arrays.equals(input,LocalAiKeyEnvelope.open(p,key,first)))throw new AssertionError("roundtrip or IV");
        try{LocalAiKeyEnvelope.open(LocalAiKeyEnvelope.Provider.GROQ,key,first);throw new AssertionError("cross-provider");}catch(java.security.GeneralSecurityException expected){}
        first[first.length-1]^=1;
        try{LocalAiKeyEnvelope.open(p,key,first);throw new AssertionError("tampering");}catch(java.security.GeneralSecurityException expected){}
        try{LocalAiKeyEnvelope.encode("bad key\n".toCharArray());throw new AssertionError("invalid");}catch(IllegalArgumentException expected){}
        try{LocalAiKeyEnvelope.seal(null,key,input);throw new AssertionError("null provider");}catch(NullPointerException expected){}
        try{LocalAiKeyEnvelope.open(null,key,second);throw new AssertionError("null provider on read");}catch(NullPointerException expected){}
        byte[] invalid="bad key!".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        try{LocalAiKeyEnvelope.seal(p,key,invalid);throw new AssertionError("invalid bytes");}catch(java.security.GeneralSecurityException expected){}
        byte[] max=LocalAiKeyEnvelope.encode("a".repeat(LocalAiKeyEnvelope.MAX_KEY).toCharArray());
        byte[] maxEnvelope=LocalAiKeyEnvelope.seal(p,key,max);
        if(maxEnvelope.length!=LocalAiKeyEnvelope.MAX_ENVELOPE||!Arrays.equals(max,LocalAiKeyEnvelope.open(p,key,maxEnvelope)))throw new AssertionError("maximum boundary");
        try{LocalAiKeyEnvelope.open(p,key,Arrays.copyOf(maxEnvelope,maxEnvelope.length+1));throw new AssertionError("oversized envelope");}catch(java.security.GeneralSecurityException expected){}
        try{LocalAiKeyEnvelope.open(p,key,null);throw new AssertionError("null envelope");}catch(java.security.GeneralSecurityException expected){}
        // Authenticated legacy envelope with an invalid credential must still be rejected.
        javax.crypto.Cipher legacy=javax.crypto.Cipher.getInstance("AES/GCM/NoPadding");
        legacy.init(javax.crypto.Cipher.ENCRYPT_MODE,key);
        byte[] header={'P','A','I',1,1};legacy.updateAAD(header);
        byte[] ciphertext=legacy.doFinal(invalid);
        byte[] legacyEnvelope=java.nio.ByteBuffer.allocate(header.length+legacy.getIV().length+ciphertext.length).put(header).put(legacy.getIV()).put(ciphertext).array();
        try{LocalAiKeyEnvelope.open(p,key,legacyEnvelope);throw new AssertionError("authenticated invalid credential");}catch(java.security.GeneralSecurityException expected){}
        Arrays.fill(max,(byte)0);Arrays.fill(invalid,(byte)0);
        Arrays.fill(input,(byte)0);
        System.out.println("PASS: local AI credential envelope");
    }
}
