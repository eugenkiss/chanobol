package anabolicandroids.chanobol.ui;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.koushikdutta.ion.Ion;
import com.nineoldandroids.view.ViewHelper;

import javax.inject.Inject;

import anabolicandroids.chanobol.BaseFragment;
import anabolicandroids.chanobol.R;
import anabolicandroids.chanobol.annotations.ForActivity;
import anabolicandroids.chanobol.api.ChanService;
import anabolicandroids.chanobol.util.Util;
import butterknife.ButterKnife;

public abstract class UiFragment extends BaseFragment {
    @Inject @ForActivity public Context context;
    @Inject public MainActivity activity;
    @Inject public ChanService service;
    @Inject public Ion ion;
    @Inject public PersistentData persistentData;
    @Inject public SharedPreferences prefs;

    Toolbar toolbar;
    DrawerLayout drawer;
    // As getView apparently does not return the view returned by
    // onCreateView as suggested in the documentation but the container
    View rootView;

    protected boolean loading;
    public boolean wasToolbarShowing = true;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        setRetainInstance(true);
    }

    @Override public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        // So that menu items from fragments in the backstack disappear except
        // for the topmost i.e. most recent fragment. The reason is that fragments
        // in Chanobol own the whole screen and thus also the toolbar.
        menu.clear();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(getLayoutResource(), container, false);
        ButterKnife.inject(this, view);
        rootView = view;
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        toolbar = activity.toolbar;
        drawer = activity.drawerLayout;
        activity.showToolbar();

        // Implementation of 'Quick Return Toolbar'
        final TypedArray styledAttributes = activity.getTheme().obtainStyledAttributes(
                new int[]{android.support.v7.appcompat.R.attr.actionBarSize});
        final int actionBarHeight = (int) styledAttributes.getDimension(0, 0);
        styledAttributes.recycle();
        rootView.setPadding(
                rootView.getPaddingLeft(),
                rootView.getPaddingTop() + actionBarHeight,
                rootView.getPaddingRight(),
                rootView.getPaddingBottom());
        if (rootView instanceof RecyclerView) {
            RecyclerView rv = (RecyclerView) rootView;
            if (Build.VERSION.SDK_INT >= 11) {
                rv.setOnScrollListener(new RecyclerView.OnScrollListener() {
                    @TargetApi(Build.VERSION_CODES.HONEYCOMB) @Override
                    public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                        if (!prefs.getBoolean(Settings.HIDABLE_TOOLBAR, true)) return;
                        float y = Util.clamp(-toolbar.getHeight(), toolbar.getTranslationY() - dy, 0);
                        toolbar.setTranslationY(y);
                    }
                });
            } else {
                rv.setOnScrollListener(new RecyclerView.OnScrollListener() {
                    @Override public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                        if (!prefs.getBoolean(Settings.HIDABLE_TOOLBAR, true)) return;
                        float y = Util.clamp(-toolbar.getHeight(), ViewHelper.getTranslationY(toolbar) - dy, 0);
                        ViewHelper.setTranslationY(toolbar, y);
                    }
                });
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        cancelPending();
    }

    protected void startFragment(Fragment f, String backStack) {
        // These should be used to remember the visibility of the toolbar
        // and thus rehide it on return to this fragment if necessary.
        wasToolbarShowing = activity.isToolbarFullyShowing();
        activity.getSupportFragmentManager().beginTransaction()
                .add(R.id.container, f, null)
                .addToBackStack(backStack)
                .commit();
    }
    protected void startFragment(Fragment f) {
        startFragment(f, null);
    }

    protected void showToast(String msg) {
        Util.showToast(appContext, msg);
    }

    protected void load() {
        loading = true;
        activity.loadingBar.setVisibility(View.VISIBLE);
    }

    protected void loaded() {
        loading = false;
        activity.loadingBar.setVisibility(View.GONE);
    }

    protected void cancelPending() {
        loading = false;
        activity.loadingBar.setVisibility(View.GONE);
    }
}
