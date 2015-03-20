package anabolicandroids.chanobol.ui.media;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.util.Pair;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPager;
import android.support.v8.renderscript.RSRuntimeException;
import android.transition.Transition;
import android.transition.TransitionInflater;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import anabolicandroids.chanobol.R;
import anabolicandroids.chanobol.api.ApiModule;
import anabolicandroids.chanobol.ui.scaffolding.UiActivity;
import anabolicandroids.chanobol.util.HackyViewPager;
import anabolicandroids.chanobol.util.TransitionListenerAdapter;
import anabolicandroids.chanobol.util.Util;
import butterknife.InjectView;

// The following is the problem I was facing all the time:
// https://plus.google.com/app/basic/photos/108918686796284921012/album/6082900202909178977/6082900205521388114
public class MediaActivity extends UiActivity {

    // Construction ////////////////////////////////////////////////////////////////////////////////

    // Transition related
    public static Bitmap transitionBitmap;
    private static String EXTRA_TRANSITIONNAME = "transitionName";
    private static String EXTRA_REVEALPOINT = "revealPoint";
    private static String EXTRA_REVEALRADIUS = "revealRadius";
    private static String EXTRA_REVEALCOLOR = "revealColor";
    private static String EXTRA_FROM_GALLERY = "fromGallery";
    private static String EXTRA_INITIALINDEX = "initialIndex";
    private String transitionName;
    private Point revealPoint;
    private int revealRadius;
    private int revealColor;
    private boolean fromGallery;
    private int initialIndex;

    // Needed to form a valid request to 4Chan
    private static String EXTRA_BOARDNAME = "boardName";
    private static String EXTRA_THREADNUMBER = "threadNumber";
    private String boardName;
    private String threadNumber;

    // Swipes
    private static String EXTRA_IMAGEPOINTERS = "imagePointers";
    private static String EXTRA_INDEX = "index";
    private List<MediaPointer> mediaPointers;
    private int currentIndex;

    // To prevent transition glitches
    private int initialStatusBarColor;
    private int targetStatusBarColor;
    private int initialNavBarColor;
    private int targetNavBarColor;

    public static void launch(
            Activity activity, View transitionView, String transitionName,
            Point revealPoint, int revealStartRadius, int revealColor, boolean fromGallery,
            String boardName, String threadNumber,
            int index, ArrayList<MediaPointer> mediaPointers
    ) {
        ViewCompat.setTransitionName(transitionView, transitionName);
        ActivityOptionsCompat options = makeSceneTransitionAnimation(activity,
                Pair.create(transitionView, transitionName)
        );
        Intent intent = new Intent(activity, MediaActivity.class);
        intent.putExtra(EXTRA_TRANSITIONNAME, transitionName);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            intent.putExtra(EXTRA_REVEALPOINT, revealPoint);
        } else {
            // Do nothing because of 2.3.7's:
            // "java.lang.RuntimeException: Parcel: unable to marshal value Point..."
        }
        intent.putExtra(EXTRA_REVEALRADIUS, revealStartRadius);
        intent.putExtra(EXTRA_REVEALCOLOR, revealColor);
        intent.putExtra(EXTRA_FROM_GALLERY, fromGallery);
        intent.putExtra(EXTRA_BOARDNAME, boardName);
        intent.putExtra(EXTRA_THREADNUMBER, threadNumber);
        intent.putExtra(EXTRA_INITIALINDEX, index);
        intent.putExtra(EXTRA_INDEX, index);
        intent.putExtra(EXTRA_IMAGEPOINTERS, Parcels.wrap(mediaPointers));
        ActivityCompat.startActivity(activity, intent, options.toBundle());
    }

    @InjectView(R.id.revealbg) View background;
    @InjectView(R.id.releaseIndicator) View releaseIndicator;
    @InjectView(R.id.imageViewForTransition) ImageView imageViewForTransition;
    @InjectView(R.id.viewPager) HackyViewPager viewPager;

    MediaView[] pagerViews;

    @Override protected int getLayoutResource() { return R.layout.activity_media; }

    @Override protected void onCreate(final Bundle savedInstanceState) {
        supportPostponeEnterTransition();
        super.onCreate(savedInstanceState);
        background.getBackground().setAlpha(255);
        setTitle("");

        Bundle b = getIntent().getExtras();
        revealPoint   = b.getParcelable(EXTRA_REVEALPOINT);
        revealRadius  = b.getInt(EXTRA_REVEALRADIUS) / 2;
        revealColor   = b.getInt(EXTRA_REVEALCOLOR);
        boardName     = b.getString(EXTRA_BOARDNAME);
        threadNumber  = b.getString(EXTRA_THREADNUMBER);
        initialIndex  = b.getInt(EXTRA_INITIALINDEX);
        currentIndex  = b.getInt(EXTRA_INDEX);
        fromGallery   = b.getBoolean(EXTRA_FROM_GALLERY);
        mediaPointers = Parcels.unwrap(b.getParcelable(EXTRA_IMAGEPOINTERS));

        transitionName = b.getString(EXTRA_TRANSITIONNAME);
        ViewCompat.setTransitionName(imageViewForTransition, transitionName);
        imageViewForTransition.setImageBitmap(transitionBitmap);
        if (prefs.theme().isLightTheme)
            background.setBackgroundColor(revealColor);

        setupViewPager();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().getSharedElementEnterTransition().addListener(new TransitionListenerAdapter() {
                @Override public void onTransitionEnd(Transition transition) {
                    if (savedInstanceState == null) {
                        makeToolbarShadowAppear();
                        makeViewPagerAppear();
                    }
                }
            });
        } else {
            if (savedInstanceState == null) {
                makeToolbarShadowAppear();
                makeViewPagerAppear();
            }
        }

        // Make activity full screen with fully transparent navigation and status bars
        // http://developer.android.com/reference/android/view/Window.html#setNavigationBarColor(int)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
            Window window = getWindow();
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
            setTopPadding(drawer, getStatusBarHeight());
            // TODO: The following should only be set if savedinstancestate == null and saved in onsaveinstance
            initialStatusBarColor = window.getStatusBarColor();
            targetStatusBarColor = getResources().getColor(R.color.transparent);
            window.setStatusBarColor(targetStatusBarColor);
            initialNavBarColor = window.getNavigationBarColor();
            targetNavBarColor = getResources().getColor(R.color.transparent);
            window.setNavigationBarColor(targetNavBarColor);
        }

        // Workarounds

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // Otherwise toolbar shadow stays in same position (looks ugly)
            getWindow().getSharedElementReturnTransition().addListener(new TransitionListenerAdapter() {
                @Override public void onTransitionStart(Transition transition) {
                    toolbarShadow.setImageDrawable(null);
                }
            });

            // Workaround for glitchy toolbar shared return transition
            int statusBarHeight = getStatusBarHeight();
            toolbar.setTranslationY(statusBarHeight);
            toolbarShadow.setTranslationY(statusBarHeight);

            // http://stackoverflow.com/a/26748694/283607
            Transition fade = TransitionInflater.from(this).inflateTransition(android.R.transition.fade);
            fade.excludeTarget(R.id.revealbg, true);
            getWindow().setExitTransition(fade);
            getWindow().setEnterTransition(fade);

            if (savedInstanceState == null) {
                // http://stackoverflow.com/a/26861930/283607
                background.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
                    @Override public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                        if (Build.VERSION.SDK_INT >= 21) v.removeOnLayoutChangeListener(this);
                        revealBackground();
                    }
                });
            } else {
                background.setVisibility(View.VISIBLE);
            }
        } else {
            background.setVisibility(View.VISIBLE);
        }

        // http://stackoverflow.com/a/26748694/283607
        final ViewTreeObserver observer = getWindow().getDecorView().getViewTreeObserver();
        observer.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                // java.lang.IllegalStateException: This ViewTreeObserver is not alive, call getViewTreeObserver() again
                ViewTreeObserver observer2 = observer.isAlive() ? observer : getWindow().getDecorView().getViewTreeObserver();
                observer2.removeOnPreDrawListener(this);
                supportStartPostponedEnterTransition();
                return true;
            }
        });
    }

    @Override protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(EXTRA_INDEX, currentIndex);
        // TODO: save photoattacher state (zoom etc.)
    }

    private void setupViewPager() {
        // MediaViews are recycled
        pagerViews = new MediaView[4]; // Didn't manage to make it work with 3
        for (int i = 0; i < pagerViews.length; i++) {
            pagerViews[i] = (MediaView) LayoutInflater.from(this).inflate(R.layout.view_media, null);
            pagerViews[i].ion = ion;
            pagerViews[i].background = background;
            pagerViews[i].releaseIndicator = releaseIndicator;
            pagerViews[i].viewPager = viewPager;
        }

        viewPager.setAdapter(new MediaPagerAdapter());
        viewPager.setCurrentItem(currentIndex);
        viewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) { }
            @Override public void onPageScrollStateChanged(int state) { }
            @Override public void onPageSelected(int position) {
                currentIndex = position;
                MediaPointer current = mediaPointers.get(currentIndex);
                MediaView currentView = pagerView(currentIndex);
                currentView.prefs = prefs;
                currentView.setTransitionNameForImageView(currentIndex+"");
                currentView.bindTo(boardName, current);
                // Free bitmaps to be collected
                int l = pagerViews.length;
                for (int i = 0; i < l; i++) {
                    if (i != currentIndex % l) {
                        MediaView otherView = pagerView(i);
                        otherView.setTransitionNameForImageView("");
                        MediaPointer last = otherView.lastMediaPointer;
                        if (last != null) {
                            otherView.reset();
                            otherView.bindToPreview(boardName, last);
                        }
                    }
                }
            }
        });

        final MediaPointer current = mediaPointers.get(currentIndex);
        final MediaView currentView = pagerView(currentIndex);
        currentView.prefs = prefs;
        viewPager.postDelayed(new Runnable() {
            @Override public void run() {
                if (transitionBitmap != null) currentView.bindToPreview(transitionBitmap);
                transitionBitmap = null;
                currentView.setTransitionNameForImageView(currentIndex + "");
                currentView.bindTo(boardName, current);
            }
        }, 10);
    }

    private MediaView pagerView(int i) {
        return pagerViews[i % pagerViews.length];
    }

    private void makeToolbarShadowAppear() {
        // Delay as otherwise there is no shadow under overflow options icon
        toolbar.postDelayed(new Runnable() {
            @Override public void run() {
                updateToolbarShadow();
            }
        }, 50);
    }

    private void makeViewPagerAppear() {
        // Without delay it doesn't work
        imageViewForTransition.postDelayed(new Runnable() {
            @Override public void run() {
                viewPager.setVisibility(View.VISIBLE);
                imageViewForTransition.setVisibility(View.GONE);
            }
        }, 80);
    }

    // Lifecycle ///////////////////////////////////////////////////////////////////////////////////

    @Override public void onResume() {
        super.onResume();
        toolbar.getBackground().setAlpha(0);
    }

    @Override public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        toolbar.postDelayed(new Runnable() {
            @Override public void run() {
                updateToolbarShadow();
            }
        }, 100);
    }

    @Override public void onDestroy() {
        transitionBitmap = null;
        toolbarShadow.setImageBitmap(null);
        System.gc();
        super.onDestroy();
    }

    @Override public void onBackPressed() {
        if (Build.VERSION.SDK_INT >= 21) {
            // Such that the image flies back from left/right back into its former position
            // in the previous activity depending on the direction the user swiped to.
            float translationAmount = 1 + Math.abs(currentIndex-initialIndex) * 0.1f;
            int screenOffset = Util.getScreenWidth(this);
            if (initialIndex < currentIndex) {
                imageViewForTransition.setTranslationX(-screenOffset*translationAmount);
                imageViewForTransition.setVisibility(View.VISIBLE);
            } else if (currentIndex < initialIndex) {
                imageViewForTransition.setTranslationX(+screenOffset*translationAmount);
                imageViewForTransition.setVisibility(View.VISIBLE);
            } else {
                MediaView currentView = pagerView(currentIndex);
                currentView.setTransitionNameForImageView(transitionName);
            }

            Drawable bg = toolbar.getBackground();
            int alpha = 0;
            if (Build.VERSION.SDK_INT >= 21) alpha = bg.getAlpha();
            animateAlpha(bg, alpha, 255, null);
            animateAlpha(toolbarShadow, 255, 0, new Runnable() {
                @Override public void run() {
                    toolbarShadow.setImageBitmap(null);
                }
            });

            // Workaround for glitchy nav bar shared return transition
            final Window window = getWindow();
            ValueAnimator colorAnimation = ValueAnimator.ofArgb(targetNavBarColor, initialNavBarColor);
            colorAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @TargetApi(Build.VERSION_CODES.LOLLIPOP)
                @Override public void onAnimationUpdate(ValueAnimator animation) {
                    window.setNavigationBarColor((int) animation.getAnimatedValue());
                }
            });
            colorAnimation.setDuration(400);
            colorAnimation.start();
            // It's funny. If I place this statement before colorAnimation.start()
            // or omit it entirely then the transition keeps being glitchy.
            window.setNavigationBarColor(initialNavBarColor);

            // Workaround for glitchy status bar shared return transition
            colorAnimation = ValueAnimator.ofArgb(targetStatusBarColor, initialStatusBarColor);
            colorAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @TargetApi(Build.VERSION_CODES.LOLLIPOP)
                @Override public void onAnimationUpdate(ValueAnimator animation) {
                    window.setStatusBarColor((int) animation.getAnimatedValue());
                }
            });
            colorAnimation.setDuration(400);
            colorAnimation.start();
            window.setStatusBarColor(initialStatusBarColor);

            // Otherwise background is removed too early and the return reveal animation glitches
            getWindow().setTransitionBackgroundFadeDuration(500);
            hideBackground();
        }
        super.onBackPressed();
    }

    private void updateToolbarShadow() {
        if (Build.VERSION.SDK_INT < 11) return;
        try {
            toolbar.setDrawingCacheEnabled(true);
            toolbar.buildDrawingCache();
            Bitmap bm = toolbar.getDrawingCache();
            if (bm == null) {
                toolbar.setDrawingCacheEnabled(false);
                return;
            }
            if (Build.VERSION.SDK_INT < 12) bm = Util.setHasAlphaCompat(bm);
            else bm.setHasAlpha(true);
            toolbarShadow.setImageBitmap(Util.blur(this, bm, 1f));
            toolbarShadow.setColorFilter(Color.argb(255, 0, 0, 0));
            toolbar.setDrawingCacheEnabled(false);
        } catch (RSRuntimeException ignored) {
            // Simply do not draw a shadow for devices that can't handle it
        }
    }

    // Toolbar Menu ////////////////////////////////////////////////////////////////////////////////

    @Override public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.media, menu);
        return true;
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        MediaPointer current = mediaPointers.get(currentIndex);
        String url = ApiModule.imgUrl(boardName, current.id, current.ext);
        Intent intent;
        String path;
        // https://github.com/codepath/android_guides/wiki/Sharing-Content-with-Intents#sharing-remote-images-without-explicit-file-io
        switch(item.getItemId()) {
            case R.id.view:
                intent = new Intent();
                intent.setAction(Intent.ACTION_VIEW);
                if (current.isWebm()) {
                    Util.startWebmActivity(this, url);
                } else {
                    // TODO: This is not good, download asynchronously
                    Bitmap bitmap;
                    try {
                        // TODO: Write directly into Mediastore! Out of Memory possible
                        bitmap = ion.build(this).load(url).asBitmap().get();
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                        return true;
                    }
                    if (bitmap == null) return true;
                    path = MediaStore.Images.Media.insertImage(getContentResolver(), bitmap, null, null);
                    intent.putExtra(Intent.EXTRA_STREAM, Uri.parse(path));
                    intent.setType("image/*");
                    startActivity(Intent.createChooser(intent, getResources().getText(R.string.view_image)));
                }
                break;
            case R.id.share:
                intent = new Intent();
                intent.setAction(Intent.ACTION_SEND);
                if (current.isWebm()) {
                    // TODO: Webm must be downloaded
                    intent.setDataAndType(Uri.parse(url), "video/webm");
                    startActivity(Intent.createChooser(intent, getResources().getText(R.string.share_video)));
                } else {
                    // TODO: This is not good, download asynchronously
                    Bitmap bitmap;
                    try {
                        // TODO: Write directly into Mediastore! Out of Memory possible
                        bitmap = ion.build(this).load(url).asBitmap().get();
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                        return true;
                    }
                    if (bitmap == null) return true;
                    path = MediaStore.Images.Media.insertImage(getContentResolver(), bitmap, null, null);
                    intent.putExtra(Intent.EXTRA_STREAM, Uri.parse(path));
                    intent.setType("image/*");
                    startActivity(Intent.createChooser(intent, getResources().getText(R.string.share_image)));
                }
                break;
            case R.id.showPost:
                // TODO
                break;
            case R.id.gotoPost:
                // TODO
                break;
            case R.id.gallery:
                if (fromGallery) {
                    onBackPressed();
                } else {
                    // TODO: Shared element transition
                    // I find it weird that the background drawable of the toolbar is for some
                    // reason shared between activities in Lollipop... (if I don't set it to 255 it
                    // remains translucent in the next activity)
                    toolbar.getBackground().setAlpha(255);
                    GalleryActivity.launch(this, boardName, threadNumber, mediaPointers);
                    ActivityCompat.finishAfterTransition(this);
                }
                break;
            case R.id.save:

                Bitmap bitmap;
                try {
                    // TODO: Write directly into Mediastore! Out of Memory possible
                    bitmap = ion.build(this).load(url).asBitmap().get();
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                    return true;
                }

                MediaStore.Images.Media.insertImage(getContentResolver(), bitmap, UUID.randomUUID().toString() , "");
                Toast.makeText( this, getString( R.string.image_saved_to_gallery ), Toast.LENGTH_SHORT ).show();
                break;
        }
        
        return super.onOptionsItemSelected(item);
    }

    // Animations //////////////////////////////////////////////////////////////////////////////////

    // https://developer.android.com/training/material/animations.html#Reveal
    @TargetApi(Build.VERSION_CODES.LOLLIPOP) private void revealBackground() {
        int cx = revealPoint.x;
        int cy = revealPoint.y;
        int finalRadius = Math.max(background.getWidth(), background.getHeight());
        Animator anim = ViewAnimationUtils.createCircularReveal(
                background, cx, cy, revealRadius, finalRadius
        );
        anim.setDuration(300);
        background.setVisibility(View.VISIBLE);
        anim.start();
    }
    @TargetApi(Build.VERSION_CODES.LOLLIPOP) private void hideBackground() {
        int cx = revealPoint.x;
        int cy = revealPoint.y;
        int initialRadius = Math.max(background.getWidth(), background.getHeight());
        Animator anim = ViewAnimationUtils.createCircularReveal(
                background, cx, cy, initialRadius, revealRadius
        );
        anim.setDuration(300);
        anim.setStartDelay(100);
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                background.setVisibility(View.INVISIBLE);
            }
        });
        anim.start();
    }

    // Java's abstraction capabilities are limiting! I cannot reduce code size without a performance hit.
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void animateAlpha(final Drawable d, int from, int to, final Runnable onFinish) {
        ValueAnimator v = ValueAnimator.ofInt(from, to);
        v.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override public void onAnimationUpdate(ValueAnimator animator) {
                d.setAlpha((int) animator.getAnimatedValue());
            }
        });
        if (onFinish != null) {
            v.addListener(new AnimatorListenerAdapter() {
                @Override public void onAnimationEnd(Animator animation) {
                    onFinish.run();
                }
            });
        }
        v.setDuration(400);
        v.start();
    }
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void animateAlpha(final View d, int from, int to, final Runnable onFinish) {
        ValueAnimator v = ValueAnimator.ofInt(from, to);
        v.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override public void onAnimationUpdate(ValueAnimator animator) {
                d.setAlpha((int) animator.getAnimatedValue());
            }
        });
        if (onFinish != null) {
            v.addListener(new AnimatorListenerAdapter() {
                @Override public void onAnimationEnd(Animator animation) {
                    onFinish.run();
                }
            });
        }
        v.setDuration(400);
        v.start();
    }

    // Utility /////////////////////////////////////////////////////////////////////////////////////

    public int getStatusBarHeight() {
        int result = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    public void setTopPadding(View v, int padding) {
        v.setPadding(v.getPaddingLeft(), padding, v.getPaddingRight(), v.getPaddingBottom());
    }

    // TODO: From http://developer.android.com/reference/android/support/v4/view/PagerAdapter.html
    /*
    PagerAdapter supports data set changes. Data set changes must occur on the main thread and must
    end with a call to notifyDataSetChanged() similar to AdapterView adapters derived from
    BaseAdapter. A data set change may involve pages being added, removed, or changing position.
    The ViewPager will keep the current page active provided the adapter implements the method
    getItemPosition(Object).
     */
    // I.e., when post and thus images are updated in the background notifyDataSetChanged should
    // be called on this adapter so that new images are added to the right. Related to manager object.
    private class MediaPagerAdapter extends PagerAdapter {

        @Override public Object instantiateItem(ViewGroup container, int position) {
            MediaView mediaView = pagerView(position);
            mediaView.prefs = prefs;
            MediaPointer current = mediaPointers.get(position);
            mediaView.bindToPreview(boardName, current);
            if (container.indexOfChild(mediaView) != -1) container.removeView(mediaView);
            container.addView(mediaView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            return mediaView;
        }

        @Override public void destroyItem(ViewGroup container, int position, Object object) {
            MediaView mediaView = pagerView(position);
            mediaView.reset();
            container.removeView(mediaView);
        }

        @Override public int getCount() { return mediaPointers.size(); }
        @Override public boolean isViewFromObject(View view, Object object) { return view == object; }
    }

}
