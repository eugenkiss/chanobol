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
import android.widget.EditText;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import anabolicandroids.chanobol.R;
import anabolicandroids.chanobol.annotations.Nsfw;
import anabolicandroids.chanobol.api.data.Board;
import anabolicandroids.chanobol.ui.PersistentData;
import anabolicandroids.chanobol.ui.UiFragment;
import anabolicandroids.chanobol.ui.threads.ThreadsFragment;
import anabolicandroids.chanobol.util.Util;
import butterknife.InjectView;

public class FavoritesFragment extends UiFragment {
    @InjectView(R.id.boards) RecyclerView favoritesView;

    @Inject @Nsfw List<Board> allBoards;
    BoardsAdapter boardsAdapter;

    @Override
    protected int getLayoutResource() { return R.layout.fragment_boards; }

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
                        .setTitle("Delete?")
                        .setNegativeButton(android.R.string.cancel, null)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                persistentData.removeFavorite(bv.board);
                            }
                        }).show();
                return true;
            }
        };

        boardsAdapter = new BoardsAdapter(context, ion,
                                          new ArrayList<>(persistentData.getFavorites()),
                                          clickListener, longClickListener);
        favoritesView.setAdapter(boardsAdapter);
        favoritesView.setHasFixedSize(true);
        GridLayoutManager glm = new GridLayoutManager(context, 2);
        favoritesView.setLayoutManager(glm);
        favoritesView.setItemAnimator(new DefaultItemAnimator());
        boardsAdapter.notifyDataSetChanged();
        Util.calcDynamicSpanCount(context, favoritesView, glm);

        persistentData.addFavoritesChangedCallback(new PersistentData.FavoritesCallback() {
            @Override
            public void onChanged(Set<Board> newFavorites) {
                boardsAdapter.replaceWith(new ArrayList<>(newFavorites));
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        activity.setTitle("Favorite Boards");
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.favorites, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.addFavorite) {
            final EditText input = new EditText(context);
            new AlertDialog.Builder(context)
                    .setTitle("Add Favorite Board")
                    .setView(input)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            String normalizedName = input.getText().toString().replace("/", "");
                            for (Board board : allBoards) {
                                if (board.name.equals(normalizedName)) {
                                    persistentData.addFavorite(board);
                                    showToast(board.name + " added as favorite");
                                    return;
                                }
                            }
                            showToast("Board doesn't exist");
                        }
                    }).show();
        }
        return super.onOptionsItemSelected(item);
    }
}
