package anabolicandroids.chanobol.ui.images;

import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import java.util.ArrayList;
import java.util.List;

import anabolicandroids.chanobol.R;
import anabolicandroids.chanobol.api.ApiModule;
import anabolicandroids.chanobol.ui.UiFragment;
import anabolicandroids.chanobol.util.Util;
import butterknife.InjectView;
import uk.co.senab.photoview.PhotoView;

public class ImageFragment extends UiFragment {
    @InjectView(R.id.image) PhotoView imageView;

    // Needed to form a valid request to 4Chan
    private String boardName;
    private String threadNumber; // Probably unnecessary
    // Currently not really used (has always length 1), but the idea is to have swipes to
    // the left/right reveal the next/previous image, see issue #85
    private List<ImgIdExt> imagePointers;
    private int index;

    private Drawable preview;

    private String url;

    public static ImageFragment create(String boardName, String threadNumber,
                                       Drawable image, int index, ArrayList<ImgIdExt> imageIdAndExts) {
        ImageFragment f = new ImageFragment();
        Bundle b = new Bundle();
        b.putString("boardName", boardName);
        b.putString("threadNumber", threadNumber);
        b.putInt("index", index);
        b.putParcelableArrayList("imagePointers", imageIdAndExts);
        f.setArguments(b);
        // No need to put in bundle - it's transient state anyway
        f.preview = image;
        return f;
    }

    @Override protected int getLayoutResource() { return R.layout.fragment_image; }

    @Override protected boolean shouldAddPaddingForToolbar() { return false; }

    @Override
    public void onActivityCreated2(Bundle savedInstanceState) {
        super.onActivityCreated2(savedInstanceState);
        Bundle b = getArguments();
        boardName = b.getString("boardName");
        threadNumber = b.getString("threadNumber");
        index = b.getInt("index");
        imagePointers = b.getParcelableArrayList("imagePointers");

        ImgIdExt imagePointer = imagePointers.get(index);
        url = ApiModule.imgUrl(boardName, imagePointer.id, imagePointer.ext);
        // TODO: Makes application break after a while of zooming in and out
        //imageView.setMaximumScale(25); // Default value is too small for some images
        // TODO: Would be great if deepZoom was more configurable. Too early too low resolution.
        ion.build(imageView).placeholder(preview).deepZoom().load(url);
    }

    @Override
    public void onResume() {
        super.onResume();
        activity.setTitle("");
        toolbar.getBackground().setAlpha(0);
        toolbar.post(new Runnable() {
            @Override public void run() {
                updateToolbarShadow();
            }
        });
    }

    @Override public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        toolbar.postDelayed(new Runnable() {
            @Override public void run() {
                updateToolbarShadow();
            }
        }, 100);
    }

    @Override public void onDestroy() {
        super.onDestroy();
        toolbar.getBackground().setAlpha(255);
        toolbarShadow.setImageBitmap(null);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.image, menu);
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

    private void updateToolbarShadow() {
        toolbar.setDrawingCacheEnabled(true);
        toolbar.buildDrawingCache();
        Bitmap bm = toolbar.getDrawingCache();
        if (Build.VERSION.SDK_INT < 12) bm = Util.setHasAlphaCompat(bm);
        else bm.setHasAlpha(true);
        toolbarShadow.setImageBitmap(Util.blur(activity, bm, 1f));
        toolbarShadow.setColorFilter(Color.argb(255, 0, 0, 0));
        toolbar.setDrawingCacheEnabled(false);
    }
}
