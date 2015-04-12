package anabolicandroids.chanobol.ui.threads;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
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
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import anabolicandroids.chanobol.R;
import anabolicandroids.chanobol.api.data.Board;
import anabolicandroids.chanobol.api.data.Thread;
import anabolicandroids.chanobol.api.data.ThreadPreview;
import anabolicandroids.chanobol.ui.boards.BoardsActivity;
import anabolicandroids.chanobol.ui.media.GalleryActivity;
import anabolicandroids.chanobol.ui.posts.PostsActivity;
import anabolicandroids.chanobol.ui.scaffolding.PersistentData;
import anabolicandroids.chanobol.ui.scaffolding.SwipeRefreshActivity;
import anabolicandroids.chanobol.ui.scaffolding.UiAdapter;
import anabolicandroids.chanobol.util.Util;
import butterknife.InjectView;

// TODO: Maybe extract abstract class and have both ThreadsActivity and WatchlistActivity
public class ThreadsActivity extends SwipeRefreshActivity {

    // Construction ////////////////////////////////////////////////////////////////////////////////

    // To load the respective threads from 4Chan
    private static String EXTRA_BOARD = "board";
    private static String EXTRA_IS_WATCHLIST = "watchlist";
    private Board board;
    private boolean watchlist;

    // Internal state
    private static String THREADS = "threads";
    private static String THREADMAP = "threadMap";
    private static String SEARCH_QUERY = "searchQuery";
    private ArrayList<ThreadPreview> threadPreviews;
    private HashMap<String, Integer> threadMap;
    private ThreadsAdapter threadsAdapter;
    private String searchQuery;
    // No need to persist, is recreated on demand
    private ThreadSortOrder sortOrder;
    private ArrayList<ThreadPreview> sortedThreadPreviews;
    private ArrayList<ThreadPreview> filteredThreadPreviews;

    // This whole bitMap stuff is only needed to prevent the thumbnails from blinking after
    // update is finished. I tried many other approaches but this is the only one that worked.
    private HashMap<String, Bitmap> bitMap;

    private static void launch(Class activityClass, Activity callerActivity, Board board, boolean isWatchlist, boolean excludeFromRecents) {
        Intent intent = new Intent(callerActivity, activityClass);
        if (board != null) intent.putExtra(EXTRA_BOARD, Parcels.wrap(board));
        intent.putExtra(EXTRA_IS_WATCHLIST, isWatchlist);
        if (isWatchlist) intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        if (excludeFromRecents) intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        callerActivity.startActivity(intent);
    }
    public static void launch(Activity activity, Board board) {
        launch(ThreadsActivity.class, activity, board, false, false);
    }
    public static void launchForWatchlist(Activity activity, boolean excludeFromRecents) {
        launch(WatchlistThreadsActivity.class, activity, null, true, excludeFromRecents);
    }

    @InjectView(R.id.threads) RecyclerView threadsView;

    @Override protected int getLayoutResource() { return R.layout.activity_threads; }
    @Override protected RecyclerView getRootRecyclerView() { return threadsView; }

    @Override protected void onCreate(Bundle savedInstanceState) {
        Bundle b = getIntent().getExtras();
        watchlist = b.getBoolean(EXTRA_IS_WATCHLIST);
        if (watchlist) board = null;
        else board = Parcels.unwrap(b.getParcelable(EXTRA_BOARD));
        taskRoot = watchlist;

        super.onCreate(savedInstanceState);

        if (!watchlist && (board == null || board.name == null)) {
            // Get some crash reports for this: http://crashes.to/s/9eae4cc77a6
            showToast("Could not load board");
            finish();
            return;
        }

        if (savedInstanceState == null) {
            freeMemory();
            threadPreviews = new ArrayList<>();
            threadMap = new HashMap<>();
            searchQuery = "";
        } else {
            threadPreviews = Parcels.unwrap(savedInstanceState.getParcelable(THREADS));
            threadMap = Parcels.unwrap(savedInstanceState.getParcelable(THREADMAP));
            searchQuery = savedInstanceState.getString(SEARCH_QUERY);
        }

        if (watchlist) setTitle(getResources().getString(R.string.watchlist));
        else setTitle(board.name + " – " + board.title);

        sortOrder = prefs.threadSortOrder();
        sortedThreadPreviews = new ArrayList<>();
        filteredThreadPreviews = new ArrayList<>();

        bitMap = new HashMap<>();

        threadsAdapter = new ThreadsAdapter(clickCallback, longClickCallback);
        threadsView.setAdapter(threadsAdapter);
        threadsView.setHasFixedSize(true);
        GridLayoutManager glm = new GridLayoutManager(ThreadsActivity.this, 2);
        threadsView.setLayoutManager(glm);
        Util.calcDynamicSpanCountById(ThreadsActivity.this, threadsView, glm, R.dimen.column_width);

        if (watchlist) {
            if (swipe != null) swipe.setEnabled(false); // null-check due to: http://crashes.to/s/172ca87ecb8 and http://crashes.to/s/9ef45e4e36b
            persistentData.addWatchlistChangedCallback(watchlistChangedCallback);
            if (savedInstanceState == null) loadWatchlistThreadsFromStorage();
            else notifyDataSetChanged();
        } else {
            if (savedInstanceState == null) load(); else notifyDataSetChanged();
        }
    }

    private void loadWatchlistThreadsFromStorage() {
        // TODO: Load asynchronously in background
        threadPreviews.clear();
        threadMap.clear();
        Set<PersistentData.WatchlistEntry> watchlistEntries = persistentData.getWatchlist();
        int i = 0;
        for (PersistentData.WatchlistEntry e : watchlistEntries) {
            Thread t = persistentData.getWatchlistThread(e.id);
            if (t == null || t.opPost() == null) continue;
            ThreadPreview p = t.opPost().toThreadPreview();
            p.dead = t.dead;
            threadPreviews.add(p);
            threadMap.put(p.number, i);
            i++;
        }
        notifyDataSetChanged();
    }

    @Override protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(THREADS, Parcels.wrap(threadPreviews));
        outState.putParcelable(THREADMAP, Parcels.wrap(threadMap));
        outState.putString(SEARCH_QUERY, searchQuery);
    }

    // Callbacks ///////////////////////////////////////////////////////////////////////////////////

    private View.OnClickListener clickCallback = new View.OnClickListener() {
        @Override public void onClick(View v) {
            final ThreadView tv = (ThreadView) v;
            ThreadPreview threadPreview = tv.threadPreview;
            if (threadPreview.dead && !watchlist) {
                showToast(R.string.no_thread);
                return;
            }
            pauseUpdating = true;
            String uuid = UUID.randomUUID().toString();
            pauseUpdating = true; // To prevent update during transition
            PostsActivity.launch(
                    ThreadsActivity.this, tv.image, uuid,
                    threadPreview.toOpPost(), threadPreview.board, threadPreview.number
            );
        }
    };

    private View.OnLongClickListener longClickCallback = new View.OnLongClickListener() {
        @Override public boolean onLongClick(View v) {
            final ThreadView tv = (ThreadView) v;
            ThreadPreview threadPreview = tv.threadPreview;
            if (threadPreview.dead) {
                showToast(R.string.no_thread);
                return true;
            }
            pauseUpdating = true;
            // TODO: Shared element animation
            pauseUpdating = true; // To prevent update during transition
            GalleryActivity.launchFromCatalog(ThreadsActivity.this, threadPreview.toThread());
            return true;
        }
    };

    private PersistentData.WatchlistCallback watchlistChangedCallback = new PersistentData.WatchlistCallback() {
        @Override public void onChanged(Set<PersistentData.WatchlistEntry> newWatchlist) {
            loadWatchlistThreadsFromStorage();
        }
    };

    private void setSearchCallback(final SearchView searchView) {
        if (searchQuery != null && searchQuery.length() > 0) {
            searchView.setIconified(false);
            searchView.setQuery(searchQuery, false);
            searchView.clearFocus();
        }
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String query) {
                searchView.clearFocus();
                return true;
            }
            @Override public boolean onQueryTextChange(String query) {
                searchQuery = query;
                notifyDataSetChanged();
                return true;
            }
        });
    }

    // Data Loading ////////////////////////////////////////////////////////////////////////////////

    @Override protected void load() {
        if (watchlist) return;
        super.load();
        service.listThreads(this, board.name, new FutureCallback<List<ThreadPreview>>() {
            @Override public void onCompleted(Exception e, List<ThreadPreview> result) {
                if (e != null) {
                    if (e.getMessage() != null) showToast(e.getMessage());
                    System.out.println("" + e.getMessage());
                    loaded();
                    return;
                }
                boolean empty = threadPreviews.isEmpty();
                threadPreviews.clear();
                sortedThreadPreviews.clear();
                threadMap.clear();
                bitMap.clear(); // prevent strong references to possibly stale bitmaps
                for (int i = 0; i < result.size(); i++) {
                    ThreadPreview threadPreview = result.get(i);
                    threadPreviews.add(threadPreview);
                    threadMap.put(threadPreview.number, i);
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
        if (watchlist) return;
        if (loading) return;
        lastUpdate = System.currentTimeMillis();
        service.listThreads(this, board.name, new FutureCallback<List<ThreadPreview>>() {
            @Override public void onCompleted(Exception e, List<ThreadPreview> result) {
                if (e != null) {
                    System.out.println("" + e.getMessage());
                    return;
                }
                if (loading) return; // cancel on real refresh
                boolean[] positions = new boolean[threadPreviews.size()];
                for (ThreadPreview threadPreview : result) {
                    Integer position = threadMap.get(threadPreview.number);
                    if (position != null) {
                        positions[position] = true;
                        threadPreviews.set(position, threadPreview);
                    }
                }
                for (int i = 0; i < positions.length; i++)
                    if (!positions[i]) threadPreviews.get(i).dead = true;
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
        sortedThreadPreviews.clear();
        boolean hasSticky = threadPreviews.size() > 0 && threadPreviews.get(0).isSticky();
        for (int i = hasSticky ? 1 : 0; i < threadPreviews.size(); i++) {
            sortedThreadPreviews.add(threadPreviews.get(i));
        }
        Collections.sort(sortedThreadPreviews, new Comparator<ThreadPreview>() {
            @Override public int compare(ThreadPreview lhs, ThreadPreview rhs) {
                switch(sortOrder) {
                    case Bump: return 0;
                    case Replies: return rhs.replies - lhs.replies;
                    case Images: return rhs.images - lhs.images;
                    case Date: return rhs.time - lhs.time;
                }
                return 0;
            }
        });
        if (hasSticky) sortedThreadPreviews.add(0, threadPreviews.get(0));

        filteredThreadPreviews.clear();
        for (ThreadPreview t : sortedThreadPreviews) {
            if (t.excerpt.toLowerCase().contains(searchQuery))
                filteredThreadPreviews.add(t);
        }

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

    @Override public void onPause() {
        super.onPause();
        pauseUpdating = true;
    }

    @Override public void onDestroy() {
        if (executor != null) executor.shutdown();
        bitMap.clear();
        persistentData.removeWatchlistChangedCallback(watchlistChangedCallback);
        super.onDestroy();
    }

    // Toolbar Menu ////////////////////////////////////////////////////////////////////////////////

    @Override public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.threads, menu);
        if (watchlist) {
            menu.findItem(R.id.favorize).setVisible(false);
            menu.findItem(R.id.refresh).setVisible(false);
        }

        final MenuItem searchItem = menu.findItem(R.id.search);
        SearchView searchView = null;
        if (searchItem != null) searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        if (searchView != null) setSearchCallback(searchView);

        return super.onCreateOptionsMenu(menu);
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
                threadsView.scrollToPosition(threadPreviews.size()-1);
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

    class ThreadsAdapter extends UiAdapter<ThreadPreview> {
        boolean hide = false;

        public ThreadsAdapter(View.OnClickListener clickListener, View.OnLongClickListener longClickListener) {
            super(ThreadsActivity.this, clickListener, longClickListener);
            this.items = filteredThreadPreviews;
        }

        @Override public View newView(ViewGroup container) {
            return inflater.inflate(R.layout.view_thread, container, false);
        }

        @Override public void bindView(ThreadPreview item, int position, View view) {
            ThreadView tv = (ThreadView) view;
            tv.bindTo(item, item.board, ion, onlyUpdateText, watchlist, bitMap);
            setVisibility(tv, !hide);
        }
    }

}
