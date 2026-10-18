package com.local.simpleledger.ui;

import android.app.DatePickerDialog;
import android.content.ActivityNotFoundException;
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
import com.local.simpleledger.util.Exporter;
import com.local.simpleledger.util.Money;

import java.io.File;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Export flow: choose a period, preview, then share a PNG or CSV statement. */
public final class ExportPage extends LinearLayout {

    private static final String[] PRESETS = {"本月", "本周", "本日", "近七天"};

    private final LedgerDb db;
    private final Navigator navigator;
    private final TextView[] quickChips = new TextView[PRESETS.length];
    private TextView startValue;
    private TextView endValue;
    private final TextView rangeTitle;
    private final TextView totalValue;
    private final TextView metaValue;
    private final LinearLayout preview;
    private String start;
    private String end;
    private int presetIndex = 0;
    private boolean exporting;

    public ExportPage(Context context, LedgerDb db, Navigator navigator) {
        super(context);
        this.db = db;
        this.navigator = navigator;
        DateRange initial = DateRange.month();
        start = initial.start;
        end = initial.end;

        setOrientation(VERTICAL);
        setBackgroundColor(Ui.color(context, R.color.bg));
        addView(Ui.toolbar(context, "导出账单", navigator::popPage));

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
        rangeInner.addView(Ui.label(context, "导出时间段", 10));
        LinearLayout quickRow = Ui.horizontal(context);
        quickRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams quickParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        quickParams.topMargin = Ui.dp(context, 9);
        for (int i = 0; i < PRESETS.length; i++) {
            final int index = i;
            TextView chip = Ui.text(context, PRESETS[i], 11,
                    Ui.color(context, R.color.text_secondary), true);
            chip.setGravity(Gravity.CENTER);
            chip.setPadding(Ui.dp(context, 8), Ui.dp(context, 7),
                    Ui.dp(context, 8), Ui.dp(context, 7));
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
        rangeCard.addView(rangeInner);
        body.addView(rangeCard, Ui.lpMatchWrap());

        MaterialCardView summary = Ui.card(context, 22, false, false);
        summary.setCardBackgroundColor(Ui.color(context, R.color.accent_soft));
        LinearLayout summaryInner = Ui.vertical(context);
        summaryInner.setPadding(Ui.dp(context, 17), Ui.dp(context, 14),
                Ui.dp(context, 17), Ui.dp(context, 15));
        rangeTitle = Ui.text(context, "", 11,
                Ui.color(context, R.color.text_secondary), true);
        summaryInner.addView(rangeTitle);
        totalValue = Ui.text(context, "¥0.00", 30,
                Ui.color(context, R.color.text_primary), true);
        totalValue.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        LinearLayout.LayoutParams totalParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        totalParams.topMargin = Ui.dp(context, 7);
        summaryInner.addView(totalValue, totalParams);
        metaValue = Ui.text(context, "", 10,
                Ui.color(context, R.color.text_tertiary), false);
        LinearLayout.LayoutParams metaParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        metaParams.topMargin = Ui.dp(context, 5);
        summaryInner.addView(metaValue, metaParams);
        summary.addView(summaryInner);
        LinearLayout.LayoutParams summaryParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        summaryParams.topMargin = Ui.dp(context, 12);
        body.addView(summary, summaryParams);

        LinearLayout previewHeader = Ui.horizontal(context);
        previewHeader.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams previewHeaderParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        previewHeaderParams.topMargin = Ui.dp(context, 24);
        previewHeaderParams.bottomMargin = Ui.dp(context, 9);
        previewHeader.addView(Ui.sectionTitle(context, "内容预览"),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        body.addView(previewHeader, previewHeaderParams);
        preview = Ui.vertical(context);
        body.addView(preview);

        LinearLayout formatHeader = Ui.horizontal(context);
        formatHeader.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams formatHeaderParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        formatHeaderParams.topMargin = Ui.dp(context, 24);
        formatHeaderParams.bottomMargin = Ui.dp(context, 9);
        formatHeader.addView(Ui.sectionTitle(context, "选择格式"),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        body.addView(formatHeader, formatHeaderParams);

        LinearLayout formats = Ui.horizontal(context);
        formats.setGravity(Gravity.TOP);
        View imageCard = formatCard(context, R.drawable.ic_image, "图片",
                "PNG", true);
        imageCard.setOnClickListener(v -> export(true));
        View csvCard = formatCard(context, R.drawable.ic_document, "文档",
                "CSV", false);
        csvCard.setOnClickListener(v -> export(false));
        LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        imageParams.rightMargin = Ui.dp(context, 5);
        formats.addView(imageCard, imageParams);
        LinearLayout.LayoutParams csvParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        csvParams.leftMargin = Ui.dp(context, 5);
        formats.addView(csvCard, csvParams);
        body.addView(formats, Ui.lpMatchWrap());

        updateDates();
        updateQuickStyles();
        renderPreview();
    }

    private MaterialCardView dateCard(Context context, String label, boolean isStart) {
        MaterialCardView card = Ui.card(context, 13, true, true);
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
        card.setContentDescription(isStart ? "选择导出开始日期" : "选择导出结束日期");
        if (isStart) {
            startValue = value;
        } else {
            endValue = value;
        }
        return card;
    }

    private View formatCard(Context context, int icon, String title, String subtitle,
                            boolean filled) {
        MaterialCardView card = Ui.card(context, 17, !filled, true);
        if (filled) {
            card.setCardBackgroundColor(Ui.color(context, R.color.accent_soft));
        }
        LinearLayout content = Ui.vertical(context);
        content.setPadding(Ui.dp(context, 15), Ui.dp(context, 15),
                Ui.dp(context, 15), Ui.dp(context, 14));
        content.addView(Ui.iconCircle(context, icon, Ui.color(context, R.color.accent),
                40, 20));
        TextView titleView = Ui.text(context, title, 16,
                Ui.color(context, R.color.text_primary), true);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        titleParams.topMargin = Ui.dp(context, 10);
        content.addView(titleView, titleParams);
        TextView subtitleView = Ui.text(context, subtitle, 9,
                Ui.color(context, R.color.text_tertiary), false);
        LinearLayout.LayoutParams subtitleParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        subtitleParams.topMargin = Ui.dp(context, 4);
        content.addView(subtitleView, subtitleParams);
        card.addView(content);
        Ui.pressScale(card);
        return card;
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
        renderPreview();
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
            renderPreview();
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

    private void renderPreview() {
        Context context = getContext();
        preview.removeAllViews();
        long total = db.totalBetween(start, end);
        int count = db.countBetween(start, end);
        rangeTitle.setText(DateRange.rangeLabel(start, end));
        totalValue.setText(Money.yuan(total));
        metaValue.setText(count == 0
                ? "没有可导出的支出"
                : "共 " + count + " 笔");
        List<SubcategoryTotal> rows = db.subcategoryTotals(start, end);
        if (rows.isEmpty()) {
            MaterialCardView empty = Ui.card(context, 18, true, false);
            LinearLayout emptyInner = Ui.horizontal(context);
            emptyInner.setGravity(Gravity.CENTER_VERTICAL);
            emptyInner.setPadding(Ui.dp(context, 15), Ui.dp(context, 17),
                    Ui.dp(context, 15), Ui.dp(context, 17));
            emptyInner.addView(Ui.iconCircle(context, R.drawable.ic_export,
                    Ui.color(context, R.color.text_tertiary), 36, 18));
            TextView text = Ui.text(context, "没有可导出的内容，换个时间段试试", 11,
                    Ui.color(context, R.color.text_secondary), false);
            LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            textParams.leftMargin = Ui.dp(context, 10);
            emptyInner.addView(text, textParams);
            empty.addView(emptyInner);
            preview.addView(empty);
            return;
        }

        Map<Category, List<SubcategoryTotal>> grouped = new LinkedHashMap<>();
        for (SubcategoryTotal row : rows) {
            List<SubcategoryTotal> list = grouped.get(row.category);
            if (list == null) {
                list = new java.util.ArrayList<>();
                grouped.put(row.category, list);
            }
            list.add(row);
        }
        MaterialCardView card = Ui.card(context, 18, true, false);
        LinearLayout list = Ui.vertical(context);
        list.setPadding(Ui.dp(context, 14), Ui.dp(context, 6),
                Ui.dp(context, 14), Ui.dp(context, 8));
        boolean firstGroup = true;
        for (Map.Entry<Category, List<SubcategoryTotal>> entry : grouped.entrySet()) {
            Category category = entry.getKey();
            long categoryTotal = 0;
            for (SubcategoryTotal row : entry.getValue()) {
                categoryTotal += row.amountCents;
            }
            if (!firstGroup) {
                View divider = Ui.divider(context);
                LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, Ui.dp(context, 1)));
                dividerParams.topMargin = Ui.dp(context, 6);
                dividerParams.bottomMargin = Ui.dp(context, 2);
                list.addView(divider, dividerParams);
            }
            firstGroup = false;
            LinearLayout header = Ui.horizontal(context);
            header.setGravity(Gravity.CENTER_VERTICAL);
            header.setPadding(0, Ui.dp(context, 7), 0, Ui.dp(context, 3));
            View dot = new View(context);
            dot.setBackground(Ui.circle(category.color(context)));
            LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams(
                    Ui.dp(context, 6), Ui.dp(context, 6));
            dotParams.rightMargin = Ui.dp(context, 6);
            header.addView(dot, dotParams);
            header.addView(Ui.text(context, category.title, 12,
                    Ui.color(context, R.color.text_primary), true),
                    new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            header.addView(Ui.text(context, Money.yuan(categoryTotal), 12,
                    Ui.color(context, R.color.text_secondary), true));
            list.addView(header);
            for (SubcategoryTotal row : entry.getValue()) {
                LinearLayout subRow = Ui.horizontal(context);
                subRow.setGravity(Gravity.CENTER_VERTICAL);
                subRow.setPadding(Ui.dp(context, 12), Ui.dp(context, 5),
                        0, Ui.dp(context, 5));
                subRow.addView(Ui.text(context, row.subcategory, 11,
                        Ui.color(context, R.color.text_secondary), false),
                        new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
                subRow.addView(Ui.text(context, Money.yuan(row.amountCents), 12,
                        Ui.color(context, R.color.text_primary), true));
                list.addView(subRow);
            }
        }
        card.addView(list);
        preview.addView(card);
    }

    private void export(boolean image) {
        if (exporting) {
            return;
        }
        Context context = getContext();
        long total = db.totalBetween(start, end);
        List<SubcategoryTotal> rows = db.subcategoryTotals(start, end);
        if (total <= 0 || rows.isEmpty()) {
            Ui.toast(context, "这个时间段没有可导出的支出");
            return;
        }
        exporting = true;
        try {
            DateRange range = DateRange.custom(start, end);
            File file = image
                    ? Exporter.exportImage(context, range, rows, total)
                    : Exporter.exportCsv(context, range, rows, total);
            Exporter.share(context, file, image ? "image/png" : "text/csv",
                    "简单记账账单");
            Ui.toast(context, image ? "图片已生成" : "文档已生成");
        } catch (ActivityNotFoundException e) {
            Ui.toast(context, "没有可用的分享应用");
        } catch (Exception e) {
            Ui.toast(context, "导出失败，请重试");
        } finally {
            exporting = false;
        }
    }
}
