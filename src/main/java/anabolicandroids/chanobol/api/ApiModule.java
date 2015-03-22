package anabolicandroids.chanobol.api;

import android.app.Application;
import android.content.Context;

import com.koushikdutta.ion.Ion;

import java.io.File;

import javax.inject.Singleton;

import anabolicandroids.chanobol.App;
import anabolicandroids.chanobol.annotations.ForApplication;
import anabolicandroids.chanobol.util.FileCache;
import dagger.Module;
import dagger.Provides;

// https://github.com/4chan/4chan-API
@SuppressWarnings("UnusedDeclaration")
@Module(
        complete = false,
        library = true
)
public class ApiModule {
    public static final String endpoint = "http://a.4cdn.org";
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

    @Provides @Singleton
    Ion provideIon(Application app) {
        return Ion.getDefault(app);
    }

    @Provides @Singleton
    ChanService provideChanService(@ForApplication Context context, Ion ion) {
        return new ChanService(context, ion);
    }

    // From Clover
    private static final long FILE_CACHE_DISK_SIZE = 50 * 1024 * 1024; // 50mb
    private static final String FILE_CACHE_NAME = "filecache";

    @Provides @Singleton
    FileCache provideFileCache(App app, Ion ion) {
        File cacheDir = app.getExternalCacheDir() != null ? app.getExternalCacheDir() : app.getCacheDir();
        return new FileCache(new File(cacheDir, FILE_CACHE_NAME), FILE_CACHE_DISK_SIZE, ion);
    }
}
