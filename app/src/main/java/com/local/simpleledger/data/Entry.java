package com.local.simpleledger.data;

import com.local.simpleledger.model.Category;

/** A saved expense. Amounts are always integer cents to avoid floating point drift. */
public final class Entry {

    public static final String[] COLUMNS = {
            "_id", "date", "category", "subcategory", "custom_label",
            "item", "amount_cents", "created_at"
    };

    public final long id;
    public final String date;
    public final String categoryKey;
    public final String subcategory;
    public final String customLabel;
    public final String item;
    public final long amountCents;
    public final long createdAt;

    public Entry(long id, String date, String categoryKey, String subcategory,
                 String customLabel, String item, long amountCents, long createdAt) {
        this.id = id;
        this.date = date;
        this.categoryKey = categoryKey;
        this.subcategory = subcategory;
        this.customLabel = customLabel == null ? "" : customLabel;
        this.item = item;
        this.amountCents = amountCents;
        this.createdAt = createdAt;
    }

    public Category category() {
        return Category.byKey(categoryKey);
    }

    /** The user-facing subcategory, using the name given to "其他" when present. */
    public String displaySubcategory() {
        return customLabel.isEmpty() ? subcategory : customLabel;
    }

    public String displayLine() {
        return category().title + "-" + displaySubcategory() + "-" + item;
    }
}
