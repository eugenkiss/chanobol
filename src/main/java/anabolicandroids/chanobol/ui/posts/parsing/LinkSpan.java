package anabolicandroids.chanobol.ui.posts.parsing;

import android.support.annotation.NonNull;
import android.text.TextPaint;

import anabolicandroids.chanobol.ui.ThemeContext;

public class LinkSpan extends PostSpan {

    public final String url;

    public LinkSpan(String url) {
        this.url = url;
    }

    @Override public void updateDrawState(@NonNull TextPaint ds) {
        ds.setColor(ThemeContext.getInstance().getLinkColor());
        ds.setUnderlineText(true);
    }
}
