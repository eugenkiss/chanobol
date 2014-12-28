package anabolicandroids.chanobol;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;

import javax.inject.Named;
import javax.inject.Singleton;

import anabolicandroids.chanobol.annotations.ForApplication;
import anabolicandroids.chanobol.api.ApiModule;
import anabolicandroids.chanobol.ui.PersistentData;
import anabolicandroids.chanobol.ui.Settings;
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
}
