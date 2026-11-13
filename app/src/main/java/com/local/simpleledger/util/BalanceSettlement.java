package com.local.simpleledger.util;

import android.content.Context;
import android.content.pm.PackageManager;

import com.local.simpleledger.data.LedgerDb;

import java.util.Calendar;

/**
 * Turns each completed week's leftover (weekly budget - spending) into one savings record.
 * The first week starts on the Monday after the 1.3 package update, so installing the feature
 * never back-fills old weeks. Settlement is idempotent thanks to the unique week index.
 */
public final class BalanceSettlement {

    private BalanceSettlement() {
    }

    public static void settleCompletedWeeks(Context context, LedgerDb db) {
        String startKey = ensureStartWeek(context);
        Calendar cursor = DateUtils.parse(startKey);
        Calendar currentMonday = DateUtils.startOfWeek(DateUtils.today());
        if (cursor == null) {
            return;
        }
        while (cursor.before(currentMonday)) {
            String weekStart = DateUtils.key(cursor);
            String weekEnd = DateUtils.key(DateUtils.addDays(cursor, 6));
            if (!db.hasBalanceSettlement(weekStart)) {
                long budget = LedgerPrefs.budget(context, true);
                long spent = db.totalBetween(weekStart, weekEnd);
                db.addBalanceSettlement(weekStart, weekEnd, budget, spent, budget - spent);
            }
            cursor.add(Calendar.DAY_OF_MONTH, 7);
        }
    }

    /** The Monday on/after the 1.3 update date; saved once so later releases keep the origin. */
    public static String ensureStartWeek(Context context) {
        String saved = LedgerPrefs.balanceStartWeek(context);
        if (saved != null && DateUtils.parse(saved) != null) {
            return saved;
        }
        Calendar updated = Calendar.getInstance();
        try {
            updated.setTimeInMillis(context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0).lastUpdateTime);
        } catch (PackageManager.NameNotFoundException ignored) {
            updated.setTimeInMillis(System.currentTimeMillis());
        }
        Calendar monday = DateUtils.startOfWeek(updated);
        if (!DateUtils.key(monday).equals(DateUtils.key(updated))) {
            monday.add(Calendar.DAY_OF_MONTH, 7);
        }
        String key = DateUtils.key(monday);
        LedgerPrefs.setBalanceStartWeek(context, key);
        return key;
    }

    public static long currentWeekRemaining(Context context, LedgerDb db) {
        DateRange week = DateRange.week();
        return LedgerPrefs.budget(context, true) - db.totalBetween(week.start, week.end);
    }

    /** Starts fresh after "clear personal data" without re-crediting already deleted weeks. */
    public static void restartFromNextWeek(Context context) {
        Calendar next = DateUtils.startOfWeek(DateUtils.today());
        next.add(Calendar.DAY_OF_MONTH, 7);
        LedgerPrefs.setBalanceStartWeek(context, DateUtils.key(next));
    }

    public static String startLabel(Context context) {
        return DateUtils.shortDate(ensureStartWeek(context));
    }
}
