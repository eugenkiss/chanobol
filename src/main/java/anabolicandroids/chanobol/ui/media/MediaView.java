package anabolicandroids.chanobol.ui.media;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;
import com.koushikdutta.ion.builder.Builders;

import anabolicandroids.chanobol.R;
import anabolicandroids.chanobol.api.ApiModule;
import anabolicandroids.chanobol.util.HackyViewPager;
import anabolicandroids.chanobol.util.Util;
import butterknife.ButterKnife;
import butterknife.InjectView;
import uk.co.senab.photoview.PhotoViewAttacher;

public class MediaView extends FrameLayout {

    private static final int THRESHOLD_IMG_SIZE = 3000;

    public Ion ion;
    public View background;
    public View releaseIndicator;
    public HackyViewPager viewPager;

    @InjectView(R.id.imageView) ImageView imageView;
    @InjectView(R.id.play) ImageView play;
    @InjectView(R.id.progressbar) ProgressBar progressbar;
    @SuppressWarnings("UnusedDeclaration") private PhotoViewAttacher attacher;

    public MediaPointer lastMediaPointer;
    boolean loaded;
    // I observed that sometimes the higher quality media is not loaded after a swipe.
    // I speculate that due to asynchronous timing issues a call to bindToPreview comes
    // after bindTo and "overwrites" it. This is a rough and speculative fix and another
    // approach (e.g. Rx) approach would probably be more principled.
    public boolean higherQualityAlreadyLoading;

    public MediaView(Context context) {
        super(context);
    }
    public MediaView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    public MediaView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }
    @SuppressWarnings("UnusedDeclaration") @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public MediaView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override protected void onFinishInflate() {
        super.onFinishInflate();
        ButterKnife.inject(this);
    }

    @Override protected void onDetachedFromWindow() {
        if (attacher != null) attacher.cleanup();
        super.onDetachedFromWindow();
    }

    public void reset() {
        lastMediaPointer = null;
        loaded = false;
        higherQualityAlreadyLoading = false;
    }

    public void setTransitionNameForImageView(String name) {
        ViewCompat.setTransitionName(imageView, name);
    }

    public void bindToPreview(Bitmap bitmap) {
        imageView.setImageBitmap(bitmap);
        if (attacher != null) attacher.cleanup();
        attacher = new PhotoViewAttacher(imageView);
    }

    public void bindToPreview(String boardName, MediaPointer mediaPointer) {
        if (higherQualityAlreadyLoading) return;
        lastMediaPointer = mediaPointer;

        progressbar.setVisibility(View.GONE);
        Util.setVisibility(play, mediaPointer.isWebm());

        imageView.setImageBitmap(null);
        String thumbUrl = ApiModule.thumbUrl(boardName, mediaPointer.id);
        ion.build(imageView).crossfade(true).load(thumbUrl).setCallback(new FutureCallback<ImageView>() {
            @Override public void onCompleted(Exception e, ImageView result) {
                if (attacher != null) attacher.cleanup();
                attacher = new PhotoViewAttacher(imageView);
            }
        });
    }

    public void bindTo(String boardName, MediaPointer mediaPointer) {
        higherQualityAlreadyLoading = true;
        lastMediaPointer = mediaPointer;

        final String url = ApiModule.imgUrl(boardName, mediaPointer.id, mediaPointer.ext);

        if (mediaPointer.isWebm()) {
            progressbar.setVisibility(View.GONE);
            play.setVisibility(View.VISIBLE);

            String thumbUrl = ApiModule.thumbUrl(boardName, mediaPointer.id);
            ion.build(imageView).load(thumbUrl).setCallback(new FutureCallback<ImageView>() {
                @Override public void onCompleted(Exception e, ImageView result) {
                    if (e != null) return;
                    setupPhotoAttacher();
                    attacher.setOnViewTapListener(new PhotoViewAttacher.OnViewTapListener() {
                        @Override public void onViewTap(View view, float x, float y) {
                            Util.startWebmActivity(getContext(), url);
                        }
                    });
                }
            });
        } else {
            play.setVisibility(View.GONE);
            // Only show progress bar if loading takes especially long
            loaded = false;
            this.postDelayed(new Runnable() {
                @Override public void run() {
                    if (!loaded) progressbar.setVisibility(View.VISIBLE);
                }
            }, 500);

            Builders.IV.F<?> b = ion.build(imageView)
                    .crossfade(true);
                    if (mediaPointer.w > THRESHOLD_IMG_SIZE || mediaPointer.h > THRESHOLD_IMG_SIZE) b = b.deepZoom(); b
                    .load(url)
                    .setCallback(new FutureCallback<ImageView>() {
                        @Override public void onCompleted(Exception e, ImageView result) {
                            loaded = true;
                            progressbar.setVisibility(View.GONE);
                            if (e != null) return;
                            setupPhotoAttacher();
                        }
                    });
        }

    }

    private static final float releaseScaleThresholdBuffer = 0.005f;
    private static final float releaseScaleThreshold = 0.515f;
    private Matrix previousMatrix;
    private void setupPhotoAttacher() {
        if (attacher != null) attacher.cleanup();
        attacher = new PhotoViewAttacher(imageView);
        attacher.setMaximumScale(25);
        attacher.setOnMatrixChangeListener(new PhotoViewAttacher.OnMatrixChangedListener() {
            @Override public void onMatrixChanged(RectF rect) {
                double s = attacher.getScale();
                // TODO: It would be better if it'd be realiably possible to swipe even when zoomed in
                if (s <= 1) viewPager.setLocked(false); else viewPager.setLocked(true);
                if (s < releaseScaleThreshold) {
                    attacher.setOnMatrixChangeListener(null);
                    previousMatrix.setScale(releaseScaleThreshold, releaseScaleThreshold);
                    attacher.setDisplayMatrix(previousMatrix);
                    attacher.setOnMatrixChangeListener(this);
                }
                background.getBackground().setAlpha(Util.clamp(50, 255*s*Math.sqrt(s)*1.3, 255));
                Util.setVisibility(releaseIndicator, s <= releaseScaleThreshold + releaseScaleThresholdBuffer);
                previousMatrix = attacher.getDisplayMatrix();
            }
        });
    }

    @Override public boolean dispatchTouchEvent(@NonNull MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
            if (attacher != null && attacher.getScale() <= releaseScaleThreshold + releaseScaleThresholdBuffer) {
                ((Activity) getContext()).onBackPressed();
                return true;
            }
        }
        return super.dispatchTouchEvent(event);
    }
}
