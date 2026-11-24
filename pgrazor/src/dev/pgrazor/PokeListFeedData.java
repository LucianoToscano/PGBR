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

    // ---------------------------------------------------------------------------------------------
    // Canonical events (the five PokeList categories). The historical APK schema exposes
    // type=pokemon|raid|quest|rocket|rocket_leader; each is parsed into its own kind so a quest can
    // never be rendered as a spawn. Fields are read through aliases because the published contract
    // is preliminary; anything absent stays null instead of being invented.
    // ---------------------------------------------------------------------------------------------

    enum Category {
        POKEMON("pokemon"), RAID("raid"), QUEST("quest"), ROCKET("rocket"), ROCKET_LEADER("rocket_leader");
        final String wire;
        Category(String wire) { this.wire = wire; }
        static Category fromWire(String value) {
            for (Category category : values()) if (category.wire.equals(value)) return category;
            return null;
        }
    }

    /** Canonical rows plus the honest counts the radar needs (received vs usable). */
    static final class EventPage {
        final java.util.List<SourceEvent> events;
        final int received, rejected;
        final String note;
        EventPage(java.util.List<SourceEvent> events, int received, int rejected, String note) {
            this.events = java.util.Collections.unmodifiableList(new java.util.ArrayList<>(events));
            this.received = received;
            this.rejected = rejected;
            this.note = note == null ? "" : note;
        }
    }

    static EventPage parseEvents(JSONObject body, long now, Category category) throws JSONException {
        if (body == null) throw new JSONException("PokeList retornou dados vazios");
        Category resolved = category == null ? Category.POKEMON : category;
        String type = body.optString("type", resolved.wire);
        Category declared = Category.fromWire(type);
        if (declared != null && declared != resolved)
            throw new JSONException("PokeList devolveu o tipo " + type + " quando se pediu " + resolved.wire);
        JSONArray raw = body.optJSONArray("pokemons");
        if (raw == null) throw new JSONException("PokeList: campo pokemons ausente na resposta");
        java.util.List<SourceEvent> out = new java.util.ArrayList<>();
        java.util.Set<String> keys = new java.util.HashSet<>();
        int received = Math.min(raw.length(), MAX_RESULTS), rejected = 0;
        for (int i = 0; i < received; i++) {
            JSONObject row = raw.optJSONObject(i);
            if (row == null) { rejected++; continue; }
            SourceEvent event = toEvent(row, now, resolved);
            if (event == null) { rejected++; continue; }
            if (!keys.add(event.eventId)) { rejected++; continue; }
            out.add(event);
        }
        String note = "Fonte: PokeList / Moonani · categoria " + resolved.wire
                + " · cobertura: apenas o que a resposta devolveu"
                + (raw.length() > MAX_RESULTS ? " (limitado a " + MAX_RESULTS + ")" : "")
                + (rejected > 0 ? " · " + rejected + " registro(s) sem coordenada/expiração válida" : "");
        return new EventPage(out, received, rejected, note);
    }

    private static SourceEvent toEvent(JSONObject row, long now, Category category) throws JSONException {
        Double lat = SourceEvent.coordinate(row.opt("latitude"));
        Double lon = SourceEvent.coordinate(row.opt("longitude"));
        if (!SourceEvent.validCoordinates(lat, lon)) return null;
        String id = clean(row.optString("id", ""), 90);
        if (id.isEmpty()) {
            Integer dex = SourceEvent.integer(row.opt("number"));
            id = (dex == null ? "x" : dex.toString()) + ":"
                    + String.format(Locale.US, "%.6f,%.6f", lat, lon);
        }
        long expires = LocalEventImport.firstTime(row, "endTime", "end_time", "expires_at", "end");
        if (expires > 0 && expires <= now) return null;
        long observed = LocalEventImport.firstTime(row, "startTime", "start_time", "spawn_at");
        String name = clean(row.optString("name", ""), 80);
        String rocketType = clean(row.optString("rocket_type", ""), 40);

        switch (category) {
            case RAID: {
                Integer boss = SourceEvent.integer(row.opt("number"));
                if (boss == null) boss = SourceEvent.integer(row.opt("pokemon_id"));
                if (boss == null) boss = SourceEvent.integer(row.opt("boss"));
                Integer tier = SourceEvent.integer(row.opt("level"));
                if (tier == null) tier = SourceEvent.integer(row.opt("raid_level"));
                return SourceEvent.builder("pokelist", id, SourceEvent.Kind.RAID)
                        .originKind(OriginKind.LIVE_EXTERNAL)
                        .coordinates(lat, lon).name(name.isEmpty() ? "Ginásio" : name)
                        .pokemonId(boss == null || boss < 1 || boss > 1025 ? null : boss)
                        .level(tier == null || tier < 1 || tier > 20 ? null : tier)
                        .observedAt(observed > 0 ? observed : now).retrievedAt(now).expiresAt(expires)
                        .dataQuality(SourceEvent.QUALITY_PROVIDER_CLAIMED)
                        .coverage("PokeList · reides por área (contrato preliminar)")
                        .note("Reide declarada pela PokeList; nível/chefe somente quando informados.")
                        .build();
            }
            case QUEST: {
                String reward = clean(row.optString("reward", ""), 60);
                if (reward.isEmpty()) reward = clean(row.optString("quest", ""), 60);
                return SourceEvent.builder("pokelist", id, SourceEvent.Kind.QUEST)
                        .originKind(OriginKind.LIVE_EXTERNAL)
                        .coordinates(lat, lon).name(name.isEmpty() ? "Poképarada" : name)
                        .observedAt(observed > 0 ? observed : now).retrievedAt(now).expiresAt(expires)
                        .dataQuality(SourceEvent.QUALITY_PROVIDER_CLAIMED)
                        .coverage("PokeList · pesquisas por área (contrato preliminar)")
                        .note(reward.isEmpty() ? "Pesquisa sem recompensa informada."
                                : "Recompensa: " + reward)
                        .build();
            }
            case ROCKET:
            case ROCKET_LEADER: {
                return SourceEvent.builder("pokelist", id, SourceEvent.Kind.ROCKET)
                        .originKind(OriginKind.LIVE_EXTERNAL)
                        .coordinates(lat, lon).name(name.isEmpty()
                                ? (rocketType.isEmpty() ? "Invasão Rocket" : rocketType) : name)
                        .observedAt(observed > 0 ? observed : now).retrievedAt(now).expiresAt(expires)
                        .dataQuality(SourceEvent.QUALITY_PROVIDER_CLAIMED)
                        .coverage("PokeList · Rocket/líderes por área (contrato preliminar)")
                        .note(category == Category.ROCKET_LEADER
                                ? "Líder Rocket declarado pela fonte" + (rocketType.isEmpty() ? "." : ": " + rocketType)
                                : (rocketType.isEmpty() ? "Invasão Rocket declarada pela fonte."
                                        : "Invasão Rocket: " + rocketType))
                        .build();
            }
            default: {
                Integer dex = SourceEvent.integer(row.opt("number"));
                if (dex == null) dex = SourceEvent.integer(row.opt("pokemon_id"));
                if (dex == null || dex < 1 || dex > 1025) return null;
                int cp = number(row, "cp", 0, 20000);
                int level = number(row, "level", 0, 55);
                int atk = number(row, "attack", 0, 15);
                int def = number(row, "defense", 0, 15);
                int hp = number(row, "hp", 0, 15);
                Double iv = row.has("iv") && !row.isNull("iv") ? row.optDouble("iv", -1) : null;
                if (iv != null && (!Double.isFinite(iv) || iv < 0 || iv > 100)) iv = null;
                int form = number(row, "form", 0, 100000);
                // A source shiny flag is not proof that this account's encounter is shiny.
                return SourceEvent.builder("pokelist", id, SourceEvent.Kind.POKEMON)
                        .originKind(OriginKind.LIVE_EXTERNAL)
                        .coordinates(lat, lon).pokemonId(dex).name(name.isEmpty() ? "#" + dex : name)
                        .cp(cp < 0 ? null : cp).level(level < 0 ? null : level)
                        .ivPercent(iv).ivAtk(atk < 0 ? null : atk)
                        .ivDef(def < 0 ? null : def).ivSta(hp < 0 ? null : hp)
                        .formId(form < 0 ? null : form)
                        .gender(SourceEvent.normalizeGender(row.opt("gender")))
                        .size(SourceEvent.normalizeSize(row.opt("size")))
                        .observedAt(observed > 0 ? observed : now).retrievedAt(now).expiresAt(expires)
                        .dataQuality(SourceEvent.QUALITY_PROVIDER_CLAIMED)
                        .coverage("PokeList · consulta por espécie/área (contrato preliminar)")
                        .note("Spawn declarado pela PokeList; shiny da fonte não é shiny confirmado.")
                        .build();
            }
        }
    }
}
