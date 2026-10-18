package com.local.simpleledger.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.local.simpleledger.R;

/** Shared design-system helpers: spacing, cards, text, icons and system bars. */
public final class Ui {

    private Ui() {
    }

    public static int dp(Context context, float value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    public static int color(Context context, @ColorRes int res) {
        return ContextCompat.getColor(context, res);
    }

    public static Drawable drawable(Context context, @DrawableRes int res) {
        return AppCompatResources.getDrawable(context, res);
    }

    public static int alpha(int color, float alpha) {
        return Color.argb(Math.round(Math.max(0f, Math.min(1f, alpha)) * 255f),
                Color.red(color), Color.green(color), Color.blue(color));
    }

    public static int blend(int from, int to, float fraction) {
        fraction = Math.max(0f, Math.min(1f, fraction));
        return Color.rgb(
                Math.round(Color.red(from) + (Color.red(to) - Color.red(from)) * fraction),
                Math.round(Color.green(from) + (Color.green(to) - Color.green(from)) * fraction),
                Math.round(Color.blue(from) + (Color.blue(to) - Color.blue(from)) * fraction));
    }

    public static TextView text(Context context, CharSequence value, float sp, int color,
                                boolean medium) {
        TextView view = new TextView(context);
        view.setText(value);
        view.setTextSize(sp);
        view.setTextColor(color);
        view.setIncludeFontPadding(false);
        view.setTypeface(Typeface.create(medium ? "sans-serif-medium" : "sans-serif",
                Typeface.NORMAL));
        view.setLineSpacing(dp(context, 2), 1f);
        return view;
    }

    public static TextView title(Context context, CharSequence value, float sp) {
        TextView view = text(context, value, sp, color(context, R.color.text_primary), true);
        view.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        view.setLetterSpacing(0f);
        return view;
    }

    public static TextView secondary(Context context, CharSequence value, float sp) {
        return text(context, value, sp, color(context, R.color.text_secondary), false);
    }

    public static TextView label(Context context, CharSequence value, float sp) {
        TextView view = text(context, value, sp, color(context, R.color.text_tertiary), true);
        view.setLetterSpacing(0.02f);
        return view;
    }

    public static ImageView icon(Context context, @DrawableRes int res, int sizeDp, int tint) {
        ImageView view = new AppCompatImageView(context);
        view.setImageResource(res);
        view.setScaleType(ImageView.ScaleType.FIT_CENTER);
        view.setColorFilter(tint);
        view.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        view.setLayoutParams(new LinearLayout.LayoutParams(dp(context, sizeDp), dp(context, sizeDp)));
        return view;
    }

    public static ImageView iconButton(Context context, @DrawableRes int res, int tint,
                                       int sizeDp, View.OnClickListener click) {
        ImageView view = icon(context, res, sizeDp, tint);
        int buttonSize = dp(context, 44);
        int pad = Math.max(0, (buttonSize - dp(context, sizeDp)) / 2);
        view.setPadding(pad, pad, pad, pad);
        view.setLayoutParams(new LinearLayout.LayoutParams(buttonSize, buttonSize));
        TypedValue background = new TypedValue();
        context.getTheme().resolveAttribute(android.R.attr.selectableItemBackgroundBorderless,
                background, true);
        view.setBackgroundResource(background.resourceId);
        view.setClickable(true);
        view.setFocusable(true);
        view.setContentDescription("");
        view.setOnClickListener(click);
        return view;
    }

    public static GradientDrawable roundRect(Context context, int fill, float radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fill);
        drawable.setCornerRadius(dp(context, radiusDp));
        return drawable;
    }

    public static GradientDrawable roundRect(Context context, int fill, float radiusDp,
                                             int strokeWidthDp, int strokeColor) {
        GradientDrawable drawable = roundRect(context, fill, radiusDp);
        drawable.setStroke(dp(context, strokeWidthDp), strokeColor);
        return drawable;
    }

    public static GradientDrawable circle(int fill) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);
        drawable.setColor(fill);
        return drawable;
    }

    public static MaterialCardView card(Context context) {
        return card(context, 22, true, true);
    }

    public static MaterialCardView card(Context context, int radiusDp, boolean bordered,
                                        boolean clickable) {
        MaterialCardView card = new MaterialCardView(context);
        card.setCardBackgroundColor(color(context, R.color.surface));
        card.setRadius(dp(context, radiusDp));
        card.setCardElevation(0f);
        card.setStrokeWidth(dp(context, bordered ? 1 : 0));
        card.setStrokeColor(color(context, R.color.divider));
        card.setUseCompatPadding(false);
        card.setPreventCornerOverlap(true);
        card.setRippleColor(ColorStateList.valueOf(
                alpha(color(context, R.color.text_primary), 0.055f)));
        card.setClickable(clickable);
        card.setFocusable(clickable);
        return card;
    }

    public static MaterialCardView plainCard(Context context) {
        MaterialCardView card = new MaterialCardView(context);
        card.setCardBackgroundColor(color(context, R.color.surface));
        card.setRadius(dp(context, 22));
        card.setCardElevation(0f);
        card.setStrokeWidth(0);
        card.setUseCompatPadding(false);
        return card;
    }

    public static LinearLayout vertical(Context context) {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        return layout;
    }

    public static LinearLayout horizontal(Context context) {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        return layout;
    }

    public static LinearLayout.LayoutParams lp(int width, int height) {
        return new LinearLayout.LayoutParams(width, height);
    }

    public static LinearLayout.LayoutParams lpMatchWrap() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    public static LinearLayout.LayoutParams lpWeight(float weight) {
        return new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, weight);
    }

    public static LinearLayout.LayoutParams lpMatch(int height) {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height);
    }

    public static void margins(View view, int left, int top, int right, int bottom) {
        ViewGroup.LayoutParams raw = view.getLayoutParams();
        if (raw instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) raw;
            params.setMargins(left, top, right, bottom);
            view.setLayoutParams(params);
        }
    }

    public static View divider(Context context) {
        View view = new View(context);
        view.setBackgroundColor(color(context, R.color.divider));
        view.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, dp(context, 1))));
        return view;
    }

    public static View spacer(Context context, int heightDp) {
        View view = new View(context);
        view.setLayoutParams(lpMatch(dp(context, heightDp)));
        return view;
    }

    public static LinearLayout iconCircle(Context context, @DrawableRes int iconRes, int color,
                                          int sizeDp, int iconSizeDp) {
        LinearLayout holder = new LinearLayout(context);
        holder.setGravity(Gravity.CENTER);
        boolean night = (context.getResources().getConfiguration().uiMode
                & android.content.res.Configuration.UI_MODE_NIGHT_MASK)
                == android.content.res.Configuration.UI_MODE_NIGHT_YES;
        holder.setBackground(circle(alpha(color, night ? 0.20f : 0.12f)));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                dp(context, sizeDp), dp(context, sizeDp));
        holder.setLayoutParams(params);
        holder.addView(icon(context, iconRes, iconSizeDp, color));
        return holder;
    }

    public static TextView chip(Context context, String label, int color) {
        TextView chip = text(context, label, 12, color, true);
        chip.setGravity(Gravity.CENTER);
        chip.setPadding(dp(context, 10), dp(context, 5), dp(context, 10), dp(context, 5));
        boolean night = (context.getResources().getConfiguration().uiMode
                & android.content.res.Configuration.UI_MODE_NIGHT_MASK)
                == android.content.res.Configuration.UI_MODE_NIGHT_YES;
        chip.setBackground(roundRect(context, alpha(color, night ? 0.19f : 0.11f), 20));
        return chip;
    }

    public static TextView softPill(Context context, String label, int color) {
        TextView pill = text(context, label, 11, color, true);
        pill.setGravity(Gravity.CENTER);
        pill.setPadding(dp(context, 9), dp(context, 4), dp(context, 9), dp(context, 4));
        pill.setBackground(roundRect(context, alpha(color, 0.12f), 10));
        return pill;
    }

    public static MaterialButton button(Context context, String label, int background,
                                        int foreground) {
        MaterialButton button = new MaterialButton(context);
        button.setText(label);
        button.setAllCaps(false);
        button.setTextSize(16);
        button.setTextColor(foreground);
        button.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        button.setLetterSpacing(0f);
        button.setBackgroundTintList(ColorStateList.valueOf(background));
        button.setCornerRadius(dp(context, 16));
        button.setMinHeight(dp(context, 52));
        button.setInsetTop(0);
        button.setInsetBottom(0);
        button.setPadding(dp(context, 18), 0, dp(context, 18), 0);
        button.setRippleColor(ColorStateList.valueOf(alpha(foreground, 0.16f)));
        button.setElevation(0f);
        button.setStateListAnimator(null);
        return button;
    }

    public static MaterialButton primaryButton(Context context, String label) {
        return button(context, label, color(context, R.color.accent),
                color(context, R.color.accent_contrast));
    }

    public static MaterialButton outlineButton(Context context, String label, int foreground,
                                               int stroke) {
        MaterialButton button = new MaterialButton(context);
        button.setText(label);
        button.setAllCaps(false);
        button.setTextSize(15);
        button.setTextColor(foreground);
        button.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        button.setLetterSpacing(0f);
        button.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));
        button.setStrokeColor(ColorStateList.valueOf(stroke));
        button.setStrokeWidth(dp(context, 1));
        button.setCornerRadius(dp(context, 15));
        button.setMinHeight(dp(context, 48));
        button.setInsetTop(0);
        button.setInsetBottom(0);
        button.setPadding(dp(context, 14), 0, dp(context, 14), 0);
        button.setRippleColor(ColorStateList.valueOf(alpha(foreground, 0.08f)));
        button.setElevation(0f);
        button.setStateListAnimator(null);
        return button;
    }

    public static MaterialButton textButton(Context context, String label, int foreground) {
        MaterialButton button = new MaterialButton(context);
        button.setText(label);
        button.setAllCaps(false);
        button.setTextSize(14);
        button.setTextColor(foreground);
        button.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        button.setLetterSpacing(0f);
        button.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));
        button.setRippleColor(ColorStateList.valueOf(alpha(foreground, 0.10f)));
        button.setMinHeight(dp(context, 40));
        button.setMinimumHeight(dp(context, 40));
        button.setInsetTop(0);
        button.setInsetBottom(0);
        button.setPadding(dp(context, 10), 0, dp(context, 10), 0);
        button.setElevation(0f);
        button.setStateListAnimator(null);
        return button;
    }

    public static EditText input(Context context, String hint) {
        EditText input = new EditText(context);
        input.setHint(hint);
        input.setTextSize(16);
        input.setTextColor(color(context, R.color.text_primary));
        input.setHintTextColor(color(context, R.color.text_tertiary));
        input.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        input.setBackground(roundRect(context, color(context, R.color.surface_alt), 14,
                1, color(context, R.color.divider)));
        input.setPadding(dp(context, 15), dp(context, 13),
                dp(context, 15), dp(context, 13));
        input.setSingleLine(true);
        input.setSelectAllOnFocus(false);
        return input;
    }

    public static TextView sectionTitle(Context context, String title) {
        TextView view = text(context, title, 15,
                color(context, R.color.text_primary), true);
        view.setPadding(dp(context, 3), 0, 0, 0);
        return view;
    }

    public static void pressScale(View view) {
        view.setOnTouchListener((v, event) -> {
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                v.animate().scaleX(0.978f).scaleY(0.978f).setDuration(90).start();
            } else if (event.getActionMasked() == MotionEvent.ACTION_UP
                    || event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
                v.animate().scaleX(1f).scaleY(1f).setDuration(160).start();
            }
            return false;
        });
    }

    @SuppressLint("InlinedApi")
    public static void applySystemBars(Activity activity) {
        Window window = activity.getWindow();
        boolean night = (activity.getResources().getConfiguration().uiMode
                & android.content.res.Configuration.UI_MODE_NIGHT_MASK)
                == android.content.res.Configuration.UI_MODE_NIGHT_YES;
        int bg = color(activity, R.color.bg);
        window.setStatusBarColor(bg);
        window.setNavigationBarColor(bg);
        int flags = window.getDecorView().getSystemUiVisibility();
        if (!night) {
            flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            }
        } else {
            flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            flags &= ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        }
        window.getDecorView().setSystemUiVisibility(flags);
    }

    public static void toast(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    public static void hideKeyboard(View view) {
        InputMethodManager manager = (InputMethodManager) view.getContext()
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        if (manager != null) {
            manager.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    public static void showKeyboard(View view) {
        view.postDelayed(() -> {
            InputMethodManager manager = (InputMethodManager) view.getContext()
                    .getSystemService(Context.INPUT_METHOD_SERVICE);
            if (manager != null) {
                manager.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
            }
        }, 160);
    }

    public static LinearLayout toolbar(Context context, String title, Runnable onBack) {
        LinearLayout bar = horizontal(context);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(context, 12), dp(context, 8), dp(context, 16), dp(context, 8));
        bar.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(context, 58)));
        ImageView back = iconButton(context, R.drawable.ic_arrow_back,
                color(context, R.color.text_primary), 24, v -> onBack.run());
        back.setContentDescription("返回");
        back.setLayoutParams(new LinearLayout.LayoutParams(dp(context, 44), dp(context, 44)));
        bar.addView(back);
        TextView titleView = text(context, title, 19,
                color(context, R.color.text_primary), true);
        titleView.setMaxLines(1);
        titleView.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        titleParams.leftMargin = dp(context, 8);
        titleView.setLayoutParams(titleParams);
        bar.addView(titleView);
        return bar;
    }

    public static String ellipsize(String value, int max) {
        if (value == null) {
            return "";
        }
        return value.length() <= max ? value : value.substring(0, max) + "…";
    }
}
