package anabolicandroids.chanobol.ui.posts.parsing;

import android.support.annotation.NonNull;
import android.text.TextPaint;

import anabolicandroids.chanobol.ui.ThemeHelper;

public class ThreadSpan extends PostSpan {

    public final ThreadLink threadLink;

    public ThreadSpan(ThreadLink threadLink) {
        this.threadLink = threadLink;
    }

    @Override public void updateDrawState(@NonNull TextPaint ds) {
        ds.setColor(ThemeHelper.getInstance().getQuoteColor());
        ds.setUnderlineText(true);
    }
}
