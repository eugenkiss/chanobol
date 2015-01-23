package anabolicandroids.chanobol.util;

import android.content.Context;
import android.support.v7.widget.CardView;
import android.util.AttributeSet;

// TODO: I gave up making this work it's unbelievably hard
// I want the postview to not be too wide in landscape mode so as to make reading easier.
// That means, the posts should not take all the available width but only to a certain maximum
// width and be centered.
// It's not enough to put "android:maxWidth" into the xml node, though—that would be too easy.
// It's fucking horribly complex to achieve this seemingly easy task in Android!
// See http://stackoverflow.com/a/11654395/283607
// This is extended by me to make the cardviews centered inside the recyclerview. Needed some hacks.
public class MaxWidthCardView extends CardView {
    public MaxWidthCardView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    /*
    private final int mMaxWidth;
    private int screenWidth;
    private int measuredWidth;

    public MaxWidthCardView(Context context) {
        super(context);
        mMaxWidth = 0;
    }

    public MaxWidthCardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.MaxWidthCardView);
        mMaxWidth = a.getDimensionPixelSize(R.styleable.MaxWidthCardView_maxWidth, Integer.MAX_VALUE);
        a.recycle();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int measuredWidth = MeasureSpec.getSize(widthMeasureSpec);
        //MarginLayoutParams lps = (MarginLayoutParams) getLayoutParams();
        ViewGroup.LayoutParams layoutParams = getLayoutParams();
        if (mMaxWidth > 0 && mMaxWidth < measuredWidth) {
            int measureMode = MeasureSpec.getMode(widthMeasureSpec);
            widthMeasureSpec = MeasureSpec.makeMeasureSpec(mMaxWidth, measureMode);
            // Such a hack...
            screenWidth = Util.getScreenWidth(getContext());
            int leftMargin = (int) Math.round((screenWidth - mMaxWidth) / 2.0);
            if (layoutParams instanceof MarginLayoutParams) {
                MarginLayoutParams lps = (MarginLayoutParams) layoutParams;
                lps.leftMargin = leftMargin;
            }
            // Holy shit http://stackoverflow.com/a/21186703/283607
            if (layoutParams instanceof AbsListView.LayoutParams && getParent() instanceof ListView) {
                ListView lv = (ListView) getParent();
                lv.setPadding(leftMargin, lv.getPaddingTop(), lv.getPaddingRight(), lv.getPaddingBottom());
            }
        } else {
            if (layoutParams instanceof MarginLayoutParams) {
                MarginLayoutParams lps = (MarginLayoutParams) layoutParams;
                lps.leftMargin = 0;
            }
            if (layoutParams instanceof AbsListView.LayoutParams && getParent() instanceof ListView) {
                ListView lv = (ListView) getParent();
                lv.setPadding(0, lv.getPaddingTop(), lv.getPaddingRight(), lv.getPaddingBottom());
            }
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
    */
}
