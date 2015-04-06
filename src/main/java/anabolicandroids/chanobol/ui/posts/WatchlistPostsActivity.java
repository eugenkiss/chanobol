package anabolicandroids.chanobol.ui.posts;

import android.app.ActivityManager;
import android.os.Build;
import android.os.Bundle;

import anabolicandroids.chanobol.R;

// The main reason to have this class is to be able to give it it's own affinity
public class WatchlistPostsActivity extends PostsActivity {

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setTaskDescription(new ActivityManager.TaskDescription(getResources().getString(R.string.watchtask)));
        }
    }

}
