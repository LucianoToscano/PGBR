package android.content;

/** Espelho da interface real do Android (só o que o app usa; remove()/commit() existem de verdade). */
public interface SharedPreferences {
    String getString(String key, String defValue);
    int getInt(String key, int defValue);
    long getLong(String key, long defValue);
    boolean getBoolean(String key, boolean defValue);
    boolean contains(String key);
    Editor edit();

    interface Editor {
        Editor putString(String key, String value);
        Editor putInt(String key, int value);
        Editor putLong(String key, long value);
        Editor putBoolean(String key, boolean value);
        Editor remove(String key);
        Editor clear();
        void apply();
        boolean commit();
    }
}
