package anabolicandroids.chanobol;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;

import javax.inject.Named;
import javax.inject.Singleton;

import anabolicandroids.chanobol.annotations.ForApplication;
import anabolicandroids.chanobol.annotations.Nsfw;
import anabolicandroids.chanobol.annotations.SfwMode;
import anabolicandroids.chanobol.api.ApiModule;
import anabolicandroids.chanobol.api.data.Board;
import anabolicandroids.chanobol.ui.PersistentData;
import anabolicandroids.chanobol.ui.Settings;
import anabolicandroids.chanobol.util.Util;
import dagger.Module;
import dagger.Provides;

@Module(
        injects = {
                App.class,
                Settings.class
        },
        includes = {
                ApiModule.class
        },
        library = true
)
public class AppModule {
    private App app;

    public AppModule(App app) {
        this.app = app;
    }

    @Provides @Singleton
    Application provideApplication() {
        return app;
    }

    @Provides @Singleton @ForApplication
    Context provideApplicationContext() {
        return app;
    }

    @Provides @Singleton
    LayoutInflater provideLayoutInflater() {
        return (LayoutInflater) app.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Provides @Singleton
    Resources provideResources() {
        return app.getResources();
    }

    @Provides @Singleton
    AssetManager provideAssetManager() {
        return app.getAssets();
    }

    @Provides @Singleton
    SharedPreferences provideSharedPreferences(Application app) {
        return PreferenceManager.getDefaultSharedPreferences(app);
    }

    @Provides @Singleton
    PersistentData providePersistentData(SharedPreferences prefs) {
        return new PersistentData(prefs);
    }

    @Provides @Singleton @Named("DebugSettings")
    Class provideDebugSettings() {
        return null;
    }

    @Provides @Singleton @SfwMode
    boolean provideSfwMode() {
        return false;
    }

    @Provides @Singleton
    List<Board> provideBoards(@ForApplication Context context, @SfwMode boolean sfw) {
        return getBoards(context, sfw);
    }

    @Provides @Singleton @Nsfw
    List<Board> provideAllBoards(@ForApplication Context context) {
        return getBoards(context, false);
    }

    private static List<Board> getBoards(Context context, boolean sfw) {
        String path = sfw ? "boards_sfw.json" : "boards.json";
        String boardsJson = Util.loadJSONFromAsset(context, path);
        JsonObject root = new JsonParser().parse(boardsJson).getAsJsonObject();
        Type type = new TypeToken<List<Board>>() {}.getType();
        return new Gson().fromJson(root.get("boards").getAsJsonArray(), type);
    }
}
