package anabolicandroids.chanobol.api.data;

import android.text.Html;

import com.koushikdutta.async.future.FutureCallback;

import org.parceler.Parcel;
import org.parceler.Transient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import anabolicandroids.chanobol.api.ChanService;
import anabolicandroids.chanobol.ui.scaffolding.Prefs;
import timber.log.Timber;

@Parcel
public class Thread {

    public String boardName;
    public String threadNumber;
    public String id;

    @SuppressWarnings("UnusedDeclaration") public Thread() {}
    public Thread(String boardName, String threadNumber) {
        this.boardName = boardName;
        this.threadNumber = threadNumber;
        this.id = boardName + "/" + threadNumber;
    }

    @Override public String toString() { return id; }
    @Override public int hashCode() { return id.hashCode(); }
    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Thread t = (Thread) o;
        return t.id.equals(id);
    }

    public ArrayList<Post> posts = new ArrayList<>();
    // Map from a post number to its post POJO
    public HashMap<String, Post> postMap = new HashMap<>();
    // Map from a post number X to the post numbers of the posts that refer to X
    public HashMap<String, ArrayList<String>> replies = new HashMap<>();
    // This is provided to the gallery fragment so that it can immediately load its images without
    // waiting for the result of requesting the thread json and extracting its media references once more
    public ArrayList<MediaPointer> mediaPointers = new ArrayList<>();
    // Map from a post number to the index of its mediaPointer in mediaPointers (important for swiping)
    public HashMap<String, Integer> mediaMap = new HashMap<>();

    public boolean dead;
    public int lastVisibleIndex;

    public Post opPost() {
        if (posts.size() == 0) return null;
        return posts.get(0);
    }

    public String title(boolean gallery) {
        if (posts.size() == 0) return id;
        Post op = opPost();
        String title = "";
        if (op.subject != null && !op.subject.equals("null") && op.subject.length() > 0) title = op.subject;
        else if (op.text != null && !op.text.equals("null") && op.text.length() > 0) title = op.text;
        title = Html.fromHtml(title).toString();
        title = title.substring(0, Math.min(title.length(), 60));
        title = title.split("\\r?\\n")[0];
        if (title.length() == 0) return id;
        if (gallery) title = boardName + "/gal/" + threadNumber + " – " + title;
        else title = id + " – " + title;
        if (dead) title = "☠ " + title;
        return title;
    }
    public String title() {
        return title(false);
    }

    public static interface OnResultCallback {
        public void onSuccess();
        public void onError(String message);
    }

    public void load(ChanService service, final OnResultCallback onResultCallback) {
        service.listPosts(this, boardName, threadNumber, new FutureCallback<List<Post>>() {
            @Override public void onCompleted(Exception e, List<Post> result) {
                if (e != null) {
                    String m = e.getMessage(); if (m == null) m = "";
                    Timber.e(m);
                    onResultCallback.onError(m);
                    return;
                }
                posts.clear();
                postMap.clear();
                replies.clear();
                mediaPointers.clear();
                mediaMap.clear();
                int mediaIndex = 0;
                for (Post p : result) {
                    posts.add(p);
                    postMap.put(p.number, p);
                    for (String number : referencedPosts(p)) {
                        if (!replies.containsKey(number))
                            replies.put(number, new ArrayList<String>());
                        Post referenced = postMap.get(number);
                        if (referenced != null) { // e.g. a stale reference to deleted post could be null
                            replies.get(number).add(p.number);
                            referenced.replyCount++;
                        }
                    }
                    if (p.mediaId != null) {
                        mediaPointers.add(new MediaPointer(p, p.mediaId, p.mediaExtension, p.mediaWidth, p.mediaHeight));
                        mediaMap.put(p.number, mediaIndex);
                        mediaIndex++;
                    }
                }
                lastUpdate = System.currentTimeMillis();
                onResultCallback.onSuccess();
            }
        });
    }

    final static Pattern postReferencePattern = Pattern.compile("#p(\\d+)");
    // http://stackoverflow.com/a/6020436/283607
    private LinkedHashSet<String> referencedPosts(Post post) {
        LinkedHashSet<String> refs = new LinkedHashSet<>();
        Matcher m = postReferencePattern.matcher(post.text == null ? "" : post.text);
        while (m.find()) { refs.add(m.group(1)); }
        return refs;
    }

    long lastUpdate;
    static final long updateInterval = 60_000;
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
