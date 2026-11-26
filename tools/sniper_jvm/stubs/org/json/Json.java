package org.json;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Tiny JSON reader/writer, only for the offline JVM test harness. */
final class Json {
    private final String text;
    private int at;

    Json(String text) { this.text = text == null ? "" : text; }

    Object parseValue() throws JSONException {
        skip();
        if (at >= text.length()) throw new JSONException("fim inesperado");
        char c = text.charAt(at);
        if (c == '{') return parseObject();
        if (c == '[') return parseArray();
        if (c == '"') return parseString();
        if (text.startsWith("true", at)) { at += 4; return Boolean.TRUE; }
        if (text.startsWith("false", at)) { at += 5; return Boolean.FALSE; }
        if (text.startsWith("null", at)) { at += 4; return null; }
        return parseNumber();
    }

    /** Converte o resultado cru do parse nos tipos org.json, como a implementação real faz. */
    static Object convert(Object raw) {
        if (raw instanceof Map) {
            JSONObject object = new JSONObject();
            @SuppressWarnings("unchecked") Map<String, Object> map = (Map<String, Object>) raw;
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                try { object.put(entry.getKey(), convert(entry.getValue())); }
                catch (JSONException impossible) { throw new IllegalStateException(impossible); }
            }
            return object;
        }
        if (raw instanceof List) {
            JSONArray array = new JSONArray();
            for (Object item : (List<?>) raw) array.put(convert(item));
            return array;
        }
        return raw;
    }

    private Map<String, Object> parseObject() throws JSONException {
        expect('{');
        Map<String, Object> map = new LinkedHashMap<>();
        skip();
        if (peek() == '}') { at++; return map; }
        while (true) {
            skip();
            String key = parseString();
            skip();
            expect(':');
            map.put(key, parseValue());
            skip();
            char c = peek();
            if (c == ',') { at++; continue; }
            expect('}');
            return map;
        }
    }

    private List<Object> parseArray() throws JSONException {
        expect('[');
        List<Object> list = new ArrayList<>();
        skip();
        if (peek() == ']') { at++; return list; }
        while (true) {
            list.add(parseValue());
            skip();
            char c = peek();
            if (c == ',') { at++; continue; }
            expect(']');
            return list;
        }
    }

    private String parseString() throws JSONException {
        expect('"');
        StringBuilder out = new StringBuilder();
        while (true) {
            if (at >= text.length()) throw new JSONException("string sem fim");
            char c = text.charAt(at++);
            if (c == '"') return out.toString();
            if (c != '\\') { out.append(c); continue; }
            if (at >= text.length()) throw new JSONException("escape sem fim");
            char e = text.charAt(at++);
            switch (e) {
                case 'n': out.append('\n'); break;
                case 't': out.append('\t'); break;
                case 'r': out.append('\r'); break;
                case 'b': out.append('\b'); break;
                case 'f': out.append('\f'); break;
                case 'u':
                    if (at + 4 > text.length()) throw new JSONException("unicode inválido");
                    out.append((char) Integer.parseInt(text.substring(at, at + 4), 16));
                    at += 4;
                    break;
                default: out.append(e);
            }
        }
    }

    private Object parseNumber() throws JSONException {
        int start = at;
        while (at < text.length() && "+-.eE0123456789".indexOf(text.charAt(at)) >= 0) at++;
        String token = text.substring(start, at);
        if (token.isEmpty()) throw new JSONException("número inválido");
        if (token.indexOf('.') < 0 && token.indexOf('e') < 0 && token.indexOf('E') < 0) {
            try { return Long.valueOf(token); } catch (NumberFormatException big) { return Double.valueOf(token); }
        }
        return Double.valueOf(token);
    }

    private void skip() { while (at < text.length() && Character.isWhitespace(text.charAt(at))) at++; }
    private char peek() { return at < text.length() ? text.charAt(at) : '\0'; }
    private void expect(char c) throws JSONException {
        if (peek() != c) throw new JSONException("esperado '" + c + "' na posição " + at);
        at++;
    }

    // ------------------------------------------------------------------ writing

    static String writeObject(Map<String, Object> map) {
        StringBuilder out = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) out.append(',');
            first = false;
            out.append(quote(entry.getKey())).append(':').append(writeValue(entry.getValue()));
        }
        return out.append('}').toString();
    }

    static String writeArray(List<Object> list) {
        StringBuilder out = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) out.append(',');
            out.append(writeValue(list.get(i)));
        }
        return out.append(']').toString();
    }

    private static String writeValue(Object value) {
        // JSONObject.NULL é um objeto marcador, não um texto. Sem esta checagem o stub gravava
        // "null" ENTRE ASPAS e o JSON relido trazia a STRING "null"; no Android o campo volta
        // como nulo de verdade. Era essa divergência que fazia um backup com name/id nulos
        // parecer válido só no JVM (SettingsBackupRulesTest: "Accepted invalid backup").
        if (value == null || value == JSONObject.NULL) return "null";
        if (value instanceof JSONObject) return value.toString();
        if (value instanceof JSONArray) return value.toString();
        if (value instanceof Boolean) return value.toString();
        if (value instanceof Number) {
            double d = ((Number) value).doubleValue();
            if (Double.isNaN(d) || Double.isInfinite(d)) return "null";
            return value.toString();
        }
        return quote(String.valueOf(value));
    }

    private static String quote(String text) {
        StringBuilder out = new StringBuilder("\"");
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '"': out.append("\\\""); break;
                case '\\': out.append("\\\\"); break;
                case '\n': out.append("\\n"); break;
                case '\r': out.append("\\r"); break;
                case '\t': out.append("\\t"); break;
                default:
                    if (c < 0x20) out.append(String.format("\\u%04x", (int) c));
                    else out.append(c);
            }
        }
        return out.append('"').toString();
    }
}
