package android.widget;
import android.content.Context;
import android.graphics.Typeface;
public class TextView extends android.view.View {
    public TextView(Context context) { }
    public void setText(CharSequence text) { }
    public CharSequence getText() { return ""; }
    public void setTextSize(float size) { }
    public void setTextColor(int color) { }
    public void setTypeface(Typeface tf, int style) { }
    public void setGravity(int gravity) { }
    public void setHint(CharSequence hint) { }
    public void setSingleLine(boolean singleLine) { }
    public void addTextChangedListener(android.text.TextWatcher watcher) { }
}
