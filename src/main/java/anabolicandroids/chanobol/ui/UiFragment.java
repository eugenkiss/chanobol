package anabolicandroids.chanobol.ui;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.transition.Transition;
import android.transition.TransitionInflater;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

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
    @Inject public Resources resources;
    @Inject public ChanService service;
    @Inject public Ion ion;
    @Inject public PersistentData persistentData;
    @Inject public SharedPreferences prefs;

    public Toolbar toolbar;
    public ImageView toolbarShadow;
    DrawerLayout drawer;
    // As getView apparently does not return the view returned by
    // onCreateView as suggested in the documentation but the container
    View rootView;

    protected boolean loading;
    // To remember the toolbar's position for this fragment when another get's on top
    // of the stack so that when this one's back on top it'll have the toolbar just
    // where it was before.
    float toolbarPosition = 0;

    // Workaround for fragment transition bug
    // see https://code.google.com/p/android/issues/detail?id=82832#c4
    // see http://stackoverflow.com/questions/11353075/how-can-i-maintain-fragment-state-when-added-to-the-back-stack/23533575#23533575
    boolean alreadyCreated;

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
        if (rootView != null) {
            ((ViewGroup)rootView.getParent()).removeView(rootView);
            return rootView;
        }
        View view = inflater.inflate(getLayoutResource(), container, false);
        ButterKnife.inject(this, view);
        rootView = view;
        return view;
    }

    protected boolean shouldAddPaddingForToolbar() { return true; }

    protected void onActivityCreated2(Bundle savedInstanceState) {
        toolbar = activity.toolbar;
        toolbarShadow = activity.toolbarShadow;
        drawer = activity.drawerLayout;
        activity.showToolbar();

        // Implementation of 'Quick Return Toolbar'
        if (shouldAddPaddingForToolbar()) {
            // I'd love to just use `toolbar.getHeight()` but it might not yet be known
            // at this point. Going the global layout listener route led to jumps.
            // One could combine both approaches but as long as the toolbar's height is
            // fixed and known beforehand anyway why go through the trouble...
            int actionBarHeight = Util.getActionBarHeight(context);
            rootView.setPadding(
                    rootView.getPaddingLeft(),
                    rootView.getPaddingTop() + actionBarHeight,
                    rootView.getPaddingRight(),
                    rootView.getPaddingBottom());
        }
        if (rootView instanceof RecyclerView) {
            RecyclerView rv = (RecyclerView) rootView;
            if (Build.VERSION.SDK_INT >= 11) {
                rv.setOnScrollListener(new RecyclerView.OnScrollListener() {
                    @TargetApi(Build.VERSION_CODES.HONEYCOMB) @Override
                    public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                        if (!prefs.getBoolean(Settings.HIDABLE_TOOLBAR, true)) return;
                        float y = Util.clamp(-toolbar.getHeight(), toolbar.getTranslationY() - dy, 0);
                        toolbar.setTranslationY(y);
                        toolbarShadow.setTranslationY(y);
                    }
                });
            } else {
                rv.setOnScrollListener(new RecyclerView.OnScrollListener() {
                    @Override public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                        if (!prefs.getBoolean(Settings.HIDABLE_TOOLBAR, true)) return;
                        float y = Util.clamp(-toolbar.getHeight(), ViewHelper.getTranslationY(toolbar) - dy, 0);
                        ViewHelper.setTranslationY(toolbar, y);
                        ViewHelper.setTranslationY(toolbarShadow, y);
                    }
                });
            }
        }
    }

    @Override
    public final void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (alreadyCreated) return;
        alreadyCreated = true;

        onActivityCreated2(savedInstanceState);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        cancelPending();
    }

    protected FragmentTransaction startTransaction(Fragment f, String backStack) {
        toolbarPosition = ViewHelper.getTranslationY(toolbar);
        return activity.getSupportFragmentManager().beginTransaction()
                .replace(R.id.container, f, null)
                .addToBackStack(backStack);
    }
    protected FragmentTransaction startTransaction(Fragment f) {
        return startTransaction(f, null);
    }
    protected FragmentTransaction startAddTransaction(Fragment f, String backStack) {
        toolbarPosition = ViewHelper.getTranslationY(toolbar);
        return activity.getSupportFragmentManager().beginTransaction()
                .add(R.id.container, f, null)
                .addToBackStack(backStack);
    }

    protected void showToast(String msg) {
        Util.showToast(appContext, msg);
    }

    protected void showToast(int res) {
        Util.showToast(appContext, res);
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

    protected Transition inflateTransition(int id) {
        return TransitionInflater.from(activity).inflateTransition(id);
    }

    protected boolean transitionsAllowed() {
        return activity.transitionsAllowed();
    }

    protected boolean onBackPressed() { return false; }
}
