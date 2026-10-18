package com.local.simpleledger.ui;

import android.content.Context;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.local.simpleledger.R;
import com.local.simpleledger.util.LedgerPrefs;
import com.local.simpleledger.util.Money;

/** Full-page editor for the three green / orange / red warning pairs. */
public final class WarningSettingsPage extends LinearLayout {

    private final Navigator navigator;
    private final ThresholdGroup day;
    private final ThresholdGroup week;
    private final ThresholdGroup month;

    public WarningSettingsPage(Context context, Navigator navigator) {
        super(context);
        this.navigator = navigator;
        setOrientation(VERTICAL);
        setBackgroundColor(Ui.color(context, R.color.bg));
        addView(Ui.toolbar(context, "预警设置", navigator::popPage));

        ScrollView scroll = new ScrollView(context);
        scroll.setFillViewport(true);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setClipToPadding(false);
        scroll.setOverScrollMode(OVER_SCROLL_NEVER);
        LinearLayout body = Ui.vertical(context);
        body.setPadding(Ui.dp(context, 20), Ui.dp(context, 4),
                Ui.dp(context, 20), Ui.dp(context, 20));
        scroll.addView(body);
        addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        LedgerPrefs.WarningSettings current = LedgerPrefs.warnings(context);
        day = addGroup(body, "本日", "每天的花费", current.dayOrange, current.dayRed);
        week = addGroup(body, "本周", "周一至周日", current.weekOrange, current.weekRed);
        month = addGroup(body, "本月", "自然月", current.monthOrange, current.monthRed);

        LinearLayout footer = Ui.horizontal(context);
        footer.setGravity(Gravity.CENTER_VERTICAL);
        footer.setPadding(Ui.dp(context, 20), Ui.dp(context, 10),
                Ui.dp(context, 20), Ui.dp(context, 14));
        MaterialButton reset = Ui.textButton(context, "恢复默认",
                Ui.color(context, R.color.text_secondary));
        reset.setOnClickListener(v -> restoreDefaults());
        footer.addView(reset, new LinearLayout.LayoutParams(
                Ui.dp(context, 96), Ui.dp(context, 52)));
        MaterialButton save = Ui.primaryButton(context, "保存设置");
        save.setOnClickListener(v -> save());
        LinearLayout.LayoutParams saveParams = new LinearLayout.LayoutParams(
                0, Ui.dp(context, 52), 1f);
        saveParams.leftMargin = Ui.dp(context, 8);
        footer.addView(save, saveParams);
        addView(footer, Ui.lpMatchWrap());
    }

    private ThresholdGroup addGroup(LinearLayout parent, String title, String subtitle,
                                     long orangeCents, long redCents) {
        Context context = getContext();
        MaterialCardView card = Ui.card(context, 20, true, false);
        LinearLayout content = Ui.vertical(context);
        content.setPadding(Ui.dp(context, 15), Ui.dp(context, 14),
                Ui.dp(context, 15), Ui.dp(context, 14));
        LinearLayout header = Ui.horizontal(context);
        header.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout texts = Ui.vertical(context);
        texts.addView(Ui.text(context, title, 16,
                Ui.color(context, R.color.text_primary), true));
        texts.addView(Ui.text(context, subtitle, 9,
                Ui.color(context, R.color.text_tertiary), false));
        header.addView(texts, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView preview = Ui.chip(context, "绿 / 橙 / 红",
                Ui.color(context, R.color.accent));
        header.addView(preview);
        content.addView(header);

        LinearLayout fields = Ui.horizontal(context);
        LinearLayout.LayoutParams fieldsParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        fieldsParams.topMargin = Ui.dp(context, 12);
        EditText orange = thresholdInput(context, "0.00", orangeCents);
        EditText red = thresholdInput(context, "0.00", redCents);
        LinearLayout orangeField = thresholdField(context, "橙色线（元）", orange);
        LinearLayout redField = thresholdField(context, "红色线（元）", red);
        LinearLayout.LayoutParams orangeParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        orangeParams.rightMargin = Ui.dp(context, 5);
        fields.addView(orangeField, orangeParams);
        LinearLayout.LayoutParams redParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        redParams.leftMargin = Ui.dp(context, 5);
        fields.addView(redField, redParams);
        content.addView(fields, fieldsParams);

        LinearLayout stages = Ui.horizontal(context);
        LinearLayout.LayoutParams stagesParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        stagesParams.topMargin = Ui.dp(context, 10);
        TextView green = stage(context, "绿", Ui.color(context, R.color.warning_green));
        TextView orangeStage = stage(context, "橙", Ui.color(context, R.color.warning_orange));
        TextView redStage = stage(context, "红", Ui.color(context, R.color.warning_red));
        stages.addView(green, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        LinearLayout.LayoutParams middleParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        middleParams.leftMargin = Ui.dp(context, 4);
        middleParams.rightMargin = Ui.dp(context, 4);
        stages.addView(orangeStage, middleParams);
        stages.addView(redStage, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        content.addView(stages, stagesParams);

        ThresholdGroup group = new ThresholdGroup(orange, red, green, orangeStage, redStage);
        group.refreshStage();
        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                group.refreshStage();
            }
        };
        orange.addTextChangedListener(watcher);
        red.addTextChangedListener(watcher);

        card.addView(content);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardParams.topMargin = Ui.dp(context, 10);
        parent.addView(card, cardParams);
        return group;
    }

    private LinearLayout thresholdField(Context context, String label, EditText input) {
        LinearLayout wrapper = Ui.vertical(context);
        TextView labelView = Ui.text(context, label, 9,
                Ui.color(context, R.color.text_tertiary), true);
        labelView.setPadding(Ui.dp(context, 4), 0, 0, 0);
        wrapper.addView(labelView);
        LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(context, 48));
        inputParams.topMargin = Ui.dp(context, 5);
        wrapper.addView(input, inputParams);
        return wrapper;
    }

    private EditText thresholdInput(Context context, String hint, long cents) {        EditText input = Ui.input(context, hint);
        input.setText(inputValue(cents));
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setTextSize(14);
        input.setFilters(new android.text.InputFilter[]{
                new android.text.InputFilter.LengthFilter(10)});
        input.addTextChangedListener(new TextWatcher() {
            private boolean applying;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (applying) {
                    return;
                }
                String clean = Money.sanitizeAmount(s.toString());
                if (!clean.equals(s.toString())) {
                    applying = true;
                    input.setText(clean);
                    input.setSelection(clean.length());
                    applying = false;
                }
            }
        });
        return input;
    }

    private TextView stage(Context context, String label, int color) {
        TextView view = Ui.text(context, label, 10, color, true);
        view.setGravity(Gravity.CENTER);
        view.setPadding(0, Ui.dp(context, 7), 0, Ui.dp(context, 7));
        view.setBackground(Ui.roundRect(context, Ui.alpha(color, 0.12f), 9));
        view.setMaxLines(2);
        return view;
    }

    private void restoreDefaults() {
        LedgerPrefs.WarningSettings defaults = LedgerPrefs.WarningSettings.defaults();
        day.setValues(defaults.dayOrange, defaults.dayRed);
        week.setValues(defaults.weekOrange, defaults.weekRed);
        month.setValues(defaults.monthOrange, defaults.monthRed);
        Ui.toast(getContext(), "已恢复默认值，请点保存");
    }

    private void save() {
        if (!validateGroup(day, "本日") || !validateGroup(week, "本周")
                || !validateGroup(month, "本月")) {
            return;
        }
        LedgerPrefs.saveWarnings(getContext(), new LedgerPrefs.WarningSettings(
                day.orangeCents(), day.redCents(),
                week.orangeCents(), week.redCents(),
                month.orangeCents(), month.redCents()));
        Ui.toast(getContext(), "预警设置已保存");
        navigator.dataChanged();
        navigator.popPage();
    }

    /** Validates one group and shows inline errors. */
    private boolean validateGroup(ThresholdGroup group, String label) {
        long orange = group.orangeCents();
        long red = group.redCents();
        boolean valid = true;
        if (orange <= 0) {
            group.orange.setError("请输入大于 0 的橙色线");
            valid = false;
        } else {
            group.orange.setError(null);
        }
        if (red <= 0) {
            group.red.setError("请输入大于 0 的红色线");
            valid = false;
        } else if (orange > 0 && red <= orange) {
            group.red.setError("红色线需高于橙色线");
            valid = false;
        } else {
            group.red.setError(null);
        }
        if (!valid) {
            Ui.toast(getContext(), label + "的预警线需要检查");
        }
        return valid;
    }

    private static String inputValue(long cents) {
        if (cents % 100 == 0) {
            return String.valueOf(cents / 100);
        }
        return Money.plain(cents);
    }

    private static final class ThresholdGroup {
        final EditText orange;
        final EditText red;
        final TextView greenStage;
        final TextView orangeStage;
        final TextView redStage;

        ThresholdGroup(EditText orange, EditText red, TextView greenStage,
                       TextView orangeStage, TextView redStage) {
            this.orange = orange;
            this.red = red;
            this.greenStage = greenStage;
            this.orangeStage = orangeStage;
            this.redStage = redStage;
        }

        long orangeCents() {
            return Money.parseCents(orange.getText().toString());
        }

        long redCents() {
            return Money.parseCents(red.getText().toString());
        }

        void setValues(long orangeCents, long redCents) {
            orange.setText(inputValue(orangeCents));
            red.setText(inputValue(redCents));
            refreshStage();
        }

        void refreshStage() {
            long orangeValue = orangeCents();
            long redValue = redCents();
            greenStage.setText("绿\n< " + display(orangeValue));
            orangeStage.setText("橙\n" + display(orangeValue)
                    + "–" + display(redValue));
            redStage.setText("红\n> " + display(redValue));
        }

        private static String display(long cents) {
            return cents > 0 ? "¥" + (cents % 100 == 0
                    ? String.valueOf(cents / 100) : Money.plain(cents)) : "—";
        }
    }
}
