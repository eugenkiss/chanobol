package anabolicandroids.chanobol.ui;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

@SuppressWarnings("UnusedDeclaration")
public abstract class UiAdapter<T> extends RecyclerView.Adapter<UiAdapter.ViewHolder> {
    protected final Context context;
    protected final LayoutInflater inflater;
    protected final View.OnClickListener clickListener;
    protected final View.OnLongClickListener longClickListener;
    protected List<T> items;

    public UiAdapter(Context context,
                     View.OnClickListener clickListener,
                     View.OnLongClickListener longClickListener) {
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.clickListener = clickListener;
        this.longClickListener = longClickListener;
    }

    public UiAdapter(Context context, View.OnClickListener clickListener) {
        this(context, clickListener, null);
    }
    public UiAdapter(Context context, View.OnLongClickListener longClickListener) {
        this(context, null, longClickListener);
    }
    public UiAdapter(Context context) {
        this(context, null, null);
    }

    public abstract View newView(ViewGroup container);

    public abstract void bindView(T item, int position, View view);

    public void replaceWith(List<T> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    @Override public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = newView(parent);
        v.setOnClickListener(clickListener);
        v.setOnLongClickListener(longClickListener);
        return new ViewHolder(v);
    }

    @Override public void onBindViewHolder(ViewHolder holder, int position) {
        bindView(items.get(position), position, holder.itemView);
    }

    @Override public int getItemCount() { return items.size(); }
    @Override public long getItemId(int position) { return position; }

    static class ViewHolder extends RecyclerView.ViewHolder {
        public ViewHolder(View itemView) { super(itemView); }
    }
}
