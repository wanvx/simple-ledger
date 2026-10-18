package com.local.simpleledger.ui;

import android.content.Context;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.local.simpleledger.R;

import java.util.ArrayList;
import java.util.List;

/** Compact pill-style segmented switch used for month/week and other binary choices. */
public final class SegmentedControl extends LinearLayout {

    public interface Listener {
        void onSelected(int index);
    }

    private final List<TextView> segments = new ArrayList<>();
    private Listener listener;
    private int selected = 0;

    public SegmentedControl(Context context, String[] labels) {
        super(context);
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER);
        setPadding(Ui.dp(context, 3), Ui.dp(context, 3),
                Ui.dp(context, 3), Ui.dp(context, 3));
        setBackground(Ui.roundRect(context, Ui.color(context, R.color.surface_alt), 14));
        for (int i = 0; i < labels.length; i++) {
            final int index = i;
            TextView label = Ui.text(context, labels[i], 13,
                    Ui.color(context, R.color.text_secondary), true);
            label.setGravity(Gravity.CENTER);
            label.setPadding(Ui.dp(context, 14), Ui.dp(context, 8),
                    Ui.dp(context, 14), Ui.dp(context, 8));
            label.setOnClickListener(v -> select(index, true));
            label.setClickable(true);
            label.setFocusable(true);
            addView(label, new LinearLayout.LayoutParams(
                    0, Ui.dp(context, 34), 1f));
            segments.add(label);
        }
        style();
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public int getSelected() {
        return selected;
    }

    public void select(int index, boolean notify) {
        int safe = Math.max(0, Math.min(segments.size() - 1, index));
        if (selected == safe && notify) {
            style();
            return;
        }
        selected = safe;
        style();
        if (notify && listener != null) {
            listener.onSelected(selected);
        }
    }

    private void style() {
        for (int i = 0; i < segments.size(); i++) {
            TextView label = segments.get(i);
            boolean active = i == selected;
            label.setTextColor(Ui.color(getContext(),
                    active ? R.color.accent : R.color.text_secondary));
            label.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            if (active) {
                label.setBackground(Ui.roundRect(getContext(),
                        Ui.color(getContext(), R.color.surface), 11,
                        1, Ui.color(getContext(), R.color.divider)));
                label.animate().alpha(1f).setDuration(140).start();
            } else {
                label.setBackground(null);
                label.animate().alpha(0.82f).setDuration(140).start();
            }
        }
    }

    public void setEnabledAll(boolean enabled) {
        for (TextView segment : segments) {
            segment.setEnabled(enabled);
            segment.setAlpha(enabled ? 1f : 0.45f);
        }
    }

    public View segmentAt(int index) {
        return segments.get(index);
    }
}
