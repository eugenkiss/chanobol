package anabolicandroids.chanobol.ui.posts.parsing;

import android.support.annotation.NonNull;
import android.text.TextPaint;

import anabolicandroids.chanobol.ui.scaffolding.ThemeContext;

public class ThreadSpan extends PostSpan {

    public final ThreadLink threadLink;

    public ThreadSpan(ThreadLink threadLink) {
        this.threadLink = threadLink;
    }

    @Override public void updateDrawState(@NonNull TextPaint ds) {
        ds.setColor(ThemeContext.getInstance().quoteColor);
        ds.setUnderlineText(true);
    }
}
