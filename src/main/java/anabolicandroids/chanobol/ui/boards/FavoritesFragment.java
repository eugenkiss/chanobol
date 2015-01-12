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
import android.widget.EditText;
import android.widget.GridView;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import anabolicandroids.chanobol.R;
import anabolicandroids.chanobol.annotations.Nsfw;
import anabolicandroids.chanobol.api.ChanService;
import anabolicandroids.chanobol.api.data.Board;
import anabolicandroids.chanobol.ui.PersistentData;
import anabolicandroids.chanobol.ui.UiFragment;
import anabolicandroids.chanobol.ui.threads.ThreadsFragment;
import butterknife.InjectView;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class FavoritesFragment extends UiFragment {
    @InjectView(R.id.boards) GridView favoritesView;

    @Inject @Nsfw List<Board> allBoards;
    BoardsAdapter boardsAdapter;

    @Override
    protected int getLayoutResource() {
        return R.layout.fragment_boards;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        boardsAdapter = new BoardsAdapter(context, ion, new ArrayList<>(persistentData.getFavorites()));
        favoritesView.setAdapter(boardsAdapter);
        boardsAdapter.notifyDataSetChanged();
        persistentData.addFavoritesChangedCallback(new PersistentData.FavoritesCallback() {
            @Override
            public void onChanged(Set<Board> newFavorites) {
                boardsAdapter.replaceWith(new ArrayList<>(newFavorites));
            }
        });

        favoritesView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Fragment f = ThreadsFragment.create(boardsAdapter.getItem(position).name);
                getFragmentManager().beginTransaction()
                        .replace(R.id.container, f, null)
                        .commit();
            }
        });

        favoritesView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
                new AlertDialog.Builder(context)
                        .setTitle("Delete?")
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                persistentData.removeFavorite(boardsAdapter.getItem(position));
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
                    }).setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    // Do nothing.
                }
            }).show();
        }
        return super.onOptionsItemSelected(item);
    }
}
