package android.app;
import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.widget.Button;
public class AlertDialog {
    public static final int BUTTON_POSITIVE = -1;
    public static final int BUTTON_NEGATIVE = -2;
    public static final int BUTTON_NEUTRAL = -3;
    public boolean isShowing() { return false; }
    public void show() { }
    public void dismiss() { }
    public void setTitle(CharSequence title) { }
    public void setMessage(CharSequence message) { }
    public void setView(View view) { }
    public Button getButton(int which) { return null; }
    public void setOnShowListener(DialogInterface.OnShowListener listener) { }

    public static class Builder {
        public Builder(Context context) { }
        public Builder setTitle(CharSequence title) { return this; }
        public Builder setMessage(CharSequence message) { return this; }
        public Builder setView(View view) { return this; }
        public Builder setItems(CharSequence[] items, DialogInterface.OnClickListener listener) { return this; }
        public Builder setPositiveButton(CharSequence text, DialogInterface.OnClickListener listener) { return this; }
        public Builder setNegativeButton(CharSequence text, DialogInterface.OnClickListener listener) { return this; }
        public Builder setNeutralButton(CharSequence text, DialogInterface.OnClickListener listener) { return this; }
        public AlertDialog create() { return new AlertDialog(); }
    }
}
