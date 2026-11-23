package android.widget;
import android.content.Context;
public class Spinner extends android.view.View {
    public Spinner(Context context) { }
    public void setAdapter(ArrayAdapter<?> adapter) { }
    public void setSelection(int position) { }
    /** Real platform API: index of the selected item, or -1 when nothing is selected. */
    public int getSelectedItemPosition() { return 0; }
}
