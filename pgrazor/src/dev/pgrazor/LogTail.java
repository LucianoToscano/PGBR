package dev.pgrazor;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;

/** Read-only, bounded snapshot of the current segment. Appends after the snapshot are not read. */
final class LogTail {
    static final int MAX_BYTES=65536;
    private LogTail(){}
    static final class Snapshot {
        final byte[] bytes;
        final boolean truncated;
        Snapshot(byte[] bytes,boolean truncated){this.bytes=bytes;this.truncated=truncated;}
    }
    static Snapshot read(File file)throws IOException{
        if(file==null||!file.isFile())throw new IOException("Log unavailable");
        try(RandomAccessFile input=new RandomAccessFile(file,"r")){
            return read(input);
        }
    }
    static Snapshot read(RandomAccessFile input)throws IOException{
        long end=input.length();
        long start=Math.max(0,end-MAX_BYTES);
        byte[] bytes=new byte[(int)(end-start)];
        input.seek(start);
        int used=0;
        while(used<bytes.length){
            int n=input.read(bytes,used,bytes.length-used);
            if(n<=0)break;
            used+=n;
        }
        return new Snapshot(used==bytes.length?bytes:Arrays.copyOf(bytes,used),start>0);
    }
}
