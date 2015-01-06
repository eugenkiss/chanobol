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
import it.sephiroth.android.library.imagezoom.ImageViewTouch;
import it.sephiroth.android.library.imagezoom.ImageViewTouchBase;

public class ImagesFragment extends UiFragment {
    @InjectView(R.id.image) ImageViewTouch imageView;

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
        imageView.setDisplayType(ImageViewTouchBase.DisplayType.FIT_TO_SCREEN);
        imageView.setImageDrawable(image);
        String[] imageIdAndExt = imageIdAndExts.get(index).split("\\.");
        final String url = ApiModule.imgUrl(boardName, imageIdAndExt[0], "."+imageIdAndExt[1]);
        if (imageIdAndExt[1].equals("gif")) {
            // Do nothing
        } else {
            picasso.load(url)
                   .noPlaceholder()
                   .noFade()
                   .into(imageView);
        }
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
