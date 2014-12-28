package anabolicandroids.chanobol;

import android.app.Application;
import android.content.Context;

import dagger.ObjectGraph;
import timber.log.Timber;

public class App extends Application {
    private ObjectGraph appGraph;

    @Override
    public void onCreate() {
        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        }
        buildAppGraphAndInject();
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
