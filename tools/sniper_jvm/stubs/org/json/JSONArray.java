package org.json;
import java.util.ArrayList;
import java.util.List;

/** Minimal org.json array used only to compile/run the PGBR engine tests on a JVM. */
public class JSONArray {
    private final List<Object> values = new ArrayList<>();

    public JSONArray() {}

    /** Como no Android: aceita um array em texto e converte aninhados em JSONObject/JSONArray. */
    public JSONArray(String text) throws JSONException {
        Object parsed = Json.convert(new Json(text).parseValue());
        if (!(parsed instanceof JSONArray)) throw new JSONException("array esperado");
        values.addAll(((JSONArray) parsed).values);
    }

    /** Como no Android: aceita qualquer Collection, útil para montar fixtures no teste. */
    public JSONArray(java.util.Collection<?> items) {
        for (Object item : items) values.add(item instanceof java.util.Map
                ? Json.convert(item) : item);
    }

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
    public String optString(int index, String fallback) {
        Object v = opt(index);
        return v == null ? fallback : String.valueOf(v);
    }
    public JSONArray optJSONArray(int index) {
        Object value = opt(index);
        return value instanceof JSONArray ? (JSONArray) value : null;
    }
    public double optDouble(int index) { return optDouble(index, Double.NaN); }
    public double optDouble(int index, double fallback) {
        Object value = opt(index);
        if (value instanceof Number) return ((Number) value).doubleValue();
        if (value instanceof String) {
            try { return Double.parseDouble(((String) value).trim()); }
            catch (NumberFormatException e) { return fallback; }
        }
        return fallback;
    }
    public long optLong(int index) { return optLong(index, 0L); }
    public long optLong(int index, long fallback) {
        Object value = opt(index);
        if (value instanceof Number) return ((Number) value).longValue();
        return fallback;
    }
    public boolean optBoolean(int index) { return optBoolean(index, false); }
    public boolean optBoolean(int index, boolean fallback) {
        Object value = opt(index);
        return value instanceof Boolean ? (Boolean) value : fallback;
    }
    public String getString(int index) throws JSONException { return String.valueOf(get(index)); }
    public int getInt(int index) throws JSONException {
        Object value = get(index);
        if (value instanceof Number) return ((Number) value).intValue();
        if (value instanceof String) {
            try { return Integer.parseInt(((String) value).trim()); }
            catch (NumberFormatException notANumber) {
                throw new JSONException("número esperado em " + index);
            }
        }
        throw new JSONException("número esperado em " + index);
    }
    public JSONObject optJSONObject(int index) { Object v = opt(index); return v instanceof JSONObject ? (JSONObject) v : null; }

    @Override public String toString() { return Json.writeArray(values); }
}
