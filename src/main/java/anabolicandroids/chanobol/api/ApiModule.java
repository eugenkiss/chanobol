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
    public static final String endpoint = "https://a.4cdn.org";
    // https://t.4cdn.org/board/1358180697001s.jpg
    public static final String thumbCdn = "https://t.4cdn.org";
    // https://i.4cdn.org/board/1358180697001.ext
    public static final String mediaCdn = "https://i.4cdn.org";

    public static String thumbUrl(String board, String mediaId) {
        return thumbCdn + "/" + board + "/" + mediaId + "s.jpg";
    }

    public static String mediaUrl(String board, String mediaId, String ending) {
        return mediaCdn + "/" + board + "/" + mediaId + ending;
    }

    // https://boards.4chan.org/fit/thread/31627542
    public static String threadUrl(String board, String number) {
        return "https://boards.4chan.org/" + board + "/thread/" + number;
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
