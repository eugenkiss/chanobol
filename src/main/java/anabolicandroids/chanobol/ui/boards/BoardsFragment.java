package anabolicandroids.chanobol.ui.boards;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;

import java.util.List;

import javax.inject.Inject;

import anabolicandroids.chanobol.R;
import anabolicandroids.chanobol.api.data.Board;
import anabolicandroids.chanobol.ui.UiFragment;
import anabolicandroids.chanobol.ui.threads.ThreadsFragment;
import butterknife.InjectView;

public class BoardsFragment extends UiFragment {
    @InjectView(R.id.boards) GridView boardsView;

    @Inject List<Board> boards;
    BoardsAdapter boardsAdapter;

    @Override
    protected int getLayoutResource() {
        return R.layout.fragment_boards;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        boardsAdapter = new BoardsAdapter(context, ion, boards);
        boardsView.setAdapter(boardsAdapter);

        boardsView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Fragment f = ThreadsFragment.create(boardsAdapter.getItem(position).name);
                getFragmentManager().beginTransaction()
                        .replace(R.id.container, f, null)
                        .commit();
            }
        });

        boardsView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
                new AlertDialog.Builder(context)
                        .setTitle("Favorize?")
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                Board board = boardsAdapter.getItem(position);
                                persistentData.addFavorite(board);
                                showToast(board.name + " added to favorites");
                            }
                        }).setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // Do nothing.
                    }
                }).show();
                return true;
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        activity.setTitle("All Boards");
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
