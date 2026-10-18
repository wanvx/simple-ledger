package com.local.simpleledger.model;

/** Which remaining balance is shown on the records page. */
public enum RemainingMode {
    MONTH("month", "本月"),
    WEEK("week", "本周");

    public final String key;
    public final String label;

    RemainingMode(String key, String label) {
        this.key = key;
        this.label = label;
    }

    public static RemainingMode fromKey(String key) {
        return WEEK.key.equals(key) ? WEEK : MONTH;
    }

    public RemainingMode toggle() {
        return this == MONTH ? WEEK : MONTH;
    }
}
