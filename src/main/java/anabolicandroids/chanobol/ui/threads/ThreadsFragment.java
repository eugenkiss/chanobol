package anabolicandroids.chanobol.ui.threads;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
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
    @InjectView(R.id.threads) RecyclerView threadsView;

    Board board;
    ArrayList<Thread> threads;
    HashMap<String, Integer> threadMap;
    WeakHashMap<String, Bitmap> bitMap;
    ThreadsAdapter threadsAdapter;

    // TODO: Isn't there a more elegant solution?
    long lastUpdate;
    static final long updateInterval = 30_000;
    boolean pauseUpdating = false;
    private ScheduledThreadPoolExecutor executor;

    public static ThreadsFragment create(Board board) {
        ThreadsFragment f = new ThreadsFragment();
        f.board = board;
        return f;
    }

    @Override
    protected int getLayoutResource() {
        return R.layout.fragment_threads;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        threads = new ArrayList<>();
        threadMap = new HashMap<>();
        bitMap = new WeakHashMap<>();

        View.OnClickListener clickListener = new View.OnClickListener() {
            @Override public void onClick(View v) {
                ThreadView tv = (ThreadView) v;
                Thread thread = tv.thread;
                if (thread.dead) {
                    showToast(R.string.no_thread);
                    return;
                }
                pauseUpdating = true;
                Bitmap b = bitMap.get(thread.id);
                Drawable d = b == null ? null : new BitmapDrawable(getResources(), b);
                PostsFragment f = PostsFragment.create(board.name, thread.toOpPost(), d);
                // Doesn't work, see: https://code.google.com/p/android/issues/detail?id=82832&thanks=82832&ts=1418767240
                //f.setEnterTransition(new Slide(Gravity.RIGHT));
                //f.setExitTransition(new Slide(Gravity.RIGHT));
                startFragment(f);
            }
        };

        threadsAdapter = new ThreadsAdapter(clickListener, null);
        threadsView.setAdapter(threadsAdapter);
        threadsView.setHasFixedSize(true);
        GridLayoutManager glm = new GridLayoutManager(context, 2);
        threadsView.setLayoutManager(glm);
        threadsView.setItemAnimator(new DefaultItemAnimator());
        Util.calcDynamicSpanCountById(context, threadsView, glm, R.dimen.column_width);

        load();
        initBackgroundUpdater();
    }

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

    @Override
    public void onResume() {
        super.onResume();
        activity.setTitle(board.name);
        update();
        initBackgroundUpdater();
        pauseUpdating = false;
    }

    @Override
    public void onStop() {
        super.onStop();
        pauseUpdating = true;
    }

    @Override
    protected void load() {
        super.load();
        service.listThreads(this, board.name, new FutureCallback<List<Thread>>() {
            @Override public void onCompleted(Exception e, List<Thread> result) {
                if (e != null) {
                    if (e.getMessage() != null) showToast(e.getMessage());
                    System.out.println("" + e.getMessage());
                    loaded();
                    return;
                }
                threads.clear();
                threadMap.clear();
                for (int i = 0; i < result.size(); i++) {
                    Thread thread = result.get(i);
                    threads.add(thread);
                    threadMap.put(thread.id, i);
                }
                threadsAdapter.notifyDataSetChanged();
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
                    Integer position = threadMap.get(thread.id);
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

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (executor != null) executor.shutdown();
    }

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

    class ThreadsAdapter extends UiAdapter<Thread> {

        public ThreadsAdapter(View.OnClickListener clickListener, View.OnLongClickListener longClickListener) {
            super(ThreadsFragment.this.context, clickListener, longClickListener);
            this.items = threads;
        }

        @Override public View newView(ViewGroup container) {
            return inflater.inflate(R.layout.view_thread, container, false);
        }

        @Override public void bindView(Thread item, int position, View view) {
            ((ThreadView) view).bindTo(item, board.name, ion, bitMap);
        }
    }
}

