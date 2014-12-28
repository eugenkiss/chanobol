package anabolicandroids.chanobol.ui;

import android.content.Context;

import java.util.List;

import anabolicandroids.chanobol.BindableAdapter;

public abstract class UiAdapter<T> extends BindableAdapter<T> {
    protected List<T> items;

    public UiAdapter(Context context) {
        super(context);
    }

    public void replaceWith(List<T> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public T getItem(int position) {
        return items.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }
}
