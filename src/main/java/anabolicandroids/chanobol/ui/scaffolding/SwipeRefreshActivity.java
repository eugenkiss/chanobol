package anabolicandroids.chanobol.ui.scaffolding;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import anabolicandroids.chanobol.R;
import anabolicandroids.chanobol.util.Util;

public abstract class SwipeRefreshActivity extends UiActivity {

    private SwipeRefreshLayout swipe;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        if (Build.VERSION.SDK_INT < 11) {
            // After the switch to Recyclerview, scrolling up always leads to a swipe
            // refresh even if you're in the middle of a list, so simply do not support
            // swipe refreshs for OS versions below a certain API level threshold.
            return view;
        }
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
        swipe.setBackgroundResource(R.color.bg); // Transition bug fix
        return swipe;
    }

    @Override
    protected void load() {
        super.load();
        if (swipe != null) swipe.setEnabled(false);
    }

    @Override
    protected void loaded() {
        super.loaded();
        if (swipe != null) {
            swipe.setEnabled(true);
            swipe.setRefreshing(false);
        }
    }
}
