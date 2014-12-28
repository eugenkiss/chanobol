package anabolicandroids.chanobol.ui.threads;

import android.content.Context;
import android.support.v7.widget.CardView;
import android.text.Html;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import anabolicandroids.chanobol.R;
import anabolicandroids.chanobol.api.ApiModule;
import anabolicandroids.chanobol.api.data.Thread;
import anabolicandroids.chanobol.util.Util;
import butterknife.ButterKnife;
import butterknife.InjectView;

public class ThreadView extends CardView {
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

    public void bindTo(Thread thread, String boardName, Picasso picasso) {
        numReplies.setText(thread.replies+"r");
        numImages.setText(thread.images+"i");
        picasso.load(ApiModule.thumbUrl(boardName, thread.imageId)).into(image);
        String s = thread.subject;
        if (s == null) s = "";
        else s = "<b>" + s + "</b><br/>";
        String t = thread.text;
        if (t == null) t = "";
        text.setText(Html.fromHtml(s + t));
        if (thread.dead) {
            Util.setAlpha(this, 0.3f);
        } else {
            Util.setAlpha(this, 1f);
        }
    }
}
