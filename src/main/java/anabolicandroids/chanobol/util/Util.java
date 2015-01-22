package anabolicandroids.chanobol.util;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.ActionProvider;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.AlphaAnimation;
import android.widget.Toast;

import com.nineoldandroids.animation.ValueAnimator;
import com.nineoldandroids.view.ViewHelper;

import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import anabolicandroids.chanobol.R;

public class Util {

    public static int clamp(double min, double val, double max) {
        return (int) Math.max(min, Math.min(max, val));
    }

    public static List<Object> extendedList(List<Object> base, Object... others) {
        ArrayList<Object> list = new ArrayList<>();
        Collections.addAll(list, others);
        list.addAll(base);
        return list;
    }

    // http://stackoverflow.com/a/5599842
    public static String readableFileSize(long size) {
        if(size <= 0) return "0";
        final String[] units = new String[] { "B", "kB", "MB", "GB", "TB" };
        int digitGroups = (int) (Math.log10(size)/Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(size/Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }

    public static void showToast(Context context, String msg) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
    }

    public static void showToast(Context context, int res) {
        Toast.makeText(context, res, Toast.LENGTH_SHORT).show();
    }

    @SuppressWarnings("UnusedDeclaration")
    public static void setAlpha(View view, float alpha) {
        if (Build.VERSION.SDK_INT < 11) {
            final AlphaAnimation animation = new AlphaAnimation(alpha, alpha);
            animation.setDuration(0);
            animation.setFillAfter(true);
            view.startAnimation(animation);
        } else view.setAlpha(alpha);
    }

    public static String loadJSONFromAsset(Context context, String path) {
        String json;
        try {
            InputStream is = context.getAssets().open(path);
            int size = is.available();
            byte[] buffer = new byte[size];
            //noinspection ResultOfMethodCallIgnored
            is.read(buffer);
            is.close();
            json = new String(buffer, "UTF-8");
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return json;
    }

    public static int getScreenHeight(Context context) {
        android.view.Display display = ((android.view.WindowManager)context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        //noinspection deprecation
        return display.getHeight();
    }

    public static int getScreenWidth(Context context) {
        android.view.Display display = ((android.view.WindowManager)context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        //noinspection deprecation
        return display.getWidth();
    }

    public static int getActionBarHeight(Context context) {
        TypedValue tv = new TypedValue();
        if (context.getTheme().resolveAttribute(R.attr.actionBarSize, tv, true)) {
            return TypedValue.complexToDimensionPixelSize(tv.data, context.getResources().getDisplayMetrics());
        }
        return -1;
    }

    public static float dpToPx(float dp, Context context){
        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        return dp * (metrics.densityDpi / 160f);
    }

    @SuppressWarnings("UnusedDeclaration")
    public static float pxToDp(float px, Context context){
        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        return px / (metrics.densityDpi / 160f);
    }

    // Unsatisfactory compared to GridView but given the current constraint best solution
    // http://stackoverflow.com/questions/26666143/recyclerview-gridlayoutmanager-how-to-auto-detect-span-count
    public static void calcDynamicSpanCount(final RecyclerView rv,
                                            final GridLayoutManager glm,
                                            final float cardWidth) {
        rv.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        //noinspection deprecation
                        rv.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                        int viewWidth = rv.getMeasuredWidth();
                        //float cardViewWidth = cxt.getResources().getDimension(R.dimen.column_width);
                        int newSpanCount = (int) Math.floor(viewWidth / cardWidth);
                        glm.setSpanCount(newSpanCount);
                        glm.requestLayout();
                    }
                });
    }
    public static void calcDynamicSpanCountById(final Context cxt,
                                                final RecyclerView rv,
                                                final GridLayoutManager glm,
                                                final int cardWidthId) {
        float cardWidth = cxt.getResources().getDimension(cardWidthId);
        calcDynamicSpanCount(rv, glm, cardWidth);
    }

    public static void animateY(final View view, int from, int to, int duration) {
        ValueAnimator anim = ValueAnimator.ofInt(from, to);
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                int val = (Integer) valueAnimator.getAnimatedValue();
                ViewHelper.setTranslationY(view, val);
            }
        });
        anim.setDuration(duration);
        anim.start();
    }

    // Fuck this
    public static MenuItem mockHomeButton = new MenuItem() {
        @TargetApi(Build.VERSION_CODES.HONEYCOMB) @Override
        public int getItemId() {
            return android.R.id.home;
        }

        @Override
        public int getGroupId() {
            return 0;
        }

        @Override
        public int getOrder() {
            return 0;
        }

        @Override
        public MenuItem setTitle(CharSequence title) {
            return null;
        }

        @Override
        public MenuItem setTitle(int title) {
            return null;
        }

        @Override
        public CharSequence getTitle() {
            return null;
        }

        @Override
        public MenuItem setTitleCondensed(CharSequence title) {
            return null;
        }

        @Override
        public CharSequence getTitleCondensed() {
            return null;
        }

        @Override
        public MenuItem setIcon(Drawable icon) {
            return null;
        }

        @Override
        public MenuItem setIcon(int iconRes) {
            return null;
        }

        @Override
        public Drawable getIcon() {
            return null;
        }

        @Override
        public MenuItem setIntent(Intent intent) {
            return null;
        }

        @Override
        public Intent getIntent() {
            return null;
        }

        @Override
        public MenuItem setShortcut(char numericChar, char alphaChar) {
            return null;
        }

        @Override
        public MenuItem setNumericShortcut(char numericChar) {
            return null;
        }

        @Override
        public char getNumericShortcut() {
            return 0;
        }

        @Override
        public MenuItem setAlphabeticShortcut(char alphaChar) {
            return null;
        }

        @Override
        public char getAlphabeticShortcut() {
            return 0;
        }

        @Override
        public MenuItem setCheckable(boolean checkable) {
            return null;
        }

        @Override
        public boolean isCheckable() {
            return false;
        }

        @Override
        public MenuItem setChecked(boolean checked) {
            return null;
        }

        @Override
        public boolean isChecked() {
            return false;
        }

        @Override
        public MenuItem setVisible(boolean visible) {
            return null;
        }

        @Override
        public boolean isVisible() {
            return false;
        }

        @Override
        public MenuItem setEnabled(boolean enabled) {
            return null;
        }

        @Override
        public boolean isEnabled() {
            return false;
        }

        @Override
        public boolean hasSubMenu() {
            return false;
        }

        @Override
        public SubMenu getSubMenu() {
            return null;
        }

        @Override
        public MenuItem setOnMenuItemClickListener(OnMenuItemClickListener menuItemClickListener) {
            return null;
        }

        @Override
        public ContextMenu.ContextMenuInfo getMenuInfo() {
            return null;
        }

        @Override
        public void setShowAsAction(int actionEnum) {

        }

        @Override
        public MenuItem setShowAsActionFlags(int actionEnum) {
            return null;
        }

        @Override
        public MenuItem setActionView(View view) {
            return null;
        }

        @Override
        public MenuItem setActionView(int resId) {
            return null;
        }

        @Override
        public View getActionView() {
            return null;
        }

        @Override
        public MenuItem setActionProvider(ActionProvider actionProvider) {
            return null;
        }

        @Override
        public ActionProvider getActionProvider() {
            return null;
        }

        @Override
        public boolean expandActionView() {
            return false;
        }

        @Override
        public boolean collapseActionView() {
            return false;
        }

        @Override
        public boolean isActionViewExpanded() {
            return false;
        }

        @Override
        public MenuItem setOnActionExpandListener(OnActionExpandListener listener) {
            return null;
        }
    };
}
