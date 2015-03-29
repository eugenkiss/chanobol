package anabolicandroids.chanobol.api.data;

import com.koushikdutta.async.future.FutureCallback;

import org.parceler.Parcel;
import org.parceler.Transient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import anabolicandroids.chanobol.api.ChanService;
import anabolicandroids.chanobol.ui.scaffolding.Prefs;
import anabolicandroids.chanobol.ui.threads.ThreadSortOrder;
import timber.log.Timber;

// TODO: maybe finish in order to extract loading handling from ThreadsActivity
// Handles (updating of) thread previews that are possibly from different boards (e.g. in watchlist).
@Parcel
public class ThreadPreviewsManager {

    // Union of the thread previews boards
    public TreeSet<String> boards = new TreeSet<>();
    public ArrayList<ThreadPreview> threadPreviews = new ArrayList<>();
    // Needed for updating
    public HashMap<String, Integer> threadMap = new HashMap<>();
    // Sorting related
    public ArrayList<ThreadPreview> sortedThreadPreviews = new ArrayList<>();
    public ThreadSortOrder sortOrder = ThreadSortOrder.Bump;

    public void setThreadPreviews(ArrayList<ThreadPreview> previews) {
        this.threadPreviews = previews;
        updateThreadPreviews();
    }

    private void updateThreadPreviews() {
        sort();
        threadMap.clear();
        for (int i = 0; i < threadPreviews.size(); i++) {
            ThreadPreview p = threadPreviews.get(i);
            threadMap.put(p.uid(), i);
            boards.add(p.board);
        }
    }

    // TODO: ignore stickies for watchlist
    private void sort() {
        sortedThreadPreviews.clear();
        boolean hasSticky = threadPreviews.size() > 0 && threadPreviews.get(0).isSticky();
        for (int i = hasSticky ? 1 : 0; i < threadPreviews.size(); i++) {
            sortedThreadPreviews.add(threadPreviews.get(i));
        }
        Collections.sort(sortedThreadPreviews, new Comparator<ThreadPreview>() {
            @Override public int compare(ThreadPreview lhs, ThreadPreview rhs) {
                switch (sortOrder) {
                    case Bump: return 0;
                    case Replies: return rhs.replies - lhs.replies;
                    case Images: return rhs.images - lhs.images;
                    case Date: return rhs.time - lhs.time;
                }
                return 0;
            }
        });
        if (hasSticky) sortedThreadPreviews.add(0, threadPreviews.get(0));
    }

    public static interface OnResultCallback {
        public void onSuccess(boolean last, String board);
        public void onError(boolean last, String board, String message);
    }

    public void load(ChanService service, List<String> filterIds, final OnResultCallback onResultCallback) {
        final ArrayList<ThreadPreview> accumulator = new ArrayList<>();
        final int[] counter = new int[]{boards.size()};

        for (final String board : boards) {
            service.listThreads(this, board, new FutureCallback<List<ThreadPreview>>() {
                @Override public void onCompleted(Exception e, List<ThreadPreview> result) {
                    counter[0]--;
                    boolean last = counter[0] == 0;

                    if (e != null) {
                        String m = e.getMessage(); if (m == null) m = "";
                        Timber.e(m);
                        onResultCallback.onError(last, board, m);
                        return;
                    }
                    //boolean empty = threadPreviews.isEmpty();
                    if (!last) {

                    }

                    threadPreviews.clear();
                    sortedThreadPreviews.clear();
                    threadMap.clear();
                    for (int i = 0; i < result.size(); i++) {
                        ThreadPreview threadPreview = result.get(i);
                        threadPreviews.add(threadPreview);
                        threadMap.put(threadPreview.number, i);
                    }
                    lastUpdate = System.currentTimeMillis();
                    onResultCallback.onSuccess(last, board);
                }
            });
        }
    }

    long lastUpdate;
    static final long updateInterval = 30_000;
    boolean pauseUpdating = false;
    @Transient transient ScheduledThreadPoolExecutor executor;

    public void initBackgroundUpdater(final Prefs prefs, final Runnable onUpdateCallback) {
        if (executor == null) {
            executor = new ScheduledThreadPoolExecutor(1);
            executor.scheduleWithFixedDelay(new Runnable() {
                @Override
                public void run() {
                    if (!prefs.autoRefresh()) return;
                    if (pauseUpdating) return;
                    if (System.currentTimeMillis() - lastUpdate > updateInterval) {
                        onUpdateCallback.run();
                    }
                }
            }, 5000, 5000, TimeUnit.MILLISECONDS);
        }
    }

    public void resumeBackgroundUpdating() {
        pauseUpdating = false;
    }

    public void stopBackgroundUpdating() {
        pauseUpdating = true;
    }

    public void killBackgroundUpdater() {
        if (executor != null) executor.shutdown();
        executor = null;
    }

    public void resetUpdateTimer() {
        lastUpdate = System.currentTimeMillis();
    }
}
