package anabolicandroids.chanobol.ui.posts;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.CardView;
import android.text.Layout;
import android.text.Spannable;
import android.text.format.DateUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.ImageViewBitmapInfo;
import com.koushikdutta.ion.Ion;
import com.koushikdutta.ion.bitmap.BitmapInfo;
import com.koushikdutta.ion.builder.AnimateGifMode;
import com.koushikdutta.ion.builder.Builders;

import java.util.ArrayList;

import anabolicandroids.chanobol.App;
import anabolicandroids.chanobol.R;
import anabolicandroids.chanobol.api.ApiModule;
import anabolicandroids.chanobol.api.data.Post;
import anabolicandroids.chanobol.ui.posts.parsing.LinkSpan;
import anabolicandroids.chanobol.ui.posts.parsing.QuoteSpan;
import anabolicandroids.chanobol.ui.posts.parsing.ThreadLink;
import anabolicandroids.chanobol.ui.posts.parsing.ThreadSpan;
import anabolicandroids.chanobol.ui.scaffolding.UiActivity;
import anabolicandroids.chanobol.util.Util;
import butterknife.ButterKnife;
import butterknife.InjectView;

public class PostView extends CardView {
    @InjectView(R.id.number) TextView number;
    @InjectView(R.id.name) TextView name;
    @InjectView(R.id.date) TextView date;
    @InjectView(R.id.replies) TextView replies;
    @InjectView(R.id.reply) TextView reply;
    @InjectView(R.id.mediaContainer) ViewGroup mediaContainer;
    @InjectView(R.id.imageTouchOverlay) View imageTouchOverlay; // I couldn't get the FrameLayout clickable...
    @InjectView(R.id.image) ImageView image;
    @InjectView(R.id.play) ImageView play;
    @InjectView(R.id.progressbar) ProgressBar progress;
    @InjectView(R.id.text) TextView text;
    @InjectView(R.id.footer) ViewGroup footer;
    @InjectView(R.id.footerTripCode) TextView footerTripCode;
    @InjectView(R.id.footerFlag) ImageView footerFlag;
    @InjectView(R.id.footerCountryName) TextView footerCountryName;
    @InjectView(R.id.footerImage) TextView footerImage;

    private PostViewMovementMethod postViewMovementMethod = new PostViewMovementMethod();
    private static final int W = 0, H = 1;
    private int maxImgWidth;
    private int maxImgHeight;

    public Post post;

    public PostView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override protected void onFinishInflate() {
        super.onFinishInflate();
        ButterKnife.inject(this);
        updateImageBounds();
    }

    @Override protected void onConfigurationChanged(Configuration newConfig) {
        if (post == null || image == null || image.getLayoutParams() == null) return;
        updateImageBounds();
        final int[] size = new int[2]; calcSize(size, post);
        image.getLayoutParams().height = size[H];
        postInvalidate();
    }

    private void updateImageBounds() {
        int screenWidth, screenHeight;
        if (getContext() == null) { // Weird anomaly bug
            screenWidth = App.screenWidth;
            screenHeight = App.screenHeight;
        } else {
            screenWidth = Util.getScreenWidth(getContext());
            screenHeight = Util.getScreenHeight(getContext());
        }
        maxImgWidth = screenWidth;
        maxImgHeight = (int) (screenHeight * 0.8);
    }

    private void reset() {
        replies.setOnClickListener(null);
        mediaContainer.setVisibility(GONE);
        image.setVisibility(GONE);
        image.setImageBitmap(null);
        image.setOnClickListener(null);
        play.setVisibility(GONE);
        progress.setVisibility(GONE);
        text.setVisibility(GONE);
        footer.setVisibility(View.GONE);
        footerTripCode.setVisibility(GONE);
        footerFlag.setVisibility(GONE);
        footerFlag.setImageBitmap(null);
        footerCountryName.setVisibility(GONE);
        footerImage.setVisibility(GONE);

        reply.setOnClickListener(dummy);
    }

    OnClickListener dummy = new OnClickListener() {
        @Override public void onClick(View v) {
            Util.showToast(getContext(), "Not yet implemented");
        }
    };

    // If needed reduce the media size such that it is only as big as needed and is
    // never bigger than approx. the screen size. For example, an image with a height
    // of one billion pixels should fit on the screen. Therefore, the width and height
    // have to be reduced proportionally such that the new height is smaller equal the
    // screen height.
    private void calcSize(int[] size, Post post) {
        double w = post.mediaWidth, h = post.mediaHeight;
        if (w >= maxImgWidth) {
            double w_old = w;
            w = maxImgWidth;
            h *= w / w_old;
        }
        if (h >= maxImgHeight) {
            double h_old = h;
            h = maxImgHeight;
            w *= h / h_old;
        }
        size[W] = (int) w;
        size[H] = (int) h;
    }

    private void initImageCallback(final Post post, final PostsActivity.ImageCallback cb) {
        OnClickListener l = new OnClickListener() {
            @Override public void onClick(View v) {
                imageTouchOverlay.postDelayed(new Runnable() {
                    @Override public void run() {
                        cb.onClick(post, image);
                    }
                }, UiActivity.RIPPLE_DELAY());
            }
        };
        imageTouchOverlay.setOnClickListener(l);
        // Without this touches are not registered on 2.3.7 for some reason. I tried other
        // solutions but this is the best I came up with.
        image.setOnClickListener(l);
    }

    private void initWebmCallback(final String url) {
        OnClickListener l = new OnClickListener() {
            @Override public void onClick(View v) {
                Util.startWebmActivity(getContext(), url);
            }
        };
        imageTouchOverlay.setOnClickListener(l);
        image.setOnClickListener(l);
    }

    private void initText(final Ion ion,
                          final Post post,
                          final PostsActivity.RepliesCallback repliesCallback,
                          final PostsActivity.QuoteCallback quoteCallback) {
        name.setText(post.name);
        number.setText(post.number);
        date.setText(DateUtils.getRelativeTimeSpanString(
                post.time * 1000L, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS));
        if (post.replyCount != 0) {
            replies.setVisibility(VISIBLE);
            String suffix = post.replyCount > 1 ? " REPLIES" : " REPLY";
            replies.setText(post.replyCount + suffix);
            replies.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (repliesCallback != null) repliesCallback.onClick(post);
                }
            });
        } else {
            replies.setVisibility(GONE);
        }

        Util.setVisibility(text, post.parsedText().length() > 0);
        text.setText(post.parsedText());
        postViewMovementMethod.quoteCallback = quoteCallback;
        text.setMovementMethod(postViewMovementMethod);

        if (post.mediaId != null && !"null".equals(post.mediaId)) {
            footer.setVisibility(VISIBLE);
            footerImage.setVisibility(VISIBLE);
            footerImage.setText(String.format("%dx%d ~ %s ~ %s%s", post.mediaWidth, post.mediaHeight,
                    Util.readableFileSize(post.filesize), post.filename, post.mediaExtension));
        }
        if (post.tripCode != null) {
            footer.setVisibility(VISIBLE);
            footerTripCode.setVisibility(VISIBLE);
            footerTripCode.setText(post.tripCode);
            //name.setText(post.tripCode);
        }
        if (post.country != null) {
            footer.setVisibility(VISIBLE);
            footerCountryName.setVisibility(VISIBLE);
            footerFlag.setVisibility(VISIBLE);
            footerCountryName.setText(post.countryName);
            ion.build(footerFlag)
                    .fitCenter()
                    .load("file:///android_asset/flags/"+post.country.toLowerCase()+".png");
        }
    }

    // The raison d'être for this method is to immediately populate the OP post cardview
    // when viewing a single thread without first waiting for the 4Chan API request to finish
    // that gets all the posts in order to give the impression (illusion) of promptness
    // (compare with the iOS splash screen concept).
    public void bindToOp(final Drawable opImage, final String transitionName,
                         final Post post, final String boardName,
                         final Ion ion) {
        this.post = post;
        reset();
        initText(ion, post, null, null);

        final int[] size = new int[2]; calcSize(size, post);
        ViewCompat.setTransitionName(image, transitionName);
        mediaContainer.setVisibility(View.VISIBLE);
        image.setVisibility(View.VISIBLE);
        image.setImageDrawable(opImage);
        image.getLayoutParams().height = size[H];
        image.requestLayout();
        ion.build(getContext()).load(ApiModule.imgUrl(boardName, post.mediaId, post.mediaExtension)).asBitmap().tryGet();
    }

    private boolean loaded;
    public void bindTo(final Post post, final String boardName,
                       @SuppressWarnings("UnusedParameters") final String threadId, final Ion ion,
                       final ArrayList<String> bitmapCacheKeys,
                       final PostsActivity.RepliesCallback repliesCallback,
                       final PostsActivity.QuoteCallback quoteCallback,
                       final PostsActivity.ImageCallback imageCallback) {
        this.post = post;
        reset();
        initText(ion, post, repliesCallback, quoteCallback);

        if (post.mediaId != null && !"null".equals(post.mediaId)) {
            final int[] size = new int[2]; calcSize(size, post);
            // Only show progress bar if loading takes especially long
            loaded = false;
            postDelayed(new Runnable() {
                @Override public void run() {
                    if (!loaded) progress.setVisibility(View.VISIBLE);
                }
            }, 500);
            mediaContainer.setVisibility(View.VISIBLE);
            image.setVisibility(View.VISIBLE);
            image.getLayoutParams().height = size[H];
            imageTouchOverlay.getLayoutParams().height = size[H];
            String thumbUrl = ApiModule.thumbUrl(boardName, post.mediaId);
            ion.build(image)
                .load(thumbUrl)
                .withBitmapInfo()
                .setCallback(new FutureCallback<ImageViewBitmapInfo>() {
                    @Override
                    public void onCompleted(Exception e, ImageViewBitmapInfo result) {
                        BitmapInfo bitmapInfo = result.getBitmapInfo();
                        if (e != null || bitmapInfo == null) {
                            loaded = true;
                            progress.setVisibility(View.GONE);
                            return;
                        }
                        bitmapCacheKeys.add(result.getBitmapInfo().key);
                        final String ext = post.mediaExtension;
                        final String url = ApiModule.imgUrl(boardName, post.mediaId, post.mediaExtension);
                        switch (ext) {
                            case ".webm":
                                loaded = true;
                                progress.setVisibility(View.GONE);
                                play.setVisibility(View.VISIBLE);
                                initWebmCallback(url);
                                break;
                            default:
                                initImageCallback(post, imageCallback);
                                Builders.IV.F<?> placeholder = ion.build(image);
                                // I'd love to have something like Picasso's noplaceholder s.t.
                                // Ion doesn't clear the thumbnail preview...
                                BitmapDrawable bd = new BitmapDrawable(getResources(), result.getBitmapInfo().bitmap);
                                placeholder = placeholder.placeholder(bd);
                                // Resized Gifs don't animate apparently, that's the reason for the case analysis
                                if (".gif".equals(ext)) placeholder.animateGif(AnimateGifMode.ANIMATE).smartSize(true);
                                else placeholder.resize(size[W], size[H]);
                                placeholder.load(url).withBitmapInfo().setCallback(new FutureCallback<ImageViewBitmapInfo>() {
                                    @Override public void onCompleted(Exception e, final ImageViewBitmapInfo result) {
                                        loaded = true;
                                        progress.setVisibility(View.GONE);
                                        if (e != null) { return; }
                                        initImageCallback(post, imageCallback);
                                        if (result.getBitmapInfo() != null) {
                                            bitmapCacheKeys.add(result.getBitmapInfo().key);
                                        }
                                    }
                                });
                                break;
                        }
                    }
                });
        }
    }

    // Adapted from Clover
    private class PostViewMovementMethod extends LinkMovementMethod {
        PostsActivity.QuoteCallback quoteCallback;
        @Override public boolean onTouchEvent(@NonNull TextView widget, @NonNull Spannable buffer, @NonNull MotionEvent event) {
            int action = event.getAction();
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_DOWN) {
                int x = (int) event.getX();
                int y = (int) event.getY();
                x -= widget.getTotalPaddingLeft();
                y -= widget.getTotalPaddingTop();
                x += widget.getScrollX();
                y += widget.getScrollY();
                Layout layout = widget.getLayout();
                int line = layout.getLineForVertical(y);
                int off = layout.getOffsetForHorizontal(line, x);
                ClickableSpan[] spans = buffer.getSpans(off, off, ClickableSpan.class);
                if (spans.length == 0) {
                    PostView.this.onTouchEvent(event);
                    return true;
                }

                if (action == MotionEvent.ACTION_UP) {
                    ClickableSpan span = spans[0];
                    if (span instanceof QuoteSpan) {
                        QuoteSpan quoteSpan = (QuoteSpan) span;
                        quoteCallback.onClick(quoteSpan.quoterId, quoteSpan.quotedId);
                    } else if (span instanceof LinkSpan) {
                        LinkSpan linkSpan = (LinkSpan) span;
                        Util.openLink(getContext(), linkSpan.url);
                    } else if (span instanceof ThreadSpan) {
                        ThreadSpan threadSpan = (ThreadSpan) span;
                        final ThreadLink threadLink = threadSpan.threadLink;
                        new AlertDialog.Builder(getContext())
                                .setNegativeButton(android.R.string.cancel, null)
                                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                    @Override public void onClick(final DialogInterface dialog, final int which) {
                                        PostsActivity.launch((Activity) getContext(), null, null, null,
                                                threadLink.board, String.valueOf(threadLink.threadNumber));
                                    }
                                })
                                .setTitle(R.string.open_thread_confirmation)
                                .setMessage("/" + threadLink.board + "/" + threadLink.threadNumber)
                                .show();
                    }
                    span.onClick(widget);
                }

                text.invalidate();
            } else {
                PostView.this.onTouchEvent(event);
            }
            return true;
        }
    }
}
