package com.local.simpleledger.model;

/** One aggregated row of the SQL summary, at subcategory granularity. */
public final class SubcategoryTotal {
    public final Category category;
    public final String subcategory;
    public final long amountCents;

    public SubcategoryTotal(Category category, String subcategory, long amountCents) {
        this.category = category;
        this.subcategory = subcategory;
        this.amountCents = amountCents;
    }
}
