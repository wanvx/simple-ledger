package com.local.simpleledger.ui;

/** Minimal navigation contract between pages and MainActivity. */
public interface Navigator {
    void openEntryForm(String categoryKey, long entryId, String presetDate);

    void openBreakdown(String title, String start, String end);

    void openRangeStats();

    void openWarningSettings();

    void openExport();

    void popPage();

    void dataChanged();

    void openTab(int index);
}
