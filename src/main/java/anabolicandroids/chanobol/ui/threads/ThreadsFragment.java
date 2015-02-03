package anabolicandroids.chanobol.ui.threads;

import android.annotation.TargetApi;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.transition.Slide;
import android.transition.TransitionManager;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.koushikdutta.async.future.FutureCallback;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.WeakHashMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import anabolicandroids.chanobol.R;
import anabolicandroids.chanobol.api.data.Board;
import anabolicandroids.chanobol.api.data.Thread;
import anabolicandroids.chanobol.ui.Settings;
import anabolicandroids.chanobol.ui.SwipeRefreshFragment;
import anabolicandroids.chanobol.ui.UiAdapter;
import anabolicandroids.chanobol.ui.boards.BoardsFragment;
import anabolicandroids.chanobol.ui.posts.PostsFragment;
import anabolicandroids.chanobol.util.Util;
import butterknife.InjectView;

public class ThreadsFragment extends SwipeRefreshFragment {

    // Callbacks ///////////////////////////////////////////////////////////////////////////////////

    View.OnClickListener clickCallback = new View.OnClickListener() {
        @Override public void onClick(View v) {
            ThreadView tv = (ThreadView) v;
            Thread thread = tv.thread;
            if (thread.dead) {
                showToast(R.string.no_thread);
                return;
            }
            pauseUpdating = true;
            Drawable d = null;
            // Why not just `b = ((BitmapDrawable) tv.image.getDrawable()).getBitmap();`
            // You see, tv.image.getDrawable is not of type BitmapDrawable but IonDrawable and
            // IonDrawable is not public so bad luck. That's the whole reason for bitMap.
            Bitmap b = bitMap.get(thread.number);
            if (b != null) {
                b = b.copy(b.getConfig(), true);
                d = new BitmapDrawable(getResources(), b);
            }
            final PostsFragment f = PostsFragment.create(board.name, thread.number);
            f.opPost = thread.toOpPost();
            f.opImage = d;
            if (transitionsAllowed()) {
                if (Build.VERSION.SDK_INT >= 21) { // To make inspection shut up
                    ThreadsFragment.this.setExitTransition(null);
                    f.setEnterTransition(inflateTransition(android.R.transition.slide_bottom));
                    f.setReturnTransition(inflateTransition(android.R.transition.fade));
                    f.setSharedElementEnterTransition(inflateTransition(android.R.transition.move));
                    f.setSharedElementReturnTransition(null);
                    f.transitionDuration = 450;
                    tv.image.setTransitionName("op");
                    startTransaction(f)
                            .addSharedElement(tv.image, "op")
                            .commit();
                }
            } else {
                startTransaction(f).commit();
            }
        }
    };

    // Construction ////////////////////////////////////////////////////////////////////////////////

    @InjectView(R.id.threads) RecyclerView threadsView;

    private Board board;
    private ArrayList<Thread> threads;
    private HashMap<String, Integer> threadMap;
    private WeakHashMap<String, Bitmap> bitMap; // To work around IonDrawable not being public
    private ThreadsAdapter threadsAdapter;

    public static ThreadsFragment create(Board board) {
        ThreadsFragment f = new ThreadsFragment();
        Bundle b = new Bundle();
        b.putParcelable("board", board);
        f.setArguments(b);
        return f;
    }

    @Override protected int getLayoutResource() { return R.layout.fragment_threads; }

    @Override
    public void onActivityCreated2(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated2(savedInstanceState);

        board = getArguments().getParcelable("board");
        threads = new ArrayList<>();
        threadMap = new HashMap<>();
        bitMap = new WeakHashMap<>();

        threadsAdapter = new ThreadsAdapter(clickCallback, null);
        threadsView.setAdapter(threadsAdapter);
        threadsView.setHasFixedSize(true);
        GridLayoutManager glm = new GridLayoutManager(context, 2);
        threadsView.setLayoutManager(glm);
        Util.calcDynamicSpanCountById(context, threadsView, glm, R.dimen.column_width);

        load();
        initBackgroundUpdater();
    }

    // Data Loading ////////////////////////////////////////////////////////////////////////////////

    @Override protected void load() {
        super.load();
        service.listThreads(this, board.name, new FutureCallback<List<Thread>>() {
            @Override public void onCompleted(Exception e, List<Thread> result) {
                if (e != null) {
                    if (e.getMessage() != null) showToast(e.getMessage());
                    System.out.println("" + e.getMessage());
                    loaded();
                    return;
                }
                boolean empty = threads.isEmpty();
                threads.clear();
                threadMap.clear();
                for (int i = 0; i < result.size(); i++) {
                    Thread thread = result.get(i);
                    threads.add(thread);
                    threadMap.put(thread.number, i);
                }
                if (empty && transitionsAllowed()) {
                    animateThreadsArrival();
                } else {
                    threadsAdapter.notifyDataSetChanged();
                }
                loaded();
                lastUpdate = System.currentTimeMillis();
            }
        });
    }

    private void update() {
        if (loading) return;
        lastUpdate = System.currentTimeMillis();
        service.listThreads(this, board.name, new FutureCallback<List<Thread>>() {
            @Override public void onCompleted(Exception e, List<Thread> result) {
                if (e != null) {
                    System.out.println("" + e.getMessage());
                    return;
                }
                if (loading) return; // cancel on real refresh
                boolean[] positions = new boolean[threads.size()];
                for (Thread thread : result) {
                    Integer position = threadMap.get(thread.number);
                    if (position != null) {
                        positions[position] = true;
                        threads.set(position, thread);
                    }
                }
                for (int i = 0; i < positions.length; i++)
                    if (!positions[i]) threads.get(i).dead = true;
                threadsAdapter.notifyDataSetChanged();
                lastUpdate = System.currentTimeMillis();
            }
        });
    }

    @Override
    protected void cancelPending() {
        super.cancelPending();
        ion.cancelAll(this);
    }

    // Lifecycle ///////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onResume() {
        super.onResume();
        activity.setTitle(board.name);
        update();
        threadsAdapter.notifyDataSetChanged();
        initBackgroundUpdater();
        pauseUpdating = false;
        setupTransitions();
    }

    @Override public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Util.updateRecyclerViewGridOnConfigChange(threadsView, R.dimen.column_width);
    }

    @Override
    public void onStop() {
        super.onStop();
        pauseUpdating = true;
    }


    // Toolbar Menu ////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.threads, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.refresh:
                load();
                break;
            case R.id.up:
                threadsView.scrollToPosition(0);
                break;
            case R.id.favorize:
                BoardsFragment.showAddFavoriteDialog(context, persistentData, board);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    // Background Refresh //////////////////////////////////////////////////////////////////////////

    // TODO: Isn't there a more elegant solution?
    long lastUpdate;
    static final long updateInterval = 30_000;
    boolean pauseUpdating = false;
    private ScheduledThreadPoolExecutor executor;

    private void initBackgroundUpdater() {
        if (executor == null) {
            executor = new ScheduledThreadPoolExecutor(1);
            executor.scheduleWithFixedDelay(new Runnable() {
                @Override
                public void run() {
                    if (!prefs.getBoolean(Settings.REFRESH, true)) return;
                    if (pauseUpdating) return;
                    if (System.currentTimeMillis() - lastUpdate > updateInterval) {
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                update();
                            }
                        });
                    }
                }
            }, 5000, 5000, TimeUnit.MILLISECONDS);
        }
    }

    // Transitions /////////////////////////////////////////////////////////////////////////////////

    @TargetApi(Build.VERSION_CODES.LOLLIPOP) private void animateThreadsArrival() {
        if (transitionsAllowed()) {
            threadsAdapter.hide = true;
            threadsAdapter.notifyDataSetChanged();
            TransitionManager.beginDelayedTransition(threadsView, new Slide(Gravity.BOTTOM));
            threadsAdapter.hide = false;
            threadsAdapter.notifyDataSetChanged();
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP) private void setupTransitions() {
        if (transitionsAllowed()) {
            setExitTransition(new Slide(Gravity.BOTTOM));
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (executor != null) executor.shutdown();
    }

    // Adapters ////////////////////////////////////////////////////////////////////////////////////

    class ThreadsAdapter extends UiAdapter<Thread> {
        boolean hide;

        public ThreadsAdapter(View.OnClickListener clickListener, View.OnLongClickListener longClickListener) {
            super(ThreadsFragment.this.context, clickListener, longClickListener);
            this.items = threads;
        }

        @Override public View newView(ViewGroup container) {
            return inflater.inflate(R.layout.view_thread, container, false);
        }

        @Override public void bindView(Thread item, int position, View view) {
            ThreadView tv = (ThreadView) view;
            tv.bindTo(item, board.name, ion, bitMap);
            if (hide) tv.setVisibility(View.GONE);
            else tv.setVisibility(View.VISIBLE);
            if (Build.VERSION.SDK_INT >= 21) {
                tv.image.setTransitionName(null);
            }
        }
    }
}

