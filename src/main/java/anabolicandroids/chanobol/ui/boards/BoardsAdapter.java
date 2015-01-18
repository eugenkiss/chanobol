package anabolicandroids.chanobol.ui.boards;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import com.koushikdutta.ion.Ion;

import java.util.List;

import anabolicandroids.chanobol.R;
import anabolicandroids.chanobol.api.data.Board;
import anabolicandroids.chanobol.ui.UiAdapter;

class BoardsAdapter extends UiAdapter<Board> {
    private Ion ion;

    public BoardsAdapter(Context context, Ion ion, List<Board> boards,
                         View.OnClickListener clickListener,
                         View.OnLongClickListener longClickListener) {
        super(context, clickListener, longClickListener);
        this.ion = ion;
        this.items = boards;
    }

    @Override public View newView(ViewGroup container) {
        return inflater.inflate(R.layout.view_board, container, false);
    }

    @Override public void bindView(Board item, int position, View view) {
        ((BoardView) view).bindTo(item, ion);
    }
}
