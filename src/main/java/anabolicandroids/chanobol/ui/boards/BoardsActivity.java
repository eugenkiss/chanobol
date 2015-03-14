package anabolicandroids.chanobol.ui.boards;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.View;

import java.util.List;

import javax.inject.Inject;

import anabolicandroids.chanobol.R;
import anabolicandroids.chanobol.api.data.Board;
import anabolicandroids.chanobol.ui.scaffolding.PersistentData;
import anabolicandroids.chanobol.ui.scaffolding.UiActivity;
import anabolicandroids.chanobol.ui.threads.ThreadsActivity;
import anabolicandroids.chanobol.util.Util;
import butterknife.InjectView;

public class BoardsActivity extends UiActivity {

    // Construction ////////////////////////////////////////////////////////////////////////////////

    public static void launch(Activity activity) {
        Intent intent = new Intent(activity, BoardsActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        activity.startActivity(intent);
        activity.finish();
    }

    @InjectView(R.id.boards) RecyclerView boardsView;

    @Inject List<Board> boards;
    BoardsAdapter boardsAdapter;

    @Override protected int getLayoutResource() { return R.layout.fragment_boards; }
    @Override protected RecyclerView getRootRecyclerView() { return boardsView; }

    @Override public void onCreate(Bundle savedInstanceState) {
        taskRoot = true;
        super.onCreate(savedInstanceState);
        setTitle(R.string.all_boards);

        boardsAdapter = new BoardsAdapter(this, ion, boards, clickListener, longClickListener);
        boardsView.setAdapter(boardsAdapter);
        boardsView.setHasFixedSize(true);
        GridLayoutManager glm = new GridLayoutManager(this, 2);
        boardsView.setLayoutManager(glm);
        boardsView.setItemAnimator(new DefaultItemAnimator());
        Util.calcDynamicSpanCountById(this, boardsView, glm, R.dimen.column_width);
    }

    // Callbacks ///////////////////////////////////////////////////////////////////////////////////

    View.OnClickListener clickListener = new View.OnClickListener() {
        @Override public void onClick(View v) {
            final BoardView bv = (BoardView) v;
            bv.postDelayed(new Runnable() {
                @Override public void run() {
                    ThreadsActivity.launch(BoardsActivity.this, bv.board);
                }
            }, RIPPLE_DELAY());
        }
    };

    View.OnLongClickListener longClickListener = new View.OnLongClickListener() {
        @Override public boolean onLongClick(View v) {
            final Board board = ((BoardView) v).board;
            showAddFavoriteDialog(BoardsActivity.this, persistentData, board);
            return true;
        }
    };

    public static void showAddFavoriteDialog(
            final Context context, final PersistentData persistentData, final Board board
    ) {
        new AlertDialog.Builder(context)
                .setTitle(R.string.favorize_title)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        persistentData.addFavorite(board);
                        Util.showToast(context,
                                board.name + " " + context.getResources().getString(R.string.favorite_added));
                    }
                }).show();
    }

    // Lifecycle ///////////////////////////////////////////////////////////////////////////////////

    @Override public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Util.updateRecyclerViewGridOnConfigChange(boardsView, R.dimen.column_width);
    }

    // Toolbar Menu ////////////////////////////////////////////////////////////////////////////////

    @Override public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.boards, menu);
        return true;
    }
}
