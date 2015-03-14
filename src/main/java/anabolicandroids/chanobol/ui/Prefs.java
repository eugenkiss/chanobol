package anabolicandroids.chanobol.ui;

import android.content.SharedPreferences;

// Convenience wrapper around SharedPreferences to make use of Java's static nature
public class Prefs {

    private final SharedPreferences p;

    public Prefs(SharedPreferences sharedPrefs) {
        this.p = sharedPrefs;
    }

    public static final String THEME = "pref_theme";
    public Theme theme() {
        String name = p.getString("pref_theme", Theme.LIGHT.name);
        if (name.equals(Theme.LIGHT.name)) return Theme.LIGHT;
        if (name.equals(Theme.DARK.name)) return Theme.DARK;
        return Theme.LIGHT;
    }
    public void theme(Theme theme) {
        p.edit().putString(THEME, theme.name).apply();
    }

    public static final String HIDABLE_TOOLBAR = "pref_hidable_toolbar";
    public boolean hidableToolbar() {
        return p.getBoolean(HIDABLE_TOOLBAR, true);
    }
    public void hidableToolbar(boolean v) {
        p.edit().putBoolean(HIDABLE_TOOLBAR, v).apply();
    }

    public static final String PRELOAD_THUMBNAILS = "pref_preload_thumbnails";
    public boolean preloadThumbnails() {
        return p.getBoolean(PRELOAD_THUMBNAILS, true);
    }
    public void preloadThumbnails(boolean v) {
        p.edit().putBoolean(PRELOAD_THUMBNAILS, v).apply();
    }

    public static final String AUTO_REFRESH = "pref_refresh";
    public boolean autoRefresh() {
        return p.getBoolean(AUTO_REFRESH, true);
    }
    public void autoRefresh(boolean v) {
        p.edit().putBoolean(AUTO_REFRESH, v).apply();
    }
}
