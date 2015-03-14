package anabolicandroids.chanobol.util;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

import javax.inject.Inject;

import anabolicandroids.chanobol.App;
import anabolicandroids.chanobol.R;
import anabolicandroids.chanobol.ui.scaffolding.Prefs;
import dagger.ObjectGraph;

// The following resources have been helpful:
// http://stackoverflow.com/a/3026922/283607
public abstract class BaseSettings extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    @Inject Prefs prefs;

    protected Toolbar toolbar;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        App app = (App) getApplication();
        ObjectGraph activityGraph = app.getAppGraph();
        activityGraph.inject(this);

        Util.setTheme(this, prefs);
        setContentView(R.layout.activity_settings);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }
}
