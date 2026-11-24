package dev.pgbr.geolab;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
/** Synthetic fixtures only, with no caller-supplied host, route, or credentials. */
public final class ActionDispatcher {
    private final int port;
    private final Set<String> seen=new HashSet<>();
    private final int limit;
    public ActionDispatcher(int port,int limit){if(port<1024||port>65535||limit<1||limit>100)throw new IllegalArgumentException();this.port=port;this.limit=limit;}
    public synchronized boolean dispatch(Entity e) throws IOException {
        if(!e.id.matches("fixture_[0-9]{1,5}") || Math.abs(e.lat)>0.1 || Math.abs(e.lon)>0.1) throw new IllegalArgumentException("Synthetic fixture only");
        if(seen.size()>=limit||seen.contains(e.id)) return false;
        seen.add(e.id);
        HttpURLConnection c=(HttpURLConnection)new URL("http://127.0.0.1:"+port+"/lab/position").openConnection(Proxy.NO_PROXY);
        try {c.setInstanceFollowRedirects(false);c.setConnectTimeout(1500);c.setReadTimeout(1500);c.setRequestMethod("POST");c.setDoOutput(true);c.setRequestProperty("Content-Type","application/json");
            byte[] payload=e.json().getBytes(StandardCharsets.UTF_8);c.setFixedLengthStreamingMode(payload.length);
            try(OutputStream o=c.getOutputStream()){o.write(payload);}
            if(c.getResponseCode()!=204) throw new IOException("Simulator rejected request"); return true;
        }finally{c.disconnect();}
    }
}
