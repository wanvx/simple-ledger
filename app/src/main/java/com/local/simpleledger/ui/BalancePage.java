package com.local.simpleledger.ui;

import android.content.Context;
import android.text.InputType;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.local.simpleledger.R;
import com.local.simpleledger.data.BalanceRecord;
import com.local.simpleledger.data.LedgerDb;
import com.local.simpleledger.util.BalanceSettlement;
import com.local.simpleledger.util.DateRange;
import com.local.simpleledger.util.DateUtils;
import com.local.simpleledger.util.LedgerPrefs;
import com.local.simpleledger.util.Money;

import java.util.List;

/** Page three: a simple savings balance built from each week's leftover. */
public final class BalancePage extends LinearLayout {

    private static final long GREEN_FROM_CENTS = 50000L;

    private final LedgerDb db;
    private final TextView balanceAmount;
    private final TextView balanceHint;
    private final TextView weekRemaining;
    private final TextView weekMeta;
    private final LinearLayout recordsHost;

    public BalancePage(Context context, LedgerDb db) {
        super(context);
        this.db = db;
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
        headingTexts.addView(Ui.title(context, "余额", 29));
        TextView subtitle = Ui.secondary(context, "把每周剩下的钱存起来", 12);
        LinearLayout.LayoutParams subParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        subParams.topMargin = Ui.dp(context, 7);
        headingTexts.addView(subtitle, subParams);
        heading.addView(headingTexts, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        body.addView(heading);

        MaterialCardView balanceCard = Ui.card(context, 22, true, false);
        LinearLayout balanceInner = Ui.vertical(context);
        balanceInner.setPadding(Ui.dp(context, 20), Ui.dp(context, 19),
                Ui.dp(context, 20), Ui.dp(context, 18));

        balanceInner.addView(Ui.label(context, "当前余额", 10));
        balanceAmount = Ui.text(context, "¥0.00", 38,
                Ui.color(context, R.color.text_secondary), true);
        LinearLayout.LayoutParams amountParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        amountParams.topMargin = Ui.dp(context, 7);
        balanceInner.addView(balanceAmount, amountParams);

        balanceHint = Ui.text(context, "", 10,
                Ui.color(context, R.color.text_secondary), false);
        LinearLayout.LayoutParams hintParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hintParams.topMargin = Ui.dp(context, 5);
        balanceInner.addView(balanceHint, hintParams);

        View divider = Ui.divider(context);
        LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, Ui.dp(context, 1)));
        dividerParams.topMargin = Ui.dp(context, 16);
        balanceInner.addView(divider, dividerParams);

        LinearLayout weekRow = Ui.horizontal(context);
        weekRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout weekTexts = Ui.vertical(context);
        weekTexts.addView(Ui.text(context, "本周剩余", 13,
                Ui.color(context, R.color.text_primary), true));
        weekMeta = Ui.text(context, "", 10,
                Ui.color(context, R.color.text_secondary), false);
        LinearLayout.LayoutParams weekMetaParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        weekMetaParams.topMargin = Ui.dp(context, 3);
        weekTexts.addView(weekMeta, weekMetaParams);
        weekRow.addView(weekTexts, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        weekRemaining = Ui.text(context, "¥0.00", 20,
                Ui.color(context, R.color.text_secondary), true);
        weekRow.addView(weekRemaining);
        LinearLayout.LayoutParams weekRowParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        weekRowParams.topMargin = Ui.dp(context, 14);
        balanceInner.addView(weekRow, weekRowParams);

        LinearLayout actions = Ui.horizontal(context);
        LinearLayout.LayoutParams actionsParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(context, 52));
        actionsParams.topMargin = Ui.dp(context, 17);

        MaterialButton deposit = Ui.primaryButton(context, "存入");
        deposit.setTextSize(14);
        deposit.setOnClickListener(v -> openAmountDialog(true));
        actions.addView(deposit, weightedActionParams(context, 0, 4));

        MaterialButton withdraw = Ui.outlineButton(context, "扣款",
                Ui.color(context, R.color.danger), Ui.color(context, R.color.divider));
        withdraw.setTextSize(14);
        withdraw.setOnClickListener(v -> openAmountDialog(false));
        actions.addView(withdraw, weightedActionParams(context, 4, 4));

        MaterialButton adjust = Ui.outlineButton(context, "调整",
                Ui.color(context, R.color.text_secondary), Ui.color(context, R.color.divider));
        adjust.setTextSize(14);
        adjust.setOnClickListener(v -> openAdjustDialog());
        actions.addView(adjust, weightedActionParams(context, 4, 0));
        balanceInner.addView(actions, actionsParams);

        balanceCard.addView(balanceInner);
        LinearLayout.LayoutParams balanceParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        balanceParams.topMargin = Ui.dp(context, 22);
        body.addView(balanceCard, balanceParams);

        addSectionHeader(body, "余额记录", 26);
        MaterialCardView recordsCard = Ui.card(context, 20, true, false);
        recordsHost = Ui.vertical(context);
        recordsCard.addView(recordsHost);
        body.addView(recordsCard);

        TextView rule = Ui.text(context,
                "每周结束后，本周剩余自动计入余额；空周按整周预算计入。"
                        + "存入和扣款只影响余额，不计入日常支出。",
                10, Ui.color(context, R.color.text_tertiary), false);
        LinearLayout.LayoutParams ruleParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ruleParams.topMargin = Ui.dp(context, 14);
        ruleParams.leftMargin = Ui.dp(context, 3);
        ruleParams.rightMargin = Ui.dp(context, 3);
        body.addView(rule, ruleParams);

        refresh();
    }

    private LinearLayout.LayoutParams weightedActionParams(Context context,
                                                           int leftMargin, int rightMargin) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
        params.leftMargin = Ui.dp(context, leftMargin);
        params.rightMargin = Ui.dp(context, rightMargin);
        return params;
    }

    public void refresh() {
        Context context = getContext();
        BalanceSettlement.settleCompletedWeeks(context, db);

        long total = db.balanceTotal();
        balanceAmount.setText(balanceText(total));
        balanceAmount.setTextColor(balanceColor(context, total));
        int settledWeeks = db.balanceSettlementCount();
        String origin = BalanceSettlement.startLabel(context);
        balanceHint.setText(settledWeeks == 0
                ? "自 " + origin + " 起，每周结算"
                : "已累计 " + settledWeeks + " 周 · 自 " + origin + " 起");

        DateRange week = DateRange.week();
        long budget = LedgerPrefs.budget(context, true);
        long spent = db.totalBetween(week.start, week.end);
        long remaining = budget - spent;
        weekRemaining.setText(balanceText(remaining));
        weekRemaining.setTextColor(balanceColor(context, remaining));
        weekMeta.setText("预算 " + Money.yuan(budget) + " · 支出 " + Money.yuan(spent));

        recordsHost.removeAllViews();
        List<BalanceRecord> records = db.balanceRecords();
        if (records.isEmpty()) {
            recordsHost.addView(emptyState(context));
        } else {
            for (int i = 0; i < records.size(); i++) {
                recordsHost.addView(recordRow(context, records.get(i)));
                if (i < records.size() - 1) {
                    recordsHost.addView(indentedDivider(context));
                }
            }
        }
    }

    private int balanceColor(Context context, long cents) {
        if (cents < 0) {
            return Ui.color(context, R.color.danger);
        }
        if (cents > GREEN_FROM_CENTS) {
            return Ui.color(context, R.color.accent);
        }
        return Ui.color(context, R.color.text_secondary);
    }

    private static String balanceText(long cents) {
        return cents < 0 ? "-" + Money.yuan(Math.abs(cents)) : Money.yuan(cents);
    }

    private void openAmountDialog(boolean deposit) {
        Context context = getContext();
        LinearLayout form = Ui.vertical(context);
        form.setPadding(Ui.dp(context, 20), Ui.dp(context, 4),
                Ui.dp(context, 20), 0);

        EditText project = Ui.input(context, deposit ? "例如：奖学金" : "例如：聚餐");
        addField(context, form, "项目", project);

        EditText amount = Ui.input(context, "0.00");
        amount.setInputType(InputType.TYPE_CLASS_NUMBER
                | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        addField(context, form, "金额", amount);

        AlertDialog dialog = new MaterialAlertDialogBuilder(context)
                .setTitle(deposit ? "存入余额" : "从余额扣款")
                .setView(form)
                .setNegativeButton("取消", null)
                .setPositiveButton("保存", null)
                .create();
        dialog.setOnShowListener(ignored -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    .setTextColor(Ui.color(context, R.color.accent));
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                    .setTextColor(Ui.color(context, R.color.text_secondary));
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String name = project.getText().toString().trim();
                if (name.isEmpty()) {
                    Ui.toast(context, "请填写项目");
                    project.requestFocus();
                    return;
                }
                long cents = Money.parseCents(amount.getText().toString());
                if (cents <= 0) {
                    Ui.toast(context, "请输入大于 0 的金额，最多两位小数");
                    amount.requestFocus();
                    return;
                }
                long signed = deposit ? cents : -cents;
                db.addBalanceRecord(deposit ? BalanceRecord.TYPE_DEPOSIT
                                : BalanceRecord.TYPE_WITHDRAW,
                        name, signed, DateUtils.todayKey(), null, null, 0, 0);
                Ui.hideKeyboard(amount);
                dialog.dismiss();
                refresh();
                Ui.toast(context, deposit ? "已存入 " + Money.yuan(cents)
                        : "已扣款 " + Money.yuan(cents));
            });
        });
        dialog.show();
        Ui.showKeyboard(project);
    }

    private void openAdjustDialog() {
        Context context = getContext();
        LinearLayout form = Ui.vertical(context);
        form.setPadding(Ui.dp(context, 20), Ui.dp(context, 4),
                Ui.dp(context, 20), 0);

        EditText note = Ui.input(context, "备注（可选）");
        addField(context, form, "说明", note);

        EditText amount = Ui.input(context, "正数增加，负数减少");
        amount.setInputType(InputType.TYPE_CLASS_NUMBER
                | InputType.TYPE_NUMBER_FLAG_DECIMAL
                | InputType.TYPE_NUMBER_FLAG_SIGNED);
        addField(context, form, "调整金额", amount);

        AlertDialog dialog = new MaterialAlertDialogBuilder(context)
                .setTitle("手动调整余额")
                .setView(form)
                .setNegativeButton("取消", null)
                .setPositiveButton("保存", null)
                .create();
        dialog.setOnShowListener(ignored -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    .setTextColor(Ui.color(context, R.color.accent));
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                    .setTextColor(Ui.color(context, R.color.text_secondary));
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                long signed = parseSignedCents(amount.getText().toString());
                if (signed == 0) {
                    Ui.toast(context, "请输入非 0 金额，最多两位小数");
                    amount.requestFocus();
                    return;
                }
                String label = note.getText().toString().trim();
                if (label.isEmpty()) {
                    label = "手动调整";
                }
                db.addBalanceRecord(BalanceRecord.TYPE_ADJUST, label, signed,
                        DateUtils.todayKey(), null, null, 0, 0);
                Ui.hideKeyboard(amount);
                dialog.dismiss();
                refresh();
                Ui.toast(context, "余额已调整");
            });
        });
        dialog.show();
        Ui.showKeyboard(note);
    }

    private static long parseSignedCents(String raw) {
        if (raw == null) {
            return 0;
        }
        String value = raw.trim();
        boolean negative = value.startsWith("-");
        if (negative || value.startsWith("+")) {
            value = value.substring(1).trim();
        }
        long cents = Money.parseCents(value);
        if (cents <= 0) {
            return 0;
        }
        return negative ? -cents : cents;
    }

    private void addField(Context context, LinearLayout form, String label, EditText input) {
        TextView labelView = Ui.label(context, label, 10);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        labelParams.topMargin = Ui.dp(context, 12);
        form.addView(labelView, labelParams);
        LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        inputParams.topMargin = Ui.dp(context, 6);
        form.addView(input, inputParams);
    }

    private View emptyState(Context context) {
        LinearLayout empty = Ui.vertical(context);
        empty.setGravity(Gravity.CENTER_HORIZONTAL);
        empty.setPadding(Ui.dp(context, 18), Ui.dp(context, 28),
                Ui.dp(context, 18), Ui.dp(context, 28));
        empty.addView(Ui.iconCircle(context, R.drawable.ic_wallet,
                Ui.color(context, R.color.text_tertiary), 48, 24));
        TextView title = Ui.text(context, "还没有余额记录", 13,
                Ui.color(context, R.color.text_primary), true);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        titleParams.topMargin = Ui.dp(context, 10);
        empty.addView(title, titleParams);
        TextView hint = Ui.text(context, "每周结算、存入和扣款会显示在这里", 10,
                Ui.color(context, R.color.text_secondary), false);
        LinearLayout.LayoutParams hintParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hintParams.topMargin = Ui.dp(context, 4);
        empty.addView(hint, hintParams);
        return empty;
    }

    private View recordRow(Context context, BalanceRecord record) {
        LinearLayout row = Ui.horizontal(context);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(Ui.dp(context, 14), Ui.dp(context, 12),
                Ui.dp(context, 14), Ui.dp(context, 12));

        int amountColor = record.amountCents >= 0
                ? Ui.color(context, R.color.accent)
                : Ui.color(context, R.color.danger);
        row.addView(Ui.iconCircle(context, iconFor(record), amountColor, 36, 18));

        LinearLayout texts = Ui.vertical(context);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        textParams.leftMargin = Ui.dp(context, 12);

        TextView title = Ui.text(context, titleFor(record), 13,
                Ui.color(context, R.color.text_primary), true);
        title.setSingleLine(true);
        title.setEllipsize(TextUtils.TruncateAt.END);
        texts.addView(title);

        TextView meta = Ui.text(context, metaFor(record), 10,
                Ui.color(context, R.color.text_secondary), false);
        meta.setMaxLines(2);
        meta.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams metaParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        metaParams.topMargin = Ui.dp(context, 3);
        texts.addView(meta, metaParams);
        row.addView(texts, textParams);

        TextView amount = Ui.text(context, Money.signedYuan(record.amountCents), 14,
                amountColor, true);
        amount.setGravity(Gravity.END);
        row.addView(amount);
        return row;
    }

    private static int iconFor(BalanceRecord record) {
        if (record.isSettlement()) {
            return R.drawable.ic_update;
        }
        if (record.isDeposit()) {
            return R.drawable.ic_plus;
        }
        if (record.isWithdraw()) {
            return R.drawable.ic_minus;
        }
        return R.drawable.ic_edit;
    }

    private static String titleFor(BalanceRecord record) {
        if (record.isSettlement()) {
            return "周结算";
        }
        return record.title.isEmpty() ? "余额调整" : record.title;
    }

    private static String metaFor(BalanceRecord record) {
        if (record.isSettlement()) {
            return DateUtils.shortDate(record.weekStart) + "–"
                    + DateUtils.shortDate(record.weekEnd)
                    + " · 预算 " + Money.yuan(record.budgetCents)
                    + " · 支出 " + Money.yuan(record.spentCents);
        }
        String action = record.isDeposit() ? "存入"
                : record.isWithdraw() ? "扣款" : "调整";
        return action + " · " + DateUtils.shortDate(record.occurredAt);
    }

    private void addSectionHeader(LinearLayout parent, String title, int topMarginDp) {
        Context context = getContext();
        TextView header = Ui.text(context, title, 15,
                Ui.color(context, R.color.text_primary), true);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = Ui.dp(context, topMarginDp);
        params.bottomMargin = Ui.dp(context, 8);
        params.leftMargin = Ui.dp(context, 3);
        parent.addView(header, params);
    }

    private View indentedDivider(Context context) {
        View divider = Ui.divider(context);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, Ui.dp(context, 1)));
        params.leftMargin = Ui.dp(context, 62);
        divider.setLayoutParams(params);
        return divider;
    }
}
