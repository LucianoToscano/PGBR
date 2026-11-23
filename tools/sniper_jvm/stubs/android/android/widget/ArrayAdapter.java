package android.widget;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import java.util.List;
public class ArrayAdapter<T> {
    public ArrayAdapter(Context context, int resource, List<T> objects) { }
    public View getView(int position, View convertView, ViewGroup parent) { return null; }
    public T getItem(int position) { return null; }
    public int getCount() { return 0; }
    public void notifyDataSetChanged() { }
    public void setDropDownViewResource(int resource) { }
}
