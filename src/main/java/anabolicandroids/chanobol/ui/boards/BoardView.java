package anabolicandroids.chanobol.ui.boards;

import android.content.Context;
import android.support.v7.widget.CardView;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.TextView;

import com.koushikdutta.ion.Ion;

import anabolicandroids.chanobol.R;
import anabolicandroids.chanobol.api.data.Board;
import butterknife.ButterKnife;
import butterknife.InjectView;

public class BoardView extends CardView {
    @InjectView(R.id.name) TextView name;
    @InjectView(R.id.image) ImageView image;
    @InjectView(R.id.title) TextView title;

    public Board board;

    public BoardView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override protected void onFinishInflate() {
        super.onFinishInflate();
        ButterKnife.inject(this);
    }

    public void bindTo(Board board, Ion ion) {
        this.board = board;
        name.setText(board.name);
        title.setText(board.title);
        ion.build(image).load("file:///android_asset/boardimages/"+board.name+".jpg");
    }
}
