package android.app;
public class Activity extends android.content.Context {
    public boolean isFinishing() { return false; }
    public boolean isDestroyed() { return false; }
    public android.content.Context getApplicationContext() { return this; }
    public void runOnUiThread(Runnable action) { }
}
