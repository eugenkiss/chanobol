package anabolicandroids.chanobol;

import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.crashlytics.android.Crashlytics;

import dagger.ObjectGraph;
import io.fabric.sdk.android.Fabric;
import timber.log.Timber;

public class App extends Application {
    // This variable is needed to communicate between the Settings and the Main activity.
    // Using a static variable is the easiest solution. Note that having a static variable
    // in the Application class is safe with respect to stale references so it's all good.
    public static boolean needToProtractToolbar = false;

    private ObjectGraph appGraph;

    @Override
    public void onCreate() {
        super.onCreate();
        Fabric.with(this, new Crashlytics());
        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        }
        buildAppGraphAndInject();
        //Thread.setDefaultUncaughtExceptionHandler(_unCaughtExceptionHandler);
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


    // Global Exception Handling ///////////////////////////////////////////////////////////////////
    // see http://stackoverflow.com/a/8943671/283607 and http://stackoverflow.com/a/26560727/283607
    // In order to prevent a certain non-critical exception to kill the app.

    private Thread.UncaughtExceptionHandler defaultUEH =
            Thread.getDefaultUncaughtExceptionHandler();
    private Thread.UncaughtExceptionHandler _unCaughtExceptionHandler =
            new Thread.UncaughtExceptionHandler() {
                @Override
                public void uncaughtException(Thread thread, Throwable ex) {
                    // This one happens when you click on a "pseudo-URL" in a post text which
                    // is somehow interpreteded as an intent to start an activity. For example
                    // in a link to a thread in another board s.th. like ">>>>ck/23423".
                    // A better parsing and transformation solution in PostView would render
                    // this band-aid obsolete. See issue #3.
                    // Note: This doesn't really work the application still crashes... but I'll
                    // keep it here maybe I'll improve it.
                    if (ex.getMessage().contains("Calling startActivity() from outside")) {
                        Timber.i("Ignoring the following exception");
                        Timber.e(ex.getMessage());
                        return;
                    }
                    System.exit(2);
                    // re-throw critical exception further to the os (important)
                    defaultUEH.uncaughtException(thread, ex);
                }
            };
}
