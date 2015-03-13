package anabolicandroids.chanobol.ui.posts.parsing;

import android.support.annotation.NonNull;
import android.text.TextPaint;

import anabolicandroids.chanobol.ui.ThemeHelper;

public class SpoilerSpan extends PostSpan {

    private boolean clicked = false;

    @Override public void updateDrawState(@NonNull TextPaint ds) {
        if (!clicked) {
            ds.setColor(ThemeHelper.getInstance().getSpoilerColor());
            ds.bgColor = ThemeHelper.getInstance().getSpoilerColor();
            ds.setUnderlineText(false);
        }
    }
}
