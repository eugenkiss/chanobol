package anabolicandroids.chanobol;

import android.app.Application;
import android.preference.PreferenceManager;
import android.util.Log;

import com.koushikdutta.ion.Ion;

import javax.inject.Named;
import javax.inject.Singleton;

import anabolicandroids.chanobol.api.MockImageLoader;
import anabolicandroids.chanobol.api.MockRestLoader;
import dagger.Module;
import dagger.Provides;

@Module(
        injects = DebugSettings.class,
        addsTo = AppModule.class, // TODO: Why is this needed?
        library = true,
        overrides = true
)
public final class DebugAppModule {

    @Provides @Singleton @MockMode
    boolean provideMockMode(Application app) {
        return PreferenceManager.getDefaultSharedPreferences(app).getBoolean(DebugSettings.MOCK, true);
    }

    @Provides @Singleton
    Ion provideIon(Application app) {
        Ion ion = Ion.getDefault(app);
        ion.configure()
                .setLogging("ion", Log.VERBOSE)
                .addLoader(0, new MockRestLoader())
                .addLoader(0, new MockImageLoader())
        ;
        return ion;
    }

    @Provides @Singleton @Named("DebugSettings")
    Class provideDebugSettingsClass() {
        return DebugSettings.class;
    }
}
