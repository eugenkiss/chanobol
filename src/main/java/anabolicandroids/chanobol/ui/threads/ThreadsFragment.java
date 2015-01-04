package anabolicandroids.chanobol.ui.threads;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import anabolicandroids.chanobol.R;
import anabolicandroids.chanobol.api.ApiModule;
import anabolicandroids.chanobol.api.ChanService;
import anabolicandroids.chanobol.api.data.Thread;
import anabolicandroids.chanobol.ui.SwipeRefreshFragment;
import anabolicandroids.chanobol.ui.UiAdapter;
import anabolicandroids.chanobol.ui.posts.PostsFragment;
import butterknife.InjectView;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class ThreadsFragment extends SwipeRefreshFragment {
    @InjectView(R.id.threads) GridView threadsView;

    Menu menu;
    String name;
    ArrayList<Thread> threads;
    HashMap<String, Integer> threadMap;
    ThreadsAdapter threadsAdapter;

    // TODO: Isn't there a more elegant solution?
    long lastUpdate;
    static final long updateInterval = 30_000;
    boolean pauseUpdating = false;
    private final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);

    public static ThreadsFragment create(String name) {
        ThreadsFragment f = new ThreadsFragment();
        f.name = name;
        return f;
    }

    @Override
    protected int getLayoutResource() {
        return R.layout.fragment_threads;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        threads = new ArrayList<>();
        threadMap = new HashMap<>();
        threadsAdapter = new ThreadsAdapter();
        threadsView.setAdapter(threadsAdapter);

        load();
        executor.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                if (pauseUpdating) return;
                if (System.currentTimeMillis() - lastUpdate > updateInterval) {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            update();
                        }
                    });
                }
            }
        }, 5000, 5000, TimeUnit.MILLISECONDS);

        threadsView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Thread thread = threads.get(position);
                if (thread.dead) {
                    showToast("Thread is dead");
                    return;
                }
                pauseUpdating = true;
                ImageView iv = (ImageView) view.findViewById(R.id.image);
                PostsFragment f = PostsFragment.create(name, thread.toOpPost(), iv.getDrawable());
                // Doesn't work, see: https://code.google.com/p/android/issues/detail?id=82832&thanks=82832&ts=1418767240
                //f.setEnterTransition(new Slide(Gravity.RIGHT));
                //f.setExitTransition(new Slide(Gravity.RIGHT));
                startFragment(f);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        activity.setTitle(name);
        if (menu != null) menu.setGroupVisible(R.id.threads, true);
        update();
        pauseUpdating = false;
    }

    @Override
    public void onStop() {
        super.onPause();
        pauseUpdating = true;
    }

    @Override
    protected void load() {
        super.load();
        service.listThreads(name, new Callback<List<ChanService.Threads>>() {
            @Override
            public void success(List<ChanService.Threads> pages, Response response) {
                threads.clear();
                threadMap.clear();
                int position = 0;
                for (ChanService.Threads page : pages) {
                    for (Thread thread : page.threads) {
                        threads.add(thread);
                        threadMap.put(thread.id, position);
                        position++;
                        // Preload thumbnails
                        if (thread.imageId != null)
                            picasso.load(ApiModule.thumbUrl(name, thread.imageId)).fetch();
                    }
                }
                threadsAdapter.notifyDataSetChanged();
                loaded();
                lastUpdate = System.currentTimeMillis();
            }

            @Override
            public void failure(RetrofitError error) {
                showToast(error.getMessage());
                System.out.println(error.getMessage());
                loaded();
            }
        });
    }

    @Override
    protected void cancelPending() {
        super.cancelPending();
        // TODO: Use Observables or Retrofit 2.0 to cancel listPosts and Picasso
    }

    private void update() {
        if (loading) return;
        lastUpdate = System.currentTimeMillis();
        service.listThreads(name, new Callback<List<ChanService.Threads>>() {
            @Override
            public void success(List<ChanService.Threads> pages, Response response) {
                if (loading) return; // cancel on real refresh
                boolean[] positions = new boolean[threads.size()];
                for (ChanService.Threads page : pages) {
                    for (Thread thread : page.threads) {
                        Integer position = threadMap.get(thread.id);
                        if (position != null) {
                            positions[position] = true;
                            threads.set(position, thread);
                        }
                    }
                }
                for (int i = 0; i < positions.length; i++)
                    if (!positions[i]) threads.get(i).dead = true;
                threadsAdapter.notifyDataSetChanged();
                lastUpdate = System.currentTimeMillis();
            }

            @Override
            public void failure(RetrofitError error) {
                System.out.println(error.getMessage());
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.threads, menu);
        this.menu = menu;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.refresh:
                load();
                break;
            case R.id.up:
                threadsView.setSelection(0);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    class ThreadsAdapter extends UiAdapter<Thread> {

        public ThreadsAdapter() {
            super(context);
            this.items = threads;
        }

        @Override
        public View newView(LayoutInflater inflater, int position, ViewGroup container) {
            return inflater.inflate(R.layout.view_thread, container, false);
        }

        @Override
        public void bindView(Thread item, int position, View view) {
            ((ThreadView) view).bindTo(item, name, picasso);
        }
    }
}

