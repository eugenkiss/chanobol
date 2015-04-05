package anabolicandroids.chanobol.ui.posts;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.util.Pair;
import android.support.v4.view.ViewCompat;
import android.support.v7.graphics.Palette;
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
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.inject.Inject;

import anabolicandroids.chanobol.R;
import anabolicandroids.chanobol.api.ApiModule;
import anabolicandroids.chanobol.api.data.MediaPointer;
import anabolicandroids.chanobol.api.data.Post;
import anabolicandroids.chanobol.api.data.Thread;
import anabolicandroids.chanobol.api.data.ThreadPreview;
import anabolicandroids.chanobol.ui.media.GalleryActivity;
import anabolicandroids.chanobol.ui.media.MediaActivity;
import anabolicandroids.chanobol.ui.scaffolding.SwipeRefreshActivity;
import anabolicandroids.chanobol.ui.scaffolding.UiAdapter;
import anabolicandroids.chanobol.util.BindableAdapter;
import anabolicandroids.chanobol.util.ImageSaver;
import anabolicandroids.chanobol.util.SpacesItemDecoration;
import anabolicandroids.chanobol.util.TransitionListenerAdapter;
import anabolicandroids.chanobol.util.Util;
import butterknife.InjectView;

public class PostsActivity extends SwipeRefreshActivity {

    // Construction ////////////////////////////////////////////////////////////////////////////////

    // Transition related
    private static String EXTRA_TRANSITIONNAME = "transitionName";
    private String transitionName;
    private BitmapDrawable opImage;

    // Essential state
    private static String EXTRA_ROOT = "root";
    private static String THREAD = "thread";
    private Thread thread;

    // Internal state
    private PostsAdapter postsAdapter;
    // To remove all the bitmaps from Ion's cache once the activity's been destroyed
    private static String BITMAP_CACHE_KEYS = "bitmapCacheKeys";
    private ArrayList<String> bitmapCacheKeys;
    // Convenience
    private String boardName;
    private String threadNumber;
    private LinearLayoutManager layoutManager;

    public static void launch(
            Activity activity, View transitionView, String transitionName,
            Thread thread, boolean root
    ) {
        if (activity instanceof PostsActivity) {
            // Special case: Thread was opened from catalog, is scrolled and reopened from
            // watchlist in nav drawer. Then the scroll position should be remembered correctly.
            PostsActivity pa = (PostsActivity) activity;
            if (thread.id.equals(pa.thread.id)) {
                pa.persistPositionMaybe();
            }
        }

        // Must not be null or there will be glitches with the first PostView!
        if (transitionName == null) transitionName = "not null";
        if (transitionView != null) ViewCompat.setTransitionName(transitionView, transitionName);
        ActivityOptionsCompat options = makeSceneTransitionAnimation(activity,
                Pair.create(transitionView, transitionName)
        );
        Intent intent = new Intent(activity, PostsActivity.class);
        intent.putExtra(EXTRA_TRANSITIONNAME, transitionName);
        intent.putExtra(EXTRA_ROOT, root);
        intent.putExtra(THREAD, Parcels.wrap(thread));
        if (root) {
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(intent);
            activity.finish();
        } else {
            ActivityCompat.startActivity(activity, intent, options.toBundle());
        }
    }

    public static void launch(
            Activity activity, View transitionView, String transitionName,
            Thread thread
    ) { launch(activity, transitionView, transitionName, thread, false); }

    public static void launch(
            Activity activity, View transitionView, String transitionName,
            Post opPost, String boardName, String threadNumber
    ) {
        Thread thread = new Thread(boardName, threadNumber);
        thread.posts.add(opPost);
        launch(activity, transitionView, transitionName, thread);
    }

    public static void launchFromWatchlist(Activity activity, Thread thread) {
        launch(activity, null, null, thread, true);
    }

    @Inject ImageSaver imageSaver;

    @InjectView(R.id.posts) RecyclerView postsView;

    @Override protected int getLayoutResource() { return R.layout.activity_posts; }
    @Override protected RecyclerView getRootRecyclerView() { return postsView; }

    @Override protected void onCreate(Bundle savedInstanceState) {
        supportPostponeEnterTransition();
        Bundle b = getIntent().getExtras();
        taskRoot = b.getBoolean(EXTRA_ROOT);
        super.onCreate(savedInstanceState);

        transitionName = b.getString(EXTRA_TRANSITIONNAME);
        thread = Parcels.unwrap(b.getParcelable(THREAD));
        boolean inWatchlist = persistentData.isInWatchlist(thread.id);
        if (inWatchlist) thread = persistentData.getWatchlistThread(thread.id);
        boardName = thread.boardName;
        threadNumber = thread.threadNumber;
        Post opPost = thread.opPost();

        if (opPost != null) {
            String thumbUrl = ApiModule.thumbUrl(boardName, opPost.mediaId);
            try {
                opImage = new BitmapDrawable(getResources(), ion.build(this).load(thumbUrl).asBitmap().get(500, TimeUnit.MILLISECONDS));
                if (opImage.getBitmap() != null) {
                    Palette palette = Palette.generate(opImage.getBitmap());
                    int primaryDark = getResources().getColor(R.color.colorPrimaryDark);
                    opPost.thumbMutedColor = palette.getMutedColor(primaryDark);
                }
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                e.printStackTrace();
            }
        }

        if (savedInstanceState == null) {
            firstLoad = true;
            bitmapCacheKeys = new ArrayList<>();
        } else {
            firstLoad = false;
            thread = Parcels.unwrap(savedInstanceState.getParcelable(THREAD));
            bitmapCacheKeys = Parcels.unwrap(savedInstanceState.getParcelable(BITMAP_CACHE_KEYS));

            previousToolbarPosition = savedInstanceState.getFloat(PREVIOUS_TOOLBAR_POSITION);
            previousStackHeight = savedInstanceState.getInt(PREVIOUS_STACK_HEIGHT);
            previousTitle = savedInstanceState.getString(PREVIOUS_TITLE);
        }

        setTitle(thread.title());

        postsAdapter = new PostsAdapter();
        postsView.setAdapter(postsAdapter);
        layoutManager = new LinearLayoutManager(this);
        postsView.setLayoutManager(layoutManager);
        postsView.addItemDecoration(new SpacesItemDecoration((int) resources.getDimension(R.dimen.post_spacing)));
        if (inWatchlist) postsView.scrollToPosition(thread.lastVisibleIndex);

        getSupportFragmentManager().addOnBackStackChangedListener(backStackChangedListener);
        setupUpLongClickDismissAll();

        if (firstLoad && opPost != null) {
            postsAdapter.notifyItemChanged(0);
        }

        if (inWatchlist) {
            firstLoad = false;
            postsAdapter.notifyDataSetChanged();
        } else {
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
        }

        if (savedInstanceState == null) load();

        // Why this trivial delay? Otherwise the transition glitches for some reason.
        postsView.postDelayed(new Runnable() {
            @Override public void run() {
                supportStartPostponedEnterTransition();
            }
        }, inWatchlist ? 100 : 20);
    }

    @Override protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(THREAD, Parcels.wrap(thread));
        outState.putParcelable(BITMAP_CACHE_KEYS, Parcels.wrap(bitmapCacheKeys));

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
                    for (String id : thread.replies.get(post.number)) {
                        posts.add(thread.postMap.get(id));
                    }
                    showPostsDialog(post, posts);
                }
            }, RIPPLE_DELAY());
        }
    };

    public static interface QuoteCallback {
        public void onClick(String quoterId, String quotedId);
    }
    QuoteCallback quoteCallback = new QuoteCallback() {
        @Override public void onClick(final String quoterId, final String quotedId) {
            Post quoted = thread.postMap.get(quotedId);
            if (quoted != null) {
                postsView.postDelayed(new Runnable() {
                    @Override public void run() {
                        showPostsDialog(thread.postMap.get(quoterId), thread.postMap.get(quotedId));
                    }
                }, RIPPLE_DELAY());
            }
        }
    };

    public static interface ImageCallback {
        public void onClick(int position, Post post, ImageView iv);
    }
    ImageCallback imageCallback = new ImageCallback() {
        @Override public void onClick(int position, Post post, ImageView iv) {
            int w = iv.getWidth();
            int h = iv.getHeight();
            Drawable d = iv.getDrawable();
            int r = Math.min(d.getIntrinsicHeight(), d.getIntrinsicWidth());
            int[] xy = new int[2];
            iv.getLocationOnScreen(xy);
            int cx = xy[0] + w/2;
            int cy = xy[1] + h/2;
            String uuid = position == 0 ? transitionName : UUID.randomUUID().toString();
            MediaActivity.transitionBitmap = Util.drawableToBitmap(iv.getDrawable());
            int color = post.thumbMutedColor != -1 ? post.thumbMutedColor : getResources().getColor(R.color.colorPrimaryDark);
            MediaActivity.launch(
                    PostsActivity.this, iv, uuid, new Point(cx, cy), r, color, false,
                    thread, thread.mediaMap.get(post.number)
            );
        }
    };

    public static interface JumpCallback {
        public void onJumpTo(Post post);
    }
    JumpCallback jumpCallback = new JumpCallback() {
        @Override public void onJumpTo(Post post) {
            final int index = thread.indexForPost(post);
            if (index == -1) return;
            dismissAllPostsDialogs();
            showToolbar();
            postsView.scrollToPosition(index);
            postsView.postDelayed(new Runnable() {
                // A more reliable way would be to listen to when the scroll finished
                // but too much work for this visual hint, so a rough delay it will be.
                @Override public void run() {
                    PostView pv = (PostView) layoutManager.findViewByPosition(index);
                    if (pv != null) pv.blinkRed();
                }
            }, 500);
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

    public boolean firstLoad;

    @Override protected void load() { load(false); }

    private void load(final boolean silent) {
        if (thread.dead) {
            loaded();
            if (!silent) showToast(R.string.no_thread);
            return;
        }

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

        thread.load(service, new Thread.OnResultCallback() {
            @Override public void onSuccess() {
                if (prefs.preloadThumbnails()) {
                    for (MediaPointer mp : thread.mediaPointers)
                        ion.build(PostsActivity.this).load(ApiModule.thumbUrl(boardName, mp.id)).asBitmap().tryGet();
                }
                if (firstLoad) revealAnimationCallback.markDataLoaded();
                else postsAdapter.notifyDataSetChanged();
                loaded();
            }

            @Override public void onError(String message) {
                if (!silent) showToast(message);
                loaded();
            }
        });

        service.listThreads(this, boardName, new FutureCallback<List<ThreadPreview>>() {
            @Override public void onCompleted(Exception e, List<ThreadPreview> result) {
                if (e != null) return;
                boolean dead = true;
                for (ThreadPreview t : result) {
                    if (t.number.equals(threadNumber)) {
                        dead = false;
                        break;
                    }
                }
                if (dead) {
                    thread.dead = true;
                    setTitle(thread.title());
                    // Simply to update the title in the nav bar with a dead indication
                    if (persistentData.isInWatchlist(thread.id)) {
                        persistentData.addWatchlistThread(thread);
                    }
                }
            }
        });
    }

    private void update() {
        if (loading) return;
        thread.resetUpdateTimer();
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
        thread.initBackgroundUpdater(prefs, new Runnable() {
            @Override public void run() {
                PostsActivity.this.runOnUiThread(new Runnable() {
                    @Override public void run() {
                        update();
                    }
                });
            }
        });
        thread.resumeBackgroundUpdating();
    }

    @Override public void onStop() {
        super.onStop();
        thread.stopBackgroundUpdating();
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
        thread.killBackgroundUpdater();
        persistPositionMaybe();
        getSupportFragmentManager().removeOnBackStackChangedListener(backStackChangedListener);
        for (String key : bitmapCacheKeys) {
            if (key != null) ion.getBitmapCache().remove(key);
        }
        System.gc();
        super.onDestroy();
    }

    public void persistPositionMaybe() {
        if (persistentData.isInWatchlist(thread.id)) {
            thread.lastVisibleIndex = layoutManager.findFirstVisibleItemPosition();
            persistentData.addWatchlistThread(thread);
        }
    }

    // Toolbar Menu ////////////////////////////////////////////////////////////////////////////////

    Menu menu;

    @Override public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.posts, menu);
        this.menu = menu;
        if (persistentData.isInWatchlist(thread.id)) {
            menu.findItem(R.id.addWatchlist).setVisible(false);
            menu.findItem(R.id.removeWatchlist).setVisible(true);
        } else {
            menu.findItem(R.id.addWatchlist).setVisible(true);
            menu.findItem(R.id.removeWatchlist).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.up:
                postsView.scrollToPosition(0);
                break;
            case R.id.down:
                postsView.scrollToPosition(postsAdapter.getItemCount() - 1);
                break;
            case R.id.refresh:
                load();
                break;
            case R.id.gallery:
                GalleryActivity.launch(this, thread);
                break;
            case R.id.close:
                dismissAllPostsDialogs();
                break;
            case R.id.saveAll:
                GalleryActivity.saveAll(this, imageSaver, boardName, threadNumber, thread.mediaPointers);
                break;
            case R.id.addWatchlist:
                persistentData.addWatchlistThread(thread);
                invalidateOptionsMenu();
                break;
            case R.id.removeWatchlist:
                persistentData.removeWatchlistThread(thread);
                invalidateOptionsMenu();
                break;
            case R.id.openBrowser:
                Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(ApiModule.threadUrl(boardName, threadNumber)));
                startActivity(Intent.createChooser(i, "Open in Browser"));
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
            if (dialog != null) dialog.animatePostsRemoval();
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
    private void setupUpLongClickDismissAll() {
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
        FragmentManager fm = getSupportFragmentManager();
        if (fm.getFragments() == null) return;
        removingAll = true;

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

    // Animations //////////////////////////////////////////////////////////////////////////////////

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

        public PostsAdapter() { this(thread.posts, null, null); }
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
            bindViewDRY(item, position, false, view);
            setVisibility(view, !hide || position == 0);
        }
    }

    // The reason for these methods is that I need both a RecyclerView adapter
    // and a ListView adapter that are almost identical. See 8ebd8a30115e2e62ffc28cc02f47873d4035290d
    private View newViewDRY(ViewGroup container) {
        return getLayoutInflater().inflate(R.layout.view_post, container, false);
    }

    private void bindViewDRY(final Post item, int position, boolean inDialog, View view) {
        PostView v = (PostView) view;
        if (item == null) return;
        v.prefs = prefs;
        if (position == 0 && firstLoad) {
            v.bindToOp(opImage, item, boardName, ion);
        } else {
            v.bindTo(position, item, boardName, ion, bitmapCacheKeys, inDialog,
                     repliesCallback, quoteCallback, imageCallback, jumpCallback);
        }
        v.setImageTransitionName(position == 0 ? transitionName : null);
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
            bindViewDRY(item, position, true, view);
            setVisibility(view, !hide);
        }

        @Override public int getCount() { return items.size(); }
        @Override public Post getItem(int position) { return items.get(position); }
        @Override public long getItemId(int position) { return position; }
    }

}
