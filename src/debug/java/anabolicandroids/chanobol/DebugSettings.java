package anabolicandroids.chanobol;

import android.content.SharedPreferences;
import android.os.Bundle;

import anabolicandroids.chanobol.util.BaseSettings;
import anabolicandroids.chanobol.util.Util;

// The following resources have been helpful:
// http://stackoverflow.com/a/3026922/283607
public class DebugSettings extends BaseSettings {

    public static final String MOCK = "pref_mock";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.debugprefs);
        toolbar.setTitle("Debug Settings");
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        if (key.equals(MOCK)) {
            Util.restartApp(getApplication(), this);
        }
    }
}
