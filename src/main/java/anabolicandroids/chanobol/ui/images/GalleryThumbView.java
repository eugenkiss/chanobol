package anabolicandroids.chanobol.ui.images;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;

import com.koushikdutta.ion.Ion;

import anabolicandroids.chanobol.api.ApiModule;

public class GalleryThumbView extends ImageView {
    public ImgIdExt imagePointer;

    public GalleryThumbView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void bindTo(Ion ion, String board, ImgIdExt imagePointer) {
        this.imagePointer = imagePointer;
        ion.build(this).load(ApiModule.thumbUrl(board, imagePointer.id));
    }
}
