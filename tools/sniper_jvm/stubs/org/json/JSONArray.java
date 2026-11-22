package org.json;
import java.util.ArrayList;
import java.util.List;

/** Minimal org.json array used only to compile/run the PGBR engine tests on a JVM. */
public class JSONArray {
    private final List<Object> values = new ArrayList<>();

    public JSONArray() {}

    public JSONArray put(Object value) { values.add(value); return this; }
    public JSONArray put(int value) { values.add(value); return this; }
    public JSONArray put(boolean value) { values.add(value); return this; }

    public int length() { return values.size(); }
    public Object get(int index) throws JSONException {
        Object value = opt(index);
        if (value == null) throw new JSONException("índice ausente: " + index);
        return value;
    }
    public JSONObject getJSONObject(int index) throws JSONException {
        Object value = opt(index);
        if (!(value instanceof JSONObject)) throw new JSONException("objeto ausente em " + index);
        return (JSONObject) value;
    }
    public Object opt(int index) { return index >= 0 && index < values.size() ? values.get(index) : null; }
    public int optInt(int index) { return optInt(index, 0); }
    public int optInt(int index, int fallback) {
        Object value = opt(index);
        if (value instanceof Number) return ((Number) value).intValue();
        if (value instanceof String) {
            try { return Integer.parseInt(((String) value).trim()); } catch (NumberFormatException e) { return fallback; }
        }
        return fallback;
    }
    public String optString(int index) { Object v = opt(index); return v == null ? "" : String.valueOf(v); }
    public JSONObject optJSONObject(int index) { Object v = opt(index); return v instanceof JSONObject ? (JSONObject) v : null; }

    @Override public String toString() { return Json.writeArray(values); }
}
