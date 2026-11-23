package android.view;
public class ViewGroup extends View {
    public void addView(View child) { }
    public void addView(View child, LayoutParams params) { }
    public void setClipToPadding(boolean value) { }
    public static class LayoutParams {
        public static final int MATCH_PARENT = -1, WRAP_CONTENT = -2;
        public LayoutParams(int width, int height) { }
        public LayoutParams(int width, int height, float weight) { }
    }
    public static class MarginLayoutParams extends LayoutParams {
        public MarginLayoutParams(int width, int height) { super(width, height); }
    }
}
