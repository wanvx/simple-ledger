package com.local.simpleledger.ui;

import android.content.Context;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.android.material.card.MaterialCardView;
import com.local.simpleledger.R;
import com.local.simpleledger.data.Entry;
import com.local.simpleledger.data.LedgerDb;
import com.local.simpleledger.model.Category;
import com.local.simpleledger.util.DateRange;
import com.local.simpleledger.util.DateUtils;
import com.local.simpleledger.util.Money;

import java.util.Calendar;
import java.util.List;

/** Page one: the categorical entry point plus today's running account. */
public final class RecordPage extends LinearLayout {

    private final LedgerDb db;
    private final Navigator navigator;
    private final TextView todayAmount;
    private final TextView todayMeta;
    private final TextView monthPill;
    private final TextView todayCount;
    private final LinearLayout todayList;
    private final LinearLayout categoryList;

    public RecordPage(Context context, LedgerDb db, Navigator navigator) {
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
        headingTexts.addView(Ui.title(context, "记账", 29));
        TextView subtitle = Ui.secondary(context, "今天，也清楚每一笔", 12);
        LinearLayout.LayoutParams subParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        subParams.topMargin = Ui.dp(context, 7);
        headingTexts.addView(subtitle, subParams);
        heading.addView(headingTexts, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView datePill = Ui.chip(context, DateUtils.monthDayLabel(DateUtils.today())
                + " " + DateUtils.weekday(DateUtils.today()),
                Ui.color(context, R.color.accent));
        heading.addView(datePill);
        body.addView(heading);

        MaterialCardView hero = Ui.card(context, 24, false, false);
        hero.setCardBackgroundColor(Ui.color(context, R.color.accent));
        hero.setStrokeWidth(0);
        LinearLayout heroInner = Ui.vertical(context);
        heroInner.setPadding(Ui.dp(context, 19), Ui.dp(context, 17),
                Ui.dp(context, 19), Ui.dp(context, 18));
        int contrast = Ui.color(context, R.color.accent_contrast);
        LinearLayout heroTop = Ui.horizontal(context);
        heroTop.setGravity(Gravity.CENTER_VERTICAL);
        TextView todayLabel = Ui.text(context, "今日支出", 12,
                Ui.alpha(contrast, 0.76f), true);
        heroTop.addView(todayLabel, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        monthPill = Ui.text(context, "", 10, Ui.alpha(contrast, 0.86f), true);
        monthPill.setGravity(Gravity.CENTER);
        monthPill.setPadding(Ui.dp(context, 9), Ui.dp(context, 5),
                Ui.dp(context, 9), Ui.dp(context, 5));
        monthPill.setBackground(Ui.roundRect(context, Ui.alpha(contrast, 0.14f), 9));
        heroTop.addView(monthPill);
        heroInner.addView(heroTop);

        LinearLayout amountRow = Ui.horizontal(context);
        amountRow.setGravity(Gravity.BOTTOM);
        LinearLayout.LayoutParams amountRowParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        amountRowParams.topMargin = Ui.dp(context, 13);
        TextView currency = Ui.text(context, "¥", 20, Ui.alpha(contrast, 0.82f), true);
        LinearLayout.LayoutParams currencyParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        currencyParams.bottomMargin = Ui.dp(context, 4);
        currencyParams.rightMargin = Ui.dp(context, 5);
        amountRow.addView(currency, currencyParams);
        todayAmount = Ui.text(context, "0.00", 35, contrast, true);
        todayAmount.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        amountRow.addView(todayAmount, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        heroInner.addView(amountRow, amountRowParams);
        todayMeta = Ui.text(context, "", 11, Ui.alpha(contrast, 0.72f), false);
        LinearLayout.LayoutParams metaParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        metaParams.topMargin = Ui.dp(context, 8);
        heroInner.addView(todayMeta, metaParams);
        hero.addView(heroInner);
        LinearLayout.LayoutParams heroParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        heroParams.topMargin = Ui.dp(context, 20);
        body.addView(hero, heroParams);

        LinearLayout categoryHeader = Ui.horizontal(context);
        categoryHeader.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams categoryHeaderParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        categoryHeaderParams.topMargin = Ui.dp(context, 26);
        categoryHeaderParams.bottomMargin = Ui.dp(context, 10);
        categoryHeader.addView(Ui.sectionTitle(context, "选择分类"),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        body.addView(categoryHeader, categoryHeaderParams);

        categoryList = Ui.vertical(context);
        body.addView(categoryList);
        buildCategories();

        LinearLayout todayHeader = Ui.horizontal(context);
        todayHeader.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams todayHeaderParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        todayHeaderParams.topMargin = Ui.dp(context, 28);
        todayHeaderParams.bottomMargin = Ui.dp(context, 10);
        todayHeader.addView(Ui.sectionTitle(context, "今日明细"),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        todayCount = Ui.text(context, "", 10,
                Ui.color(context, R.color.text_tertiary), true);
        todayHeader.addView(todayCount);
        body.addView(todayHeader, todayHeaderParams);

        todayList = Ui.vertical(context);
        body.addView(todayList);

        refresh();
    }

    private void buildCategories() {
        Context context = getContext();
        Category[] categories = Category.values();
        for (int i = 0; i < categories.length; i++) {
            Category category = categories[i];
            MaterialCardView card = Ui.card(context, 18, true, true);
            LinearLayout row = Ui.horizontal(context);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(Ui.dp(context, 14), Ui.dp(context, 12),
                    Ui.dp(context, 12), Ui.dp(context, 12));
            row.addView(Ui.iconCircle(context, category.iconRes,
                    category.color(context), 45, 23));
            LinearLayout texts = Ui.vertical(context);
            LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            textParams.leftMargin = Ui.dp(context, 13);
            TextView title = Ui.text(context, category.title, 16,
                    Ui.color(context, R.color.text_primary), true);
            TextView preview = Ui.text(context, category.subcategoryPreview(), 10,
                    Ui.color(context, R.color.text_tertiary), false);
            preview.setMaxLines(1);
            preview.setEllipsize(android.text.TextUtils.TruncateAt.END);
            LinearLayout.LayoutParams previewParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            previewParams.topMargin = Ui.dp(context, 5);
            texts.addView(title);
            texts.addView(preview, previewParams);
            row.addView(texts, textParams);
            row.addView(Ui.icon(context, R.drawable.ic_chevron_right, 17,
                    Ui.color(context, R.color.text_tertiary)));
            card.addView(row);
            card.setOnClickListener(v -> {
                v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                navigator.openEntryForm(category.key, -1, DateUtils.todayKey());
            });
            Ui.pressScale(card);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            if (i > 0) {
                params.topMargin = Ui.dp(context, 9);
            }
            categoryList.addView(card, params);

            card.setAlpha(0f);
            card.setTranslationY(Ui.dp(context, 10));
            card.animate().alpha(1f).translationY(0f).setStartDelay(50L + i * 36L)
                    .setDuration(260).start();
        }
    }

    public void refresh() {
        Context context = getContext();
        String today = DateUtils.todayKey();
        long todayTotal = db.totalBetween(today, today);
        List<Entry> entries = db.entriesForDate(today);
        todayAmount.setText(Money.grouped(todayTotal));
        Calendar now = DateUtils.today();
        todayMeta.setText(entries.isEmpty()
                ? "还没有支出 · " + DateUtils.fullDayLabel(today)
                : "共 " + entries.size() + " 笔 · " + DateUtils.fullDayLabel(today));
        long monthTotal = db.totalBetween(DateRange.monthToDate().start, today);
        monthPill.setText("本月 " + Money.yuan(monthTotal));
        todayCount.setText(entries.isEmpty() ? "暂无记录" : entries.size() + " 笔");
        renderToday(entries);
    }

    private void renderToday(List<Entry> entries) {
        Context context = getContext();
        todayList.removeAllViews();
        if (entries.isEmpty()) {
            MaterialCardView empty = Ui.card(context, 20, true, false);
            LinearLayout emptyInner = Ui.vertical(context);
            emptyInner.setGravity(Gravity.CENTER);
            emptyInner.setPadding(Ui.dp(context, 16), Ui.dp(context, 24),
                    Ui.dp(context, 16), Ui.dp(context, 24));
            emptyInner.addView(Ui.iconCircle(context, R.drawable.ic_empty_receipt,
                    Ui.color(context, R.color.text_tertiary), 45, 22));
            TextView message = Ui.text(context, "今天还没有记账", 13,
                    Ui.color(context, R.color.text_secondary), true);
            LinearLayout.LayoutParams messageParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            messageParams.topMargin = Ui.dp(context, 10);
            emptyInner.addView(message, messageParams);
            empty.addView(emptyInner);
            todayList.addView(empty);
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
            TextView amount = Ui.text(context, Money.yuan(entry.amountCents), 16,
                    Ui.color(context, R.color.text_primary), true);
            amount.setGravity(Gravity.END);
            TextView time = Ui.text(context, DateUtils.timeLabel(entry.createdAt), 9,
                    Ui.color(context, R.color.text_tertiary), false);
            LinearLayout amountColumn = Ui.vertical(context);
            amountColumn.setGravity(Gravity.END);
            amountColumn.addView(amount, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            LinearLayout.LayoutParams timeParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            timeParams.topMargin = Ui.dp(context, 3);
            timeParams.gravity = Gravity.END;
            amountColumn.addView(time, timeParams);
            row.addView(amountColumn);
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
        todayList.addView(card);
    }
}
