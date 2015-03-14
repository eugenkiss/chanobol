package anabolicandroids.chanobol.api;

import android.app.Application;
import android.content.Context;

import com.koushikdutta.ion.Ion;

import javax.inject.Singleton;

import anabolicandroids.chanobol.annotations.ForApplication;
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
}
