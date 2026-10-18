package com.local.simpleledger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.local.simpleledger.data.Entry;
import com.local.simpleledger.model.Category;
import com.local.simpleledger.model.WarningLevel;
import com.local.simpleledger.util.DateRange;
import com.local.simpleledger.util.DateUtils;
import com.local.simpleledger.util.Money;

import org.junit.Test;

import java.util.Calendar;

/** Pure logic checks for money precision, week boundaries and warning stages. */
public class LogicTest {

    @Test
    public void moneyKeepsExactCents() {
        assertEquals(999L, Money.parseCents("9.99"));
        assertEquals(20000L, Money.parseCents("200"));
        assertEquals(50L, Money.parseCents(".50"));
        assertEquals(1L, Money.parseCents("0.01"));
        assertEquals(-1L, Money.parseCents("9.999"));
        assertEquals(-1L, Money.parseCents("0"));
        assertEquals(-1L, Money.parseCents("-3"));
        assertEquals("9.99", Money.sanitizeAmount("9.999"));
        assertEquals("12.30", Money.sanitizeAmount("12.30"));
    }

    @Test
    public void warningStagesUseInclusiveOrangeLines() {
        assertEquals(WarningLevel.GREEN, WarningLevel.of(0, 5000, 7500));
        assertEquals(WarningLevel.GREEN, WarningLevel.of(4999, 5000, 7500));
        assertEquals(WarningLevel.ORANGE, WarningLevel.of(5000, 5000, 7500));
        assertEquals(WarningLevel.ORANGE, WarningLevel.of(7500, 5000, 7500));
        assertEquals(WarningLevel.RED, WarningLevel.of(7501, 5000, 7500));
    }

    @Test
    public void weekStartsOnMondayAndEndsOnSunday() {
        Calendar sunday = Calendar.getInstance();
        sunday.clear();
        sunday.set(2026, Calendar.OCTOBER, 18);
        Calendar monday = DateUtils.startOfWeek(sunday);
        Calendar end = DateUtils.endOfWeek(sunday);
        assertEquals("2026-10-12", DateUtils.key(monday));
        assertEquals("2026-10-18", DateUtils.key(end));
    }

    @Test
    public void customOtherNameIsShownWhenPresent() {
        Entry plain = new Entry(1, "2026-10-18", "daily", "其他", "",
                "纸巾", 1000, 0);
        Entry named = new Entry(2, "2026-10-18", "daily", "其他", "话费",
                "充值", 2000, 0);
        assertEquals("其他", plain.displaySubcategory());
        assertEquals("话费", named.displaySubcategory());
        assertTrue(named.displayLine().contains("话费"));
    }

    @Test
    public void dailyCatalogIncludesMedical() {
        boolean found = false;
        for (String value : Category.DAILY.subcategories()) {
            if ("看病买药".equals(value)) {
                found = true;
            }
        }
        assertTrue(found);
    }

    @Test
    public void customRangeSwapsReversedDates() {
        DateRange range = DateRange.custom("2026-10-18", "2026-10-01");
        assertEquals("2026-10-01", range.start);
        assertEquals("2026-10-18", range.end);
        assertTrue(range.label.contains("10月1日"));
    }
}
