package android.widget;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
public class ListView extends ViewGroup {
    public ListView(Context context) { }
    public void setAdapter(android.widget.ArrayAdapter<?> adapter) { }
    public void setOnItemClickListener(OnItemClickListener listener) { }
    public interface OnItemClickListener {
        void onItemClick(AdapterView<?> parent, View view, int position, long id);
    }
    public static class AdapterView<T> { }
}
