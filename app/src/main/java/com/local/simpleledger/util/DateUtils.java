package com.local.simpleledger.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/** Calendar helpers. Kept Android-free so date logic is easy to reason about. */
public final class DateUtils {

    private static final String[] WEEKDAYS = {"周日", "周一", "周二", "周三", "周四", "周五", "周六"};
    private static final String[] WEEKDAY_SHORT = {"一", "二", "三", "四", "五", "六", "日"};

    private DateUtils() {
    }

    public static Calendar today() {
        return startOfDay(Calendar.getInstance());
    }

    public static Calendar startOfDay(Calendar source) {
        Calendar calendar = (Calendar) source.clone();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar;
    }

    public static Calendar startOfMonth(Calendar source) {
        Calendar calendar = startOfDay(source);
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        return calendar;
    }

    public static Calendar endOfMonth(Calendar source) {
        Calendar calendar = startOfMonth(source);
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
        return calendar;
    }

    /** Monday-based start of week. */
    public static Calendar startOfWeek(Calendar source) {
        Calendar calendar = startOfDay(source);
        int offset = (calendar.get(Calendar.DAY_OF_WEEK) + 5) % 7;
        calendar.add(Calendar.DAY_OF_MONTH, -offset);
        return calendar;
    }

    public static Calendar endOfWeek(Calendar source) {
        Calendar calendar = startOfWeek(source);
        calendar.add(Calendar.DAY_OF_MONTH, 6);
        return calendar;
    }

    public static Calendar addDays(Calendar source, int days) {
        Calendar calendar = (Calendar) source.clone();
        calendar.add(Calendar.DAY_OF_MONTH, days);
        return calendar;
    }

    public static Calendar addMonths(Calendar source, int months) {
        Calendar calendar = startOfMonth(source);
        calendar.add(Calendar.MONTH, months);
        return calendar;
    }

    public static String key(Calendar calendar) {
        return String.format(Locale.CHINA, "%04d-%02d-%02d",
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.DAY_OF_MONTH));
    }

    public static String todayKey() {
        return key(today());
    }

    public static String monthKey(Calendar calendar) {
        return String.format(Locale.CHINA, "%04d-%02d",
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH) + 1);
    }

    public static String monthLabel(Calendar calendar) {
        return String.format(Locale.CHINA, "%d年%d月",
                calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1);
    }

    public static String monthDayLabel(Calendar calendar) {
        return String.format(Locale.CHINA, "%d月%d日",
                calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH));
    }

    public static String shortDate(String dateKey) {
        Calendar calendar = parse(dateKey);
        return calendar == null ? dateKey : monthDayLabel(calendar);
    }

    public static String dayLabel(String dateKey) {
        Calendar calendar = parse(dateKey);
        if (calendar == null) {
            return dateKey;
        }
        return String.format(Locale.CHINA, "%d月%d日 · %s",
                calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.DAY_OF_MONTH),
                weekday(calendar));
    }

    public static String fullDayLabel(String dateKey) {
        Calendar calendar = parse(dateKey);
        if (calendar == null) {
            return dateKey;
        }
        return String.format(Locale.CHINA, "%d年%d月%d日 · %s",
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.DAY_OF_MONTH),
                weekday(calendar));
    }

    public static String dateWithWeekday(String dateKey) {
        Calendar calendar = parse(dateKey);
        if (calendar == null) {
            return dateKey;
        }
        return monthDayLabel(calendar) + " " + weekday(calendar);
    }

    public static String weekday(Calendar calendar) {
        int index = calendar.get(Calendar.DAY_OF_WEEK);
        if (index < Calendar.SUNDAY || index > Calendar.SATURDAY) {
            return "";
        }
        return WEEKDAYS[index - 1];
    }

    public static String weekdayShort(int mondayBasedIndex) {
        if (mondayBasedIndex < 0 || mondayBasedIndex >= WEEKDAY_SHORT.length) {
            return "";
        }
        return WEEKDAY_SHORT[mondayBasedIndex];
    }

    public static Calendar parse(String dateKey) {
        if (dateKey == null || dateKey.length() != 10) {
            return null;
        }
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA);
            format.setLenient(false);
            Date date = format.parse(dateKey);
            return date == null ? null : startOfDayFrom(date);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Calendar startOfDayFrom(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return startOfDay(calendar);
    }

    public static String timeLabel(long timestamp) {
        return new SimpleDateFormat("HH:mm", Locale.CHINA).format(new Date(timestamp));
    }

    public static String fileStamp() {
        return new SimpleDateFormat("yyyyMMdd_HHmm", Locale.CHINA).format(new Date());
    }

    public static int daysInMonth(Calendar month) {
        return month.getActualMaximum(Calendar.DAY_OF_MONTH);
    }

    /** Monday-based offset (0..6) for the first day of the month. */
    public static int firstDayOffset(Calendar month) {
        Calendar copy = startOfMonth(month);
        return (copy.get(Calendar.DAY_OF_WEEK) + 5) % 7;
    }

    public static boolean isSameMonth(Calendar a, Calendar b) {
        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR)
                && a.get(Calendar.MONTH) == b.get(Calendar.MONTH);
    }

    public static boolean isSameDay(Calendar a, Calendar b) {
        return isSameMonth(a, b)
                && a.get(Calendar.DAY_OF_MONTH) == b.get(Calendar.DAY_OF_MONTH);
    }

    public static boolean isToday(String dateKey) {
        return todayKey().equals(dateKey);
    }

    public static boolean isFuture(String dateKey) {
        return dateKey != null && dateKey.compareTo(todayKey()) > 0;
    }

    public static String friendlyDate(String dateKey) {
        if (todayKey().equals(dateKey)) {
            return "今天";
        }
        Calendar today = today();
        if (key(addDays(today, -1)).equals(dateKey)) {
            return "昨天";
        }
        return dateWithWeekday(dateKey);
    }
}
