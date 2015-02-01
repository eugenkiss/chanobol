package anabolicandroids.chanobol.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;

import anabolicandroids.chanobol.App;
import anabolicandroids.chanobol.BaseSettings;
import anabolicandroids.chanobol.R;

import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

// The following resources have been helpful:
// http://stackoverflow.com/a/3026922/283607
public class Settings extends BaseSettings {

    public static final String HIDABLE_TOOLBAR = "pref_hidable_toolbar";
    public static final String PRELOAD_THUMBNAILS = "pref_preload_thumbnails";
    public static final String REFRESH = "pref_refresh";
    public static final String TRANSITIONS = "pref_transitions";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.prefs);
        Preference p = findPreference(TRANSITIONS);
        if (Build.VERSION.SDK_INT < 21) {
            p.setSummary("Only relevant for Lollipop devices");
            p.setEnabled(false);
        } else {
            p.setSummary("Requires Restart. Experimental");
        }

    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        if (key.equals(HIDABLE_TOOLBAR)) {
            // For the case that the toolbar is not fully protracted and the user
            // disables the auto-hiding toolbar it needs to be fully protracted afterwards.
            App.needToProtractToolbar = true;
        }
        if (key.equals(TRANSITIONS)) {
            Intent newApp = new Intent(this, MainActivity.class);
            newApp.setFlags(FLAG_ACTIVITY_CLEAR_TASK | FLAG_ACTIVITY_NEW_TASK);
            getApplication().startActivity(newApp);
            App.get(getApplicationContext()).buildAppGraphAndInject();
        }
    }
}
