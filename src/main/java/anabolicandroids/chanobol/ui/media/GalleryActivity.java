package anabolicandroids.chanobol.ui.media;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.transition.TransitionInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.koushikdutta.async.future.FutureCallback;

import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import anabolicandroids.chanobol.R;
import anabolicandroids.chanobol.api.ApiModule;
import anabolicandroids.chanobol.api.data.MediaPointer;
import anabolicandroids.chanobol.api.data.Thread;
import anabolicandroids.chanobol.api.data.ThreadPreview;
import anabolicandroids.chanobol.ui.scaffolding.SwipeRefreshActivity;
import anabolicandroids.chanobol.ui.scaffolding.UiAdapter;
import anabolicandroids.chanobol.util.ImageSaver;
import anabolicandroids.chanobol.util.Util;
import butterknife.InjectView;

public class GalleryActivity extends SwipeRefreshActivity {

    // Construction ////////////////////////////////////////////////////////////////////////////////

    private static Thread threadTransfer;
    // backup
    private static String EXTRA_THREAD = "thread";

    private static String THREAD = "thread";
    private Thread thread;
    // Convenience
    private String boardName;
    private String threadNumber;

    private GalleryAdapter galleryAdapter;

    public static void launch(Activity activity, Thread thread) {
        ActivityOptionsCompat options = makeSceneTransitionAnimation(activity);
        Intent intent = new Intent(activity, GalleryActivity.class);
        intent.putExtra(EXTRA_THREAD, Parcels.wrap(new Thread(thread.boardName, thread.threadNumber)));
        threadTransfer = thread;
        ActivityCompat.startActivity(activity, intent, options.toBundle());
    }

    @Inject ImageSaver imageSaver;

    @InjectView(R.id.gallery) RecyclerView galleryView;

    @Override protected int getLayoutResource() { return R.layout.activity_gallery; }
    @Override protected RecyclerView getRootRecyclerView() { return galleryView; }

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // To prevent transition glitch
        getWindow().setBackgroundDrawableResource(R.color.transparent);

        Bundle b = getIntent().getExtras();

        // Ugly quick fix (transaction too large)
        thread = Parcels.unwrap(b.getParcelable(EXTRA_THREAD));
        if (threadTransfer != null && threadTransfer.threadNumber.equals(thread.threadNumber)) {
            // Deep copy threadTransfer
            Bundle bundle = new Bundle();
            String key = "thread";
            bundle.putParcelable(key, Parcels.wrap(threadTransfer));
            thread = Parcels.unwrap(bundle.getParcelable(key));
            thread = threadTransfer;
        }
        threadTransfer = null;

        boardName = thread.boardName;
        threadNumber = thread.threadNumber;

        if (savedInstanceState == null) {
            firstLoad = true;
            load();
        } else {
            firstLoad = false;
            thread = Parcels.unwrap(savedInstanceState.getParcelable(THREAD));
        }

        setTitle(thread.titleForGallery());

        galleryAdapter = new GalleryAdapter(clickListener, null);
        galleryView.setAdapter(galleryAdapter);
        galleryView.setHasFixedSize(true);
        GridLayoutManager glm = new GridLayoutManager(this, 3);
        galleryView.setLayoutManager(glm);
        galleryView.setItemAnimator(new DefaultItemAnimator());
        Util.calcDynamicSpanCountById(this, galleryView, glm, R.dimen.column_width_gallery);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setEnterTransition(TransitionInflater.from(this).inflateTransition(android.R.transition.slide_right));
            getWindow().setReturnTransition(TransitionInflater.from(this).inflateTransition(android.R.transition.slide_right));
        }
    }

    @Override protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(THREAD, Parcels.wrap(thread));
    }

    // Callbacks ///////////////////////////////////////////////////////////////////////////////////

    View.OnClickListener clickListener = new View.OnClickListener() {
        @Override public void onClick(View v) {
            final GalleryThumbView iv = (GalleryThumbView) v;
            int w = iv.getWidth();
            int h = iv.getHeight();
            final Drawable d = iv.image.getDrawable();
            final int r = Math.min(d.getIntrinsicHeight(), d.getIntrinsicWidth());
            int[] xy = new int[2];
            iv.getLocationOnScreen(xy);
            final int cx = xy[0] + w/2;
            final int cy = xy[1] + h/2;
            iv.postDelayed(new Runnable() {
                @Override public void run() {
                    MediaActivity.transitionBitmap = Util.drawableToBitmap(iv.image.getDrawable());
                    int color = getResources().getColor(R.color.colorPrimaryDark);
                    if (MediaActivity.transitionBitmap != null) {
                        Palette palette = Palette.generate(MediaActivity.transitionBitmap);
                        color = palette.getMutedColor(color);
                    }
                    MediaActivity.launch(
                            GalleryActivity.this, iv.image, iv.index + "", new Point(cx, cy), r, color,
                            true, thread, iv.index
                    );
                }
            }, 200);
        }
    };

    // Data Loading ////////////////////////////////////////////////////////////////////////////////

    public boolean firstLoad;

    @Override protected void load() { load(false); }

    private void load(final boolean silent) {
        if (thread.dead) {
            loaded();
            if (!silent) showToast(R.string.no_thread);
            return;
        }

        super.load();
        if (silent || firstLoad) loadingBar.setVisibility(View.GONE);

        // Indicate ongoing request after all if first load takes especially long
        if (firstLoad) {
            galleryView.postDelayed(new Runnable() {
                @Override public void run() {
                    if (firstLoad && loading) {
                        loadingBar.setVisibility(View.VISIBLE);
                    }
                }
            }, 500);
        }

        thread.load(service, new Thread.OnResultCallback() {
            @Override public void onSuccess() {
                galleryAdapter.notifyDataSetChanged();
                firstLoad = false;
                loaded();
            }

            @Override public void onError(String message) {
                if (!silent) showToast(message);
                firstLoad = false;
                loaded();
            }
        });

        service.listThreads(this, boardName, new FutureCallback<List<ThreadPreview>>() {
            @Override public void onCompleted(Exception e, List<ThreadPreview> result) {
                if (e != null) return;
                boolean dead = true;
                for (ThreadPreview t : result) {
                    if (t.number.equals(threadNumber)) {
                        dead = false;
                        break;
                    }
                }
                if (dead) {
                    thread.dead = true;
                    setTitle(thread.titleForGallery());
                }
            }
        });
    }

    private void update() {
        if (loading) return;
        thread.resetUpdateTimer();
        load(true);
    }

    @Override protected void cancelPending() {
        super.cancelPending();
        // TODO: Reenable once https://github.com/koush/ion/issues/422 is fixed
        //ion.cancelAll(this);
    }

    // Lifecycle ///////////////////////////////////////////////////////////////////////////////////

    @Override protected void onResume() {
        super.onResume();
        thread.initBackgroundUpdater(prefs, new Runnable() {
            @Override public void run() {
                GalleryActivity.this.runOnUiThread(new Runnable() {
                    @Override public void run() {
                        update();
                    }
                });
            }
        });
        thread.resumeBackgroundUpdating();
    }

    @Override protected void onPause() {
        super.onPause();
        thread.pauseBackgroundUpdating();
    }

    @Override public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Util.updateRecyclerViewGridOnConfigChange(galleryView, R.dimen.column_width_gallery);
    }

    // Toolbar Menu ////////////////////////////////////////////////////////////////////////////////

    @Override public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.gallery, menu);
        return true;
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.saveAll:
                saveAll(this, imageSaver, boardName, threadNumber, thread.mediaPointers);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    public static void saveAll(Context context, ImageSaver imageSaver, String boardName, String threadNumber, List<MediaPointer> mediaPointers) {
        // From Clover
        if (mediaPointers.size() > 0) {
            List<ImageSaver.DownloadPair> list = new ArrayList<>();
            String folderName = ThreadPreview.generateTitle(boardName, threadNumber);
            for (MediaPointer p : mediaPointers) {
                list.add(new ImageSaver.DownloadPair(ApiModule.mediaUrl(boardName, p.id, p.ext), p.fileName()));
            }
            imageSaver.saveAll(context, folderName, list);
        }
    }

    // Adapters ////////////////////////////////////////////////////////////////////////////////////

    class GalleryAdapter extends UiAdapter<MediaPointer> {

        public GalleryAdapter(View.OnClickListener clickListener, View.OnLongClickListener longClickListener) {
            super(GalleryActivity.this, clickListener, longClickListener);
            this.items = thread.mediaPointers;
        }

        @Override public View newView(ViewGroup container) {
            return getLayoutInflater().inflate(R.layout.view_gallery_thumb, container, false);
        }

        @Override public void bindView(MediaPointer mediaPointer, int position, View view) {
            GalleryThumbView g = (GalleryThumbView) view;
            g.bindTo(ion, boardName, mediaPointer, position);
            ViewCompat.setTransitionName(g.image, position+"");
        }
    }
}
