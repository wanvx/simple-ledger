package com.local.simpleledger.util;

import java.util.Calendar;
import java.util.Locale;

/** An inclusive date range backed by yyyy-MM-dd keys. */
public final class DateRange {

    public final String start;
    public final String end;
    public final String label;
    /** True when the range was produced by one of the four quick presets. */
    public final boolean quick;

    private DateRange(String start, String end, String label, boolean quick) {
        this.start = start;
        this.end = end;
        this.label = label;
        this.quick = quick;
    }

    public static DateRange today() {
        Calendar now = DateUtils.today();
        return new DateRange(DateUtils.key(now), DateUtils.key(now), "本日", true);
    }

    public static DateRange week() {
        Calendar now = DateUtils.today();
        Calendar start = DateUtils.startOfWeek(now);
        Calendar end = DateUtils.endOfWeek(now);
        return new DateRange(DateUtils.key(start), DateUtils.key(end), "本周", true);
    }

    public static DateRange month() {
        Calendar now = DateUtils.today();
        Calendar start = DateUtils.startOfMonth(now);
        Calendar end = DateUtils.endOfMonth(now);
        return new DateRange(DateUtils.key(start), DateUtils.key(end), "本月", true);
    }

    public static DateRange lastSevenDays() {
        Calendar now = DateUtils.today();
        Calendar start = DateUtils.addDays(now, -6);
        return new DateRange(DateUtils.key(start), DateUtils.key(now), "近七天", true);
    }

    public static DateRange ofMonth(Calendar month) {
        Calendar safe = DateUtils.startOfMonth(month);
        return new DateRange(DateUtils.key(safe), DateUtils.key(DateUtils.endOfMonth(safe)),
                DateUtils.monthLabel(safe), false);
    }

    public static DateRange monthToDate() {
        Calendar now = DateUtils.today();
        return new DateRange(DateUtils.key(DateUtils.startOfMonth(now)), DateUtils.key(now),
                "本月", true);
    }

    public static DateRange weekToDate() {
        Calendar now = DateUtils.today();
        return new DateRange(DateUtils.key(DateUtils.startOfWeek(now)), DateUtils.key(now),
                "本周", true);
    }

    public static DateRange custom(String start, String end) {
        String safeStart = start;
        String safeEnd = end;
        if (safeStart == null || DateUtils.parse(safeStart) == null) {
            safeStart = DateUtils.todayKey();
        }
        if (safeEnd == null || DateUtils.parse(safeEnd) == null) {
            safeEnd = DateUtils.todayKey();
        }
        if (safeStart.compareTo(safeEnd) > 0) {
            String swap = safeStart;
            safeStart = safeEnd;
            safeEnd = swap;
        }
        return new DateRange(safeStart, safeEnd, rangeLabel(safeStart, safeEnd), false);
    }

    public static DateRange forExport(String preset) {
        if ("week".equals(preset)) {
            return weekToDate();
        }
        if ("day".equals(preset)) {
            return today();
        }
        if ("seven".equals(preset)) {
            return lastSevenDays();
        }
        return monthToDate();
    }

    public static String rangeLabel(String start, String end) {
        Calendar startCalendar = DateUtils.parse(start);
        Calendar endCalendar = DateUtils.parse(end);
        if (startCalendar == null || endCalendar == null) {
            return start + " – " + end;
        }
        if (start.equals(end)) {
            return DateUtils.fullDayLabel(start);
        }
        String startPart = DateUtils.monthDayLabel(startCalendar);
        String endPart = DateUtils.monthDayLabel(endCalendar);
        if (startCalendar.get(Calendar.YEAR) != endCalendar.get(Calendar.YEAR)) {
            startPart = String.format(Locale.CHINA, "%d年%s",
                    startCalendar.get(Calendar.YEAR), startPart);
            endPart = String.format(Locale.CHINA, "%d年%s",
                    endCalendar.get(Calendar.YEAR), endPart);
        }
        return startPart + " – " + endPart;
    }

    public String filePart() {
        if (start.equals(end)) {
            return start;
        }
        return start + "_" + end;
    }
}
