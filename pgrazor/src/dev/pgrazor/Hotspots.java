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
    // Static suggestions, not live observations or verified counts of game resources.
    public enum Category {
        TOP("Destaque",""),
        HOTSPOT("Hotspot","ic_map.png"),
        FARM("Farm","stop.png"),
        REIDES("Reides","raid-active.png"),
        POKEPARADAS("Poképaradas","stop.png"),
        GINASIOS("Ginásios","gym.png"),
        ROTAS("Rotas","routes.png"),
        EVENTOS("Eventos",""),
        CEDO("Eventos cedo",""),
        TARDIOS("Eventos tardios",""),
        PRIMEIRO_FUSO("Primeiro fuso",""),
        ULTIMO_FUSO("Último fuso",""),
        ALTERNATIVO("Alternativo",""),
        INCENSO("Incenso","daily-adventure-incense.png"),
        CAMINHADA("Caminhada","routes.png"),
        GALAR("Pássaros de Galar","daily-adventure-incense.png"),
        PESQUISAS("Pesquisas",""),
        REGIONAIS("Regionais",""),
        ESPECIAL("Especial",""),
        HISTORICO("Histórico",""),
        PARQUE("Parque","ic_map.png"),
        POPULAR("Lugar popular","ic_map.png");
        public final String label,icon;
        Category(String label,String icon){this.label=label;this.icon=icon;}
    }
    public static final class Spot {
        /** Legacy static label kept for source and serialized-UI compatibility; local() is authoritative. */
        @Deprecated public final String utc;
        public final String country, city, name,countryCode;
        public final java.util.Set<Category> categories;
        public final double latitude, longitude;
        public final TimeZone zone;
        Spot(String utc,String zone,String country,String city,String name,double latitude,double longitude) {
            this(utc,zone,country,city,name,latitude,longitude,"",Category.POPULAR);
        }
        Spot(String utc,String zone,String country,String city,String name,double latitude,double longitude,String countryCode,Category... categories){
            this.utc=utc;this.zone=timeZone(zone);this.country=country;this.city=city;this.name=name;this.countryCode=countryCode;
            this.latitude=latitude;this.longitude=longitude;
            java.util.EnumSet<Category> set=java.util.EnumSet.noneOf(Category.class);Collections.addAll(set,categories);this.categories=Collections.unmodifiableSet(set);
        }
        public boolean matches(Category category){return category==null||categories.contains(category);}
        public String categoryText(){StringBuilder s=new StringBuilder();for(Category c:categories){if(s.length()>0)s.append(" · ");s.append(c.label);}return s.toString();}
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
    private static final List<Spot> LEGACY=Arrays.asList(
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
    );
    private static Spot point(String countryCode,String country,String city,String name,double latitude,double longitude,String zone,Category... cats){
        return new Spot(offset(timeZone(zone).getRawOffset()),zone,country,city,name,latitude,longitude,countryCode,cats);
    }
    private static final List<Spot> CATALOG=Arrays.asList(
        point("KI","Kiribati","Kiritimati","Kiritimati",1.982358,-157.473189,"Pacific/Kiritimati",Category.EVENTOS,Category.PRIMEIRO_FUSO),
        point("NZ","Nova Zelândia","Auckland","Auckland",-36.852095,174.76318,"Pacific/Auckland",Category.HOTSPOT,Category.EVENTOS,Category.CEDO),
        point("NZ","Nova Zelândia","Wellington","Wellington",-41.286,174.7769,"Pacific/Auckland",Category.REIDES,Category.EVENTOS,Category.CEDO),
        point("AU","Austrália","Sydney","Sydney",-33.8624,151.2107,"Australia/Sydney",Category.TOP,Category.REIDES,Category.HOTSPOT),
        point("JP","Japão","Tóquio","PokéPark KANTO",35.62621,139.52167,"Asia/Tokyo",Category.ESPECIAL,Category.PESQUISAS),
        point("JP","Japão","Tóquio","Shibuya",35.6595,139.7005,"Asia/Tokyo",Category.TOP,Category.FARM),
        point("JP","Japão","Tóquio","Shinjuku",35.6912,139.6978,"Asia/Tokyo",Category.TOP,Category.REIDES),
        point("JP","Japão","Tóquio","Ikebukuro / Sunshine City",35.7292,139.7192,"Asia/Tokyo",Category.TOP,Category.REIDES),
        point("JP","Japão","Nagoya","Nagoya",35.1553,136.91978,"Asia/Tokyo",Category.ALTERNATIVO),
        point("JP","Japão","Osaka","Osaka",34.65011,135.5105,"Asia/Tokyo",Category.HOTSPOT,Category.FARM),
        point("KR","Coreia do Sul","Seul","Seul — hotspot",37.513613,127.103707,"Asia/Seoul",Category.HOTSPOT),
        point("KR","Coreia do Sul","Seul","Seul — popular",37.567308,126.977133,"Asia/Seoul",Category.POPULAR),
        point("JP","Japão","Tóquio","Tóquio — popular",35.66959,139.69969,"Asia/Tokyo",Category.POPULAR),
        point("JP","Japão","Sendai","Tsutsujigaoka Park",38.2597,140.897045,"Asia/Tokyo",Category.EVENTOS,Category.PARQUE),
        point("JP","Japão","Osaka","Namba",34.6664,135.5006,"Asia/Tokyo",Category.FARM,Category.GINASIOS),
        point("JP","Japão","Tóquio","Ueno Park",35.714982,139.773581,"Asia/Tokyo",Category.FARM,Category.ROTAS),
        point("TW","Taiwan","Taipei","Taipei — destaque",25.030686,121.535933,"Asia/Taipei",Category.TOP,Category.REIDES,Category.HOTSPOT),
        point("TW","Taiwan","Taiwan","Reta",23.018484,120.143802,"Asia/Taipei",Category.INCENSO,Category.GALAR),
        point("HK","Hong Kong","Hong Kong","Hong Kong",22.319303,114.169361,"Asia/Hong_Kong",Category.HOTSPOT),
        point("SG","Singapura","Singapura","Marina Bay Sands",1.2838,103.8585,"Asia/Singapore",Category.FARM,Category.PESQUISAS),
        point("SG","Singapura","Singapura","VivoCity",1.2642,103.8224,"Asia/Singapore",Category.REIDES),
        point("MY","Malásia","Kuala Lumpur","Kuala Lumpur",3.155323,101.71263,"Asia/Kuala_Lumpur",Category.FARM),
        point("TW","Taiwan","Taipei","Taipei — popular",25.048323,121.517228,"Asia/Taipei",Category.POPULAR),
        point("AE","Emirados Árabes Unidos","Dubai","Dubai",25.075201,55.131068,"Asia/Dubai",Category.ALTERNATIVO),
        point("SA","Arábia Saudita","Arábia Saudita","Reta longa",24.145556,49.065278,"Asia/Riyadh",Category.INCENSO,Category.CAMINHADA),
        point("TR","Turquia","Izmir","Bornova",38.4634,27.2163,"Europe/Istanbul",Category.ALTERNATIVO),
        point("GR","Grécia","Larissa","Larissa",39.638309,22.416071,"Europe/Athens",Category.ALTERNATIVO),
        point("GR","Grécia","Tessalônica","Egnatia",40.628,22.955,"Europe/Athens",Category.FARM),
        point("HU","Hungria","Budapeste","Budapeste",47.5313,19.0528,"Europe/Budapest",Category.ALTERNATIVO),
        point("IT","Itália","Roma","Roma",41.8902,12.4922,"Europe/Rome",Category.ALTERNATIVO),
        point("FR","França","Paris","Paris",48.8566,2.3522,"Europe/Paris",Category.ALTERNATIVO),
        point("FR","França","Dunkerque","Dunkerque",51.047048,2.280146,"Europe/Paris",Category.INCENSO,Category.CAMINHADA),
        point("ES","Espanha","Zaragoza","Zaragoza — destaque",41.6616,-0.8946,"Europe/Madrid",Category.TOP,Category.FARM,Category.REIDES),
        point("ES","Espanha","Sevilha","Sevilha",37.38595,-5.998314,"Europe/Madrid",Category.ALTERNATIVO),
        point("ES","Espanha","Zaragoza","Zaragoza — popular",41.661012,-0.893407,"Europe/Madrid",Category.POPULAR),
        point("GB","Reino Unido","Londres","Londres",51.5074,-0.1278,"Europe/London",Category.HOTSPOT),
        point("BR","Brasil","São Paulo","Consolação",-23.5512,-46.6584,"America/Sao_Paulo",Category.HOTSPOT,Category.POKEPARADAS),
        point("BR","Brasil","São Paulo","Ibirapuera",-23.587599,-46.655574,"America/Sao_Paulo",Category.TOP,Category.FARM,Category.REIDES),
        point("BR","Brasil","Porto Alegre","Centro",-30.034647,-51.217658,"America/Sao_Paulo",Category.FARM,Category.POKEPARADAS),
        point("US","Estados Unidos","Nova York","Bryant Park",40.7536,-73.9832,"America/New_York",Category.TOP,Category.REIDES,Category.POKEPARADAS),
        point("US","Estados Unidos","Nova York","Central Park",40.782323,-73.965416,"America/New_York",Category.FARM,Category.ROTAS),
        point("US","Estados Unidos","Orlando","Flórida",28.418727,-81.581194,"America/New_York",Category.REGIONAIS),
        point("PE","Peru","Chancay","Chancay",-11.5628,-77.27,"America/Lima",Category.ALTERNATIVO),
        point("US","Estados Unidos","Nova York","Nova York — popular",40.7552,-73.983,"America/New_York",Category.POPULAR),
        point("MX","México","Cidade do México","Centro",19.435272,-99.145163,"America/Mexico_City",Category.HOTSPOT),
        point("US","Estados Unidos","Chicago","Loop",41.882728,-87.622562,"America/Chicago",Category.FARM,Category.REIDES),
        point("US","Estados Unidos","Los Angeles","Santa Monica Pier",34.0094,-118.4973,"America/Los_Angeles",Category.FARM,Category.REIDES),
        point("US","Estados Unidos","Pasadena","Rose Bowl",34.1613,-118.1676,"America/Los_Angeles",Category.ALTERNATIVO),
        point("US","Estados Unidos","San Francisco","Pier 39",37.8086,-122.4098,"America/Los_Angeles",Category.REIDES,Category.ROTAS),
        point("US","Estados Unidos","San Francisco","Mundial 2026",37.78449,-122.400912,"America/Los_Angeles",Category.HISTORICO),
        point("US","Estados Unidos","Honolulu","Havaí",21.2911,-157.8436,"Pacific/Honolulu",Category.EVENTOS,Category.TARDIOS,Category.REIDES),
        point("AS","Samoa Americana","Pago Pago","Pago Pago",-14.27806,-170.7025,"Pacific/Pago_Pago",Category.ULTIMO_FUSO,Category.EVENTOS)
    );
    private static List<Spot> combine(){
        java.util.ArrayList<Spot> out=new java.util.ArrayList<>();
        for(Spot old:LEGACY){
            Spot same=null;for(Spot p:CATALOG)if(old.latitude==p.latitude&&old.longitude==p.longitude){same=p;break;}
            out.add(same==null?old:new Spot(old.utc,same.zone.getID(),same.country,same.city,old.name,old.latitude,old.longitude,same.countryCode,same.categories.toArray(new Category[0])));
        }
        for(Spot p:CATALOG){boolean duplicate=false;for(Spot old:out)if(old.latitude==p.latitude&&old.longitude==p.longitude){duplicate=true;break;}if(!duplicate)out.add(p);}
        return Collections.unmodifiableList(out);
    }
    public static final List<Spot> ALL=combine();
}
