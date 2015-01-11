package anabolicandroids.chanobol.ui;

import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.Toast;

import com.koushikdutta.ion.Ion;

import javax.inject.Inject;

import anabolicandroids.chanobol.BaseFragment;
import anabolicandroids.chanobol.R;
import anabolicandroids.chanobol.api.ChanService;
import butterknife.ButterKnife;

public abstract class UiFragment extends BaseFragment {
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
    protected boolean wasToolbarShowing = true;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        setRetainInstance(true);
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

        // TODO: I bet there is a better way to get an auto-hiding toolbar...
        // I mean the Play Store App can do it and do it better, i.e.
        // it hides immediately on scrolling down and does not rely on
        // the visibility of an adapterview's items.
        // http://www.techrepublic.com/article/pro-tip-maximize-android-screen-real-estate-by-showing-and-hiding-the-action-bar/
        final TypedArray styledAttributes = activity.getTheme().obtainStyledAttributes(
                new int[]{android.support.v7.appcompat.R.attr.actionBarSize});
        final int actionBarHeight = (int) styledAttributes.getDimension(0, 0);
        styledAttributes.recycle();
        rootView.setPadding(
                rootView.getPaddingLeft(),
                rootView.getPaddingTop() + actionBarHeight,
                rootView.getPaddingRight(),
                rootView.getPaddingBottom());
        if (rootView instanceof AdapterView<?>) {
            rootView.getViewTreeObserver().addOnScrollChangedListener(
                    new ViewTreeObserver.OnScrollChangedListener() {
                        int oldPos = 0;
                        public void onScrollChanged() {
                            int pos = ((AdapterView) rootView).getFirstVisiblePosition();
                            // actionBar.show()/.hide() does not animate the toolbar -> add animation manually
                            if (pos < oldPos) {
                                activity.showToolbar();
                            } else if (pos > oldPos) {
                                activity.hideToolbar();
                            }
                            oldPos = pos;
                        }
                    });
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (wasToolbarShowing) activity.showToolbar();
        else activity.hideToolbar();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        cancelPending();
    }

    // These should be used to remember the visibility of the toolbar
    // and thus rehide it on return to this fragment if necessary.
    protected void startFragment(FragmentTransaction ft) {
        // http://stackoverflow.com/a/22881361/283607
        FragmentManager fm = getFragmentManager();
        Fragment top = fm.findFragmentById(R.id.container);
        if (top == this) wasToolbarShowing = activity.isToolbarShowing;
        ft.commit();
    }
    protected void startFragment(Fragment f, String backStack) {
        FragmentTransaction ft = activity.getSupportFragmentManager().beginTransaction()
                .add(R.id.container, f, null)
                .addToBackStack(backStack);
        startFragment(ft);
    }
    protected void startFragment(Fragment f) {
        startFragment(f, null);
    }

    protected void showToast(String msg) {
        Toast.makeText(appContext, msg, Toast.LENGTH_SHORT).show();
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
