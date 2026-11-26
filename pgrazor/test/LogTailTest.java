package dev.pgrazor;
import java.io.*;
import java.nio.file.*;
import java.util.Arrays;
final class LogTailTest {
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    public static void main(String[] args)throws Exception{
        Path path=Files.createTempFile("pgbr-log-tail-",".txt");
        try{
            check(LogTail.read(path.toFile()).bytes.length==0,"empty");
            Files.write(path,new byte[]{1,2,3});
            LogTail.Snapshot small=LogTail.read(path.toFile());
            check(Arrays.equals(small.bytes,new byte[]{1,2,3})&&!small.truncated,"small unchanged");
            byte[] longLog=new byte[LogTail.MAX_BYTES+500];
            for(int i=0;i<longLog.length;i++)longLog[i]=(byte)(i%127);
            Files.write(path,longLog);
            LogTail.Snapshot tail=LogTail.read(path.toFile());
            check(tail.truncated&&tail.bytes.length==LogTail.MAX_BYTES,"bounded");
            check(Arrays.equals(tail.bytes,Arrays.copyOfRange(longLog,500,longLog.length)),"exact tail");
            try(RandomAccessFile input=new RandomAccessFile(path.toFile(),"r")){
                input.seek(1);check(Arrays.equals(LogTail.read(input).bytes,tail.bytes),"independent of current pointer");
            }
            check(Arrays.equals(Files.readAllBytes(path),longLog),"no mutation");
            Files.write(path,new byte[LogTail.MAX_BYTES]);
            check(!LogTail.read(path.toFile()).truncated,"exact boundary");
            try{LogTail.read(path.resolveSibling("nonexistent-log-pgbr").toFile());throw new AssertionError("missing accepted");}catch(IOException expected){}
            System.out.println("PASS LogTail");
        }finally{Files.deleteIfExists(path);}
    }
}
