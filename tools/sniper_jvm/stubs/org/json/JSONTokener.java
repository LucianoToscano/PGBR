package org.json;

/**
 * Minimal JSONTokener for the JVM harness. The Android platform ships this exact class; the
 * vendored mirror only needs {@link #nextValue()} so {@code SettingsBackupRules} can validate
 * free-form JSON values without re-implementing the parser.
 */
public class JSONTokener {
    private final String text;
    private boolean consumed;

    public JSONTokener(String text) {
        this.text = text == null ? "" : text;
    }

    /** Parses the first JSON value in the source, or throws when there is none. */
    public Object nextValue() throws JSONException {
        if (consumed) throw new JSONException("fim do texto");
        consumed = true;
        return Json.convert(new Json(text).parseValue());
    }

    public char next() {
        return text.isEmpty() ? '\0' : text.charAt(0);
    }
}
