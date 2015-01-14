package anabolicandroids.chanobol.ui.threads;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.v7.widget.CardView;
import android.text.Html;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.ImageViewBitmapInfo;
import com.koushikdutta.ion.Ion;

import java.util.WeakHashMap;

import anabolicandroids.chanobol.R;
import anabolicandroids.chanobol.api.ApiModule;
import anabolicandroids.chanobol.api.data.Thread;
import butterknife.ButterKnife;
import butterknife.InjectView;

public class ThreadView extends CardView {
    @InjectView(R.id.blackness) View blackness;
    @InjectView(R.id.numReplies) TextView numReplies;
    @InjectView(R.id.numImages) TextView numImages;
    @InjectView(R.id.image) ImageView image;
    @InjectView(R.id.text) TextView text;

    public ThreadView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override protected void onFinishInflate() {
        super.onFinishInflate();
        ButterKnife.inject(this);
    }

    public void bindTo(final Thread thread, String boardName, Ion ion, final WeakHashMap<String, Bitmap> bitMap) {
        numReplies.setText(thread.replies+"r");
        numImages.setText(thread.images+"i");
        ion.build(image).load(ApiModule.thumbUrl(boardName, thread.imageId)).withBitmapInfo().setCallback(new FutureCallback<ImageViewBitmapInfo>() {
            @Override public void onCompleted(Exception e, ImageViewBitmapInfo result) {
                if (e == null && result.getBitmapInfo() != null)
                    bitMap.put(thread.id, result.getBitmapInfo().bitmap);
            }
        });
        String s = thread.subject;
        if (s == null) s = "";
        else s = "<b>" + s + "</b><br/>";
        String t = thread.text;
        if (t == null) t = "";
        text.setText(Html.fromHtml(s + t));
        if (thread.dead) {
            blackness.setVisibility(View.VISIBLE);
        } else {
            blackness.setVisibility(View.GONE);
        }
    }
}
