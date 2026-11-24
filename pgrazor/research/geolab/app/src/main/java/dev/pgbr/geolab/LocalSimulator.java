package dev.pgbr.geolab;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
public final class LocalSimulator implements AutoCloseable {
    private final ServerSocket server;
    private final Thread thread;
    private final AtomicInteger accepted=new AtomicInteger();
    public LocalSimulator() throws IOException {
        server=new ServerSocket(0,8,InetAddress.getByName("127.0.0.1"));
        thread=new Thread(this::serve,"geolab-fixture");thread.setDaemon(true);thread.start();
    }
    public int port(){return server.getLocalPort();}
    public int accepted(){return accepted.get();}
    private static String line(InputStream in) throws IOException {
        ByteArrayOutputStream out=new ByteArrayOutputStream();
        for(int i=0;i<4096;i++) {int b=in.read();if(b<0)throw new EOFException();if(b==13){if(in.read()!=10)throw new IOException("Invalid CRLF");return out.toString("US-ASCII");}if(b<32||b>126)throw new IOException("Invalid header");out.write(b);}
        throw new IOException("Line too long");
    }
    private void serve(){while(!server.isClosed()){
        try(Socket s=server.accept()){
            s.setSoTimeout(1000);InputStream in=new BufferedInputStream(s.getInputStream());
            String request=line(in);int size=-1;boolean complete=false;
            for(int i=0;i<32;i++){String h=line(in);if(h.isEmpty()){complete=true;break;}if(h.regionMatches(true,0,"Content-Length:",0,15)){if(size!=-1)throw new IOException("Duplicate length");size=Integer.parseInt(h.substring(15).trim());}if(h.regionMatches(true,0,"Transfer-Encoding:",0,18))throw new IOException("Chunking unsupported");}
            if(!complete||!request.equals("POST /lab/position HTTP/1.1")||size<1||size>4096)throw new IOException("Invalid request");
            byte[] body=new byte[size];new DataInputStream(in).readFully(body);
            String wrapped="["+new String(body,StandardCharsets.UTF_8)+"]";
            java.util.List<Entity> parsed=JsonParser.entities(wrapped.getBytes(StandardCharsets.UTF_8));
            if(parsed.size()!=1)throw new IOException("Expected one fixture");
            Entity e=parsed.get(0);
            if(!e.id.matches("fixture_[0-9]{1,5}")||Math.abs(e.lat)>0.1||Math.abs(e.lon)>0.1)throw new IOException("Not a fixture");
            accepted.incrementAndGet();s.getOutputStream().write("HTTP/1.1 204 No Content\r\nConnection: close\r\n\r\n".getBytes(StandardCharsets.US_ASCII));
        }catch(IOException|RuntimeException ex){if(server.isClosed())return;}
    }}
    public void close() throws IOException {server.close();try{thread.join(1500);}catch(InterruptedException e){Thread.currentThread().interrupt();}}
}
