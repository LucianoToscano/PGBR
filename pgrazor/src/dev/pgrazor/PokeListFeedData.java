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
 * Defensive, data-only adaptation of the PokeList/Moonani schema documented in
 * the supplied APK analysis. No connection or credentials are used here.
 * The endpoint and its contract are preliminary until validated live.
 */
final class PokeListFeedData {
    private PokeListFeedData() {}
    static final int MAX_RESULTS = 3000;

    static LiveFeedData.Snapshot parse(JSONObject body, long now) throws JSONException {
        if (body == null) throw new JSONException("PokeList returned no data");
        String kind = body.optString("type", "pokemon");
        if (!"pokemon".equalsIgnoreCase(kind) && !kind.isEmpty())
            throw new JSONException("Unexpected PokeList feed type");
        JSONArray raw = body.optJSONArray("pokemons");
        if (raw == null) throw new JSONException("PokeList JSON did not include pokemons");
        List<LiveFeedData.Entry> list = new ArrayList<>();
        Set<String> keys = new HashSet<>();
        int length = Math.min(raw.length(), MAX_RESULTS);
        for (int i=0; i<length; i++) {
            JSONObject p = raw.optJSONObject(i);
            if (p == null) continue;
            double lat = p.optDouble("latitude", Double.NaN);
            double lon = p.optDouble("longitude", Double.NaN);
            if (!Double.isFinite(lat) || !Double.isFinite(lon)
                    || lat < -90 || lat > 90 || lon < -180 || lon > 180
                    || (lat == 0.0 && lon == 0.0)) continue;
            int dex = p.optInt("number", -1);
            if (dex < 1 || dex > 1025) continue;
            long expires = epochMs(p.optLong("endTime", 0));
            if (expires > 0 && expires <= now) continue;
            String key = clean(p.optString("id", ""), 90);
            if (key.isEmpty()) key = dex + ":" + String.format(Locale.US, "%.6f,%.6f", lat, lon);
            if (!keys.add(key)) continue;
            String name = clean(p.optString("name", ""), 80);
            if (name.isEmpty()) name = "#" + dex;
            int cp = number(p, "cp", 0, 20000);
            int level = number(p, "level", 0, 55);
            int atk = number(p, "attack", 0, 15);
            int def = number(p, "defense", 0, 15);
            int hp = number(p, "hp", 0, 15);
            double iv = p.has("iv") && !p.isNull("iv") ? p.optDouble("iv", -1) : -1;
            if (!Double.isFinite(iv) || iv < 0 || iv > 100) iv = -1;
            // A source-specific shiny flag cannot guarantee the trainer's encounter is shiny.
            StringBuilder detail = new StringBuilder();
            if (cp >= 0) detail.append("PC ").append(cp);
            if (iv >= 0) append(detail, String.format(Locale.US, "%.1f%% IV", iv));
            if (level >= 0) append(detail, "Nv " + level);
            if (atk >= 0 && def >= 0 && hp >= 0)
                append(detail, atk + "/" + def + "/" + hp);
            if (p.optInt("shiny", 0) == 1) append(detail, "Shiny indicado pela fonte (não garantido)");
            LiveFeedData.Attrs attrs = new LiveFeedData.Attrs(
                    number(p, "form", 0, 100000), -1,
                    number(p, "gender", 0, 3), -1,
                    atk, def, hp, -1, -1, -1);
            list.add(new LiveFeedData.Entry(
                    "pokelist:" + key, LiveFeedData.Kind.POKEMON,
                    name, detail.toString(), lat, lon, dex, cp, level, iv,
                    expires, iv >= 0, 0, 0, false, false, attrs));
        }
        String note = "Fonte: PokeList / Moonani (dados próprios, não PGSharp/iPogo/Pokémod)."
                + (raw.length() > MAX_RESULTS ? " Resultados limitados a " + MAX_RESULTS + "." : "");
        return new LiveFeedData.Snapshot(list, now, 60, note);
    }

    private static void append(StringBuilder b,String text) {
        if (b.length()>0) b.append(" · ");
        b.append(text);
    }
    private static int number(JSONObject o, String key, int lo, int hi) {
        if (!o.has(key) || o.isNull(key)) return -1;
        int v = o.optInt(key, -1);
        return v >= lo && v <= hi ? v : -1;
    }
    /** Treat known contemporary 10-digit timestamps as seconds and 13-digit as ms. */
    private static long epochMs(long raw) {
        if (raw >= 1_000_000_000L && raw < 10_000_000_000L) return raw * 1000L;
        if (raw >= 1_000_000_000_000L && raw < 10_000_000_000_000L) return raw;
        return 0L; // unknown rather than guessing
    }
    private static String clean(String value,int length) {
        if (value == null) return "";
        String cleaned = value.replaceAll("[\\p{Cntrl}]+", " ").trim();
        return cleaned.length() <= length ? cleaned : cleaned.substring(0, length);
    }
}
