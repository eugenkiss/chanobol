package anabolicandroids.chanobol.ui.scaffolding;

import anabolicandroids.chanobol.R;

public enum Theme {
    LIGHT("light", R.style.AppTheme, true),
    DARK("dark", R.style.AppTheme_Dark, false);

    public String name;
    public int resValue;
    public boolean isLightTheme;

    Theme(String name, int resValue, boolean isLightTheme) {
        this.name = name;
        this.resValue = resValue;
        this.isLightTheme = isLightTheme;
    }
}
