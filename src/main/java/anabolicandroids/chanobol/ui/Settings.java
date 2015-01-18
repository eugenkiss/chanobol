package anabolicandroids.chanobol.ui;

import android.content.SharedPreferences;
import android.os.Bundle;

import anabolicandroids.chanobol.App;
import anabolicandroids.chanobol.BaseSettings;
import anabolicandroids.chanobol.R;

// The following resources have been helpful:
// http://stackoverflow.com/a/3026922/283607
public class Settings extends BaseSettings {

    public static final String HIDABLE_TOOLBAR = "pref_hidable_toolbar";
    public static final String PRELOAD_THUMBNAILS = "pref_preload_thumbnails";
    public static final String REFRESH = "pref_refresh";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.prefs);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        if (key.equals(HIDABLE_TOOLBAR)) {
            // For the case that the toolbar is not fully protracted and the user
            // disables the auto-hiding toolbar it needs to be fully protracted afterwards.
            App.needToProtractToolbar = true;
        }
    }
}
