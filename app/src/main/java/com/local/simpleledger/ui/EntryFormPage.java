package com.local.simpleledger.ui;

import android.app.DatePickerDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.local.simpleledger.R;
import com.local.simpleledger.data.Entry;
import com.local.simpleledger.data.LedgerDb;
import com.local.simpleledger.model.Category;
import com.local.simpleledger.util.DateUtils;
import com.local.simpleledger.util.Money;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/** The add / edit form: choose a subcategory, name the item and enter an amount. */
public final class EntryFormPage extends LinearLayout {

    private final LedgerDb db;
    private final Navigator navigator;
    private final TextView categoryTitle;
    private final LinearLayout categoryIconHolder;
    private final FlowLayout chipContainer;
    private final LinearLayout customNameContainer;
    private final EditText customNameInput;
    private final EditText itemInput;
    private final EditText amountInput;
    private final TextView dateValue;
    private final TextView preview;
    private final MaterialCardView dateRow;
    private final List<TextView> chips = new ArrayList<>();

    private Category category;
    private String selectedSubcategory;
    private String dateKey;
    private Entry editing;
    private boolean applyingAmount;
    private boolean saving;

    public EntryFormPage(Context context, LedgerDb db, Navigator navigator,
                         String categoryKey, long entryId, String presetDate) {
        super(context);
        this.db = db;
        this.navigator = navigator;
        this.category = Category.byKey(categoryKey);
        this.dateKey = presetDate != null ? presetDate : DateUtils.todayKey();
        if (entryId > 0) {
            editing = db.entry(entryId);
            if (editing != null) {
                this.category = editing.category();
                this.dateKey = editing.date;
            }
        }

        setOrientation(VERTICAL);
        setBackgroundColor(Ui.color(context, R.color.bg));

        addView(Ui.toolbar(context, editing == null ? "记一笔" : "修改记录",
                navigator::popPage));

        ScrollView scroll = new ScrollView(context);
        scroll.setFillViewport(true);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setClipToPadding(false);
        scroll.setOverScrollMode(OVER_SCROLL_NEVER);
        LinearLayout body = Ui.vertical(context);
        body.setPadding(Ui.dp(context, 20), Ui.dp(context, 8),
                Ui.dp(context, 20), Ui.dp(context, 20));
        scroll.addView(body, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        MaterialCardView header = Ui.card(context);
        header.setClickable(false);
        LinearLayout headerRow = Ui.horizontal(context);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);
        headerRow.setPadding(Ui.dp(context, 15), Ui.dp(context, 14),
                Ui.dp(context, 12), Ui.dp(context, 14));
        categoryIconHolder = Ui.vertical(context);
        categoryIconHolder.setGravity(Gravity.CENTER);
        headerRow.addView(categoryIconHolder, new LinearLayout.LayoutParams(
                Ui.dp(context, 48), Ui.dp(context, 48)));
        LinearLayout headerTexts = Ui.vertical(context);
        LinearLayout.LayoutParams headerTextParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        headerTextParams.leftMargin = Ui.dp(context, 13);
        categoryTitle = Ui.text(context, "", 19,
                Ui.color(context, R.color.text_primary), true);
        headerTexts.addView(categoryTitle);
        headerRow.addView(headerTexts, headerTextParams);
        TextView change = Ui.text(context, "更换", 12,
                Ui.color(context, R.color.accent), true);
        change.setPadding(Ui.dp(context, 10), Ui.dp(context, 7),
                Ui.dp(context, 8), Ui.dp(context, 7));
        change.setOnClickListener(v -> showCategoryPicker());
        headerRow.addView(change);
        header.addView(headerRow);
        body.addView(header, Ui.lpMatchWrap());

        body.addView(sectionLabel("选择小类", 22));
        chipContainer = new FlowLayout(context);
        chipContainer.setSpacing(8, 8);
        body.addView(chipContainer, Ui.lpMatchWrap());

        customNameContainer = Ui.vertical(context);
        customNameContainer.addView(sectionLabel("具体名称", 18));
        customNameInput = Ui.input(context, "例如：话费");
        customNameInput.setFilters(new InputFilter[]{new InputFilter.LengthFilter(20)});
        customNameInput.setImeOptions(EditorInfo.IME_ACTION_NEXT);
        customNameContainer.addView(customNameInput, Ui.lpMatch(Ui.dp(context, 52)));
        customNameContainer.setVisibility(GONE);
        body.addView(customNameContainer, Ui.lpMatchWrap());

        body.addView(sectionLabel("项目名", 22));
        itemInput = Ui.input(context, "");
        itemInput.setFilters(new InputFilter[]{new InputFilter.LengthFilter(30)});
        itemInput.setImeOptions(EditorInfo.IME_ACTION_NEXT);
        body.addView(itemInput, Ui.lpMatch(Ui.dp(context, 52)));

        body.addView(sectionLabel("金额", 22));
        MaterialCardView amountCard = Ui.card(context);
        amountCard.setClickable(false);
        LinearLayout amountRow = Ui.horizontal(context);
        amountRow.setGravity(Gravity.CENTER_VERTICAL);
        amountRow.setPadding(Ui.dp(context, 16), Ui.dp(context, 8),
                Ui.dp(context, 14), Ui.dp(context, 8));
        TextView currency = Ui.text(context, "¥", 25,
                Ui.color(context, R.color.text_secondary), true);
        amountRow.addView(currency);
        amountInput = new EditText(context);
        amountInput.setHint("0.00");
        amountInput.setTextSize(31);
        amountInput.setTextColor(Ui.color(context, R.color.text_primary));
        amountInput.setHintTextColor(Ui.color(context, R.color.text_tertiary));
        amountInput.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        amountInput.setBackground(null);
        amountInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        amountInput.setImeOptions(EditorInfo.IME_ACTION_DONE);
        amountInput.setPadding(Ui.dp(context, 8), 0, 0, 0);
        amountInput.setSingleLine(true);
        amountRow.addView(amountInput, new LinearLayout.LayoutParams(
                0, Ui.dp(context, 54), 1f));
        TextView suffix = Ui.text(context, "元", 12,
                Ui.color(context, R.color.text_tertiary), false);
        amountRow.addView(suffix);
        amountCard.addView(amountRow);
        body.addView(amountCard, Ui.lpMatchWrap());

        body.addView(sectionLabel("日期", 22));
        dateRow = Ui.card(context, 16, true, true);
        LinearLayout dateInner = Ui.horizontal(context);
        dateInner.setGravity(Gravity.CENTER_VERTICAL);
        dateInner.setPadding(Ui.dp(context, 15), Ui.dp(context, 13),
                Ui.dp(context, 14), Ui.dp(context, 13));
        dateInner.addView(Ui.icon(context, R.drawable.ic_tab_records, 19,
                Ui.color(context, R.color.text_secondary)));
        dateValue = Ui.text(context, "", 14,
                Ui.color(context, R.color.text_primary), true);
        LinearLayout.LayoutParams dateTextParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        dateTextParams.leftMargin = Ui.dp(context, 10);
        dateInner.addView(dateValue, dateTextParams);
        dateInner.addView(Ui.icon(context, R.drawable.ic_chevron_right, 17,
                Ui.color(context, R.color.text_tertiary)));
        dateRow.addView(dateInner);
        dateRow.setOnClickListener(v -> pickDate());
        body.addView(dateRow, Ui.lpMatchWrap());

        preview = Ui.text(context, "", 12,
                Ui.color(context, R.color.text_tertiary), false);
        preview.setPadding(Ui.dp(context, 4), Ui.dp(context, 14),
                Ui.dp(context, 4), Ui.dp(context, 4));
        body.addView(preview, Ui.lpMatchWrap());

        LinearLayout footer = Ui.horizontal(context);
        footer.setGravity(Gravity.CENTER_VERTICAL);
        footer.setPadding(Ui.dp(context, 20), Ui.dp(context, 10),
                Ui.dp(context, 20), Ui.dp(context, 14));
        footer.setBackgroundColor(Ui.color(context, R.color.bg));
        if (editing != null) {
            android.widget.ImageView delete = Ui.iconButton(context, R.drawable.ic_delete,
                    Ui.color(context, R.color.danger), 21, v -> confirmDelete());
            delete.setContentDescription("删除这笔");
            delete.setBackground(Ui.roundRect(context,
                    Ui.color(context, R.color.surface_alt), 16));
            LinearLayout.LayoutParams deleteParams = new LinearLayout.LayoutParams(
                    Ui.dp(context, 52), Ui.dp(context, 52));
            deleteParams.rightMargin = Ui.dp(context, 10);
            footer.addView(delete, deleteParams);
        }
        com.google.android.material.button.MaterialButton save =
                Ui.primaryButton(context, editing == null ? "记下这笔" : "保存修改");
        save.setOnClickListener(v -> save());
        footer.addView(save, new LinearLayout.LayoutParams(
                0, Ui.dp(context, 52), 1f));
        addView(footer, Ui.lpMatchWrap());

        amountInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (applyingAmount) {
                    return;
                }
                String clean = Money.sanitizeAmount(s.toString());
                if (!clean.equals(s.toString())) {
                    applyingAmount = true;
                    amountInput.setText(clean);
                    amountInput.setSelection(clean.length());
                    applyingAmount = false;
                }
                updatePreview();
            }
        });
        itemInput.addTextChangedListener(new SimpleWatcher(this::updatePreview));
        customNameInput.addTextChangedListener(new SimpleWatcher(this::updatePreview));

        renderCategory();
        if (editing != null) {
            selectedSubcategory = editing.subcategory;
            customNameInput.setText(editing.customLabel);
            itemInput.setText(editing.item);
            itemInput.setSelection(itemInput.length());
            amountInput.setText(Money.input(editing.amountCents));
            renderChips();
        } else {
            renderChips();
        }
        updateCustomFieldVisibility();
        dateValue.setText(DateUtils.friendlyDate(dateKey) + " · " + dateKey);
        updatePreview();
        Ui.showKeyboard(amountInput);
    }

    private TextView sectionLabel(String text, int topMarginDp) {
        TextView label = Ui.label(getContext(), text, 11);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = Ui.dp(getContext(), topMarginDp);
        params.bottomMargin = Ui.dp(getContext(), 8);
        params.leftMargin = Ui.dp(getContext(), 3);
        label.setLayoutParams(params);
        return label;
    }

    private void renderCategory() {
        Context context = getContext();
        int color = category.color(context);
        categoryTitle.setText(category.title);
        categoryIconHolder.removeAllViews();
        categoryIconHolder.setBackground(Ui.circle(Ui.alpha(color, 0.13f)));
        categoryIconHolder.addView(Ui.icon(context, category.iconRes, 25, color));
        renderChips();
    }

    private void renderChips() {
        Context context = getContext();
        chipContainer.removeAllViews();
        chips.clear();
        if (selectedSubcategory != null && !category.hasSubcategory(selectedSubcategory)) {
            selectedSubcategory = null;
        }
        for (String subcategory : category.subcategories()) {
            TextView chip = Ui.text(context, subcategory, 13,
                    Ui.color(context, R.color.text_secondary), true);
            chip.setGravity(Gravity.CENTER);
            chip.setPadding(Ui.dp(context, 14), Ui.dp(context, 10),
                    Ui.dp(context, 14), Ui.dp(context, 10));
            chip.setBackground(Ui.roundRect(context,
                    Ui.color(context, R.color.surface), 13,
                    1, Ui.color(context, R.color.divider)));
            chip.setClickable(true);
            chip.setFocusable(true);
            chip.setOnClickListener(v -> {
                selectedSubcategory = subcategory;
                v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                styleChips();
                updateCustomFieldVisibility();
                updatePreview();
            });
            chipContainer.addView(chip);
            chips.add(chip);
        }
        styleChips();
        updateCustomFieldVisibility();
    }

    private void styleChips() {
        Context context = getContext();
        int accent = Ui.color(context, R.color.accent);
        String[] subs = category.subcategories();
        for (int i = 0; i < chips.size(); i++) {
            TextView chip = chips.get(i);
            boolean selected = subs[i].equals(selectedSubcategory);
            chip.setTextColor(selected
                    ? Ui.color(context, R.color.accent_contrast)
                    : Ui.color(context, R.color.text_secondary));
            chip.setBackground(Ui.roundRect(context,
                    selected ? accent : Ui.color(context, R.color.surface),
                    13, 1, selected ? accent : Ui.color(context, R.color.divider)));
            chip.animate().scaleX(selected ? 1.02f : 1f).scaleY(selected ? 1.02f : 1f)
                    .setDuration(130).start();
        }
    }

    private void pickDate() {
        Calendar initial = DateUtils.parse(dateKey);
        if (initial == null) {
            initial = DateUtils.today();
        }
        DatePickerDialog dialog = new DatePickerDialog(getContext(), (view, year, month, day) -> {
            Calendar picked = Calendar.getInstance();
            picked.set(year, month, day);
            dateKey = DateUtils.key(picked);
            dateValue.setText(DateUtils.friendlyDate(dateKey) + " · " + dateKey);
            updatePreview();
        }, initial.get(Calendar.YEAR), initial.get(Calendar.MONTH),
                initial.get(Calendar.DAY_OF_MONTH));
        dialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        dialog.show();
        dialog.getButton(DatePickerDialog.BUTTON_POSITIVE)
                .setTextColor(Ui.color(getContext(), R.color.accent));
        dialog.getButton(DatePickerDialog.BUTTON_NEGATIVE)
                .setTextColor(Ui.color(getContext(), R.color.text_secondary));
    }

    private void showCategoryPicker() {
        String[] labels = new String[Category.values().length];
        for (int i = 0; i < Category.values().length; i++) {
            labels[i] = Category.values()[i].title;
        }
        AlertDialog dialog = new MaterialAlertDialogBuilder(getContext())
                .setTitle("选择大类")
                .setItems(labels, (d, which) -> {
                    Category selected = Category.values()[which];
                    if (selected != category) {
                        category = selected;
                        selectedSubcategory = null;
                        customNameInput.setText("");
                        renderCategory();
                        updatePreview();
                    }
                })
                .setNegativeButton("取消", null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                .setTextColor(Ui.color(getContext(), R.color.text_secondary)));
        dialog.show();
    }

    private void updatePreview() {
        String item = itemInput == null ? "" : itemInput.getText().toString().trim();
        String amountText = amountInput == null ? "" : amountInput.getText().toString();
        long cents = Money.parseCents(amountText);
        if (selectedSubcategory == null || item.isEmpty() || cents <= 0) {
            preview.setVisibility(GONE);
            return;
        }
        preview.setVisibility(VISIBLE);
        preview.setText(category.title + "-" + currentDisplaySubcategory() + "-" + item
                + "：" + Money.yuan(cents));
        preview.setTextColor(Ui.color(getContext(), R.color.accent));
    }

    private boolean isOtherSubcategory() {
        return "其他".equals(selectedSubcategory);
    }

    private String currentDisplaySubcategory() {
        if (isOtherSubcategory()) {
            String custom = customNameInput.getText().toString().trim();
            if (!custom.isEmpty()) {
                return custom;
            }
        }
        return selectedSubcategory == null ? "" : selectedSubcategory;
    }

    private void updateCustomFieldVisibility() {
        boolean show = isOtherSubcategory();
        customNameContainer.setVisibility(show ? VISIBLE : GONE);
        if (!show) {
            customNameInput.clearFocus();
        }
    }

    private void save() {
        if (saving) {
            return;
        }
        if (selectedSubcategory == null) {
            Ui.toast(getContext(), "请先选择一个小类");
            chipContainer.requestFocus();
            return;
        }
        String item = itemInput.getText().toString().trim();
        if (item.isEmpty()) {
            Ui.toast(getContext(), "请填写项目名");
            itemInput.requestFocus();
            Ui.showKeyboard(itemInput);
            return;
        }
        long cents = Money.parseCents(amountInput.getText().toString());
        if (cents <= 0) {
            Ui.toast(getContext(), "请输入大于 0 的金额，最多两位小数");
            amountInput.requestFocus();
            Ui.showKeyboard(amountInput);
            return;
        }
        saving = true;
        Ui.hideKeyboard(amountInput);
        String customLabel = isOtherSubcategory()
                ? customNameInput.getText().toString().trim() : "";
        try {
            if (editing == null) {
                long id = db.addEntry(dateKey, category, selectedSubcategory,
                        customLabel, item, cents);
                if (id <= 0) {
                    saving = false;
                    Ui.toast(getContext(), "保存失败，请重试");
                    return;
                }
                Ui.toast(getContext(), "已记下 " + Money.yuan(cents));
            } else {
                int updated = db.updateEntry(editing.id, dateKey, category,
                        selectedSubcategory, customLabel, item, cents);
                if (updated <= 0) {
                    saving = false;
                    Ui.toast(getContext(), "修改失败，请重试");
                    return;
                }
                Ui.toast(getContext(), "已保存修改");
            }
            performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
            navigator.dataChanged();
            navigator.popPage();
        } catch (Exception e) {
            saving = false;
            Ui.toast(getContext(), "数据保存失败，请重试");
        }
    }

    private void confirmDelete() {
        if (editing == null) {
            return;
        }
        AlertDialog dialog = new MaterialAlertDialogBuilder(getContext())
                .setTitle("删除这笔记录？")
                .setMessage(editing.displayLine() + "  " + Money.yuan(editing.amountCents))
                .setNegativeButton("取消", null)
                .setPositiveButton("删除", null)
                .create();
        dialog.setOnShowListener(ignored -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    .setTextColor(Ui.color(getContext(), R.color.danger));
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                    .setTextColor(Ui.color(getContext(), R.color.text_secondary));
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                int deleted = db.deleteEntry(editing.id);
                dialog.dismiss();
                if (deleted > 0) {
                    Ui.toast(getContext(), "已删除");
                    navigator.dataChanged();
                    navigator.popPage();
                } else {
                    Ui.toast(getContext(), "删除失败，请重试");
                }
            });
        });
        dialog.show();
    }

    private static final class SimpleWatcher implements TextWatcher {
        private final Runnable after;

        SimpleWatcher(Runnable after) {
            this.after = after;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            after.run();
        }
    }
}
