package android.content;
public abstract class Context {
    public static final int MODE_PRIVATE = 0;
    public abstract Context getApplicationContext();
    public SharedPreferences getSharedPreferences(String name, int mode) { return null; }
    public android.content.res.Resources getResources() { return null; }
}
