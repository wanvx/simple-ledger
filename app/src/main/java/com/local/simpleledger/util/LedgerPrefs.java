package com.local.simpleledger.util;

import android.content.Context;
import android.content.SharedPreferences;

import com.local.simpleledger.model.RemainingMode;

/** All lightweight local preferences: theme, balance mode, budgets and warning lines. */
public final class LedgerPrefs {

    private static final String FILE = "simple_ledger_settings";

    public static final long DEFAULT_DAY_ORANGE = 5000L;
    public static final long DEFAULT_DAY_RED = 7500L;
    public static final long DEFAULT_WEEK_ORANGE = 38000L;
    public static final long DEFAULT_WEEK_RED = 50000L;
    public static final long DEFAULT_MONTH_ORANGE = 170000L;
    public static final long DEFAULT_MONTH_RED = 190000L;

    public static final long DEFAULT_WEEK_BUDGET = 50000L;
    public static final long DEFAULT_MONTH_BUDGET = 190000L;

    private LedgerPrefs() {
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    public static boolean isNight(Context context) {
        return prefs(context).getBoolean("night_mode", false);
    }

    public static void setNight(Context context, boolean night) {
        prefs(context).edit().putBoolean("night_mode", night).apply();
    }

    public static RemainingMode remainingMode(Context context) {
        return RemainingMode.fromKey(prefs(context).getString("remaining_mode", "month"));
    }

    public static void setRemainingMode(Context context, RemainingMode mode) {
        prefs(context).edit().putString("remaining_mode", mode.key).apply();
    }

    /** Budget adjustment, not the absolute budget, so "reset" can restore the default. */
    public static long budgetAdjustment(Context context, boolean week) {
        return prefs(context).getLong(week ? "week_budget_adj" : "month_budget_adj", 0L);
    }

    public static long budget(Context context, boolean week) {
        long base = week ? DEFAULT_WEEK_BUDGET : DEFAULT_MONTH_BUDGET;
        return Math.max(0L, base + budgetAdjustment(context, week));
    }

    /**
     * Changes the available budget by delta cents. Remaining money moves by exactly the same
     * amount, while spending keeps reducing it independently.
     */
    public static void adjustBudget(Context context, boolean week, long deltaCents) {
        long base = week ? DEFAULT_WEEK_BUDGET : DEFAULT_MONTH_BUDGET;
        long adjustment = budgetAdjustment(context, week);
        long nextBudget = Math.max(0L, base + adjustment + deltaCents);
        long nextAdjustment = nextBudget - base;
        prefs(context).edit()
                .putLong(week ? "week_budget_adj" : "month_budget_adj", nextAdjustment)
                .apply();
    }

    public static void resetBudget(Context context, boolean week) {
        prefs(context).edit()
                .putLong(week ? "week_budget_adj" : "month_budget_adj", 0L)
                .apply();
    }

    public static long remaining(Context context, boolean week, long spentCents) {
        return budget(context, week) - spentCents;
    }

    public static WarningSettings warnings(Context context) {
        SharedPreferences p = prefs(context);
        return new WarningSettings(
                p.getLong("day_orange", DEFAULT_DAY_ORANGE),
                p.getLong("day_red", DEFAULT_DAY_RED),
                p.getLong("week_orange", DEFAULT_WEEK_ORANGE),
                p.getLong("week_red", DEFAULT_WEEK_RED),
                p.getLong("month_orange", DEFAULT_MONTH_ORANGE),
                p.getLong("month_red", DEFAULT_MONTH_RED));
    }

    public static void saveWarnings(Context context, WarningSettings settings) {
        WarningSettings safe = settings.sanitized();
        prefs(context).edit()
                .putLong("day_orange", safe.dayOrange)
                .putLong("day_red", safe.dayRed)
                .putLong("week_orange", safe.weekOrange)
                .putLong("week_red", safe.weekRed)
                .putLong("month_orange", safe.monthOrange)
                .putLong("month_red", safe.monthRed)
                .apply();
    }

    /** Restores warning lines, budgets and remaining mode. Theme is deliberately preserved. */
    public static void resetFinancialSettings(Context context) {
        SharedPreferences.Editor editor = prefs(context).edit();
        editor.remove("day_orange").remove("day_red");
        editor.remove("week_orange").remove("week_red");
        editor.remove("month_orange").remove("month_red");
        editor.putLong("week_budget_adj", 0L);
        editor.putLong("month_budget_adj", 0L);
        editor.putString("remaining_mode", RemainingMode.MONTH.key);
        editor.apply();
    }

    public static final class WarningSettings {
        public final long dayOrange;
        public final long dayRed;
        public final long weekOrange;
        public final long weekRed;
        public final long monthOrange;
        public final long monthRed;

        public WarningSettings(long dayOrange, long dayRed,
                               long weekOrange, long weekRed,
                               long monthOrange, long monthRed) {
            this.dayOrange = dayOrange;
            this.dayRed = dayRed;
            this.weekOrange = weekOrange;
            this.weekRed = weekRed;
            this.monthOrange = monthOrange;
            this.monthRed = monthRed;
        }

        public static WarningSettings defaults() {
            return new WarningSettings(DEFAULT_DAY_ORANGE, DEFAULT_DAY_RED,
                    DEFAULT_WEEK_ORANGE, DEFAULT_WEEK_RED,
                    DEFAULT_MONTH_ORANGE, DEFAULT_MONTH_RED);
        }

        public WarningSettings sanitized() {
            long dayO = Math.max(0, dayOrange);
            long weekO = Math.max(0, weekOrange);
            long monthO = Math.max(0, monthOrange);
            return new WarningSettings(dayO, Math.max(dayO + 1, dayRed),
                    weekO, Math.max(weekO + 1, weekRed),
                    monthO, Math.max(monthO + 1, monthRed));
        }
    }
}
