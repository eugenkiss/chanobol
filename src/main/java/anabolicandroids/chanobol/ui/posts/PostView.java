package anabolicandroids.chanobol.ui.posts;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.CardView;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.format.DateUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.URLSpan;
import android.util.AttributeSet;
import android.util.Patterns;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import anabolicandroids.chanobol.R;
import anabolicandroids.chanobol.api.ApiModule;
import anabolicandroids.chanobol.api.data.Post;
import anabolicandroids.chanobol.util.Util;
import butterknife.ButterKnife;
import butterknife.InjectView;

public class PostView extends CardView {
    @InjectView(R.id.header) ViewGroup header;
    @InjectView(R.id.id) TextView id;
    @InjectView(R.id.date) TextView date;
    @InjectView(R.id.repliesContainer) ViewGroup repliesContainer;
    @InjectView(R.id.replies) TextView replies;
    @InjectView(R.id.image) ImageView image;
    @InjectView(R.id.progressbar) ProgressBar progress;
    @InjectView(R.id.text) TextView text;
    @InjectView(R.id.footer) TextView footer;

    private static final int W = 0, H = 1, H0 = 2;
    private int screenWidth, screenHeight;

    public PostView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override protected void onFinishInflate() {
        super.onFinishInflate();
        ButterKnife.inject(this);
        screenWidth = Util.getScreenWidth(getContext());
        screenHeight = Util.getScreenHeight(getContext());
    }

    private void reset() {
        repliesContainer.setVisibility(View.GONE);
        header.setOnClickListener(null);
        image.setVisibility(View.GONE);
        image.setImageBitmap(null);
        image.setOnClickListener(null);
        progress.setVisibility(View.GONE);
        footer.setVisibility(View.GONE);
    }

    private void calcSize(int[] size, Post post) {
        final int width = Math.min(screenWidth, post.imageWidth);
        final int height = Util.clamp(screenHeight * 0.25, post.imageHeight, screenHeight * 0.85);
        final double factor = 1.0 * width / post.imageWidth;
        size[W] = width;
        size[H] = height;
        size[H0] = (int) (height * factor);
    }

    private void initImageListener(final String imageIdAndExt, final PostsFragment.ImageCallback cb) {
        image.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                cb.onClick(imageIdAndExt, image.getDrawable());
            }
        });
    }

    private void initText(final Post post,
                          final PostsFragment.RepliesCallback repliesCallback,
                          final PostsFragment.ReferencedPostCallback referencedPostCallback) {
        id.setText(post.id);
        date.setText(DateUtils.getRelativeTimeSpanString(
                post.time * 1000L, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS));
        if (post.postReplies != 0) {
            repliesContainer.setVisibility(View.VISIBLE);
            replies.setText(post.postReplies + "r");
            header.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    repliesCallback.onClick(post);
                }
            });
        }

        String s = post.subject;
        if (s == null) s = "";
        else s = "<h2>" + s + "</h2>";
        String t = post.text;
        if (t == null) t = "";

        // TODO: I'd love to use a more principled approach
        String r = (s + t)
                .replaceAll("<wbr>", "")
                // TODO: I simply want to turn all plain text urls into links without matching urls that are assigned to a href attribute
                .replaceAll("(?!href=\")("+Patterns.WEB_URL.pattern()+")", "<a href=\"$1\">$1</a>")
                .replaceAll("<span class=\"quote\">", "<font color=\"#23b423\">")
                .replaceAll("</span>", "</font>");
        setTextViewHTML(post.id, text, r, referencedPostCallback);

        if (post.imageId != null && !"null".equals(post.imageId)) {
            footer.setVisibility(VISIBLE);
            footer.setText(String.format("%dx%d ~ %s ~ %s%s", post.imageWidth, post.imageHeight,
                    Util.readableFileSize(post.filesize), post.filename, post.imageExtension));
        }
    }

    // http://stackoverflow.com/a/19989677/283607
    protected void setTextViewHTML(final String quoterId, TextView text, String html,
                                   final PostsFragment.ReferencedPostCallback referencedPostCallback)
    {
        CharSequence sequence = Html.fromHtml(html);
        SpannableStringBuilder strBuilder = new SpannableStringBuilder(sequence);
        URLSpan[] urls = strBuilder.getSpans(0, sequence.length(), URLSpan.class);
        for(final URLSpan span : urls) {
            if (!span.getURL().startsWith("#p")) continue;
            int start = strBuilder.getSpanStart(span);
            int end = strBuilder.getSpanEnd(span);
            int flags = strBuilder.getSpanFlags(span);
            ClickableSpan clickable = new ClickableSpan() {
                public void onClick(View view) {
                    referencedPostCallback.onClick(quoterId, span.getURL().substring(2));
                }
            };
            strBuilder.setSpan(clickable, start, end, flags);
            strBuilder.removeSpan(span);
        }
        text.setText(strBuilder);
        text.setLinksClickable(true);
        text.setMovementMethod(LinkMovementMethod.getInstance());
    }

    public void bindToOp(final Drawable opImage,
                         final Post post, final String boardName,
                         final String threadId, final Picasso picasso,
                         PostsFragment.RepliesCallback repliesCallback,
                         PostsFragment.ReferencedPostCallback referencedPostCallback,
                         PostsFragment.ImageCallback imageCallback) {
        reset();
        initText(post, repliesCallback, referencedPostCallback);
        initImageListener(post.imageId + post.imageExtension, imageCallback);

        image.setVisibility(View.VISIBLE);
        progress.setVisibility(View.VISIBLE);

        final int[] size = new int[3]; calcSize(size, post);
        image.setImageDrawable(opImage);
        image.getLayoutParams().height = size[H0];
        image.requestLayout();
        picasso.load(ApiModule.imgUrl(boardName, post.imageId, post.imageExtension))
                .noPlaceholder()
                .resize(size[W], size[H])
                .centerInside()
                .into(image);
    }

    public void bindTo(final Post post, final String boardName,
                       final String threadId, final Picasso picasso,
                       PostsFragment.RepliesCallback repliesCallback,
                       PostsFragment.ReferencedPostCallback referencedPostCallback,
                       PostsFragment.ImageCallback imageCallback) {
        reset();
        initText(post, repliesCallback, referencedPostCallback);

        if (post.imageId != null && !"null".equals(post.imageId)) {
            initImageListener(post.imageId + post.imageExtension, imageCallback);

            final int[] size = new int[3]; calcSize(size, post);
            progress.setVisibility(View.VISIBLE);
            image.setVisibility(View.VISIBLE);
            image.getLayoutParams().height = size[H0];
            // TODO: Load the image and thumbnail in parallel into the imageView but cancel
            // thumbnail when real image was loaded earlier. Real image always wins.
            picasso
                .load(ApiModule.thumbUrl(boardName, post.imageId))
                .noFade()
                .into(image, new Callback() {
                    @Override
                    public void onSuccess() {
                        final String ext = post.imageExtension;
                        final String url = ApiModule.imgUrl(boardName, post.imageId, post.imageExtension);
                        switch (ext) {
                            case ".gif":
                                // TODO: gif
                                // The following does not work
                                /*
                                new GifDataDownloader() {
                                    @Override
                                    protected void onPostExecute(final byte[] bytes) {
                                        v.progress.setVisibility(View.GONE);
                                        v.image.setBytes(bytes);
                                        v.image.startAnimation();
                                    }
                                }.execute(url);
                                */
                                //break;
                            case ".webm":
                                // TODO: Webm
                                //break;
                            default:
                                picasso
                                        .load(url)
                                        .noPlaceholder()
                                        .noFade()
                                        .resize(size[W], size[H])
                                        .centerInside()
                                        .into(image, new Callback() {
                                            @Override
                                            public void onSuccess() {
                                                progress.setVisibility(View.GONE);
                                            }

                                            @Override
                                            public void onError() {
                                                progress.setVisibility(View.GONE);
                                            }
                                        });
                                break;
                        }
                    }

                    @Override
                    public void onError() {
                        progress.setVisibility(View.GONE);
                    }
                });
        }
    }
}
