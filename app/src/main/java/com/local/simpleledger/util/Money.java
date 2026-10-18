package com.local.simpleledger.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.Locale;

/** Money conversion and display. Storage is always a long count of cents. */
public final class Money {

    private static final DecimalFormat PLAIN = new DecimalFormat("0.00");
    private static final DecimalFormat GROUPED = new DecimalFormat("#,##0.00");

    private Money() {
    }

    public static String plain(long cents) {
        return PLAIN.format(cents / 100.0);
    }

    public static String grouped(long cents) {
        return GROUPED.format(cents / 100.0);
    }

    public static String yuan(long cents) {
        return "¥" + grouped(cents);
    }

    public static String yuanPlain(long cents) {
        return "¥" + plain(cents);
    }

    public static String signedYuan(long cents) {
        return (cents >= 0 ? "+¥" : "-¥") + grouped(Math.abs(cents));
    }

    /**
     * Parses a user-typed yuan amount. Returns -1 for empty, malformed, over-precise,
     * non-positive or excessively large input.
     */
    public static long parseCents(String raw) {
        if (raw == null) {
            return -1;
        }
        String value = raw.trim();
        if (value.isEmpty() || ".".equals(value)) {
            return -1;
        }
        if (value.startsWith(".")) {
            value = "0" + value;
        }
        if (!value.matches("\\d{0,9}(\\.\\d{0,2})?")) {
            return -1;
        }
        try {
            BigDecimal decimal = new BigDecimal(value);
            if (decimal.scale() > 2) {
                return -1;
            }
            if (decimal.compareTo(BigDecimal.ZERO) <= 0) {
                return -1;
            }
            BigDecimal cents = decimal.movePointRight(2).setScale(0, RoundingMode.UNNECESSARY);
            if (cents.compareTo(new BigDecimal("99999999999")) > 0) {
                return -1;
            }
            return cents.longValue();
        } catch (Exception ignored) {
            return -1;
        }
    }

    /** Keeps only digits and one decimal point, with at most two decimal places. */
    public static String sanitizeAmount(String raw) {
        if (raw == null) {
            return "";
        }
        StringBuilder digits = new StringBuilder();
        boolean dotSeen = false;
        int decimals = 0;
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c >= '0' && c <= '9') {
                if (dotSeen) {
                    if (decimals >= 2) {
                        continue;
                    }
                    decimals++;
                }
                if (digits.length() >= 11) {
                    continue;
                }
                digits.append(c);
            } else if ((c == '.' || c == '。') && !dotSeen) {
                if (digits.length() == 0) {
                    digits.append('0');
                }
                digits.append('.');
                dotSeen = true;
            }
        }
        if (digits.length() == 0) {
            return "";
        }
        String clean = digits.toString();
        if (clean.startsWith("0") && clean.length() > 1 && clean.charAt(1) != '.') {
            while (clean.length() > 1 && clean.charAt(0) == '0' && clean.charAt(1) != '.') {
                clean = clean.substring(1);
            }
        }
        return clean;
    }

    public static String thresholdLabel(long cents) {
        return PLAIN.format(cents / 100.0);
    }

    public static String input(long cents) {
        return String.format(Locale.CHINA, "%.2f", cents / 100.0);
    }
}
