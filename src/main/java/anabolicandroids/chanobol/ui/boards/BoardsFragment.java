package anabolicandroids.chanobol.ui.boards;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import java.util.List;

import javax.inject.Inject;

import anabolicandroids.chanobol.R;
import anabolicandroids.chanobol.api.data.Board;
import anabolicandroids.chanobol.ui.UiFragment;
import anabolicandroids.chanobol.ui.threads.ThreadsFragment;
import anabolicandroids.chanobol.util.Util;
import butterknife.InjectView;

public class BoardsFragment extends UiFragment {
    @InjectView(R.id.boards) RecyclerView boardsView;

    @Inject List<Board> boards;
    BoardsAdapter boardsAdapter;

    @Override
    protected int getLayoutResource() {
        return R.layout.fragment_boards;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        View.OnClickListener clickListener = new View.OnClickListener() {
            @Override public void onClick(View v) {
                BoardView bv = (BoardView) v;
                Fragment f = ThreadsFragment.create(bv.board.name);
                getFragmentManager().beginTransaction()
                        .replace(R.id.container, f, null)
                        .commit();
            }
        };
        View.OnLongClickListener longClickListener = new View.OnLongClickListener() {
            @Override public boolean onLongClick(View v) {
                final BoardView bv = (BoardView) v;
                new AlertDialog.Builder(context)
                        .setTitle(R.string.favorize_title)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                Board board = bv.board;
                                persistentData.addFavorite(board);
                                showToast(board.name + " " + resources.getString(R.string.favorite_added));
                            }
                        }).setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // Do nothing.
                    }
                }).show();
                return true;
            }
        };

        boardsAdapter = new BoardsAdapter(context, ion, boards, clickListener, longClickListener);
        boardsView.setAdapter(boardsAdapter);
        boardsView.setHasFixedSize(true);
        GridLayoutManager glm = new GridLayoutManager(context, 2);
        boardsView.setLayoutManager(glm);
        boardsView.setItemAnimator(new DefaultItemAnimator());
        Util.calcDynamicSpanCount(context, boardsView, glm);
    }

    @Override
    public void onResume() {
        super.onResume();
        activity.setTitle(R.string.all_boards);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.boards, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }
}
