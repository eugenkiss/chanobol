/*
 * Clover - 4chan browser https://github.com/Floens/Clover/
 * Copyright (C) 2014  Floens
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package anabolicandroids.chanobol.ui.scaffolding;

import android.content.Context;
import android.content.res.TypedArray;

import anabolicandroids.chanobol.R;

// Adapted from Clover

// In order to have a global (dynamically bound would be better in an ideal world) way to
// get the current theme's respective colors for the PostView (see e.g. QuoteSpan's updateDrawState).
// A non-global solution would be much too inconvenient.
public class ThemeContext {

    private int quoteColor;
    private int highlightQuoteColor;
    private int linkColor;
    private int spoilerColor;
    private int inlineQuoteColor;
    private int codeTagSize;

    private ThemeContext() { }
    private static ThemeContext instance;
    public static ThemeContext getInstance() {
        if (instance == null) instance = new ThemeContext();
        return instance;
    }

    public void reloadPostViewColors(Context context) {
        TypedArray ta = context.obtainStyledAttributes(null, R.styleable.PostView, R.attr.post_style, 0);
        quoteColor = ta.getColor(R.styleable.PostView_quote_color, 0);
        highlightQuoteColor = ta.getColor(R.styleable.PostView_highlight_quote_color, 0);
        linkColor = ta.getColor(R.styleable.PostView_link_color, 0);
        spoilerColor = ta.getColor(R.styleable.PostView_spoiler_color, 0);
        inlineQuoteColor = ta.getColor(R.styleable.PostView_inline_quote_color, 0);
        codeTagSize = ta.getDimensionPixelSize(R.styleable.PostView_code_tag_size, 0);
        ta.recycle();
    }

    public int getQuoteColor() {
        return quoteColor;
    }

    public int getHighlightQuoteColor() {
        return highlightQuoteColor;
    }

    public int getLinkColor() {
        return linkColor;
    }

    public int getSpoilerColor() {
        return spoilerColor;
    }

    public int getInlineQuoteColor() {
        return inlineQuoteColor;
    }

    public int getCodeTagSize() {
        return codeTagSize;
    }

}
