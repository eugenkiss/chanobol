package anabolicandroids.chanobol.ui.images;

import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.koushikdutta.async.future.FutureCallback;

import java.util.ArrayList;
import java.util.List;

import anabolicandroids.chanobol.R;
import anabolicandroids.chanobol.api.data.Post;
import anabolicandroids.chanobol.ui.SwipeRefreshFragment;
import anabolicandroids.chanobol.ui.UiAdapter;
import anabolicandroids.chanobol.util.Util;
import butterknife.InjectView;

public class GalleryFragment extends SwipeRefreshFragment {
    @InjectView(R.id.threads) RecyclerView galleryView;

    // The necessary information to request the data from 4Chan
    String boardName;
    String threadNumber;

    // To instantly load and display thumbnails
    ArrayList<ImgIdExt> imagePointers;
    GalleryAdapter galleryAdapter;

    public static GalleryFragment create(String boardName, String threadNumber, ArrayList<ImgIdExt> imagePointers) {
        GalleryFragment f = new GalleryFragment();
        Bundle b = new Bundle();
        b.putString("boardName", boardName);
        b.putString("threadNumber", threadNumber);
        f.setArguments(b);
        // No need to put in bundle - it's transient state anyway
        f.imagePointers = imagePointers;
        return f;
    }

    @Override
    protected int getLayoutResource() {
        return R.layout.fragment_threads;
    }

    @Override
    public void onActivityCreated2(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated2(savedInstanceState);

        Bundle b = getArguments();
        boardName = b.getString("boardName");
        threadNumber = b.getString("threadNumber");

        View.OnClickListener clickListener = new View.OnClickListener() {
            @Override public void onClick(View v) {
                GalleryThumbView g = (GalleryThumbView) v;
                ImageFragment f = ImageFragment.create(boardName, threadNumber, g.getDrawable(),
                        0, Util.arrayListOf(g.imagePointer));
                startFragment(f);
            }
        };

        galleryAdapter = new GalleryAdapter(clickListener, null);
        galleryView.setAdapter(galleryAdapter);
        galleryView.setHasFixedSize(true);
        GridLayoutManager glm = new GridLayoutManager(context, 3);
        galleryView.setLayoutManager(glm);
        galleryView.setItemAnimator(new DefaultItemAnimator());
        Util.calcDynamicSpanCountById(context, galleryView, glm, R.dimen.column_width_gallery);

        load();
    }

    @Override
    public void onResume() {
        super.onResume();
        activity.setTitle(boardName + "/gal/" + threadNumber);
        galleryAdapter.notifyDataSetChanged();
    }

    @Override public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Util.updateRecyclerViewGridOnConfigChange(galleryView, R.dimen.column_width_gallery);
    }

    @Override
    protected void load() {
        super.load();
        service.listPosts(this, boardName, threadNumber, new FutureCallback<List<Post>>() {
            @Override public void onCompleted(Exception e, List<Post> result) {
                if (e != null) {
                    showToast(e.getMessage());
                    System.out.println("" + e.getMessage());
                    loaded();
                    return;
                }
                imagePointers.clear();
                for (Post p : result) {
                    if (p.imageId != null) imagePointers.add(new ImgIdExt(p.imageId, p.imageExtension));
                }
                galleryAdapter.notifyDataSetChanged();
                loaded();
            }
        });
    }

    @Override
    protected void cancelPending() {
        super.cancelPending();
        ion.cancelAll(this);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.gallery, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
        }
        return super.onOptionsItemSelected(item);
    }

    class GalleryAdapter extends UiAdapter<ImgIdExt> {

        public GalleryAdapter(View.OnClickListener clickListener, View.OnLongClickListener longClickListener) {
            super(GalleryFragment.this.context, clickListener, longClickListener);
            this.items = imagePointers;
        }

        @Override public View newView(ViewGroup container) {
            return inflater.inflate(R.layout.view_gallery_thumb, container, false);
        }

        @Override public void bindView(ImgIdExt imagePointer, int position, View view) {
            ((GalleryThumbView) view).bindTo(ion, boardName, imagePointer);
        }
    }
}

