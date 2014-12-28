package anabolicandroids.chanobol.ui.boards;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.squareup.picasso.Picasso;

import java.util.List;

import anabolicandroids.chanobol.R;
import anabolicandroids.chanobol.api.data.Board;
import anabolicandroids.chanobol.ui.UiAdapter;

class BoardsAdapter extends UiAdapter<Board> {
    private Picasso picasso;

    public BoardsAdapter(Context context, Picasso picasso, List<Board> boards) {
        super(context);
        this.picasso = picasso;
        this.items = boards;
    }

    @Override
    public View newView(LayoutInflater inflater, int position, ViewGroup container) {
        return inflater.inflate(R.layout.view_board, container, false);
    }

    @Override
    public void bindView(Board item, int position, View view) {
        ((BoardView) view).bindTo(item, picasso);
    }
}
