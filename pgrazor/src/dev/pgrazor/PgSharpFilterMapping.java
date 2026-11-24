package dev.pgrazor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Maps the PGSharp data the user already imported ({@link PgSharpDataImport}) into canonical
 * presets. It is an explicit, field-by-field mapping: every unknown key stays in {@code raw_source}
 * and is reported as unsupported instead of being applied by guesswork.
 *
 * <p>Nothing here talks to PGSharp, enables a feed or turns an archived filter into a live query.
 * The result is a local preset the user can edit, save and export.
 */
final class PgSharpFilterMapping {
    private PgSharpFilterMapping() {}

    /** Fields this version understands, per filter family. */
    private static final Set<String> KNOWN_POKEMON = new LinkedHashSet<>();
    private static final Set<String> KNOWN_QUEST = new LinkedHashSet<>();
    private static final Set<String> KNOWN_RAID = new LinkedHashSet<>();

    static {
        Collections.addAll(KNOWN_POKEMON, "name", "ivMin", "ivMax", "iv", "minIv", "maxIv",
                "attackMin", "attackMax", "defenseMin", "defenseMax", "hpMin", "hpMax",
                "levelMin", "levelMax", "cpMin", "cpMax", "species", "pokemon", "pokemonIds", "mons",
                "gender", "sizes", "size", "weather", "pvpLeague", "pvpRank", "radiusKm", "lat", "lon",
                "minTtlSeconds", "enabled", "id");
        Collections.addAll(KNOWN_QUEST, "name", "reward", "rewardType", "questTitles", "enabled", "id");
        Collections.addAll(KNOWN_RAID, "name", "tiers", "bosses", "exclusiveMode", "enabled", "id");
    }

    static final class MappingReport {
        final List<PresetCodec.Preset> presets;
        final int converted, preserved, unsupported;
        final List<String> unsupportedFields;
        final String rawSource;

        MappingReport(List<PresetCodec.Preset> presets, int converted, int preserved,
                      int unsupported, List<String> unsupportedFields, String rawSource) {
            this.presets = Collections.unmodifiableList(new ArrayList<>(presets));
            this.converted = converted;
            this.preserved = preserved;
            this.unsupported = unsupported;
            this.unsupportedFields = Collections.unmodifiableList(new ArrayList<>(unsupportedFields));
            this.rawSource = rawSource == null ? "" : rawSource;
        }

        String summary() {
            return "Filtros PGSharp: " + converted + " convertidos em presets locais, "
                    + preserved + " campo(s) preservado(s) em raw_source, "
                    + unsupported + " campo(s) não suportado(s)"
                    + (unsupportedFields.isEmpty() ? "." : " (" + join(unsupportedFields) + ").")
                    + " Nenhum feed ou teleporte foi ativado.";
        }
    }

    /** Maps every archived filter of a validated PGSharp migration document. */
    static MappingReport map(JSONObject archived) {
        if (archived == null) return new MappingReport(Collections.<PresetCodec.Preset>emptyList(),
                0, 0, 0, Collections.<String>emptyList(), "");
        List<PresetCodec.Preset> presets = new ArrayList<>();
        List<String> unsupportedFields = new ArrayList<>();
        Set<String> seenFields = new LinkedHashSet<>();
        int preserved = 0;

        preserved += mapArray(archived.optJSONArray("pokemonFilters"), KNOWN_POKEMON,
                SourceEvent.Kind.POKEMON, presets, unsupportedFields, seenFields);
        preserved += mapArray(archived.optJSONArray("questFilters"), KNOWN_QUEST,
                SourceEvent.Kind.QUEST, presets, unsupportedFields, seenFields);
        preserved += mapArray(archived.optJSONArray("raidFilters"), KNOWN_RAID,
                SourceEvent.Kind.RAID, presets, unsupportedFields, seenFields);

        List<PresetCodec.Preset> unique = new ArrayList<>(new LinkedHashSet<>(presets));
        return new MappingReport(unique, unique.size(), preserved,
                unsupportedFields.size(), unsupportedFields, archived.toString());
    }

    private static int mapArray(JSONArray array, Set<String> known, SourceEvent.Kind kind,
                                List<PresetCodec.Preset> out, List<String> unsupported,
                                Set<String> seenFields) {
        if (array == null) return 0;
        int preserved = 0;
        long now = System.currentTimeMillis();
        for (int i = 0; i < array.length(); i++) {
            JSONObject filter = array.optJSONObject(i);
            if (filter == null) continue;
            String name = filter.optString("name", "").trim();
            if (name.isEmpty()) name = "Filtro " + kind.name().toLowerCase(Locale.ROOT) + " " + (i + 1);
            SourceQuery.Builder query = SourceQuery.builder().kind(kind);
            Double ivMin = firstDecimal(filter, "ivMin", "minIv", "iv");
            Double ivMax = firstDecimal(filter, "ivMax", "maxIv");
            if (ivMin != null && ivMax == null) ivMax = Double.valueOf(100.0);
            query.iv(ivMin, ivMax);
            query.attack(firstInteger(filter, "attackMin"), firstInteger(filter, "attackMax"));
            query.defense(firstInteger(filter, "defenseMin"), firstInteger(filter, "defenseMax"));
            query.stamina(firstInteger(filter, "hpMin"), firstInteger(filter, "hpMax"));
            query.level(firstInteger(filter, "levelMin"), firstInteger(filter, "levelMax"));
            query.cp(firstInteger(filter, "cpMin"), firstInteger(filter, "cpMax"));
            addSpecies(query, filter);
            String gender = SourceEvent.normalizeGender(filter.opt("gender"));
            if (gender != null) query.gender(gender);
            addSizes(query, filter);
            String league = filter.optString("pvpLeague", null);
            if (SourceQuery.LEAGUE_GREAT.equals(league) || SourceQuery.LEAGUE_ULTRA.equals(league))
                query.pvp(league, firstInteger(filter, "pvpRank"));
            Double lat = firstDecimal(filter, "lat");
            Double lon = firstDecimal(filter, "lon");
            Double radius = firstDecimal(filter, "radiusKm");
            if (SourceEvent.validCoordinates(lat, lon) && radius != null)
                query.center(lat, lon, radius);
            Long minTtl = firstInteger(filter, "minTtlSeconds") == null ? null
                    : Long.valueOf(firstInteger(filter, "minTtlSeconds").intValue());
            query.minTtlSeconds(minTtl);

            // Report and preserve everything this version does not understand.
            Iterator<String> keys = filter.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                if (!known.contains(key) && seenFields.add(kind + ":" + key)) {
                    unsupported.add(kind.name().toLowerCase(Locale.ROOT) + "." + key);
                    preserved++;
                }
            }
            SourceQuery built = query.build();
            try {
                built.validate();
                PresetCodec.Preset preset = new PresetCodec.Preset(
                        "pgsharp-" + PresetCodec.slug(name) + "-" + i, name, built,
                        PresetCodec.VERSION, now, now, false, "pgsharp-data", filter.toString());
                out.add(preset);
            } catch (JSONException invalid) {
                unsupported.add(kind.name().toLowerCase(Locale.ROOT) + ".intervalo inválido");
            }
        }
        return preserved;
    }

    private static void addSpecies(SourceQuery.Builder query, JSONObject filter) {
        JSONArray ids = filter.optJSONArray("species");
        if (ids == null) ids = filter.optJSONArray("pokemon");
        if (ids == null) ids = filter.optJSONArray("pokemonIds");
        if (ids == null) ids = filter.optJSONArray("mons");
        if (ids != null) {
            for (int i = 0; i < ids.length(); i++) {
                Integer id = SourceEvent.integer(ids.opt(i));
                if (id != null) query.pokemon(id.intValue());
            }
            return;
        }
        String raw = filter.optString("species", null);
        if (raw == null) raw = filter.optString("mons", null);
        if (raw == null || raw.trim().isEmpty()) return;
        for (String part : raw.split(",")) {
            Integer id = SourceEvent.integer(part.trim());
            if (id != null) query.pokemon(id.intValue());
        }
    }

    private static void addSizes(SourceQuery.Builder query, JSONObject filter) {
        JSONArray sizes = filter.optJSONArray("sizes");
        if (sizes != null) {
            for (int i = 0; i < sizes.length(); i++) query.size(sizes.optString(i, null));
            return;
        }
        String size = filter.optString("size", null);
        if (size != null) query.size(size);
    }

    private static Double firstDecimal(JSONObject o, String... keys) {
        for (String key : keys) {
            Double value = SourceEvent.decimal(o.opt(key));
            if (value != null) return value;
        }
        return null;
    }

    private static Integer firstInteger(JSONObject o, String... keys) {
        for (String key : keys) {
            Integer value = SourceEvent.integer(o.opt(key));
            if (value != null) return value;
        }
        return null;
    }

    private static String join(List<String> values) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.size() && i < 6; i++) {
            if (i > 0) sb.append(", ");
            sb.append(values.get(i));
        }
        if (values.size() > 6) sb.append("…");
        return sb.toString();
    }
}
