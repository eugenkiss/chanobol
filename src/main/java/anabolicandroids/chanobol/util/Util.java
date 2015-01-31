package anabolicandroids.chanobol.util;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.Build;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v8.renderscript.Allocation;
import android.support.v8.renderscript.Element;
import android.support.v8.renderscript.RenderScript;
import android.support.v8.renderscript.ScriptIntrinsicBlur;
import android.util.DisplayMetrics;
import android.util.TypedValue;
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

    public static void updateRecyclerViewGridOnConfigChange(final RecyclerView rv, final int cardWidthId) {
        rv.postDelayed(new Runnable() {
            @Override public void run() {
                GridLayoutManager glm = (GridLayoutManager) rv.getLayoutManager();
                Util.calcDynamicSpanCountById(rv.getContext(), rv, glm, cardWidthId);
                rv.requestLayout();
            }
        }, 100);
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

    // http://stackoverflow.com/a/21051758/283607
    public static Bitmap blur(Context ctx, Bitmap image, float radius) {
        int width = Math.round(image.getWidth());
        int height = Math.round(image.getHeight());

        Bitmap inputBitmap = Bitmap.createScaledBitmap(image, width, height, false);
        Bitmap outputBitmap = Bitmap.createBitmap(inputBitmap);

        RenderScript rs = RenderScript.create(ctx);
        ScriptIntrinsicBlur theIntrinsic = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));
        Allocation tmpIn = Allocation.createFromBitmap(rs, inputBitmap);
        Allocation tmpOut = Allocation.createFromBitmap(rs, outputBitmap);
        theIntrinsic.setRadius(radius);
        theIntrinsic.setInput(tmpIn);
        theIntrinsic.forEach(tmpOut);
        tmpOut.copyTo(outputBitmap);

        return outputBitmap;
    }

    // http://stackoverflow.com/a/24313590/283607
    public static Bitmap setHasAlphaCompat(Bitmap bit) {
        int width =  bit.getWidth();
        int height = bit.getHeight();
        Bitmap myBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        int [] allpixels = new int [ myBitmap.getHeight()*myBitmap.getWidth()];
        bit.getPixels(allpixels, 0, myBitmap.getWidth(), 0, 0, myBitmap.getWidth(),myBitmap.getHeight());
        myBitmap.setPixels(allpixels, 0, width, 0, 0, width, height);
        return myBitmap;
    }
}
