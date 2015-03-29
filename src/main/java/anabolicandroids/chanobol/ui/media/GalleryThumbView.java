package anabolicandroids.chanobol.ui.media;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.koushikdutta.ion.Ion;

import anabolicandroids.chanobol.R;
import anabolicandroids.chanobol.api.ApiModule;
import anabolicandroids.chanobol.api.data.MediaPointer;
import butterknife.ButterKnife;
import butterknife.InjectView;

public class GalleryThumbView extends FrameLayout {
    @InjectView(R.id.image) ImageView image;
    @InjectView(R.id.info) TextView info;

    public int index;

    public GalleryThumbView(Context context, AttributeSet attrs) { super(context, attrs); }

    @Override protected void onFinishInflate() {
        super.onFinishInflate();
        ButterKnife.inject(this);
    }

    public void bindTo(Ion ion, String board, MediaPointer mediaPointer, int index) {
        this.index = index;
        ion.build(image).load(ApiModule.thumbUrl(board, mediaPointer.id));

        switch (mediaPointer.ext) {
            case ".webm": info.setText("webm"); break;
            case ".gif": info.setText("gif"); break;
            default: info.setText("");
        }
    }

}
