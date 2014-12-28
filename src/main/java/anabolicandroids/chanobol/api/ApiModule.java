package anabolicandroids.chanobol.api;

import android.app.Application;
import android.net.Uri;

import com.squareup.okhttp.Cache;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.picasso.OkHttpDownloader;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.IOException;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import retrofit.RestAdapter;
import retrofit.client.Client;
import retrofit.client.OkClient;
import timber.log.Timber;

// https://github.com/4chan/4chan-API
@Module(
        complete = false,
        library = true
)
public class ApiModule {
    // http://t.4cdn.org/board/1358180697001s.jpg
    public static final String thumbCdn = "http://t.4cdn.org";
    // http://i.4cdn.org/board/1358180697001.ext
    public static final String imgCdn = "http://i.4cdn.org";

    public static String thumbUrl(String board, String imageId) {
        return thumbCdn + "/" + board + "/" + imageId + "s.jpg";
    }

    public static String imgUrl(String board, String imageId, String ending) {
        return imgCdn + "/" + board + "/" + imageId + ending;
    }

    static final int DISK_CACHE_SIZE = 100 * 1024 * 1024; // 100MB

    @Provides @Singleton
    RestAdapter provideRestAdapter(Client client) {
        return new RestAdapter.Builder()
                .setClient(client)
                .setEndpoint("http://a.4cdn.org")
                .build();
    }

    @Provides @Singleton
    ChanService provideChanService(RestAdapter restAdapter) {
        return restAdapter.create(ChanService.class);
    }

    @Provides @Singleton
    Picasso providePicasso(Application app, OkHttpClient client) {
        return new Picasso.Builder(app)
                .downloader(new OkHttpDownloader(client))
                .listener(new Picasso.Listener() {
                    @Override public void onImageLoadFailed(Picasso picasso, Uri uri, Exception e) {
                        Timber.e(e, "Failed to load image: %s", uri);
                    }
                })
                .build();
    }

    @Provides @Singleton
    Client provideClient(OkHttpClient client) {
        return new OkClient(client);
    }

    @Provides @Singleton
    OkHttpClient provideOkHttpClient(Application app) {
        return createOkHttpClient(app);
    }

    static OkHttpClient createOkHttpClient(Application app) {
        OkHttpClient client = new OkHttpClient();
        // Install an HTTP cache in the application cache directory.
        try {
            File cacheDir = new File(app.getCacheDir(), "http");
            Cache cache = new Cache(cacheDir, DISK_CACHE_SIZE);
            client.setCache(cache);
        } catch (IOException e) {
            Timber.e(e, "Unable to install disk cache.");
        }
        return client;
    }
}
