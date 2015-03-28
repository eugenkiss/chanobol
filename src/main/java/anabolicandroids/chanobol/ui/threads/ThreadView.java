package anabolicandroids.chanobol.ui.threads;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.text.Spannable;
import android.text.style.StyleSpan;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.ImageViewBitmapInfo;
import com.koushikdutta.ion.Ion;

import java.util.HashMap;

import anabolicandroids.chanobol.R;
import anabolicandroids.chanobol.api.ApiModule;
import anabolicandroids.chanobol.api.data.Thread;
import butterknife.ButterKnife;
import butterknife.InjectView;

public class ThreadView extends FrameLayout {
    @InjectView(R.id.blackness) View blackness;
    @InjectView(R.id.numReplies) TextView numReplies;
    @InjectView(R.id.numImages) TextView numImages;
    @InjectView(R.id.image) ImageView image;
    @InjectView(R.id.text) TextView text;

    public Thread thread;

    StyleSpan boldSpan;

    public ThreadView(Context context, AttributeSet attrs) { super(context, attrs); }

    @Override protected void onFinishInflate() {
        super.onFinishInflate();
        ButterKnife.inject(this);
        boldSpan = new StyleSpan(Typeface.BOLD);
    }

    public void bindTo(final Thread thread, String boardName, Ion ion,
                       boolean onlyUpdateText, final HashMap<String, Bitmap> bitMap) {

        // Remove reference to bitmap which is out of view
        if (this.thread != null && !thread.number.equals(this.thread.number)) {
            bitMap.remove(this.thread.number);
        }
        this.thread = thread;
        Bitmap b = bitMap.get(thread.number);
        numReplies.setText(thread.replies+"r");
        numImages.setText(thread.images+"i");
        if (!onlyUpdateText || b == null) {
            ion.build(image)
                    .load(ApiModule.thumbUrl(boardName, thread.mediaId))
                    .withBitmapInfo()
                    .setCallback(new FutureCallback<ImageViewBitmapInfo>() {
                        @Override public void onCompleted(Exception e, ImageViewBitmapInfo result) {
                            if (e == null && result.getBitmapInfo() != null)
                                if (bitMap.get(thread.number) == null)
                                    bitMap.put(thread.number, result.getBitmapInfo().bitmap);
                        }
                    });
        } else {
            image.setImageDrawable(new BitmapDrawable(getResources(), b));
        }

        text.setText(thread.excerpt);
        Spannable spannable = (Spannable) text.getText();
        spannable.setSpan(boldSpan, 0, thread.strippedSubject.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);

        if (thread.dead) blackness.setVisibility(View.VISIBLE);
        else blackness.setVisibility(View.GONE);
    }
}
