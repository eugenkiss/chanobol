package anabolicandroids.chanobol.ui.images;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;

import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.ImageViewBitmapInfo;
import com.koushikdutta.ion.bitmap.BitmapInfo;

import java.util.ArrayList;
import java.util.List;

import anabolicandroids.chanobol.R;
import anabolicandroids.chanobol.api.ApiModule;
import anabolicandroids.chanobol.ui.UiFragment;
import anabolicandroids.chanobol.util.Util;
import butterknife.InjectView;
import uk.co.senab.photoview.PhotoView;

public class ImageFragment extends UiFragment {

    // Construction ////////////////////////////////////////////////////////////////////////////////

    @InjectView(R.id.image) PhotoView imageView;

    // Transient state - only helpful for transitions
    public Drawable preview;
    public boolean thumbnailPreview;
    public String transitionName = "";
    public long transitionDuration;
    public ImageView postIv;

    // Needed to form a valid request to 4Chan
    private String boardName;
    private String threadNumber;
    // Currently not really used (has always length 1), but the idea is to have swipes to
    // the left/right reveal the next/previous image, see issue #85
    private List<ImgIdExt> imagePointers;
    private int index;

    private String url;

    public static ImageFragment create(String boardName, String threadNumber,
                                       int index, ArrayList<ImgIdExt> imageIdAndExts) {
        ImageFragment f = new ImageFragment();
        Bundle b = new Bundle();
        b.putString("boardName", boardName);
        b.putString("threadNumber", threadNumber);
        b.putInt("index", index);
        b.putParcelableArrayList("imagePointers", imageIdAndExts);
        f.setArguments(b);
        return f;
    }

    @Override protected int getLayoutResource() { return R.layout.fragment_image; }

    @Override protected boolean shouldAddPaddingForToolbar() { return false; }

    @Override
    public void onActivityCreated2(Bundle savedInstanceState) {
        super.onActivityCreated2(savedInstanceState);

        Bundle b = getArguments();
        boardName = b.getString("boardName");
        threadNumber = b.getString("threadNumber");
        index = b.getInt("index");
        imagePointers = b.getParcelableArrayList("imagePointers");

        ImgIdExt imagePointer = imagePointers.get(index);
        url = ApiModule.imgUrl(boardName, imagePointer.id, imagePointer.ext);
        if (transitionsAllowed()) {
            if (preview != null) imageView.setImageDrawable(preview);
            if (Build.VERSION.SDK_INT >= 21) imageView.setTransitionName(transitionName);
            imageView.setEnabled(false);
            imageView.postDelayed(new Runnable() {
                @Override public void run() {
                    // Deepzoom breaks shared return transition for high-res images
                    ion.build(imageView).crossfade(true).load(url)
                            .withBitmapInfo().setCallback(new FutureCallback<ImageViewBitmapInfo>() {
                        @Override public void onCompleted(Exception e, ImageViewBitmapInfo result) {
                            BitmapInfo bitmapInfo = result.getBitmapInfo();
                            if (e != null || bitmapInfo == null || postIv == null) {
                                return;
                            }
                            // To fix glitch on return shared element transition when it started with thumbnail,
                            // but imagefragment loaded real image in the meantime.
                            if (ImageFragment.this.isAdded())
                                postIv.setImageDrawable(new BitmapDrawable(getResources(), bitmapInfo.bitmap));
                        }
                    });
                    imageView.setEnabled(true);
                }
            }, transitionDuration);
        } else {
            // TODO: Makes application break after a while of zooming in and out
            //imageView.setMaximumScale(25); // Default value is too small for some images
            // TODO: Would be great if deepZoom was more configurable. Too early too low resolution.
            ion.build(imageView).placeholder(preview).deepZoom().load(url);
        }
    }

    private void updateToolbarShadow() {
        toolbar.setDrawingCacheEnabled(true);
        toolbar.buildDrawingCache();
        Bitmap bm = toolbar.getDrawingCache();
        if (Build.VERSION.SDK_INT < 12) bm = Util.setHasAlphaCompat(bm);
        else bm.setHasAlpha(true);
        toolbarShadow.setImageBitmap(Util.blur(activity, bm, 1f));
        toolbarShadow.setColorFilter(Color.argb(255, 0, 0, 0));
        toolbar.setDrawingCacheEnabled(false);
    }

    // Lifecycle ///////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onResume() {
        super.onResume();
        activity.setTitle("");
        if (transitionsAllowed()) {
            Drawable bg = toolbar.getBackground();
            int alpha = 255; if (Build.VERSION.SDK_INT >= 21)
                // E.g. if toolbar was hidden then immediately start transparent
                alpha = (int) ((1+(toolbar.getTranslationY()/toolbar.getHeight()))*255);
            animateAlpha(bg, alpha, 0, new Runnable() {
                @Override public void run() {
                    updateToolbarShadow();
                    animateAlpha(toolbarShadow, 0, 255, null);
                }
            });
        } else {
            toolbar.getBackground().setAlpha(0);
            toolbar.post(new Runnable() {
                @Override public void run() {
                    updateToolbarShadow();
                }
            });
        }
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
        if (transitionsAllowed()) {
            Drawable bg = toolbar.getBackground();
            int alpha = 0; if (Build.VERSION.SDK_INT >= 21) alpha = bg.getAlpha();
            animateAlpha(bg, alpha, 255, null);
            animateAlpha(toolbarShadow, 255, 0, new Runnable() {
                @Override public void run() {
                    toolbarShadow.setImageBitmap(null);
                }
            });
        } else {
            toolbar.getBackground().setAlpha(255);
            toolbarShadow.setImageBitmap(null);
        }
    }

    // Toolbar Menu ////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.image, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        switch(item.getItemId()) {
            case R.id.openExternal:
                intent = new Intent();
                intent.setAction(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.parse(url), "image/*");
                if(intent.resolveActivity(activity.getPackageManager()) != null)
                    startActivity(intent);
                else
                    showToast("No suitable app");
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

    // Transitions /////////////////////////////////////////////////////////////////////////////////

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
        v.start();
    }
}
