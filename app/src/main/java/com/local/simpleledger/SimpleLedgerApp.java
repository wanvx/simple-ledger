package com.local.simpleledger;

import android.app.Application;

import androidx.appcompat.app.AppCompatDelegate;

import com.local.simpleledger.util.LedgerPrefs;

public final class SimpleLedgerApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        AppCompatDelegate.setDefaultNightMode(LedgerPrefs.isNight(this)
                ? AppCompatDelegate.MODE_NIGHT_YES
                : AppCompatDelegate.MODE_NIGHT_NO);
    }
}
