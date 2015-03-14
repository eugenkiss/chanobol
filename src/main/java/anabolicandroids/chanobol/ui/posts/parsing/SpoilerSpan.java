package anabolicandroids.chanobol.ui.posts.parsing;

import android.support.annotation.NonNull;
import android.text.TextPaint;
import android.view.View;

import anabolicandroids.chanobol.ui.scaffolding.ThemeContext;

public class SpoilerSpan extends PostSpan {

    private boolean clicked = false;

    @Override public void onClick(View widget) {
        clicked = true;
    }

    @Override public void updateDrawState(@NonNull TextPaint ds) {
        if (!clicked) {
            ds.setColor(ThemeContext.getInstance().spoilerColor);
            ds.bgColor = ThemeContext.getInstance().spoilerColor;
            ds.setUnderlineText(false);
        }
    }
}
