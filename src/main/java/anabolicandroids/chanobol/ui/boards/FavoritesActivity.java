package anabolicandroids.chanobol.ui.boards;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import anabolicandroids.chanobol.App;
import anabolicandroids.chanobol.R;
import anabolicandroids.chanobol.annotations.Nsfw;
import anabolicandroids.chanobol.annotations.SfwMode;
import anabolicandroids.chanobol.api.data.Board;
import anabolicandroids.chanobol.ui.PersistentData;
import anabolicandroids.chanobol.ui.UiActivity;
import anabolicandroids.chanobol.ui.threads.ThreadsActivity;
import anabolicandroids.chanobol.util.Util;
import butterknife.InjectView;

public class FavoritesActivity extends UiActivity {

    // Construction ////////////////////////////////////////////////////////////////////////////////

    public static void launch(Activity activity) {
        Intent intent = new Intent(activity, FavoritesActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        activity.startActivity(intent);
        activity.finish();
    }

    @InjectView(R.id.boards) RecyclerView favoritesView;

    @Inject @SfwMode boolean sfw;
    @Inject @Nsfw List<Board> allBoards;
    BoardsAdapter boardsAdapter;

    @Override protected int getLayoutResource() { return R.layout.fragment_boards; }
    @Override protected RecyclerView getRootRecyclerView() { return favoritesView; }

    @Override public void onCreate(Bundle savedInstanceState) {
        taskRoot = true;
        super.onCreate(savedInstanceState);
        setTitle(R.string.favorite_boards);

        if (!sfw && App.firstStart && persistentData.getFavorites().size() == 0) {
            App.firstStart = false;
            BoardsActivity.launch(this);
        }
        App.firstStart = false;

        boardsAdapter = new BoardsAdapter(this, ion,
                new ArrayList<>(persistentData.getFavorites()),
                clickListener, longClickListener);
        favoritesView.setAdapter(boardsAdapter);
        favoritesView.setHasFixedSize(true);
        GridLayoutManager glm = new GridLayoutManager(this, 2);
        favoritesView.setLayoutManager(glm);
        boardsAdapter.notifyDataSetChanged();
        Util.calcDynamicSpanCountById(this, favoritesView, glm, R.dimen.column_width);

        persistentData.addFavoritesChangedCallback(favoritesCallback);
    }

    // Callbacks ///////////////////////////////////////////////////////////////////////////////////

    View.OnClickListener clickListener = new View.OnClickListener() {
        @Override public void onClick(View v) {
            final BoardView bv = (BoardView) v;
            bv.postDelayed(new Runnable() {
                @Override public void run() {
                    ThreadsActivity.launch(FavoritesActivity.this, bv.board);
                }
            }, RIPPLE_DELAY);
        }
    };

    View.OnLongClickListener longClickListener = new View.OnLongClickListener() {
        @Override public boolean onLongClick(View v) {
            final BoardView bv = (BoardView) v;
            new AlertDialog.Builder(FavoritesActivity.this)
                    .setTitle(R.string.delete_title)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            persistentData.removeFavorite(bv.board);
                        }
                    }).show();
            return true;
        }
    };

    PersistentData.FavoritesCallback favoritesCallback = new PersistentData.FavoritesCallback() {
        @Override public void onChanged(Set<Board> newFavorites) {
            boardsAdapter.replaceWith(new ArrayList<>(newFavorites));
        }
    };

    // Lifecycle ///////////////////////////////////////////////////////////////////////////////////

    @Override public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Why is there no onPostConfigurationChanged...
        Util.updateRecyclerViewGridOnConfigChange(favoritesView, R.dimen.column_width);
    }

    // Toolbar Menu ////////////////////////////////////////////////////////////////////////////////

    @Override public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.favorites, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.addFavorite) {
            final EditText input = new EditText(this);
            input.setHint("ck");
            new AlertDialog.Builder(this)
                    .setTitle(R.string.add_favorite_title)
                    .setView(input)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            String normalizedName = input.getText().toString().replace("/", "");
                            for (Board board : allBoards) {
                                if (board.name.equals(normalizedName)) {
                                    persistentData.addFavorite(board);
                                    showToast(board.name + " " + resources.getString(R.string.favorite_added));
                                    return;
                                }
                            }
                            showToast(R.string.no_board);
                        }
                    }).show();
        }
        return super.onOptionsItemSelected(item);
    }
}
