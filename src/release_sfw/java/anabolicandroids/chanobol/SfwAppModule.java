package anabolicandroids.chanobol;

import javax.inject.Singleton;

import anabolicandroids.chanobol.annotations.SfwMode;
import dagger.Module;
import dagger.Provides;

@Module(
        addsTo = AppModule.class,
        library = true,
        overrides = true
)
public final class DebugAppModule {

    @Provides @Singleton @SfwMode
    boolean provideSfwMode() {
        return true;
    }
}
