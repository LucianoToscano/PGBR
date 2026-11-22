package android.view;
public class View {
    public static final int VISIBLE = 0;
    public interface OnClickListener { void onClick(View v); }
    public void setOnClickListener(OnClickListener listener) { }
    public void setPadding(int l, int t, int r, int b) { }
    public void setVisibility(int visibility) { }
    public ViewGroup.LayoutParams getLayoutParams() { return null; }
    public void setLayoutParams(ViewGroup.LayoutParams params) { }
    public android.content.Context getContext() { return null; }
}
