package anabolicandroids.chanobol.ui.posts;

import android.annotation.TargetApi;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.transition.Slide;
import android.transition.Transition;
import android.transition.TransitionManager;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.koushikdutta.async.future.FutureCallback;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import anabolicandroids.chanobol.BindableAdapter;
import anabolicandroids.chanobol.R;
import anabolicandroids.chanobol.api.ApiModule;
import anabolicandroids.chanobol.api.data.Post;
import anabolicandroids.chanobol.ui.Settings;
import anabolicandroids.chanobol.ui.SwipeRefreshFragment;
import anabolicandroids.chanobol.ui.UiAdapter;
import anabolicandroids.chanobol.ui.images.GalleryFragment;
import anabolicandroids.chanobol.ui.images.ImageFragment;
import anabolicandroids.chanobol.ui.images.ImgIdExt;
import anabolicandroids.chanobol.util.Util;
import butterknife.InjectView;

public class PostsFragment extends SwipeRefreshFragment {

    // Callbacks ///////////////////////////////////////////////////////////////////////////////////

    public static interface RepliesCallback {
        public void onClick(Post post);
    }
    RepliesCallback repliesCallback = new RepliesCallback() {
        @Override
        public void onClick(Post post) {
            ArrayList<Post> posts = new ArrayList<>(post.replyCount);
            for (String id : replies.get(post.number)) {
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
        public void onClick(ImgIdExt imageIdAndExt, Drawable preview, ImageView iv, boolean thumbnail);
    }
    ImageCallback imageCallback = new ImageCallback() {
        @Override public void onClick(ImgIdExt imageIdAndExt, Drawable preview, ImageView iv, boolean thumbnail) {
            ImageFragment f = ImageFragment.create(boardName, threadNumber,
                                                   0, Util.arrayListOf(imageIdAndExt));
            f.preview = preview;
            if (transitionsAllowed() && !isTopFragmentPostsDialog()) {
                if (Build.VERSION.SDK_INT >= 21) { // To make inspection shut up
                    f.setEnterTransition(inflateTransition(android.R.transition.fade));
                    f.setReturnTransition(inflateTransition(android.R.transition.fade));
                    f.setSharedElementEnterTransition(inflateTransition(android.R.transition.move));
                    f.setSharedElementReturnTransition(inflateTransition(android.R.transition.move));
                    String uuid = UUID.randomUUID().toString();
                    iv.setTransitionName(uuid);
                    f.transitionName = uuid;
                    f.transitionDuration = 450;
                    f.thumbnailPreview = thumbnail;
                    f.postIv = iv;
                    startTransaction(f)
                            .addSharedElement(iv, uuid)
                            .commit();
                }
            } else if (transitionsAllowed()) {
                startAddTransaction(f, null).commit();
            } else {
                startTransaction(f).commit();
            }
        }
    };

    // Construction ////////////////////////////////////////////////////////////////////////////////

    @InjectView(R.id.posts) RecyclerView postsView;

    // Transient state - only necessary for transition and "illusion" of promptness
    public Post opPost; // To make first post instantly visible
    public Drawable opImage; // To make image of first post instantly visible

    // To load the respective posts from 4Chan
    private String threadNumber;
    private String boardName;

    private ArrayList<Post> posts;
    private PostsAdapter postsAdapter;
    // Map from a post number to its post POJO
    private HashMap<String, Post> postsMap;
    // Map from a post number X to the post numbers of the posts that refer to X
    private HashMap<String, ArrayList<String>> replies;
    // This is provided to the gallery fragment so that it can immediately load its images without
    // waiting for the result of requesting the thread json and extracting its image references once more
    private ArrayList<ImgIdExt> imagePointers;

    public static PostsFragment create(String boardName, String threadNumber) {
        PostsFragment f = new PostsFragment();
        Bundle b = new Bundle();
        b.putString("boardName", boardName);
        b.putString("threadNumber", threadNumber);
        f.setArguments(b);
        return f;
    }

    @Override protected int getLayoutResource() { return R.layout.fragment_posts; }

    @Override public void onActivityCreated2(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated2(savedInstanceState);

        firstLoad = transitionsAllowed();

        Bundle b = getArguments();
        boardName = b.getString("boardName");
        threadNumber = b.getString("threadNumber");

        posts = new ArrayList<>();
        postsMap = new HashMap<>();
        replies = new HashMap<>();
        imagePointers = new ArrayList<>();

        postsAdapter = new PostsAdapter();
        postsView.setAdapter(postsAdapter);
        postsView.setLayoutManager(new LinearLayoutManager(context));
        postsView.addItemDecoration(new SpacesItemDecoration((int) resources.getDimension(R.dimen.post_spacing)));

        if (opPost != null) {
            posts.add(opPost);
            postsAdapter.notifyItemChanged(0);
        }

        load();
        initBackgroundUpdater();
    }

    // Data Loading ////////////////////////////////////////////////////////////////////////////////

    @Override protected void load() { load(false); }

    private void load(final boolean silent) {
        super.load();
        if (silent || firstLoad) activity.loadingBar.setVisibility(View.GONE);

        // Indicate ongoing request after all if first load takes especially long
        if (firstLoad) {
            postsView.postDelayed(new Runnable() {
                @Override public void run() {
                    if (firstLoad && loading) {
                        activity.loadingBar.setVisibility(View.VISIBLE);
                    }
                }
            }, transitionDuration + 100);
        }

        final long start = System.currentTimeMillis();
        service.listPosts(this, boardName, threadNumber, new FutureCallback<List<Post>>() {
            @Override public void onCompleted(Exception e, List<Post> result) {
                if (e != null) {
                    if (!silent && e.getMessage() != null) showToast(e.getMessage());
                    System.out.println("" + e.getMessage());
                    loaded();
                    return;
                }
                posts.clear();
                postsMap.clear();
                replies.clear();
                imagePointers.clear();
                for (Post p : result) {
                    posts.add(p);
                    postsMap.put(p.number, p);
                    for (String number : referencedPosts(p)) {
                        if (!replies.containsKey(number)) replies.put(number, new ArrayList<String>());
                        Post referenced = postsMap.get(number);
                        if (referenced != null) { // e.g. a stale reference to deleted post could be null
                            replies.get(number).add(p.number);
                            referenced.replyCount++;
                        }
                    }
                    if (p.imageId != null) {
                        if (prefs.getBoolean(Settings.PRELOAD_THUMBNAILS, true))
                            ion.build(context).load(ApiModule.thumbUrl(boardName, p.imageId)).asBitmap().tryGet();
                        imagePointers.add(new ImgIdExt(p.imageId, p.imageExtension));
                    }
                }
                lastUpdate = System.currentTimeMillis();
                if (firstLoad) {
                    long elapsed = System.currentTimeMillis() - start;
                    postsView.postDelayed(new Runnable() {
                        @Override public void run() {
                            firstLoad = false;
                            if (posts.size() > 1) animatePostsArrival();
                            loaded();
                        }
                    }, Math.max(0, transitionDuration - elapsed));
                } else {
                    postsAdapter.notifyDataSetChanged();
                    loaded();
                }

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

    // Lifecycle ///////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onResume() {
        super.onResume();
        activity.setTitle(boardName + "/" + threadNumber);
        if (!firstLoad) {
            // Somehow breaks shared element return transition
            // But important such that e.g. gifs restart playing when coming back from image fragment
            if (!transitionsAllowed()) {
                postsAdapter.notifyDataSetChanged();
                postsView.requestLayout();
            } else {
                postsView.postDelayed(new Runnable() {
                    @Override public void run() {
                        postsAdapter.notifyDataSetChanged();
                        postsView.requestLayout();
                    }
                }, 450);
            }
        }
        initBackgroundUpdater();
        pauseUpdating = false;
    }

    @Override public void onStop() {
        super.onStop();
        pauseUpdating = true;
    }

    @Override public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        postsView.postDelayed(new Runnable() {
            @Override public void run() {
                postsView.requestLayout();
            }
        }, 100);
    }


    @Override public void onDestroy() {
        super.onDestroy();
        if (executor != null) executor.shutdown();
    }

    // Toolbar Menu ////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.posts, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.down:
                postsView.scrollToPosition(postsAdapter.getItemCount() - 1);
                break;
            case R.id.refresh:
                load();
                break;
            case R.id.gallery:
                GalleryFragment f = GalleryFragment.create(boardName, threadNumber);
                f.imagePointers = imagePointers;
                startTransaction(f).commit();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    // Posts Dialogs ///////////////////////////////////////////////////////////////////////////////

    private void showPostsDialog(Post repliedTo, List<Post> posts) {
        PostsDialog dialog = new PostsDialog();
        dialog.repliedTo = repliedTo;
        dialog.adapter = new PostsDialogAdapter(posts);
        startAddTransaction(dialog, PostsDialog.STACK_ID).commit();
    }

    private void showPostsDialog(Post quotedBy, Post post) {
        PostsDialog dialog = new PostsDialog();
        dialog.quotedBy = quotedBy;
        dialog.adapter = new PostsDialogAdapter(Arrays.asList(post));
        startAddTransaction(dialog, PostsDialog.STACK_ID).commit();
    }

    // Crude, but gets the job done
    private boolean isTopFragmentPostsDialog() {
        FragmentManager fm = getFragmentManager();
        int stackHeight = fm.getBackStackEntryCount();
        Fragment fragment = fm.getFragments().get(stackHeight);
        return fragment instanceof PostsDialog;
    }

    public static Pattern postReferencePattern = Pattern.compile("#p(\\d+)");
    // http://stackoverflow.com/a/6020436/283607
    private static LinkedHashSet<String> referencedPosts(Post post) {
        LinkedHashSet<String> refs = new LinkedHashSet<>();
        Matcher m = postReferencePattern.matcher(post.text == null ? "" : post.text);
        while (m.find()) { refs.add(m.group(1)); }
        return refs;
    }

    // Background Refresh //////////////////////////////////////////////////////////////////////////

    // TODO: Isn't there a more elegant solution?
    long lastUpdate;
    static final long updateInterval = 60_000;
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

    public boolean firstLoad;
    public long transitionDuration;

    @TargetApi(Build.VERSION_CODES.LOLLIPOP) private void animatePostsArrival() {
        postsAdapter.hide = true;
        postsAdapter.firstLoad = true;
        postsAdapter.notifyDataSetChanged();
        TransitionManager.beginDelayedTransition(postsView, new Slide(Gravity.BOTTOM));
        postsAdapter.hide = false;
        postsAdapter.firstLoad = false;
        postsAdapter.notifyDataSetChanged();
        // Unreliable, often leads to https://code.google.com/p/android/issues/detail?id=77846
        //postsAdapter.notifyItemChanged(0);
        //postsAdapter.notifyItemRangeInserted(1, posts.size()-1);
    }

    // I'd loved to simply define a return transitions but no matter what I tried it looked bad
    // so here is something that looks fine. Although the problem is that there is no animation
    // overlap...
    boolean abortBackCatch;
    @TargetApi(Build.VERSION_CODES.LOLLIPOP) @Override protected boolean onBackPressed() {
        if (!transitionsAllowed()) return false;
        if (abortBackCatch) return false;
        super.onBackPressed();
        Transition t = inflateTransition(android.R.transition.slide_bottom);
        t.setDuration(230);
        postsAdapter.notifyDataSetChanged();
        TransitionManager.beginDelayedTransition(postsView, t);
        postsAdapter.hide = true;
        postsAdapter.notifyDataSetChanged();
        postsView.postDelayed(new Runnable() {
            @Override public void run() {
                abortBackCatch = true;
                activity.onBackPressed();
                abortBackCatch = false;
            }
        }, 450);
        return true;
    }

    // Adapters ////////////////////////////////////////////////////////////////////////////////////

    class PostsAdapter extends UiAdapter<Post> {
        boolean hide;
        boolean firstLoad;

        public PostsAdapter() {
            this(posts, null, null);
        }
        public PostsAdapter(List<Post> posts,
                            View.OnClickListener clickListener,
                            View.OnLongClickListener longClickListener) {
            super(PostsFragment.this.context, clickListener, longClickListener);
            this.items = posts;
        }

        @Override public View newView(ViewGroup container) {
            return newViewDRY(container);
        }

        @Override
        public void bindView(final Post item, int position, View view) {
            bindViewDRY(item, position, view);
            if (hide && Util.implies(firstLoad, position != 0)) view.setVisibility(View.GONE);
            else view.setVisibility(View.VISIBLE);
        }
    }

    // The reason for these methods is that I need both a RecyclerView adapter
    // and a ListView adapter that are almost identical. See 8ebd8a30115e2e62ffc28cc02f47873d4035290d
    private View newViewDRY(ViewGroup container) {
        return activity.getLayoutInflater().inflate(R.layout.view_post, container, false);
    }

    private void bindViewDRY(final Post item, int position, View view) {
        PostView v = (PostView) view;
        if (item == null) return;
        if (position == 0 && opImage != null && firstLoad) {
            v.bindToOp(opImage, item, boardName, ion);
            //opImage = null;
        } else {
            v.bindTo(item, boardName, threadNumber, ion,
                    repliesCallback, referencedPostCallback, imageCallback);
        }
        if (Build.VERSION.SDK_INT >= 21) {
            if (position == 0) v.image.setTransitionName("op");
            else v.image.setTransitionName(null);
        }
    }

    class PostsDialogAdapter extends BindableAdapter<Post> {
        List<Post> items;

        public PostsDialogAdapter(List<Post> posts) {
            super(context);
            this.items = posts;
        }

        @Override
        public View newView(LayoutInflater inflater, int position, ViewGroup container) {
            return newViewDRY(container);
        }

        @Override
        public void bindView(final Post item, int position, View view) {
            bindViewDRY(item, position, view);
        }

        @Override public int getCount() {
            return items.size();
        }

        @Override public Post getItem(int position) {
            return items.get(position);
        }

        @Override public long getItemId(int position) {
            return position;
        }

    }
}

