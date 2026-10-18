package com.local.simpleledger.ui;

import android.content.Context;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.android.material.card.MaterialCardView;
import com.local.simpleledger.R;
import com.local.simpleledger.data.Entry;
import com.local.simpleledger.data.LedgerDb;
import com.local.simpleledger.model.Category;
import com.local.simpleledger.model.RemainingMode;
import com.local.simpleledger.model.WarningLevel;
import com.local.simpleledger.util.DateRange;
import com.local.simpleledger.util.DateUtils;
import com.local.simpleledger.util.LedgerPrefs;
import com.local.simpleledger.util.Money;

import java.util.Calendar;
import java.util.List;
import java.util.Map;

/** Page two: month calendar, daily details, overview totals, balance and export. */
public final class HistoryPage extends LinearLayout {

    private final LedgerDb db;
    private final Navigator navigator;
    private final Calendar month = DateUtils.startOfMonth(DateUtils.today());
    private Calendar selected = DateUtils.today();

    private final TextView monthLabel;
    private final TextView todayButton;
    private final TextView summaryTitle;
    private final TextView summaryAmount;
    private final TextView summaryMeta;
    private final TextView summaryBadge;
    private final TextView dayTitle;
    private final TextView dayMeta;
    private final TextView dayBadge;
    private final LinearLayout dayDetail;
    private final LinearLayout calendarGrid;
    private final LinearLayout overviewRow;
    private final LinearLayout remainingHost;

    public HistoryPage(Context context, LedgerDb db, Navigator navigator) {
        super(context);
        this.db = db;
        this.navigator = navigator;
        setOrientation(VERTICAL);
        setBackgroundColor(Ui.color(context, R.color.bg));

        ScrollView scroll = new ScrollView(context);
        scroll.setFillViewport(true);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setClipToPadding(false);
        scroll.setOverScrollMode(OVER_SCROLL_NEVER);
        LinearLayout body = Ui.vertical(context);
        body.setPadding(Ui.dp(context, 20), Ui.dp(context, 22),
                Ui.dp(context, 20), Ui.dp(context, 30));
        scroll.addView(body);
        addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        LinearLayout heading = Ui.horizontal(context);
        heading.setGravity(Gravity.BOTTOM);
        LinearLayout headingTexts = Ui.vertical(context);
        headingTexts.addView(Ui.title(context, "记录", 29));
        TextView subtitle = Ui.secondary(context, "回看每一笔，心里有数", 12);
        LinearLayout.LayoutParams subParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        subParams.topMargin = Ui.dp(context, 7);
        headingTexts.addView(subtitle, subParams);
        heading.addView(headingTexts, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        LinearLayout rangeButton = Ui.horizontal(context);
        rangeButton.setGravity(Gravity.CENTER_VERTICAL);
        rangeButton.setPadding(Ui.dp(context, 11), Ui.dp(context, 8),
                Ui.dp(context, 11), Ui.dp(context, 8));
        rangeButton.setBackground(Ui.roundRect(context,
                Ui.color(context, R.color.accent_soft), 12));
        rangeButton.addView(Ui.icon(context, R.drawable.ic_chart, 14,
                Ui.color(context, R.color.accent)));
        TextView rangeLabel = Ui.text(context, "区间统计", 11,
                Ui.color(context, R.color.accent), true);
        LinearLayout.LayoutParams rangeLabelParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rangeLabelParams.leftMargin = Ui.dp(context, 5);
        rangeButton.addView(rangeLabel, rangeLabelParams);
        rangeButton.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
            navigator.openRangeStats();
        });
        Ui.pressScale(rangeButton);
        heading.addView(rangeButton);
        body.addView(heading);

        LinearLayout monthBar = Ui.horizontal(context);
        monthBar.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams monthBarParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(context, 48));
        monthBarParams.topMargin = Ui.dp(context, 14);
        ImageView prev = Ui.iconButton(context, R.drawable.ic_chevron_left,
                Ui.color(context, R.color.text_primary), 23, v -> shiftMonth(-1));
        prev.setContentDescription("上个月");
        monthBar.addView(prev, new LinearLayout.LayoutParams(
                Ui.dp(context, 44), Ui.dp(context, 44)));
        monthLabel = Ui.text(context, "", 17,
                Ui.color(context, R.color.text_primary), true);
        monthLabel.setGravity(Gravity.CENTER);
        monthBar.addView(monthLabel, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        todayButton = Ui.text(context, "今天", 11,
                Ui.color(context, R.color.accent), true);
        todayButton.setGravity(Gravity.CENTER);
        todayButton.setPadding(Ui.dp(context, 9), Ui.dp(context, 5),
                Ui.dp(context, 9), Ui.dp(context, 5));
        todayButton.setBackground(Ui.roundRect(context,
                Ui.alpha(Ui.color(context, R.color.accent), 0.13f), 20));
        todayButton.setOnClickListener(v -> goToday());
        LinearLayout.LayoutParams todayParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        todayParams.rightMargin = Ui.dp(context, 5);
        monthBar.addView(todayButton, todayParams);
        ImageView next = Ui.iconButton(context, R.drawable.ic_chevron_right,
                Ui.color(context, R.color.text_primary), 23, v -> shiftMonth(1));
        next.setContentDescription("下个月");
        monthBar.addView(next, new LinearLayout.LayoutParams(
                Ui.dp(context, 44), Ui.dp(context, 44)));
        body.addView(monthBar, monthBarParams);

        MaterialCardView summary = Ui.card(context, 22, true, true);
        LinearLayout summaryInner = Ui.vertical(context);
        summaryInner.setPadding(Ui.dp(context, 18), Ui.dp(context, 16),
                Ui.dp(context, 18), Ui.dp(context, 16));
        LinearLayout summaryTop = Ui.horizontal(context);
        summaryTop.setGravity(Gravity.CENTER_VERTICAL);
        summaryTitle = Ui.text(context, "", 12,
                Ui.color(context, R.color.text_secondary), true);
        summaryTop.addView(summaryTitle, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        summaryBadge = Ui.chip(context, "", Ui.color(context, R.color.warning_green));
        summaryTop.addView(summaryBadge);
        summaryInner.addView(summaryTop);
        summaryAmount = Ui.text(context, "¥0.00", 32,
                Ui.color(context, R.color.text_primary), true);
        summaryAmount.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        LinearLayout.LayoutParams amountParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        amountParams.topMargin = Ui.dp(context, 11);
        summaryInner.addView(summaryAmount, amountParams);
        summaryMeta = Ui.text(context, "", 11,
                Ui.color(context, R.color.text_tertiary), false);
        LinearLayout.LayoutParams metaParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        metaParams.topMargin = Ui.dp(context, 6);
        summaryInner.addView(summaryMeta, metaParams);
        summary.addView(summaryInner);
        summary.setOnClickListener(v -> navigator.openRangeStats());
        body.addView(summary, Ui.lpMatchWrap());

        MaterialCardView calendarCard = Ui.card(context, 22, true, false);
        LinearLayout calendarInner = Ui.vertical(context);
        calendarInner.setPadding(Ui.dp(context, 9), Ui.dp(context, 13),
                Ui.dp(context, 9), Ui.dp(context, 11));
        LinearLayout weekHeader = Ui.horizontal(context);
        for (int i = 0; i < 7; i++) {
            TextView day = Ui.text(context, DateUtils.weekdayShort(i), 10,
                    Ui.color(context, R.color.text_tertiary), true);
            day.setGravity(Gravity.CENTER);
            weekHeader.addView(day, new LinearLayout.LayoutParams(
                    0, Ui.dp(context, 24), 1f));
        }
        calendarInner.addView(weekHeader);
        calendarGrid = Ui.vertical(context);
        LinearLayout.LayoutParams gridParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        gridParams.topMargin = Ui.dp(context, 2);
        calendarInner.addView(calendarGrid, gridParams);
        View legendDivider = Ui.divider(context);
        LinearLayout.LayoutParams legendDividerParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, Ui.dp(context, 1)));
        legendDividerParams.topMargin = Ui.dp(context, 8);
        calendarInner.addView(legendDivider, legendDividerParams);
        LinearLayout legend = Ui.horizontal(context);
        legend.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams legendParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        legendParams.topMargin = Ui.dp(context, 8);
        legend.addView(legendSquare(context, R.color.warning_green));
        legend.addView(legendText(context, "正常"));
        legend.addView(legendSquare(context, R.color.warning_orange));
        legend.addView(legendText(context, "注意"));
        legend.addView(legendSquare(context, R.color.warning_red));
        legend.addView(legendText(context, "超支"));
        calendarInner.addView(legend, legendParams);
        calendarCard.addView(calendarInner);
        LinearLayout.LayoutParams calendarParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        calendarParams.topMargin = Ui.dp(context, 12);
        body.addView(calendarCard, calendarParams);

        LinearLayout dayHeader = Ui.horizontal(context);
        dayHeader.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams dayHeaderParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dayHeaderParams.topMargin = Ui.dp(context, 24);
        dayHeaderParams.bottomMargin = Ui.dp(context, 9);
        LinearLayout dayTexts = Ui.vertical(context);
        dayTitle = Ui.text(context, "", 17,
                Ui.color(context, R.color.text_primary), true);
        dayMeta = Ui.text(context, "", 10,
                Ui.color(context, R.color.text_tertiary), false);
        LinearLayout.LayoutParams dayMetaParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dayMetaParams.topMargin = Ui.dp(context, 4);
        dayTexts.addView(dayTitle);
        dayTexts.addView(dayMeta, dayMetaParams);
        dayHeader.addView(dayTexts, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        dayBadge = Ui.chip(context, "", Ui.color(context, R.color.warning_green));
        dayHeader.addView(dayBadge);
        body.addView(dayHeader, dayHeaderParams);
        dayDetail = Ui.vertical(context);
        body.addView(dayDetail);

        LinearLayout overviewHeader = Ui.horizontal(context);
        overviewHeader.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams overviewHeaderParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        overviewHeaderParams.topMargin = Ui.dp(context, 26);
        overviewHeaderParams.bottomMargin = Ui.dp(context, 9);
        overviewHeader.addView(Ui.sectionTitle(context, "支出概览"),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        body.addView(overviewHeader, overviewHeaderParams);
        overviewRow = Ui.horizontal(context);
        overviewRow.setGravity(Gravity.TOP);
        body.addView(overviewRow, Ui.lpMatchWrap());

        remainingHost = Ui.vertical(context);
        LinearLayout.LayoutParams remainingParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        remainingParams.topMargin = Ui.dp(context, 16);
        body.addView(remainingHost, remainingParams);

        LinearLayout exportCard = Ui.horizontal(context);
        exportCard.setGravity(Gravity.CENTER_VERTICAL);
        exportCard.setPadding(Ui.dp(context, 17), Ui.dp(context, 16),
                Ui.dp(context, 15), Ui.dp(context, 16));
        exportCard.setBackground(Ui.roundRect(context,
                Ui.color(context, R.color.surface), 18, 1,
                Ui.color(context, R.color.divider)));
        exportCard.setClickable(true);
        exportCard.setFocusable(true);
        exportCard.addView(Ui.iconCircle(context, R.drawable.ic_export,
                Ui.color(context, R.color.accent), 42, 21));
        LinearLayout exportTexts = Ui.vertical(context);
        LinearLayout.LayoutParams exportTextParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        exportTextParams.leftMargin = Ui.dp(context, 12);
        exportTexts.addView(Ui.text(context, "导出账单", 15,
                Ui.color(context, R.color.text_primary), true));
        exportTexts.addView(Ui.text(context, "图片 / 文档", 10,
                Ui.color(context, R.color.text_tertiary), false));
        exportCard.addView(exportTexts, exportTextParams);
        exportCard.addView(Ui.icon(context, R.drawable.ic_chevron_right, 18,
                Ui.color(context, R.color.text_tertiary)));
        exportCard.setOnClickListener(v -> navigator.openExport());
        Ui.pressScale(exportCard);
        LinearLayout.LayoutParams exportParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        exportParams.topMargin = Ui.dp(context, 20);
        body.addView(exportCard, exportParams);

        refresh();
    }

    public void refresh() {
        if (!DateUtils.isSameMonth(month, DateUtils.today())) {
            todayButton.setVisibility(VISIBLE);
        } else {
            todayButton.setVisibility(INVISIBLE);
        }
        if (selected == null) {
            selected = DateUtils.today();
        }
        if (!DateUtils.isSameMonth(selected, month)) {
            selected = DateUtils.isSameMonth(month, DateUtils.today())
                    ? DateUtils.today() : DateUtils.startOfMonth(month);
        }
        monthLabel.setText(DateUtils.monthLabel(month));
        renderCalendar(false);
        renderSummary();
        renderDay(false);
        renderOverview();
        renderRemaining();
    }

    private void shiftMonth(int delta) {
        Calendar next = DateUtils.addMonths(month, delta);
        month.setTimeInMillis(next.getTimeInMillis());
        selected = DateUtils.isSameMonth(month, DateUtils.today())
                ? DateUtils.today() : DateUtils.startOfMonth(month);
        monthLabel.setText(DateUtils.monthLabel(month));
        todayButton.setVisibility(DateUtils.isSameMonth(month, DateUtils.today())
                ? INVISIBLE : VISIBLE);
        renderCalendar(true);
        renderSummary();
        renderDay(true);
    }

    private void goToday() {
        Calendar today = DateUtils.today();
        month.setTimeInMillis(DateUtils.startOfMonth(today).getTimeInMillis());
        selected = today;
        monthLabel.setText(DateUtils.monthLabel(month));
        todayButton.setVisibility(INVISIBLE);
        renderCalendar(true);
        renderSummary();
        renderDay(true);
    }

    private void selectDate(Calendar date) {
        selected = (Calendar) date.clone();
        renderCalendar(false);
        renderDay(true);
    }

    private void renderCalendar(boolean animate) {
        Context context = getContext();
        calendarGrid.removeAllViews();
        Map<String, Long> daily = db.dailyTotalsForMonth(DateUtils.monthKey(month));
        LedgerPrefs.WarningSettings warnings = LedgerPrefs.warnings(context);
        int offset = DateUtils.firstDayOffset(month);
        int days = DateUtils.daysInMonth(month);
        int rows = (offset + days + 6) / 7;
        int day = 1;
        String todayKey = DateUtils.todayKey();
        for (int rowIndex = 0; rowIndex < rows; rowIndex++) {
            LinearLayout row = Ui.horizontal(context);
            for (int col = 0; col < 7; col++) {
                FrameLayout cell = new FrameLayout(context);
                row.addView(cell, new LinearLayout.LayoutParams(
                        0, Ui.dp(context, 50), 1f));
                int index = rowIndex * 7 + col;
                if (index < offset || day > days) {
                    continue;
                }
                Calendar date = (Calendar) month.clone();
                date.set(Calendar.DAY_OF_MONTH, day);
                String key = DateUtils.key(date);
                int dayNumber = day;
                day++;

                boolean isSelected = DateUtils.key(selected).equals(key);
                boolean isToday = key.equals(todayKey);
                Long totalValue = daily.get(key);
                long total = totalValue == null ? 0L : totalValue;
                WarningLevel level = WarningLevel.of(total, warnings.dayOrange,
                        warnings.dayRed);

                if (isSelected) {
                    cell.setBackground(Ui.roundRect(context,
                            Ui.alpha(Ui.color(context, R.color.accent), 0.09f), 13));
                }
                LinearLayout inner = Ui.vertical(context);
                inner.setGravity(Gravity.CENTER);
                cell.addView(inner, new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT));
                TextView number = Ui.text(context, String.valueOf(dayNumber), 13,
                        Ui.color(context, R.color.text_primary), false);
                number.setGravity(Gravity.CENTER);
                number.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
                number.setLayoutParams(new LinearLayout.LayoutParams(
                        Ui.dp(context, 31), Ui.dp(context, 31)));
                if (isSelected) {
                    number.setTextColor(Ui.color(context, R.color.accent_contrast));
                    number.setBackground(Ui.circle(Ui.color(context, R.color.accent)));
                } else if (isToday) {
                    number.setTextColor(Ui.color(context, R.color.accent));
                    number.setBackground(Ui.roundRect(context, android.graphics.Color.TRANSPARENT,
                            16, 1, Ui.alpha(Ui.color(context, R.color.accent), 0.55f)));
                }
                inner.addView(number);

                FrameLayout squareHolder = new FrameLayout(context);
                squareHolder.setLayoutParams(new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(context, 9)));
                if (total > 0) {
                    View square = new View(context);
                    square.setBackground(Ui.roundRect(context, level.color(context), 1.8f));
                    FrameLayout.LayoutParams squareParams = new FrameLayout.LayoutParams(
                            Ui.dp(context, 6), Ui.dp(context, 6));
                    squareParams.gravity = Gravity.CENTER;
                    squareHolder.addView(square, squareParams);
                    square.setAlpha(isSelected ? 1f : 0.88f);
                }
                inner.addView(squareHolder);

                StringBuilder description = new StringBuilder();
                description.append(dayNumber).append("日");
                if (total > 0) {
                    description.append("，支出").append(Money.yuan(total))
                            .append("，").append(level.title);
                }
                cell.setContentDescription(description.toString());
                cell.setClickable(true);
                cell.setFocusable(true);
                cell.setOnClickListener(v -> selectDate(date));
            }
            calendarGrid.addView(row);
        }
        if (animate) {
            calendarGrid.setAlpha(0f);
            calendarGrid.setTranslationY(Ui.dp(context, 6));
            calendarGrid.animate().alpha(1f).translationY(0f)
                    .setDuration(210).start();
        } else {
            calendarGrid.setAlpha(1f);
            calendarGrid.setTranslationY(0f);
        }
    }

    private void renderSummary() {
        Context context = getContext();
        String start = DateUtils.key(DateUtils.startOfMonth(month));
        String end = DateUtils.key(DateUtils.endOfMonth(month));
        long total = db.totalBetween(start, end);
        int count = db.countBetween(start, end);
        summaryTitle.setText(DateUtils.monthLabel(month) + "支出");
        summaryAmount.setText(Money.yuan(total));
        summaryMeta.setText(count == 0 ? "这个月还没有记录" : "共 " + count + " 笔记录");
        LedgerPrefs.WarningSettings warnings = LedgerPrefs.warnings(context);
        WarningLevel level = WarningLevel.of(total, warnings.monthOrange, warnings.monthRed);
        styleBadge(summaryBadge, level, "月度 " + level.title);
    }

    private void renderDay(boolean animate) {
        Context context = getContext();
        dayDetail.animate().cancel();
        if (animate) {
            dayDetail.animate().alpha(0f).translationY(Ui.dp(context, 5))
                    .setDuration(70).withEndAction(() -> {
                        buildDayDetail();
                        dayDetail.setAlpha(0f);
                        dayDetail.setTranslationY(Ui.dp(context, 5));
                        dayDetail.animate().alpha(1f).translationY(0f)
                                .setDuration(160).start();
                    }).start();
        } else {
            buildDayDetail();
            dayDetail.setAlpha(1f);
            dayDetail.setTranslationY(0f);
        }
    }

    private void buildDayDetail() {
        Context context = getContext();
        dayDetail.removeAllViews();
        String key = DateUtils.key(selected);
        List<Entry> entries = db.entriesForDate(key);
        long total = 0;
        for (Entry entry : entries) {
            total += entry.amountCents;
        }
        dayTitle.setText(DateUtils.dayLabel(key));
        dayMeta.setText(entries.isEmpty() ? "" : "共 " + entries.size() + " 笔");
        dayMeta.setVisibility(entries.isEmpty() ? GONE : VISIBLE);
        LedgerPrefs.WarningSettings warnings = LedgerPrefs.warnings(context);
        WarningLevel level = WarningLevel.of(total, warnings.dayOrange, warnings.dayRed);
        if (entries.isEmpty()) {
            dayBadge.setVisibility(GONE);
        } else {
            dayBadge.setVisibility(VISIBLE);
            styleBadge(dayBadge, level, Money.yuan(total));
        }

        if (entries.isEmpty()) {
            MaterialCardView empty = Ui.card(context, 18, true, false);
            LinearLayout emptyInner = Ui.horizontal(context);
            emptyInner.setGravity(Gravity.CENTER_VERTICAL);
            emptyInner.setPadding(Ui.dp(context, 15), Ui.dp(context, 15),
                    Ui.dp(context, 15), Ui.dp(context, 15));
            emptyInner.addView(Ui.iconCircle(context, R.drawable.ic_empty_receipt,
                    Ui.color(context, R.color.text_tertiary), 36, 18));
            TextView text = Ui.text(context, "这一天没有记账", 12,
                    Ui.color(context, R.color.text_secondary), false);
            LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            textParams.leftMargin = Ui.dp(context, 10);
            emptyInner.addView(text, textParams);
            empty.addView(emptyInner);
            dayDetail.addView(empty);
            return;
        }

        MaterialCardView card = Ui.card(context, 20, true, false);
        LinearLayout list = Ui.vertical(context);
        for (int i = 0; i < entries.size(); i++) {
            Entry entry = entries.get(i);
            LinearLayout row = Ui.horizontal(context);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(Ui.dp(context, 14), Ui.dp(context, 11),
                    Ui.dp(context, 14), Ui.dp(context, 11));
            row.setClickable(true);
            row.setFocusable(true);
            row.setBackground(Ui.drawable(context, R.drawable.ripple_round));
            row.addView(Ui.iconCircle(context, entry.category().iconRes,
                    entry.category().color(context), 38, 20));
            LinearLayout texts = Ui.vertical(context);
            LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            textParams.leftMargin = Ui.dp(context, 11);
            TextView item = Ui.text(context, entry.item, 14,
                    Ui.color(context, R.color.text_primary), true);
            item.setMaxLines(1);
            item.setEllipsize(android.text.TextUtils.TruncateAt.END);
            TextView meta = Ui.text(context, entry.category().title + " · "
                            + entry.displaySubcategory(),
                    10, Ui.color(context, R.color.text_secondary), false);
            LinearLayout.LayoutParams metaParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            metaParams.topMargin = Ui.dp(context, 4);
            texts.addView(item);
            texts.addView(meta, metaParams);
            row.addView(texts, textParams);
            TextView amount = Ui.text(context, Money.yuan(entry.amountCents), 15,
                    Ui.color(context, R.color.text_primary), true);
            row.addView(amount);
            row.setOnClickListener(v -> navigator.openEntryForm(entry.categoryKey,
                    entry.id, entry.date));
            list.addView(row);
            if (i < entries.size() - 1) {
                View divider = Ui.divider(context);
                LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, Ui.dp(context, 1)));
                dividerParams.leftMargin = Ui.dp(context, 63);
                list.addView(divider, dividerParams);
            }
        }
        card.addView(list);
        dayDetail.addView(card);
    }

    private void renderOverview() {
        Context context = getContext();
        overviewRow.removeAllViews();
        DateRange monthRange = DateRange.month();
        DateRange weekRange = DateRange.week();
        long monthTotal = db.totalBetween(monthRange.start, monthRange.end);
        long weekTotal = db.totalBetween(weekRange.start, weekRange.end);
        Map<Category, Long> monthCategories = db.categoryTotals(monthRange.start,
                monthRange.end);
        Map<Category, Long> weekCategories = db.categoryTotals(weekRange.start,
                weekRange.end);

        View monthCard = overviewCard(context, "本月", monthTotal, monthCategories,
                "本月支出", monthRange);
        View weekCard = overviewCard(context, "本周", weekTotal, weekCategories,
                "本周支出", weekRange);
        LinearLayout.LayoutParams leftParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        leftParams.rightMargin = Ui.dp(context, 5);
        overviewRow.addView(monthCard, leftParams);
        LinearLayout.LayoutParams rightParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        rightParams.leftMargin = Ui.dp(context, 5);
        overviewRow.addView(weekCard, rightParams);
    }

    private View overviewCard(Context context, String label, long total,
                              Map<Category, Long> categories, String breakdownTitle,
                              DateRange range) {
        MaterialCardView card = Ui.card(context, 18, true, true);
        LinearLayout content = Ui.vertical(context);
        content.setPadding(Ui.dp(context, 13), Ui.dp(context, 13),
                Ui.dp(context, 13), Ui.dp(context, 12));
        LinearLayout header = Ui.horizontal(context);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.addView(Ui.text(context, label, 11,
                Ui.color(context, R.color.text_secondary), true),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        header.addView(Ui.icon(context, R.drawable.ic_chevron_right, 14,
                Ui.color(context, R.color.text_tertiary)));
        content.addView(header);
        TextView amount = Ui.text(context, Money.yuan(total), 19,
                Ui.color(context, R.color.text_primary), true);
        LinearLayout.LayoutParams amountParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        amountParams.topMargin = Ui.dp(context, 4);
        amountParams.bottomMargin = Ui.dp(context, 8);
        content.addView(amount, amountParams);
        View divider = Ui.divider(context);
        content.addView(divider, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, Ui.dp(context, 1))));
        for (Category category : Category.values()) {
            LinearLayout row = Ui.horizontal(context);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, Ui.dp(context, 4), 0, Ui.dp(context, 4));
            View dot = new View(context);
            dot.setBackground(Ui.circle(Ui.alpha(category.color(context), 0.9f)));
            LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams(
                    Ui.dp(context, 5), Ui.dp(context, 5));
            dotParams.rightMargin = Ui.dp(context, 5);
            row.addView(dot, dotParams);
            row.addView(Ui.text(context, category.title, 9,
                    Ui.color(context, R.color.text_secondary), false),
                    new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            long value = categories.get(category) == null ? 0L : categories.get(category);
            row.addView(Ui.text(context, value == 0 ? "—" : Money.grouped(value), 9,
                    Ui.color(context, value == 0 ? R.color.text_tertiary : R.color.text_primary),
                    value > 0));
            content.addView(row);
        }
        card.addView(content);
        card.setOnClickListener(v -> navigator.openBreakdown(breakdownTitle,
                range.start, range.end));
        return card;
    }

    private void renderRemaining() {
        Context context = getContext();
        remainingHost.removeAllViews();
        RemainingMode mode = LedgerPrefs.remainingMode(context);
        boolean week = mode == RemainingMode.WEEK;
        DateRange range = week ? DateRange.week() : DateRange.month();
        long spent = db.totalBetween(range.start, range.end);
        long budget = LedgerPrefs.budget(context, week);
        long remaining = budget - spent;
        LedgerPrefs.WarningSettings warnings = LedgerPrefs.warnings(context);
        WarningLevel level = WarningLevel.of(spent,
                week ? warnings.weekOrange : warnings.monthOrange,
                week ? warnings.weekRed : warnings.monthRed);
        int levelColor = level.color(context);

        MaterialCardView card = Ui.card(context, 20, false, false);
        card.setCardBackgroundColor(Ui.color(context, R.color.accent_soft));
        LinearLayout content = Ui.vertical(context);
        content.setPadding(Ui.dp(context, 17), Ui.dp(context, 14),
                Ui.dp(context, 17), Ui.dp(context, 15));
        LinearLayout header = Ui.horizontal(context);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.addView(Ui.iconCircle(context, R.drawable.ic_wallet,
                Ui.color(context, R.color.accent), 36, 18));
        LinearLayout titleTexts = Ui.vertical(context);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        titleParams.leftMargin = Ui.dp(context, 10);
        titleTexts.addView(Ui.text(context, mode.label + "剩余", 12,
                Ui.color(context, R.color.text_secondary), true));
        titleTexts.addView(Ui.text(context,
                "额度 " + Money.yuan(budget), 9,
                Ui.color(context, R.color.text_tertiary), false));
        header.addView(titleTexts, titleParams);

        LinearLayout badges = Ui.horizontal(context);
        badges.setGravity(Gravity.CENTER_VERTICAL);
        badges.addView(levelChip(context, level));
        TextView adjust = Ui.text(context, "调整", 10,
                Ui.color(context, R.color.accent), true);
        adjust.setPadding(Ui.dp(context, 8), Ui.dp(context, 5),
                Ui.dp(context, 8), Ui.dp(context, 5));
        LinearLayout.LayoutParams adjustParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        adjustParams.leftMargin = Ui.dp(context, 5);
        badges.addView(adjust, adjustParams);
        adjust.setOnClickListener(v -> navigator.openTab(2));
        header.addView(badges);
        content.addView(header);

        TextView remainingValue = Ui.text(context, Money.yuan(remaining), 27,
                remaining < 0 ? Ui.color(context, R.color.danger)
                        : Ui.color(context, R.color.text_primary), true);
        remainingValue.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        LinearLayout.LayoutParams valueParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        valueParams.topMargin = Ui.dp(context, 8);
        content.addView(remainingValue, valueParams);

        LinearLayout track = Ui.horizontal(context);
        track.setBackground(Ui.roundRect(context,
                Ui.alpha(Ui.color(context, R.color.accent), 0.16f), 4));
        LinearLayout.LayoutParams trackParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(context, 6));
        trackParams.topMargin = Ui.dp(context, 11);
        float fraction = budget <= 0 ? 1f : Math.min(1f, spent / (float) budget);
        if (fraction > 0) {
            View fill = new View(context);
            fill.setBackground(Ui.roundRect(context,
                    remaining < 0 ? Ui.color(context, R.color.danger) : levelColor, 4));
            track.addView(fill, new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.MATCH_PARENT, fraction));
        }
        if (fraction < 1f) {
            View rest = new View(context);
            track.addView(rest, new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.MATCH_PARENT, 1f - fraction));
        }
        content.addView(track, trackParams);
        TextView progressText = Ui.text(context,
                "已支出 " + Money.yuan(spent)
                        + (remaining < 0 ? " · 超出 " + Money.yuan(-remaining) : ""),
                9, Ui.color(context, R.color.text_tertiary), false);
        LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        progressParams.topMargin = Ui.dp(context, 7);
        content.addView(progressText, progressParams);
        card.addView(content);
        remainingHost.addView(card);
    }

    private TextView levelChip(Context context, WarningLevel level) {
        return Ui.softPill(context, level.title, level.color(context));
    }

    private void styleBadge(TextView badge, WarningLevel level, String text) {
        Context context = getContext();
        int color = level.color(context);
        badge.setText(text);
        badge.setTextColor(color);
        badge.setBackground(Ui.roundRect(context, Ui.alpha(color, 0.12f), 20));
    }

    private View legendSquare(Context context, int colorRes) {
        View square = new View(context);
        square.setBackground(Ui.roundRect(context, Ui.color(context, colorRes), 1.8f));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                Ui.dp(context, 6), Ui.dp(context, 6));
        params.leftMargin = Ui.dp(context, 8);
        params.rightMargin = Ui.dp(context, 4);
        square.setLayoutParams(params);
        return square;
    }

    private TextView legendText(Context context, String text) {
        return Ui.text(context, text, 9,
                Ui.color(context, R.color.text_secondary), false);
    }
}
