package anabolicandroids.chanobol.api;

import android.app.Application;

import com.koushikdutta.ion.Ion;
import com.squareup.okhttp.Cache;
import com.squareup.okhttp.OkHttpClient;

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
    Ion provideIon(Application app) {
        return Ion.getDefault(app);
    }

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
