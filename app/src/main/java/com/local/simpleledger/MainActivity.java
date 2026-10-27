package com.local.simpleledger;

import android.os.Bundle;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.local.simpleledger.data.LedgerDb;
import com.local.simpleledger.ui.BreakdownPage;
import com.local.simpleledger.ui.EntryFormPage;
import com.local.simpleledger.ui.ExportPage;
import com.local.simpleledger.ui.HistoryPage;
import com.local.simpleledger.ui.Navigator;
import com.local.simpleledger.ui.ProfilePage;
import com.local.simpleledger.ui.RangeStatsPage;
import com.local.simpleledger.ui.RecordPage;
import com.local.simpleledger.ui.Ui;
import com.local.simpleledger.ui.WarningSettingsPage;

import java.util.ArrayDeque;
import java.util.Deque;

public final class MainActivity extends AppCompatActivity implements Navigator {

    private FrameLayout pageContent;
    private FrameLayout overlayHost;
    private LinearLayout navContainer;
    private LedgerDb db;
    private RecordPage recordPage;
    private HistoryPage historyPage;
    private ProfilePage profilePage;
    private int currentTab = -1;
    private final Deque<View> overlays = new ArrayDeque<>();
    private final NavItem[] navItems = new NavItem[3];

    private static final String[] NAV_LABELS = {"记账", "记录", "我的"};
    private static final int[] NAV_ICONS = {
            R.drawable.ic_tab_ledger, R.drawable.ic_tab_records, R.drawable.ic_tab_profile
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Ui.applySystemBars(this);
        setContentView(R.layout.activity_main);
        db = LedgerDb.get(this);
        pageContent = findViewById(R.id.page_content);
        overlayHost = findViewById(R.id.overlay_host);
        navContainer = findViewById(R.id.nav_container);
        buildBottomNavigation();
        int initialTab = savedInstanceState == null ? 0 : savedInstanceState.getInt("tab", 0);
        showTab(initialTab, false);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt("tab", Math.max(0, currentTab));
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (profilePage != null) {
            profilePage.onResumeFromActivity();
        }
    }

    private void buildBottomNavigation() {
        for (int i = 0; i < 3; i++) {
            View item = getLayoutInflater().inflate(R.layout.nav_item, navContainer, false);
            ImageView icon = item.findViewById(R.id.nav_icon);
            TextView label = item.findViewById(R.id.nav_label);
            icon.setImageResource(NAV_ICONS[i]);
            label.setText(NAV_LABELS[i]);
            final int index = i;
            item.setOnClickListener(v -> {
                v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                showTab(index, true);
            });
            navContainer.addView(item);
            navItems[i] = new NavItem(icon, label);
        }
    }

    private void showTab(int index, boolean animate) {
        if (index == currentTab) {
            refreshPage(index);
            return;
        }
        View next = pageFor(index);
        if (next.getParent() instanceof ViewGroup) {
            ((ViewGroup) next.getParent()).removeView(next);
        }
        pageContent.removeAllViews();
        pageContent.addView(next, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        currentTab = index;
        updateNavSelection(index);
        refreshPage(index);

        if (animate) {
            next.setAlpha(0f);
            next.setTranslationY(Ui.dp(this, 8));
            next.animate().alpha(1f).translationY(0f).setDuration(230)
                    .setInterpolator(new DecelerateInterpolator()).start();
        } else {
            next.setAlpha(1f);
            next.setTranslationY(0f);
        }
    }

    private View pageFor(int index) {
        switch (index) {
            case 1:
                if (historyPage == null) {
                    historyPage = new HistoryPage(this, db, this);
                }
                return historyPage;
            case 2:
                if (profilePage == null) {
                    profilePage = new ProfilePage(this, db, this);
                }
                return profilePage;
            case 0:
            default:
                if (recordPage == null) {
                    recordPage = new RecordPage(this, db, this);
                }
                return recordPage;
        }
    }

    private void refreshPage(int index) {
        switch (index) {
            case 0:
                if (recordPage != null) {
                    recordPage.refresh();
                }
                break;
            case 1:
                if (historyPage != null) {
                    historyPage.refresh();
                }
                break;
            case 2:
                if (profilePage != null) {
                    profilePage.refresh();
                }
                break;
            default:
                break;
        }
    }

    private void updateNavSelection(int index) {
        int active = Ui.color(this, R.color.accent);
        int inactive = Ui.color(this, R.color.text_tertiary);
        for (int i = 0; i < navItems.length; i++) {
            NavItem item = navItems[i];
            if (item == null) {
                continue;
            }
            boolean selected = i == index;
            item.icon.setColorFilter(selected ? active : inactive);
            item.label.setTextColor(selected ? active : inactive);
            item.label.setTypeface(android.graphics.Typeface.create(
                    "sans-serif-medium", android.graphics.Typeface.NORMAL));
            item.icon.animate().scaleX(selected ? 1.08f : 1f)
                    .scaleY(selected ? 1.08f : 1f).setDuration(150).start();
        }
    }

    @Override
    public void openEntryForm(String categoryKey, long entryId, String presetDate) {
        pushOverlay(new EntryFormPage(this, db, this, categoryKey, entryId, presetDate));
    }

    @Override
    public void openBreakdown(String title, String start, String end) {
        pushOverlay(new BreakdownPage(this, db, this, title, start, end));
    }

    @Override
    public void openRangeStats() {
        pushOverlay(new RangeStatsPage(this, db, this));
    }

    @Override
    public void openWarningSettings() {
        pushOverlay(new WarningSettingsPage(this, this));
    }

    @Override
    public void openExport() {
        pushOverlay(new ExportPage(this, db, this));
    }

    private void pushOverlay(View page) {
        page.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        overlayHost.setVisibility(View.VISIBLE);
        overlayHost.addView(page);
        overlays.push(page);
        page.setAlpha(0f);
        page.setTranslationY(Ui.dp(this, 20));
        page.post(() -> page.animate().alpha(1f).translationY(0f).setDuration(260)
                .setInterpolator(new DecelerateInterpolator()).start());
    }

    @Override
    public void popPage() {
        if (overlays.isEmpty()) {
            return;
        }
        View page = overlays.pop();
        page.animate().alpha(0f).translationY(Ui.dp(this, 16))
                .setDuration(190).setInterpolator(new AccelerateInterpolator())
                .withEndAction(() -> {
                    overlayHost.removeView(page);
                    if (overlays.isEmpty()) {
                        overlayHost.setVisibility(View.GONE);
                    }
                }).start();
        refreshPage(currentTab);
    }

    @Override
    public void dataChanged() {
        if (recordPage != null) {
            recordPage.refresh();
        }
        if (historyPage != null) {
            historyPage.refresh();
        }
        if (profilePage != null) {
            profilePage.refresh();
        }
    }

    @Override
    public void openTab(int index) {
        if (!overlays.isEmpty()) {
            while (!overlays.isEmpty()) {
                overlayHost.removeView(overlays.pop());
            }
            overlayHost.setVisibility(View.GONE);
        }
        showTab(index, true);
    }

    @Override
    public void onBackPressed() {
        if (!overlays.isEmpty()) {
            popPage();
            return;
        }
        if (currentTab != 0) {
            showTab(0, true);
            return;
        }
        super.onBackPressed();
    }

    private static final class NavItem {
        final ImageView icon;
        final TextView label;

        NavItem(ImageView icon, TextView label) {
            this.icon = icon;
            this.label = label;
        }
    }
}
