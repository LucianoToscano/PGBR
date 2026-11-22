package dev.pgrazor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Defensive normalized reader for the documented RocketMap /raw_data JSON.
 * RocketMap is a server implementation, NOT an open public Pokemon feed.
 * Based on public schema, independently implemented (no AGPL code copied).
 */
final class RocketMapFeedData {
    private static final int MAX_ROWS = 3000;
    private RocketMapFeedData() {}

    static LiveFeedData.Snapshot parse(JSONObject body, long now, boolean raidsOnly)
            throws JSONException {
        if (body == null) throw new JSONException("RocketMap retornou corpo vazio");
        List<LiveFeedData.Entry> results = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        if (!raidsOnly) {
            JSONArray pokemons = body.optJSONArray("pokemons");
            if (pokemons == null) throw new JSONException(
                    "RocketMap: não há campo pokemons (verifique o servidor e permissões)");
            for (int i=0; i<Math.min(MAX_ROWS, pokemons.length()); i++) {
                JSONObject p = pokemons.optJSONObject(i);
                if (p == null) continue;
                double lat = p.optDouble("latitude", Double.NaN);
                double lon = p.optDouble("longitude", Double.NaN);
                int dex = number(p, "pokemon_id", 1, 1025);
                long expires = unixMillis(p.opt("disappear_time"));
                if (!validCoords(lat, lon) || dex < 0 || expires <= now) continue;
                String id = key(p, "encounter_id", "rocket:spawn:", dex, lat, lon);
                if (!ids.add(id)) continue;
                int atk = number(p, "individual_attack", 0, 15);
                int def = number(p, "individual_defense", 0, 15);
                int sta = number(p, "individual_stamina", 0, 15);
                double iv = atk >= 0 && def >= 0 && sta >= 0
                        ? 100.0 * (atk + def + sta) / 45.0 : -1;
                int cp = number(p, "cp", 0, 20000);
                int gender = number(p, "gender", 0, 3);
                String name = clean(p.optString("pokemon_name", ""), 80);
                if (name.isEmpty()) name = "#" + dex;
                StringBuilder detail = new StringBuilder();
                if (cp >= 0) detail.append("PC ").append(cp);
                if (iv >= 0) append(detail, String.format(Locale.US, "%.1f%% IV", iv));
                if (atk >= 0 && def >= 0 && sta >= 0)
                    append(detail, atk + "/" + def + "/" + sta);
                LiveFeedData.Attrs attrs = new LiveFeedData.Attrs(
                        number(p, "form", 0, 99999), number(p, "costume", 0, 99999),
                        gender, -1, atk, def, sta, -1, -1, -1);
                results.add(new LiveFeedData.Entry(id, LiveFeedData.Kind.POKEMON,
                        name, detail.toString(), lat, lon, dex, cp, -1, iv,
                        expires, iv >= 0, 0, 0, false, false, attrs));
            }
        } else {
            JSONObject gyms = body.optJSONObject("gyms");
            if (gyms == null) throw new JSONException(
                    "RocketMap: não há campo gyms (verifique o servidor e permissões)");
            JSONArray gymIDs = gyms.names();
            if (gymIDs != null) for (int i=0; i<Math.min(MAX_ROWS, gymIDs.length()); i++) {
                String gymID = gymIDs.optString(i, "");
                JSONObject gym = gyms.optJSONObject(gymID);
                if (gym == null) continue;
                JSONObject raid = gym.optJSONObject("raid");
                if (raid == null) continue;
                double lat = gym.optDouble("latitude", Double.NaN);
                double lon = gym.optDouble("longitude", Double.NaN);
                long ends = unixMillis(raid.opt("end"));
                long starts = unixMillis(raid.opt("start"));
                if (!validCoords(lat, lon) || ends <= now) continue;
                String id = key(gym, "gym_id", "rocket:raid:", 0, lat, lon);
                if (!ids.add(id)) continue;
                int boss = number(raid, "pokemon_id", 1, 1025);
                int tier = number(raid, "level", 1, 20);
                int team = number(gym, "team_id", 0, 3);
                boolean hatched = starts > 0 && starts <= now;
                boolean confirmedBoss = hatched && boss > 0;
                String name = clean(gym.optString("name", ""), 80);
                if (name.isEmpty()) name = "Ginásio";
                StringBuilder detail = new StringBuilder(hatched ? "Reide" : "Ovo de Reide");
                if (tier >= 0) append(detail, "Nível " + tier);
                if (confirmedBoss) append(detail, "Chefe #" + boss);
                int cp = number(raid, "cp", 0, 200000);
                if (confirmedBoss && cp >= 0) append(detail, "PC " + cp);
                results.add(new LiveFeedData.Entry(id, LiveFeedData.Kind.RAID,
                        name, detail.toString(), lat, lon,
                        confirmedBoss ? boss : -1, confirmedBoss ? cp : -1,
                        tier, -1, ends, false, team < 0 ? 0 : team,
                        starts, confirmedBoss));
            }
        }
        return new LiveFeedData.Snapshot(results, now, 60,
                "Fonte: servidor RocketMap configurado pelo usuário; cobertura depende "
                        + "do scanner conectado. Não é o serviço PGSharp.");
    }

    /** RocketMap's CustomJSONEncoder writes UTC datetimes in Unix milliseconds. */
    static long unixMillis(Object value) {
        if (!(value instanceof Number)) return 0;
        double raw = ((Number) value).doubleValue();
        if (!Double.isFinite(raw) || Math.floor(raw) != raw) return 0;
        if (raw >= 1_000_000_000_000d && raw < 10_000_000_000_000d) return (long)raw;
        return 0;
    }

    private static String key(JSONObject o,String field,String prefix,int dex,double lat,double lon) {
        String id = clean(o.optString(field, ""), 90);
        if (id.isEmpty())
            id = dex + ":" + String.format(Locale.US, "%.6f,%.6f", lat,lon);
        return prefix + id;
    }
    private static boolean validCoords(double lat,double lon) {
        return Double.isFinite(lat) && Double.isFinite(lon)
                && lat >= -90 && lat <= 90 && lon >= -180 && lon <= 180
                && (lat != 0 || lon != 0);
    }
    private static int number(JSONObject obj, String field, int min, int max) {
        if (!obj.has(field) || obj.isNull(field)) return -1;
        Object raw = obj.opt(field);
        if (!(raw instanceof Number)) return -1;
        double number = ((Number) raw).doubleValue();
        return Double.isFinite(number) && Math.floor(number) == number
                && number >= min && number <= max ? (int)number : -1;
    }
    private static String clean(String value, int max) {
        if (value == null) return "";
        String result = value.replaceAll("[\\p{Cntrl}]+", " ").trim();
        return result.length() <= max ? result : result.substring(0,max);
    }
    private static void append(StringBuilder out,String value) {
        if (out.length() > 0) out.append(" · ");
        out.append(value);
    }
}
