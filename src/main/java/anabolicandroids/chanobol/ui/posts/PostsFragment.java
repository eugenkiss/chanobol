package anabolicandroids.chanobol.ui.posts;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.koushikdutta.async.future.FutureCallback;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import anabolicandroids.chanobol.R;
import anabolicandroids.chanobol.api.ApiModule;
import anabolicandroids.chanobol.api.data.Post;
import anabolicandroids.chanobol.ui.Settings;
import anabolicandroids.chanobol.ui.SwipeRefreshFragment;
import anabolicandroids.chanobol.ui.UiAdapter;
import anabolicandroids.chanobol.ui.images.ImagesFragment;
import butterknife.InjectView;

public class PostsFragment extends SwipeRefreshFragment {

    public static interface RepliesCallback {
        public void onClick(Post post);
    }
    RepliesCallback repliesCallback = new RepliesCallback() {
        @Override
        public void onClick(Post post) {
            ArrayList<Post> posts = new ArrayList<>(post.postReplies);
            for (String id : answers.get(post.id)) {
                posts.add(postsMap.get(id));
            }
            showPostsDialog(post, posts);
        }
    };

    public static interface ReferencedPostCallback {
        public void onClick(String quoterId, String quotedId);
    }
    ReferencedPostCallback referencedPostCallback = new ReferencedPostCallback() {
        @Override
        public void onClick(String quoterId, String quotedId) {
            Post quoted = postsMap.get(quotedId);
            if (quoted != null) showPostsDialog(postsMap.get(quoterId), postsMap.get(quotedId));
        }
    };

    public static interface ImageCallback {
        public void onClick(String imageIdAndExt, Drawable preview);
    }
    ImageCallback imageCallback = new ImageCallback() {
        @Override
        public void onClick(String imageIdAndExt, Drawable preview) {
            ImagesFragment f = ImagesFragment.create(board, threadId, preview, 0, Arrays.asList(imageIdAndExt));
            startFragment(f);
        }
    };

    @InjectView(R.id.posts) ListView postsView;

    Menu menu;
    Post op;
    Drawable opImage;
    String threadId;
    String board;
    ArrayList<Post> posts;
    PostsAdapter postsAdapter;
    HashMap<String, Post> postsMap;
    HashMap<String, ArrayList<String>> answers;

    // TODO: Isn't there a more elegant solution?
    long lastUpdate;
    static final long updateInterval = 60_000;
    boolean pauseUpdating = false;
    private ScheduledThreadPoolExecutor executor;

    public static PostsFragment create(String board, Post op, Drawable opImage) {
        PostsFragment f = new PostsFragment();
        f.board = board;
        f.op = op;
        f.opImage = opImage;
        f.threadId = op.id;
        return f;
    }

    @Override
    protected int getLayoutResource() {
        return R.layout.fragment_posts;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        posts = new ArrayList<>();
        postsAdapter = new PostsAdapter();
        postsView.setAdapter(postsAdapter);
        posts.add(op);
        postsAdapter.notifyDataSetChanged();
        postsMap = new HashMap<>();
        answers = new HashMap<>();

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
        activity.setTitle(board + "/" + threadId);
        if (menu != null) {
            menu.setGroupVisible(R.id.threads, false);
            menu.setGroupVisible(R.id.posts, true);
        }
        postsAdapter.notifyDataSetChanged();
        initBackgroundUpdater();
        pauseUpdating = false;
    }

    @Override public void onStop() {
        super.onStop();
        pauseUpdating = true;
    }

    @Override
    protected void load() { load(false); }

    private void load(final boolean silent) {
        if (!silent) super.load();
        service.listPosts(this, board, threadId, new FutureCallback<List<Post>>() {
            @Override public void onCompleted(Exception e, List<Post> result) {
                if (e != null) {
                    if (!silent) showToast(e.getMessage());
                    System.out.println(e.getMessage());
                    loaded();
                    return;
                }
                posts.clear();
                postsMap.clear();
                answers.clear();
                for (Post p : result) {
                    posts.add(p);
                    postsMap.put(p.id, p);
                    for (String id : referencedPosts(p)) {
                        if (!answers.containsKey(id)) answers.put(id, new ArrayList<String>());
                        Post referenced = postsMap.get(id);
                        if (referenced != null) { // e.g. stale reference to deleted post
                            answers.get(id).add(p.id);
                            referenced.postReplies++;
                        }
                    }
                    if (p.imageId != null && prefs.getBoolean(Settings.PRELOAD_THUMBNAILS, true))
                        ion.build(context).load(ApiModule.thumbUrl(board, p.imageId)).asBitmap().tryGet();
                }
                postsAdapter.notifyDataSetChanged();
                loaded();
                lastUpdate = System.currentTimeMillis();
            }
        });
    }

    private void update() {
        if (loading) return;
        lastUpdate = System.currentTimeMillis();
        load(true);
    }

    @Override
    protected void cancelPending() {
        super.cancelPending();
        ion.cancelAll(this);
    }

    @Override public void onDestroy() {
        super.onDestroy();
        if (executor != null) executor.shutdown();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.posts, menu);
        menu.setGroupVisible(R.id.threads, false);
        this.menu = menu;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.down:
                postsView.setSelection(postsAdapter.getCount()-1);
                break;
            case R.id.refresh:
                load();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showPostsDialog(Post repliedTo, List<Post> posts) {
        PostsDialog dialog = new PostsDialog();
        dialog.repliedTo = repliedTo;
        dialog.adapter = new PostsAdapter(posts);
        startFragment(dialog, PostsDialog.STACK_ID);
    }

    private void showPostsDialog(Post quotedBy, Post post) {
        PostsDialog dialog = new PostsDialog();
        dialog.quotedBy = quotedBy;
        dialog.adapter = new PostsAdapter(Arrays.asList(post));
        startFragment(dialog, PostsDialog.STACK_ID);
    }

    public static Pattern postReferencePattern = Pattern.compile("#p(\\d+)");
    // http://stackoverflow.com/a/6020436/283607
    private static LinkedHashSet<String> referencedPosts(Post post) {
        LinkedHashSet<String> refs = new LinkedHashSet<>();
        Matcher m = postReferencePattern.matcher(post.text == null ? "" : post.text);
        while (m.find()) { refs.add(m.group(1)); }
        return refs;
    }

    class PostsAdapter extends UiAdapter<Post> {
        public PostsAdapter() {
            this(posts);
        }
        public PostsAdapter(List<Post> posts) {
            super(context);
            this.items = posts;
        }

        @Override
        public View newView(LayoutInflater inflater, int position, ViewGroup container) {
            return inflater.inflate(R.layout.view_post, container, false);
        }

        @Override
        public void bindView(final Post item, int position, View view) {
            PostView v = (PostView) view;
            if (position == 0 && opImage != null) {
                v.bindToOp(opImage, item, board, threadId, ion,
                        repliesCallback, referencedPostCallback, imageCallback);
                opImage = null;
            } else {
                v.bindTo(item, board, threadId, ion,
                        repliesCallback, referencedPostCallback, imageCallback);
            }
        }
    }
}

