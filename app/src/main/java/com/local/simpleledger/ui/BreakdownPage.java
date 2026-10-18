package com.local.simpleledger.ui;

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
import com.local.simpleledger.util.Money;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** A focused subcategory report for one month or week. */
public final class BreakdownPage extends LinearLayout {

    private final LedgerDb db;
    private final Navigator navigator;
    private final String start;
    private final String end;

    public BreakdownPage(Context context, LedgerDb db, Navigator navigator,
                        String title, String start, String end) {
        super(context);
        this.db = db;
        this.navigator = navigator;
        this.start = start;
        this.end = end;
        setOrientation(VERTICAL);
        setBackgroundColor(Ui.color(context, R.color.bg));
        addView(Ui.toolbar(context, title, navigator::popPage));

        ScrollView scroll = new ScrollView(context);
        scroll.setFillViewport(true);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setClipToPadding(false);
        scroll.setOverScrollMode(OVER_SCROLL_NEVER);
        LinearLayout body = Ui.vertical(context);
        body.setPadding(Ui.dp(context, 20), Ui.dp(context, 4),
                Ui.dp(context, 20), Ui.dp(context, 30));
        scroll.addView(body);
        addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        long total = db.totalBetween(start, end);
        int count = db.countBetween(start, end);

        MaterialCardView summary = Ui.card(context, 22, false, false);
        summary.setCardBackgroundColor(Ui.color(context, R.color.accent_soft));
        LinearLayout summaryInner = Ui.vertical(context);
        summaryInner.setPadding(Ui.dp(context, 18), Ui.dp(context, 16),
                Ui.dp(context, 18), Ui.dp(context, 16));
        summaryInner.addView(Ui.text(context, DateRange.rangeLabel(start, end), 10,
                Ui.color(context, R.color.text_secondary), true));
        TextView amount = Ui.text(context, Money.yuan(total), 32,
                Ui.color(context, R.color.text_primary), true);
        amount.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        LinearLayout.LayoutParams amountParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        amountParams.topMargin = Ui.dp(context, 8);
        summaryInner.addView(amount, amountParams);
        summaryInner.addView(Ui.text(context,
                count == 0 ? "这段时间没有支出" : "共 " + count + " 笔",
                10, Ui.color(context, R.color.text_tertiary), false));
        summary.addView(summaryInner);
        body.addView(summary, Ui.lpMatchWrap());

        TextView section = Ui.sectionTitle(context, "分类明细");
        LinearLayout.LayoutParams sectionParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        sectionParams.topMargin = Ui.dp(context, 24);
        sectionParams.bottomMargin = Ui.dp(context, 9);
        body.addView(section, sectionParams);

        List<SubcategoryTotal> rows = db.subcategoryTotals(start, end);
        if (rows.isEmpty()) {
            MaterialCardView empty = Ui.card(context, 18, true, false);
            LinearLayout emptyInner = Ui.vertical(context);
            emptyInner.setGravity(Gravity.CENTER);
            emptyInner.setPadding(Ui.dp(context, 15), Ui.dp(context, 26),
                    Ui.dp(context, 15), Ui.dp(context, 26));
            emptyInner.addView(Ui.iconCircle(context, R.drawable.ic_empty_receipt,
                    Ui.color(context, R.color.text_tertiary), 42, 20));
            TextView text = Ui.text(context, "没有支出记录", 12,
                    Ui.color(context, R.color.text_secondary), true);
            LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            textParams.topMargin = Ui.dp(context, 9);
            emptyInner.addView(text, textParams);
            empty.addView(emptyInner);
            body.addView(empty);
            return;
        }

        Map<Category, List<SubcategoryTotal>> grouped = new LinkedHashMap<>();
        for (SubcategoryTotal row : rows) {
            List<SubcategoryTotal> list = grouped.get(row.category);
            if (list == null) {
                list = new ArrayList<>();
                grouped.put(row.category, list);
            }
            list.add(row);
        }
        boolean first = true;
        for (Map.Entry<Category, List<SubcategoryTotal>> entry : grouped.entrySet()) {
            Category category = entry.getKey();
            long categoryTotal = 0;
            for (SubcategoryTotal row : entry.getValue()) {
                categoryTotal += row.amountCents;
            }
            MaterialCardView card = Ui.card(context, 18, true, false);
            LinearLayout content = Ui.vertical(context);
            content.setPadding(Ui.dp(context, 14), Ui.dp(context, 13),
                    Ui.dp(context, 14), Ui.dp(context, 10));
            LinearLayout header = Ui.horizontal(context);
            header.setGravity(Gravity.CENTER_VERTICAL);
            header.addView(Ui.iconCircle(context, category.iconRes, category.color(context),
                    36, 19));
            LinearLayout titleTexts = Ui.vertical(context);
            LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            titleParams.leftMargin = Ui.dp(context, 10);
            titleTexts.addView(Ui.text(context, category.title, 15,
                    Ui.color(context, R.color.text_primary), true));
            TextView percent = Ui.text(context,
                    total <= 0 ? "" : Math.round(categoryTotal * 100f / total) + "%",
                    9, Ui.color(context, R.color.text_tertiary), false);
            titleTexts.addView(percent);
            header.addView(titleTexts, titleParams);
            header.addView(Ui.text(context, Money.yuan(categoryTotal), 16,
                    Ui.color(context, R.color.text_primary), true));
            content.addView(header);

            LinearLayout progress = Ui.horizontal(context);
            progress.setBackground(Ui.roundRect(context,
                    Ui.alpha(category.color(context), 0.14f), 3));
            LinearLayout.LayoutParams trackParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(context, 5));
            trackParams.topMargin = Ui.dp(context, 10);
            trackParams.bottomMargin = Ui.dp(context, 4);
            float fraction = total <= 0 ? 0f : categoryTotal / (float) total;
            if (fraction > 0) {
                View fill = new View(context);
                fill.setBackground(Ui.roundRect(context, category.color(context), 3));
                progress.addView(fill, new LinearLayout.LayoutParams(
                        0, ViewGroup.LayoutParams.MATCH_PARENT, fraction));
            }
            if (fraction < 1f) {
                View rest = new View(context);
                progress.addView(rest, new LinearLayout.LayoutParams(
                        0, ViewGroup.LayoutParams.MATCH_PARENT, 1f - fraction));
            }
            content.addView(progress, trackParams);

            for (SubcategoryTotal row : entry.getValue()) {
                LinearLayout sub = Ui.horizontal(context);
                sub.setGravity(Gravity.CENTER_VERTICAL);
                sub.setPadding(Ui.dp(context, 4), Ui.dp(context, 7),
                        Ui.dp(context, 2), Ui.dp(context, 7));
                View dot = new View(context);
                dot.setBackground(Ui.circle(Ui.alpha(category.color(context), 0.55f)));
                LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams(
                        Ui.dp(context, 4), Ui.dp(context, 4));
                dotParams.rightMargin = Ui.dp(context, 8);
                sub.addView(dot, dotParams);
                sub.addView(Ui.text(context, row.subcategory, 12,
                        Ui.color(context, R.color.text_secondary), false),
                        new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
                sub.addView(Ui.text(context, Money.yuan(row.amountCents), 13,
                        Ui.color(context, R.color.text_primary), true));
                content.addView(sub);
            }
            card.addView(content);
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            if (!first) {
                cardParams.topMargin = Ui.dp(context, 9);
            }
            first = false;
            body.addView(card, cardParams);
        }
    }
}
