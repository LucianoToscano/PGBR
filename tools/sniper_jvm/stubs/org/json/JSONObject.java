package org.json;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Minimal org.json object used only to compile/run the PGBR engine tests on a JVM. */
public class JSONObject {
    private final Map<String, Object> values = new LinkedHashMap<>();

    public JSONObject() {}

    public JSONObject(String text) throws JSONException {
        Object parsed = new Json(text).parseValue();
        if (!(parsed instanceof Map)) throw new JSONException("objeto esperado");
        @SuppressWarnings("unchecked") Map<String, Object> map = (Map<String, Object>) parsed;
        values.putAll(map);
    }

    public JSONObject put(String key, Object value) throws JSONException {
        if (key == null) throw new JSONException("chave nula");
        values.put(key, value);
        return this;
    }
    public JSONObject put(String key, int value) throws JSONException { return put(key, (Object) Integer.valueOf(value)); }
    public JSONObject put(String key, long value) throws JSONException { return put(key, (Object) Long.valueOf(value)); }
    public JSONObject put(String key, double value) throws JSONException { return put(key, (Object) Double.valueOf(value)); }
    public JSONObject put(String key, boolean value) throws JSONException { return put(key, (Object) Boolean.valueOf(value)); }

    public boolean has(String key) { return values.containsKey(key); }
    public boolean isNull(String key) { return !values.containsKey(key) || values.get(key) == null; }

    public JSONObject getJSONObject(String key) throws JSONException {
        Object value = values.get(key);
        if (!(value instanceof JSONObject)) throw new JSONException("objeto ausente: " + key);
        return (JSONObject) value;
    }
    public JSONArray getJSONArray(String key) throws JSONException {
        Object value = values.get(key);
        if (!(value instanceof JSONArray)) throw new JSONException("array ausente: " + key);
        return (JSONArray) value;
    }
    public int getInt(String key) throws JSONException {
        Object value = values.get(key);
        if (value instanceof Number) return ((Number) value).intValue();
        throw new JSONException("número ausente: " + key);
    }
    public long getLong(String key) throws JSONException {
        Object value = values.get(key);
        if (value instanceof Number) return ((Number) value).longValue();
        throw new JSONException("número ausente: " + key);
    }
    public double getDouble(String key) throws JSONException {
        Object value = values.get(key);
        if (value instanceof Number) return ((Number) value).doubleValue();
        throw new JSONException("número ausente: " + key);
    }
    public String getString(String key) throws JSONException {
        Object value = values.get(key);
        if (value instanceof String) return (String) value;
        throw new JSONException("texto ausente: " + key);
    }
    public boolean getBoolean(String key) throws JSONException {
        Object value = values.get(key);
        if (value instanceof Boolean) return (Boolean) value;
        throw new JSONException("booleano ausente: " + key);
    }
    public boolean optBoolean(String key) { return optBoolean(key, false); }

    public Object remove(String key) { return values.remove(key); }
    public int length() { return values.size(); }
    public Object opt(String key) { return values.get(key); }
    public int optInt(String key) { return optInt(key, 0); }
    public long optLong(String key) { return optLong(key, 0L); }
    public long optLong(String key, long fallback) {
        Object value = values.get(key);
        if (value instanceof Number) return ((Number) value).longValue();
        if (value instanceof String) {
            try { return Long.parseLong(((String) value).trim()); } catch (NumberFormatException e) { return fallback; }
        }
        return fallback;
    }
    public int optInt(String key, int fallback) {
        Object value = values.get(key);
        if (value instanceof Number) return ((Number) value).intValue();
        if (value instanceof String) {
            try { return Integer.parseInt(((String) value).trim()); } catch (NumberFormatException e) { return fallback; }
        }
        return fallback;
    }
    public double optDouble(String key) { return optDouble(key, Double.NaN); }
    public double optDouble(String key, double fallback) {
        Object value = values.get(key);
        if (value instanceof Number) return ((Number) value).doubleValue();
        if (value instanceof String) {
            try { return Double.parseDouble(((String) value).trim()); } catch (NumberFormatException e) { return fallback; }
        }
        return fallback;
    }
    public boolean optBoolean(String key, boolean fallback) {
        Object value = values.get(key);
        return value instanceof Boolean ? (Boolean) value : fallback;
    }
    public String optString(String key) { return optString(key, ""); }
    public String optString(String key, String fallback) {
        Object value = values.get(key);
        return value == null ? fallback : String.valueOf(value);
    }
    public JSONObject optJSONObject(String key) {
        Object value = values.get(key);
        return value instanceof JSONObject ? (JSONObject) value : null;
    }
    public JSONArray optJSONArray(String key) {
        Object value = values.get(key);
        return value instanceof JSONArray ? (JSONArray) value : null;
    }
    public java.util.Iterator<String> keys() { return new ArrayList<>(values.keySet()).iterator(); }

    /** Removes every key; used by tests that recycle an instance. */
    public JSONObject clearAll() { values.clear(); return this; }

    @Override public String toString() { return Json.writeObject(values); }

    Map<String, Object> raw() { return values; }
}
