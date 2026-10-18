package com.local.simpleledger.model;

import android.content.Context;

import androidx.annotation.ColorRes;

import com.local.simpleledger.R;

/** Three-stage spending warning that mirrors the green / orange / red UI. */
public enum WarningLevel {
    GREEN("正常", "保持得很好", R.color.warning_green),
    ORANGE("注意", "接近预警线", R.color.warning_orange),
    RED("超支", "已超过预警线", R.color.warning_red);

    public final String title;
    public final String hint;
    @ColorRes
    public final int colorRes;

    WarningLevel(String title, String hint, @ColorRes int colorRes) {
        this.title = title;
        this.hint = hint;
        this.colorRes = colorRes;
    }

    public int color(Context context) {
        return context.getColor(colorRes);
    }

    /**
     * Green is below the orange line, orange covers the range up to and including the red
     * line, and red starts just above the red line.
     */
    public static WarningLevel of(long spentCents, long orangeCents, long redCents) {
        long orange = Math.max(1, orangeCents);
        long red = Math.max(orange, redCents);
        if (spentCents <= 0) {
            return GREEN;
        }
        if (spentCents > red) {
            return RED;
        }
        if (spentCents >= orange) {
            return ORANGE;
        }
        return GREEN;
    }
}
