package anabolicandroids.chanobol;

import android.app.Application;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.picasso.OkHttpDownloader;
import com.squareup.picasso.Picasso;

import javax.inject.Named;
import javax.inject.Singleton;

import anabolicandroids.chanobol.api.ChanService;
import anabolicandroids.chanobol.api.MockChanService;
import anabolicandroids.chanobol.api.MockDownloader;
import dagger.Module;
import dagger.Provides;
import retrofit.MockRestAdapter;
import retrofit.RestAdapter;
import timber.log.Timber;

@Module(
        injects = DebugSettings.class,
        addsTo = AppModule.class, // TODO: Why is this needed?
        library = true,
        overrides = true
)
public final class DebugAppModule {

    @Provides @Singleton @MockMode boolean provideMockMode(Application app) {
        return PreferenceManager.getDefaultSharedPreferences(app).getBoolean(DebugSettings.MOCK, true);
    }

    @Provides @Singleton @DebugIndicators boolean provideDebugIndicators(Application app) {
        return PreferenceManager.getDefaultSharedPreferences(app).getBoolean(DebugSettings.INDICATORS, true);
    }

    @Provides
    @Singleton
    MockRestAdapter provideMockRestAdapter(RestAdapter restAdapter, SharedPreferences preferences) {
        MockRestAdapter mockRestAdapter = MockRestAdapter.from(restAdapter);
        //AndroidMockValuePersistence.install(mockRestAdapter, preferences);
        //mockRestAdapter.setErrorPercentage(100);
        return mockRestAdapter;
    }

    @Provides @Singleton
    Picasso providePicasso(OkHttpClient client, MockRestAdapter mockRestAdapter, Application app,
                           @MockMode boolean mockMode, @DebugIndicators boolean indicators) {
        Picasso.Builder builder = new Picasso.Builder(app);
        if (mockMode) {
            builder.downloader(new MockDownloader(mockRestAdapter, app.getAssets()));
        } else {
            builder.downloader(new OkHttpDownloader(client));
        }
        builder.indicatorsEnabled(indicators)
            .listener(new Picasso.Listener() {
                @Override public void onImageLoadFailed(Picasso picasso, Uri uri, Exception e) {
                    Timber.e(e, "Failed to load image: %s", uri);
                }
            });
        return builder.build();
    }

    @Provides @Singleton
    ChanService provideChanService(RestAdapter restAdapter, @MockMode boolean mockMode,
                                   MockRestAdapter mockRestAdapter, MockChanService mockService) {
        if (mockMode) return mockRestAdapter.create(ChanService.class, mockService);
        return restAdapter.create(ChanService.class);
    }

    @Provides @Singleton @Named("DebugSettings")
    Class provideDebugSettingsClass() {
        return DebugSettings.class;
    }

}
