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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.nineoldandroids.view.ViewHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import anabolicandroids.chanobol.App;
import anabolicandroids.chanobol.BaseActivity;
import anabolicandroids.chanobol.BindableAdapter;
import anabolicandroids.chanobol.BuildConfig;
import anabolicandroids.chanobol.R;
import anabolicandroids.chanobol.api.data.Board;
import anabolicandroids.chanobol.ui.boards.BoardsFragment;
import anabolicandroids.chanobol.ui.boards.FavoritesFragment;
import anabolicandroids.chanobol.ui.posts.PostsDialog;
import anabolicandroids.chanobol.ui.posts.PostsFragment;
import anabolicandroids.chanobol.ui.threads.ThreadsFragment;
import anabolicandroids.chanobol.util.Util;
import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

// The following resources have been helpful:
// http://stackoverflow.com/questions/17258020/switching-between-android-navigation-drawer-image-and-up-caret-when-using-fragme?lq=1
public class MainActivity extends BaseActivity {

    // Callbacks ///////////////////////////////////////////////////////////////////////////////////

    boolean wasPreviousFragmentPostsDialog;
    // Make the up button work as a back button
    // http://stackoverflow.com/a/24878407/283607
    private FragmentManager.OnBackStackChangedListener backStackChangedListener =
            new FragmentManager.OnBackStackChangedListener() {
        @Override
        public void onBackStackChanged() {
            updateUpButtonState();
            // http://stackoverflow.com/a/18752763/283607
            int stackHeight = fm.getBackStackEntryCount();
            Fragment fragment = fm.getFragments().get(stackHeight);
            // To remember the visibility of the toolbar and reset it on return if necessary.
            if (fragment instanceof UiFragment && prefs.getBoolean(Settings.HIDABLE_TOOLBAR, true)) {
                UiFragment f = (UiFragment) fragment;
                if (Build.VERSION.SDK_INT >= 14) {
                    toolbar.animate().y(f.toolbarPosition).setDuration(dur);
                    toolbarShadow.animate().y(f.toolbarPosition).setDuration(dur);
                } else {
                    ViewHelper.setTranslationY(toolbar, f.toolbarPosition);
                    ViewHelper.setTranslationY(toolbarShadow, f.toolbarPosition);
                }
            }
            // Horrible fix to force onResume on PostsFragment when last PostsDialog was popped off
            // These problems are a result of this peculiar constraint:
            // wanting transitions, working with fragments and having a bug in Android...
            if (fragment instanceof PostsFragment && wasPreviousFragmentPostsDialog) {
                fragment.onResume();
            }
            // Horrible fix to force onResume on PostsDialog when e.g. coming from ImageFragment
            if (fragment instanceof PostsDialog && !wasPreviousFragmentPostsDialog) {
                fragment.onResume();
            }
            wasPreviousFragmentPostsDialog = fragment instanceof PostsDialog;
        }
    };

    private View.OnClickListener toolbarNavCallback = new View.OnClickListener() {
        @Override public void onClick(View v) {
            if (!drawerToggle.isDrawerIndicatorEnabled()) {
                onBackPressed();
            }
        }
    };

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
        SpannableString s = new SpannableString(getResources().getString(R.string.about_app));
        Linkify.addLinks(s, Linkify.ALL);

        AlertDialog d = new AlertDialog.Builder(this)
                .setTitle(R.string.about_app_title)
                .setMessage(s)
                .setPositiveButton(android.R.string.ok, null)
                .show();

        ((TextView)d.findViewById(android.R.id.message))
                .setMovementMethod(LinkMovementMethod.getInstance());
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(drawer)) {
            drawerLayout.closeDrawers();
        } else if (fm.getBackStackEntryCount() <= 1) {
            new AlertDialog.Builder(this)
                    .setMessage(R.string.exit_sure)
                    .setCancelable(false)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            finish();
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
        } else {
            Fragment top = fm.getFragments().get(fm.getBackStackEntryCount());
            if (top instanceof UiFragment) {
                if(((UiFragment) top).onBackPressed()) return;
            }
            super.onBackPressed();
        }
    }

    // Construction ////////////////////////////////////////////////////////////////////////////////

    @Inject SharedPreferences prefs;
    @Inject PersistentData persistentData;
    @Inject @Named("DebugSettings") Class debugSettingsClass;

    @InjectView(R.id.toolbar) Toolbar toolbar;
    @InjectView(R.id.toolbarShadow) ImageView toolbarShadow;
    @InjectView(R.id.loadingBar) public ProgressBar loadingBar;
    @InjectView(R.id.drawerLayout) DrawerLayout drawerLayout;
    @InjectView(R.id.drawer) LinearLayout drawer;
    @InjectView(R.id.debugSettings) TextView debugSettings;
    @InjectView(R.id.favoriteBoardsHeader) CompatTextView favoriteBoardsHeader;
    @InjectView(R.id.favoriteBoards) ListView favoriteBoardsView;

    ActionBarDrawerToggle drawerToggle;
    FavoritesAdapter favoriteBoardsAdapter;
    FragmentManager fm;

    @Override protected List<Object> getModules() {
        return Util.extendedList(super.getModules(), new UiModule(this));
    }

    @Override protected int getLayoutResource() { return R.layout.activity_main; }

    // I don't like that the order of statements is so important here to guarantee proper initializiation
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setSupportActionBar(toolbar);
        fm = getSupportFragmentManager();
        if (BuildConfig.DEBUG) debugSettings.setVisibility(View.VISIBLE);

        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerToggle.setToolbarNavigationClickListener(toolbarNavCallback);
        drawerLayout.setDrawerListener(drawerToggle);

        favoriteBoardsAdapter = new FavoritesAdapter(this, new ArrayList<>(persistentData.getFavorites()));
        favoriteBoardsView.setAdapter(favoriteBoardsAdapter);
        favoriteBoardsAdapter.notifyDataSetChanged();
        // Shit, I think here we have a memory leak. persistentData is a singleton and on the first
        // start a callback is added which closes over favoriteBoardsAdapter which itself has a
        // necessary reference to the activity. If the activity is recreated then that callback
        // keeps a reference to the old activity and it won't be collected. One solution is to
        // keep a weak reference in favoriteBoardsAdapter to the activity. But test it with the
        // Memory Monitor.
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
                Fragment f = ThreadsFragment.create(favoriteBoardsAdapter.getItem(position));
                fm.beginTransaction()
                        .replace(R.id.container, f, null)
                        .commit();
            }
        });

        if (savedInstanceState == null) {
            addDummyFragment();
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
        fm.addOnBackStackChangedListener(backStackChangedListener);
        updateUpButtonState();
    }

    // Lifecycle ///////////////////////////////////////////////////////////////////////////////////

    @Override protected void onDestroy() {
        fm.removeOnBackStackChangedListener(backStackChangedListener);
        super.onDestroy();
    }

    @Override protected void onResume() {
        super.onResume();
        if (App.needToProtractToolbar) {
            showToolbar();
            App.needToProtractToolbar = false;
        }
    }

    @Override protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        drawerToggle.syncState();
    }

    @Override public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        drawerToggle.onConfigurationChanged(newConfig);
    }

    // Toolbar Menu ////////////////////////////////////////////////////////////////////////////////

    @Override public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        if (drawerToggle.isDrawerIndicatorEnabled() &&
                drawerToggle.onOptionsItemSelected(item)) return true;
        return super.onOptionsItemSelected(item);
    }

    // Transitions /////////////////////////////////////////////////////////////////////////////////

    private static int dur = 120;

    public void showToolbar() {
        if (Build.VERSION.SDK_INT >= 14) {
            toolbar.animate().y(0).setDuration(dur);
            toolbarShadow.animate().y(0).setDuration(dur);
        }
        else {
            Util.animateY(toolbar, toolbar.getTop(), 0, dur);
            Util.animateY(toolbarShadow, toolbarShadow.getTop(), 0, dur);
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    public void hideToolbar() {
        if (!prefs.getBoolean(Settings.HIDABLE_TOOLBAR, true)) return;
        if (Build.VERSION.SDK_INT >= 14)
            toolbar.animate().y(-toolbar.getHeight()).setDuration(dur);
        else
            Util.animateY(toolbar, 0, -toolbar.getHeight(), dur);
    }

    public boolean transitionsAllowed() {
        return Build.VERSION.SDK_INT >= 21 && prefs.getBoolean(Settings.TRANSITIONS, true);
    }

    // Utility /////////////////////////////////////////////////////////////////////////////////////

    // As per the design guidelines
    private void clearBackStackOnDrawerClick() {
        fm.popBackStackImmediate("dummy", 0);
        drawerLayout.closeDrawers();
    }

    private void addDummyFragment() {
        // Workaround to fragment transition bug, see: https://code.google.com/p/android/issues/detail?id=82832#c4
        fm.beginTransaction()
                .add(R.id.container, new Fragment())
                .addToBackStack("dummy")
                .commit();
        fm.executePendingTransactions();
    }

    private void updateUpButtonState() {
        int stackHeight = fm.getBackStackEntryCount();
        if (stackHeight > 1) {
            drawerToggle.setDrawerIndicatorEnabled(false);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        } else {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            drawerToggle.setDrawerIndicatorEnabled(true);
            drawerToggle.syncState();
        }
    }

    // Adapters ////////////////////////////////////////////////////////////////////////////////////

    private static class FavoritesAdapter extends BindableAdapter<Board> {
        private List<Board> items;

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

        @Override
        public int getCount() {
            return items.size();
        }

        @Override
        public Board getItem(int position) {
            return items.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
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
