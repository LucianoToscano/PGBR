package dev.pgrazor;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/** Curated locations supplied by the user, each with its time zone for the local date and time. */
public final class Hotspots {
    private Hotspots() {}
    public static final class Spot {
        /** Legacy static label kept for source and serialized-UI compatibility; local() is authoritative. */
        @Deprecated public final String utc;
        public final String country, city, name;
        public final double latitude, longitude;
        public final TimeZone zone;
        Spot(String utc,String zone,String country,String city,String name,double latitude,double longitude) {
            this.utc=utc;this.zone=timeZone(zone);this.country=country;this.city=city;this.name=name;
            this.latitude=latitude;this.longitude=longitude;
        }
        /** The spot's date, time and current UTC offset at {@code nowMs}, e.g. "Fri, Nov 20 · 9:00 PM · UTC+9". */
        public String local(long nowMs,boolean hours24) {
            SimpleDateFormat format=new SimpleDateFormat(hours24?"EEE, d MMM · HH:mm":"EEE, d MMM · h:mm a",new Locale("pt","BR"));
            format.setTimeZone(zone);
            return format.format(new Date(nowMs))+" · "+offset(zone.getOffset(nowMs));
        }
    }
    // Older Android time zone data only knows Nuuk by its former name.
    private static TimeZone timeZone(String id) {
        TimeZone zone=TimeZone.getTimeZone(id);
        if(zone.getID().equals("GMT")&&id.equals("America/Nuuk"))zone=TimeZone.getTimeZone("America/Godthab");
        return zone;
    }
    static String offset(int ms) {
        int minutes=Math.abs(ms)/60000;
        String label="UTC"+(ms<0?"-":"+")+minutes/60;
        return minutes%60==0?label:label+String.format(Locale.US,":%02d",minutes%60);
    }
    public static final List<Spot> ALL=Collections.unmodifiableList(Arrays.asList(
        new Spot("UTC -10","Pacific/Honolulu","Hawaii","Honolulu","Moana Mall",21.291100,-157.843600),
        new Spot("UTC -7","America/Los_Angeles","USA","San Francisco","Pier 39",37.808600,-122.409800),
        new Spot("UTC -6","America/Mexico_City","Mexico","Mexico City","Alameda Central",19.43585,-99.14434),
        new Spot("UTC -5","America/Chicago","USA","Chicago","Lincoln Zoo Park",41.918776,-87.635437),
        new Spot("UTC -4","America/New_York","USA","New York","Bryant Park",40.753751,-73.983537),
        new Spot("UTC -3","America/Sao_Paulo","Brasil","São Paulo","Ibirapuera Park",-23.5864600,-46.6581793),
        new Spot("UTC -2","America/Nuuk","Greenland","Nuuk","Downtown",64.175642,-51.737517),
        new Spot("UTC +0","Europe/London","UK","London","Trafalgar Square",51.5080,-0.1281),
        new Spot("UTC +1","Europe/Madrid","España","Zaragoza","Plaza de Europa",41.659268,-0.892103),
        new Spot("UTC +3","Europe/Istanbul","Türkiye","Bornova","Buyuk Park",38.462677,27.217599),
        new Spot("UTC +4","Asia/Dubai","UAE","Dubai","Jumeirah Beach",25.074792,55.130958),
        new Spot("UTC +8","Asia/Taipei","Taiwan","Taipei","Main Station",25.0480,121.5168),
        new Spot("UTC +9","Asia/Tokyo","Japan","Tokyo","Ikebukuro Station",35.729503,139.710900),
        new Spot("UTC +13","Pacific/Auckland","New Zealand","Wellington","Botanic Garden",-41.282348,174.767127)
    ));
}
