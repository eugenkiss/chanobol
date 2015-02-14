package anabolicandroids.chanobol.ui.images;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.koushikdutta.ion.Ion;

import anabolicandroids.chanobol.R;
import anabolicandroids.chanobol.api.ApiModule;
import butterknife.ButterKnife;
import butterknife.InjectView;

public class GalleryThumbView extends FrameLayout {
    @InjectView(R.id.image) ImageView image;

    public ImagePointer imagePointer;

    public GalleryThumbView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override protected void onFinishInflate() {
        super.onFinishInflate();
        ButterKnife.inject(this);
    }

    public void bindTo(Ion ion, String board, ImagePointer imagePointer) {
        this.imagePointer = imagePointer;
        ion.build(image).load(ApiModule.thumbUrl(board, imagePointer.id));
    }

}
