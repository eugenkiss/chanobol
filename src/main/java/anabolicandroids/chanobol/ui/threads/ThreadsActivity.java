package anabolicandroids.chanobol.ui.threads;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.transition.Slide;
import android.transition.TransitionManager;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.koushikdutta.async.future.FutureCallback;

import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import anabolicandroids.chanobol.R;
import anabolicandroids.chanobol.api.data.Board;
import anabolicandroids.chanobol.api.data.Thread;
import anabolicandroids.chanobol.ui.boards.BoardsActivity;
import anabolicandroids.chanobol.ui.media.GalleryActivity;
import anabolicandroids.chanobol.ui.media.MediaPointer;
import anabolicandroids.chanobol.ui.posts.PostsActivity;
import anabolicandroids.chanobol.ui.scaffolding.SwipeRefreshActivity;
import anabolicandroids.chanobol.ui.scaffolding.UiAdapter;
import anabolicandroids.chanobol.util.Util;
import butterknife.InjectView;

public class ThreadsActivity extends SwipeRefreshActivity {

    // Construction ////////////////////////////////////////////////////////////////////////////////

    // To load the respective threads from 4Chan
    private static String EXTRA_BOARD = "board";
    private Board board;

    // Internal state
    private static String THREADS = "threads";
    private static String THREADMAP = "threadMap";
    private ArrayList<Thread> threads;
    private HashMap<String, Integer> threadMap;
    private ThreadsAdapter threadsAdapter;
    // No need to persist, is recreated on demand
    private ArrayList<Thread> sortedThreads;
    private ThreadSortOrder sortOrder;

    // This whole bitMap stuff is only needed to prevent the thumbnails from blinking after
    // update is finished. I tried many other approaches but this is the only one that worked.
    private HashMap<String, Bitmap> bitMap;

    public static void launch(Activity activity, Board board) {
        Intent intent = new Intent(activity, ThreadsActivity.class);
        intent.putExtra(EXTRA_BOARD, Parcels.wrap(board));
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        activity.startActivity(intent);
        activity.finish();
    }

    @InjectView(R.id.threads) RecyclerView threadsView;

    @Override protected int getLayoutResource() { return R.layout.activity_threads; }
    @Override protected RecyclerView getRootRecyclerView() { return threadsView; }

    @Override protected void onCreate(Bundle savedInstanceState) {
        taskRoot = true;
        super.onCreate(savedInstanceState);

        Bundle b = getIntent().getExtras();
        board = Parcels.unwrap(b.getParcelable(EXTRA_BOARD));

        setTitle(board.name);

        if (savedInstanceState == null) {
            freeMemory();
            threads = new ArrayList<>();
            threadMap = new HashMap<>();
        } else {
            threads = Parcels.unwrap(savedInstanceState.getParcelable(THREADS));
            threadMap = Parcels.unwrap(savedInstanceState.getParcelable(THREADMAP));
        }

        sortOrder = prefs.threadSortOrder();
        sortedThreads = new ArrayList<>();

        bitMap = new HashMap<>();

        threadsAdapter = new ThreadsAdapter(clickCallback, longClickCallback);
        threadsView.setAdapter(threadsAdapter);
        threadsView.setHasFixedSize(true);
        GridLayoutManager glm = new GridLayoutManager(ThreadsActivity.this, 2);
        threadsView.setLayoutManager(glm);
        Util.calcDynamicSpanCountById(ThreadsActivity.this, threadsView, glm, R.dimen.column_width);

        if (savedInstanceState == null) load(); else notifyDataSetChanged();
    }

    @Override protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(THREADS, Parcels.wrap(threads));
        outState.putParcelable(THREADMAP, Parcels.wrap(threadMap));
    }

    // Callbacks ///////////////////////////////////////////////////////////////////////////////////

    View.OnClickListener clickCallback = new View.OnClickListener() {
        @Override public void onClick(View v) {
            final ThreadView tv = (ThreadView) v;
            Thread thread = tv.thread;
            if (thread.dead) {
                showToast(R.string.no_thread);
                return;
            }
            pauseUpdating = true;
            String uuid = UUID.randomUUID().toString();
            PostsActivity.launch(
                    ThreadsActivity.this, tv.image, uuid,
                    thread.toOpPost(), board.name, thread.number
            );
        }
    };

    View.OnLongClickListener longClickCallback = new View.OnLongClickListener() {
        @Override public boolean onLongClick(View v) {
            final ThreadView tv = (ThreadView) v;
            Thread thread = tv.thread;
            if (thread.dead) {
                showToast(R.string.no_thread);
                return true;
            }
            pauseUpdating = true;
            // TODO: Shared element animation
            GalleryActivity.launch(
                    ThreadsActivity.this, board.name, thread.number, new ArrayList<MediaPointer>()
            );
            return true;
        }
    };

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
                sortedThreads.clear();
                threadMap.clear();
                bitMap.clear(); // prevent strong references to possibly stale bitmaps
                for (int i = 0; i < result.size(); i++) {
                    Thread thread = result.get(i);
                    threads.add(thread);
                    threadMap.put(thread.number, i);
                }
                if (empty) {
                    animateThreadsArrival();
                } else {
                    notifyDataSetChanged();
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
                notifyDataSetChangedWithoutBlinking();
                lastUpdate = System.currentTimeMillis();
            }
        });
    }

    @Override protected void cancelPending() {
        super.cancelPending();
        // TODO: Reenable once https://github.com/koush/ion/issues/422 is fixed
        //ion.cancelAll(this);
    }

    private void notifyDataSetChanged() {
        sortedThreads.clear();
        boolean hasSticky = threads.size() > 0 && threads.get(0).isSticky();
        for (int i = hasSticky ? 1 : 0; i < threads.size(); i++) {
            sortedThreads.add(threads.get(i));
        }
        Collections.sort(sortedThreads, new Comparator<Thread>() {
            @Override public int compare(Thread lhs, Thread rhs) {
                switch(sortOrder) {
                    case Bump: return 0;
                    case Replies: return rhs.replies - lhs.replies;
                    case Images: return rhs.images - lhs.images;
                    case Date: return rhs.time - lhs.time;
                }
                return 0;
            }
        });
        if (hasSticky) sortedThreads.add(0, threads.get(0));
        threadsAdapter.notifyDataSetChanged();
    }

    boolean onlyUpdateText;
    public void notifyDataSetChangedWithoutBlinking() {
        onlyUpdateText = true;
        notifyDataSetChanged();
        threadsView.postDelayed(new Runnable() {
            @Override public void run() {
                onlyUpdateText = false;
            }
        }, 500);
    }

    // Lifecycle ///////////////////////////////////////////////////////////////////////////////////

    @Override public void onResume() {
        super.onResume();
        // When the sort order has been changed in the settings activity and one resumes back to this activity
        if (sortOrder != prefs.threadSortOrder()) {
            sortOrder = prefs.threadSortOrder();
            notifyDataSetChanged();
        }
        update();
        initBackgroundUpdater();
        pauseUpdating = false;
    }

    @Override public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Util.updateRecyclerViewGridOnConfigChange(threadsView, R.dimen.column_width);
    }

    @Override public void onStop() {
        super.onStop();
        pauseUpdating = true;
    }

    @Override public void onDestroy() {
        if (executor != null) executor.shutdown();
        bitMap.clear();
        super.onDestroy();
    }

    // Toolbar Menu ////////////////////////////////////////////////////////////////////////////////

    @Override public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.threads, menu);
        return true;
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.refresh:
                load();
                break;
            case R.id.up:
                threadsView.scrollToPosition(0);
                break;
            case R.id.down:
                threadsView.scrollToPosition(threads.size()-1);
                break;
            case R.id.favorize:
                BoardsActivity.showAddFavoriteDialog(this, persistentData, board);
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
                    if (!prefs.autoRefresh()) return;
                    if (pauseUpdating) return;
                    if (System.currentTimeMillis() - lastUpdate > updateInterval) {
                        runOnUiThread(new Runnable() {
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

    // Animations //////////////////////////////////////////////////////////////////////////////////

    @TargetApi(Build.VERSION_CODES.LOLLIPOP) private void animateThreadsArrival() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            notifyDataSetChanged();
            return;
        }
        // Add slight delay because thumbnails must be retrieved, too,
        // and it looks bad when the thumbnails are loaded afterwards.
        threadsView.postDelayed(new Runnable() {
            @Override public void run() {
                threadsAdapter.hide = true;
                notifyDataSetChanged();
                TransitionManager.beginDelayedTransition(threadsView, new Slide(Gravity.BOTTOM));
                threadsAdapter.hide = false;
                notifyDataSetChanged();
            }
        }, 300);
    }

    // Adapters ////////////////////////////////////////////////////////////////////////////////////

    class ThreadsAdapter extends UiAdapter<Thread> {
        boolean hide = false;

        public ThreadsAdapter(View.OnClickListener clickListener, View.OnLongClickListener longClickListener) {
            super(ThreadsActivity.this, clickListener, longClickListener);
            this.items = sortedThreads;
        }

        @Override public View newView(ViewGroup container) {
            return inflater.inflate(R.layout.view_thread, container, false);
        }

        @Override public void bindView(Thread item, int position, View view) {
            ThreadView tv = (ThreadView) view;
            tv.bindTo(item, board.name, ion, onlyUpdateText, bitMap);
            setVisibility(tv, !hide);
        }
    }
}
