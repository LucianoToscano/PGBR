package dev.pgbr.geolab;
public final class Entity {
    private static final java.util.regex.Pattern ID=java.util.regex.Pattern.compile("[A-Za-z0-9_-]{1,64}");
    private static final java.util.regex.Pattern STATUS=java.util.regex.Pattern.compile("[A-Za-z0-9_-]{1,32}");
    public final String id, status;
    public final double lat, lon, value;
    public Entity(String id, double lat, double lon, String status, double value) {
        if (id == null || !ID.matcher(id).matches() || status == null || !STATUS.matcher(status).matches() || !Double.isFinite(lat) || !Double.isFinite(lon) || !Double.isFinite(value) || Math.abs(lat)>90 || Math.abs(lon)>180) throw new IllegalArgumentException("Entidade invalida");
        this.id=id; this.lat=lat; this.lon=lon; this.status=status; this.value=value;
    }
    public String json() { return "{\"id\":\""+id+"\",\"lat\":"+lat+",\"lon\":"+lon+",\"status\":\""+status+"\",\"value\":"+value+"}"; }
}
