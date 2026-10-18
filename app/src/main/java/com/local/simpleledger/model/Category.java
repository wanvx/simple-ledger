package com.local.simpleledger.model;

import android.content.Context;

import androidx.core.content.ContextCompat;

import com.local.simpleledger.R;

import java.util.LinkedHashMap;
import java.util.Map;

/** The six fixed spending groups and their fixed subcategories. */
public enum Category {
    FOOD("food", "饮食", R.color.cat_food, R.drawable.ic_cat_food,
            new String[]{"早餐", "午餐", "晚餐", "夜宵", "饮料", "咖啡", "嘴馋"}),
    TRANSPORT("transport", "出行", R.color.cat_transport, R.drawable.ic_cat_transport,
            new String[]{"电动车充电", "住宿", "地铁", "打车", "其他"}),
    DAILY("daily", "日常", R.color.cat_daily, R.drawable.ic_cat_daily,
            new String[]{"洗澡", "喝水", "消耗品", "电费", "看病买药", "其他"}),
    FUN("fun", "娱乐", R.color.cat_fun, R.drawable.ic_cat_fun,
            new String[]{"外出娱乐", "社交吃饭", "游戏", "其他"}),
    STUDY("study", "学习", R.color.cat_study, R.drawable.ic_cat_study,
            new String[]{"书籍与文具", "AItoken", "其他"}),
    OTHER("other", "其他", R.color.cat_other, R.drawable.ic_cat_other,
            new String[]{"网购", "无分类"});

    private static final Map<String, Category> BY_KEY = new LinkedHashMap<>();

    static {
        for (Category category : values()) {
            BY_KEY.put(category.key, category);
        }
    }

    public final String key;
    public final String title;
    public final int colorRes;
    public final int iconRes;
    private final String[] subcategories;

    Category(String key, String title, int colorRes, int iconRes, String[] subcategories) {
        this.key = key;
        this.title = title;
        this.colorRes = colorRes;
        this.iconRes = iconRes;
        this.subcategories = subcategories;
    }

    public String[] subcategories() {
        return subcategories.clone();
    }

    public boolean hasSubcategory(String value) {
        for (String subcategory : subcategories) {
            if (subcategory.equals(value)) {
                return true;
            }
        }
        return false;
    }

    public int color(Context context) {
        return ContextCompat.getColor(context, colorRes);
    }

    public String subcategoryPreview() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < subcategories.length; i++) {
            if (i > 0) {
                builder.append(" · ");
            }
            builder.append(subcategories[i]);
        }
        return builder.toString();
    }

    public static Category byKey(String key) {
        Category category = BY_KEY.get(key);
        return category == null ? OTHER : category;
    }
}
