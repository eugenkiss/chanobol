package anabolicandroids.chanobol.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
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
import butterknife.InjectView;
import butterknife.Optional;

// The following resources have been helpful:
// http://stackoverflow.com/questions/17258020/switching-between-android-navigation-drawer-image-and-up-caret-when-using-fragme?lq=1
public class MainActivity extends BaseActivity {
    @Inject SharedPreferences prefs;
    @Inject PersistentData persistentData;
    @Inject @Named("DebugSettings") Class debugSettingsClass;

    @InjectView(R.id.container) RelativeLayout container;
    @InjectView(R.id.toolbar) Toolbar toolbar;
    @InjectView(R.id.loadingBar) ProgressBar loadingBar;
    @InjectView(R.id.drawerLayout) DrawerLayout drawerLayout;
    @InjectView(R.id.drawer) LinearLayout drawer;
    @InjectView(R.id.favoriteBoardsBtn) TextView favoriteBoards;
    @InjectView(R.id.allBoards) TextView allBoards;
    @InjectView(R.id.settings) TextView settings;
    @InjectView(R.id.debugSettings) @Optional TextView debugSettings;
    @InjectView(R.id.favoriteBoards) ListView favoriteBoardsView;
    ArrayAdapter<Board> favoriteBoardsAdapter;

    FragmentManager fm;
    FragmentManager.OnBackStackChangedListener backStackChangedListener;
    Menu menu;
    ActionBarDrawerToggle drawerToggle;

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
        // TODO: Doesn't work even on Nexus 7 5.0.1
        getSupportActionBar().setElevation(100);

        // Make the up button work as a back button
        // http://stackoverflow.com/a/24878407/283607
        backStackChangedListener = new FragmentManager.OnBackStackChangedListener() {
            @Override
            public void onBackStackChanged() {
                showToolbar();
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
                fragment.onResume();
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

        settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, Settings.class);
                startActivity(intent);
            }
        });
        if (BuildConfig.DEBUG) {
            debugSettings.setVisibility(View.VISIBLE);
            debugSettings.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(MainActivity.this, debugSettingsClass);
                    startActivity(intent);
                }
            });
        }

        allBoards.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearBackStackOnDrawerClick();
                Fragment f = new BoardsFragment();
                fm.beginTransaction()
                        .replace(R.id.container, f, null)
                        .commit();
            }
        });

        favoriteBoards.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearBackStackOnDrawerClick();
                Fragment f = new FavoritesFragment();
                fm.beginTransaction()
                        .replace(R.id.container, f, null)
                        .commit();
            }
        });

        favoriteBoardsAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1,
                new ArrayList<>(persistentData.getFavorites()));
        favoriteBoardsView.setAdapter(favoriteBoardsAdapter);
        favoriteBoardsAdapter.notifyDataSetChanged();
        persistentData.addFavoritesChangedCallback(new PersistentData.FavoritesCallback() {
            @Override
            public void onChanged(Set<Board> newFavorites) {
                favoriteBoardsAdapter.clear();
                for (Board f : newFavorites) favoriteBoardsAdapter.add(f);
                favoriteBoardsAdapter.notifyDataSetChanged();
            }
        });
        favoriteBoardsView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                clearBackStackOnDrawerClick();
                Fragment f = ThreadsFragment.create(favoriteBoardsAdapter.getItem(position).name);
                fm.beginTransaction()
                        .replace(R.id.container, f, null)
                        .commit();
            }
        });

        if (persistentData.getFavorites().size() == 0) {
            Fragment f = new BoardsFragment();
            fm.beginTransaction()
                    .replace(R.id.container, f, null)
                    .commit();
        } else {
            Fragment f = new FavoritesFragment();
            fm.beginTransaction()
                    .replace(R.id.container, f, null)
                    .commit();
        }
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
        if (drawerLayout.isDrawerOpen(drawer))
            drawerLayout.closeDrawers();
        else
            super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        fm.removeOnBackStackChangedListener(backStackChangedListener);
        super.onDestroy();
    }

    public void showToolbar() {
        if (toolbar.getTop() == -toolbar.getHeight()) {
            Util.animateY(toolbar, toolbar.getTop(), 0, 120);
            //Util.animatePaddingTop(container, toolbar.getHeight(), 120);
            isToolbarShowing = true;
        }
    }

    public void hideToolbar() {
        if (toolbar.getTop() == 0 && prefs.getBoolean(Settings.HIDABLE_TOOLBAR, true)) {
            Util.animateY(toolbar, 0, -toolbar.getHeight(), 120);
            //Util.animatePaddingTop(container, 0, 120);
            isToolbarShowing = false;
        }
    }

    // As per the design guidelines
    private void clearBackStackOnDrawerClick() {
        fm.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        drawerLayout.closeDrawers();
    }
}
