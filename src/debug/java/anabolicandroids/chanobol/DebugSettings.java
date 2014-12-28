package anabolicandroids.chanobol;

import android.annotation.TargetApi;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;

import com.squareup.picasso.Picasso;

import javax.inject.Inject;

import anabolicandroids.chanobol.ui.MainActivity;

import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

// The following resources have been helpful:
// http://stackoverflow.com/a/3026922/283607
public class DebugSettings extends BaseSettings {

    public static final String MOCK = "pref_mock";
    public static final String INDICATORS = "pref_indicators";

    @Inject Picasso picasso;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.debugprefs);
        toolbar.setTitle("Debug Settings");
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        if (key.equals(MOCK)) {
            // From U2020 DebugAppContainer.setEndpointAndRelaunch
            Intent newApp = new Intent(this, MainActivity.class);
            newApp.setFlags(FLAG_ACTIVITY_CLEAR_TASK | FLAG_ACTIVITY_NEW_TASK);
            getApplication().startActivity(newApp);
            App.get(getApplicationContext()).buildAppGraphAndInject();
        }
        if (key.equals(INDICATORS)) {
            picasso.setIndicatorsEnabled(prefs.getBoolean(key, true));
        }
    }
}
