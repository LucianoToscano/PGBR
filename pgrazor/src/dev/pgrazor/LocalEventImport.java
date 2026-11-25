package dev.pgrazor;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Local entry/import that actually works: clipboard coordinates, typed {@code lat, lon}, JSON,
 * CSV and GPX become canonical records. The original text is kept in the report, every rejected row
 * carries a reason, duplicates are refused, and nothing is invented: a row without coordinates
 * stays without coordinates, and a row without an expiry keeps {@code expiresAt = 0}.
 *
 * <p>Imported rows are always {@link OriginKind#PERSONAL_IMPORT} with
 * {@code data_quality = imported}. They are never promoted to live data.
 */
public final class LocalEventImport {
    private LocalEventImport() {}

    public static final int MAX_BYTES = 2 * 1024 * 1024;
    public static final int MAX_ROWS = 5000;
    public static final String IMPORT_SOURCE = "import";
    public static final String EVENT_FORMAT = "PGBR events";
    public static final int EVENT_VERSION = 1;

    public enum Format { JSON, CSV, GPX, COORDINATES, UNKNOWN }

    public static final class Rejection {
        public final int row;
        public final String reason;
        public final String raw;
        Rejection(int row, String reason, String raw) {
            this.row = row;
            this.reason = reason;
            this.raw = raw == null ? "" : raw;
        }
    }

    public static final class Report {
        public final Format format;
        public final int imported, duplicates, rejected;
        public final List<SourceEvent> events;
        public final List<Rejection> rejections;
        public final String note;

        Report(Format format, List<SourceEvent> events, int duplicates,
               List<Rejection> rejections, String note) {
            this.format = format;
            this.events = Collections.unmodifiableList(new ArrayList<>(events));
            this.duplicates = duplicates;
            this.rejections = Collections.unmodifiableList(new ArrayList<>(rejections));
            this.rejected = rejections.size();
            this.imported = events.size();
            this.note = note == null ? "" : note;
        }

        public boolean ok() { return imported > 0; }

        /** PT-BR summary with the reason for every rejection; never hides a rejected row. */
        public String summary() {
            StringBuilder sb = new StringBuilder();
            sb.append("Importados: ").append(imported)
              .append(" · duplicados: ").append(duplicates)
              .append(" · rejeitados: ").append(rejected).append('.');
            if (!note.isEmpty()) sb.append(' ').append(note);
            for (int i = 0; i < rejections.size() && i < 8; i++) {
                Rejection rejection = rejections.get(i);
                sb.append("\nLinha ").append(rejection.row).append(": ").append(rejection.reason);
            }
            if (rejections.size() > 8) sb.append("\n+ ").append(rejections.size() - 8).append(" outros erros.");
            return sb.toString();
        }
    }

    /** Detects the format and imports; never throws for user content, it reports instead. */
    public static Report parse(String text, long now) {
        return parse(text, now, IMPORT_SOURCE);
    }

    public static Report parse(String text, long now, String sourceId) {
        String raw = text == null ? "" : text;
        if (raw.length() > MAX_BYTES || raw.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES)
            return new Report(Format.UNKNOWN, Collections.<SourceEvent>emptyList(),
                    0, Collections.singletonList(new Rejection(1, "Arquivo maior que 2 MB.", "")), "");
        if (raw.startsWith("\uFEFF")) raw = raw.substring(1);
        if (raw.trim().isEmpty()) return new Report(Format.UNKNOWN, Collections.<SourceEvent>emptyList(),
                0, Collections.singletonList(new Rejection(1, "Nenhum dado para importar.", "")), "");
        String trimmed = raw.trim();
        try {
            if (trimmed.startsWith("{") || trimmed.startsWith("[")) return parseJson(trimmed, now, sourceId);
            if (trimmed.startsWith("<")) return parseGpx(trimmed, now, sourceId);
            if (looksLikeCsv(trimmed)) return parseCsv(trimmed, now, sourceId);
            if (looksLikeCoordinates(trimmed)) return parseCoordinates(trimmed, now, sourceId);
        } catch (JSONException malformed) {
            return new Report(Format.UNKNOWN, Collections.<SourceEvent>emptyList(), 0,
                    Collections.singletonList(new Rejection(1, "Formato não reconhecido: "
                            + message(malformed), trimmed)), "");
        }
        return new Report(Format.UNKNOWN, Collections.<SourceEvent>emptyList(), 0,
                Collections.singletonList(new Rejection(1,
                        "Formato não reconhecido. Use JSON, CSV, GPX ou coordenadas lat, lon.", trimmed)), "");
    }

    private static String message(Exception error) {
        String text = error.getMessage();
        return text == null || text.isEmpty() ? error.getClass().getSimpleName() : text;
    }

    // --- JSON ------------------------------------------------------------------------------------

    private static Report parseJson(String text, long now, String sourceId) throws JSONException {
        List<Rejection> rejections = new ArrayList<>();
        List<SourceEvent> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        int duplicates = 0;
        String note = "";

        JSONObject root = null;
        JSONArray rows = null;
        String trimmed = text.trim();
        if (trimmed.startsWith("[")) {
            rows = new JSONArray(trimmed);
        } else {
            root = new JSONObject(trimmed);
            if (EVENT_FORMAT.equals(root.optString("format")) && root.optInt("version", EVENT_VERSION) != EVENT_VERSION) {
                rejections.add(new Rejection(1, "Versão do arquivo de eventos não suportada.", ""));
                return new Report(Format.JSON, out, duplicates, rejections, note);
            }
            String[] containers = {"events", "items", "spawns", "pokemons", "results", "data"};
            for (String container : containers) {
                Object value = root.opt(container);
                if (value instanceof JSONArray) { rows = (JSONArray) value; break; }
                if (value instanceof JSONObject && "data".equals(container)) {
                    JSONObject data = (JSONObject) value;
                    for (String inner : new String[]{"events", "items", "spawns", "pokemons", "results"}) {
                        if (data.optJSONArray(inner) != null) { rows = data.optJSONArray(inner); break; }
                    }
                    if (rows != null) break;
                }
            }
            if (rows == null && root.optJSONArray("hundos") != null) {
                // The PGBR 100% IV feed: [[encounterId, dex, form, lat, lon, despawnMs, cp, level], ...]
                Report hundo = parseHundo(root, now, sourceId);
                return hundo;
            }
            if (rows == null && root.has("format") && EVENT_FORMAT.equals(root.optString("format"))) {
                rejections.add(new Rejection(1, "Arquivo de eventos sem lista.", text));
                return new Report(Format.JSON, out, duplicates, rejections, note);
            }
            if (rows == null) {
                rejections.add(new Rejection(1, "JSON sem lista de eventos reconhecível.", text));
                return new Report(Format.JSON, out, duplicates, rejections, note);
            }
        }

        int length = Math.min(rows.length(), MAX_ROWS);
        for (int i = 0; i < length; i++) {
            JSONObject row = rows.optJSONObject(i);
            if (row == null) {
                rejections.add(new Rejection(i + 1, "Registro não é um objeto JSON.", String.valueOf(rows.opt(i))));
                continue;
            }
            if (row.optBoolean("is_demo", false) || OriginKind.fromWire(row.optString("origin_kind", "")) == OriginKind.SIMULATED_FIXTURE) {
                rejections.add(new Rejection(i + 1, "Registro demonstrativo não pode ser importado como real.", row.toString()));
                continue;
            }
            SourceEvent event = rowToEvent(row, now, sourceId);
            if (event == null) {
                rejections.add(new Rejection(i + 1, "Registro sem coordenadas válidas.", row.toString()));
                continue;
            }
            String key = event.sourceId + "|" + event.eventId;
            if (!seen.add(key)) { duplicates++; continue; }
            out.add(event);
        }
        if (rows.length() > MAX_ROWS) note = "Apenas as primeiras " + MAX_ROWS + " linhas foram lidas.";
        if (out.isEmpty() && rejections.isEmpty())
            note = "Nenhum registro válido. O arquivo não virou fonte de dados ao vivo.";
        return new Report(Format.JSON, out, duplicates, rejections, note);
    }

    private static Report parseHundo(JSONObject root, long now, String sourceId) throws JSONException {
        List<Rejection> rejections = new ArrayList<>();
        List<SourceEvent> out = new ArrayList<>();
        JSONArray rows = root.optJSONArray("hundos");
        int length = Math.min(rows.length(), MAX_ROWS);
        for (int i = 0; i < length; i++) {
            JSONArray row = rows.optJSONArray(i);
            if (row == null || row.length() < 6) {
                rejections.add(new Rejection(i + 1, "Linha do feed 100% IV incompleta.", String.valueOf(row)));
                continue;
            }
            String id = row.optString(0, "");
            Integer dex = SourceEvent.integer(row.opt(1));
            Double lat = SourceEvent.coordinate(row.opt(3));
            Double lon = SourceEvent.coordinate(row.opt(4));
            long despawn = firstTime(new JSONObject().put("expiry", row.opt(5)), "expiry");
            if (id.isEmpty() || dex == null || !SourceEvent.validCoordinates(lat, lon)) {
                rejections.add(new Rejection(i + 1, "Linha do feed 100% IV inválida.", row.toString()));
                continue;
            }
            Integer form = SourceEvent.integer(row.opt(2));
            Integer cp = row.length() > 6 ? SourceEvent.integer(row.opt(6)) : null;
            Double level = row.length() > 7 ? SourceEvent.decimal(row.opt(7)) : null;
            out.add(SourceEvent.builder(sourceId, "hundo:" + id, SourceEvent.Kind.POKEMON)
                    .originKind(OriginKind.PERSONAL_IMPORT)
                    .pokemonId(dex).formId(form == null || form < 0 ? null : form)
                    .coordinates(lat, lon)
                    .ivPercent(Double.valueOf(100.0)) // the feed asserts 100% IV; the app does not recompute it
                    .cp(cp == null || cp <= 0 ? null : cp)
                    .level(level == null || level < 1 || level > 50 ? null : level.intValue())
                    .observedAt(now).retrievedAt(now)
                    .expiresAt(despawn)
                    .dataQuality(SourceEvent.QUALITY_IMPORTED)
                    .note("Importação do feed 100% IV; IV declarado pela fonte.")
                    .build());
        }
        return new Report(Format.JSON, out, 0, rejections,
                out.isEmpty() ? "Nenhuma linha válida no feed 100% IV." : "");
    }

    /** Maps one JSON row to a canonical event using the field aliases of the supported sources. */
    private static SourceEvent rowToEvent(JSONObject row, long now, String sourceId) throws JSONException {
        Double lat = firstCoordinate(row, "latitude", "lat");
        Double lon = firstCoordinate(row, "longitude", "lon", "lng", "long");
        Integer dex = firstInteger(row, "pokemon_id", "pokemonId", "pokemon", "number", "dex", "species_id");
        if (!SourceEvent.validCoordinates(lat, lon)) return null;
        String id = firstString(row, "event_id", "id", "spawn_id", "encounter_id", "encounterId");
        String name = firstString(row, "name", "pokemon_name");
        Integer form = firstInteger(row, "form_id", "form", "formId");
        Integer cp = firstInteger(row, "cp");
        Integer level = firstInteger(row, "level");
        Double iv = firstDecimal(row, "iv_percent", "iv", "percent_iv", "ivPercent");
        Integer atk = firstInteger(row, "iv_atk", "attack", "attack_iv", "atk");
        Integer def = firstInteger(row, "iv_def", "defense", "defense_iv", "def");
        Integer sta = firstInteger(row, "iv_sta", "hp", "stamina_iv", "sta");
        String gender = SourceEvent.normalizeGender(row.has("gender") ? row.opt("gender") : null);
        String size = SourceEvent.normalizeSize(row.has("size") ? row.opt("size") : null);
        String kindRaw = firstString(row, "kind", "type");
        long expires = firstTime(row, "expires_at", "endTime", "end_time", "despawn_ms", "despawn", "expire_at");
        long observed = firstTime(row, "observed_at", "startTime", "start_time", "spawn_at");
        if (iv != null && (iv < 0 || iv > 100)) iv = null;
        if (atk != null && (atk < 0 || atk > 15)) atk = null;
        if (def != null && (def < 0 || def > 15)) def = null;
        if (sta != null && (sta < 0 || sta > 15)) sta = null;
        if (dex != null && (dex < 1 || dex > 1025)) dex = null;

        String resolvedSource = sourceId == null || sourceId.isEmpty() ? IMPORT_SOURCE : sourceId;
        String resolvedId = id == null || id.trim().isEmpty()
                ? (dex == null ? "x" : dex.toString()) + ":"
                  + String.format(Locale.US, "%.6f,%.6f", lat, lon)
                : id.trim();
        SourceEvent.Builder builder = SourceEvent.builder(resolvedSource, resolvedId, kind(kindRaw))
                .originKind(OriginKind.PERSONAL_IMPORT)
                .coordinates(lat, lon)
                .pokemonId(dex).formId(form).name(name == null ? "" : name)
                .ivPercent(iv).ivAtk(atk).ivDef(def).ivSta(sta)
                .cp(cp).level(level).gender(gender).size(size)
                .observedAt(observed > 0 ? observed : now)
                .retrievedAt(now)
                .expiresAt(expires)
                .dataQuality(SourceEvent.QUALITY_IMPORTED)
                .note("Importação pessoal do usuário; não é feed ao vivo.");
        if (row.has("coordinate_precision"))
            builder.precision(SourceEvent.CoordinatePrecision.fromWire(row.optString("coordinate_precision", "")));
        return builder.build();
    }

    private static SourceEvent.Kind kind(String raw) {
        if (raw == null) return SourceEvent.Kind.POKEMON;
        String value = raw.trim().toLowerCase(Locale.ROOT);
        if (value.contains("raid")) return SourceEvent.Kind.RAID;
        if (value.contains("quest") || value.contains("research") || value.contains("pesquisa")) return SourceEvent.Kind.QUEST;
        if (value.contains("rocket") || value.contains("invasion") || value.contains("grunt")) return SourceEvent.Kind.ROCKET;
        if (value.contains("power") || value.contains("spot")) return SourceEvent.Kind.POWERSPOT;
        if (value.contains("nest") || value.contains("ninho")) return SourceEvent.Kind.NEST;
        if (value.contains("route") || value.contains("rota") || value.contains("waypoint")) return SourceEvent.Kind.ROUTE;
        return SourceEvent.Kind.POKEMON;
    }

    private static Double firstCoordinate(JSONObject row, String... keys) {
        for (String key : keys) {
            Double value = SourceEvent.coordinate(row.opt(key));
            if (value != null) return value;
        }
        return null;
    }

    private static Integer firstInteger(JSONObject row, String... keys) {
        for (String key : keys) {
            Integer value = SourceEvent.integer(row.opt(key));
            if (value != null) return value;
        }
        return null;
    }

    private static Double firstDecimal(JSONObject row, String... keys) {
        for (String key : keys) {
            Double value = SourceEvent.decimal(row.opt(key));
            if (value != null) return value;
        }
        return null;
    }

    private static String firstString(JSONObject row, String... keys) {
        for (String key : keys) {
            if (!row.has(key) || row.isNull(key)) continue;
            String value = row.optString(key, "").trim();
            if (!value.isEmpty()) return value;
        }
        return null;
    }

    /** Accepts epoch seconds, epoch millis and ISO-8601; 0 when absent or unreadable. */
    static long firstTime(JSONObject row, String... keys) {
        for (String key : keys) {
            if (!row.has(key) || row.isNull(key)) continue;
            Object value = row.opt(key);
            if (value instanceof Number) {
                long raw = ((Number) value).longValue();
                if (raw >= 1_000_000_000L && raw < 10_000_000_000L) return raw * 1000L;
                if (raw >= 1_000_000_000_000L && raw < 10_000_000_000_000L) return raw;
                continue;
            }
            String text = String.valueOf(value).trim();
            if (text.isEmpty()) continue;
            try {
                long raw = Long.parseLong(text);
                if (raw >= 1_000_000_000L && raw < 10_000_000_000L) return raw * 1000L;
                if (raw >= 1_000_000_000_000L && raw < 10_000_000_000_000L) return raw;
            } catch (NumberFormatException ignored) {
                // fall through to ISO-8601
            }
            long iso = IFlowGoFeedData.isoMs(text);
            if (iso > 0) return iso;
        }
        return 0L;
    }

    // --- CSV -------------------------------------------------------------------------------------

    private static boolean looksLikeCsv(String text) {
        String first = firstLine(text).toLowerCase(Locale.ROOT);
        return first.contains("lat") && first.contains("lon") && (first.contains(",") || first.contains(";"));
    }

    /**
     * The first meaningful line decides the shape, so a single bad line among several coordinates is
     * reported per line by {@link #parseCoordinates} instead of failing the whole file.
     */
    private static boolean looksLikeCoordinates(String text) {
        for (String line : text.split("\\r?\\n")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;
            return trimmed.matches("[-+]?[0-9]+(\\.[0-9]+)?\\s*[,;\\s]\\s*[-+]?[0-9]+(\\.[0-9]+)?");
        }
        return false;
    }

    private static Report parseCsv(String text, long now, String sourceId) {
        String[] lines = text.split("\\r?\\n");
        if (lines.length == 0) return new Report(Format.CSV, Collections.<SourceEvent>emptyList(), 0,
                Collections.singletonList(new Rejection(1, "CSV vazio.", "")), "");
        String header = lines[0];
        String delimiter = header.contains(";") && !header.contains(",") ? ";" : ",";
        String[] columns = header.toLowerCase(Locale.ROOT).split(delimiter, -1);
        Map<String, Integer> index = new LinkedHashMap<>();
        for (int i = 0; i < columns.length; i++) index.put(columns[i].trim(), i);
        int latIndex = indexOf(index, "lat", "latitude");
        int lonIndex = indexOf(index, "lon", "lng", "longitude");
        if (latIndex < 0 || lonIndex < 0)
            return new Report(Format.CSV, Collections.<SourceEvent>emptyList(), 0,
                    Collections.singletonList(new Rejection(1,
                            "CSV sem colunas de latitude/longitude no cabeçalho.", header)), "");

        List<SourceEvent> out = new ArrayList<>();
        List<Rejection> rejections = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        int duplicates = 0;
        int limit = Math.min(lines.length, MAX_ROWS + 1);
        for (int i = 1; i < limit; i++) {
            String line = lines[i];
            if (line.trim().isEmpty()) continue;
            String[] cells = line.split(delimiter, -1);
            Double lat = cell(cells, latIndex);
            Double lon = cell(cells, lonIndex);
            if (!SourceEvent.validCoordinates(lat, lon)) {
                rejections.add(new Rejection(i + 1, "Coordenada inválida ou ausente.", line));
                continue;
            }
            Integer dex = integerCell(cells, index, "pokemon_id", "pokemon", "dex", "number", "species");
            Integer cp = integerCell(cells, index, "cp");
            Integer level = integerCell(cells, index, "level");
            Double iv = decimalCell(cells, index, "iv", "iv_percent", "percent_iv");
            Integer atk = integerCell(cells, index, "attack", "atk", "iv_atk");
            Integer def = integerCell(cells, index, "defense", "def", "iv_def");
            Integer sta = integerCell(cells, index, "hp", "sta", "iv_sta");
            String id = stringCell(cells, index, "id", "event_id", "spawn_id");
            String name = stringCell(cells, index, "name");
            String rawKind = stringCell(cells, index, "kind", "type");
            String expiresText = stringCell(cells, index, "expires_at", "expires", "end_time", "despawn");
            long expires = 0L;
            if (expiresText != null) {
                try {
                    long raw = Long.parseLong(expiresText.trim());
                    expires = raw >= 1_000_000_000_000L ? raw : (raw >= 1_000_000_000L ? raw * 1000L : 0L);
                } catch (NumberFormatException notNumeric) {
                    expires = IFlowGoFeedData.isoMs(expiresText);
                }
            }
            String resolvedId = id == null || id.trim().isEmpty()
                    ? (dex == null ? "x" : dex.toString()) + ":"
                      + String.format(Locale.US, "%.6f,%.6f", lat, lon)
                    : id.trim();
            String key = (sourceId == null ? IMPORT_SOURCE : sourceId) + "|" + resolvedId;
            if (!seen.add(key)) { duplicates++; continue; }
            if (dex != null && (dex < 1 || dex > 1025)) dex = null;
            if (iv != null && (iv < 0 || iv > 100)) iv = null;
            out.add(SourceEvent.builder(sourceId == null ? IMPORT_SOURCE : sourceId, resolvedId, kind(rawKind))
                    .originKind(OriginKind.PERSONAL_IMPORT)
                    .coordinates(lat, lon).pokemonId(dex)
                    .name(name == null ? "" : name)
                    .ivPercent(iv).ivAtk(atk).ivDef(def).ivSta(sta).cp(cp).level(level)
                    .observedAt(now).retrievedAt(now).expiresAt(expires)
                    .dataQuality(SourceEvent.QUALITY_IMPORTED)
                    .note("Importação pessoal (CSV).")
                    .build());
        }
        return new Report(Format.CSV, out, duplicates, rejections,
                out.isEmpty() ? "Nenhuma linha CSV válida." : "");
    }

    private static int indexOf(Map<String, Integer> index, String... keys) {
        for (String key : keys) {
            Integer found = index.get(key);
            if (found != null) return found;
        }
        return -1;
    }

    private static Double cell(String[] cells, int index) {
        if (index < 0 || index >= cells.length) return null;
        return SourceEvent.coordinate(cells[index].trim());
    }

    private static Integer integerCell(String[] cells, Map<String, Integer> index, String... keys) {
        int at = indexOf(index, keys);
        if (at < 0 || at >= cells.length) return null;
        return SourceEvent.integer(cells[at].trim());
    }

    private static Double decimalCell(String[] cells, Map<String, Integer> index, String... keys) {
        int at = indexOf(index, keys);
        if (at < 0 || at >= cells.length) return null;
        return SourceEvent.decimal(cells[at].trim());
    }

    private static String stringCell(String[] cells, Map<String, Integer> index, String... keys) {
        int at = indexOf(index, keys);
        if (at < 0 || at >= cells.length) return null;
        String value = cells[at].trim();
        return value.isEmpty() ? null : value;
    }

    // --- GPX -------------------------------------------------------------------------------------

    private static Report parseGpx(String text, long now, String sourceId) {
        List<SourceEvent> out = new ArrayList<>();
        List<Rejection> rejections = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        int duplicates = 0;
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            try { factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true); }
            catch (Exception ignored) { }
            try { factory.setFeature("http://xml.org/sax/features/external-general-entities", false); }
            catch (Exception ignored) { }
            try { factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false); }
            catch (Exception ignored) { }
            SAXParser parser = factory.newSAXParser();
            final List<double[]> points = new ArrayList<>();
            final List<String> names = new ArrayList<>();
            parser.parse(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)), new DefaultHandler() {
                private StringBuilder name = new StringBuilder();
                private boolean inName;
                private int depth, pointDepth = -1, nameDepth = -1;

                @Override public void startElement(String uri, String localName, String qName, Attributes attributes) {
                    depth++;
                    String element = localName == null || localName.isEmpty() ? qName : localName;
                    if ("wpt".equals(element) || "trkpt".equals(element) || "rtept".equals(element)) {
                        Double lat = SourceEvent.coordinate(attributes.getValue("lat"));
                        Double lon = SourceEvent.coordinate(attributes.getValue("lon"));
                        points.add(new double[]{lat == null ? Double.NaN : lat,
                                lon == null ? Double.NaN : lon});
                        names.add("");
                        pointDepth = depth;
                    } else if ("name".equals(element) && pointDepth >= 0 && depth == pointDepth + 1) {
                        inName = true;
                        nameDepth = depth;
                        name.setLength(0);
                    }
                }

                @Override public void characters(char[] ch, int start, int length) {
                    if (inName) name.append(ch, start, length);
                }

                @Override public void endElement(String uri, String localName, String qName) {
                    String element = localName == null || localName.isEmpty() ? qName : localName;
                    if ("name".equals(element) && inName && depth == nameDepth) {
                        inName = false;
                        nameDepth = -1;
                        if (!names.isEmpty()) names.set(names.size() - 1, name.toString().trim());
                    }
                    if (depth == pointDepth) pointDepth = -1;
                    depth--;
                }
            });
            int limit = Math.min(points.size(), MAX_ROWS);
            for (int i = 0; i < limit; i++) {
                double[] point = points.get(i);
                if (!SourceEvent.validCoordinates(point[0], point[1])) {
                    rejections.add(new Rejection(i + 1, "Ponto GPX sem coordenada válida.",
                            names.size() > i ? names.get(i) : ""));
                    continue;
                }
                String label = names.size() > i && !names.get(i).isEmpty() ? names.get(i) : "Ponto " + (i + 1);
                String resolvedId = "gpx:" + i + ":" + String.format(Locale.US, "%.6f,%.6f", point[0], point[1]);
                String key = (sourceId == null ? IMPORT_SOURCE : sourceId) + "|" + resolvedId;
                if (!seen.add(key)) { duplicates++; continue; }
                out.add(SourceEvent.builder(sourceId == null ? IMPORT_SOURCE : sourceId, resolvedId,
                                SourceEvent.Kind.ROUTE)
                        .originKind(OriginKind.PERSONAL_IMPORT)
                        .coordinates(point[0], point[1]).name(label)
                        .observedAt(now).retrievedAt(now)
                        .dataQuality(SourceEvent.QUALITY_IMPORTED)
                        .note("Ponto GPX importado; não é um spawn ativo.")
                        .build());
            }
            if (points.size() > MAX_ROWS)
                return new Report(Format.GPX, out, duplicates, rejections,
                        "Apenas os primeiros " + MAX_ROWS + " pontos foram importados.");
            return new Report(Format.GPX, out, duplicates, rejections,
                    out.isEmpty() ? "GPX sem pontos válidos." : "");
        } catch (Exception error) {
            rejections.add(new Rejection(1, "GPX inválido: " + message(error), ""));
            return new Report(Format.GPX, Collections.<SourceEvent>emptyList(), 0, rejections, "");
        }
    }

    // --- typed/clipboard coordinates -------------------------------------------------------------

    private static Report parseCoordinates(String text, long now, String sourceId) {
        List<SourceEvent> out = new ArrayList<>();
        List<Rejection> rejections = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        int duplicates = 0;
        String[] lines = text.split("\\r?\\n");
        int limit = Math.min(lines.length, MAX_ROWS);
        for (int i = 0; i < limit; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) continue;
            String normalized = line.replace(";", ",").trim();
            String[] parts = normalized.contains(",") ? normalized.split(",")
                    : normalized.split("\\s+");
            if (parts.length < 2) {
                rejections.add(new Rejection(i + 1, "Use o formato lat, lon.", line));
                continue;
            }
            Double lat = SourceEvent.coordinate(parts[0]);
            Double lon = SourceEvent.coordinate(parts[1]);
            if (!SourceEvent.validCoordinates(lat, lon)) {
                rejections.add(new Rejection(i + 1, "Coordenada fora do intervalo (ou 0,0).", line));
                continue;
            }
            String id = "coord:" + String.format(Locale.US, "%.6f,%.6f", lat, lon);
            String key = (sourceId == null ? IMPORT_SOURCE : sourceId) + "|" + id;
            if (!seen.add(key)) { duplicates++; continue; }
            out.add(SourceEvent.builder(sourceId == null ? IMPORT_SOURCE : sourceId, id,
                            SourceEvent.Kind.ROUTE)
                    .originKind(OriginKind.PERSONAL_IMPORT)
                    .coordinates(lat, lon).name("Coordenada importada")
                    .observedAt(now).retrievedAt(now)
                    .dataQuality(SourceEvent.QUALITY_IMPORTED)
                    .note("Coordenada digitada/colada pelo usuário.")
                    .build());
        }
        return new Report(Format.COORDINATES, out, duplicates, rejections,
                out.isEmpty() ? "Nenhuma coordenada válida." : "");
    }

    // --- export ----------------------------------------------------------------------------------

    /**
     * GPX 1.1 containing only the supplied rows that have valid coordinates. Used by the "exportar
     * seleção" action, so an expired or coordinate-less row can never leak into a file.
     */
    public static String toGpx(List<SourceEvent> events) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
          .append("<gpx version=\"1.1\" creator=\"PGBR\" xmlns=\"http://www.topografix.com/GPX/1/1\">\n");
        int written = 0;
        if (events != null) for (SourceEvent event : events) {
            if (event == null || !event.hasCoordinates()) continue;
            sb.append("  <wpt lat=\"").append(String.format(Locale.US, "%.6f", event.lat))
              .append("\" lon=\"").append(String.format(Locale.US, "%.6f", event.lon)).append("\">");
            sb.append("<name>").append(escape(event.name.isEmpty() ? event.eventId : event.name)).append("</name>");
            sb.append("<desc>").append(escape("origem=" + event.originKind.wire)).append("</desc>");
            sb.append("</wpt>\n");
            written++;
            if (written >= MAX_ROWS) break;
        }
        sb.append("</gpx>\n");
        return sb.toString();
    }

    private static String escape(String value) {
        return value == null ? "" : value.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;");
    }

    private static String firstLine(String text) {
        int end = text.indexOf('\n');
        return end < 0 ? text : text.substring(0, end);
    }
}
