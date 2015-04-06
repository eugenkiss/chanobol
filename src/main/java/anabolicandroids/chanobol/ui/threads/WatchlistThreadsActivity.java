package anabolicandroids.chanobol.ui.threads;

import android.app.ActivityManager;
import android.os.Build;
import android.os.Bundle;

import anabolicandroids.chanobol.R;

// The main reason to have this class is to be able to give it it's own affinity
public class WatchlistThreadsActivity extends ThreadsActivity {

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setTaskDescription(new ActivityManager.TaskDescription(getResources().getString(R.string.watchtask)));
        }
    }

}
