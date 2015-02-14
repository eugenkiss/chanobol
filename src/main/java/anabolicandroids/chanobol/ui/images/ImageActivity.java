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
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.util.Pair;
import android.support.v4.view.ViewCompat;
import android.support.v8.renderscript.RSRuntimeException;
import android.transition.Transition;
import android.transition.TransitionInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.koushikdutta.async.future.FutureCallback;

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

    // Construction ////////////////////////////////////////////////////////////////////////////////

    private static String EXTRA_TRANSITIONNAME = "transitionName";
    private static String EXTRA_REVEALPOINT = "revealPoint";
    private static String EXTRA_REVEALRADIUS = "revealRadius";
    private static String EXTRA_FROM_THUMBNAIL = "fromThumbnail";
    // Needed to form a valid request to 4Chan
    private static String EXTRA_BOARDNAME = "boardName";
    private static String EXTRA_THREADNUMBER = "threadNumber";
    // Currently not really used (has always length 1), but the idea is to have swipes to
    // the left/right reveal the next/previous image, see issue #85
    private static String EXTRA_INDEX = "index";
    private static String EXTRA_IMAGEPOINTERS = "imagePointers";

    private String url;
    private Bitmap bm;
    private boolean loaded;
    private Point revealPoint;
    private int revealRadius;
    private boolean fromThumbnail;

    public static void launch(
            Activity activity, View transitionView, String transitionName,
            Point revealPoint, int revealStartRadius,
            boolean fromThumbnail, String boardName, String threadNumber,
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
        intent.putExtra(EXTRA_BOARDNAME, boardName);
        intent.putExtra(EXTRA_THREADNUMBER, threadNumber);
        intent.putExtra(EXTRA_INDEX, index);
        intent.putExtra(EXTRA_IMAGEPOINTERS, Parcels.wrap(imagePointers));
        ActivityCompat.startActivity(activity, intent, options.toBundle());
    }

    @InjectView(R.id.revealbg) View background;
    @InjectView(R.id.imageView) ImageView imageView;
    @InjectView(R.id.progressbar) ProgressBar progressbar;
    @SuppressWarnings("UnusedDeclaration") private PhotoViewAttacher attacher;

    @Override protected int getLayoutResource() { return R.layout.activity_image; }

    // TODO: May be refactored
    @Override protected void onCreate(Bundle savedInstanceState) {
        supportPostponeEnterTransition();
        super.onCreate(savedInstanceState);
        setTitle("");

        Bundle b = getIntent().getExtras();
        String transitionName = b.getString(EXTRA_TRANSITIONNAME);
        revealPoint = b.getParcelable(EXTRA_REVEALPOINT);
        revealRadius = b.getInt(EXTRA_REVEALRADIUS) / 2;
        String boardName = b.getString(EXTRA_BOARDNAME);
        //noinspection UnusedDeclaration
        String threadNumber = b.getString(EXTRA_THREADNUMBER);
        int index = b.getInt(EXTRA_INDEX);
        List<ImagePointer> imagePointers = Parcels.unwrap(b.getParcelable(EXTRA_IMAGEPOINTERS));
        fromThumbnail = b.getBoolean(EXTRA_FROM_THUMBNAIL);

        ImagePointer imagePointer = imagePointers.get(index);
        url = ApiModule.imgUrl(boardName, imagePointer.id, imagePointer.ext);
        String thumbUrl = ApiModule.thumbUrl(boardName, imagePointer.id);
        // Blocking load s.t. imageView is initialized before transition begins to avoid glitches.
        // This is not a big problem because Ion has loaded the image previously.
        // Note: Somehow Ion's future callback wasn't called when postponeEnterTransaition was
        // used. Otherwise I would have put startPostPonedEnterTransition there!
        // Curiosly, it did work once with a  gifs... see https://github.com/koush/ion/issues/486
        ViewCompat.setTransitionName(imageView, transitionName);
        try {
            bm = ion.build(this).load(fromThumbnail ? thumbUrl : url).asBitmap().get();
            imageView.setImageBitmap(bm);
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().getSharedElementEnterTransition().addListener(new TransitionListenerAdapter() {
                @Override public void onTransitionEnd(Transition transition) {
                    setup();
                }
            });
        } else {
            setup();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().getSharedElementReturnTransition().addListener(new TransitionListenerAdapter() {
                @Override public void onTransitionStart(Transition transition) {
                    toolbarShadow.setImageDrawable(null);
                }
            });
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

    private void setup() {
        ion.build(imageView)
                // Placeholder _and_ crossfade to prevent blinking
                .crossfade(true)
                .placeholder(new BitmapDrawable(resources, bm))
                .deepZoom()
                .load(url)
                .setCallback(new FutureCallback<ImageView>() {
                    @Override public void onCompleted(Exception e, ImageView result) {
                        loaded = true;
                        progressbar.setVisibility(View.GONE);
                        attacher = new PhotoViewAttacher(imageView);
                        // TODO: Makes application break after a while of zooming in and out
                        //imageView.setMaximumScale(25); // Default value is too small for some images
                    }
                });
        if (fromThumbnail) {
            // Only show progressbar if loading takes a bit more time
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
        super.onDestroy();
        toolbarShadow.setImageBitmap(null);
    }

    private void updateToolbarShadow() {
        try {
            toolbar.setDrawingCacheEnabled(true);
            toolbar.buildDrawingCache();
            Bitmap bm = toolbar.getDrawingCache();
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
        switch(item.getItemId()) {
            case R.id.openExternal:
                intent = new Intent();
                intent.setAction(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.parse(url), "image/*");
                if(intent.resolveActivity(getPackageManager()) != null) startActivity(intent);
                else showToast("No suitable app");
                break;
            case R.id.chooseExternal:
                intent = new Intent();
                intent.setAction(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.parse(url), "image/*");
                startActivity(Intent.createChooser(intent, "Open image with"));
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
        int initialRadius = background.getWidth();
        Animator anim = ViewAnimationUtils.createCircularReveal(
                background, cx, cy, initialRadius, revealRadius
        );
        anim.setDuration(350);
        anim.setStartDelay(250);
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
}
