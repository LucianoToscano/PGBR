package dev.pgrazor;
import java.io.*;
import java.nio.charset.StandardCharsets;
final class BackupInputTest {
    private static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
    private static String read(String s,int limit)throws IOException{return BackupInput.readJson(new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8)),limit);}
    private static void reject(String s,int limit)throws Exception{try{read(s,limit);throw new AssertionError("accepted invalid input");}catch(IOException expected){}}
    public static void main(String[] args)throws Exception{
        check(read("{}",2).equals("{}"),"exact byte limit");
        check(read("\uFEFF{\"name\":\"ação\"}",100).equals("{\"name\":\"ação\"}"),"BOM and UTF8");
        check(read("{\"name\":\"{[\\\"}]\"}",100).contains("name"),"escaped quotes and brackets");
        reject("{}",1);
        reject("{\"name\":\"ação\"}",10);
        reject("[]",100);
        reject("{}{}",100);
        reject("{\"x\":[}",100);
        reject("{\"x\":\"unterminated}",100);
        reject("{'x':1}",100);
        reject("{/*comment*/\"x\":1}",100);
        reject("{\"x\":\"a\nb\"}",100);
        reject("{\"x\":"+"[".repeat(64)+"]".repeat(64)+"}",1000);
        check(read("{\"x\":"+"[".repeat(63)+"]".repeat(63)+"}",1000).length()>100,"exact depth limit");
        try{BackupInput.readJson(new ByteArrayInputStream(new byte[]{123,34,(byte)0xc3,34,125}),100);throw new AssertionError("invalid UTF8");}catch(java.nio.charset.CharacterCodingException expected){}
        check(!BackupInput.safeFailure(new IOException("secret-coordinate-and-key")).contains("secret"),"no private error payload");
        check(!BackupInput.safeFailure(new SecurityException("content://private")).contains("content://"),"no private URI");
        System.out.println("PASS: BackupInput");
    }
}
