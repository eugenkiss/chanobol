package anabolicandroids.chanobol.ui.images;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.util.Pair;
import android.support.v4.view.ViewCompat;
import android.support.v8.renderscript.RSRuntimeException;
import android.transition.Transition;
import android.transition.TransitionInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.ImageViewBitmapInfo;
import com.koushikdutta.ion.builder.Builders;

import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import anabolicandroids.chanobol.R;
import anabolicandroids.chanobol.api.ApiModule;
import anabolicandroids.chanobol.ui.UiActivity;
import anabolicandroids.chanobol.util.TransitionListenerAdapter;
import anabolicandroids.chanobol.util.Util;
import butterknife.InjectView;
import uk.co.senab.photoview.PhotoViewAttacher;

// The following is the problem I was facing all the time:
// https://plus.google.com/app/basic/photos/108918686796284921012/album/6082900202909178977/6082900205521388114
public class ImageActivity extends UiActivity {

    private static int THRESHOLD_IMG_SIZE = 3000;

    // Construction ////////////////////////////////////////////////////////////////////////////////

    // Transition related
    private static String EXTRA_TRANSITIONNAME = "transitionName";
    private static String EXTRA_REVEALPOINT = "revealPoint";
    private static String EXTRA_REVEALRADIUS = "revealRadius";
    private static String EXTRA_FROM_THUMBNAIL = "fromThumbnail";
    private static String EXTRA_FROM_GALLERY = "fromGallery";
    private Point revealPoint;
    private int revealRadius;
    @SuppressWarnings("FieldCanBeLocal")
    private boolean fromThumbnail;
    private boolean fromGallery;

    // Needed to form a valid request to 4Chan
    private static String EXTRA_BOARDNAME = "boardName";
    private static String EXTRA_THREADNUMBER = "threadNumber";
    private String boardName;
    private String threadNumber;

    // Currently not really used (has always length 1), but the idea is to have swipes to
    // the left/right reveal the next/previous image, see issue #85
    private static String EXTRA_INDEX = "index";
    private static String EXTRA_IMAGEPOINTERS = "imagePointers";
    @SuppressWarnings("FieldCanBeLocal")
    private int index;
    private List<ImagePointer> imagePointers;

    // Non-local lifetime
    private ImagePointer current;
    private String url;
    private Bitmap bm; // Workaround for preventing transition glitches
    private Bitmap bitmap; // For sharing
    private boolean loaded;
    private String bitmapCacheKey;
    // To prevent transition glitches
    private int initialStatusBarColor;
    private int targetStatusBarColor;
    private int initialNavBarColor;
    private int targetNavBarColor;

    public static void launch(
            Activity activity, View transitionView, String transitionName,
            Point revealPoint, int revealStartRadius,
            boolean fromThumbnail, boolean fromGallery,
            String boardName, String threadNumber,
            int index, ArrayList<ImagePointer> imagePointers
    ) {
        ViewCompat.setTransitionName(transitionView, transitionName);
        ActivityOptionsCompat options = makeSceneTransitionAnimation(activity,
                Pair.create(transitionView, transitionName)
        );
        Intent intent = new Intent(activity, ImageActivity.class);
        intent.putExtra(EXTRA_TRANSITIONNAME, transitionName);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            intent.putExtra(EXTRA_REVEALPOINT, revealPoint);
        } else {
            // Do nothing because of 2.3.7's:
            // "java.lang.RuntimeException: Parcel: unable to marshal value Point..."
        }
        intent.putExtra(EXTRA_REVEALRADIUS, revealStartRadius);
        intent.putExtra(EXTRA_FROM_THUMBNAIL, fromThumbnail);
        intent.putExtra(EXTRA_FROM_GALLERY, fromGallery);
        intent.putExtra(EXTRA_BOARDNAME, boardName);
        intent.putExtra(EXTRA_THREADNUMBER, threadNumber);
        intent.putExtra(EXTRA_INDEX, index);
        intent.putExtra(EXTRA_IMAGEPOINTERS, Parcels.wrap(imagePointers));
        ActivityCompat.startActivity(activity, intent, options.toBundle());
    }

    @InjectView(R.id.revealbg) View background;
    @InjectView(R.id.releaseIndicator) View releaseIndicator;
    @InjectView(R.id.imageView) ImageView imageView;
    @InjectView(R.id.play) ImageView play;
    @InjectView(R.id.progressbar) ProgressBar progressbar;
    @SuppressWarnings("UnusedDeclaration") private PhotoViewAttacher attacher;

    @Override protected int getLayoutResource() { return R.layout.activity_image; }

    @Override protected void onCreate(Bundle savedInstanceState) {
        supportPostponeEnterTransition();
        super.onCreate(savedInstanceState);
        background.getBackground().setAlpha(255);
        setTitle("");

        Bundle b = getIntent().getExtras();
        String transitionName = b.getString(EXTRA_TRANSITIONNAME);
        revealPoint   = b.getParcelable(EXTRA_REVEALPOINT);
        revealRadius  = b.getInt(EXTRA_REVEALRADIUS) / 2;
        boardName     = b.getString(EXTRA_BOARDNAME);
        threadNumber  = b.getString(EXTRA_THREADNUMBER);
        index         = b.getInt(EXTRA_INDEX);
        imagePointers = Parcels.unwrap(b.getParcelable(EXTRA_IMAGEPOINTERS));
        fromThumbnail = b.getBoolean(EXTRA_FROM_THUMBNAIL);
        fromGallery   = b.getBoolean(EXTRA_FROM_GALLERY);

        current = imagePointers.get(index);
        url = ApiModule.imgUrl(boardName, current.id, current.ext);
        String thumbUrl = ApiModule.thumbUrl(boardName, current.id);
        // Blocking load s.t. imageView is initialized before transition begins to avoid glitches.
        // This is not a big problem because Ion has loaded the image previously.
        // Note: Somehow Ion's future callback wasn't called when postponeEnterTransaition was
        // used. Otherwise I would have put startPostPonedEnterTransition there!
        // Curiosly, it did work once with a  gifs... see https://github.com/koush/ion/issues/486
        ViewCompat.setTransitionName(imageView, transitionName);
        try {
            // This is actually problematic for big images -> can lead to out of memory error because
            // bm is not resized. But if I resize it the following call apparently takes too long
            // and the activity crashes because of this... So use a compromise.
            boolean useThumbnail = fromThumbnail || current.isWebm() ||
                    current.w > THRESHOLD_IMG_SIZE || current.h > THRESHOLD_IMG_SIZE;
            bm = ion.build(this).load(useThumbnail ? thumbUrl : url).asBitmap().get();
            imageView.setImageBitmap(bm);
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        // Setup main view
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().getSharedElementEnterTransition().addListener(new TransitionListenerAdapter() {
                @Override public void onTransitionEnd(Transition transition) {
                    if (current.isWebm()) setupWebm();
                    else setupImage();
                    postSetup();
                }
            });
        } else {
            if (current.isWebm()) setupWebm();
            else setupImage();
            postSetup();
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
            initialStatusBarColor = window.getStatusBarColor();
            targetStatusBarColor = getResources().getColor(R.color.transparent);
            window.setStatusBarColor(targetStatusBarColor);
            initialNavBarColor = window.getNavigationBarColor();
            targetNavBarColor = getResources().getColor(R.color.transparent);
            window.setNavigationBarColor(targetNavBarColor);
            setTopPadding(drawer, getStatusBarHeight());
        }

        // Workarounds

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().getSharedElementReturnTransition().addListener(new TransitionListenerAdapter() {
                @Override public void onTransitionStart(Transition transition) {
                    toolbarShadow.setImageDrawable(null);
                }
            });
            // Workaround for glitchy toolbar shared return transition
            int statusBarHeight = getStatusBarHeight();
            toolbar.setTranslationY(statusBarHeight);
            toolbarShadow.setTranslationY(statusBarHeight);
        }

        // http://stackoverflow.com/a/26748694/283607
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Transition fade = TransitionInflater.from(this).inflateTransition(android.R.transition.fade);
            fade.excludeTarget(R.id.revealbg, true);
            getWindow().setExitTransition(fade);
            getWindow().setEnterTransition(fade);
        }

        // http://stackoverflow.com/a/26861930/283607
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            background.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
                @Override public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                    if (Build.VERSION.SDK_INT >= 21) v.removeOnLayoutChangeListener(this);
                    revealBackground();
                }
            });
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

    private void setupWebm() {
        loaded = true;
        progressbar.setVisibility(View.GONE);
        play.setVisibility(View.VISIBLE);
        setupPhotoAttacher();
        attacher.setOnViewTapListener(new PhotoViewAttacher.OnViewTapListener() {
            @Override public void onViewTap(View view, float x, float y) {
                Util.startWebmActivity(ImageActivity.this, url);
            }
        });
    }

    private void setupImage() {
        Builders.IV.F<?> b = ion.build(imageView)
                // Placeholder _and_ crossfade to prevent blinking
                .crossfade(true)
                .placeholder(new BitmapDrawable(resources, bm));
                if (current.w > THRESHOLD_IMG_SIZE || current.h > THRESHOLD_IMG_SIZE) b = b.deepZoom(); b
                .load(url)
                .withBitmapInfo()
                .setCallback(new FutureCallback<ImageViewBitmapInfo>() {
                    @Override public void onCompleted(Exception e, ImageViewBitmapInfo result) {
                        if (e != null) return;
                        loaded = true;
                        progressbar.setVisibility(View.GONE);
                        setupPhotoAttacher();
                        if (result.getBitmapInfo() != null) {
                            bitmap = result.getBitmapInfo().bitmap;
                            bitmapCacheKey = result.getBitmapInfo().key;
                        }
                    }
                });
    }

    private void postSetup() {
        // Only show progressbar if loading takes a bit more time
        if (fromThumbnail) {
            progressbar.postDelayed(new Runnable() {
                @Override public void run() {
                    if (!loaded) progressbar.setVisibility(View.VISIBLE);
                }
            }, 200);
        }
        // Delay as otherwise there is no shadow under overflow options icon
        toolbar.postDelayed(new Runnable() {
            @Override public void run() {
                updateToolbarShadow();
            }
        }, 50);
    }

    private float releaseScaleThresholdBuffer = 0.005f;
    private float releaseScaleThreshold = 0.515f;
    private Matrix previousMatrix;
    private void setupPhotoAttacher() {
        attacher = new PhotoViewAttacher(imageView);
        attacher.setMaximumScale(25);
        attacher.setOnMatrixChangeListener(new PhotoViewAttacher.OnMatrixChangedListener() {
            @Override public void onMatrixChanged(RectF rect) {
                double s = attacher.getScale();
                if (s < releaseScaleThreshold) {
                    attacher.setOnMatrixChangeListener(null);
                    previousMatrix.setScale(releaseScaleThreshold, releaseScaleThreshold);
                    attacher.setDisplayMatrix(previousMatrix);
                    attacher.setOnMatrixChangeListener(this);
                }
                background.getBackground().setAlpha(Util.clamp(50, 255*s*Math.sqrt(s)*1.3, 255));
                setVisibility(releaseIndicator, s <= releaseScaleThreshold + releaseScaleThresholdBuffer);
                previousMatrix = attacher.getDisplayMatrix();
            }
        });
    }

    @Override public boolean dispatchTouchEvent(@NonNull MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
            if (attacher != null && attacher.getScale() <= releaseScaleThreshold + releaseScaleThresholdBuffer) {
                onBackPressed();
                return true;
            }
        }
        return super.dispatchTouchEvent(event);
    }

    @Override public void onBackPressed() {
        if (Build.VERSION.SDK_INT >= 21) {
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
        toolbarShadow.setImageBitmap(null);
        if (bitmapCacheKey != null) ion.getBitmapCache().remove(bitmapCacheKey);
        System.gc();
        super.onDestroy();
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
        getMenuInflater().inflate(R.menu.image, menu);
        return true;
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        String path;
        // https://github.com/codepath/android_guides/wiki/Sharing-Content-with-Intents#sharing-remote-images-without-explicit-file-io
        switch(item.getItemId()) {
            case R.id.openExternal:
                if (bitmap == null) break;
                intent = new Intent();
                intent.setAction(Intent.ACTION_VIEW);
                if (current.isWebm()) {
                    Util.startWebmActivity(this, url);
                } else {
                    // I actually want to share the remote URL and have the resulting application
                    // download the image itself especially because the image here might have been
                    // resized or because of deep zoom. Alas, it does not seem to be easy to solve...
                    // ...unless I would try to download the image into the media store (if it
                    // takes too much time simply share the current bitmap) if it has been resized or
                    // deep zoom is used and then share that downloaded bitmap (but do _not_ load it
                    // into RAM!).
                    path = MediaStore.Images.Media.insertImage(getContentResolver(), bitmap, null, null);
                    intent.putExtra(Intent.EXTRA_STREAM, Uri.parse(path));
                    intent.setType("image/*");
                    startActivity(Intent.createChooser(intent, getResources().getText(R.string.view_image)));
                }
                break;
            case R.id.share:
                if (bitmap == null) break;
                intent = new Intent();
                intent.setAction(Intent.ACTION_SEND);
                if (current.isWebm()) {
                    // TODO: Webm must be downloaded
                    intent.setDataAndType(Uri.parse(url), "video/webm");
                    startActivity(Intent.createChooser(intent, getResources().getText(R.string.share_video)));
                } else {
                    path = MediaStore.Images.Media.insertImage(getContentResolver(), bitmap, null, null);
                    intent.putExtra(Intent.EXTRA_STREAM, Uri.parse(path));
                    intent.setType("image/*");
                    startActivity(Intent.createChooser(intent, getResources().getText(R.string.share_image)));
                }
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
                    GalleryActivity.launch(this, boardName, threadNumber, imagePointers);
                    ActivityCompat.finishAfterTransition(this);
                }
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

}
