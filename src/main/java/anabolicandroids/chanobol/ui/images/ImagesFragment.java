package anabolicandroids.chanobol.ui.images;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import java.util.List;

import anabolicandroids.chanobol.R;
import anabolicandroids.chanobol.api.ApiModule;
import anabolicandroids.chanobol.ui.UiFragment;
import butterknife.InjectView;
import uk.co.senab.photoview.PhotoView;

public class ImagesFragment extends UiFragment {
    @InjectView(R.id.image) PhotoView imageView;

    private String boardName;
    private String threadId;
    private Drawable image;
    private int index;
    private List<String> imageIdAndExts;

    private String url;

    public static ImagesFragment create(String boardName, String threadId,
                                        Drawable image, int index, List<String> imageIdAndExts) {
        ImagesFragment f = new ImagesFragment();
        f.boardName = boardName;
        f.threadId = threadId;
        f.image = image;
        f.index = index;
        f.imageIdAndExts = imageIdAndExts;
        return f;
    }

    @Override protected int getLayoutResource() { return R.layout.fragment_images; }

    @Override protected boolean shouldAddPaddingForToolbar() { return false; }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        String[] imageIdAndExt = imageIdAndExts.get(index).split("\\.");
        url = ApiModule.imgUrl(boardName, imageIdAndExt[0], "."+imageIdAndExt[1]);
        // TODO: Makes application break after a while of zooming in and out
        //imageView.setMaximumScale(25); // Default value is to small for some images
        // TODO: Would be great if deepZoom was more configurable. Too early too low resolution.
        ion.build(imageView).placeholder(image).deepZoom().load(url);
    }

    @Override
    public void onResume() {
        super.onResume();
        activity.setTitle(boardName+"/imgs/"+threadId);
        toolbar.getBackground().setAlpha(0);
    }

    @Override public void onDestroy() {
        super.onDestroy();
        toolbar.getBackground().setAlpha(255);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.images, menu);
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
}
