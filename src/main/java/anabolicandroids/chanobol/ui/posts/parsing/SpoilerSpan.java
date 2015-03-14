package anabolicandroids.chanobol.ui.posts.parsing;

import android.support.annotation.NonNull;
import android.text.TextPaint;

import anabolicandroids.chanobol.ui.scaffolding.ThemeContext;

public class SpoilerSpan extends PostSpan {

    private boolean clicked = false;

    @Override public void updateDrawState(@NonNull TextPaint ds) {
        if (!clicked) {
            ds.setColor(ThemeContext.getInstance().getSpoilerColor());
            ds.bgColor = ThemeContext.getInstance().getSpoilerColor();
            ds.setUnderlineText(false);
        }
    }
}
