package com.local.simpleledger.ui;

import android.app.DatePickerDialog;
import android.content.Context;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.android.material.card.MaterialCardView;
import com.local.simpleledger.R;
import com.local.simpleledger.data.LedgerDb;
import com.local.simpleledger.model.Category;
import com.local.simpleledger.model.SubcategoryTotal;
import com.local.simpleledger.util.DateRange;
import com.local.simpleledger.util.DateUtils;
import com.local.simpleledger.util.Money;

import java.util.Calendar;
import java.util.List;

/** Range report: pick any time span and read totals down to subcategory level. */
public final class RangeStatsPage extends LinearLayout {

    private static final String[] PRESETS = {"本月", "本周", "本日", "近七天"};

    private final LedgerDb db;
    private final Navigator navigator;
    private TextView startValue;
    private TextView endValue;
    private final TextView rangeTitle;
    private final TextView totalValue;
    private final TextView metaValue;
    private final LinearLayout quickRow;
    private final LinearLayout breakdown;
    private final TextView[] quickChips = new TextView[PRESETS.length];

    private String start;
    private String end;
    private int presetIndex = 0;

    public RangeStatsPage(Context context, LedgerDb db, Navigator navigator) {
        super(context);
        this.db = db;
        this.navigator = navigator;
        DateRange initial = DateRange.month();
        start = initial.start;
        end = initial.end;

        setOrientation(VERTICAL);
        setBackgroundColor(Ui.color(context, R.color.bg));
        addView(Ui.toolbar(context, "区间统计", navigator::popPage));

        ScrollView scroll = new ScrollView(context);
        scroll.setFillViewport(true);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setClipToPadding(false);
        scroll.setOverScrollMode(OVER_SCROLL_NEVER);
        LinearLayout body = Ui.vertical(context);
        body.setPadding(Ui.dp(context, 20), Ui.dp(context, 12),
                Ui.dp(context, 20), Ui.dp(context, 30));
        scroll.addView(body);
        addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        MaterialCardView rangeCard = Ui.card(context, 20, true, false);
        LinearLayout rangeInner = Ui.vertical(context);
        rangeInner.setPadding(Ui.dp(context, 15), Ui.dp(context, 14),
                Ui.dp(context, 15), Ui.dp(context, 14));
        rangeInner.addView(Ui.label(context, "统计区间", 10));
        LinearLayout dates = Ui.horizontal(context);
        LinearLayout.LayoutParams datesParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        datesParams.topMargin = Ui.dp(context, 9);
        MaterialCardView startCard = dateCard(context, "开始日期", true);
        MaterialCardView endCard = dateCard(context, "结束日期", false);
        LinearLayout.LayoutParams startParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        startParams.rightMargin = Ui.dp(context, 5);
        dates.addView(startCard, startParams);
        LinearLayout.LayoutParams endParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        endParams.leftMargin = Ui.dp(context, 5);
        dates.addView(endCard, endParams);
        rangeInner.addView(dates, datesParams);

        quickRow = Ui.horizontal(context);
        quickRow.setGravity(Gravity.START);
        LinearLayout.LayoutParams quickParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        quickParams.topMargin = Ui.dp(context, 11);
        for (int i = 0; i < PRESETS.length; i++) {
            final int index = i;
            TextView chip = Ui.text(context, PRESETS[i], 11,
                    Ui.color(context, R.color.text_secondary), true);
            chip.setGravity(Gravity.CENTER);
            chip.setPadding(Ui.dp(context, 12), Ui.dp(context, 7),
                    Ui.dp(context, 12), Ui.dp(context, 7));
            chip.setOnClickListener(v -> applyPreset(index));
            LinearLayout.LayoutParams chipParams = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            if (i > 0) {
                chipParams.leftMargin = Ui.dp(context, 5);
            }
            quickRow.addView(chip, chipParams);
            quickChips[i] = chip;
        }
        rangeInner.addView(quickRow, quickParams);
        rangeCard.addView(rangeInner);
        body.addView(rangeCard, Ui.lpMatchWrap());

        MaterialCardView summary = Ui.card(context, 22, false, false);
        summary.setCardBackgroundColor(Ui.color(context, R.color.accent));
        LinearLayout summaryInner = Ui.vertical(context);
        summaryInner.setPadding(Ui.dp(context, 18), Ui.dp(context, 16),
                Ui.dp(context, 18), Ui.dp(context, 16));
        int contrast = Ui.color(context, R.color.accent_contrast);
        rangeTitle = Ui.text(context, "", 11, Ui.alpha(contrast, 0.74f), true);
        summaryInner.addView(rangeTitle);
        totalValue = Ui.text(context, "¥0.00", 33, contrast, true);
        totalValue.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        LinearLayout.LayoutParams totalParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        totalParams.topMargin = Ui.dp(context, 8);
        summaryInner.addView(totalValue, totalParams);
        metaValue = Ui.text(context, "", 10, Ui.alpha(contrast, 0.72f), false);
        LinearLayout.LayoutParams metaParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        metaParams.topMargin = Ui.dp(context, 7);
        summaryInner.addView(metaValue, metaParams);
        summary.addView(summaryInner);
        LinearLayout.LayoutParams summaryParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        summaryParams.topMargin = Ui.dp(context, 14);
        body.addView(summary, summaryParams);

        LinearLayout breakdownHeader = Ui.horizontal(context);
        breakdownHeader.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams breakdownHeaderParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        breakdownHeaderParams.topMargin = Ui.dp(context, 24);
        breakdownHeaderParams.bottomMargin = Ui.dp(context, 9);
        breakdownHeader.addView(Ui.sectionTitle(context, "小类明细"),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        body.addView(breakdownHeader, breakdownHeaderParams);
        breakdown = Ui.vertical(context);
        body.addView(breakdown);

        updateDates();
        updateQuickStyles();
        render(false);
    }

    private MaterialCardView dateCard(Context context, String label, boolean isStart) {
        MaterialCardView card = Ui.card(context, 14, true, true);
        LinearLayout content = Ui.vertical(context);
        content.setPadding(Ui.dp(context, 11), Ui.dp(context, 10),
                Ui.dp(context, 11), Ui.dp(context, 10));
        content.addView(Ui.text(context, label, 9,
                Ui.color(context, R.color.text_tertiary), true));
        TextView value = Ui.text(context, "", 12,
                Ui.color(context, R.color.text_primary), true);
        LinearLayout.LayoutParams valueParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        valueParams.topMargin = Ui.dp(context, 4);
        content.addView(value, valueParams);
        card.addView(content);
        card.setOnClickListener(v -> pickDate(isStart));
        card.setContentDescription(isStart ? "选择开始日期" : "选择结束日期");
        if (isStart) {
            startValue = value;
        } else {
            endValue = value;
        }
        return card;
    }

    private void pickDate(boolean isStart) {
        Calendar initial = DateUtils.parse(isStart ? start : end);
        if (initial == null) {
            initial = DateUtils.today();
        }
        DatePickerDialog dialog = new DatePickerDialog(getContext(), (view, year, month, day) -> {
            Calendar picked = Calendar.getInstance();
            picked.set(year, month, day);
            String key = DateUtils.key(picked);
            if (isStart) {
                start = key;
                if (start.compareTo(end) > 0) {
                    end = start;
                }
            } else {
                end = key;
                if (end.compareTo(start) < 0) {
                    start = end;
                }
            }
            presetIndex = -1;
            updateDates();
            updateQuickStyles();
            render(true);
        }, initial.get(Calendar.YEAR), initial.get(Calendar.MONTH),
                initial.get(Calendar.DAY_OF_MONTH));
        dialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        if (!isStart) {
            Calendar min = DateUtils.parse(start);
            if (min != null) {
                dialog.getDatePicker().setMinDate(min.getTimeInMillis());
            }
        }
        dialog.show();
        dialog.getButton(DatePickerDialog.BUTTON_POSITIVE)
                .setTextColor(Ui.color(getContext(), R.color.accent));
        dialog.getButton(DatePickerDialog.BUTTON_NEGATIVE)
                .setTextColor(Ui.color(getContext(), R.color.text_secondary));
    }

    private void applyPreset(int index) {
        DateRange range;
        switch (index) {
            case 1:
                range = DateRange.week();
                break;
            case 2:
                range = DateRange.today();
                break;
            case 3:
                range = DateRange.lastSevenDays();
                break;
            case 0:
            default:
                range = DateRange.month();
                break;
        }
        start = range.start;
        end = range.end;
        presetIndex = index;
        updateDates();
        updateQuickStyles();
        render(true);
    }

    private void updateDates() {
        startValue.setText(DateUtils.dateWithWeekday(start));
        endValue.setText(DateUtils.dateWithWeekday(end));
    }

    private void updateQuickStyles() {
        Context context = getContext();
        for (int i = 0; i < quickChips.length; i++) {
            boolean active = i == presetIndex;
            TextView chip = quickChips[i];
            chip.setTextColor(active
                    ? Ui.color(context, R.color.accent_contrast)
                    : Ui.color(context, R.color.text_secondary));
            chip.setBackground(Ui.roundRect(context,
                    active ? Ui.color(context, R.color.accent)
                            : Ui.color(context, R.color.surface),
                    11, 1, active ? Ui.color(context, R.color.accent)
                            : Ui.color(context, R.color.divider)));
        }
    }

    private void render(boolean animate) {
        Context context = getContext();
        long total = db.totalBetween(start, end);
        int count = db.countBetween(start, end);
        rangeTitle.setText(DateRange.rangeLabel(start, end));
        totalValue.setText(Money.yuan(total));
        int days = daysBetween(start, end);
        long average = days <= 0 ? 0 : total / days;
        metaValue.setText(count == 0
                ? "这段时间还没有支出"
                : count + " 笔 · 共 " + days + " 天 · 日均 " + Money.yuan(average));

        breakdown.animate().cancel();
        if (animate) {
            breakdown.animate().alpha(0f).translationY(Ui.dp(context, 5))
                    .setDuration(70).withEndAction(() -> {
                        buildBreakdown();
                        breakdown.setAlpha(0f);
                        breakdown.setTranslationY(Ui.dp(context, 5));
                        breakdown.animate().alpha(1f).translationY(0f)
                                .setDuration(170).start();
                    }).start();
        } else {
            buildBreakdown();
            breakdown.setAlpha(1f);
            breakdown.setTranslationY(0f);
        }
    }

    private void buildBreakdown() {
        Context context = getContext();
        breakdown.removeAllViews();
        List<SubcategoryTotal> all = db.subcategoryTotals(start, end);
        long total = db.totalBetween(start, end);
        if (all.isEmpty()) {
            MaterialCardView empty = Ui.card(context, 18, true, false);
            LinearLayout emptyInner = Ui.vertical(context);
            emptyInner.setGravity(Gravity.CENTER);
            emptyInner.setPadding(Ui.dp(context, 15), Ui.dp(context, 26),
                    Ui.dp(context, 15), Ui.dp(context, 26));
            emptyInner.addView(Ui.iconCircle(context, R.drawable.ic_chart,
                    Ui.color(context, R.color.text_tertiary), 42, 20));
            TextView message = Ui.text(context, "这段时间没有支出", 12,
                    Ui.color(context, R.color.text_secondary), true);
            LinearLayout.LayoutParams messageParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            messageParams.topMargin = Ui.dp(context, 9);
            emptyInner.addView(message, messageParams);
            empty.addView(emptyInner);
            breakdown.addView(empty);
            return;
        }
        Category current = null;
        MaterialCardView card = null;
        LinearLayout categoryList = null;
        long categoryTotal = 0;
        for (int i = 0; i < all.size(); i++) {
            SubcategoryTotal row = all.get(i);
            if (row.category != current) {
                if (i > 0) {
                    finishCategoryCard(card, categoryList, current, categoryTotal, total);
                }
                current = row.category;
                card = Ui.card(context, 18, true, false);
                categoryList = Ui.vertical(context);
                categoryList.setPadding(Ui.dp(context, 14), Ui.dp(context, 13),
                        Ui.dp(context, 14), Ui.dp(context, 11));
                LinearLayout categoryHeader = Ui.horizontal(context);
                categoryHeader.setGravity(Gravity.CENTER_VERTICAL);
                categoryHeader.addView(Ui.iconCircle(context, current.iconRes,
                        current.color(context), 33, 18));
                LinearLayout names = Ui.vertical(context);
                LinearLayout.LayoutParams namesParams = new LinearLayout.LayoutParams(
                        0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
                namesParams.leftMargin = Ui.dp(context, 9);
                names.addView(Ui.text(context, current.title, 14,
                        Ui.color(context, R.color.text_primary), true));
                categoryHeader.addView(names, namesParams);
                categoryList.addView(categoryHeader);
                View track = new View(context);
                track.setBackground(Ui.roundRect(context,
                        Ui.alpha(current.color(context), 0.14f), 3));
                LinearLayout.LayoutParams trackParams = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(context, 5));
                trackParams.topMargin = Ui.dp(context, 9);
                trackParams.bottomMargin = Ui.dp(context, 4);
                categoryList.addView(track, trackParams);
                card.addView(categoryList);
                LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                if (i > 0) {
                    cardParams.topMargin = Ui.dp(context, 9);
                }
                breakdown.addView(card, cardParams);
                categoryTotal = 0;
            }
            categoryTotal += row.amountCents;
            LinearLayout subRow = Ui.horizontal(context);
            subRow.setGravity(Gravity.CENTER_VERTICAL);
            subRow.setPadding(Ui.dp(context, 4), Ui.dp(context, 7),
                    Ui.dp(context, 2), Ui.dp(context, 7));
            View dot = new View(context);
            dot.setBackground(Ui.circle(Ui.alpha(current.color(context), 0.55f)));
            LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams(
                    Ui.dp(context, 4), Ui.dp(context, 4));
            dotParams.rightMargin = Ui.dp(context, 8);
            subRow.addView(dot, dotParams);
            subRow.addView(Ui.text(context, row.subcategory, 12,
                    Ui.color(context, R.color.text_secondary), false),
                    new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            subRow.addView(Ui.text(context, Money.yuan(row.amountCents), 13,
                    Ui.color(context, R.color.text_primary), true));
            categoryList.addView(subRow);
        }
        finishCategoryCard(card, categoryList, current, categoryTotal, total);
    }

    private void finishCategoryCard(MaterialCardView card, LinearLayout content,
                                    Category category, long categoryTotal, long grandTotal) {
        if (card == null || content == null || category == null) {
            return;
        }
        // Replace the placeholder progress bar at index 1 with one sized to the category.
        if (content.getChildCount() > 1) {
            View placeholder = content.getChildAt(1);
            Context context = getContext();
            FrameLayoutBar bar = new FrameLayoutBar(context, category.color(context),
                    grandTotal <= 0 ? 0f : categoryTotal / (float) grandTotal, category);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(context, 5));
            params.topMargin = Ui.dp(context, 9);
            params.bottomMargin = Ui.dp(context, 4);
            content.removeViewAt(1);
            content.addView(bar, 1, params);
        }
    }

    private static int daysBetween(String start, String end) {
        Calendar startCalendar = DateUtils.parse(start);
        Calendar endCalendar = DateUtils.parse(end);
        if (startCalendar == null || endCalendar == null) {
            return 1;
        }
        long diff = endCalendar.getTimeInMillis() - startCalendar.getTimeInMillis();
        return Math.max(1, (int) (diff / (24L * 60L * 60L * 1000L)) + 1);
    }

    /** Small proportional bar used in the category header. */
    private static final class FrameLayoutBar extends LinearLayout {
        FrameLayoutBar(Context context, int color, float fraction, Category category) {
            super(context);
            setOrientation(HORIZONTAL);
            setBackground(Ui.roundRect(context, Ui.alpha(color, 0.14f), 3));
            float safe = Math.max(0f, Math.min(1f, fraction));
            if (safe > 0) {
                View fill = new View(context);
                fill.setBackground(Ui.roundRect(context, color, 3));
                addView(fill, new LinearLayout.LayoutParams(
                        0, ViewGroup.LayoutParams.MATCH_PARENT, safe));
            }
            if (safe < 1f) {
                View rest = new View(context);
                addView(rest, new LinearLayout.LayoutParams(
                        0, ViewGroup.LayoutParams.MATCH_PARENT, 1f - safe));
            }
            setContentDescription(category.title + "占比" + Math.round(safe * 100) + "%");
        }
    }
}
