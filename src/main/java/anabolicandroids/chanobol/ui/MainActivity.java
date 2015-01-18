package anabolicandroids.chanobol.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.internal.widget.CompatTextView;
import android.support.v7.widget.Toolbar;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import anabolicandroids.chanobol.BaseActivity;
import anabolicandroids.chanobol.BuildConfig;
import anabolicandroids.chanobol.R;
import anabolicandroids.chanobol.api.data.Board;
import anabolicandroids.chanobol.ui.boards.BoardsFragment;
import anabolicandroids.chanobol.ui.boards.FavoritesFragment;
import anabolicandroids.chanobol.ui.threads.ThreadsFragment;
import anabolicandroids.chanobol.util.Util;
import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

// The following resources have been helpful:
// http://stackoverflow.com/questions/17258020/switching-between-android-navigation-drawer-image-and-up-caret-when-using-fragme?lq=1
public class MainActivity extends BaseActivity {
    @Inject SharedPreferences prefs;
    @Inject PersistentData persistentData;
    @Inject @Named("DebugSettings") Class debugSettingsClass;

    @InjectView(R.id.toolbar) Toolbar toolbar;
    @InjectView(R.id.loadingBar) ProgressBar loadingBar;
    @InjectView(R.id.drawerLayout) DrawerLayout drawerLayout;
    @InjectView(R.id.drawer) LinearLayout drawer;
    @InjectView(R.id.debugSettings) TextView debugSettings;
    @InjectView(R.id.favoriteBoardsHeader) CompatTextView favoriteBoardsHeader;
    @InjectView(R.id.favoriteBoards) ListView favoriteBoardsView;

    Menu menu;
    ActionBarDrawerToggle drawerToggle;
    FavoritesAdapter favoriteBoardsAdapter;
    FragmentManager fm;
    FragmentManager.OnBackStackChangedListener backStackChangedListener;

    boolean isToolbarShowing = true;

    @Override
    protected List<Object> getModules() {
        return Util.extendedList(super.getModules(), new UiModule(this));
    }

    @Override
    protected int getLayoutResource() { return R.layout.activity_main; }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fm = getSupportFragmentManager();

        setSupportActionBar(toolbar);
        getSupportActionBar().setHomeButtonEnabled(true);

        if (BuildConfig.DEBUG) debugSettings.setVisibility(View.VISIBLE);

        // Make the up button work as a back button
        // http://stackoverflow.com/a/24878407/283607
        backStackChangedListener = new FragmentManager.OnBackStackChangedListener() {
            @Override
            public void onBackStackChanged() {
                FragmentManager fm = getSupportFragmentManager();
                int stackHeight = fm.getBackStackEntryCount();
                if (stackHeight > 0) {
                    drawerToggle.setDrawerIndicatorEnabled(false);
                    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                } else {
                    getSupportActionBar().setDisplayHomeAsUpEnabled(false);
                    drawerToggle.setDrawerIndicatorEnabled(true);
                    drawerToggle.syncState();
                }
                // http://stackoverflow.com/a/18752763/283607
                Fragment fragment = fm.getFragments().get(stackHeight);
                if (fragment != null) fragment.onResume();
                // To remember the visibility of the toolbar
                // and rehide it on return if necessary.
                if (fragment instanceof UiFragment) {
                    UiFragment f = (UiFragment) fragment;
                    if (!f.wasToolbarShowing) hideToolbar();
                    else showToolbar();
                }
            }
        };
        fm.addOnBackStackChangedListener(backStackChangedListener);

        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        // If the drawerindicator is disabled the onOptionsItemSelected of
        // MainActivity used to be called with the old actionbar. But not so
        // with the retarted toolbar.
        drawerToggle.setToolbarNavigationClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!drawerToggle.isDrawerIndicatorEnabled()) {
                    onOptionsItemSelected(Util.mockHomeButton);
                }
            }
        });
        drawerLayout.setDrawerListener(drawerToggle);

        favoriteBoardsAdapter = new FavoritesAdapter(this, new ArrayList<>(persistentData.getFavorites()));
        favoriteBoardsView.setAdapter(favoriteBoardsAdapter);
        favoriteBoardsAdapter.notifyDataSetChanged();
        persistentData.addFavoritesChangedCallback(new PersistentData.FavoritesCallback() {
            @Override public void onChanged(Set<Board> newFavorites) {
                favoriteBoardsAdapter.updateItems(new ArrayList<>(newFavorites));
                if (newFavorites.isEmpty())
                    favoriteBoardsHeader.setVisibility(View.GONE);
                else
                    favoriteBoardsHeader.setVisibility(View.VISIBLE);
            }
        });
        favoriteBoardsView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                clearBackStackOnDrawerClick();
                Fragment f = ThreadsFragment.create(favoriteBoardsAdapter.getItem(position).name);
                fm.beginTransaction()
                        .replace(R.id.container, f, null)
                        .commit();
            }
        });

        if (persistentData.getFavorites().size() == 0) {
            favoriteBoardsHeader.setVisibility(View.GONE);
            Fragment f = new BoardsFragment();
            fm.beginTransaction()
                    .replace(R.id.container, f, null)
                    .commit();
        } else {
            favoriteBoardsHeader.setVisibility(View.VISIBLE);
            Fragment f = new FavoritesFragment();
            fm.beginTransaction()
                    .replace(R.id.container, f, null)
                    .commit();
        }
    }

    @OnClick(R.id.allBoards) void onAllBoards() {
        clearBackStackOnDrawerClick();
        Fragment f = new BoardsFragment();
        fm.beginTransaction()
                .replace(R.id.container, f, null)
                .commit();
    }

    @OnClick(R.id.favoriteBoardsBtn) void onFavoriteBoards() {
        clearBackStackOnDrawerClick();
        Fragment f = new FavoritesFragment();
        fm.beginTransaction()
                .replace(R.id.container, f, null)
                .commit();
    }

    @OnClick(R.id.settings) void onSettings() {
        Intent intent = new Intent(MainActivity.this, Settings.class);
        startActivity(intent);
    }

    @OnClick(R.id.debugSettings) void onDebugSettings() {
        Intent intent = new Intent(MainActivity.this, debugSettingsClass);
        startActivity(intent);
    }

    @OnClick(R.id.about) void onAbout() {
        SpannableString s = new SpannableString(
            "Chanobol is a fast and usable 4Chan reader inspired by Chanu.\n" +
            "Find the source code here: " +
            "https://github.com/eugenkiss/chanobol"
        );
        Linkify.addLinks(s, Linkify.ALL);

        AlertDialog d = new AlertDialog.Builder(this)
                .setTitle("About Chanobol")
                .setMessage(s)
                .setPositiveButton(android.R.string.ok, null)
                .show();

        ((TextView)d.findViewById(android.R.id.message))
                .setMovementMethod(LinkMovementMethod.getInstance());
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        drawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        drawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        this.menu = menu;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (drawerToggle.isDrawerIndicatorEnabled() &&
                drawerToggle.onOptionsItemSelected(item)) return true;
        switch (item.getItemId()) {
            case android.R.id.home:
                getSupportFragmentManager().popBackStack();
                drawerLayout.closeDrawers();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(drawer)) {
            drawerLayout.closeDrawers();
        } else if (fm.getBackStackEntryCount() == 0) {
            new AlertDialog.Builder(this)
                    .setMessage("Are you sure you want to exit?")
                    .setCancelable(false)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            finish();
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        fm.removeOnBackStackChangedListener(backStackChangedListener);
        super.onDestroy();
    }

    private static int dur = 120;

    public void showToolbar() {
        if (Build.VERSION.SDK_INT >= 14) {
            if (toolbar.getY() == -toolbar.getHeight()) {
                toolbar.animate().y(0).setDuration(dur);
                isToolbarShowing = true;
            }
        } else {
            if (toolbar.getTop() == -toolbar.getHeight()) {
                Util.animateY(toolbar, toolbar.getTop(), 0, dur);
                isToolbarShowing = true;
            }
        }
    }

    public void hideToolbar() {
        if (!prefs.getBoolean(Settings.HIDABLE_TOOLBAR, true)) return;
        if (Build.VERSION.SDK_INT >= 14) {
            if (toolbar.getY() == 0) {
                toolbar.animate().y(-toolbar.getHeight()).setDuration(dur);
                isToolbarShowing = false;
            }
        } else {
            if (toolbar.getTop() == 0) {
                Util.animateY(toolbar, 0, -toolbar.getHeight(), 120);
                isToolbarShowing = false;
            }
        }
    }

    // As per the design guidelines
    private void clearBackStackOnDrawerClick() {
        fm.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        drawerLayout.closeDrawers();
    }


    private static class FavoritesAdapter extends UiAdapter<Board> {

        public FavoritesAdapter(Context context, List<Board> items) {
            super(context);
            this.items = items;
        }

        @Override
        public View newView(LayoutInflater inflater, int position, ViewGroup container) {
            return inflater.inflate(R.layout.view_favorite, container, false);
        }

        @Override
        public void bindView(Board item, int position, View view) {
            ((FavoriteView) view).bindTo(item);
        }

        public void updateItems(List<Board> items) {
            this.items = items;
            notifyDataSetChanged();
        }
    }

    public static class FavoriteView extends LinearLayout {
        @InjectView(R.id.shortName) TextView shortName;
        @InjectView(R.id.longName) TextView longName;

        public FavoriteView(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        @Override protected void onFinishInflate() {
            super.onFinishInflate();
            ButterKnife.inject(this);
        }

        public void bindTo(Board board) {
            shortName.setText(board.name);
            longName.setText(board.title);
        }
    }
}
