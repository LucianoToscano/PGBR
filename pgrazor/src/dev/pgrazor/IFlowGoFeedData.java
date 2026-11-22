package dev.pgrazor;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Normalizes observed iFlowGo /api/v1/nearby response fields to the existing
 * PGBR read-only live-feed model. Does not scrape metadata or create spawns.
 * Source reference: publicly observable response schema tested 2026-11-21.
 */
final class IFlowGoFeedData {
    static final int MAX_ROWS = 500;
    private IFlowGoFeedData() {}

    static LiveFeedData.Snapshot parse(JSONObject body, long now, boolean raidsOnly) throws JSONException {
        if (body == null) throw new JSONException("iFlowGo: resposta vazia");
        String group = raidsOnly ? "raids" : "spawns";
        JSONArray rows = body.optJSONArray(group);
        if (rows == null) throw new JSONException("iFlowGo: campo " + group + " ausente");
        List<LiveFeedData.Entry> entries = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        for (int i = 0; i < Math.min(rows.length(), MAX_ROWS); i++) {
            JSONObject o = rows.optJSONObject(i);
            if (o == null) continue;
            double lat = o.optDouble("lat", Double.NaN);
            double lon = o.optDouble("lon", Double.NaN);
            if (!validCoord(lat, lon)) continue;
            LiveFeedData.Entry entry = raidsOnly ? raid(o, lat, lon, now) : spawn(o, lat, lon, now);
            if (entry != null && ids.add(entry.id)) entries.add(entry);
        }
        String note = "Fonte: iFlowGo · dados consultados por área, não servidor PGSharp."
                + (rows.length() >= MAX_ROWS ? " Exibindo até " + MAX_ROWS + " registros." : "");
        return new LiveFeedData.Snapshot(entries, now, 60, note);
    }

    /** The world endpoint returns {items:[...], total:n}; item fields match nearby spawns. */
    static LiveFeedData.Snapshot parseGlobal(JSONObject body, long now) throws JSONException {
        if (body == null || body.optJSONArray("items") == null)
            throw new JSONException("iFlowGo global: campo items ausente");
        LiveFeedData.Snapshot parsed = parse(
                new JSONObject().put("spawns", body.getJSONArray("items")), now, false);
        String note = "Fonte: iFlowGo · busca global (primeira página; cobertura e limites "
                + "da fonte). Resultados filtrados no aparelho.";
        return new LiveFeedData.Snapshot(parsed.entries, now, 60, note);
    }

    private static LiveFeedData.Entry spawn(JSONObject o, double lat, double lon, long now) {
        int dex = number(o, "pokemon_id", 1, 1025);
        if (dex < 1) return null;
        long expires = isoMs(o.optString("expires_at", ""));
        if (expires > 0 && expires <= now) return null;

        int cp = number(o, "cp", 0, 20000);
        int level = number(o, "level", 1, 55);
        int atk = number(o, "attack_iv", 0, 15);
        int def = number(o, "defense_iv", 0, 15);
        int sta = number(o, "stamina_iv", 0, 15);
        int gender = number(o, "gender", 0, 3);
        double iv = o.has("percent_iv") && !o.isNull("percent_iv")
                ? o.optDouble("percent_iv", Double.NaN) : Double.NaN;
        if (!Double.isFinite(iv) || iv < 0 || iv > 100) iv = -1;

        StringBuilder detail = new StringBuilder();
        if (cp >= 0) detail.append("PC ").append(cp);
        if (level >= 0) append(detail, "Nv " + level);
        if (iv >= 0) append(detail, String.format(Locale.US, "%.1f%% IV", iv));
        if (atk >= 0 && def >= 0 && sta >= 0)
            append(detail, atk + "/" + def + "/" + sta);
        if (expires == 0) append(detail, "Expiração não informada");
        LiveFeedData.Attrs attrs = new LiveFeedData.Attrs(
                -1, -1, gender, -1, atk, def, sta, -1, -1, -1);
        String id = key(o, "spawn", dex, lat, lon);
        return new LiveFeedData.Entry(id, LiveFeedData.Kind.POKEMON,
                "#" + dex, detail.toString(), lat, lon, dex,
                cp, level, iv, expires, iv >= 0, 0, 0, false, false, attrs);
    }

    private static LiveFeedData.Entry raid(JSONObject o, double lat, double lon, long now) {
        long ends = isoMs(o.optString("raid_end_at", ""));
        // A raid without confirmed future end-time cannot be offered as live.
        if (ends <= now) return null;
        long hatch = isoMs(o.optString("raid_battle_at", ""));
        int boss = number(o, "raid_pokemon_id", 1, 1025);
        int tier = number(o, "raid_level", 1, 20);
        boolean egg = hatch > now;
        // Without a confirmed hatch time, a boss identifier alone is not
        // evidence that the raid has already hatched.
        boolean bossLive = hatch > 0 && !egg && boss > 0;
        String name = clean(o.optString("gym_name", ""), 90);
        if (name.isEmpty()) name = "Ginásio";
        StringBuilder detail = new StringBuilder(egg ? "Ovo de Reide" : "Reide");
        if (tier > 0) append(detail, "Nível " + tier);
        if (bossLive) append(detail, "Chefe #" + boss);
        if (o.optBoolean("ex_raid_eligible", false)) append(detail, "Ginásio EX elegível");
        return new LiveFeedData.Entry(key(o, "raid", boss, lat, lon),
                LiveFeedData.Kind.RAID, name, detail.toString(), lat, lon,
                bossLive ? boss : -1, -1, tier, -1,
                ends, false, 0, hatch, bossLive);
    }

    private static String key(JSONObject o, String type, int dex, double lat, double lon) {
        String remote = clean(o.optString("id", ""), 90);
        if (remote.isEmpty())
            remote = dex + ":" + String.format(Locale.US, "%.6f,%.6f", lat, lon);
        return "iflowgo:" + type + ":" + remote;
    }

    static long isoMs(String value) {
        if (value == null || value.trim().isEmpty()) return 0;
        // Normalize microsecond ISO fraction to milliseconds; don't guess time zones.
        String input = value.trim().replaceFirst("(\\.\\d{3})\\d+(?=Z$|[+-]\\d{2}:?\\d{2}$)", "$1");
        String[] patterns = {"yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
                "yyyy-MM-dd'T'HH:mm:ssXXX",
                "yyyy-MM-dd'T'HH:mm:ss.SSSX",
                "yyyy-MM-dd'T'HH:mm:ssX"};
        for (String pattern : patterns) {
            SimpleDateFormat format = new SimpleDateFormat(pattern, Locale.US);
            format.setLenient(false);
            ParsePosition position = new ParsePosition(0);
            Date date = format.parse(input, position);
            if (date != null && position.getIndex() == input.length()) return date.getTime();
        }
        return 0;
    }

    private static boolean validCoord(double lat, double lon) {
        return Double.isFinite(lat) && Double.isFinite(lon)
                && lat >= -90 && lat <= 90 && lon >= -180 && lon <= 180
                && (lat != 0 || lon != 0);
    }

    private static int number(JSONObject o, String key, int min, int max) {
        if (!o.has(key) || o.isNull(key)) return -1;
        Object value = o.opt(key);
        if (!(value instanceof Number)) return -1;
        double n = ((Number) value).doubleValue();
        if (!Double.isFinite(n) || n < min || n > max || Math.floor(n) != n) return -1;
        return (int) n;
    }

    private static String clean(String s, int max) {
        if (s == null) return "";
        String value = s.replaceAll("[\\p{Cntrl}]+", " ").trim();
        return value.length() <= max ? value : value.substring(0, max);
    }

    private static void append(StringBuilder b, String text) {
        if (b.length() > 0) b.append(" · ");
        b.append(text);
    }
}
