package dev.pgrazor;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

/** Bounded local JSON input, independent of Android storage and game code. */
final class BackupInput {
    private BackupInput(){}
    static final int MAX_DEPTH=64;
    static String readJson(InputStream in,int limit)throws IOException{
        if(in==null||limit<=0)throw new IOException("Invalid input");
        ByteArrayOutputStream out=new ByteArrayOutputStream(Math.min(limit,8192));
        byte[] buf=new byte[Math.min(limit,8192)];
        int n;
        while((n=in.read(buf,0,Math.min(buf.length,limit-out.size()+1)))!=-1){
            if(n==0){
                int one=in.read();
                if(one<0)break;
                if(out.size()==limit)throw new IOException("Backup is too large");
                out.write(one);
                continue;
            }
            if(n>limit-out.size())throw new IOException("Backup is too large");
            out.write(buf,0,n);
        }
        String json=StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(out.toByteArray())).toString();
        if(json.startsWith("\uFEFF"))json=json.substring(1);
        checkStructure(json);
        return json;
    }
    // Preflight before org.json: bound nesting, reject trailing roots and do not interpret braces in strings.
    static void checkStructure(String json)throws IOException{
        char[] stack=new char[MAX_DEPTH];
        int depth=0;
        boolean quoted=false,escaped=false,started=false,ended=false;
        for(int i=0;i<json.length();i++){
            char ch=json.charAt(i);
            if(quoted){
                if(escaped){escaped=false;continue;}
                if(ch=='\\'){escaped=true;continue;}
                if(ch=='"'){quoted=false;continue;}
                if(ch<32)throw new IOException("Invalid JSON string");
                continue;
            }
            if(ch==' '||ch=='\n'||ch=='\r'||ch=='\t')continue;
            if(ended)throw new IOException("Unexpected data after JSON");
            if(!started){if(ch!='{')throw new IOException("JSON object required");started=true;}
            if(ch=='"'){quoted=true;continue;}
            if(ch=='\''||ch=='/'||ch=='#')throw new IOException("Nonstandard JSON syntax");
            if(ch=='{'||ch=='['){
                if(depth==MAX_DEPTH)throw new IOException("Backup nesting limit");
                stack[depth++]=ch;continue;
            }
            if(ch=='}'||ch==']'){
                if(depth==0||stack[depth-1]!=(ch=='}'?'{':'['))throw new IOException("Invalid JSON nesting");
                if(--depth==0)ended=true;
            }
        }
        if(!ended||depth!=0||quoted||escaped)throw new IOException("Incomplete JSON object");
    }
    static String safeFailure(Throwable error){
        // Exception messages can contain names, file paths, URIs and imported personal data.
        if(error instanceof java.nio.charset.CharacterCodingException)return "Arquivo não está em UTF-8 válido. Nenhuma importação realizada.";
        if(error instanceof SecurityException)return "Permissão de armazenamento indisponível.";
        return "Falha no backup: arquivo inválido ou armazenamento indisponível.";
    }
}
