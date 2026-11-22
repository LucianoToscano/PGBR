package dev.pgrazor;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.os.SystemClock;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Optional one-shot reverse geocoder for the *displayed* (spoofed) coordinates.
 * Never blocks the map/HUD thread and never changes the location used by the game.
 * Calls are throttled and only scheduled if the player selects a city clock format.
 */
final class LocalTimeCityLookup {
    private final ExecutorService executor=Executors.newSingleThreadExecutor(task -> {
        Thread thread=new Thread(task,"pgbr-clock-city");
        thread.setDaemon(true);
        return thread;
    });
    private volatile String city="";
    private volatile double cityLat=Double.NaN,cityLon=Double.NaN;
    private volatile long lastRequest;
    private volatile boolean closed, working;

    static String cityFrom(Address address) {
        if(address==null)return "";
        String result=address.getLocality();
        if(result==null||result.trim().isEmpty())result=address.getSubAdminArea();
        if(result==null||result.trim().isEmpty())result=address.getAdminArea();
        if(result==null)return "";
        String clean=result.trim().replaceAll("[\\r\\n\\t]"," ");
        return clean.length()>50?clean.substring(0,50):clean;
    }

    String resolve(Context context,double lat,double lon,Runnable updated) {
        if(closed||!Double.isFinite(lat)||!Double.isFinite(lon)
            ||Math.abs(lat)>90||Math.abs(lon)>180)return "";
        boolean same=Math.abs(lat-cityLat)<.005&&Math.abs(lon-cityLon)<.005;
        if(same)return city;
        long now=SystemClock.elapsedRealtime();
        if(working||now-lastRequest<12000)return "";
        // No network exception is propagated to the map. Android Geocoder can
        // be unavailable without Google services; the clock keeps showing time.
        lastRequest=now;
        working=true;
        city="";
        final double requestedLat=lat,requestedLon=lon;
        executor.execute(()->{
            String found="";
            try {
                if(Geocoder.isPresent()){
                    List<Address> addresses=new Geocoder(context.getApplicationContext(),
                        new Locale("pt","BR")).getFromLocation(requestedLat,requestedLon,1);
                    if(addresses!=null&&!addresses.isEmpty())found=cityFrom(addresses.get(0));
                }
            }catch(Exception ignored){ /* keep the clock usable offline */ }
            if(!closed){
                cityLat=requestedLat;
                cityLon=requestedLon;
                city=found;
            }
            working=false;
            if(!closed&&updated!=null)updated.run();
        });
        return "";
    }

    void close(){
        closed=true;
        executor.shutdownNow();
    }
}
