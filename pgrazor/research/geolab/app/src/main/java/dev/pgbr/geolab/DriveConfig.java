package dev.pgbr.geolab;
import java.io.*;
import java.net.*;
import javax.net.ssl.HttpsURLConnection;
/** Explicit call with short-lived OAuth token; never stores credentials. */
public final class DriveConfig {
    private DriveConfig(){}
    public static FilterEngine.Rule load(String fileId,String accessToken) throws IOException {
        if(fileId==null||!fileId.matches("[A-Za-z0-9_-]{1,200}")||accessToken==null||!accessToken.matches("[A-Za-z0-9._~-]{1,4096}")) throw new IllegalArgumentException("Invalid credentials or ID");
        HttpsURLConnection c=(HttpsURLConnection)new URL("https://www.googleapis.com/drive/v3/files/"+fileId+"?alt=media").openConnection();
        try {c.setInstanceFollowRedirects(false);c.setConnectTimeout(5000);c.setReadTimeout(5000);c.setRequestProperty("Authorization","Bearer "+accessToken);
            if(c.getResponseCode()!=200)throw new IOException("Drive download failed: "+c.getResponseCode());
            try(InputStream in=c.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){
                byte[] buf=new byte[4096];int n;while((n=in.read(buf))!=-1){if(out.size()+n>16384)throw new IOException("Config too large");out.write(buf,0,n);}
                return JsonParser.rule(out.toByteArray());
            }
        }finally{c.disconnect();}
    }
}
