package anabolicandroids.chanobol.ui.posts;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.transition.Transition;
import android.transition.TransitionInflater;
import android.transition.TransitionManager;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.widget.ListView;

import anabolicandroids.chanobol.R;
import anabolicandroids.chanobol.api.data.Post;
import anabolicandroids.chanobol.ui.UiActivity;
import anabolicandroids.chanobol.util.BaseFragment;
import anabolicandroids.chanobol.util.Util;
import butterknife.InjectView;
import butterknife.OnClick;

// TODO: This one should be improved
public class PostsDialog extends BaseFragment {

    public static final String STACK_ID = "postsdialog";
    public static final int ANIM_DURATION = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ?
            500 : UiActivity.RIPPLE_DELAY;

    @InjectView(R.id.blackness) View background;
    // Why not RecyclerView? It _used to be_ RecyclerView but as the by default provided
    // Layoutmanagers do not support wrap_content and thus no easy ability to center the
    // inline posts vertically, it seemed to be the best course of action to simply go back to
    // ListView in the meantime even if it means that PostsActivity.PostsAdapter cannot be reused.
    @InjectView(R.id.posts) ListView postsView;


    Post repliedTo;
    Post quotedBy;
    PostsActivity.PostsDialogAdapter adapter;

    private boolean dismissed = false;
    @OnClick(R.id.blackness) void dismiss() {
        if (dismissed) return;
        dismissed = true;
        animatePostsRemoval();
        postsView.postDelayed(new Runnable() {
            @Override public void run() {
                getFragmentManager().popBackStack();
            }
        }, ANIM_DURATION);
    }

    @Override protected int getLayoutResource() { return R.layout.fragment_posts_dialog; }

    @Override public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (adapter != null) {
            postsView.setAdapter(adapter);
            adapter.notifyDataSetChanged();
            animatePostsArrival();
        }
    }

    @Override public void onResume() {
        super.onResume();
        adapter.notifyDataSetChanged();
        postsView.requestLayout();
        if (repliedTo != null)
            getActivity().setTitle(resources.getString(R.string.replies_to) + " " + repliedTo.number);
        else if (quotedBy != null)
            getActivity().setTitle(resources.getString(R.string.quoted_by) + " " + quotedBy.number);
    }

    @Override public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        postsView.postDelayed(new Runnable() {
            @Override public void run() {
                postsView.requestLayout();
            }
        }, 100);
    }

    // Ideally, I would use fragment manager transitions but see https://code.google.com/p/android/issues/detail?id=82832
    @TargetApi(Build.VERSION_CODES.LOLLIPOP) private void animatePostsArrival() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            postsView.setVisibility(View.INVISIBLE);
            postsView.postDelayed(new Runnable() {
                @Override public void run() {
                    revealBackground();
                    postsView.setVisibility(View.GONE);
                    Transition t = TransitionInflater.from(getActivity()).inflateTransition(R.transition.postsdialog);
                    TransitionManager.beginDelayedTransition(postsView, t);
                    postsView.setVisibility(View.VISIBLE);
                }
            }, 10);
        } else {
            background.setVisibility(View.VISIBLE);
        }
    }
    @TargetApi(Build.VERSION_CODES.LOLLIPOP) void animatePostsRemoval() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            hideBackground();
            postsView.setVisibility(View.VISIBLE);
            Transition t = TransitionInflater.from(getActivity()).inflateTransition(R.transition.postsdialog);
            TransitionManager.beginDelayedTransition(postsView, t);
            postsView.setVisibility(View.GONE);
        }
    }

    // TODO: The circle should start at the position of the click (suggests origin)
    // https://developer.android.com/training/material/animations.html#Reveal
    @TargetApi(Build.VERSION_CODES.LOLLIPOP) private void revealBackground() {
        int cx = Util.getScreenWidth(getActivity()) / 2;
        int cy = Util.getScreenHeight(getActivity());
        int finalRadius = Math.max(background.getWidth(), background.getHeight());
        Animator anim = ViewAnimationUtils.createCircularReveal(
                background, cx, cy, 0, finalRadius
        );
        anim.setDuration(ANIM_DURATION/2);
        background.setVisibility(View.VISIBLE);
        anim.start();
    }
    @TargetApi(Build.VERSION_CODES.LOLLIPOP) private void hideBackground() {
        int cx = Util.getScreenWidth(getActivity()) / 2;
        int cy = Util.getScreenHeight(getActivity());
        int initialRadius = Math.max(background.getWidth(), background.getHeight());
        Animator anim = ViewAnimationUtils.createCircularReveal(
                background, cx, cy, initialRadius, 0
        );
        anim.setDuration(ANIM_DURATION/2);
        anim.setStartDelay((int)(ANIM_DURATION/2.5));
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                if (background != null) background.setVisibility(View.INVISIBLE);
            }
        });
        anim.start();
    }
}
