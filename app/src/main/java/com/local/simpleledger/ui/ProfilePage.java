package com.local.simpleledger.ui;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.local.simpleledger.BuildConfig;
import com.local.simpleledger.R;
import com.local.simpleledger.data.LedgerDb;
import com.local.simpleledger.model.RemainingMode;
import com.local.simpleledger.model.WarningLevel;
import com.local.simpleledger.util.DateRange;
import com.local.simpleledger.util.LedgerPrefs;
import com.local.simpleledger.util.Money;
import com.local.simpleledger.util.UpdateManager;

import java.io.File;
import java.util.Locale;

/** Page three: theme, warning thresholds, balances, about and the data reset entry. */
public final class ProfilePage extends LinearLayout {

    /**
     * Pre-filled from the author's existing project. Change this single constant if the
     * profile URL is different.
     */
    private static final String AUTHOR_GITHUB_URL = "https://github.com/wanvx";

    private final LedgerDb db;
    private final Navigator navigator;

    private final TextView totalAmount;
    private final TextView totalCount;
    private final TextView activeDays;
    private final TextView remainingAmount;
    private final TextView remainingMeta;
    private final TextView remainingLevel;
    private final TextView budgetAdjustment;
    private final TextView warningSubtitle;
    private final TextView updateSubtitle;
    private final LinearLayout remainingTrack;
    private final SegmentedControl modeSwitch;
    private SwitchMaterial themeSwitch;
    private boolean checkingUpdate;
    private File pendingInstallFile;

    public ProfilePage(Context context, LedgerDb db, Navigator navigator) {
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
        headingTexts.addView(Ui.title(context, "我的", 29));
        TextView subtitle = Ui.secondary(context, "简单一点，清楚一点", 12);
        LinearLayout.LayoutParams subParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        subParams.topMargin = Ui.dp(context, 7);
        headingTexts.addView(subtitle, subParams);
        heading.addView(headingTexts, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        body.addView(heading);

        MaterialCardView profile = Ui.card(context, 22, true, false);
        LinearLayout profileRow = Ui.horizontal(context);
        profileRow.setGravity(Gravity.CENTER_VERTICAL);
        profileRow.setPadding(Ui.dp(context, 17), Ui.dp(context, 18),
                Ui.dp(context, 17), Ui.dp(context, 18));
        LinearLayout appIcon = Ui.vertical(context);
        appIcon.setGravity(Gravity.CENTER);
        appIcon.setBackground(Ui.circle(Ui.color(context, R.color.accent)));
        ImageView icon = new ImageView(context);
        icon.setImageResource(R.drawable.ic_launcher_foreground);
        icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        appIcon.addView(icon, new LinearLayout.LayoutParams(
                Ui.dp(context, 54), Ui.dp(context, 54)));
        profileRow.addView(appIcon, new LinearLayout.LayoutParams(
                Ui.dp(context, 56), Ui.dp(context, 56)));
        LinearLayout profileTexts = Ui.vertical(context);
        LinearLayout.LayoutParams profileTextParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        profileTextParams.leftMargin = Ui.dp(context, 14);
        profileTexts.addView(Ui.text(context, "简单记账", 20,
                Ui.color(context, R.color.text_primary), true));
        TextView local = Ui.text(context, "本地存储 · 无需联网 · 无广告", 10,
                Ui.color(context, R.color.text_secondary), false);
        LinearLayout.LayoutParams localParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        localParams.topMargin = Ui.dp(context, 5);
        profileTexts.addView(local, localParams);
        profileRow.addView(profileTexts, profileTextParams);
        profileRow.addView(Ui.chip(context, "v" + BuildConfig.VERSION_NAME,
                Ui.color(context, R.color.accent)));
        profile.addView(profileRow);
        LinearLayout.LayoutParams profileParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        profileParams.topMargin = Ui.dp(context, 22);
        body.addView(profile, profileParams);

        MaterialCardView stats = Ui.card(context, 20, true, false);
        LinearLayout statsRow = Ui.horizontal(context);
        statsRow.setPadding(Ui.dp(context, 6), Ui.dp(context, 16),
                Ui.dp(context, 6), Ui.dp(context, 16));
        totalAmount = statColumn(context, statsRow, "累计支出");
        statsRow.addView(statDivider(context));
        totalCount = statColumn(context, statsRow, "总笔数");
        statsRow.addView(statDivider(context));
        activeDays = statColumn(context, statsRow, "记账天数");
        stats.addView(statsRow);
        LinearLayout.LayoutParams statsParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        statsParams.topMargin = Ui.dp(context, 11);
        body.addView(stats, statsParams);

        addSectionHeader(body, "剩余钱数", 25);
        MaterialCardView remaining = Ui.card(context, 22, true, false);
        LinearLayout remainingInner = Ui.vertical(context);
        remainingInner.setPadding(Ui.dp(context, 17), Ui.dp(context, 15),
                Ui.dp(context, 17), Ui.dp(context, 15));
        LinearLayout remainingHeader = Ui.horizontal(context);
        remainingHeader.setGravity(Gravity.CENTER_VERTICAL);
        remainingHeader.addView(Ui.text(context, "本月 / 本周", 12,
                Ui.color(context, R.color.text_secondary), true),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        modeSwitch = new SegmentedControl(context, new String[]{"本月", "本周"});
        modeSwitch.select(LedgerPrefs.remainingMode(context) == RemainingMode.WEEK ? 1 : 0, false);
        modeSwitch.setListener(index -> {
            LedgerPrefs.setRemainingMode(context,
                    index == 1 ? RemainingMode.WEEK : RemainingMode.MONTH);
            refreshRemaining();
            navigator.dataChanged();
        });
        remainingHeader.addView(modeSwitch, new LinearLayout.LayoutParams(
                Ui.dp(context, 132), Ui.dp(context, 40)));
        remainingInner.addView(remainingHeader);

        LinearLayout amountRow = Ui.horizontal(context);
        amountRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams amountRowParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        amountRowParams.topMargin = Ui.dp(context, 12);
        remainingAmount = Ui.text(context, "¥0.00", 31,
                Ui.color(context, R.color.text_primary), true);
        amountRow.addView(remainingAmount, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        remainingLevel = Ui.chip(context, "", Ui.color(context, R.color.warning_green));
        amountRow.addView(remainingLevel);
        remainingInner.addView(amountRow, amountRowParams);

        remainingMeta = Ui.text(context, "", 10,
                Ui.color(context, R.color.text_tertiary), false);
        LinearLayout.LayoutParams metaParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        metaParams.topMargin = Ui.dp(context, 7);
        remainingInner.addView(remainingMeta, metaParams);

        remainingTrack = Ui.horizontal(context);
        remainingTrack.setBackground(Ui.roundRect(context,
                Ui.alpha(Ui.color(context, R.color.accent), 0.14f), 4));
        LinearLayout.LayoutParams trackParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(context, 6));
        trackParams.topMargin = Ui.dp(context, 11);
        remainingInner.addView(remainingTrack, trackParams);

        budgetAdjustment = Ui.text(context, "", 9,
                Ui.color(context, R.color.text_tertiary), false);
        LinearLayout.LayoutParams adjustmentParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        adjustmentParams.topMargin = Ui.dp(context, 4);
        remainingInner.addView(budgetAdjustment, adjustmentParams);

        LinearLayout actions = Ui.horizontal(context);
        actions.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams actionsParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        actionsParams.topMargin = Ui.dp(context, 12);
        com.google.android.material.button.MaterialButton decrease =
                Ui.outlineButton(context, "减少", Ui.color(context, R.color.text_primary),
                        Ui.color(context, R.color.divider));
        decrease.setOnClickListener(v -> showAdjustDialog(false));
        actions.addView(decrease, new LinearLayout.LayoutParams(
                0, Ui.dp(context, 45), 1f));
        com.google.android.material.button.MaterialButton increase =
                Ui.primaryButton(context, "增加");
        increase.setOnClickListener(v -> showAdjustDialog(true));
        LinearLayout.LayoutParams increaseParams = new LinearLayout.LayoutParams(
                0, Ui.dp(context, 45), 1f);
        increaseParams.leftMargin = Ui.dp(context, 6);
        increaseParams.rightMargin = Ui.dp(context, 6);
        actions.addView(increase, increaseParams);
        com.google.android.material.button.MaterialButton reset =
                Ui.textButton(context, "重置", Ui.color(context, R.color.text_secondary));
        reset.setOnClickListener(v -> confirmResetBudget());
        LinearLayout.LayoutParams resetParams = new LinearLayout.LayoutParams(
                0, Ui.dp(context, 45), 0.8f);
        actions.addView(reset, resetParams);
        remainingInner.addView(actions, actionsParams);
        remaining.addView(remainingInner);
        body.addView(remaining, Ui.lpMatchWrap());

        addSectionHeader(body, "设置", 26);
        MaterialCardView settings = Ui.card(context, 20, true, false);
        LinearLayout settingsList = Ui.vertical(context);
        settingsList.addView(themeRow(context));
        settingsList.addView(indentedDivider(context));
        warningSubtitle = Ui.text(context, "", 10,
                Ui.color(context, R.color.text_secondary), false);
        settingsList.addView(warningRow(context));
        settingsList.addView(indentedDivider(context));
        updateSubtitle = Ui.text(context, "当前版本 v" + BuildConfig.VERSION_NAME, 10,
                Ui.color(context, R.color.text_secondary), false);
        settingsList.addView(updateRow(context));
        settingsList.addView(indentedDivider(context));
        settingsList.addView(settingRow(context, R.drawable.ic_info, "软件说明",
                null, false, v -> showAbout(context)));
        settingsList.addView(indentedDivider(context));
        settingsList.addView(settingRow(context, R.drawable.ic_github, "作者 GitHub",
                AUTHOR_GITHUB_URL.replace("https://", ""), true,
                v -> openGithub(context)));
        settings.addView(settingsList);
        body.addView(settings);

        addSectionHeader(body, "数据", 26);
        MaterialCardView dataCard = Ui.card(context, 20, true, false);
        LinearLayout dataList = Ui.vertical(context);
        dataList.addView(settingRow(context, R.drawable.ic_delete, "个人数据清除",
                "删除全部账单，重置额度与预警", false, v -> confirmClear(context), true));
        dataCard.addView(dataList);
        body.addView(dataCard);

        TextView footer = Ui.text(context, "简单记账 v" + BuildConfig.VERSION_NAME, 9,
                Ui.color(context, R.color.text_tertiary), false);
        footer.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams footerParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        footerParams.topMargin = Ui.dp(context, 8);
        body.addView(footer, footerParams);

        refresh();
    }

    public void refresh() {
        Context context = getContext();
        totalAmount.setText(Money.yuan(db.totalAll()));
        totalCount.setText(String.valueOf(db.countAll()));
        activeDays.setText(String.valueOf(db.activeDays()));
        refreshRemaining();
        if (themeSwitch != null) {
            themeSwitch.setOnCheckedChangeListener(null);
            themeSwitch.setChecked(LedgerPrefs.isNight(context));
            themeSwitch.setOnCheckedChangeListener((buttonView, isChecked) ->
                    applyTheme(isChecked));
        }
        LedgerPrefs.WarningSettings warnings = LedgerPrefs.warnings(context);
        warningSubtitle.setText("本日 " + threshold(warnings.dayOrange)
                + "/" + threshold(warnings.dayRed)
                + " · 本周 " + threshold(warnings.weekOrange)
                + "/" + threshold(warnings.weekRed)
                + " · 本月 " + threshold(warnings.monthOrange)
                + "/" + threshold(warnings.monthRed));
    }

    private static String threshold(long cents) {
        return cents % 100 == 0 ? String.valueOf(cents / 100) : Money.plain(cents);
    }

    private void refreshRemaining() {
        Context context = getContext();
        RemainingMode mode = LedgerPrefs.remainingMode(context);
        boolean week = mode == RemainingMode.WEEK;
        modeSwitch.select(week ? 1 : 0, false);
        DateRange range = week ? DateRange.week() : DateRange.month();
        long spent = db.totalBetween(range.start, range.end);
        long budget = LedgerPrefs.budget(context, week);
        long remaining = budget - spent;
        long adjustment = LedgerPrefs.budgetAdjustment(context, week);
        LedgerPrefs.WarningSettings warnings = LedgerPrefs.warnings(context);
        WarningLevel level = WarningLevel.of(spent,
                week ? warnings.weekOrange : warnings.monthOrange,
                week ? warnings.weekRed : warnings.monthRed);

        remainingAmount.setText(Money.yuan(remaining));
        remainingAmount.setTextColor(remaining < 0
                ? Ui.color(context, R.color.danger)
                : Ui.color(context, R.color.text_primary));
        remainingLevel.setText(level.title);
        remainingLevel.setTextColor(level.color(context));
        remainingLevel.setBackground(Ui.roundRect(context,
                Ui.alpha(level.color(context), 0.12f), 20));
        remainingMeta.setText(mode.label + "额度 " + Money.yuan(budget)
                + " · 已支出 " + Money.yuan(spent));
        if (adjustment == 0) {
            budgetAdjustment.setVisibility(GONE);
        } else {
            budgetAdjustment.setVisibility(VISIBLE);
            budgetAdjustment.setText("默认额度 " + Money.yuan(week
                    ? LedgerPrefs.DEFAULT_WEEK_BUDGET : LedgerPrefs.DEFAULT_MONTH_BUDGET)
                    + " · 已调整 " + Money.signedYuan(adjustment));
        }
        remainingTrack.removeAllViews();
        float fraction = budget <= 0 ? 1f : Math.min(1f, spent / (float) budget);
        if (fraction > 0) {
            View fill = new View(context);
            fill.setBackground(Ui.roundRect(context,
                    remaining < 0 ? Ui.color(context, R.color.danger)
                            : level.color(context), 4));
            remainingTrack.addView(fill, new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.MATCH_PARENT, fraction));
        }
        if (fraction < 1f) {
            View rest = new View(context);
            remainingTrack.addView(rest, new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.MATCH_PARENT, 1f - fraction));
        }
    }

    private LinearLayout themeRow(Context context) {
        LinearLayout row = Ui.horizontal(context);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(Ui.dp(context, 14), Ui.dp(context, 12),
                Ui.dp(context, 14), Ui.dp(context, 12));
        row.addView(Ui.iconCircle(context, R.drawable.ic_moon,
                Ui.color(context, R.color.accent), 40, 21));
        LinearLayout texts = Ui.vertical(context);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        textParams.leftMargin = Ui.dp(context, 12);
        texts.addView(Ui.text(context, "夜间模式", 15,
                Ui.color(context, R.color.text_primary), true));
        row.addView(texts, textParams);
        SwitchMaterial toggle = new SwitchMaterial(context);
        toggle.setChecked(LedgerPrefs.isNight(context));
        toggle.setThumbTintList(ColorStateList.valueOf(
                Ui.color(context, R.color.switch_thumb)));
        toggle.setTrackTintList(new ColorStateList(
                new int[][]{new int[]{android.R.attr.state_checked}, new int[]{}},
                new int[]{Ui.color(context, R.color.accent),
                        Ui.color(context, R.color.divider)}));
        toggle.setContentDescription("夜间模式");
        toggle.setOnCheckedChangeListener((buttonView, isChecked) -> applyTheme(isChecked));
        row.addView(toggle);
        row.setOnClickListener(v -> toggle.toggle());
        themeSwitch = toggle;
        return row;
    }

    private void applyTheme(boolean night) {
        if (LedgerPrefs.isNight(getContext()) == night) {
            return;
        }
        LedgerPrefs.setNight(getContext(), night);
        AppCompatDelegate.setDefaultNightMode(night
                ? AppCompatDelegate.MODE_NIGHT_YES
                : AppCompatDelegate.MODE_NIGHT_NO);
    }

    private void showAdjustDialog(boolean increase) {
        Context context = getContext();
        boolean week = LedgerPrefs.remainingMode(context) == RemainingMode.WEEK;
        String period = week ? "本周" : "本月";
        EditText input = Ui.input(context, "0.00");
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER
                | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setText("100.00");
        input.setSelection(input.length());
        LinearLayout wrapper = Ui.vertical(context);
        wrapper.setPadding(Ui.dp(context, 20), Ui.dp(context, 8),
                Ui.dp(context, 20), 0);
        TextView note = Ui.text(context,
                (increase ? "增加的金额会直接加到" : "减少的金额会从") + period
                        + "可用额度中，支出记录不变。", 12,
                Ui.color(context, R.color.text_secondary), false);
        note.setLineSpacing(Ui.dp(context, 3), 1f);
        wrapper.addView(note);
        LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(context, 52));
        inputParams.topMargin = Ui.dp(context, 14);
        wrapper.addView(input, inputParams);
        AlertDialog dialog = new MaterialAlertDialogBuilder(context)
                .setTitle((increase ? "增加" : "减少") + period + "剩余")
                .setView(wrapper)
                .setNegativeButton("取消", null)
                .setPositiveButton(increase ? "增加" : "减少", null)
                .create();
        dialog.setOnShowListener(ignored -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    .setTextColor(Ui.color(context, R.color.accent));
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                    .setTextColor(Ui.color(context, R.color.text_secondary));
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                long cents = Money.parseCents(input.getText().toString());
                if (cents <= 0) {
                    input.setError("请输入大于 0 的金额，最多两位小数");
                    return;
                }
                long before = LedgerPrefs.budget(context, week);
                LedgerPrefs.adjustBudget(context, week, increase ? cents : -cents);
                long after = LedgerPrefs.budget(context, week);
                long changed = after - before;
                if (changed == 0) {
                    Ui.toast(context, "额度已为 0，无法继续减少");
                } else {
                    Ui.toast(context, (changed > 0 ? "已增加 " : "已减少 ")
                            + Money.yuan(Math.abs(changed)));
                }
                dialog.dismiss();
                refreshRemaining();
                navigator.dataChanged();
            });
        });
        dialog.show();
    }

    private void confirmResetBudget() {
        Context context = getContext();
        boolean week = LedgerPrefs.remainingMode(context) == RemainingMode.WEEK;
        String period = week ? "本周" : "本月";
        long budget = week ? LedgerPrefs.DEFAULT_WEEK_BUDGET : LedgerPrefs.DEFAULT_MONTH_BUDGET;
        AlertDialog dialog = new MaterialAlertDialogBuilder(context)
                .setTitle("重置" + period + "额度？")
                .setMessage("将恢复默认额度 " + Money.yuan(budget)
                        + "，已有支出不删除，剩余金额会按新的额度重新计算。")
                .setNegativeButton("取消", null)
                .setPositiveButton("重置", null)
                .create();
        dialog.setOnShowListener(ignored -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    .setTextColor(Ui.color(context, R.color.accent));
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                    .setTextColor(Ui.color(context, R.color.text_secondary));
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                LedgerPrefs.resetBudget(context, week);
                dialog.dismiss();
                refreshRemaining();
                navigator.dataChanged();
                Ui.toast(context, "已恢复默认额度");
            });
        });
        dialog.show();
    }

    private void openGithub(Context context) {
        if (AUTHOR_GITHUB_URL.isEmpty()) {
            Ui.toast(context, "作者 GitHub 待填写");
            return;
        }
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(AUTHOR_GITHUB_URL));
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Ui.toast(context, "没有可打开链接的应用");
        }
    }

    private void showAbout(Context context) {
        String description = "简单记账是一款完全离线的本地记账工具。\n\n"
                + "● 六类支出\n"
                + "饮食、出行、日常、娱乐、学习、其他，每类都有固定小类，日常中包含看病买药。"
                + "每次先选大类，再选小类，填写项目名与金额。\n\n"
                + "● 其他名称\n"
                + "选择「其他」小类时，可以填写一个具体名称，例如话费。"
                + "明细、统计和导出都会使用该名称；留空则显示「其他」。\n\n"
                + "● 记录与统计\n"
                + "月历下方的小方块显示当日预警颜色；点击日期可查看当天每一笔，"
                + "并能直接修改。本月和本周按大类汇总，点开后细化到小类。\n\n"
                + "● 预警与剩余\n"
                + "本日、本周、本月均有绿 / 橙 / 红三阶段预警线，可在「预警设置」中调整。"
                + "剩余 = 额度 - 支出；额度可增加、减少或恢复默认。\n\n"
                + "● 数据安全\n"
                + "所有账单保存在应用私有目录的 SQLite 数据库中，金额以“分”为单位存储，"
                + "不联网、不需要账号。允许系统备份时，数据可随系统备份迁移；"
                + "卸载应用或执行「个人数据清除」会删除账单。\n\n"
                + "● 导出\n"
                + "记录页最下方可按本日、本周、本月或近七天导出，支持图片与 CSV 文档。\n\n"
                + "希望每一笔都花得明白。";
        AlertDialog dialog = new MaterialAlertDialogBuilder(context)
                .setTitle("软件说明")
                .setMessage(description)
                .setPositiveButton("知道了", null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setTextColor(Ui.color(context, R.color.accent)));
        dialog.show();
    }

    private void confirmClear(Context context) {
        AlertDialog dialog = new MaterialAlertDialogBuilder(context)
                .setTitle("清除个人数据？")
                .setMessage("将删除全部账单，并重置剩余额度调整与预警设置。\n"
                        + "主题设置会保留，操作无法撤销。")
                .setNegativeButton("取消", null)
                .setPositiveButton("清除", null)
                .create();
        dialog.setOnShowListener(ignored -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    .setTextColor(Ui.color(context, R.color.danger));
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                    .setTextColor(Ui.color(context, R.color.text_secondary));
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                db.clearAllEntries();
                LedgerPrefs.resetFinancialSettings(context);
                dialog.dismiss();
                refresh();
                navigator.dataChanged();
                Ui.toast(context, "个人数据已清除");
            });
        });
        dialog.show();
    }

    private TextView statColumn(Context context, LinearLayout parent, String label) {
        LinearLayout column = Ui.vertical(context);
        column.setGravity(Gravity.CENTER);
        TextView value = Ui.text(context, "0", 18,
                Ui.color(context, R.color.text_primary), true);
        value.setGravity(Gravity.CENTER);
        value.setSingleLine(true);
        TextView caption = Ui.text(context, label, 9,
                Ui.color(context, R.color.text_secondary), false);
        caption.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams captionParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        captionParams.topMargin = Ui.dp(context, 4);
        column.addView(value);
        column.addView(caption, captionParams);
        parent.addView(column, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        return value;
    }

    private View statDivider(Context context) {
        View view = new View(context);
        view.setBackgroundColor(Ui.color(context, R.color.divider));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                Math.max(1, Ui.dp(context, 1)), Ui.dp(context, 32));
        params.gravity = Gravity.CENTER_VERTICAL;
        view.setLayoutParams(params);
        return view;
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
        params.leftMargin = Ui.dp(context, 64);
        divider.setLayoutParams(params);
        return divider;
    }

    private LinearLayout warningRow(Context context) {
        LinearLayout row = Ui.horizontal(context);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(Ui.dp(context, 14), Ui.dp(context, 12),
                Ui.dp(context, 14), Ui.dp(context, 12));
        row.setClickable(true);
        row.setFocusable(true);
        row.setBackground(Ui.drawable(context, R.drawable.ripple_round));
        row.addView(Ui.iconCircle(context, R.drawable.ic_warning,
                Ui.color(context, R.color.text_secondary), 40, 21));
        LinearLayout texts = Ui.vertical(context);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        textParams.leftMargin = Ui.dp(context, 12);
        texts.addView(Ui.text(context, "预警设置", 14,
                Ui.color(context, R.color.text_primary), true));
        LinearLayout.LayoutParams subtitleParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        subtitleParams.topMargin = Ui.dp(context, 3);
        texts.addView(warningSubtitle, subtitleParams);
        row.addView(texts, textParams);
        row.addView(Ui.icon(context, R.drawable.ic_chevron_right, 16,
                Ui.color(context, R.color.text_tertiary)));
        row.setOnClickListener(v -> navigator.openWarningSettings());
        Ui.pressScale(row);
        return row;
    }

    private LinearLayout updateRow(Context context) {
        LinearLayout row = Ui.horizontal(context);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(Ui.dp(context, 14), Ui.dp(context, 12),
                Ui.dp(context, 14), Ui.dp(context, 12));
        row.setClickable(true);
        row.setFocusable(true);
        row.setBackground(Ui.drawable(context, R.drawable.ripple_round));
        row.addView(Ui.iconCircle(context, R.drawable.ic_update,
                Ui.color(context, R.color.text_secondary), 40, 21));
        LinearLayout texts = Ui.vertical(context);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        textParams.leftMargin = Ui.dp(context, 12);
        texts.addView(Ui.text(context, "检查更新", 14,
                Ui.color(context, R.color.text_primary), true));
        LinearLayout.LayoutParams subtitleParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        subtitleParams.topMargin = Ui.dp(context, 3);
        texts.addView(updateSubtitle, subtitleParams);
        row.addView(texts, textParams);
        row.addView(Ui.icon(context, R.drawable.ic_chevron_right, 16,
                Ui.color(context, R.color.text_tertiary)));
        row.setOnClickListener(v -> checkForUpdates());
        Ui.pressScale(row);
        return row;
    }

    private void checkForUpdates() {
        if (checkingUpdate) {
            return;
        }
        final Context context = getContext();
        checkingUpdate = true;
        updateSubtitle.setText("正在检查更新…");

        LinearLayout box = Ui.vertical(context);
        box.setGravity(Gravity.CENTER_HORIZONTAL);
        box.setPadding(Ui.dp(context, 24), Ui.dp(context, 12),
                Ui.dp(context, 24), Ui.dp(context, 4));
        ProgressBar progress = new ProgressBar(context);
        progress.setIndeterminate(true);
        box.addView(progress, new LinearLayout.LayoutParams(
                Ui.dp(context, 30), Ui.dp(context, 30)));
        TextView status = Ui.text(context, "正在连接 GitHub…", 12,
                Ui.color(context, R.color.text_secondary), false);
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        statusParams.topMargin = Ui.dp(context, 10);
        box.addView(status, statusParams);

        AlertDialog dialog = new MaterialAlertDialogBuilder(context)
                .setTitle("检查更新")
                .setView(box)
                .setCancelable(false)
                .create();
        dialog.show();

        UpdateManager.checkForUpdate((update, error) -> {
            checkingUpdate = false;
            if (dialog.isShowing()) {
                dialog.dismiss();
            }
            if (error != null) {
                updateSubtitle.setText("当前版本 v" + BuildConfig.VERSION_NAME);
                showMessage(context, "检查更新失败",
                        error + "\n\n请检查网络后重试。");
                return;
            }
            if (update == null) {
                updateSubtitle.setText("当前版本 v" + BuildConfig.VERSION_NAME);
                showMessage(context, "检查更新失败", "没有读取到版本信息。");
                return;
            }
            if (!update.newer) {
                updateSubtitle.setText("已是最新 v" + BuildConfig.VERSION_NAME);
                showMessage(context, "检查更新",
                        "当前已是最新版本 v" + BuildConfig.VERSION_NAME + "。");
                return;
            }
            updateSubtitle.setText("发现新版本 v" + update.version);
            showUpdateAvailable(context, update);
        });
    }

    private void showUpdateAvailable(Context context, UpdateManager.UpdateInfo update) {
        String message = update.notes;
        if (update.sizeBytes > 0) {
            message += "\n\n安装包：" + formatSize(update.sizeBytes);
        }
        AlertDialog dialog = new MaterialAlertDialogBuilder(context)
                .setTitle("发现新版本 v" + update.version)
                .setMessage(message)
                .setNegativeButton("稍后", null)
                .setPositiveButton("下载更新",
                        (d, which) -> downloadUpdate(context, update))
                .create();
        dialog.setOnShowListener(ignored -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    .setTextColor(Ui.color(context, R.color.accent));
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                    .setTextColor(Ui.color(context, R.color.text_secondary));
        });
        dialog.show();
    }

    private void downloadUpdate(Context context, UpdateManager.UpdateInfo update) {
        LinearLayout box = Ui.vertical(context);
        box.setPadding(Ui.dp(context, 24), Ui.dp(context, 12),
                Ui.dp(context, 24), Ui.dp(context, 4));
        ProgressBar bar = new ProgressBar(context, null,
                android.R.attr.progressBarStyleHorizontal);
        bar.setMax(100);
        bar.setProgress(0);
        bar.setProgressTintList(ColorStateList.valueOf(
                Ui.color(context, R.color.accent)));
        box.addView(bar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(context, 8)));
        TextView status = Ui.text(context, "正在下载 v" + update.version + "…", 12,
                Ui.color(context, R.color.text_secondary), false);
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        statusParams.topMargin = Ui.dp(context, 10);
        box.addView(status, statusParams);

        AlertDialog dialog = new MaterialAlertDialogBuilder(context)
                .setTitle("下载更新")
                .setView(box)
                .setCancelable(false)
                .create();
        dialog.show();

        UpdateManager.download(context, update, new UpdateManager.DownloadCallback() {
            @Override
            public void onProgress(int percent) {
                bar.setProgress(percent);
                status.setText("正在下载 " + percent + "%");
            }

            @Override
            public void onComplete(File file) {
                if (dialog.isShowing()) {
                    dialog.dismiss();
                }
                updateSubtitle.setText("下载完成，等待安装");
                installDownloaded(context, file, update);
            }

            @Override
            public void onError(String message) {
                if (dialog.isShowing()) {
                    dialog.dismiss();
                }
                updateSubtitle.setText("当前版本 v" + BuildConfig.VERSION_NAME);
                showMessage(context, "下载失败", message + "\n\n请检查网络后重试。");
            }
        });
    }

    private void installDownloaded(Context context, File file, UpdateManager.UpdateInfo update) {
        UpdateManager.InstallResult result = UpdateManager.install(context, file);
        switch (result) {
            case STARTED:
                pendingInstallFile = null;
                updateSubtitle.setText("等待安装 v" + update.version);
                Ui.toast(context, "已打开安装程序");
                break;
            case NEED_PERMISSION:
                pendingInstallFile = file;
                showInstallPermission(context);
                break;
            case NO_INSTALLER:
                showMessage(context, "无法安装",
                        "系统没有可用的 APK 安装程序。");
                break;
            case ERROR:
            default:
                showMessage(context, "无法安装",
                        "安装包打开失败，请重新下载。");
                break;
        }
    }

    private void showInstallPermission(Context context) {
        AlertDialog dialog = new MaterialAlertDialogBuilder(context)
                .setTitle("需要安装权限")
                .setMessage("Android 需要允许「简单记账」安装应用，更新才能继续。"
                        + "返回应用后会自动继续安装。")
                .setNegativeButton("稍后", (d, which) -> pendingInstallFile = null)
                .setPositiveButton("去设置", null)
                .create();
        dialog.setOnShowListener(ignored -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    .setTextColor(Ui.color(context, R.color.accent));
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                    .setTextColor(Ui.color(context, R.color.text_secondary));
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                if (!UpdateManager.openInstallPermissionSettings(context)) {
                    pendingInstallFile = null;
                    showMessage(context, "无法打开设置", "请手动在系统设置中允许安装未知应用。");
                }
                dialog.dismiss();
            });
        });
        dialog.show();
    }

    /** Called by MainActivity when returning from the install-permission settings screen. */
    public void onResumeFromActivity() {
        if (pendingInstallFile == null || !pendingInstallFile.exists()) {
            return;
        }
        if (UpdateManager.needsInstallPermission(getContext())) {
            return;
        }
        File file = pendingInstallFile;
        pendingInstallFile = null;
        UpdateManager.InstallResult result = UpdateManager.install(getContext(), file);
        if (result == UpdateManager.InstallResult.STARTED) {
            Ui.toast(getContext(), "已打开安装程序");
        } else if (result == UpdateManager.InstallResult.NEED_PERMISSION) {
            pendingInstallFile = file;
            showInstallPermission(getContext());
        }
    }

    private void showMessage(Context context, String title, String message) {
        AlertDialog dialog = new MaterialAlertDialogBuilder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("知道了", null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setTextColor(Ui.color(context, R.color.accent)));
        dialog.show();
    }

    private static String formatSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        if (bytes < 1024 * 1024) {
            return String.format(Locale.CHINA, "%.1f KB", bytes / 1024.0);
        }
        return String.format(Locale.CHINA, "%.1f MB", bytes / 1024.0 / 1024.0);
    }

    private LinearLayout settingRow(Context context, int iconRes, String title,
                                    CharSequence subtitle, boolean external,
                                    View.OnClickListener click) {
        return settingRow(context, iconRes, title, subtitle, external, click, false);
    }

    private LinearLayout settingRow(Context context, int iconRes, String title,
                                    CharSequence subtitle, boolean external,
                                    View.OnClickListener click, boolean danger) {
        LinearLayout row = Ui.horizontal(context);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(Ui.dp(context, 14), Ui.dp(context, 12),
                Ui.dp(context, 14), Ui.dp(context, 12));
        row.setClickable(true);
        row.setFocusable(true);
        row.setBackground(Ui.drawable(context, R.drawable.ripple_round));
        int tint = danger ? Ui.color(context, R.color.danger)
                : Ui.color(context, R.color.text_secondary);
        row.addView(Ui.iconCircle(context, iconRes, tint, 40, 21));
        LinearLayout texts = Ui.vertical(context);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        textParams.leftMargin = Ui.dp(context, 12);
        TextView titleView = Ui.text(context, title, 14,
                danger ? Ui.color(context, R.color.danger)
                        : Ui.color(context, R.color.text_primary), true);
        texts.addView(titleView);
        if (subtitle != null && subtitle.length() > 0) {
            TextView subtitleView = Ui.text(context, subtitle, 9,
                    Ui.color(context, R.color.text_secondary), false);
            subtitleView.setMaxLines(2);
            LinearLayout.LayoutParams subtitleParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            subtitleParams.topMargin = Ui.dp(context, 3);
            texts.addView(subtitleView, subtitleParams);
        }
        row.addView(texts, textParams);
        row.addView(Ui.icon(context,
                external ? R.drawable.ic_github : R.drawable.ic_chevron_right,
                16, Ui.color(context, R.color.text_tertiary)));
        row.setOnClickListener(click);
        Ui.pressScale(row);
        return row;
    }
}
