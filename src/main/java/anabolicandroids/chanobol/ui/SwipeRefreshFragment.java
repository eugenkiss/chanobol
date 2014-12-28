package anabolicandroids.chanobol.ui;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import anabolicandroids.chanobol.util.Util;
import anabolicandroids.chanobol.util.swipebottom.SwipeRefreshLayoutBottom;

public abstract class SwipeRefreshFragment extends UiFragment {

    private SwipeRefreshLayout swipe;
    private SwipeRefreshLayoutBottom swipeBottom;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        Context cxt = inflater.getContext();
        int actionbarHeight = Util.getActionBarHeight(cxt);

        swipe = new SwipeRefreshLayout(inflater.getContext());
        SwipeRefreshLayout.LayoutParams swipeParams =
                new SwipeRefreshLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT);
        swipe.setLayoutParams(swipeParams);
        swipe.addView(view);
        swipe.setProgressViewOffset(false, 0, actionbarHeight + (int) Util.dpToPx(20, cxt));
        swipe.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                load();
            }
        });
        return swipe;
    }

    @Override
    protected void load() {
        super.load();
        swipe.setEnabled(false);
    }

    @Override
    protected void loaded() {
        super.loaded();
        swipe.setEnabled(true);
        swipe.setRefreshing(false);
    }
}
