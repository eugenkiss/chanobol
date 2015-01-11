package anabolicandroids.chanobol.ui;

import android.content.SharedPreferences;
import android.os.Bundle;

import anabolicandroids.chanobol.BaseSettings;
import anabolicandroids.chanobol.R;

// The following resources have been helpful:
// http://stackoverflow.com/a/3026922/283607
public class Settings extends BaseSettings {

    public static final String HIDABLE_TOOLBAR = "pref_hidable_toolbar";
    public static final String PRELOAD_THUMBNAILS = "pref_preload_thumbnails";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.prefs);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        // Do Nothing
    }
}
