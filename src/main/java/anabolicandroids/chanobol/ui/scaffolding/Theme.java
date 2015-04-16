package anabolicandroids.chanobol.ui.scaffolding;

import anabolicandroids.chanobol.R;

public enum Theme {
    LIGHT("light", R.style.AppTheme, true),
    DARK("dark", R.style.AppTheme_Dark, false),
    TEAL("teal", R.style.AppTheme_Teal, true),
    BLUE1("blue1", R.style.AppTheme_Blue1, true),
    BLUE2("blue2", R.style.AppTheme_Blue2, true),
    GRAY("gray", R.style.AppTheme_Gray, true),
    ;

    public String name;
    public int resValue;
    public boolean isLightTheme;

    Theme(String name, int resValue, boolean isLightTheme) {
        this.name = name;
        this.resValue = resValue;
        this.isLightTheme = isLightTheme;
    }
}
