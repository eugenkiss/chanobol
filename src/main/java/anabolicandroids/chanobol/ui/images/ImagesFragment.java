package anabolicandroids.chanobol.ui.images;

import android.graphics.drawable.Drawable;
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

    private Menu menu;
    private String boardName;
    private String threadId;
    private Drawable image;
    private int index;
    private List<String> imageIdAndExts;

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

    @Override
    protected int getLayoutResource() {
        return R.layout.fragment_images;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        String[] imageIdAndExt = imageIdAndExts.get(index).split("\\.");
        final String url = ApiModule.imgUrl(boardName, imageIdAndExt[0], "."+imageIdAndExt[1]);
        // TODO: Makes application break after a while of zooming in and out
        //imageView.setMaximumScale(25); // Default value is to small for some images
        // TODO: Would be great if deepZoom was more configurable. Too early too low resolution.
        ion.build(imageView).placeholder(image).deepZoom().load(url);
    }

    @Override
    public void onResume() {
        super.onResume();
        activity.setTitle(boardName+"/imgs/"+threadId);
        if (menu != null) {
            menu.setGroupVisible(R.id.posts, false);
            menu.setGroupVisible(R.id.postsDialog, false);
            menu.setGroupVisible(R.id.images, true);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.posts, menu);
        menu.setGroupVisible(R.id.posts, false);
        menu.setGroupVisible(R.id.postsDialog, false);
        this.menu = menu;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
        }
        return super.onOptionsItemSelected(item);
    }
}
