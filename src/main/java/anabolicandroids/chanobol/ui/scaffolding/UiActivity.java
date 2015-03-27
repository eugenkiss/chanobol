package anabolicandroids.chanobol.ui.scaffolding;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.util.Pair;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.internal.widget.CompatTextView;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.koushikdutta.ion.Ion;
import com.nineoldandroids.view.ViewHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import anabolicandroids.chanobol.App;
import anabolicandroids.chanobol.BuildConfig;
import anabolicandroids.chanobol.R;
import anabolicandroids.chanobol.annotations.ForApplication;
import anabolicandroids.chanobol.annotations.SfwMode;
import anabolicandroids.chanobol.api.ChanService;
import anabolicandroids.chanobol.api.data.Board;
import anabolicandroids.chanobol.ui.Settings;
import anabolicandroids.chanobol.ui.boards.BoardsActivity;
import anabolicandroids.chanobol.ui.boards.FavoritesActivity;
import anabolicandroids.chanobol.ui.threads.ThreadsActivity;
import anabolicandroids.chanobol.util.BaseActivity;
import anabolicandroids.chanobol.util.BindableAdapter;
import anabolicandroids.chanobol.util.Util;
import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import butterknife.Optional;

// http://stackoverflow.com/a/19451842/283607
// http://stackoverflow.com/a/26526858/283607
public abstract class UiActivity extends BaseActivity {

    // Holy shit, why this error?: https://gist.github.com/eugenkiss/1b4864ad7373ca5ce074
    // It is impossible for me to remove this static variable even though it is not used anymore
    // Syncing, cleaning, invalidating, restarting, building on command line, nothing helps.
    public static int RIPPLE_DELAY = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ?
            250 : 50;
    // So that the user is even able to see the ripple effect before the next activity launches
    // Method instead of value because Android might static values
    public static int RIPPLE_DELAY() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ?
            250 : 50;
    }

    // Construction ////////////////////////////////////////////////////////////////////////////////

    @Inject @SfwMode boolean sfw;
    @Inject @Named("DebugSettings") Class debugSettingsClass;
    @Inject @ForApplication public Context appContext;
    @Inject public App app;
    @Inject public Resources resources;
    @Inject public Prefs prefs;
    @Inject public PersistentData persistentData;
    @Inject public ChanService service;
    @Inject public Ion ion;

    @InjectView(R.id.toolbar) public Toolbar toolbar;
    @InjectView(R.id.toolbarShadow) public ImageView toolbarShadow;
    @InjectView(R.id.loadingBar) public ProgressBar loadingBar;
    @InjectView(R.id.drawerLayout) public DrawerLayout drawerLayout;
    @InjectView(R.id.drawer) public LinearLayout drawer;
    @InjectView(R.id.allBoards) public TextView allBoards;
    @InjectView(R.id.debugSettings) public TextView debugSettings;
    @InjectView(R.id.favoriteBoardsHeader) public CompatTextView favoriteBoardsHeader;
    @InjectView(R.id.favoriteBoards) public ListView favoriteBoardsView;

    public ActionBarDrawerToggle drawerToggle;
    public FavoritesAdapter favoriteBoardsAdapter;
    // http://stackoverflow.com/q/17702202/283607
    public boolean taskRoot;

    abstract protected int getLayoutResource();

    protected View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(getLayoutResource(), container, false);
    }

    protected RecyclerView getRootRecyclerView() { return null; }

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Util.setTheme(this, prefs);
        getWindow().setBackgroundDrawableResource(prefs.theme().isLightTheme ? R.color.bgLight : R.color.bgDark);
        setContentView(R.layout.activity_ui);
        ViewGroup container = (ViewGroup) findViewById(R.id.container);
        container.addView(onCreateView(getLayoutInflater(), container, savedInstanceState));
        ButterKnife.inject(this);

        if (BuildConfig.DEBUG) debugSettings.setVisibility(View.VISIBLE);
        if (sfw) allBoards.setVisibility(View.GONE);

        setSupportActionBar(toolbar);
        setupDrawer();
        setupFavoritesInDrawer();
        RecyclerView rv = getRootRecyclerView(); if (rv != null) setupQuickReturnToolbar(rv);
        updateUpButtonState();
    }

    private void setupDrawer() {
        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerToggle.setToolbarNavigationClickListener(toolbarNavCallback);
        drawerLayout.setDrawerListener(drawerToggle);
    }

    private void setupFavoritesInDrawer() {
        favoriteBoardsAdapter = new FavoritesAdapter(this, new ArrayList<>(persistentData.getFavorites()));
        favoriteBoardsView.setAdapter(favoriteBoardsAdapter);
        favoriteBoardsAdapter.notifyDataSetChanged();
        persistentData.addFavoritesChangedCallback(favoritesChangedCallback);
        favoriteBoardsView.setOnItemClickListener(favoriteCallback);
        setVisibility(favoriteBoardsHeader, persistentData.getFavorites().size() != 0);
    }

    private void updateUpButtonState() {
        if (!taskRoot) {
            drawerToggle.setDrawerIndicatorEnabled(false);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        } else {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            drawerToggle.setDrawerIndicatorEnabled(true);
            drawerToggle.syncState();
        }
    }

    // http://www.reddit.com/r/androiddev/comments/2sqcth/play_stores_autohiding_toolbar_hows_it_implemented/
    private void setupQuickReturnToolbar(RecyclerView rv) {
        // I'd love to just use `toolbar.getHeight()` but it might not yet be known
        // at this point. Going the global layout listener route led to jumps.
        // One could combine both approaches but as long as the toolbar's height is
        // fixed and known beforehand anyway why go through the trouble...
        int actionBarHeight = Util.getActionBarHeight(this);
        rv.setPadding(
                rv.getPaddingLeft(),
                rv.getPaddingTop() + actionBarHeight,
                rv.getPaddingRight(),
                rv.getPaddingBottom());
        if (Build.VERSION.SDK_INT >= 11) {
            rv.setOnScrollListener(new RecyclerView.OnScrollListener() {
                @TargetApi(Build.VERSION_CODES.HONEYCOMB) @Override
                public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                    if (!prefs.hidableToolbar()) return;
                    float y = Util.clamp(-toolbar.getHeight(), toolbar.getTranslationY() - dy, 0);
                    toolbar.setTranslationY(y);
                    toolbarShadow.setTranslationY(y);
                }
            });
        } else {
            rv.setOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                    if (!prefs.hidableToolbar()) return;
                    float y = Util.clamp(-toolbar.getHeight(), ViewHelper.getTranslationY(toolbar) - dy, 0);
                    ViewHelper.setTranslationY(toolbar, y);
                    ViewHelper.setTranslationY(toolbarShadow, y);
                }
            });
        }
    }

    // Callbacks ///////////////////////////////////////////////////////////////////////////////////

    private View.OnClickListener toolbarNavCallback = new View.OnClickListener() {
        @Override public void onClick(View v) {
            if (!drawerToggle.isDrawerIndicatorEnabled()) {
                onBackPressed();
            }
        }
    };

    private AdapterView.OnItemClickListener favoriteCallback = new AdapterView.OnItemClickListener() {
        @Override public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            final Board board = favoriteBoardsAdapter.getItem(position);
            ThreadsActivity.launch(UiActivity.this, board);
        }
    };

    private PersistentData.FavoritesCallback favoritesChangedCallback = new PersistentData.FavoritesCallback() {
        @Override public void onChanged(Set<Board> newFavorites) {
            favoriteBoardsAdapter.updateItems(new ArrayList<>(newFavorites));
            setVisibility(favoriteBoardsHeader, !newFavorites.isEmpty());
        }
    };

    @OnClick(R.id.allBoards) void onAllBoards() {
        BoardsActivity.launch(this);
    }

    @OnClick(R.id.favoriteBoardsBtn) void onFavoriteBoards() {
        FavoritesActivity.launch(this);
    }

    @OnClick(R.id.settings) void onSettings() {
        Intent intent = new Intent(this, Settings.class);
        startActivity(intent);
    }

    @Optional @OnClick(R.id.debugSettings) void onDebugSettings() {
        if (debugSettingsClass == null) return; // see https://github.com/eugenkiss/chanobol/issues/137
        Intent intent = new Intent(this, debugSettingsClass);
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

    @Override public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(drawer)) {
            drawerLayout.closeDrawers();
        } else if (isTaskRoot() || taskRoot) {
            new AlertDialog.Builder(this)
                    .setMessage(R.string.exit_sure)
                    .setCancelable(false)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            App.firstStart = true;
                            finish();
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
        } else {
            super.onBackPressed();
        }
    }

    // Data Loading ////////////////////////////////////////////////////////////////////////////////

    protected boolean loading;

    protected void load() {
        loading = true;
        loadingBar.setVisibility(View.VISIBLE);
    }

    protected void loaded() {
        loading = false;
        loadingBar.setVisibility(View.GONE);
    }

    protected void cancelPending() {
        loading = false;
        loadingBar.setVisibility(View.GONE);
    }

    // Lifecycle ///////////////////////////////////////////////////////////////////////////////////

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

    @Override public void onDestroy() {
        cancelPending();
        // The object `persistentData` is a singleton and on the first start a callback is added
        // which closes over favoriteBoardsAdapter which itself has a necessary reference to the
        // activity. If the activity is recreated then that callback keeps a reference to the old
        // activity and it won't be collected. To prevent this memory leak remove the callback
        // when the respective activity is destroyed.
        persistentData.removeFavoritesChangedCallback(favoritesChangedCallback);
        super.onDestroy();
    }

    // Toolbar Menu ////////////////////////////////////////////////////////////////////////////////

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        if (drawerToggle.isDrawerIndicatorEnabled() &&
                drawerToggle.onOptionsItemSelected(item)) return true;
        return super.onOptionsItemSelected(item);
    }

    // Animations //////////////////////////////////////////////////////////////////////////////////

    public static int dur = 120;

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
        if (!prefs.hidableToolbar()) return;
        if (Build.VERSION.SDK_INT >= 14)
            toolbar.animate().y(-toolbar.getHeight()).setDuration(dur);
        else
            Util.animateY(toolbar, 0, -toolbar.getHeight(), dur);
    }

    // Utility /////////////////////////////////////////////////////////////////////////////////////

    public void freeMemory() {
        ion.configure().getResponseCache().clear();
        ion.getBitmapCache().clear();
        System.gc();
    }

    public void setVisibility(View view, boolean show) {
        Util.setVisibility(view, show);
    }

    @SuppressWarnings("UnusedDeclaration")
    protected void showToast(int res) { Util.showToast(appContext, res); }
    protected void showToast(String msg) { Util.showToast(appContext, msg); }

    // In order to not repeat shared status-, navigation- and toolbar.
    // http://stackoverflow.com/a/26748694/283607
    @SuppressWarnings("ConstantConditions") @SafeVarargs
    public static ActivityOptionsCompat makeSceneTransitionAnimation(Activity activity, Pair<View, String>... shared)  {
        List<Pair<View, String>> pairs = new ArrayList<>();
        // Because of the following crash on some devices I need to be extra careful:
        // "java.lang.IllegalArgumentException: Shared element must not be null"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            View v; String s;
            v = activity.findViewById(android.R.id.navigationBarBackground);
            s = Window.NAVIGATION_BAR_BACKGROUND_TRANSITION_NAME;
            if (v != null && s != null) pairs.add(Pair.create(v, s));
            v = activity.findViewById(android.R.id.statusBarBackground);
            s = Window.STATUS_BAR_BACKGROUND_TRANSITION_NAME;
            if (v != null && s != null) pairs.add(Pair.create(v, s));
            // Assumed that toolbar in layout has transitionname 'toolbar'
            v = activity.findViewById(R.id.toolbar);
            s = "toolbar";
            if (v != null && s != null) pairs.add(Pair.create(v, s));
            // The custom elements
            for (Pair<View, String> p : shared) {
                v = p.first;
                s = p.second;
                if (v != null && s != null) pairs.add(Pair.create(v, s));
            }
        }
        //noinspection unchecked
        return ActivityOptionsCompat.makeSceneTransitionAnimation(activity, pairs.toArray(new Pair[pairs.size()]));
    }

    // Adapters ////////////////////////////////////////////////////////////////////////////////////

    private static class FavoritesAdapter extends BindableAdapter<Board> {
        private List<Board> items;

        public FavoritesAdapter(Context context, List<Board> items) {
            super(context);
            this.items = items;
        }

        public void updateItems(List<Board> items) {
            this.items = items;
            notifyDataSetChanged();
        }

        @Override
        public View newView(LayoutInflater inflater, int position, ViewGroup container) {
            return inflater.inflate(R.layout.view_favorite, container, false);
        }

        @Override
        public void bindView(Board item, int position, View view) {
            ((FavoriteView) view).bindTo(item);
        }

        @Override public int getCount() { return items.size(); }
        @Override public Board getItem(int position) { return items.get(position); }
        @Override public long getItemId(int position) { return position; }
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

    // Workarounds /////////////////////////////////////////////////////////////////////////////////

    // See http://stackoverflow.com/a/27024610/283607
    @Override public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU && "LGE".equalsIgnoreCase(Build.BRAND)) {
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
    @Override public boolean onKeyUp(int keyCode, @NonNull KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU && "LGE".equalsIgnoreCase(Build.BRAND)) {
            openOptionsMenu();
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }
}
