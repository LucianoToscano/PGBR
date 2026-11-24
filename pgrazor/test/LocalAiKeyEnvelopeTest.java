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
        System.out.println("PASS: local AI credential envelope");
    }
}
