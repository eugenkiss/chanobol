package anabolicandroids.chanobol.ui.media;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.media.MediaPlayer;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.graphics.Palette;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.ImageViewBitmapInfo;
import com.koushikdutta.ion.Ion;
import com.koushikdutta.ion.builder.Builders;
import com.koushikdutta.ion.future.ImageViewFuture;
import com.malmstein.fenster.controller.SimpleMediaFensterPlayerController;
import com.malmstein.fenster.view.FensterVideoView;

import anabolicandroids.chanobol.R;
import anabolicandroids.chanobol.api.ApiModule;
import anabolicandroids.chanobol.api.data.MediaPointer;
import anabolicandroids.chanobol.ui.scaffolding.Prefs;
import anabolicandroids.chanobol.util.HackyViewPager;
import anabolicandroids.chanobol.util.Util;
import butterknife.ButterKnife;
import butterknife.InjectView;
import uk.co.senab.photoview.PhotoViewAttacher;

// https://github.com/koush/ion/blob/master/ion-sample/src/com/koushikdutta/ion/sample/ProgressBarDownload.java
public class MediaView extends FrameLayout {

    private static final int THRESHOLD_IMG_SIZE = 3000;

    public Ion ion;
    public View background;
    public View releaseIndicator;
    public HackyViewPager viewPager;

    @InjectView(R.id.container) ViewGroup container;
    @InjectView(R.id.imageView) ImageView imageView;
    @InjectView(R.id.videoContainer) ViewGroup videoContainer;
    @InjectView(R.id.videoView) FensterVideoView videoView;
    @InjectView(R.id.videoViewController) SimpleMediaFensterPlayerController videoViewController;
    @InjectView(R.id.play) ImageView play;
    @InjectView(R.id.progressbar) ProgressBar progressbar;
    @SuppressWarnings("UnusedDeclaration") private PhotoViewAttacher attacher;

    public Prefs prefs;
    public MediaPointer lastMediaPointer;
    boolean loaded;
    // I observed that sometimes the higher quality media is not loaded after a swipe.
    // I speculate that due to asynchronous timing issues a call to bindToPreview comes
    // after bindTo and "overwrites" it. This is a rough and speculative fix and another
    // approach (e.g. Rx) approach would probably be more principled.
    public boolean higherQualityAlreadyLoading;

    private GestureDetectorCompat gestureDetector;

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

    public void clear() {
        lastMediaPointer = null;
        loaded = false;
        higherQualityAlreadyLoading = false;
        videoView.stopPlayback();
        videoView.suspend();
        videoContainer.setVisibility(View.GONE);
    }

    private void reset() {
        videoView.stopPlayback();
        videoView.suspend();
        videoContainer.setVisibility(View.GONE);
        container.setOnClickListener(null);
    }

    public void setTransitionNameForImageView(String name) {
        ViewCompat.setTransitionName(imageView, name);
    }

    public void bindToPreview(Bitmap bitmap) {
        imageView.setImageBitmap(bitmap);
        // Otherwise there will be glitches sometimes
        if (attacher != null) attacher.cleanup();
        attacher = new PhotoViewAttacher(imageView);
    }

    public void bindToPreview(String boardName, MediaPointer mediaPointer) {
        reset();
        if (higherQualityAlreadyLoading) return;
        lastMediaPointer = mediaPointer;

        progressbar.setVisibility(View.GONE);
        Util.setVisibility(play, mediaPointer.isWebm() && prefs.externalWebm());

        imageView.setImageBitmap(null);
        String thumbUrl = ApiModule.thumbUrl(boardName, mediaPointer.id);
        ion.build(imageView).fadeIn(false).load(thumbUrl).withBitmapInfo().setCallback(new FutureCallback<ImageViewBitmapInfo>() {
            @Override public void onCompleted(Exception e, ImageViewBitmapInfo result) {
                if (attacher != null) attacher.cleanup();
                if (e != null) return;
                attacher = new PhotoViewAttacher(imageView);
                if (result == null || result.getBitmapInfo() == null || result.getBitmapInfo().bitmap == null) return;
                if (prefs.theme().isLightTheme) {
                    // TODO: Threadmanager should encapsulate the color...
                    int primaryDark = getResources().getColor(R.color.colorPrimaryDark);
                    Palette palette = Palette.generate(result.getBitmapInfo().bitmap);
                    imageView.setBackgroundColor(palette.getMutedColor(primaryDark));
                }
            }
        });
    }

    public void bindTo(String boardName, final MediaPointer mediaPointer) {
        reset();
        // Only show progress bar if loading takes especially long
        loaded = false;
        this.postDelayed(new Runnable() {
            @Override public void run() {
                if (!loaded) progressbar.setVisibility(View.VISIBLE);
            }
        }, 500);

        higherQualityAlreadyLoading = true;
        lastMediaPointer = mediaPointer;

        final String url = ApiModule.imgUrl(boardName, mediaPointer.id, mediaPointer.ext);

        if (mediaPointer.isWebm()) {
            if (attacher != null) attacher.cleanup();
            String thumbUrl = ApiModule.thumbUrl(boardName, mediaPointer.id);
            ImageViewFuture imageViewFuture = ion.build(imageView).load(thumbUrl);

            if (prefs.externalWebm()) {
                loaded = true;
                progressbar.setVisibility(View.GONE);
                play.setVisibility(View.VISIBLE);
                imageViewFuture.setCallback(new FutureCallback<ImageView>() {
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

                container.setOnTouchListener(new OnTouchListener() {
                    @Override public boolean onTouch(View v, MotionEvent event) {
                        return gestureDetector.onTouchEvent(event);
                    }
                });
                gestureDetector = new GestureDetectorCompat(getContext(), new GestureDetector.SimpleOnGestureListener() {
                    @Override public boolean onDown(MotionEvent e) { return true; }
                    @Override public boolean onSingleTapUp(MotionEvent e) {
                        if (videoViewController.isShowing()) videoViewController.hide();
                        else videoViewController.show();
                        return true;
                    }
                });

                Util.setAlpha(videoContainer, 0);
                videoContainer.setVisibility(View.VISIBLE);
                videoView.setMediaController(videoViewController);
                videoView.setOnPlayStateListener(videoViewController);
                videoView.setVideo(url, SimpleMediaFensterPlayerController.DEFAULT_VIDEO_START);
                videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                    @Override public void onPrepared(MediaPlayer mp) {
                        mp.setOnVideoSizeChangedListener(new MediaPlayer.OnVideoSizeChangedListener() {
                            // The following are some workarounds for scaling/size issues
                            @TargetApi(Build.VERSION_CODES.HONEYCOMB) @Override
                            public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
                                // Sometimes the video's height is stretched too high or too low...
                                float imageRatio = 1f * mediaPointer.h / mediaPointer.w;
                                float videoRatio = 1f * videoView.getHeight() / videoView.getWidth(); // width/height does not work
                                float scale = imageRatio / videoRatio;
                                //videoView.setScaleY(scale);

                                // The container's height must be readjusted
                                ViewGroup.LayoutParams containerParams = videoContainer.getLayoutParams();
                                containerParams.height = (int) (videoView.getHeight() * scale);
                                videoContainer.setLayoutParams(containerParams);

                                // The controller's height must be set manually otherwise it fills the screen
                                ViewGroup.LayoutParams controllerParams = videoViewController.getLayoutParams();
                                controllerParams.height = (int) (videoView.getHeight() * scale);
                                videoViewController.setLayoutParams(controllerParams);

                                // A slight delay to prevent weird videoViewController flashes
                                MediaView.this.postDelayed(new Runnable() {
                                    @Override public void run() {
                                        Util.setAlpha(videoContainer, 1);
                                    }
                                }, 80);

                                // Indicate visually that loading is finished
                                loaded = true;
                                progressbar.setVisibility(View.GONE);
                            }
                        });
                    }
                });
                videoView.start();
                videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override public void onCompletion(MediaPlayer mp) {
                        videoView.start();
                    }
                });
            }
        } else {
            play.setVisibility(View.GONE);
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
                int a = Util.clamp(50, 255*s*Math.sqrt(s)*1.3, 255);
                background.getBackground().setAlpha(a);
                imageView.getBackground().setAlpha(a);
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
