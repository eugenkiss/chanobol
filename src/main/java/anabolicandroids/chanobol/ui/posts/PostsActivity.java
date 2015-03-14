package anabolicandroids.chanobol.ui.posts;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.util.Pair;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.transition.Slide;
import android.transition.Transition;
import android.transition.TransitionInflater;
import android.transition.TransitionManager;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.koushikdutta.async.future.FutureCallback;
import com.nineoldandroids.view.ViewHelper;

import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import anabolicandroids.chanobol.R;
import anabolicandroids.chanobol.api.ApiModule;
import anabolicandroids.chanobol.api.data.Post;
import anabolicandroids.chanobol.ui.media.GalleryActivity;
import anabolicandroids.chanobol.ui.media.MediaActivity;
import anabolicandroids.chanobol.ui.media.MediaPointer;
import anabolicandroids.chanobol.ui.scaffolding.SwipeRefreshActivity;
import anabolicandroids.chanobol.ui.scaffolding.UiAdapter;
import anabolicandroids.chanobol.util.BindableAdapter;
import anabolicandroids.chanobol.util.SpacesItemDecoration;
import anabolicandroids.chanobol.util.TransitionListenerAdapter;
import anabolicandroids.chanobol.util.Util;
import butterknife.InjectView;

public class PostsActivity extends SwipeRefreshActivity {

    // Construction ////////////////////////////////////////////////////////////////////////////////

    // Transition related
    private static String EXTRA_TRANSITIONNAME = "transitionName";
    private static String EXTRA_OPPOST = "opPost";
    private String transitionName;
    private BitmapDrawable opImage;

    // To load the respective posts from 4Chan
    private static String EXTRA_THREADNUMBER = "threadNumber";
    private static String EXTRA_BOARDNAME = "boardName";
    private String threadNumber;
    private String boardName;

    // Internal state
    private static String POSTS = "posts";
    private static String POSTMAP = "postMap";
    private static String REPLIES = "replies";
    private static String MEDIAPOINTERS = "mediaPointers";
    private static String MEDIAMAP = "mediaMap";
    private static String BITMAPCACHEKEYS = "bitmapCacheKeys";
    private ArrayList<Post> posts;
    private PostsAdapter postsAdapter;
    // Map from a post number to its post POJO
    private HashMap<String, Post> postMap;
    // Map from a post number X to the post numbers of the posts that refer to X
    private HashMap<String, ArrayList<String>> replies;
    // This is provided to the gallery fragment so that it can immediately load its images without
    // waiting for the result of requesting the thread json and extracting its media references once more
    private ArrayList<MediaPointer> mediaPointers;
    // Map from a post number to the index of its mediaPointer in mediaPointers (important for swiping)
    public Map<String, Integer> mediaMap;
    // To remove all the bitmaps from Ion's cache once the activity's been destroyed
    private ArrayList<String> bitmapCacheKeys;

    public static void launch(
            Activity activity, View transitionView, String transitionName,
            Post opPost, String boardName, String threadNumber
    ) {
        if (transitionView != null) ViewCompat.setTransitionName(transitionView, transitionName);
        ActivityOptionsCompat options = makeSceneTransitionAnimation(activity,
                Pair.create(transitionView, transitionName)
        );
        Intent intent = new Intent(activity, PostsActivity.class);
        intent.putExtra(EXTRA_TRANSITIONNAME, transitionName);
        intent.putExtra(EXTRA_OPPOST, Parcels.wrap(opPost));
        intent.putExtra(EXTRA_BOARDNAME, boardName);
        intent.putExtra(EXTRA_THREADNUMBER, threadNumber);
        ActivityCompat.startActivity(activity, intent, options.toBundle());
    }

    @InjectView(R.id.posts) RecyclerView postsView;

    @Override protected int getLayoutResource() { return R.layout.activity_posts; }
    @Override protected RecyclerView getRootRecyclerView() { return postsView; }

    @Override protected void onCreate(Bundle savedInstanceState) {
        supportPostponeEnterTransition();
        super.onCreate(savedInstanceState);

        Bundle b = getIntent().getExtras();
        transitionName = b.getString(EXTRA_TRANSITIONNAME);
        boardName = b.getString(EXTRA_BOARDNAME);
        threadNumber = b.getString(EXTRA_THREADNUMBER);
        Post opPost = Parcels.unwrap(b.getParcelable(EXTRA_OPPOST));

        setTitle(boardName + "/" + threadNumber);

        if (opPost != null) {
            String thumbUrl = ApiModule.thumbUrl(boardName, opPost.mediaId);
            try {
                opImage = new BitmapDrawable(getResources(), ion.build(this).load(thumbUrl).asBitmap().get());
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }

        if (savedInstanceState == null) {
            firstLoad = true;
            posts = new ArrayList<>();
            postMap = new HashMap<>();
            replies = new HashMap<>();
            mediaPointers = new ArrayList<>();
            mediaMap = new HashMap<>();
            bitmapCacheKeys = new ArrayList<>();
        } else {
            firstLoad = false;
            posts = Parcels.unwrap(savedInstanceState.getParcelable(POSTS));
            postMap = Parcels.unwrap(savedInstanceState.getParcelable(POSTMAP));
            replies = Parcels.unwrap(savedInstanceState.getParcelable(REPLIES));
            mediaPointers = Parcels.unwrap(savedInstanceState.getParcelable(MEDIAPOINTERS));
            mediaMap = Parcels.unwrap(savedInstanceState.getParcelable(MEDIAMAP));
            bitmapCacheKeys = Parcels.unwrap(savedInstanceState.getParcelable(BITMAPCACHEKEYS));

            previousToolbarPosition = savedInstanceState.getFloat(PREVIOUS_TOOLBAR_POSITION);
            previousStackHeight = savedInstanceState.getInt(PREVIOUS_STACK_HEIGHT);
            previousTitle = savedInstanceState.getString(PREVIOUS_TITLE);
        }

        postsAdapter = new PostsAdapter();
        postsView.setAdapter(postsAdapter);
        postsView.setLayoutManager(new LinearLayoutManager(this));
        postsView.addItemDecoration(new SpacesItemDecoration((int) resources.getDimension(R.dimen.post_spacing)));

        getSupportFragmentManager().addOnBackStackChangedListener(backStackChangedListener);
        setupUpLongClickCloseAll();

        if (firstLoad && opPost != null) {
            posts.add(opPost);
            postsAdapter.notifyItemChanged(0);
        }

        if (savedInstanceState == null) load();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().getSharedElementEnterTransition().addListener(new TransitionListenerAdapter() {
                @Override public void onTransitionEnd(Transition transition) {
                    revealAnimationCallback.markTransitionFinished();
                }
            });
            getWindow().setEnterTransition(TransitionInflater.from(this).inflateTransition(android.R.transition.slide_right));
            getWindow().setReturnTransition(TransitionInflater.from(this).inflateTransition(android.R.transition.slide_right));
        } else {
            revealAnimationCallback.markTransitionFinished();
        }

        // Why this trivial delay? Otherwise the transition glitches for some reason.
        postsView.postDelayed(new Runnable() {
            @Override public void run() {
                supportStartPostponedEnterTransition();
            }
        }, 10);
    }

    @Override protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(POSTS, Parcels.wrap(posts));
        outState.putParcelable(POSTMAP, Parcels.wrap(postMap));
        outState.putParcelable(REPLIES, Parcels.wrap(replies));
        outState.putParcelable(MEDIAPOINTERS, Parcels.wrap(mediaPointers));
        outState.putParcelable(MEDIAMAP, Parcels.wrap(mediaMap));
        outState.putParcelable(BITMAPCACHEKEYS, Parcels.wrap(bitmapCacheKeys));

        outState.putString(PREVIOUS_TITLE, previousTitle);
        outState.putFloat(PREVIOUS_TOOLBAR_POSITION, previousToolbarPosition);
        outState.putInt(PREVIOUS_STACK_HEIGHT, previousStackHeight);
    }

    // Callbacks ///////////////////////////////////////////////////////////////////////////////////

    public static interface RepliesCallback {
        public void onClick(Post post);
    }
    RepliesCallback repliesCallback = new RepliesCallback() {
        @Override public void onClick(final Post post) {
            postsView.postDelayed(new Runnable() {
                @Override public void run() {
                    ArrayList<Post> posts = new ArrayList<>(post.replyCount);
                    for (String id : replies.get(post.number)) {
                        posts.add(postMap.get(id));
                    }
                    showPostsDialog(post, posts);
                }
            }, RIPPLE_DELAY);
        }
    };

    public static interface QuoteCallback {
        public void onClick(String quoterId, String quotedId);
    }
    QuoteCallback quoteCallback = new QuoteCallback() {
        @Override public void onClick(final String quoterId, final String quotedId) {
            Post quoted = postMap.get(quotedId);
            if (quoted != null) {
                postsView.postDelayed(new Runnable() {
                    @Override public void run() {
                        showPostsDialog(postMap.get(quoterId), postMap.get(quotedId));
                    }
                }, RIPPLE_DELAY);
            }
        }
    };

    public static interface ImageCallback {
        public void onClick(Post post, ImageView iv);
    }
    ImageCallback imageCallback = new ImageCallback() {
        @Override public void onClick(Post post, ImageView iv) {
            int w = iv.getWidth();
            int h = iv.getHeight();
            Drawable d = iv.getDrawable();
            int r = Math.min(d.getIntrinsicHeight(), d.getIntrinsicWidth());
            int[] xy = new int[2];
            iv.getLocationOnScreen(xy);
            int cx = xy[0] + w/2;
            int cy = xy[1] + h/2;
            String uuid = UUID.randomUUID().toString();
            MediaActivity.transitionBitmap = Util.drawableToBitmap(iv.getDrawable());
            MediaActivity.launch(
                    PostsActivity.this, iv, uuid, new Point(cx, cy), r, false,
                    boardName, threadNumber, mediaMap.get(post.number), mediaPointers
            );
        }
    };

    private class RevealAnimationCallback {
        private boolean dataLoaded, transitionFinished;
        public void markDataLoaded() {
            dataLoaded = true;
            startAnimation();
        }
        public void markTransitionFinished() {
            transitionFinished = true;
            startAnimation();
        }
        private void startAnimation() {
            if (dataLoaded && transitionFinished && firstLoad) {
                postsView.postDelayed(new Runnable() {
                    @Override public void run() {
                        animatePostsArrival();
                        firstLoad = false;
                    }
                }, 80);
            }
        }
    }
    RevealAnimationCallback revealAnimationCallback = new RevealAnimationCallback();

    // Data Loading ////////////////////////////////////////////////////////////////////////////////

    @Override protected void load() { load(false); }

    private void load(final boolean silent) {
        super.load();
        if (silent || firstLoad) loadingBar.setVisibility(View.GONE);

        // Indicate ongoing request after all if first load takes especially long
        if (firstLoad) {
            postsView.postDelayed(new Runnable() {
                @Override public void run() {
                    if (firstLoad && loading) {
                        loadingBar.setVisibility(View.VISIBLE);
                    }
                }
            }, 500);
        }

        service.listPosts(this, boardName, threadNumber, new FutureCallback<List<Post>>() {
            @Override public void onCompleted(Exception e, List<Post> result) {
                if (e != null) {
                    if (!silent && e.getMessage() != null) showToast(e.getMessage());
                    System.out.println("" + e.getMessage());
                    loaded();
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
                        if (!replies.containsKey(number)) replies.put(number, new ArrayList<String>());
                        Post referenced = postMap.get(number);
                        if (referenced != null) { // e.g. a stale reference to deleted post could be null
                            replies.get(number).add(p.number);
                            referenced.replyCount++;
                        }
                    }
                    if (p.mediaId != null) {
                        if (prefs.preloadThumbnails())
                            ion.build(PostsActivity.this).load(ApiModule.thumbUrl(boardName, p.mediaId)).asBitmap().tryGet();
                        mediaPointers.add(new MediaPointer(p, p.mediaId, p.mediaExtension, p.mediaWidth, p.mediaHeight));
                        mediaMap.put(p.number, mediaIndex);
                        mediaIndex++;
                    }
                }
                lastUpdate = System.currentTimeMillis();
                revealAnimationCallback.markDataLoaded();
                loaded();
            }
        });
    }

    private void update() {
        if (loading) return;
        lastUpdate = System.currentTimeMillis();
        load(true);
    }

    @Override protected void cancelPending() {
        super.cancelPending();
        // TODO: Reenable once https://github.com/koush/ion/issues/422 is fixed
        //ion.cancelAll(this);
    }

    // Lifecycle ///////////////////////////////////////////////////////////////////////////////////

    @Override public void onResume() {
        super.onResume();
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
        if (executor != null) executor.shutdown();
        getSupportFragmentManager().removeOnBackStackChangedListener(backStackChangedListener);
        for (String key : bitmapCacheKeys) {
            if (key != null) ion.getBitmapCache().remove(key);
        }
        System.gc();
        super.onDestroy();
    }

    // Toolbar Menu ////////////////////////////////////////////////////////////////////////////////

    Menu menu;

    @Override public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.posts, menu);
        this.menu = menu;
        return true;
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
                GalleryActivity.launch(this, boardName, threadNumber, mediaPointers);
                break;
            case R.id.close:
                dismissAllPostsDialogs();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    // Posts Dialogs ///////////////////////////////////////////////////////////////////////////////

    private static String PREVIOUS_TITLE = "previousTitle";
    private static String PREVIOUS_TOOLBAR_POSITION = "previousToolbarPosition";
    private static String PREVIOUS_STACK_HEIGHT = "previousStackHeight";
    String previousTitle;
    float previousToolbarPosition;
    int previousStackHeight;

    private void addPostsDialog(PostsDialog d) {
        FragmentManager fm = getSupportFragmentManager();
        int stackHeight = fm.getBackStackEntryCount();
        if (stackHeight == 0) {
            previousToolbarPosition = ViewHelper.getTranslationY(toolbar);
            previousTitle = getTitle().toString();
            showToolbar();
            menu.setGroupVisible(R.id.posts, false);
            menu.setGroupVisible(R.id.postsDialog, true);
        }
        fm.beginTransaction()
                .add(R.id.container, d)
                .addToBackStack(PostsDialog.STACK_ID)
                .commitAllowingStateLoss();
    }

    FragmentManager.OnBackStackChangedListener backStackChangedListener = new FragmentManager.OnBackStackChangedListener() {
        @Override public void onBackStackChanged() {
            FragmentManager fm = getSupportFragmentManager();
            int stackHeight = fm.getBackStackEntryCount();
            if (stackHeight == 0) {
                if (Build.VERSION.SDK_INT >= 14) {
                    toolbar.animate().y(previousToolbarPosition).setDuration(dur);
                } else {
                    ViewHelper.setTranslationY(toolbar, previousToolbarPosition);
                }
                setTitle(previousTitle);
                menu.setGroupVisible(R.id.posts, true);
                menu.setGroupVisible(R.id.postsDialog, false);
            }
            if (stackHeight > 0 && stackHeight < previousStackHeight) {
                Fragment fragment = fm.getFragments().get(stackHeight-1);
                if (fragment != null) fragment.onResume();
            }
            previousStackHeight = stackHeight;
        }
    };

    boolean removing = false;
    @Override public void onBackPressed() {
        if (removing) return;
        FragmentManager fm = getSupportFragmentManager();
        int stackHeight = fm.getBackStackEntryCount();
        if (stackHeight > 0) {
            removing = true;
            PostsDialog dialog = (PostsDialog) fm.getFragments().get(stackHeight-1);
            dialog.animatePostsRemoval();
            postsView.postDelayed(new Runnable() {
                @Override public void run() {
                    removing = false;
                    PostsActivity.super.onBackPressed();
                }
            }, PostsDialog.ANIM_DURATION);
        } else {
            super.onBackPressed();
        }
    }

    @Override public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        FragmentManager fm = getSupportFragmentManager();
        int stackHeight = fm.getBackStackEntryCount();
        if (stackHeight > 0 && keyCode == KeyEvent.KEYCODE_BACK) {
            dismissAllPostsDialogs();
            return true;
        }
        return super.onKeyLongPress(keyCode, event);
    }

    // http://stackoverflow.com/a/27180584/283607
    private void setupUpLongClickCloseAll() {
        if (Build.VERSION.SDK_INT >= 14) {
            toolbar.setNavigationContentDescription("up");
            final ArrayList<View> outViews = new ArrayList<>();
            toolbar.findViewsWithText(outViews, "up", View.FIND_VIEWS_WITH_CONTENT_DESCRIPTION);
            if (outViews.size() == 0 || outViews.get(0) == null) return;
            outViews.get(0).setOnLongClickListener(new View.OnLongClickListener() {
                @Override public boolean onLongClick(View v) {
                    FragmentManager fm = getSupportFragmentManager();
                    int stackHeight = fm.getBackStackEntryCount();
                    if (stackHeight > 0 && !drawerLayout.isDrawerOpen(drawer)) {
                        dismissAllPostsDialogs();
                        return true;
                    }
                    return false;
                }
            });
        }
    }

    private void showPostsDialog(Post repliedTo, List<Post> posts) {
        PostsDialog dialog = new PostsDialog();
        dialog.repliedTo = repliedTo;
        dialog.adapter = new PostsDialogAdapter(posts);
        addPostsDialog(dialog);
    }

    private void showPostsDialog(Post quotedBy, Post post) {
        PostsDialog dialog = new PostsDialog();
        dialog.quotedBy = quotedBy;
        dialog.adapter = new PostsDialogAdapter(Arrays.asList(post));
        addPostsDialog(dialog);
    }

    boolean removingAll = false;
    private void dismissAllPostsDialogs() {
        if (removingAll) return;
        removingAll = true;
        FragmentManager fm = getSupportFragmentManager();
        for (Fragment f : fm.getFragments()) { if (f instanceof PostsDialog) { PostsDialog d = (PostsDialog) f;
            d.animatePostsRemoval();
        }}
        postsView.postDelayed(new Runnable() {
            @Override public void run() {
                removingAll = false;
                getSupportFragmentManager().popBackStack(PostsDialog.STACK_ID, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            }
        }, PostsDialog.ANIM_DURATION);
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

    public boolean firstLoad;

    @TargetApi(Build.VERSION_CODES.LOLLIPOP) private void animatePostsArrival() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
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
        } else {
            postsAdapter.notifyDataSetChanged();
        }
    }

    // Adapters ////////////////////////////////////////////////////////////////////////////////////

    class PostsAdapter extends UiAdapter<Post> {
        boolean hide;
        boolean firstLoad;

        public PostsAdapter() { this(posts, null, null); }
        public PostsAdapter(List<Post> posts,
                            View.OnClickListener clickListener,
                            View.OnLongClickListener longClickListener) {
            super(PostsActivity.this, clickListener, longClickListener);
            this.items = posts;
        }

        @Override public View newView(ViewGroup container) {
            return newViewDRY(container);
        }

        @Override public void bindView(final Post item, int position, View view) {
            bindViewDRY(item, position, view);
            setVisibility(view, !hide || position == 0);
        }
    }

    // The reason for these methods is that I need both a RecyclerView adapter
    // and a ListView adapter that are almost identical. See 8ebd8a30115e2e62ffc28cc02f47873d4035290d
    private View newViewDRY(ViewGroup container) {
        return getLayoutInflater().inflate(R.layout.view_post, container, false);
    }

    private void bindViewDRY(final Post item, int position, View view) {
        PostView v = (PostView) view;
        if (item == null) return;
        if (position == 0 && firstLoad) {
            v.bindToOp(opImage, transitionName, item, boardName, ion);
        } else {
            v.bindTo(item, boardName, threadNumber, ion, bitmapCacheKeys,
                     repliesCallback, quoteCallback, imageCallback);
        }
        if (position == 0)  ViewCompat.setTransitionName(v.image, transitionName);
        else  ViewCompat.setTransitionName(v.image, null);
    }

    class PostsDialogAdapter extends BindableAdapter<Post> {
        boolean hide = false;
        List<Post> items;

        public PostsDialogAdapter(List<Post> posts) {
            super(PostsActivity.this);
            this.items = posts;
        }

        @Override public View newView(LayoutInflater inflater, int position, ViewGroup container) {
            return newViewDRY(container);
        }

        @Override public void bindView(final Post item, int position, View view) {
            bindViewDRY(item, position, view);
            setVisibility(view, !hide);
        }

        @Override public int getCount() { return items.size(); }
        @Override public Post getItem(int position) { return items.get(position); }
        @Override public long getItemId(int position) { return position; }
    }

}
