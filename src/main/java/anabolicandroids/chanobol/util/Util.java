package anabolicandroids.chanobol.util;

import android.app.Activity;
import android.app.Application;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
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
import android.view.Window;
import android.view.animation.AlphaAnimation;
import android.widget.Toast;

import com.nineoldandroids.animation.ValueAnimator;
import com.nineoldandroids.view.ViewHelper;

import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;

import anabolicandroids.chanobol.App;
import anabolicandroids.chanobol.R;
import anabolicandroids.chanobol.ui.boards.FavoritesActivity;
import anabolicandroids.chanobol.ui.scaffolding.Prefs;
import anabolicandroids.chanobol.ui.scaffolding.Theme;
import anabolicandroids.chanobol.ui.scaffolding.ThemeContext;

import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

public class Util {

    public static int clamp(double min, double val, double max) {
        return (int) Math.max(min, Math.min(max, val));
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
                        glm.setSpanCount(Math.max(1, newSpanCount));
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

    public static void setVisibility(View view, boolean show) {
        if (show) view.setVisibility(View.VISIBLE);
        else view.setVisibility(View.GONE);
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

    public static void startWebmActivity(Context context, String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW).setDataAndType(Uri.parse(url), "video/webm");
        if(intent.resolveActivity(context.getPackageManager()) != null)
            context.startActivity(intent);
        else
            Util.showToast(context, R.string.no_app);
    }

    public static void openLink(Context context, String link) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
        if(intent.resolveActivity(context.getPackageManager()) != null)
            context.startActivity(intent);
        else
            Util.showToast(context, R.string.no_app);
    }

    public static void restartApp(Application app, Activity activity) {
        // TODO: Apparently there is a way to recreate the whole back stack: http://stackoverflow.com/a/28799124/283607
        // From U2020 DebugAppContainer.setEndpointAndRelaunch
        Intent newApp = new Intent(activity, FavoritesActivity.class);
        if (Build.VERSION.SDK_INT >= 11) {
            newApp.setFlags(FLAG_ACTIVITY_CLEAR_TASK | FLAG_ACTIVITY_NEW_TASK);
        } else {
            newApp.setFlags(FLAG_ACTIVITY_NEW_TASK);
        }
        app.startActivity(newApp);
        App.get(app).buildAppGraphAndInject();
    }

    public static void setTheme(Activity activity, Prefs prefs) {
        Theme theme = prefs.theme();
        activity.setTheme(theme.resValue);
        ThemeContext.getInstance().reloadPostViewColors(activity);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // It isn't loaded from the xml in setTheme for some reason
            Window window = activity.getWindow();
            Resources r = activity.getResources();
            if (theme == Theme.LIGHT)
                window.setStatusBarColor(r.getColor(R.color.colorPrimaryDark_light));
            if (theme == Theme.DARK)
                window.setStatusBarColor(r.getColor(R.color.colorPrimaryDark));
            if (theme == Theme.TEAL)
                window.setStatusBarColor(r.getColor(R.color.colorPrimaryDark_teal));
            if (theme == Theme.BLUE1)
                window.setStatusBarColor(r.getColor(R.color.colorPrimaryDark_blue1));
            if (theme == Theme.BLUE2)
                window.setStatusBarColor(r.getColor(R.color.colorPrimaryDark_blue2));
            if (theme == Theme.GRAY)
                window.setStatusBarColor(r.getColor(R.color.colorPrimaryDark_gray));
        }
    }

    // http://stackoverflow.com/a/10600736/283607
    public static Bitmap drawableToBitmap(Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable)drawable).getBitmap();
        }

        int w = drawable.getIntrinsicWidth();
        int h = drawable.getIntrinsicHeight();
        if (w <= 0 || h <= 0) return null;

        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }

    // http://stackoverflow.com/a/23683075/283607
    public static Bitmap copy(Bitmap b) {
        return b.copy(b.getConfig(), true);
    }

    // From Clover
    public static void runOnUiThread(Runnable runnable) {
        new Handler(Looper.getMainLooper()).post(runnable);
    }

    // From Clover and http://stackoverflow.com/a/10965541/283607
    public static void copyToClipboard(Context context, String text) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText(context.getResources().getString(R.string.clipoard_copy_label), text);
            clipboard.setPrimaryClip(clip);
        } else {
            android.text.ClipboardManager clipboard = (android.text.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            clipboard.setText(text);
        }
    }
}
