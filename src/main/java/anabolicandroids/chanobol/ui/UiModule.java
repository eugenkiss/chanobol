package anabolicandroids.chanobol.ui;

import android.content.Context;

import javax.inject.Singleton;

import anabolicandroids.chanobol.AppModule;
import anabolicandroids.chanobol.annotations.ForActivity;
import anabolicandroids.chanobol.ui.boards.BoardsFragment;
import anabolicandroids.chanobol.ui.boards.FavoritesFragment;
import anabolicandroids.chanobol.ui.images.GalleryFragment;
import anabolicandroids.chanobol.ui.images.ImageFragment;
import anabolicandroids.chanobol.ui.posts.PostsDialog;
import anabolicandroids.chanobol.ui.posts.PostsFragment;
import anabolicandroids.chanobol.ui.threads.ThreadsFragment;
import dagger.Module;
import dagger.Provides;

@Module(
        injects = {
                MainActivity.class,
                BoardsFragment.class,
                FavoritesFragment.class,
                ThreadsFragment.class,
                PostsFragment.class,
                PostsDialog.class,
                ImageFragment.class,
                GalleryFragment.class
        },
        addsTo = AppModule.class,
        library = true
)
public class UiModule {
    private final MainActivity activity;

    public UiModule(MainActivity activity) {
        this.activity = activity;
    }

    @Provides @Singleton @ForActivity
    Context provideActivityContext() {
        return activity;
    }

    @Provides @Singleton
    MainActivity provideMainActivity() {
        return activity;
    }
}
