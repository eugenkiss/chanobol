package anabolicandroids.chanobol;

import android.app.Application;
import android.content.Context;

import com.crashlytics.android.Crashlytics;

import anabolicandroids.chanobol.util.Util;
import dagger.ObjectGraph;
import io.fabric.sdk.android.Fabric;
import timber.log.Timber;

public class App extends Application {
    // This variable is needed to communicate between the Settings and the Main activity.
    // Using a static variable is the easiest solution. Note that having a static variable
    // in the Application class is safe with respect to stale references so it's all good.
    public static boolean needToProtractToolbar = false;
    public static boolean firstStart;
    // As a fallback
    public static int screenWidth;
    public static int screenHeight;

    private ObjectGraph appGraph;

    @Override
    public void onCreate() {
        super.onCreate();
        Fabric.with(this, new Crashlytics());
        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        }
        buildAppGraphAndInject();

        firstStart = true;
        screenWidth = Util.getScreenWidth(this);
        screenHeight = Util.getScreenHeight(this);
    }

    public void buildAppGraphAndInject() {
        appGraph = ObjectGraph.create(Modules.list(this));
        appGraph.inject(this);
    }

    public void inject(Object o) { appGraph.inject(o); }

    public ObjectGraph getAppGraph() {
        return appGraph;
    }

    public static App get(Context context) {
        return (App) context.getApplicationContext();
    }
}
