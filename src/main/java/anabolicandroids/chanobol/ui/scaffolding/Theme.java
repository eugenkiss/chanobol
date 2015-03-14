package anabolicandroids.chanobol.ui.scaffolding;

import anabolicandroids.chanobol.R;

public enum Theme {
    DARK("dark", R.style.AppTheme, false),
    LIGHT("light", R.style.AppTheme_Light, true);

    public String name;
    public int resValue;
    public boolean isLightTheme;

    Theme(String name, int resValue, boolean isLightTheme) {
        this.name = name;
        this.resValue = resValue;
        this.isLightTheme = isLightTheme;
    }
}
