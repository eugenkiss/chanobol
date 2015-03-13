package anabolicandroids.chanobol.ui.posts.parsing;

import android.support.annotation.NonNull;
import android.text.TextPaint;

import anabolicandroids.chanobol.ui.ThemeHelper;

public class QuoteSpan extends PostSpan {

    public final String quoterId, quotedId;

    public QuoteSpan(String quoterId, String quotedId) {
        this.quoterId = quoterId;
        this.quotedId = quotedId;
    }

    @Override public void updateDrawState(@NonNull TextPaint ds) {
        ds.setColor(ThemeHelper.getInstance().getQuoteColor());
        ds.setUnderlineText(true);
    }
}
